# Verification record — 2026-10-29

The first table records the initial 0.1.0 build. Later sections record physical-device and 0.2.0 results.

| Check | Result |
| --- | --- |
| Native Java / Android resource compilation | Passed |
| Debug APK packaging | Passed |
| Android Lint | Passed: 0 errors, 1 dependency-version advisory |
| Host-side raster/protocol regression checks | Passed: 158 assertions |
| Final delivered APK v2 signature | Passed |
| Application ID / launcher / SDK manifest inspection | Passed |
| Android phone visible to ADB | No device at handoff |
| Install, launch and actual UI verification | Pending phone |
| Real BLE / printer / paper-output test | Pending phone and powered printer |

The dependency advisory is for the pinned compile-only AndroidX annotation 1.2.0 (Lint suggests 1.3.0). It is not a runtime failure. The Gradle plugin also emits an experimental-path-option message because the workspace contains Chinese characters on Windows. The build uses that supported override and direct Java wrapper invocation; it does not relocate project output outside the requested workspace.

Protocol assertions cover full-row framing, MSB-first boundaries, all-white/all-black output, transparent-pixel compositing, dither behavior, density/start commands, status shape/ranges, and rejection of oversized or malformed images. They do not prove BLE radio behavior or paper-output quality.

Final APK and its SHA-256 are recorded in `../../releases/android/README.md`. On-device results should be added here as each physical test is actually performed.

## Physical-phone smoke test — 2026-10-29, version 0.1.1

Device: Xiaomi Mi 6 (`sagit`), Android 8.0.0 / API 26, 1080×1920. USB authorization and installation were enabled by the owner.

- APK installation and native Activity launch: passed; final cold launch reported 424 ms Activity time (one observation, not a performance benchmark).
- Create/home screen: inspected on the physical display via screenshot.
- Note editor and real raster preview: opened successfully.
- Text editing: appended a temporary `MI6` suffix to the built-in sample title, applied the preview and saved it.
- Draft persistence: the edited title survived APK update/process restart and appeared in the actual raster preview.
- Templates: navigated to the template list and selected Today's three; default sample content restored.
- Device page: verified disconnected state and unknown battery/paper/temperature rather than simulated values.
- Final app-process log sample contained no `FATAL EXCEPTION`; this is a scoped smoke check, not proof of all runtime paths.
- Fixed a real-device defect: unicode checkbox outlines lost thin edges during thresholding. Checkboxes now use explicit raster geometry, including correct alignment of wrapped text.
- Centered bottom-navigation and editor-tool captions after inspecting the native layout.
- Rebuilt with `assembleDebug` and `lintDebug`: passed (same one annotation-version advisory). Final delivered APK signature verified.

Selected historical screenshots are retained in `verification/evidence/`; the full intermediate history and UI trees remain in the original working materials. `home-final.png`, `templates-final.png` and `editor-final.png` show the final installed build; earlier files document the discovered checkbox defect and its first correction. These contain phone status-bar details and are local verification evidence, not automatically approved public marketing assets.

Still pending: photo picker/import and image-transform smoke tests, permissions/scan/reconnect testing, real BLE device states, actual paper output, abnormal print recovery, large-font layout and broader Android compatibility. No physical print command was sent during this session.

## Connected printer / no paper — 2026-10-29

The owner connected the printer and explicitly stated that no paper was installed. FOLI's actual Device screen showed `Connected via Bluetooth LE`, battery around `75–76%` across the UI snapshot and later screenshot, paper `Out of paper`, print-head temperature `0 °C`, and the corresponding paper-loading message. Screenshot: `verification/evidence/ble-no-paper.png`.

This verifies connection readiness and receipt/display of a valid four-byte device status packet for this session. Battery accuracy is not calibrated by this observation. Temperature `0 °C` is an unresolved reading, not proof of functioning temperature sensing; investigate the connected mechanism/NTC and firmware's raw ADC/conversion before heating tests.

No physical print was sent. An attempted UI-only guard check did not produce a captured paper-out rejection dialog (the phone moved to the help screen during navigation), so the runtime print-block test remains pending. Source-level preflight checks reject `paper == 1`; that is separate from an observed on-device test. Permission granting and discovery were performed by the owner, not independently validated as a full automated flow.

## 0.2.0 UX upgrade — physical phone

Build/Android Lint passed (0 errors, 1 pre-existing annotation-version advisory); 158 protocol assertions passed after the UX changes. Final APK v2 signature passed. Installed and launched on the same Mi 6 after each device-driven refinement.

Observed on device: eight-card gallery; Focus filters to two items; template preview and application; live Clean-to-Journal preview; Keep changes; header Undo restoring the previous style; favorite heart transition; Saved filtering to one item; favorite surviving the final APK update; full-screen composer with keyboard and Cancel/Done visible; temporary `UX` suffix entry and Cancel restoring the prior sample; full-screen paper inspector opening. Selected 0.2.0 captures: `verification/evidence/final_gallery.png` and `verification/evidence/journal_live.png`.

Resolved findings: non-prewarmed thumbnail blanks, keyboard covering floating text-sheet actions (replaced by a full-screen composer), undo notice resizing the editor viewport, selected category falling out of view, stale selected-option typography, and small-screen preview sizing. The screenshot `text_sheet.png` documents the earlier keyboard problem; `composer_keyboard.png` shows the corrected pattern. `final_gallery.png` records the final installed gallery.

No fatal exception appeared in the final scoped process-log sample. It recorded one 214 ms startup raster render; no sustained FPS or performance conclusion is inferred. The printer was off, and no print or firmware update was performed. Remaining validation includes physical printing, all photo paths, actual multi-touch gestures, enlarged fonts, broader accessibility and process/recreation edge cases.

## Repository integration — Android 0.2.1

The integrated application adds a tested Bluetooth access-change boundary to asynchronous callbacks and queued GATT read/write operations. Cleanup attempts close even if disconnect fails. Protocol host checks (158 assertions) and the new failure-boundary suite passed on JDK 17. `assembleDebug` and `lintDebug` passed against the integrated 0.2.1 source (0 errors, one advisory for pinned compile-only AndroidX annotation 1.2.0); this does not extend historical 0.2.0 phone UI checks to 0.2.1. No radio or physical printing validation was performed for these changes. Historical screenshots remain in the original working materials; selected public product media is maintained separately at the repository level.


The 0.2.1 permission-related checks exercise the host-side recovery boundary only. Permission revocation on a real Android device has not been tested in this integration.

## Legacy terminal-state semantics correction — 0.2.1

The actual receive path now uses shared `PrintOutcome.fromStatus`. State 3 is finished-or-stopped, never physical-success acknowledgement. A nominal observed start→terminal transition stops waiting but retains `dirty` and restart recovery; paper-out, overheat or previous failure cannot clear an interrupted job. A terminal without observed start remains pending. `MainActivity.changed()` uses typed outcome/restart confirmation rather than an English status prefix to manage persistence. Seven host outcome scenarios passed in addition to 158 encoder assertions and the BLE access boundary suite. These are source/host checks, not device or physical-print tests.
