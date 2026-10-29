# Get started with FOLI

[简体中文](../zh-CN/getting-started.md) · [Documentation](index.md)

FOLI combines a native Android app, ESP32 firmware, battery-powered electronics and a printable enclosure. You can create and inspect a paper without owning the printer. Physical printing requires the matching hardware and firmware.

## Explore in five minutes

1. Watch the [90-second product film](../../media/video/FOLI-Made-Tangible-90s.mp4).
2. See [Create, Templates and Device](../../media/gifs/app-tour.gif).
3. Open the [Android guide](../../software/android/README.md) for the available build and installation instructions.
4. Choose **Templates → Today's three → Make this yours**. Edit the words, explore **Layout**, and inspect the monochrome paper preview. Drafts are stored on the phone.
5. Read the [system architecture](architecture.md) to follow the same paper into raster bytes and the print mechanism.

The app is a development build for direct installation, not a Google Play release. Android 7.0/API 24 is the minimum runtime version. The recorded physical-phone checks used Xiaomi Mi 6, Android 8.0/API 26; they do not establish compatibility with every Android version.

## Build the Android app

Install JDK 17 and Android SDK Platform 32 with compatible build tools. Configure your local SDK path; do not commit `local.properties`. From `software/android`:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
.\build.ps1
.\test-protocol.ps1
```

On a Unix-like system the standard wrapper is available:

```sh
cd software/android
chmod +x gradlew
./gradlew assembleDebug lintDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`. The [Android guide](../../software/android/README.md) is authoritative for current build and test commands. Build success and protocol tests are separate from hardware verification.

## Prepare the hardware

Use the [final JLCEDA source](../../hardware/EDA-project/foli-mainboard.epro2), the selected firmware and the [enclosure files](assembly.md) as one versioned set. Read existing hardware notes before ordering or wiring parts. Confirm component/BOM revision and connector orientation against the board; similar Mini-Printer names alone do not establish compatibility.

The owner specified an XH 2.54 mm 2-pin battery lead and a 5C battery rating. Capacity and exact cell model have not been consolidated into a final verified specification. Do not substitute the historical 1200/2000 mAh examples as a current product claim.

## Build and flash firmware

From the repository root, with PlatformIO installed:

```sh
cd software/firmware
pio run
pio run -t upload
pio device monitor
```

Check the selected environment in [platformio.ini](../../software/firmware/platformio.ini). Set the upload port for your machine if detection fails. A successful firmware build does not mean that the resulting binary has been exercised on the physical printer. Consult [verification](verification.md) before replacing a previously working device image.

## Connect from Android

1. Install paper, power on the compatible printer and close other apps connected to it.
2. Open **Device → Find your printer**. Grant the permission requested by Android and select the device.
3. On Android 12+, BLE uses Nearby devices permissions. On Android 7–11, discovery requires location permission and enabled system location services; the app does not collect location.
4. Inspect reported battery, paper and print-head state. Unknown values are not readiness confirmation. The historical idle `0 °C` observation remains unresolved.
5. Create a short note, inspect its preview and enter the print review flow. Keep FOLI foregrounded during transfer. This is an operating procedure, not a claim that a new print was validated in this repository update.

After an interrupted or unconfirmed transfer, follow the app's restart guidance. Reconnecting is not proof that the device queue was cleared. The legacy protocol does not offer a cancel command that can guarantee stopping a print already in progress.

For unexpected output, record app version, firmware commit, hardware revision, phone/Android version, the content used and the last observed state. See [verification and open limits](verification.md).
