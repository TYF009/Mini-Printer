from pathlib import Path
import sys, json, numpy as np
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/runtime/pythondeps'))
from OCP.STEPControl import STEPControl_Reader
from OCP.TopExp import TopExp_Explorer
from OCP.TopAbs import TopAbs_SOLID,TopAbs_FACE,TopAbs_REVERSED
from OCP.TopoDS import TopoDS
from OCP.BRepMesh import BRepMesh_IncrementalMesh
from OCP.BRep import BRep_Tool
from OCP.TopLoc import TopLoc_Location
r=STEPControl_Reader();r.ReadFile(str(ROOT/'mechanical/cad/assembly.step'));r.TransferRoots()
e=TopExp_Explorer(r.OneShape(),TopAbs_SOLID);out=ROOT/'media/source/work/meshes';out.mkdir(parents=True,exist_ok=True)
i=0
while e.More():
 s=TopoDS.Solid_s(e.Current());BRepMesh_IncrementalMesh(s,0.12,False,0.25,True)
 f=TopExp_Explorer(s,TopAbs_FACE);vs=[];ts=[]
 while f.More():
  face=TopoDS.Face_s(f.Current());loc=TopLoc_Location();tri=BRep_Tool.Triangulation_s(face,loc)
  if tri:
   offset=len(vs);tr=loc.Transformation()
   for j in range(1,tri.NbNodes()+1):
    p=tri.Node(j).Transformed(tr);vs.append((p.X(),-p.Z(),p.Y()))
   for j in range(1,tri.NbTriangles()+1):
    a,b,c=tri.Triangle(j).Get()
    if face.Orientation()==TopAbs_REVERSED:b,c=c,b
    ts.append((a+offset-1,b+offset-1,c+offset-1))
  f.Next()
 np.savez_compressed(out/f'{i:04}.npz',v=np.array(vs,dtype=np.float32),f=np.array(ts,dtype=np.int32))
 if i%100==0:print('Exported',i,flush=True)
 i+=1;e.Next()
print('Done',i,flush=True)
