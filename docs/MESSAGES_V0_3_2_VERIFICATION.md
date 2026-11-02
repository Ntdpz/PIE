# PIE v0.3.2 · Daily messages and editable design

Verified locally on 2 November 2026. This remains a UI preview.

## Delivered

- 240 original Thai messages in eight themes, interleaved by day. Home and automatic reminders share a deterministic daily selection; custom reminder precedence is preserved.
- Editable `design/PIE.pen`, the original logo and both mockup photos committed together with relative image paths. Screen node IDs, geometry, text and tokens are preserved.
- `scripts/Verify-Design.ps1` checks valid Pen JSON, all eight approved R2 frames and every referenced image. CI now includes this check.

## Passed

- `testDebugUnitTest :core:domain:test lintDebug assembleDebug` — successful. 17 unit cases: diary writer 7, backup cipher 3, settings writer 2, daily messages 5. Unchanged suites may be Gradle up-to-date.
- All 5 app instrumentation cases passed on Android 17 emulator. The settings flow now also asserts that the home greeting equals the default reminder preview and that the 240-message description is displayed.
- Catalog checks cover a full 240-day unique cycle, nonblank/short copy, daily stability across schedules, custom text precedence, blank custom fallback, dates before the epoch and year rollover.
- Architecture boundaries passed for all eight modules.
- Design validation passed for eight approved R2 frames and three local image files. No external image URLs remain in `PIE.pen`.
- APK installed over the existing app on S25 Ultra (`R5CXC3DMXLD`) and launched successfully: version `0.3.2`, code `5`.

The four data instrumentation cases were not repeated; this change does not alter storage, migrations, encryption or archive handling. Previous results are recorded in [v0.3.0 verification](MOTION_V0_3_VERIFICATION.md).

The Pen package was checked structurally and against the preserved node IDs. A fresh interactive Pen render was not performed in this update. Message selection remains date-based; rearranging the catalog in a future release changes the date-to-message mapping. Device-wide performance, TalkBack acceptance and remaining V1 roadmap items are not implied by these checks.
