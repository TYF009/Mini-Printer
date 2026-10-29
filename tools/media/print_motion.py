import bpy,math
from pathlib import Path
from mathutils import Vector
B=Path(__file__).resolve().parents[2]/'media/source'
bpy.ops.wm.open_mainfile(filepath=str(B/'work/foli-production.blend'))
s=bpy.context.scene;cam=s.camera;cam.location=(4,-5,3.3);cam.rotation_euler=(Vector((0,0,1.1))-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=6.6
m=bpy.data.materials.new('Printed thermal paper');m.use_nodes=True;p=m.node_tree.nodes['Principled BSDF'];p.inputs['Roughness'].default_value=.75
n=m.node_tree.nodes.new('ShaderNodeTexImage');n.image=bpy.data.images.load(str(B/'assets/thermal-note.png'));m.node_tree.links.new(n.outputs['Color'],p.inputs['Base Color'])
mesh=bpy.data.meshes.new('Paper feed plane');mesh.from_pydata([(-.116,-.745,.95),(-.116,.68,.95),(-.116,.68,2.44),(-.116,-.745,2.44)],[],[(0,1,2,3)]);mesh.update()
uv=mesh.uv_layers.new()
for loop,co in zip(uv.data,[(0,0),(1,0),(1,1),(0,1)]):loop.uv=co
o=bpy.data.objects.new('Printed paper illustration',mesh);s.collection.objects.link(o);o.data.materials.append(m)
out=B/'work/cg';out.mkdir(exist_ok=True)
for f in range(60):
 t=f/59;e=t*t*(3-2*t);height=.18+1.31*e
 for j in (2,3):mesh.vertices[j].co.z=.95+height
 for j in (0,1):uv.data[j].uv.y=1-height/1.49
 # Growing feed reveals the same monochrome note, no performance claim.
 s.render.filepath=str(out/f'print-{f:03}.png');bpy.ops.render.render(write_still=True)
print('Paper feed complete',flush=True)
