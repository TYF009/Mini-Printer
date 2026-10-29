# Contributing / 参与贡献

Read [getting started](docs/en/getting-started.md) and [verification](docs/en/verification.md). Keep changes focused and describe affected app, firmware and hardware revisions.

1. Explain the problem and resulting behavior.
2. Coordinate Android/firmware protocol changes and document compatibility.
3. Run relevant checks: `python tools/check_docs.py`, `python tools/check_assets.py`, `python software/android/test-host.py`, Android build/Lint, or firmware build/host tests.
4. Update corresponding English/Chinese guidance and retain source notices.
5. State what was verified and what remains outstanding. Compiling firmware is not a physical printer test.

Do not commit SDKs, caches, local paths, signing keys, personal device logs or intermediate frames. Report potential vulnerabilities via [SECURITY.md](SECURITY.md).

中文：请先阅读[入门](docs/zh-CN/getting-started.md)与[验证](docs/zh-CN/verification.md)。提交写明版本、变化、检查结果和未验证范围；协议变更需考虑两端兼容，更新双语说明，保留来源。不提交密钥、个人日志、SDK、缓存或媒体中间帧。
