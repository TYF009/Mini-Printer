from pathlib import Path
import sys,subprocess,json
from PIL import Image,ImageDraw
import numpy as np
ROOT=Path(__file__).resolve().parents[2];B=ROOT/'media/rendered';B.mkdir(parents=True,exist_ok=True);OUT=B

from film import canvas,text,rr,font,screenshot,editor,sheet_slide,ease,cg,INK,BG,MINT,MUTED,BLUE,photo_motion,render_frame
W,H=1560,1040
def old(name):
 im=Image.open(ROOT/'media/source/screens/android-v011'/f'{name}.png').convert('RGB');return im.crop((0,65,1080,1920)).resize((510,876),Image.Resampling.LANCZOS)
def device():
 # Current 0.2.0 page rebuilt from MainActivity.device(); deliberately disconnected.
 im=Image.new('RGB',(510,876),'#FCFCF8');text(im,'Your FOLI',(28,24),34,INK,True);text(im,'A little connection. A world of little ideas.',(28,78),19,MUTED)
 rr(im,(25,130,485,305),MINT,25);text(im,"Let’s find your FOLI.",(48,151),26,INK,True);text(im,'Your paper companion',(48,190),18,MUTED)
 rr(im,(46,232,464,285),INK,23);text(im,'Find your printer',(157,243),20,BG,True)
 text(im,'REPORTED BY YOUR PRINTER',(28,335),14,MUTED,True)
 for j,(name,value) in enumerate([('Battery','—'),('Paper','Unknown'),('Print head','—')]):
  y=375+j*95;rr(im,(25,y,485,y+75),'#EEF0E6',20);text(im,name,(47,y+22),24);bb=ImageDraw.Draw(im).textbbox((0,0),value,font=font(23,True));text(im,value,(458-bb[2],y+23),23,INK,True)
 text(im,'Not connected',(30,680),22,MUTED);text(im,'Printing help  →',(30,735),21,INK,True)
 for j,name in enumerate(['Create','Templates','Device']):text(im,name,(38+j*165,833),18,INK,j==2)
 return im
def frame(t):
 im=Image.new('RGB',(W,H),BG);text(im,'FOLI for Android',(65,28),39,INK,True);text(im,'Three places. One little flow.',(65,86),24,MUTED)
 phase=min(2,int(t/5));u=t-phase*5
 screens=[old('home-final'),screenshot('final_gallery').copy(),device()]
 captions=['Create a note. Shape the paper.','Pick a template. Make it yours.','Find your printer. Understand its state.']
 if phase==0:
  if u>=.65:screens[0]=Image.blend(screens[0],editor(),ease((u-.65)/.3))
  if u>=2:screens[0]=sheet_slide(editor(),screenshot('journal_live'),ease((u-2)/.28))
  if u>=3.7:screens[0]=sheet_slide(editor(True),screenshot('journal_live'),1-ease((u-3.7)/.28))
 if phase==1:
  if u>=.7:screens[1]=sheet_slide(screens[1],screenshot('template_preview'),ease((u-.7)/.28))
  if u>=3:screens[1]=Image.blend(screens[1],editor(),ease((u-3)/.3))
 for j,(s,name) in enumerate(zip(screens,['01  Create','02  Templates','03  Device'])):
  x=110+j*495;sy=210;sw=350;sh=601
  rr(im,(x-28,sy-66,x+sw+28,sy+sh+108),MINT if phase==j else '#EAE9DF',28)
  text(im,name,(x-5,sy-53),26,INK,True)
  rr(im,(x-8,sy-8,x+sw+8,sy+sh+8),INK,32)
  sc=s.resize((sw,sh),Image.Resampling.LANCZOS);mask=Image.new('L',sc.size);ImageDraw.Draw(mask).rounded_rectangle((0,0,sw,sh),radius=24,fill=255);im.paste(sc,(x,sy),mask)
  steps=[['Compose','Style','Preview'],['Browse','Preview','Apply'],['Discover','Reported state','Help']][j]
  for k,word in enumerate(steps):
   yy=sy+sh+25+k*23;text(im,word,(x+5,yy),16,INK if j==phase else MUTED,j==phase and k==min(2,int(u/1.6)))
  if phase==j:
   d=ImageDraw.Draw(im)
   if j==2:
    # Feature highlights, not fabricated scan results or readings.
    box=[(25,130,485,305),(25,375,485,640),(25,715,485,780)][min(2,int(u/1.65))]
    x1,y1,x2,y2=box;d.rounded_rectangle((x+x1*sw/510-4,sy+y1*sh/876-4,x+x2*sw/510+4,sy+y2*sh/876+4),radius=16,outline='#579678',width=3)
   else:
    at=[.5,1.9,3.55][min(2,int(u/1.6))]
    if 0<=u-at<.4:
     xx=x+sw*.6;yy=sy+sh*.7;r=int(10+20*(u-at)/.4);d.ellipse((xx-r,yy-r,xx+r,yy+r),outline='#579678',width=3)
 text(im,captions[phase],(75,950),29,INK,True)
 text(im,'Interaction visualization · Device page reconstructed from current source · Disconnected state',(75,1004),16,MUTED)
 return im
