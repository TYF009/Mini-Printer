# Verification and remaining limits

[简体中文](../zh-CN/verification.md) · [Documentation](index.md)

Evidence is grouped by what it actually establishes. An attractive render is not hardware evidence, a successful compile is not a radio test, and historical printing does not qualify every newer software revision.

## Recorded baseline

| Area | Recorded evidence | What it does not establish |
|---|---|---|
| Hardware | Supplied board/mechanism photographs and a physical thermal test page | Production yield, endurance, battery capacity or new app compatibility |
| Android 0.2.0 | Build and Lint; 158 protocol assertions; APK signature; installation and scoped UI checks | Every photo path, lifecycle path, Android version or physical print |
| UI device | Xiaomi Mi 6, Android 8/API 26 | General performance/FPS or broad accessibility qualification |
| BLE baseline | Connected state and received four-byte packet; battery about 75–76%; missing paper matched the empty printer | Battery calibration, reliable head temperature or physical output |
| Repository hardening | Build and automated checks recorded with the changed source | Hardware qualification of new firmware fixes |
| Product media | CAD motion, reconstructed UI, AI lifestyle concepts and original synthesized sound | Live-action use, measured interaction smoothness or a new print test |

The [detailed Android record](../../software/android/VERIFICATION.md) contains chronological observations and limitations. Its earliest table describes the original build handoff; read the subsequent physical-phone and 0.2.0 sections as well.

![Historical hardware evidence](../../media/images/hardware-evidence.jpg)

The photographed test page reads `TM_Test: 24 °C`. Do not reuse an older README's 28 °C figure for this image without another supporting record.

## What was observed on the phone

The 0.2.0 gallery, Focus filtering, template preview/application, Clean-to-Journal live preview, Keep changes, Undo, local favorites and saved filtering were exercised. The full-screen composer kept Cancel/Done above the keyboard; cancelling a temporary title edit restored the prior content. Full-screen paper inspection opened successfully. The scoped final process-log sample contained no fatal exception.

One startup raster duration is a single observation, not a sustained-performance benchmark. Multi-touch behavior, every photo import/transform, enlarged fonts, accessibility services, process recreation and newer Android releases still require broader checks.

## Unresolved physical integration

The connected empty printer reported `0 °C`. The owner suggested idle temperature updates may explain this; that is a hypothesis, not a confirmed sensor result. Do not interpret zero as a calibrated safe temperature. No print was sent during that connection observation.

The historical protocol lacks job identifiers, row acknowledgments/checksums and a guaranteed cancel command. Interrupted transfers can leave buffered data. Old firmware completion/timeout behavior and A6 fall-through are known limitations. Changes addressing those issues must retain separate build/host-test evidence and physical-device follow-up status.

No new end-to-end print with the FOLI 0.2.0 app or repository-hardening firmware was performed in this work session. The owner chose not to repeat hardware testing. That decision does not prevent source release, but it limits compatibility claims.

## Reproduction record for future checks

Record the hardware revision, firmware commit, App version/hash, phone/API level, battery/paper condition and exact input. For each case record expected behavior, observed behavior, logs/photos and pass/fail/uncertain result. Keep these distinct:

- Build and static analysis.
- Host-side encoder/protocol tests.
- Phone UI and permission/discovery behavior.
- Actual print quality and preview/output agreement.
- Missing paper, hot-head, low battery and interrupted-job recovery.

A future test should update the matching source revision's record rather than retroactively marking older work as fully tested.

## Integration software checks

Android 0.2.1 passed build/Lint (zero errors, one dependency-version advisory), 158 encoder assertions, BLE exception-boundary checks, seven shared terminal-outcome cases and APK v2 signature verification. State 3 is treated as stopped/unconfirmed, never proof of physical success; confirmation after a printer restart clears the recovery latch.

Firmware compiled with PlatformIO Core 6.2.0, espressif32 7.0.1 and Arduino-ESP32 2.0.17. Shared C++ regression tests covered command routing, six fragment sizes, row completion, overflow, copy-out independence and 10,000 ring-buffer wraparounds. RAM: 88,012 / 327,680 bytes; flash: 1,151,101 / 1,310,720 bytes. These are build sizes, not runtime performance measurements.

No firmware was flashed and no new physical print was attempted. Legacy timing/sensor calibration and the new fault shutdown behavior still require physical follow-up.
