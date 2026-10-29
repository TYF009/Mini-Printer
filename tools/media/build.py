"""Run the optional media pipeline without relying on an old working directory."""
from pathlib import Path
import argparse,subprocess,sys,shutil,os
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
p=argparse.ArgumentParser();p.add_argument('--blender',default=os.getenv('BLENDER','blender'));p.add_argument('--graphics-only',action='store_true');p.add_argument('--repo-assets-only',action='store_true');a=p.parse_args()
def py(name):subprocess.run([sys.executable,str(HERE/name)],cwd=ROOT,check=True)
def blend(name):subprocess.run([a.blender,'--background','--factory-startup','--python',str(HERE/name)],cwd=ROOT,check=True)
work=ROOT/'media/source/work';work.mkdir(parents=True,exist_ok=True)
if a.repo_assets_only:py('build_repo_assets.py');sys.exit()
if not (ROOT/'mechanical/cad/assembly.step').is_file():p.error('Extract the mechanical source bundle to mechanical/cad/assembly.step first.')
if not shutil.which('ffmpeg') or not shutil.which('ffprobe'):p.error('Install FFmpeg and FFprobe on PATH.')
py('export_mesh.py');blend('create_base_scene.py');blend('studio.py')
sys.path.insert(0,str(HERE));from film import note
note(journal=True).convert('L').convert('RGB').save(ROOT/'media/source/assets/thermal-note.png')
blend('print_motion.py')
if not a.graphics_only:py('score.py');py('film.py');py('finish.py')
py('build_repo_assets.py')
