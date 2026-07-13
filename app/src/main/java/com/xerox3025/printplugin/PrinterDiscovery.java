package com.xerox3025.printplugin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Discovers IPP printers on the local network using mDNS and subnet scanning.
 */
public class PrinterDiscovery {

    private static final String TAG = "PrinterDiscovery";
    private static final int IPP_PORT = 631;
    private static final int CONNECT_TIMEOUT_MS = 400;
    private static final int MDNS_TIMEOUT_MS = 8000;
    private static final int SCAN_TIMEOUT_MS = 12000;

    private static final String[] MDNS_SERVICE_TYPES = {
            "_ipp._tcp",
            "_ipps._tcp",
            "_printer._tcp"
    };

    public static class DiscoveredPrinter {
        public final String ip;
        public final String name;
        public final String source;

        public DiscoveredPrinter(String ip, String name, String source) {
            this.ip = ip;
            this.name = name != null && !name.isEmpty() ? name : ip;
            this.source = source;
        }

        public String getDisplayLabel() {
            if (name.equals(ip)) {
                return ip + " (" + source + ")";
            }
            return name + " — " + ip;
        }
    }

    public interface Callback {
        void onProgress(String message);

        void onPrinterFound(DiscoveredPrinter printer);

        void onComplete(List<DiscoveredPrinter> printers);

        void onError(String message);
    }

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, DiscoveredPrinter> found = Collections.synchronizedMap(new LinkedHashMap<>());
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean finished = new AtomicBoolean(false);

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener[] listeners;
    private Callback callback;

    public PrinterDiscovery(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void discover(Callback callback) {
        this.callback = callback;
        cancelled.set(false);
        finished.set(false);
        found.clear();

        notifyProgress(appContext.getString(R.string.discovery_starting));

        new Thread(() -> {
            ExecutorService scanPool = Executors.newFixedThreadPool(32);
            try {
                startMdnsDiscovery();
                scanSubnet(scanPool);
            } catch (Exception e) {
                PrintLog.e(TAG, "Discovery failed", e);
                notifyError(e.getMessage());
            } finally {
                scanPool.shutdown();
                try {
                    scanPool.awaitTermination(SCAN_TIMEOUT_MS + 5000L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                stopMdnsDiscovery();
                finishDiscovery();
            }
        }).start();
    }

    public void cancel() {
        cancelled.set(true);
        stopMdnsDiscovery();
    }

    private void startMdnsDiscovery() {
        nsdManager = (NsdManager) appContext.getSystemService(Context.NSD_SERVICE);
        if (nsdManager == null) {
            PrintLog.w(TAG, "NsdManager not available");
            return;
        }

        listeners = new NsdManager.DiscoveryListener[MDNS_SERVICE_TYPES.length];
        for (int i = 0; i < MDNS_SERVICE_TYPES.length; i++) {
            final String serviceType = MDNS_SERVICE_TYPES[i];
            listeners[i] = createDiscoveryListener(serviceType);
            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listeners[i]);
                PrintLog.i(TAG, "Started mDNS discovery for " + serviceType);
            } catch (Exception e) {
                PrintLog.w(TAG, "Could not start mDNS for " + serviceType + ": " + e.getMessage());
            }
        }

        try {
            Thread.sleep(MDNS_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private NsdManager.DiscoveryListener createDiscoveryListener(String serviceType) {
        return new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                PrintLog.i(TAG, "mDNS started: " + regType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                if (cancelled.get()) return;
                PrintLog.i(TAG, "mDNS service found: " + service.getServiceName());
                nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        PrintLog.w(TAG, "Resolve failed for " + serviceInfo.getServiceName()
                                + " code=" + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        if (cancelled.get()) return;
                        InetAddress host = serviceInfo.getHost();
                        if (host == null) return;
                        String ip = host.getHostAddress();
                        if (ip == null || ip.contains(":")) return; // skip IPv6 for now
                        addPrinter(new DiscoveredPrinter(
                                ip,
                                serviceInfo.getServiceName(),
                                "mDNS"));
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // no-op
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                PrintLog.i(TAG, "mDNS stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                PrintLog.w(TAG, "mDNS start failed for " + serviceType + " code=" + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                PrintLog.w(TAG, "mDNS stop failed for " + serviceType + " code=" + errorCode);
            }
        };
    }

    private void stopMdnsDiscovery() {
        if (nsdManager == null || listeners == null) return;
        for (NsdManager.DiscoveryListener listener : listeners) {
            if (listener == null) continue;
            try {
                nsdManager.stopServiceDiscovery(listener);
            } catch (Exception ignored) {
            }
        }
        listeners = null;
    }

    private void scanSubnet(ExecutorService scanPool) {
        String localIp = getLocalIpAddress();
        if (localIp == null) {
            notifyProgress(appContext.getString(R.string.discovery_no_local_ip));
            return;
        }

        String prefix = localIp.substring(0, localIp.lastIndexOf('.') + 1);
        notifyProgress(appContext.getString(R.string.discovery_scanning_subnet, prefix + "x"));

        long deadline = System.currentTimeMillis() + SCAN_TIMEOUT_MS;
        for (int host = 1; host <= 254; host++) {
            if (cancelled.get() || System.currentTimeMillis() > deadline) break;
            final String ip = prefix + host;
            if (ip.equals(localIp)) continue;
            scanPool.submit(() -> {
                if (cancelled.get()) return;
                if (isIppPortOpen(ip)) {
                    addPrinter(new DiscoveredPrinter(ip, guessPrinterName(ip), "red"));
                }
            });
        }
    }

    private boolean isIppPortOpen(String ip) {
        if (found.containsKey(ip)) return false;
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(ip, IPP_PORT), CONNECT_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String guessPrinterName(String ip) {
        return "Impresora " + ip;
    }

    private void addPrinter(DiscoveredPrinter printer) {
        if (cancelled.get() || printer.ip == null || printer.ip.isEmpty()) return;
        DiscoveredPrinter previous = found.putIfAbsent(printer.ip, printer);
        if (previous == null) {
            PrintLog.i(TAG, "Found printer: " + printer.getDisplayLabel());
            notifyFound(printer);
        }
    }

    private void finishDiscovery() {
        if (!finished.compareAndSet(false, true)) return;
        List<DiscoveredPrinter> results = new ArrayList<>(found.values());
        mainHandler.post(() -> {
            if (callback != null) callback.onComplete(results);
        });
    }

    private String getLocalIpAddress() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network network = cm.getActiveNetwork();
                if (network != null) {
                    LinkProperties props = cm.getLinkProperties(network);
                    if (props != null) {
                        for (android.net.LinkAddress linkAddress : props.getLinkAddresses()) {
                            InetAddress address = linkAddress.getAddress();
                            if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                                return address.getHostAddress();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            PrintLog.w(TAG, "Could not read active network IP: " + e.getMessage());
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) continue;
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        String ip = address.getHostAddress();
                        if (ip != null && (ip.startsWith("192.168.")
                                || ip.startsWith("10.")
                                || ip.startsWith("172."))) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            PrintLog.w(TAG, "Could not enumerate network interfaces: " + e.getMessage());
        }
        return null;
    }

    private void notifyProgress(String message) {
        mainHandler.post(() -> {
            if (callback != null) callback.onProgress(message);
        });
    }

    private void notifyFound(DiscoveredPrinter printer) {
        mainHandler.post(() -> {
            if (callback != null) callback.onPrinterFound(printer);
        });
    }

    private void notifyError(String message) {
        mainHandler.post(() -> {
            if (callback != null) callback.onError(message);
        });
    }
}
