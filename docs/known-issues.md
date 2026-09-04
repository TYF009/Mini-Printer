# 已知问题与改进记录

静态代码审查发现的问题清单，按优先级排列。修复后将在此文件记录变更。

## 高优先级

1. **按键任务被饿死**：`printer_run()`（em_task.cpp）忙等循环中 `vTaskDelay(10)` 被注释，loopTask 优先级 1 空转，`task_button`（优先级 0）无法调度，单击测试/长按走纸失效。修复：恢复 yield 或改为事件驱动。
2. **A6 结束包被当作打印数据入队**（em_ble.cpp onWrite）：A6 分支处理后缺 `return`，5 字节命令包会作为一行打印且其后拼接环形缓冲残留旧数据。修复：补 return。
3. **低电量显示 100%**（em_hal.cpp read_battery）：`map(volts,3300,4200,0,100)` 在 <3.3V 时返回负数，经 uint8_t 回绕后仅被上限钳位，越亏电越显示满电。修复：映射后双向钳位。
4. **环形缓冲残留数据**：短包写入后行尾残留上一任务旧字节；BLE 重连/新任务开始时未清队列。修复：任务开始前清缓冲 + 写入时补零填充。

## 中优先级

5. 跨任务/ISR 共享变量（need_report、printer_timeout、printer_test 等）缺 `volatile`。
6. `clean_printbuffer()` 不持互斥锁，与 BLE 写入存在竞态。
7. `BEEP_MODE` 未定义、`PIN_BEEP` 与 `PIN_LED` 同为 IO18（板卡无独立蜂鸣器）：蜂鸣逻辑为死代码。
8. `run_led()`/`run_beep()` 内部 `delay(100)` 串联，最长阻塞约 0.5s。
9. 打印超时 20s 为任务开始时的一次性闹钟，满缓冲高密度整任务可能超时被误截断；建议改为"喂狗"式或按行数动态计算。
10. 缺纸中断触发后 `detachInterrupt`，重挂依赖最长 10s 的状态轮询，存在恢复盲区。

## 低优先级

11. `loop()` 内嵌 `for(;;)` 永不返回（与 #1 同源）。
12. `init_ble()` 在 `createServer()` 前误调一次 `startAdvertising()`。
13. `em_spi.h` 声明的 `spiCommand(byte)` 无定义。
14. 打印完成（FINISH）不上报 BLE，App 无法得知结束。
15. 温度 float→uint8_t 隐式截断。
16. `platformio.ini` 未锁定 espressif32 版本；`ledcSetup` 在 Arduino core 3.x 已移除。
17. IO4（固件中的采样使能脚）在此版硬件上未连接（网表核对），相关控制为无效操作。

## 待补验证

- 动态加热 vs 固定加热的整幅打印耗时对比（支撑效率提升结论）。
- 电量/温度增加滤波（滑动平均 / 中值）。
