# PIE v0.3.0 · Motion verification

Verified locally on 2 November 2026. This is a UI preview; outstanding V1 acceptance work in the roadmap still applies.

## Passed

- `testDebugUnitTest :core:domain:test lintDebug :app:assembleDebug` — successful.
- Normal system motion: 12 JVM unit cases and 7 emulator cases passed (19 cases total; unchanged suites may be Gradle up-to-date).
- Emulator data cases: persistence, migration 1→2, encrypted backup round trip and copied media retention (4 cases).
- Emulator UI cases: diary edit/read/recreate, settings/reminders/backup controls, PIN setup and background locking with wrong/correct codes (3 cases).
- Disabled system motion: repeated all 3 app UI cases with animator, transition and window animation scales set to zero on the emulator. All passed, with final SaveStatus crossfade included. Original emulator settings restored in `finally`.
- Architecture boundaries checked for all 8 modules.
- Final Debug APK installed over the existing app on Samsung S25 Ultra, serial `R5CXC3DMXLD`, and launched successfully. Android reports versionName `0.3.0`, versionCode `3`.
- No new dependencies, permissions, database schema or network capability introduced.

## Review

[Motion behavior and ownership](MOTION.md). A short recording is included at `docs/assets/motion-v0.3.mp4`, captured from the emulator using synthetic/empty diary data.

Automated checks establish functional behavior with motion enabled and disabled. They do not measure 120 Hz frame pacing, RAM, battery use, TalkBack acceptance or every OEM. S25 install/launch is verified; perceived feel and haptic intensity should be judged on the phone. Historical Pen screenshots remain in the README; this update changes movement rather than the design system.
