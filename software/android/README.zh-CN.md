# FOLI Android

[English](README.md)

原生 Java Android 应用，支持文字、照片、8 个离线模板、纸面风格、真实黑白光栅预览与 BLE 传输。当前 0.2.1 为开发侧载版本，不是商店发行版。

## 构建

准备 JDK 17、Python 3、Android SDK Platform 32。Gradle 7.4 / AGP 7.3.1 已固定，Gradle 分发校验值来自官方校验文件。AGP 当前默认使用 Build Tools 30.0.3；CI 安装已验证的 30.0.3。设置 SDK 环境或本地 `local.properties`，后者不提交仓库。

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
.\build.ps1
python test-host.py
```

Linux/macOS：

```sh
chmod +x gradlew
./gradlew --no-daemon assembleDebug lintDebug
python3 test-host.py
```

输出 `app/build/outputs/apk/debug/app-debug.apk`。CI 执行构建、Lint、协议和 BLE 异常边界检查。158 条协议断言含逐字节检查，不代表 158 个独立集成场景。

## 连接与使用

可以离线编辑和预览。实物打印需要装纸并开机，在 Device 页发现设备，授权蓝牙，等待设备真实状态，再检查纸面并发送。保持应用前台；应用进入后台会终止发送/等待并要求检查设备。连接成功和传输完成均不等于实际打印成功。

Android 12 及以上需要附近设备权限；Android 7–11 的 BLE 扫描需要定位权限及系统定位服务。应用不采集定位，无网络权限、账号、分析或服务端。照片通过系统选择器读取，降采样副本保存在应用私有目录；备份和设备迁移已排除这些应用数据。

## 0.2.1 可靠性改动

异步 GATT 回调和读写操作在权限撤销或蓝牙状态变化时进入错误恢复，关闭连接并保留任务中断标记。即使断开调用失败，也会继续尝试关闭 GATT。纯 Java 测试覆盖该恢复边界与正常错误传播，不等于模拟了真实 Android 蓝牙环境。

历史真机检查对应小米 6 上的 0.2.0。0.2.1 改动仅完成本地构建、Lint 与主机测试；没有追加手机或打印机验证。完整 BLE 协议、兼容性限制与源码地图见英文文档。固件没有远程取消指令，历史固件存在 A6 入队、固定超时及状态上报问题；不能自动重试不确定的任务。

## 重要文件

- `app/src/main/java/studio/foli/android/`：界面、纸面渲染、协议与 BLE。
- `tests/`：纯 Java 编码与异常边界检查。
- `VERIFICATION.md`：已记录的历史验证与当前改动检查。
- `THIRD_PARTY_NOTICES.md`：依赖来源与许可说明。

## 旧协议终态语义

固件状态 3 表示完成或停止，缺纸、过热或超时也可能走到这个状态，不能据此宣布打印成功。应用现在显示“Printer stopped. Check your paper.”，保留任务不确定与重启恢复要求。即使名义正常终态也应检查实际纸张，并在下一任务前重启打印机后确认。缺少已观察到的开始状态时，单独终态不会识别为当前任务结束。持久化恢复通过显式 `PrintOutcome` 判定，不再解析界面文案。共享判定代码新增 7 个主机测试场景，未新增实物测试。
