# FOLI for Android

**Little prints. Everyday joy.**

A native, English-first Android companion for the existing ESP32 Mini-Printer hardware. The app follows the approved FOLI visual direction: Chalk surfaces, Mint accents, original outline icons, and a paper-centered editor.

This is a **0.2.1 development build**. Build and static-check results are recorded in the release notes. Hardware validation is a separate requirement; no connected-printer test is implied by compilation.

[简体中文](README.zh-CN.md)

## Features implemented

- Create: note, photo and template entry points.
- Eight original editable templates, category filters, local favorites and full paper previews; Clean, Journal and Postcard styles, checklist/simple layouts, alignment and three type sizes.
- Focused editor with fixed back/undo and print controls; live bottom-sheet style previews, full-screen text composition, cancel-to-restore, session undo and full-screen zoom/pan inspection.
- Retained page scroll positions, local preview updates, debounced image processing, prewarmed thumbnail caching, native ripple feedback and reduced-motion-aware transitions.
- Photo import through Android's document picker; EXIF orientation handling, centered square/portrait crops, rotation, brightness, threshold or Floyd–Steinberg dithering.
- Real 384-dot monochrome preview generated from the exact bytes to be sent.
- Local draft persistence, including a downsampled copy of the selected photo in app-private storage.
- BLE discovery, connection, MTU negotiation, service validation, notification subscription and real device-state display.
- Three print densities, serialized acknowledged writes, transfer progress, device-state-based stop reporting, timeout handling and interrupted-job protection.
- No account, server, analytics or Internet permission. Printer compatibility is based on UUIDs and capabilities, not cosmetic device names alone.

There is no simulated connection, fake battery value, automatic retry, background-printing claim, or demo-mode success message.

## Requirements and build

- JDK 17.
- Android SDK Platform 32 and compatible build tools; Gradle 7.4 / Android Gradle Plugin 7.3.1 are pinned.
- Android 7.0 / API 24 or later at runtime, with BLE for physical printing.
- `local.properties` containing your SDK directory, e.g. `sdk.dir=E:/Android SDK`. Do not commit this machine-specific file.

Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
.\build.ps1
python test-host.py
```

`build.ps1` invokes the standard Gradle wrapper through Java directly to preserve Unicode Windows paths. It runs `assembleDebug` and `lintDebug`. The normal wrapper remains available on other systems:

```sh
chmod +x gradlew
./gradlew assembleDebug lintDebug
python3 test-host.py
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

This development build intentionally uses the existing installed API 32 toolchain. It is for direct-install development testing. A current compile/target SDK migration, Android-version validation and release signing are required before a store release. No private signing key belongs in the repository.

## Connect and print

1. Charge the compatible printer battery, install paper correctly and power on the printer.
2. Close the old printer app or any other BLE app connected to it.
3. In FOLI, open Device → Find your printer. Grant the requested runtime permission and select your printer.
4. Wait for actual paper, temperature and battery status before sending a job. Unknown data is shown as unknown.
5. Open a template or import a photo. Edit it, inspect Print preview, select density, then Send to printer.
6. Keep FOLI in the foreground and the phone near the printer. FOLI keeps the screen on during an active job.
7. “Sending” means BLE transfer. After an observed START/WORKING → state 3 transition, FOLI says “Printer stopped. Check your paper.” State 3 also represents some timeout/error stops, so the app never treats it as proof of physical success. Inspect the paper, power-cycle the printer and explicitly confirm that restart before sending another job.

