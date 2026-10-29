# Hardware

[简体中文](README.zh-CN.md) · [Assembly](../docs/en/assembly.md)

The owner-selected final source is [foli-mainboard.epro2](EDA-project/foli-mainboard.epro2), a JLCEDA Professional project. Its SHA-256 matches the original upstream `MINI-PRINTER_JLC.epro2`; only the publication filename changed.

| File | Purpose |
|---|---|
| [Mainboard source](EDA-project/foli-mainboard.epro2) | Editable electrical/PCB project |
| [Gerber archive](exports/Gerber_Mini-printer.zip) | Existing manufacturing export |
| [BOM spreadsheet](exports/Mini-Printer-BOM.xlsx) | Existing parts list |
| [Schematic image](exports/schematic.png) | Readable reference schematic |

The existing exports are retained with the upstream project. Their export time and exact manufacturing/BOM correspondence have not been independently re-established in this integration; verify against the editable final source before placing an order. The assembly/reference BOM may include parts outside the PCB BOM.

The system uses ESP32 control, rechargeable battery power, charging/regulation, a print-head supply and motor/sensor interfaces. The owner specified an XH 2.54 mm 2-pin battery lead and 5C rating. Final battery capacity/model and mechanism FPC pin count require source/part reconciliation; no unverified marketing specification is published here.

Historical technical chapters remain under [hardware design notes](../docs/02-硬件设计/). Treat these as inherited design explanations; [current architecture](../docs/en/architecture.md) and final files define this integration.
