# Mechanical files

[简体中文](README.zh-CN.md) · [Assembly guide](../docs/en/assembly.md)

| Printable part | File |
|---|---|
| Body | [body.stl](stl/body.stl) |
| Upper cover | [upper-cover.stl](stl/upper-cover.stl) |
| Lower cover | [lower-cover.stl](stl/lower-cover.stl) |
| Retaining plate | [retainer.stl](stl/retainer.stl) |

[source-map.json](source-map.json) records original supplied filenames and SHA-256. Geometry is unchanged. Independent STL coordinate systems are not an assembly-position reference.

The complete supplied 230918 STEP is packaged separately as `FOLI-mechanical-source.zip` in a release bundle rather than placed directly in the source tree. [Download status](../releases/README.md) lists what is published. Optional media builds expect the extracted assembly at `cad/assembly.step`; see [CAD input](cad/README.md).

The part set is published for inspection and fabrication, with [reference fasteners and assembly limits](../docs/en/assembly.md). Slicer profile, print orientation, tolerances and final material were not recorded in the supplied evidence; the repository does not invent validated settings. The exploded render illustrates structure, not a numbered assembly procedure.
