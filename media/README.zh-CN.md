# 产品媒体

[English](README.md) · [来源说明](../docs/zh-CN/provenance.md)

images、gifs、video 为精选发布资产。视频为90秒、1080p/24fps、立体声；三维源帧1600×900缩放合成。三屏GIF为15秒、1560×1040。[manifest.json](manifest.json)记录分类与校验值。

source/assets保留生活概念图、精确提示词和纸面纹理；source/evidence保留原始照片；source/screens保留选定真机截图。声音由源码合成，无第三方音乐；系统字体不随仓库分发。

完整媒体重建需要Python3.11、[固定依赖](../tools/media/requirements.txt)、Blender4.2、FFmpeg/FFprobe，以及解压到[mechanical/cad](../mechanical/cad/README.md)的STEP。运行 `python tools/media/build.py --blender /path/to/blender`。输出为忽略提交的media/rendered，中间文件为media/source/work，不依赖旧工作目录或旧Blender工程。AI概念图作为已保存输入，不会自动重新生图。

字体可通过FOLI_FONT_REGULAR/BOLD/SERIF/SERIF_BOLD设置，支持系统字体查找。字体、渲染器与编码器版本差异可能改变画面，替换发布素材前需重新视觉检查。仅重建仓库图时可用--repo-assets-only，但需先运行三维管线得到帧。本工具链与App/固件构建独立，不需要API密钥。
