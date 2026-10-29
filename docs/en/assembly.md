# Mechanical files and assembly preparation

[简体中文](../zh-CN/assembly.md) · [Documentation](index.md)

![Annotated CAD assembly](../../media/images/assembly-explained.jpg)

The supplied enclosure set is based on the 2023-09-18 assembly. The separated CAD view explains the relationship between parts. It is not a proven step-by-step build sequence, a tolerance report or a demonstration of how the lid moves.

## Printable parts

| Part | Source |
|---|---|
| Enclosure body | [body.stl](../../mechanical/stl/body.stl) |
| Upper cover | [upper-cover.stl](../../mechanical/stl/upper-cover.stl) |
| Lower cover | [lower-cover.stl](../../mechanical/stl/lower-cover.stl) |
| Retaining plate | [retainer.stl](../../mechanical/stl/retainer.stl) |

The original names include part dates: body 230727; upper/lower cover and retainer 230719. The full STEP assembly is approximately 224 MB and is managed separately from lightweight README media. Consult the mechanical download/source information before expecting the full assembly in a standard clone.

## Supplied assembly reference

The owner-provided visual BOM lists the following quantities. Treat it as a reference and verify against the final mechanical revision before purchasing.

| Item | Quantity |
|---|---:|
| Control board, body, upper cover, lower cover, retainer | One each |
| M3 × 6.4 mm light guide | 3 |
| M2 × 10 countersunk self-tapping screw | 4 |
| M2 × 8 pan-head self-tapping screw | 4 |
| M2 × 6 knurled shaft | 2 |
| Cutter | 1 |
| Feet | 4 |
| Thermal print mechanism | 1 |
| Battery | 1 |
| 57 × 30 paper roll reference | 1 |

Battery lead: XH 2.54 mm 2-pin; owner-specified cell rating: 5C. Capacity, polarity, actual cell dimensions and the exact procurement part number must be matched to the installed hardware. The historical BOM is not a substitute for an electrical BOM.

## Preparation workflow

1. Inspect all four STL files and the assembly model in a CAD/slicer viewer. Confirm units, part extents and revision correspondence before slicing.
2. Check the board outline, mounting holes, connector openings and paper path against the parts you actually have. The supplied individual STL files use different local coordinates; do not infer the assembled positions from their origins.
3. Inspect a dry fit of the housing, board and mechanism before fastening. Keep the battery disconnected while routing wiring and fitting the mechanism. Confirm ribbon/connector orientation against the final electrical design.
4. Check light-guide, cutter, shaft and retainer locations in the original assembly. Fastener placement and depth must be checked against the geometry; the quantity table alone does not establish where each screw belongs.
5. Confirm that the roll, feed path, cover and exposed cutter do not interfere. Record the chosen paper orientation and final assembly photos for a reproducible guide.
6. Apply the selected hardware/firmware verification procedure before treating a changed assembly as qualified.

## Manufacturing details still to record

No verified filament/resin, nozzle size, layer height, wall count, support arrangement, part orientation, shrinkage compensation or fit tolerance is supplied with the current project record. The visual finish studies do not establish a material specification. A contributor should record those settings together with printer model, part revision and fit findings when producing a new enclosure.

The current repository provides the geometry and assembly reference, not a production-ready manufacturing package. Historical hardware printing and current software checks are documented separately in [verification](verification.md).
