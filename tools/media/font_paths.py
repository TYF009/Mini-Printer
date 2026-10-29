"""Resolve installed fonts without redistributing proprietary font binaries."""
from pathlib import Path
import os
def resolve_font(bold=False,serif=False):
 key='FOLI_FONT_'+('SERIF_BOLD' if serif and bold else 'SERIF' if serif else 'BOLD' if bold else 'REGULAR')
 candidates=[os.getenv(key)]
 if serif:
  candidates+=['C:/Windows/Fonts/georgiab.ttf' if bold else 'C:/Windows/Fonts/georgia.ttf','/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf' if bold else '/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf']
 else:
  candidates+=['C:/Windows/Fonts/segoeuib.ttf' if bold else 'C:/Windows/Fonts/segoeui.ttf','/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf' if bold else '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf','/System/Library/Fonts/Supplemental/Arial Bold.ttf' if bold else '/System/Library/Fonts/Supplemental/Arial.ttf']
 for p in candidates:
  if p and Path(p).is_file():return str(p)
 raise FileNotFoundError('Install a font or set '+key+' to its font file path.')
