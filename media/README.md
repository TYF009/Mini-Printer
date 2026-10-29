# Product media

[简体中文](README.zh-CN.md) · [Provenance](../docs/en/provenance.md)

`images/`, `gifs/` and `video/` hold selected publication assets. The film is 90 seconds, 1920×1080/24fps, H.264/AAC stereo; CAD source frames were rendered at 1600×900 and scaled into the edit. The app tour is a 15-second, 1560×1040 GIF. [manifest.json](manifest.json) classifies each asset and records its checksum.

## Sources

`source/assets/` contains generated lifestyle concepts, their exact imagegen prompts and the monochrome paper texture. `source/evidence/` contains the original supplied photographs. `source/screens/` contains selected existing Android captures. Fonts and third-party music are not redistributed; music/Foley is generated from the included score script.

## Optional rebuild

Android and firmware builds do not require the media toolchain. To rebuild media, install Python 3.11, the pinned [Python requirements](../tools/media/requirements.txt), Blender 4.2, FFmpeg and FFprobe. Extract the complete assembly to [mechanical/cad](../mechanical/cad/README.md). Then run:

```sh
python -m pip install -r tools/media/requirements.txt
python tools/media/build.py --blender /path/to/blender
```

The pipeline derives meshes from STEP, creates the studio from those meshes, renders camera/assembly/paper motion, generates score/Foley, edits the film and creates repository graphics. Outputs go to ignored `media/rendered/`; intermediates go to ignored `media/source/work/`. It does not depend on an old Blender file or the external original-material folder. The AI concept images are retained inputs; rebuilding does not rerun image generation.

`FOLI_FONT_REGULAR`, `FOLI_FONT_BOLD`, `FOLI_FONT_SERIF` and `FOLI_FONT_SERIF_BOLD` can point to installed font files. Windows Segoe/Georgia and common Linux/macOS fonts are detected. Different fonts, Blender versions or encoder builds can alter pixels; rerun visual QA before replacing publication assets.

For repository graphics after CG frames exist: `python tools/media/build.py --repo-assets-only`. For a fresh checkout, the full CAD pipeline must run first. Rendering can be resource intensive. No video-generation SaaS or secret API key is required to replay the included deterministic pipeline.
