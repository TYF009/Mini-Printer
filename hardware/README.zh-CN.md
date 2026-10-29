# 硬件

[English](README.md) · [装配说明](../docs/zh-CN/assembly.md)

最终源文件为[foli-mainboard.epro2](EDA-project/foli-mainboard.epro2)，使用嘉立创 EDA 专业版。其 SHA-256 与原仓库 MINI-PRINTER_JLC.epro2 相同，本次仅规范文件名。

- [Gerber](exports/Gerber_Mini-printer.zip)
- [PCB BOM](exports/Mini-Printer-BOM.xlsx)
- [原理图](exports/schematic.png)

制造导出保留自上游，导出时间和与最终源文件的完全对应关系未在此次重新验证，下单前应对照可编辑工程核对。装配参考 BOM 与 PCB BOM 不是同一份清单。

系统包含 ESP32、电池、充电与稳压、打印头供电及电机/传感器接口。用户已确认电池线头为 XH 2.54 mm 2P、5C；最终容量/型号与 FPC 位数仍待按实物和最终文件统一，不发布猜测参数。

[历史硬件设计章节](../docs/02-硬件设计/)作为来源参考保留，当前入口为[系统架构](../docs/zh-CN/architecture.md)。
