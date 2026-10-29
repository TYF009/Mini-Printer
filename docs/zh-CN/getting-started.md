# 开始使用 FOLI

[English](../en/getting-started.md) · [文档导航](index.md)

FOLI 包含原生 Android App、ESP32 固件、电池供电电路与可打印外壳。没有打印机也能创建纸条、修改文字和查看预览；实际打印需要配套硬件及固件。

## 先了解产品

1. 查看 [90 秒产品视频](../../media/video/FOLI-Made-Tangible-90s.mp4)与[三屏功能 GIF](../../media/gifs/app-tour.gif)。
2. 按 [Android 指南](../../software/android/README.md)获取或构建开发版 APK。
3. 在 **Templates → Today's three → Make this yours** 中应用模板，编辑文字、打开 **Layout** 并查看黑白纸面。草稿保存在手机本地。
4. 阅读[系统架构](architecture.md)，理解同一份纸面如何成为字节并进入打印机构。

App 是直接安装的开发版，尚非 Google Play 发布版本。最低运行系统为 Android 7/API 24；已记录的真机检查使用小米 6、Android 8/API 26，不代表所有系统版本均已验证。

## 构建 Android

安装 JDK 17、Android SDK Platform 32 及兼容构建工具，设置本机 SDK 路径，勿提交 `local.properties`。在 `software/android` 中执行：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
.\build.ps1
.\test-protocol.ps1
```

类 Unix 系统可用标准 wrapper：

```sh
cd software/android
chmod +x gradlew
./gradlew assembleDebug lintDebug
```

APK 输出为 `app/build/outputs/apk/debug/app-debug.apk`。当前构建和测试命令以 [Android 指南](../../software/android/README.md)为准。编译、协议检查与实物验证是不同层面的证据。

## 准备硬件与固件

将[最终嘉立创工程](../../hardware/EDA-project/foli-mainboard.epro2)、选定固件与[机械文件](assembly.md)作为配套版本使用。订购或接线前核对原理图、BOM 版本和连接器方向，不能仅凭 Mini-Printer 名称判断兼容。

用户指定电池出线为 XH 2.54 mm 2P，电池倍率为 5C；容量及准确型号尚未形成统一核实规格。历史资料的 1200/2000 mAh 示例不能直接当作当前产品参数。

在已安装 PlatformIO 的环境中，从仓库根目录执行：

```sh
cd software/firmware
pio run
pio run -t upload
pio device monitor
```

环境以 [platformio.ini](../../software/firmware/platformio.ini) 为准，自动识别失败时设置本机串口。固件可编译不代表该二进制已在实物运行；替换现有工作固件前查看[验证记录](verification.md)。

## 从手机连接

1. 装入打印纸、开启兼容打印机，关闭占用它的其他蓝牙 App。
2. 进入 **Device → Find your printer**，按系统要求授予权限并选择设备。
3. Android 12 及以上使用附近设备权限；Android 7–11 的 BLE 扫描要求定位权限与系统定位服务，App 本身不收集位置。
4. 查看设备上报的电量、纸张与打印头状态。未知值不能视为就绪；历史空闲 `0 °C` 读数仍未解决。
5. 编辑短纸条、查看预览并进入打印确认流程，传输时保持 App 前台运行。这是操作说明，不表示本轮仓库更新进行了新的实物打印验证。

传输中断或完成状态不确定时，遵循 App 的重启指引。重新连接不能证明设备缓冲已清空；旧协议没有能保证停止已启动打印的取消命令。

反馈异常时请记录 App 版本、固件提交、硬件版本、手机及系统、使用内容和最后观察到的状态，并参考[验证与已知边界](verification.md)。
