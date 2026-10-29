<p align="center"><img src="media/brand/foli-wordmark.svg" alt="FOLI" width="200" /></p>
<h1 align="center">Little prints. Everyday joy.</h1>
<p align="center">A little thought. Made tangible.</p>
<p align="center"><strong>English</strong> · <a href="README.zh-CN.md">简体中文</a></p>
<p align="center"><a href="releases/README.md">Get Android</a> · <a href="docs/en/getting-started.md">Get started</a> · <a href="docs/en/index.md">Explore the engineering</a></p>

[![Watch FOLI — Made Tangible](media/images/hero.jpg)](media/video/FOLI-Made-Tangible-90s.mp4)

**[Watch the 90-second film →](media/video/FOLI-Made-Tangible-90s.mp4)**

FOLI is a complete mini thermal-printer project: battery-powered ESP32 electronics, embedded control, a printable enclosure and a native Android companion. Together, they turn notes, lists and little messages into monochrome paper you can keep close.

A short list beside your keyboard. A few words tucked into a gift. A reminder for the day ahead.

*Lifestyle concept visuals, actual CAD geometry, reconstructed app interactions and illustrative paper-feed animation.*

## One little flow, three places

![Create, Templates and Device](media/gifs/app-tour.gif)

| Create | Templates | Device |
|---|---|---|
| Shape your words, choose a paper style and inspect the monochrome preview | Start with eight templates, filter, save and make one yours | Discover the printer, inspect reported state and find printing help |

No account is required. Editing and preview work offline; physical printing needs matching hardware. The tour combines actual captures with interaction visualization; Device is reconstructed from current source in the disconnected state.

[Download 0.2.1 development APK](releases/android/FOLI-0.2.1-debug.apk) · [Android source and build guide](software/android/README.md)

## From the screen to the paper

![Software-to-paper system architecture](media/images/system-architecture.png)

The Android renderer turns a layout into a 384-dot monochrome raster. Each row occupies 48 bytes. BLE carries row data to firmware that coordinates the thermal head and paper feed. The enclosure brings the battery, board, roll and mechanism together.

![Annotated CAD assembly](media/images/assembly-explained.jpg)

[Architecture and protocol](docs/en/architecture.md) · [Hardware](hardware/README.md) · [Mechanical and assembly](mechanical/README.md) · [Firmware](software/firmware/)

## Decisions behind the experience

- **Preview the raster that will be sent.** Preview and transmission share the encoder, so the paper view reflects monochrome output.
- **Design for an older phone.** Xiaomi Mi 6 checks informed keyboard layout, editor controls and explicit checklist rendering.
- **Separate connection, transfer and completion.** Interrupted or unconfirmed jobs use conservative recovery. Firmware hardening addresses command framing and fault shutdown paths.

[Read the engineering case study](docs/en/product-case-study.md) · [Verification and known limits](docs/en/verification.md)

## Built, wired, on paper

![Historical hardware and print-test photographs](media/images/hardware-evidence.jpg)

Physical hardware has been assembled and previously print-tested. Android 0.2.0 was inspected on Xiaomi Mi 6 for UI and BLE state. Version 0.2.1 adds BLE exception recovery with software checks. The hardened firmware and new app have not been revalidated together in a fresh physical print.

## Explore the build

| Your next step | Start here |
|---|---|
| Try the app without a printer | [Installation and usage](docs/en/getting-started.md) |
| Build Android | [JDK, SDK and build commands](software/android/README.md) |
| Build and flash firmware | [PlatformIO project](software/firmware/README.md) |
| Inspect or manufacture the board | [Final project, BOM and exports](hardware/README.md) |
| Make the enclosure | [STLs, source mapping and assembly](mechanical/README.md) |
| Reuse the product media | [Provenance and rebuild guide](media/README.md) |

## Open development

This repository retains the Mini-Printer history and integrates the FOLI Android app, product design, mechanical assets, presentation media and reliability work. [Sources and contributions](docs/en/provenance.md) distinguish the inherited platform from new implementation.

[Roadmap](ROADMAP.md) · [Contributing](CONTRIBUTING.md) · [Versions and downloads](releases/README.md) · [License](LICENSE) · [Third-party and media notices](NOTICE.md)
