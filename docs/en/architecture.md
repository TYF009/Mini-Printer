# From layout to paper

[简体中文](../zh-CN/architecture.md) · [Documentation](index.md)

![FOLI system architecture](../../media/images/system-architecture.png)

## Android: one raster for preview and transfer

`PaperRenderer` lays out notes or imported photos. `PrinterProtocol` converts pixels into a 384-dot-wide monochrome raster; each row occupies 48 bytes, black is 1, and the most significant bit represents the first dot in each byte. The preview is reconstructed from the encoded output, so it describes the bytes being prepared rather than an unrelated decorative thumbnail.

The current app accepts at most 768 raster rows. This is a bounded input limit, not a guaranteed print duration. Excessive text is rejected; tall photos are fitted rather than silently cropped by the encoder.

Relevant classes are in [the Android source](../../software/android/app/src/main/java/studio/foli/android/): `MainActivity`, `PaperRenderer`, `PrinterProtocol`, `BlePrinter`, `TemplateCatalog` and `ZoomPaperView`.

## BLE: transport acceptance is not a finished print

| Field | Value |
|---|---|
| Service | `4fafc201-1fb5-459e-8fcc-c5c9c331914b` |
| Characteristic | `beb5483e-36e1-4688-b7f5-ea07361b26a8` |
| Density | `A5 A5 A5 A5 01/02/03` |
| End of transfer / start trigger | `A6 A6 A6 A6 01` |
| Status payload | Four bytes: battery, temperature, paper, print state |
| Paper | 0 loaded; 1 missing |
| Print state | 0 initial; 1 start; 2 working; 3 finish |

FOLI sends whole raster rows using acknowledged GATT writes and requires a negotiated MTU of at least 51 bytes. The protocol guide in [Android documentation](../../software/android/README.md) describes pacing and current limits. Older upstream guidance about arbitrary 20-byte splitting should not be substituted for this app's row-based transfer contract.

A GATT write callback establishes transport acceptance. The legacy wire format has no job ID, per-row sequence number or application-level checksum. The UI therefore distinguishes connection, sending and device-reported printing. Confirmation depends on observing the expected state sequence; absence of that sequence is uncertainty, not success. Restart recovery is deliberate because reconnecting alone does not clear a partially received job.

## Firmware: buffer, heat and feed

The [firmware](../../software/firmware/) receives commands and raster rows, buffers them, shifts data into the thermal head, latches it, enables heating groups and advances the paper with the motor driver. Battery, paper and head-temperature information feeds status handling. Exact timing and pin mapping belong to the selected firmware configuration and final board revision.

Repository hardening addresses legacy command fall-through and failure-state handling separately from the historical hardware test. Compile/host-test results must not be read as physical qualification of those changes. See [verification](verification.md) for the boundary.

## Electronics and enclosure

The board integrates control, charging/regulation, print-head supply, motor drive and sensor connections. The [final editable hardware project](../../hardware/EDA-project/foli-mainboard.epro2) is the authority when older diagrams or documentation disagree.

The enclosure positions the board, cell, print mechanism and paper roll. [Assembly documentation](assembly.md) links the printable parts and lists missing manufacturing details. The annotated CAD image illustrates component relationships; its separated positions are not assembly instructions or a validated motion mechanism.

## Local data

The app has no account, backend, analytics or Internet permission. Notes, favorites and a downsampled selected-photo copy are local. Android permissions are used for Bluetooth discovery/connection and the system document picker supplies images. This describes the current implementation, not a security certification. Broader accessibility, lifecycle and compatibility checks remain in the verification plan.
