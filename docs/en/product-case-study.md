# FOLI: making the whole product legible

[简体中文](../zh-CN/product-case-study.md) · [Documentation](index.md)

FOLI is an English-first companion experience built around an existing physical Mini-Printer platform. The project connects a real board and print mechanism, embedded firmware, a supplied enclosure design and a new native Android app. The portfolio objective is to show how product decisions cross those boundaries, while making the design understandable and reproducible.

## A small, concrete use case

A short list beside a keyboard is enough to expose the whole chain: choose a template, change the words, inspect the monochrome result, connect and send it, then keep the paper nearby. Gift notes and portable lists extend the same capability without requiring a cloud account or a complex content service.

Warm Chalk surfaces, Mint accents and a paper-centered interface establish a consistent visual language. The lifestyle imagery presents this intended experience; actual hardware photographs establish that the underlying electronics and printing work existed. Their evidence roles remain separate.

## Decision 1: preview the bytes, not an approximation

A decorative preview could look attractive while hiding thresholding, dithering and line-width effects. FOLI instead encodes a 384-dot-wide raster and builds the preview from that output. This makes the editor's core object meaningful: users inspect the representation being prepared for transmission.

The tradeoff is bounded content and real processing work. `PrinterProtocol` enforces width and row limits; `PaperRenderer` handles layout. UI updates preserve the previous paper while preparing the next preview rather than replacing the screen with empty content. See [architecture](architecture.md) and the [Android source](../../software/android/app/src/main/java/studio/foli/android/).

## Decision 2: design against an older physical phone

The Mi 6 exposed issues that a large desktop mockup did not: keyboard obstruction, checklist outlines lost during binarization, and editor movement when feedback appeared. The implementation moved text entry to a full-screen composer, renders checkbox geometry explicitly and separates feedback from the paper viewport.

Template filters, local favorites, larger previews, Cancel/Undo and retained scroll positions reduce the number of times a user must reconstruct context. These are implemented interactions with scoped device observations, not a claim of measured animation FPS or complete usability research. The [verification record](verification.md) identifies what was actually exercised.

## Decision 3: uncertainty is a product state

Bluetooth connection, successful data transfer and finished paper output are different events. The inherited protocol offers limited end-to-end evidence. FOLI presents transfer state separately, requires the expected device-state sequence before confirming completion and protects against blindly resending a partly received job.

This can require an explicit printer restart after an uncertain transfer. It is less convenient than automatic retry, but avoids silently duplicating or corrupting a physical job when the protocol cannot establish what was printed. Firmware fixes and future protocol improvements must preserve this distinction.

## What the repository contributes

The FOLI work includes the native Android experience, template/editor behavior, raster/protocol integration, identity, product visualization and consolidated engineering documentation. Existing hardware, firmware and mechanical inputs are retained with their source context. Purchased rights, upstream authorship and new implementation work are described separately in [provenance](provenance.md).

The release is a development project with explicit limits: historical hardware printing exists; App UI checks and protocol assertions exist; a new end-to-end physical print with all updated components has not been repeated. The useful next evidence is final assembled-device photography, manufacturing settings and a versioned integration record—not invented speed, endurance or production-readiness claims.
