# PIE v0.3.1 · Loader verification

Verified locally on 2 November 2026. This remains a UI preview.

## Behavior

- Shared `PieLoader` in `core:designsystem`: the original PIE mark breathes gently above three traveling dots, with a task-specific label.
- Compact variant for home/history loading, media copying, PIN verification and backup/restore. Full variant for opening the app and loading an editor entry.
- All instances follow real pending state; completion removes the loader immediately. No minimum display timer, new dependencies or data-layer changes.
- Indeterminate progress semantics and a polite live region describe the pending task without announcing each animation frame. Platform Compose animation timing follows the system duration scale.
- The PIN gate remains opaque and immediate.

## Passed

- `testDebugUnitTest :core:domain:test lintDebug assembleDebug` succeeded (12 JVM cases; unchanged suites can be Gradle up-to-date).
- All 5 app UI cases passed on Android 17 emulator: existing diary, settings/backup/reminder and PIN flows, plus two new loader checks.
- Loader checks verify accessible indeterminate status, immediate removal on completion, and different pixels across actual rendered animation phases.
- The accessible-status/completion check passed again with system animator duration scale zero. Original emulator setting restored afterward.
- Architecture boundaries passed for all 8 modules; staged whitespace checks passed.
- Installed and launched on S25 Ultra (`R5CXC3DMXLD`); Android reports version `0.3.1`, code `4`.

The 4 data instrumentation cases were not repeated because this change does not alter persistence, migrations or backup operations. Their previous result is recorded in [v0.3.0 verification](MOTION_V0_3_VERIFICATION.md).

[Loader preview](assets/loader-v0.3.1.gif) contains actual Compose frames advanced by the instrumentation animation clock. This is a component preview, not a measurement of device frame pacing. TalkBack acceptance and performance across devices remain outside this check.
