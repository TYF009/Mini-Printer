"""90-second deterministic edit: CAD motion, interaction visualization, diagrams."""
from pathlib import Path
from functools import lru_cache
import math,sys,subprocess,json
import numpy as np
from PIL import Image,ImageDraw,ImageFont,ImageFilter,ImageEnhance
from font_paths import resolve_font
ROOT=Path(__file__).resolve().parents[2];B=ROOT/'media/source';OUT=ROOT/'media/rendered';OUT.mkdir(exist_ok=True)
W,H,FPS=1920,1080,24
BG='#F4F1E8';INK='#213C35';MINT='#C9E8D7';MUTED='#61736A';PINK='#F0D9CB';BLUE='#D8E8F2'
def ease(t):t=max(0,min(1,t));return 1-(1-t)**3
def smooth(t):t=max(0,min(1,t));return t*t*(3-2*t)
@lru_cache(None)
def font(n,b=False,serif=False):return ImageFont.truetype(resolve_font(b,serif),int(n))
def text(im,s,xy,n=48,c=INK,b=False,serif=False):ImageDraw.Draw(im).text(xy,s,font=font(n,b,serif),fill=c)
def center(im,s,y,n=48,c=INK,b=False):
 d=ImageDraw.Draw(im);bb=d.textbbox((0,0),s,font=font(n,b));text(im,s,((W-bb[2])/2,y),n,c,b)
def rr(im,box,fill,r=30,outline=None,width=2):ImageDraw.Draw(im).rounded_rectangle(tuple(int(v) for v in box),radius=r,fill=fill,outline=outline,width=width)
def canvas(color=BG):return Image.new('RGB',(W,H),color)
def label(im,s='FOLI',dark=False):text(im,s,(95,52),24,BG if dark else INK,True)
def footer(im,s='Product & interaction visualization',dark=False):text(im,s,(95,1015),20,'#9EB8AC' if dark else MUTED)
def heading(im,a,b='',t=1,dark=False):
 color=BG if dark else INK;dy=int(20*(1-ease(t/.35)));text(im,a,(110,240+dy),76,color,True)
 if b:
  for j,line in enumerate(b.split('\n')):text(im,line,(115,355+dy+j*56),35,'#B6CDBE' if dark else MUTED)
@lru_cache(None)
def screenshot(name):
 p=B/'screens/android-v020'/f'{name}.png';im=Image.open(p).convert('RGB');return im.crop((0,65,1080,1920)).resize((510,876),Image.Resampling.LANCZOS)
@lru_cache(None)
def photo(name):return Image.open(B/'assets'/f'{name}.png').convert('RGB').resize((W,H),Image.Resampling.LANCZOS)
def photo_motion(name,t,d=4):
 im=photo(name);z=1+.035*smooth(t/d);ww=int(W/z);hh=int(H/z);x=int((W-ww)*(.4+.1*t/d));y=int((H-hh)*.5)
 return im.crop((x,y,x+ww,y+hh)).resize((W,H),Image.Resampling.LANCZOS)
@lru_cache(None)
def note(kind='focus',journal=False):
 im=Image.new('RGB',(620,650),'#FFFFFF');d=ImageDraw.Draw(im)
 title={'focus':"Today's three.",'kindness':'Just for you.','travel':'A little list.'}[kind]
 text(im,'EVERYDAY, ON PAPER' if journal else 'A LITTLE FOCUS',(46,40),19,INK,True)
 text(im,title,(46,100),43,INK,True,journal);d.line((46,175,574,175),fill=INK,width=3)
 lines={'focus':['Finish something good','Take a proper break','Make time to create'],'kindness':['You make ordinary days','a little brighter.','','I’m glad you’re here.'],'travel':['Keys and wallet','Phone and charger','Room for adventure']}[kind]
 for j,s in enumerate(lines):
  y=220+j*67
  if kind!='kindness':d.rectangle((46,y+8,65,y+27),outline=INK,width=2)
  text(im,s,(84 if kind!='kindness' else 46,y),26,INK,False,journal)
 text(im,'Made with FOLI',(46,590),19,INK);return im
def paste_card(im,card,x,y,width=520,angle=0,shadow=True):
 h=int(card.height*width/card.width);card=card.resize((int(width),h),Image.Resampling.LANCZOS).convert('RGBA')
 if angle:card=card.rotate(angle,Image.Resampling.BICUBIC,expand=True)
 if shadow:
  sh=Image.new('RGBA',(card.width+70,card.height+70));sh.paste((25,40,30,40),(35,35),card.getchannel('A'));sh=sh.filter(ImageFilter.GaussianBlur(16));im.paste(sh,(int(x)-20,int(y)-10),sh)
 im.paste(card,(int(x),int(y)),card)
