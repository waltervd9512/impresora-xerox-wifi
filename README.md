# Impresora Xerox WiFi — App Android

App Android para imprimir (y escanear en WorkCentre 3025) con impresoras Xerox Phaser 3020 / WorkCentre 3025 por Wi‑Fi local. Sin nube, sin cuentas, sin servidores externos.

> Esta app no está afiliada con Xerox Corporation.

## Características

- Imprimir desde cualquier app de Android (fotos, PDFs, documentos)
- Buscar impresora automáticamente en la red Wi‑Fi
- Configuración manual de IP
- Pruebas de conexión y página de prueba
- Historial de trabajos y logs de depuración
- Escaneo (solo WorkCentre 3025)
- 100% local — sin internet ni telemetría

## Requisitos

- Android 8.0 (Oreo) o superior
- Impresora Xerox Phaser 3020 o WorkCentre 3025 en la **misma red Wi‑Fi** que el teléfono
- IP fija o reservada para la impresora (recomendado)

## Descargar APK (sin compilar)

**Versión 1.1.0** — lista para instalar:

[**Descargar Impresora-Xerox-WiFi-v1.1.0.apk**](https://github.com/waltervd9512/impresora-xerox-wifi/raw/main/releases/Impresora-Xerox-WiFi-v1.1.0.apk)

1. Descargá el APK en tu teléfono
2. Abrilo e instalá (permití “orígenes desconocidos” si Android lo pide)
3. Seguí los pasos de configuración más abajo

## Compilar el APK

### Android Studio (recomendado)

1. Instalá [Android Studio](https://developer.android.com/studio)
2. Abrí esta carpeta como proyecto
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`

### Línea de comandos

```powershell
cd ruta/al/proyecto
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

## Instalar en el teléfono

1. Copiá el APK al teléfono
2. Abrí el archivo e instalá (permití “orígenes desconocidos” si lo pide)
3. Abrí **Impresora Xerox WiFi**
4. Configurá:
   - **Modelo:** Xerox Phaser 3020 (o WorkCentre 3025)
   - **IP:** manualmente o con **Buscar impresora en la red**
5. Tocá **Probar conexión de red** — debe mostrar puerto IPP 631 abierto

## Activar el servicio de impresión

1. **Ajustes de Android** → buscá **Impresión**
2. Activá **Impresora Xerox WiFi**
3. Desde cualquier app: **Imprimir** → elegí tu impresora

## Cómo funciona

1. Android renderiza el documento a PDF
2. La app convierte cada página a bitmap 600 DPI
3. Se codifica en **URF** (formato que acepta la impresora)
4. Se envía por **IPP** (puerto 631) a la IP configurada

## Solución de problemas

| Problema | Solución |
|----------|----------|
| No aparece la impresora | Activá el plugin en Ajustes → Impresión |
| No encuentra la IP | Verificá misma red Wi‑Fi; probá IP manual |
| Falla la conexión | Revisá que la impresora esté encendida |
| No imprime nada | Probá página de prueba; revisá logs en la app |

## Privacidad

- Sin conexión a internet
- Sin analytics ni telemetría
- Solo accede a la red local para llegar a la impresora

## Créditos

Basado en [android-print-plugin-xerox-workcenter-3025-wifi](https://github.com/pirvu/android-print-plugin-xerox-workcenter-3025-wifi) por [pirvu](https://github.com/pirvu) (MIT).

## Licencia

[MIT](LICENSE)