def save_static():
 frame(.1).save(OUT/'foli-app-three-screens.png')
 # Data chain, grouped at a useful level for a reviewer.
 im=Image.new('RGB',(1920,1080),BG);text(im,'From a little idea to a real print.',(95,80),64,INK,True);text(im,'One product. Connected across software, electronics and mechanics.',(100,175),31,MUTED)
 nodes=[('ANDROID','Compose + preview','PaperRenderer.java','PrinterProtocol.java'),('BLE','Transfer row data','48 bytes per row','384 monochrome dots'),('FIRMWARE','Buffer + drive','Thermal-head control','Motor and sensor logic'),('HARDWARE','Make it tangible','Thermal mechanism','Power + paper feed')]
 d=ImageDraw.Draw(im)
 for j,(a,b,c,e) in enumerate(nodes):
  x=95+j*455;rr(im,(x,360,x+365,710),MINT if j%2==0 else BLUE,28);text(im,a,(x+28,397),31,INK,True);text(im,b,(x+28,465),26,INK,True);text(im,c,(x+28,550),22);text(im,e,(x+28,600),22)
  if j<3:d.line((x+380,535,x+435,535),fill=INK,width=3);d.polygon([(x+435,535),(x+425,529),(x+425,541)],fill=INK)
 rr(im,(95,805,1825,955),INK,24);text(im,'384 dots ÷ 8 bits = 48 bytes per raster row',(135,833),38,BG,True);text(im,'The enclosure brings the board, battery, roll and print mechanism together.',(135,892),26,MINT)
 text(im,'Conceptual architecture · protocol width verified against Android source',(100,1010),20,MUTED);im.save(OUT/'foli-system-architecture.png')
 # Clearly separated authentic historical photographs.
 im=Image.new('RGB',(1920,1150),BG);text(im,'Built. Wired. On paper.',(90,60),66,INK,True);text(im,'Existing hardware and print-test photographs',(95,155),30,MUTED)
 for j,(fn,title) in enumerate([('hardware-test-bench.jpg','Board + battery + print mechanism'),('print-test-page.jpg','Historical thermal test output')]):
  p=Image.open(ROOT/'media/source/evidence'/fn).convert('RGB').transpose(Image.Transpose.ROTATE_180);p.thumbnail((840,650),Image.Resampling.LANCZOS);x=95+j*900;im.paste(p,(x,260));text(im,title,(x,945),31,INK,True)
 text(im,'Historical hardware evidence · not a new end-to-end print test with FOLI 0.2.0',(95,1065),23,MUTED);im.save(OUT/'foli-hardware-evidence.jpg',quality=94)
 # True CAD geometry with restrained callouts; no unlabeled conceptual internals.
 im=cg('inside',143);d=ImageDraw.Draw(im);text(im,'Inside FOLI',(85,65),62,INK,True)
 for title,xy,point in [('Upper cover',(95,255),(995,285)),('Enclosure body',(1430,440),(1135,560)),('Battery',(90,650),(550,630)),('Control board',(100,880),(780,810))]:
  x,y=xy;px,py=point;text(im,title,xy,30,INK,True);d.line((x+20,y+45,px,py),fill=INK,width=2);d.ellipse((px-5,py-5,px+5,py+5),fill=INK)
 text(im,'Actual STEP assembly · illustrative separation · finish study',(85,1010),22,MUTED);im.save(OUT/'foli-assembly-explained.jpg',quality=95)
 # Film cover reused as coherent README hero; no new product styling needed.
 render_frame(2.8).save(OUT/'foli-film-cover.jpg',quality=95)
def build():
 save_static();p=subprocess.Popen(['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r','12','-i','-','-an','-c:v','libx264','-crf','18','-pix_fmt','yuv420p','-movflags','+faststart',str(OUT/'foli-app-tour.mp4')],stdin=subprocess.PIPE)
 for f in range(180):p.stdin.write(frame(f/12).tobytes())
 p.stdin.close();assert p.wait()==0
 subprocess.run(['ffmpeg','-v','error','-y','-i',str(OUT/'foli-app-tour.mp4'),'-vf','split[a][b];[a]palettegen=max_colors=192[p];[b][p]paletteuse=dither=bayer','-loop','0',str(OUT/'foli-app-tour.gif')],check=True)
 print('Repo media exported',flush=True)
if __name__=='__main__':build()