Android 12+: Nearby devices permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`). Android 7–11: Android requires fine-location permission and enabled location services for BLE scanning; the app does not collect or transmit location. Photos use the system document picker without broad storage permission.

## Protocol and constraints

| Item | Value |
| --- | --- |
| Service UUID | `4fafc201-1fb5-459e-8fcc-c5c9c331914b` |
| Characteristic UUID | `beb5483e-36e1-4688-b7f5-ea07361b26a8` |
| Monochrome line | 384 pixels, 48 bytes, black = 1, MSB first |
| Density command | `A5 A5 A5 A5 01/02/03` for Light/Medium/Dark |
| Start after transfer | `A6 A6 A6 A6 01` |
| Status | battery, temperature, paper enum, print enum: exactly 4 bytes |
| Temperature sentinel | 255 = sensor unavailable in revised firmware; never displayed as 255 degrees |
| Paper enum | 0 = loaded, 1 = missing |
| Print enum | 0 = initial, 1 = start, 2 = working, 3 = finished or stopped (not success acknowledgement) |
| Transfer | Acknowledged GATT writes with at least 20 ms between packets |
| Minimum negotiated MTU | 51 (48-byte row plus ATT overhead) |
| App queue limit | 768 rows; excessive text is rejected, tall photos are fitted without truncating content |

MTU negotiation can be affected by other GATT clients and OS versions. FOLI checks the resulting size and rejects connections that cannot transmit a whole row. Acknowledged GATT writes establish transport acceptance, not application-level proof that every line was printed; the legacy protocol has no per-line sequence numbers or checksums.

Firmware compatibility and remaining limits:

- Older upstream firmware lets A6 fall through into the image queue and may append a stray/corrupted row. The hardened firmware in this repository returns after command handling and rejects partial/overflowed jobs; these fixes have build/host-test evidence, not a new physical print validation.
- The firmware uses a fixed 20-second print timeout. A 768-row buffer bound is not a promise that all dense prints will finish within that timer. Dense/long-image validation and an appropriate firmware change are required before release.
- Firmware state 3 means finished or stopped and does not distinguish all error causes. Even a nominal started-to-stopped transition remains unconfirmed. FOLI preserves the restart requirement and never declares physical print success from this packet. A terminal packet without an observed start also cannot identify the current job.
- There is no cancel command. Stopping the app's transfer does not stop a print that has already begun.
- After interrupted/unconfirmed transfers, the app blocks another job until the user power-cycles the printer and explicitly acknowledges that reset. Reconnecting alone does not clear the printer's queue.
- Older firmware battery calculation can produce invalid percentages. The hardened source clamps the computed value, while physical calibration and other sensor limits remain. The app validates packet shape/ranges but cannot calibrate hardware remotely.

## Source map

- `MainActivity.java`: native views, navigation, runtime permissions, document picker, editing and lifecycle.
- `BlePrinter.java`: main-looper-confined BLE connection and transfer state.
- `PaperRenderer.java`: note/photo layout and exact monochrome preview.
- `PrinterProtocol.java`: Android-independent raster encoding, commands and status validation.
- `FoliIcon.java`: original vector-style UI icon drawing.
- `TemplateCatalog.java`: original offline template content and categories.
- `ZoomPaperView.java`: inspection of the actual output bitmap without changing print data.
- `tests/ProtocolTest.java`: host-side regression checks without Android dependencies.

## Verification before release

Run the build, Lint and host tests, then validate a physical phone and printer. The important cases are: first launch without permissions, denial/regrant, Bluetooth off, scan/reconnect, image import and orientation, note editing, app recreation and draft recovery, preview/output agreement, densities, dense images, paper-out, hot-head status, interrupted transfer, restart recovery and completion reporting. Check layouts at small widths and enlarged font sizes.

This project does not reuse the old app's photo editor or BLE library; the old app and firmware were read to establish compatibility. AndroidX ExifInterface handles photo orientation. Gradle wrapper scripts/JAR retain their upstream provenance and license notices. All FOLI branding is a concept pending resolution of the previously recorded name collision.

Official API references: [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions), [BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt).

## Repository integration update — 0.2.1

BLE callbacks and queued read/write operations now recover when Bluetooth access changes, instead of allowing permission/adapter exceptions to crash the activity. Recovery closes the GATT session and retains interrupted-job protection. Closing a session attempts GATT close even if disconnect throws. `BleOperationTest` checks successful execution, access-revocation recovery, adapter-state recovery and propagation of programming errors; it is a host boundary test, not an Android radio simulation.

`test-host.py` runs on Windows, Linux and macOS with JDK 17 and Python 3. The original encoder suite reports 158 assertions, including byte-by-byte checks; this count does not mean 158 distinct integration scenarios. CI builds API 32 and runs build/Lint plus host checks. Existing physical UI checks describe 0.2.0; the 0.2.1 changes have not been revalidated on a phone or printer.

The wrapper distribution checksum is pinned to the official [Gradle 7.4 binary checksum](https://services.gradle.org/distributions/gradle-7.4-bin.zip.sha256).

### Legacy terminal-state correction

`PrintOutcome` is shared by the actual BLE receive path and host tests. A nominal terminal packet becomes `STOPPED_UNCONFIRMED`; paper-out, overheat or a previous failure cannot become success. An unstarted job remains pending. Interrupted/unconfirmed jobs retain their recovery requirement until an explicit printer-restart acknowledgement. `MainActivity` uses this typed outcome rather than parsing UI text to manage persisted uncertainty. Seven host scenarios cover this decision boundary; there is still no new physical verification.

### Unavailable print-head sensor

Revised firmware reserves temperature byte 255 for an unavailable/invalid sensor and refuses heating. Android displays Sensor unavailable, blocks printing, and treats the value during an active job as failure requiring inspection and restart rather than a cooldown. Legacy firmware does not implement this sentinel; its observed idle zero reading remains a historical limitation, not a confirmed valid sensor reading. Seven shared temperature helper cases and eight outcome cases cover the sentinel and stop semantics on the host; no sensor hardware test is implied.
