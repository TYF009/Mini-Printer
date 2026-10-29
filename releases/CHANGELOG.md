# Changelog / 版本记录

## FOLI integration / Android 0.2.1

- Integrate native Android, mechanical sources and product media with existing board/firmware history.
- Harden Android asynchronous BLE permission/adapter failures and connection cleanup.
- Harden firmware A6 command handling, fragmented row assembly, queue ownership, task yield and fault shutdown.
- Add bilingual documentation, host regression checks and CI workflows.
- Pin firmware platform and verify the Gradle wrapper distribution checksum.

Build and host validation do not establish new physical printing or broad Android device compatibility. See component verification records.

### Earlier Android work

0.2.0 introduced the refined template/editor experience and was checked on Xiaomi Mi 6. 0.1.x records establish initial install/UI/BLE observations. Historical APKs and media studies are not included as current downloads.

中文：本次整合加入原生 App、机械和媒体，修复 Android BLE 异常恢复与固件命令/队列/故障停止问题，提供双语文档与自动检查。保留历史验证范围，不宣称新增实物打印成功。
