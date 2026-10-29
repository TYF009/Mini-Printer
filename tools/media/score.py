"""Original 120 BPM stereo score and synthesized action sounds, no samples."""
from pathlib import Path
import numpy as np,wave
B=Path(__file__).resolve().parents[2]/'media/source';(B/'work').mkdir(parents=True,exist_ok=True);SR=48000;D=90;rng=np.random.default_rng(416)
mix=np.zeros((SR*D,2),np.float32)
def add(at,s,amp=1,pan=0):
 i=int(at*SR)
 if i>=len(mix):return
 if s.ndim==1:s=s[:,None]*np.array([np.sqrt((1-pan)/2),np.sqrt((1+pan)/2)])[None,:]
 n=min(len(s),len(mix)-i);mix[i:i+n]+=s[:n]*amp
def freq(m):return 440*2**((m-69)/12)
def ep(m,d=1.8):
 t=np.arange(int(d*SR))/SR;f=freq(m)
 # Rounded electric piano: fundamental, tine harmonics, gentle detuning.
 a=np.sin(2*np.pi*f*t+.55*np.sin(2*np.pi*f*2*t)*np.exp(-t*7))
 a+=.23*np.sin(2*np.pi*f*2.002*t)*np.exp(-t*3)+.07*np.sin(2*np.pi*f*4*t)*np.exp(-t*6)
 return (a*np.exp(-t*2.3)*(1-np.exp(-t*190))*np.minimum(1,(d-t)/.08)).astype(np.float32)
def pad(notes,d=4.6):
 t=np.arange(int(d*SR))/SR;a=np.zeros(len(t))
 for m in notes:
  f=freq(m)
  for det in [.998,1.002]:a+=np.sin(2*np.pi*f*det*t)+.16*np.sin(2*np.pi*f*det*2*t)
 return (a/len(notes)*np.minimum(1,t/.6)*np.minimum(1,(d-t)/.9)).astype(np.float32)
def kick():
 t=np.arange(int(.32*SR))/SR;return (np.sin(2*np.pi*(47*t+2.2*(1-np.exp(-t*35))))*np.exp(-t*18)+.03*rng.normal(size=len(t))*np.exp(-t*160)).astype(np.float32)
def hat(d=.09):
 t=np.arange(int(d*SR))/SR;n=rng.normal(size=len(t));n=np.diff(n,prepend=0)
 return (n*np.exp(-t*65)).astype(np.float32)
def clap():
 t=np.arange(int(.17*SR))/SR;n=rng.normal(size=len(t));n=np.convolve(n,np.ones(5)/5,'same')
 return (n*(np.exp(-t*28)+.5*np.exp(-((t-.013)/.006)**2)+.3*np.exp(-((t-.027)/.008)**2))).astype(np.float32)
chords=[[62,66,69,73,76],[59,62,66,69,73],[55,59,62,66,69],[57,61,64,71,76]]
for bar in range(45):
 at=bar*2;ch=chords[(bar//2)%4];section=.75 if at<6 else 1
 if 50<=at<54:section=.20
 if at>=84:section=.6
 if bar%2==0:
  add(at,pad([m-12 for m in ch[:4]]),.042*section,-.35)
  for j,m in enumerate(ch):add(at+.025*j,ep(m),.055*section,(j-2)*.18)
 if at<84 and not 50<=at<54:
  for beat in [0,1,2,3]:
   if beat in (0,2):add(at+beat*.5,kick(),.29*section)
   if beat in (1,3):add(at+beat*.5,clap(),.14*section,.13)
  for j in range(8):add(at+j*.25+(0.018 if j%2 else 0),hat(),(.022 if j%2 else .031)*section,(-.45 if j%2 else .45))
  for offset,length in [(0,.37),(.75,.20),(1,.32),(1.75,.20)]:
   t=np.arange(int(length*SR))/SR;f=freq(ch[0]-24)
   a=(np.sin(2*np.pi*f*t)+.15*np.sin(2*np.pi*f*2*t))*np.minimum(1,t/.012)*np.minimum(1,(length-t)/.07)
   add(at+offset,a,.16*section)
 if 6<=at<50 or 70<=at<84:
  pattern=[0,2,4,2,1,3,2,1];m=ch[pattern[bar%8]%len(ch)]+12
  add(at+.5,ep(m,.8),.065,-.3);add(at+1.25,ep(ch[(bar+2)%len(ch)]+12,.65),.04,.35)
 if 54<=at<70:
  for j in range(4):add(at+j*.5,ep(ch[j%len(ch)]+12,.32),.025,(-.4+j*.25))
# Physical/interaction gestures, placed on the visual edit's action cues.
for at in [0,.5,1.5,4,12,14.5,15.5,16.3,18.5,19.8,21,24,26,28.2,29.2,30.5,31.5,34,38,42,46,47,48,54,58,62,66,70,72,74,78,79.5,81,82.5]:
 t=np.arange(int(.055*SR))/SR;n=rng.normal(size=len(t));click=(.5*n+.5*np.sin(2*np.pi*1250*t))*np.exp(-t*100)
 add(at,click,.035,.12)
for at in [2,5,13,33,36,40,44,50]:
 t=np.arange(int(.3*SR))/SR;n=rng.normal(size=len(t));n=np.convolve(n,np.ones(12)/12,'same')
 add(at,n*np.sin(np.pi*t/.3)**2,.11,-.12)
# Mechanism Foley is synthesized, deliberately not represented as a recording.
t=np.arange(int(2*SR))/SR;motor=(np.sin(2*np.pi*105*t)+.35*np.sin(2*np.pi*210*t))*(.65+.35*np.sin(2*np.pi*18*t))
add(31.7,motor*np.minimum(1,t/.12)*np.minimum(1,(2-t)/.2),.022)
for at in [6,18,34,54,70,84]:
 t=np.arange(int(.35*SR))/SR;n=rng.normal(size=len(t));n=np.convolve(n,np.ones(30)/30,'same');add(at,n*np.sin(np.pi*t/.35)**2,.14)
for j,m in enumerate([74,78,81]):add(85.5+j*.45,ep(m,3),.10,(-.25+j*.25))
# Short stereo room and rhythmic delay, then gentle bus saturation.
dry=mix.copy()
for delay,gain in [(.125,.10),(.25,.07),(.375,.04)]:
 n=int(delay*SR);mix[n:]+=dry[:-n,::-1]*gain
mix=np.tanh(mix*1.15);mix*=np.minimum(1,np.arange(len(mix))/SR/.025)[:,None];mix*=np.minimum(1,(len(mix)-np.arange(len(mix)))/SR/1.7)[:,None]
peak=np.max(np.abs(mix));mix*=.82/max(peak,.01)
with wave.open(str(B/'work/score-original.wav'),'wb') as w:w.setparams((2,2,SR,0,'NONE','not compressed'));w.writeframes((mix*32767).astype('<i2').tobytes())
print('Original stereo score rendered: 90 seconds',flush=True)
