# MINI PRINTER — 便携式蓝牙热敏打印机

电池供电的小型蓝牙热敏打印机：手机通过 BLE 发送单色位图，ESP32 流式缓存并逐行驱动热敏打印头打印。硬件为两层板，锂电池供电，集成充电、升压、电机驱动与多种传感器监测。

> 项目性质：个人复刻/学习项目。在原始设计（V4 引脚版本硬件）基础上完成软硬件联调、代码阅读梳理与文档编写，并在 [已知问题](docs/known-issues.md) 中记录后续改进项。

## 目录结构（硬件 / 软件 / 项目整合 三大部分）

```
mini-printer/
├── hardware/                  # ① 硬件部分
│   ├── EDA-project/           #    嘉立创EDA专业版工程（.epro2，可直接打开编辑）
│   ├── exports/               #    导出的原理图 PDF / Gerber / BOM（待补，见下方说明）
│   └── docs/                  #    电源架构、电路模块说明（见 docs/，硬件相关文档）
├── software/                  # ② 软件部分
│   └── firmware/              #    PlatformIO 工程（ESP32 + Arduino + FreeRTOS）
├── docs/                      # ③ 项目整合
│   ├── architecture.md        #    软硬件整体架构与数据流
│   ├── pinout.md              #    引脚分配表（硬件网表与固件逐脚核对版）
│   ├── ble-protocol.md        #    BLE 应用层协议
│   └── known-issues.md        #    已知问题与改进记录
└── README.md
```

## 系统概览

- **主控**：ESP32-S 模组（板载天线，BLE 4.2）
- **打印头**：384 点/行热敏打印头，6 路 STB 分时加热，1mm 间距 32Pin FPC 连接
- **走纸**：两相步进电机 + TC1508S 双 H 桥驱动
- **供电**：单节锂电池（Type-C 充电，ME4054），PMOS 软开关机，打印时按需升压至 VH（AP2005），3.3V 由 LDO（XC6210B332）提供
- **监测**：电池电量（分压+ADC）、打印头温度（NTC + B 值公式）、缺纸检测（LM393 比较器 + GPIO 中断）

### 数据流

手机 App --BLE 写特征值--> ESP32 `onWrite` 回调 --> 环形打印缓冲（1000 行 × 48 字节，互斥锁保护）--> FreeRTOS 打印任务状态机 --> SPI 送数 → LAT 锁存 → 6 路 STB 分时加热 + 走纸脉冲 --> 打印完成 / 异常保护（缺纸 / 过热 / 20s 超时）。

### 软件任务模型

| 任务/中断 | 优先级 | 周期/触发 | 职责 |
|---|---|---|---|
| 打印主任务（loop） | 1 | 常驻 | 打印状态机、打印头时序驱动 |
| 上报任务 | 1 | 100ms | 10s 周期采集电量/温度并 BLE 上报；缺纸事件处理 |
| 按键任务 | 0 | 10ms | 软件消抖、短按（自检测试）/长按（走纸） |
| Ticker 定时器 | — | 2ms / 10s / 20s | 电机步进节拍 / 状态刷新 / 打印超时 |
| GPIO 中断 | — | 上升沿 | 缺纸检测（LM393 输出） |

## 编译与烧录

```bash
cd software/firmware
pio run                # 编译
pio run -t upload      # 烧录（Type-C 直插，板载 CH340C + 自动下载电路，无需手按 BOOT）
pio device monitor     # 串口监视
```

环境：PlatformIO，`[env:esp32dev]`，Arduino 框架（注意：`ledcSetup` 等接口依赖 Arduino-ESP32 2.x，3.x 已移除，`platformio.ini` 建议锁定 espressif32 平台版本）。

## 待补内容（上传后陆续完善）

- [ ] 实物照片 / 打印效果照片（docs/images/）
- [ ] 原理图 PDF、Gerber、BOM 导出（hardware/exports/，在嘉立创 EDA 中打开 .epro2 导出）
- [ ] 已知问题修复（docs/known-issues.md 中的高优先级三项）
- [ ] 打印耗时实测数据（验证动态加热收益）

## 致谢

本项目在原始开源/商用设计基础上复刻完成，仅用于个人学习。如有侵权请联系删除。
