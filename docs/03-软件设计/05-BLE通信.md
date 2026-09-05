# 03-软件设计 · 05 · BLE 蓝牙通信

## 1. 需求与选型

把手机上待打印的图像传到打印机：近距离无线通信，选 **BLE**（低功耗蓝牙）而非经典蓝牙（BT）：

| | 经典蓝牙 BT | 低功耗蓝牙 BLE |
|---|---|---|
| 定位 | 大数据量传输（音频、音乐） | 低成本、低功耗、实时性好的小数据场景（智能家居、传感上报、外设） |
| 物理层 | 与 BLE 调制方式不同 | — |

注意：**BT 与 BLE 物理层不兼容，两者之间不能互相通信**——所以 App 端和固件端都必须是 BLE。

## 2. 角色：服务器与客户端（概念纠错）

**Mini 打印机的板卡是 BLE 服务器（Server/外围设备 Peripheral），手机是客户端（Client/中心设备 Central）**：板卡对外提供"打印服务"，手机扫描、连接、使用该服务。（不要说"打印机初始化为主机"——主机/从机是链路层角色，BLE 外设=从机+服务器，中心=主机+客户端，两套说法不能混。）

## 3. BLE 核心概念：Service / Characteristic / UUID

- 一个 BLE 设备可包含多个 **Service（服务）**，一个 Service 下有多个 **Characteristic（特征）**
- 每个 Service / Characteristic 都有一个 **128bit UUID** 标识
- Service 是功能集合；**Characteristic 才是设备间交互的实际载体**，其 Property（读/写/订阅）决定客户端能对这个特征做什么

类比：打印机厂家 ↔ BLE 设备；4S 店 ↔ Service；店内具体车型 ↔ Characteristic；顾客 ↔ 手机 App——顾客对设备的操作，本质是对具体车型（特征）的操作。

本项目用到的：1 个 Service（UUID `4fafc201-1fb5-459e-8fcc-c5c9c331914b`）+ 1 个 Characteristic（`beb5483e-36e1-4688-b7f5-ea07361b26a8`，权限 Read/Write/WriteNoRsp/Notify，挂 BLE2902 描述符支持 Notify 订阅）。

## 4. 建立通信的流程

```
初始化设备名 "Mini-Printer" → 初始化 Server → 创建 Service → 创建 Characteristic
→ 注册连接/写入回调 → 开始广播
手机（LightBlue / 自研App）扫描 → 连接 → 读写/订阅特征 → 双向通信
```

- **下行（写）**：App 写特征值触发 onWrite 回调——普通数据包入打印缓冲，A5×4+档位 设浓度、A6×4 结束标志（完整协议见 docs/ble-protocol.md）
- **上行（Notify）**：设备周期上报 4 字节状态（电量/温度/纸/打印状态）；学习版实验为每 5s 上报一条，整机固件为 10s 周期 + 事件触发
- **断连处理**：onDisconnect 回调里重新 `startAdvertising()`（连接建立后广播自动停止，不重开就再也搜不到）

## 5. 调试工具

手机装 **LightBlue**（iOS/Android 均有）：搜索 "Mini-Printer" → 连接 → 查看服务/特征结构 → 手动写数据、订阅 Notify 验证收发。没有 App 开发能力也能先调通固件侧协议。

## 6. 防丢包设计（对应项目亮点）

- 链路层：GATT 写操作由 BLE 协议栈保证可靠传输（链路层重传）
- 应用层：**A6 结束标志包**保证"数据收全才开打"；**20s 打印超时**兜底链路中断；缓冲写满丢新行 + 拿锁超时放弃，保证 BLE 回调不被阻塞（回调阻塞才是真正的丢包源）
