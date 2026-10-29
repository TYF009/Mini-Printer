# Downloads and version compatibility

[简体中文](README.zh-CN.md)

## Android 0.2.1 development build

[Download APK](android/FOLI-0.2.1-debug.apk) · [SHA-256](android/FOLI-0.2.1-debug.apk.sha256) · [Changes](CHANGELOG.md)

Requires Android 7/API 24 or later. This is a debug-signed sideload build, not a store release. Review [setup](../docs/en/getting-started.md) before using physical hardware. Development signatures can differ between builders; a signature mismatch may require uninstalling an old build, which removes its local data.

| Component | Integration identity | Validation |
|---|---|---|
| Mainboard | Supplied 2026-09-04 JLCEDA project; same bytes as upstream source | Historical hardware/printing evidence |
| Firmware | Pinned PlatformIO environment plus hardening in this branch | Compile + host tests; not flashed here |
| Android | 0.2.1 / code 4 | Build, Lint, host checks, signature; no new device run |
| Mechanical | Supplied 230918 assembly; 230727/230719 STLs | Source integrity, no new fit test |
| Presentation | 90-second FOLI film, three-screen app tour | Decode/stream/audio checks; visualizations labeled |

## Mechanical source bundle

The [0.2.1 development preview](https://github.com/TYF009/Mini-Printer/releases/tag/v0.2.1-preview.1) is published with the APK, film and full mechanical sources.

[Download FOLI-mechanical-source.zip](https://github.com/TYF009/Mini-Printer/releases/download/v0.2.1-preview.1/FOLI-mechanical-source.zip) · [Mechanical SHA-256](https://github.com/TYF009/Mini-Printer/releases/download/v0.2.1-preview.1/FOLI-mechanical-source.zip.sha256)

The bundle contains the complete original STEP, printable STLs and source mapping. It is not required for building Android or firmware. Its SHA-256 is `ddcff17953f4c73a32fa87a5d25a1ede443b001c99b25fda2ba5865e2f13f47b`.

The repository does not treat older physical printing as proof of a fresh end-to-end print with these software revisions. [Verification scope](../docs/en/verification.md) is part of the version record.
