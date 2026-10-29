import bpy,bmesh,math
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];B=ROOT/'media/source'
bpy.ops.wm.open_mainfile(filepath=str(B/'work/base.blend'))
s=bpy.context.scene;s.render.resolution_x=1600;s.render.resolution_y=900;s.render.resolution_percentage=100;s.render.fps=24;s.eevee.taa_render_samples=32
s.world.node_tree.nodes['Background'].inputs['Strength'].default_value=.55
s.world.node_tree.nodes['Background'].inputs['Color'].default_value=(.9,.86,.76,1)
s.view_settings.view_transform='AgX';s.view_settings.look='AgX - Medium High Contrast'
parts=[]
for o in list(s.objects):
 if o.name.startswith('CAD part '):
  idx=int(o.name.split()[-1]);parts.append((idx,o));o.location=(0,0,0)
  if idx>=448:o.hide_render=True
  if idx in (0,2,3):
   o.modifiers.clear();bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.001)
   bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(o.data);bm.free()
   for p in o.data.polygons:p.use_smooth=True
   o.data.set_sharp_from_angle(angle=math.radians(45))
if 'Mint polymer' not in bpy.data.materials:
 m=bpy.data.materials['Chalk ceramic finish'].copy();m.name='Mint polymer'
for name in ['Chalk ceramic finish','Mint polymer']:
 m=bpy.data.materials[name];p=m.node_tree.nodes['Principled BSDF'];p.inputs['Roughness'].default_value=.36
chalk=bpy.data.materials['Chalk ceramic finish'];mint=bpy.data.materials['Mint polymer']
chalk.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value=(.69,.66,.57,1)
mint.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value=(.32,.57,.43,1)
for o in s.objects:
 if o.type=='LIGHT':o.data.energy*=.65
floor=bpy.data.materials['Warm studio'];floor.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value=(.82,.79,.69,1)
cam=s.camera;out=B/'work/cg';out.mkdir(parents=True,exist_ok=True)
def aim(loc,target,scale):
 cam.location=loc;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=scale
def reset():
 for idx,o in parts:
  o.location=(0,0,0)
  if idx in (0,2,3):o.data.materials[0]=mint
def frame(kind,f,n):
 reset();t=f/(n-1);e=t*t*(3-2*t)
 if kind=='hero':
  a=math.radians(-60+18*e);aim((6*math.cos(a),6*math.sin(a),3.2),(0,0,.45),5.4-.35*e)
 elif kind=='load':
  h=math.sin(math.pi*t)*1.6
  for idx,o in parts:
   if idx==3:o.location.z=h
  aim((3.5,-4.5,4.3),(0,0,.75),5.5)
 elif kind=='inside':
  a=math.radians(-55+7*e)
  for idx,o in parts:
   if idx==3:o.location.z=1.6*e
   elif idx==0:o.location.z=.8*e
   elif idx==1:o.location.x=-.65*e
  aim((6*math.cos(a),6*math.sin(a),3.9),(0,0,.45+.65*e),5.4+1.2*e)
 s.render.filepath=str(out/f'{kind}-{f:03}.png');bpy.ops.render.render(write_still=True)
if '--sample' in __import__('sys').argv:
 frame('hero',48,96)
else:
 reset();aim((3.5,-4.5,3.2),(0,0,.45),5.4);bpy.ops.wm.save_as_mainfile(filepath=str(B/'work/foli-production.blend'))
 for kind,n in [('hero',96),('load',72),('inside',144)]:
  for f in range(n):
   if (out/f'{kind}-{f:03}.png').exists():continue
   frame(kind,f,n)
   if f%24==0:print('PROGRESS',kind,f,n,flush=True)
