from pathlib import Path
import subprocess,json,hashlib
from film import render_frame,contact
ROOT=Path(__file__).resolve().parents[2];B=ROOT/'media/source';OUT=ROOT/'media/rendered'
movie=OUT/'FOLI-Made-Tangible-90s.mp4'
def run(args):return subprocess.run(args,check=True,capture_output=True,text=True,encoding='utf-8',errors='replace')
info=run(['ffprobe','-v','error','-show_entries','format=duration,size:stream=codec_name,width,height,r_frame_rate,sample_rate,channels','-of','json',str(movie)])
decode=run(['ffmpeg','-v','error','-i',str(movie),'-f','null','-'])
loud=run(['ffmpeg','-hide_banner','-i',str(movie),'-af','loudnorm=I=-15:TP=-1:LRA=9:print_format=json','-f','null','-'])
tail=loud.stderr[loud.stderr.rfind('{'):];metrics=json.JSONDecoder().raw_decode(tail)[0]
report={'file':movie.name,'probe':json.loads(info.stdout),'full_decode':'passed','measured_audio':metrics,'sha256':hashlib.sha256(movie.read_bytes()).hexdigest()}
(OUT/'verification.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
for name,start,dur in [('foli-app-flow',18,12),('foli-screen-to-paper',30,4),('foli-inside',70,6)]:
 run(['ffmpeg','-v','error','-y','-ss',str(start),'-t',str(dur),'-i',str(movie),'-vf','fps=12,scale=720:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=160[p];[s1][p]paletteuse=dither=bayer','-loop','0',str(OUT/(name+'.gif'))])
render_frame(2.8).save(OUT/'foli-cover.jpg',quality=96)
render_frame(28.2).save(OUT/'foli-app.jpg',quality=96)
render_frame(73.5).save(OUT/'foli-engineering.jpg',quality=96)
contact()
run(['ffmpeg','-v','error','-y','-i',str(movie),'-vn','-c:a','pcm_s16le',str(OUT/'FOLI-original-score-and-foley.wav')])
print(json.dumps(report,indent=2),flush=True)