def phone(im,screen,x=1230,y=88,width=500):
 h=int(width*876/510);rr(im,(x-11,y-11,x+width+11,y+h+11),INK,43)
 screen=screen.resize((width,h),Image.Resampling.LANCZOS);mask=Image.new('L',screen.size);ImageDraw.Draw(mask).rounded_rectangle((0,0,width,h),radius=31,fill=255);im.paste(screen,(int(x),int(y)),mask)
def touch(screen,x,y,t,at):
 u=t-at
 if 0<=u<.45:
  d=ImageDraw.Draw(screen);r=int(13+27*u/.45);d.ellipse((x-r,y-r,x+r,y+r),outline='#578E77',width=3)
def editor(journal=False):
 im=screenshot('journal_editor' if journal else 'editor_final').copy()
 # Remove transient snackbar from the historical capture by taking matching final UI.
 if not journal:im.paste(screenshot('journal_editor').crop((0,645,510,876)),(0,645))
 return im
def sheet_slide(base,target,p,top=170):
 # Exact captured panel content, moved independently of the dimmed underlying UI.
 im=base.copy();shade=Image.new('RGB',im.size,'#333B34');im=Image.blend(im,shade,.2*p)
 pan=target.crop((0,top,510,876));im.paste(pan,(0,int(876-(876-top)*p)));return im
def cg(kind,index):
 im=Image.open(B/'work/cg'/f'{kind}-{index:03}.png').convert('RGB');return im.resize((W,H),Image.Resampling.LANCZOS)
def pill(im,s,xy,active=True):
 x,y=xy;d=ImageDraw.Draw(im);ww=d.textbbox((0,0),s,font=font(25,True))[2]+44;rr(im,(x,y,x+ww,y+52),INK if active else MINT,26);text(im,s,(x+22,y+8),25,BG if active else INK,True)
def title_panel(im,title,sub='',dark=False):
 # Cream panel makes lifestyle headlines readable while preserving the photograph.
 overlay=Image.new('RGBA',(W,H));d=ImageDraw.Draw(overlay)
 for x in range(850):d.line((x,0,x,H),fill=(244,241,232,int(235*(1-x/850)**.4)))
 im.paste(overlay,(0,0),overlay);label(im);heading(im,title,sub)
def progress(im,i):
 for j,s in enumerate(['Choose','Make it yours','Preview']):
  x=112+j*210;rr(im,(x,800,x+180,850),MINT if j<=i else '#E6E5DA',25);text(im,s,(x+18,810),22,INK,j==i)
