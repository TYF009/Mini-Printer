# 软硬件架构

## 硬件电源架构（两层板，网表核对）

```
Type-C 5V ──> ME4054 充电管理 ──> 锂电池 (VBAT)
                                    │
                                    ▼
                        Q2 AO4407A PMOS（软开关机，SW22）
                                    │ VBAT_OUT
                 ┌──────────────────┼──────────────────┐
                 ▼                                      ▼
     XC6210B332 LDO                        Q21 AO4407A PMOS（打印电源开关）
       3V3 → 数字电路                                │ 由 Q23(NMOS)←IO17 VH_EN 控制
                                                    ▼ VCC_IN
                                        AP2005 升压 + 4.7µH + SS54
                                                    │ VH（打印头高压）
                                                    ▼
                                            TC1508S 同时由 VBAT_OUT 供电
```

要点：
- 打印头电源两级 PMOS 控制，仅打印时上电，降低待机功耗
- VH 输出配 47µF×2 + 100nF×2 储能，支撑 6 路 STB 分时加热的峰值电流
- 下载电路：CH340C + 两颗 SS8050 组成 DTR/RTS 自动复位，Type-C 直插即可烧录

## 传感电路

- 电量：R6/R7 各 10k 分压（1:1）→ IO34 ADC，`analogReadMilliVolts()`（eFuse 校准）
- 温度：打印头内置 NTC，板上 R22 10k 上拉至 3V3 → IO36；B 值公式换算（B=3950）
- 缺纸：打印头传感器 → LM393 比较（阈值约 1V，R31/R36 分压）→ IO35 上升沿中断
- 按键：轻触开关 + RC 消抖（硬件）+ 10ms 轮询消抖（软件）双保险

## 软件模块划分

```
main.cpp        初始化、任务创建
em_task.cpp     打印业务状态机（启动条件、按键响应、状态上报）
em_device.cpp   设备状态结构（电量/温度/纸/打印状态）
utils/em_queue  环形打印缓冲（1000行×48B，FreeRTOS 互斥锁）
hal/em_ble      BLE 服务端、收发回调、A5/A6 协议解析
hal/em_printer  打印头驱动（SPI→LAT→STB 分时加热、点数补偿、密度）
hal/em_motor    四相八拍步进电机（Ticker 2ms / 同步步进）
hal/em_adc      电池/NTC 采样换算
hal/em_timer    10s 状态轮询、20s 打印超时
hal/em_button   按键状态机（消抖、长短按）
```

## 打印一行时序（em_printer.cpp）

1. 从环形缓冲取 48 字节，统计各 STB 通道黑点数 → 计算附加加热时间 addTime（正比点数² × 0.001）
2. SPI(1MHz) 移入 48 字节 → LAT 低脉冲锁存
3. 依次选通 STB1~6：加热 `PRINT_TIME + addTime[i] + STBx_ADDTIME`（2600µs 基准，按密度缩放）→ 断电冷却
4. 通道间隙穿插走纸脉冲（每 2 通道一步）
5. 数据打空后走纸 230 步收尾 → FINISH

## 异常保护

| 保护 | 触发 | 动作 |
|---|---|---|
| 缺纸 | GPIO35 上升沿中断 | 立即停止、清缓冲、LED 告警、BLE 上报 |
| 过热 | 温度 > 65℃ | 停止打印、上报 |
| 打印超时 | 20s 定时器到期 | 状态机复位、清缓冲、上报 |
