"""Rebuildable FOLI studio scene, based on the supplied STEP assembly."""
import bpy, bmesh, math, sys
from pathlib import Path
from mathutils import Vector
import numpy as np
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'media/source/work/renders';OUT.mkdir(parents=True,exist_ok=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
def mat(name,color,metal=0,rough=.35):
 m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
 p=m.node_tree.nodes.get('Principled BSDF');p.inputs['Base Color'].default_value=(*color,1);p.inputs['Metallic'].default_value=metal;p.inputs['Roughness'].default_value=rough
 return m
chalk=mat('Chalk ceramic finish',(.82,.80,.73),0,.29)
mint=mat('Mint polymer',(.44,.72,.58),0,.3)
dark=mat('Graphite elastomer',(.025,.036,.032),0,.5)
metal=mat('Satin steel',(.38,.43,.45),.8,.24)
pcb=mat('PCB solder mask',(.025,.16,.10),.1,.4)
paper=mat('Warm paper',(.94,.92,.85),0,.7)
blue=mat('Battery sleeve',(.05,.25,.38),.25,.32)
shell=[];objects=[]
for path in sorted((ROOT/'media/source/work/meshes').glob('*.npz')):
 d=np.load(path);idx=int(path.stem)
 if not len(d['v']):continue
 mesh=bpy.data.meshes.new(path.stem);mesh.from_pydata((d['v']*.025).tolist(),[],d['f'].tolist());mesh.update()
 obj=bpy.data.objects.new('CAD part '+path.stem,mesh);bpy.context.collection.objects.link(obj)
 material=metal
 if idx in (0,2,3):material=chalk;shell.append(obj)
 elif idx==1:material=blue
 elif idx in (439,892):material=pcb
 elif idx in (441,442):material=paper
 elif idx in (6,447):material=dark
 elif idx>6:material=dark if idx%4 else metal
 obj.data.materials.append(material)
 bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00001);bm.to_mesh(mesh);bm.free()
 for p in mesh.polygons:p.use_smooth=True
 mesh.set_sharp_from_angle(angle=math.radians(35))
 mod=obj.modifiers.new('Surface normals','WEIGHTED_NORMAL');mod.keep_sharp=True;mod.weight=50
 objects.append(obj)
floor=mat('Warm studio',(.91,.89,.83),0,.65)
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.065));ground=bpy.context.object;ground.data.materials.append(floor)
scene=bpy.context.scene;scene.render.engine='BLENDER_EEVEE_NEXT';scene.eevee.taa_render_samples=48
scene.render.resolution_x=1920;scene.render.resolution_y=1080;scene.render.resolution_percentage=100
scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs['Color'].default_value=(.85,.82,.72,1);scene.world.node_tree.nodes['Background'].inputs['Strength'].default_value=.35
scene.view_settings.view_transform='AgX'
def area(name,loc,power,size):
 bpy.ops.object.light_add(type='AREA',location=loc);o=bpy.context.object;o.name=name;o.data.energy=power;o.data.shape='DISK';o.data.size=size;o.rotation_euler=(Vector((0,0,.4))-o.location).to_track_quat('-Z','Y').to_euler()
area('Large softbox',(-3,-4,7),650,5);area('Edge light',(4,2,5),850,4);area('Front fill',(0,-4,2),120,3)
bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.lens=55
def shot(name,loc,target,scale):
 cam.location=loc;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=scale
 scene.render.filepath=str(OUT/(name+'.png'));bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'media/source/work/base.blend'))
