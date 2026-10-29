# FOLI firmware

[简体中文](README.zh-CN.md)

ESP32 / Arduino / FreeRTOS firmware for the supplied Mini-Printer hardware. This source retains the legacy five-byte control commands and four-byte status format used by the FOLI Android app. The integration revision has been compiled and host-tested; it has **not been flashed or physically print-tested** in this integration session.

## Build

Use Python 3.11, PlatformIO Core 6.2.0 and the pinned `espressif32@7.0.1` platform. The installed reference environment resolves Arduino-ESP32 **2.0.17** (`framework-arduinoespressif32 3.20017.241212+sha.dcc1105b`); the package's leading `3.` is a PlatformIO package version, not Arduino core 3.x.

```sh
python -m pip install platformio==6.2.0
pio run --project-dir software/firmware
```

Flash only after checking pinout and hardware compatibility: `pio run --project-dir software/firmware -t upload`. Uploading is a separate physical action, not part of CI. On Windows the ESP32 response-file toolchain may fail under non-ASCII paths; use an ASCII checkout path or a temporary drive mapping to the same directory. CI builds under Linux.

## Integration fixes

- A6 completion commands return immediately instead of entering the raster queue.
- Arbitrary BLE write fragments assemble into complete 48-byte rows. Partial final rows, empty jobs and capacity overflow reject the job at A6 instead of printing stale/truncated bytes.
- Queue enqueue/dequeue/clear/count operations use one mutex; dequeued rows copy into consumer-owned storage before the lock is released.
- The main printer loop yields so the priority-zero button task can run. FreeRTOS delays explicitly convert milliseconds to ticks.
- Printing checks paper/temperature before head power is enabled. The queued path refreshes sensors every 16 rows. Every printing exit shuts off VH and strobes, stops the motor and clears residual buffered data. Faults skip normal end-feed and shut down before blocking feedback.
- Negative battery percentages clamp before converting to the unsigned status byte. Numeric `START_PRINTER_WHEN_FINISH_RAED=0` now selects the alternate preprocessor branch correctly.
- Incoming writes while printing or after job completion is latched are ignored, preventing mutation of the queued job. Disconnection clears idle partial jobs; an active physical print is not a reliable remote-cancel mechanism.

## Protocol and limits

Each row is 384 monochrome dots / 48 bytes, MSB first. A5×4 plus density byte selects 30/60/100%; A6×4 plus one byte ends reception. Status bytes are battery percentage, integer temperature, paper (0 loaded / 1 absent), and state (0 init / 1 start / 2 working / 3 finished-or-stopped). The working enum is reserved by the legacy implementation; do not treat it as guaranteed progress reporting.

The queue holds 1000 rows. Android intentionally limits jobs to 768 rows. Legacy command prefixes are ambiguous if an arbitrary five-byte raster fragment exactly matches them; the Android sender uses full rows after MTU negotiation. There is no job identifier, CRC, positive row acknowledgement, transport retry protocol or separate error-completion state. `FINISH` therefore means stopped/returned, not proof of a successful physical print. Overflow rejection has no explicit wire error packet; the app must not blindly resend an uncertain job.

The watchdog remains a fixed 20-second print deadline. A timeout remains latched until restart; no throughput claim is made. The heat-compensation formula and pulse timings are retained pending physical characterization. NTC open/short detection and calibration are not established; a zero idle reading is not evidence of a valid 0°C measurement. Do not interpret these guards as a certified safety system.

## Verified interface constants

From `src/em_config.h`: key GPIO5; paper GPIO35; battery ADC GPIO34 / enable GPIO4; temperature GPIO36; indicator GPIO18; motor 23/22/21/19; LAT12 / SCK15 / data13; STB1–6 26/27/14/32/33/25; VH enable17. These are source configuration values, not a substitute for checking the final PCB and print mechanism before flashing. The source does not establish battery capacity or FPC pin count.

## Host regression tests

```sh
g++ -std=c++11 -Wall -Wextra -Werror -I software/firmware/src software/firmware/tests/protocol_test.cpp -o /tmp/foli-tests
/tmp/foli-tests
```

The tests exercise actual shared command/row-buffer code: A5/A6 routing, raster-prefix preservation, fragmented 20/48/244-byte writes, incomplete rows, atomic overflow rejection, clearing, copy-out independence and 10,000 wraparounds. They do not simulate the radio, MCU task scheduling, electrical outputs or print quality. The Firmware workflow runs these tests and compiles the ESP32 target.

Local verification on 2026-10-30: ESP32 build passed using PlatformIO 6.2.0 / platform 7.0.1; RAM 88,012 of 327,680 bytes, flash 1,151,101 of 1,310,720 bytes. The C++11 host executable compiled with Zig 0.13 and passed all regression cases. These are local results; remote CI status is reported separately. No flash/upload command was executed.

Only the configured receive-then-print mode (`START_PRINTER_WHEN_FINISH_RAED=1`) is supported by this integration. The alternate streaming branch is retained for reference; writes are gated while printing, so do not enable it as a supported mode.