def render_frame(t):
 # 0–6: instant cause/effect and a warm lifestyle payoff.
 if t<2:
  im=canvas(MINT);label(im);heading(im,'A little thought.','One small thing worth keeping.',t)
  paste_card(im,note(),1110-80*ease(t/.45),175,580,-3)
  if t>.5:
   d=ImageDraw.Draw(im);p=ease((t-.5)/.22);x,y=1074,385;d.line((x,y,x+12*p,y+12*p),fill=INK,width=5)
 elif t<4:
  u=t-2;im=photo_motion('morning',u,2);title_panel(im,'Made tangible.','From your screen.\nInto your day.');paste_card(im,note(),230,600-50*ease(u/.3),340,-7)
 elif t<6:
  im=canvas();label(im);center(im,'Little prints.',260,116,b=True);center(im,'Everyday joy.',405,116,b=True)
  for j,s in enumerate(['Choose.','Create.','Keep.']):
   if t>=4+j*.5:pill(im,s,(590+j*255,660),j==int((t-4)*2)%3)
 elif t<10:
  u=t-6;im=cg('hero',min(95,int(u*24)));label(im);text(im,'Meet FOLI.',(110,135),82,INK,True);footer(im,'CAD product visualization')
 elif t<12:
  u=t-10;im=canvas(MINT);label(im);heading(im,'Your next little idea.','Starts right here.',u);phone(im,screenshot('final_gallery'),1200+int(120*(1-ease(u/.4))));footer(im,'Interaction visualization · FOLI for Android')
 elif t<15:
  u=t-12;im=cg('load',min(71,int(u*24)));label(im);text(im,'01  Load your paper.',(110,120),60,INK,True);footer(im,'Illustrative assembly motion · 57 mm thermal paper')
 elif t<18:
  u=t-15;im=canvas();label(im);heading(im,'02  Connect.','FOLI for Android\nBluetooth Low Energy',u)
  # Conceptual link explains preparation without inventing a scan result.
  rr(im,(1060,225,1260,660),INK,34);rr(im,(1080,250,1240,630),MINT,20);text(im,'FOLI',(1105,410),32,INK,True)
  rr(im,(1510,390,1770,620),MINT,45);rr(im,(1540,430,1740,458),INK,10)
  d=ImageDraw.Draw(im)
  for j in range(3):
   x=1315+j*55;r=7+4*math.sin(u*5-j);d.ellipse((x-r,445-r,x+r,445+r),fill=INK)
  text(im,'PHONE',(1095,710),23);text(im,'PRINTER',(1570,710),23);footer(im,'Connection diagram · no live device telemetry')
 elif t<24:
  u=t-18;im=canvas();label(im);heading(im,'A little inspiration.','Pick a template.\nMake it personal.',u);progress(im,0)
  screen=screenshot('final_gallery').copy()
  if u>=1.4:screen=sheet_slide(screen,screenshot('template_preview'),ease((u-1.4)/.28))
  if u>=3.25:
   p=ease((u-3.25)/.38);screen=Image.blend(screen,editor(),p)
  touch(screen,130,410,u,1.1);touch(screen,330,815,u,3.05)
  phone(im,screen);footer(im,'Interaction visualization · actual interface captures')
 elif t<27:
  u=t-24;im=canvas();label(im);heading(im,'Your words.','A little more you.',u);progress(im,1)
  screen=editor();panel=Image.new('RGB',(510,876),'#FCFCF8');d=ImageDraw.Draw(panel)
  text(panel,'Edit your words',(35,38),30,INK,True);text(panel,'TITLE',(35,135),13,MUTED,True);rr(panel,(28,170,482,235),'#EEF0E6',12)
  title="Today's three.";text(panel,title[:min(len(title),int(max(0,u-.3)*16))],(42,181),24)
  text(panel,'YOUR NOTE',(35,285),13,MUTED,True);rr(panel,(28,320,482,610),'#EEF0E6',14)
  for j,line in enumerate(['Finish something good','Take a proper break','Make time to create']):text(panel,line,(42,345+j*66),23)
  rr(panel,(28,770,482,837),INK,28);text(panel,'Keep changes',(165,786),25,BG,True)
  if u<.3:screen=Image.blend(screen,panel,ease(u/.3))
  elif u<2.25:screen=panel
  else:screen=Image.blend(panel,editor(),ease((u-2.25)/.3))
  phone(im,screen);footer(im,'Interaction visualization · reconstructed text editor')
 elif t<30:
  u=t-27;im=canvas();label(im);heading(im,'Find your paper’s style.','Clean. Journal. Postcard.',u);progress(im,2)
  base=editor();target=screenshot('journal_live');p=ease(u/.25) if u<2 else 1-ease((u-2)/.28)
  screen=sheet_slide(editor(u>=2),target,p)
  touch(screen,255,610,u,.85);touch(screen,310,811,u,1.8)
  phone(im,screen);footer(im,'Interaction visualization · actual paper style panel')
 elif t<34:
  u=t-30;im=canvas(MINT);label(im);heading(im,'From screen to paper.','Preview. Connect. Print.',u)
  if u<1.5:
   screen=editor(True);touch(screen,260,825,u,.5);phone(im,screen,1180,88)
  else:
   im=cg('print',min(59,int((u-1.5)*24)));label(im);text(im,'Made tangible.',(110,120),68,INK,True)
  footer(im,'Print workflow visualization · no new physical print test')
 elif t<46:
  k=int((t-34)//4);u=(t-34)%4;names=['morning','kindness','travel'];titles=['A little focus.','A little kindness.','A little less to forget.'];subs=['Three things. A fresh start.','Say the little things that matter.','Ready for the day ahead.'];kind=['focus','kindness','travel'][k]
  im=photo_motion(names[k],u);title_panel(im,titles[k],subs[k]);paste_card(im,note(kind,True),155,520+int(25*(1-ease(u/.35))),350,-5+2*smooth(u/4));footer(im,'Lifestyle concept visualization')
 elif t<50:
  u=t-46;k=int(u*2)%3;im=photo_motion(['morning','kindness','travel'][k],u/2,4)
  shade=Image.new('RGB',(W,H),INK);im=Image.blend(im,shade,.23);center(im,['A little focus.','A little kindness.','A little freedom.'][k],440,88,BG,True);footer(im,'Lifestyle concept visualization',True)
 elif t<54:
  u=t-50;im=canvas(INK);label(im,dark=True)
  if u<2:
   center(im,'Behind every little print.',410,88,BG,True);center(im,'A whole system working together.',545,34,MINT)
  else:
   d=ImageDraw.Draw(im);p=ease((u-2)/1.8)
   for y in range(9):
    for x in range(21):
     xx=240+x*72;yy=230+y*72;r=int(4+20*p);c=MINT if ((x*7+y*3)%11)<6 else '#36594C';d.ellipse((xx-r,yy-r,xx+r,yy+r),fill=c)
   center(im,'Let’s look closer.',900,40,BG)
 elif t<62:
  u=t-54;im=canvas();label(im,'FOLI / HOW IT WORKS');text(im,'From layout to dots.',(110,135),76,INK,True)
  # Same actual paper crop used to derive an exact 384-column monochrome raster.
  crop=screenshot('journal_editor').crop((58,181,451,486)).resize((384,300),Image.Resampling.LANCZOS).convert('L');a=np.array(crop)<128
  if u<3:
   paste_card(im,note(journal=True),170,310,460)
   d=ImageDraw.Draw(im);d.line((735,570,880,570),fill=INK,width=4);text(im,'1-bit raster',(1000,322),36,INK,True)
   raster=Image.fromarray(np.where(a,30,255).astype('uint8')).convert('RGB').resize((650,508),Image.Resampling.NEAREST);im.paste(raster,(1000,400));y=int((u/3)*508);d=ImageDraw.Draw(im);d.line((1000,400+y,1650,400+y),fill='#88B79A',width=5)
  else:
   d=ImageDraw.Draw(im);text(im,'384 dots across',(112,290),42,INK,True);text(im,'8 dots per byte',(112,360),30,MUTED);text(im,'48 bytes per row',(112,415),30,MUTED)
   row=a[83];cell=35;xx=110;yy=560
   for j in range(48):
    x=xx+(j%24)*70;y=yy+(j//24)*100;rr(im,(x,y,x+54,y+70),MINT if j<min(48,int((u-3)*18)) else '#E5E5D9',8);text(im,f'{sum(int(row[j*8+k])<<(7-k) for k in range(8)):02X}',(x+7,y+18),23)
   text(im,'One highlighted row → 48 packed bytes',(112,790),34,INK,True);text(im,'384 ÷ 8 = 48',(112,865),52,INK,True)
  footer(im,'Raster encoding based on PrinterProtocol.java')
 elif t<70:
  u=t-62;im=canvas(INK);label(im,'FOLI / THE PRINT PIPELINE',True);text(im,'Across Bluetooth. Onto paper.',(110,135),72,BG,True)
  boxes=[(110,'ANDROID','Layout + raster'),(565,'BLE','Row data'),(1020,'FIRMWARE','Drive + feed'),(1475,'PAPER','Thermal dots')]
  d=ImageDraw.Draw(im)
  for j,(x,a,b) in enumerate(boxes):
   on=u>j*.6;rr(im,(x,385,x+335,650),MINT if on else '#36594C',32);text(im,a,(x+25,445),32,INK if on else BG,True);text(im,b,(x+25,510),25,INK if on else MINT)
   if j<3:
    d.line((x+350,515,x+430,515),fill='#7CA48E',width=3)
    p=(u*.7-j*.17)%1;px=x+350+80*p;d.ellipse((px-7,508,px+7,522),fill=MINT)
  text(im,'A row at a time. A little thought, made tangible.',(110,770),38,BG)
  if u>4:
   for j in range(70):
    x=112+j*24;h=18+14*math.sin(u*6+j*.7);d.line((x,900,x,900-h),fill=MINT,width=9)
  footer(im,'Conceptual data flow · no simulated telemetry',True)
 elif t<76:
  u=t-70;im=cg('inside',min(143,int(u*24)));label(im,'FOLI / INSIDE THE PRODUCT');text(im,'Thoughtful, inside out.',(110,110),65,INK,True)
  footer(im,'CAD assembly visualization · enclosure / power / control / print')
 elif t<78:
  u=t-76;im=canvas();label(im,'FOLI / ENCLOSURE');heading(im,'Built around the system.','A printable shell.\nA complete assembly.',u)
  d=ImageDraw.Draw(im)
  for j in range(28):
   y=720-j*10;p=min(1,max(0,u*1.2-j*.022));w=int(520*p);d.rounded_rectangle((1100-w//2,y,1500+w//2,y+6),radius=3,fill='#7BA58D' if j%4 else INK)
  footer(im,'Illustration of additive layers · enclosure source included')
 elif t<84:
  u=t-78;im=canvas(INK);label(im,'FOLI / EXPLORE THE BUILD',True);text(im,'Every layer. One product.',(110,135),76,BG,True)
  names=['Hardware','Firmware','Enclosure','Android'];files=['嘉立创 design → PCB','Control → thermal print','STEP / STL → assembly','Java → native Android']
  # English copy is kept font-safe; original source is shown in the detail window.
  files[0]='Schematic → PCB'
  for j,s in enumerate(names):
   x=110+j*450;p=ease((u-j*.25)/.35);y=int(340+60*(1-p));rr(im,(x,y,x+410,y+180),MINT if int(u)%4==j else '#36594C',24);text(im,s,(x+28,y+35),40,INK if int(u)%4==j else BG,True);text(im,files[j],(x+28,y+110),23,INK if int(u)%4==j else MINT)
  source=['WIDTH = 384;','ROW_BYTES = 48;','rows[y][x/8] |= (0x80 >> (x%8));','A real layout. A reproducible build.']
  for j,s in enumerate(source):
   if u>1+j*.4:text(im,s,(145,635+j*62),30,MINT)
  footer(im,'Source files, hardware design and enclosure assets',True)
 else:
  u=t-84;im=photo_motion('morning',u,6);title_panel(im,'FOLI','Little prints. Everyday joy.')
  if u>.6:text(im,'Explore the build.',(115,540),38,INK,True)
  if u>1.1:text(im,'github.com/TYF009/Mini-Printer',(115,615),29,INK)
  footer(im,'Product & lifestyle visualization · Android interaction demonstration')
 # Crisp cuts except a 6-frame warm fade at the bookends.
 if t<.17:im=Image.blend(canvas(),im,ease(t/.17))
 if t>89.5:im=Image.blend(im,canvas(),smooth((t-89.5)/.5))
 return im
def contact():
 times=[.8,2.8,6.8,10.8,13.3,16.2,19,21.8,24.9,28.2,32.8,35.5,39.5,43.5,48,51,56,59,65,73,77,80,86]
 sheet=Image.new('RGB',(4*480,math.ceil(len(times)/4)*300),BG)
 for j,t in enumerate(times):
  im=render_frame(t);im.resize((480,270),Image.Resampling.LANCZOS);sheet.paste(im.resize((480,270)),((j%4)*480,(j//4)*300));ImageDraw.Draw(sheet).text(((j%4)*480+12,(j//4)*300+273),f'{t:04.1f}s',font=font(18),fill=INK)
 sheet.save(B/'work/contact-sheet.jpg',quality=93)
def build():
 p=subprocess.Popen(['ffmpeg','-v','error','-y','-f','rawvideo','-pixel_format','rgb24','-video_size',f'{W}x{H}','-framerate',str(FPS),'-i','-','-an','-c:v','libx264','-preset','fast','-crf','18','-pix_fmt','yuv420p',str(B/'work/picture.mp4')],stdin=subprocess.PIPE)
 for f in range(90*FPS):
  p.stdin.write(render_frame(f/FPS).tobytes())
  if f%240==0:print('Edited',f//FPS,'/ 90 seconds',flush=True)
 p.stdin.close();assert p.wait()==0
 subprocess.run(['ffmpeg','-v','error','-y','-i',str(B/'work/picture.mp4'),'-i',str(B/'work/score-original.wav'),'-c:v','copy','-af','loudnorm=I=-15:TP=-1.6:LRA=9','-ar','48000','-c:a','aac','-b:a','256k','-t','90','-movflags','+faststart',str(OUT/'FOLI-Made-Tangible-90s.mp4')],check=True)
 print('Complete',flush=True)
if __name__=='__main__':
 if '--contact' in sys.argv:contact()
 elif '--frame' in sys.argv:
  t=float(sys.argv[-1]);render_frame(t).save(B/'work/frame-check.png')
 else:build()
