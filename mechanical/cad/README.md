# Optional assembly input / 可选装配输入

Extract `assembly.step` from the mechanical-source release bundle into this directory. It is intentionally ignored by Git because the supplied file is large; the printable STLs remain in the repository. The exact original file is `迷你打印机装配体230918.STEP`.

Checksums and download availability are listed in [release downloads](../../releases/README.md). The optional media pipeline reads this file and derives meshes without changing it. A release bundle must be available before advertising a one-click complete CAD download.

中文：完整 STEP 从机械源文件发布包获取并放在本目录，文件名为 assembly.step。Git 不直接追踪此大文件，STL 已在仓库。下载可用性与校验值见发布说明，不能把尚未发布的下载写成已上线。
