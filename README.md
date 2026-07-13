# Impresora Xerox WiFi — App Android

App Android para imprimir (y escanear en WorkCentre 3025) con impresoras Xerox Phaser 3020 / WorkCentre 3025 por Wi‑Fi local. Sin nube, sin cuentas, sin servidores externos.

> Esta app no está afiliada con Xerox Corporation.

## Descargar APK (sin compilar)

**Versión 1.1.0** — lista para instalar en tu celular:

| | |
|---|---|
| **Descarga directa** | [**Impresora-Xerox-WiFi-v1.1.0.apk**](https://github.com/waltervd9512/impresora-xerox-wifi/raw/main/releases/Impresora-Xerox-WiFi-v1.1.0.apk) |
| **Carpeta en el repo** | [`releases/`](https://github.com/waltervd9512/impresora-xerox-wifi/tree/main/releases) |
| **Android mínimo** | 8.0 (Oreo) |
| **Tamaño** | ~5.5 MB |

### Instalación rápida

1. Abrí el link de descarga **desde tu teléfono** (o pasá el APK por USB/WhatsApp/Drive).
2. Tocá el archivo descargado e instalá.
3. Si Android lo pide, activá **“Instalar apps desconocidas”** para el navegador o gestor de archivos que uses.
4. Abrí **Impresora Xerox WiFi** y configurá tu impresora (ver abajo).

> No hace falta compilar ni instalar Android Studio si solo querés usar la app.

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

## Configurar la impresora

1. Abrí **Impresora Xerox WiFi**
2. Elegí el **modelo** (Phaser 3020 o WorkCentre 3025)
3. Configurá la **IP**:
   - Manualmente, o
   - Con **Buscar impresora en la red**
4. Tocá **Probar conexión de red** — debe mostrar puerto IPP 631 OK
5. Andá a **Ajustes de Android → Impresión** y activá **Impresora Xerox WiFi**
6. Imprimí desde cualquier app → elegí tu impresora

## Compilar el APK (opcional)

Solo necesario si querés modificar el código o generar tu propia versión.

### Android Studio

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

## Cómo funciona

1. Android renderiza el documento a PDF
2. La app convierte cada página a bitmap 600 DPI
3. Se codifica en **URF** (formato que acepta la impresora)
4. Se envía por **IPP** (puerto 631) a la IP configurada

## Solución de problemas

| Problema | Solución |
|----------|----------|
| No puedo instalar el APK | Activá “orígenes desconocidos” en Ajustes |
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
