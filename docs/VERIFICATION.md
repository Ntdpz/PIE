# Foundation verification — 2026-11-02

Historical verification for 0.1.0. Current results: [0.2.0 UI verification](UI_V0_2_VERIFICATION.md).

Checked on Windows using JDK 21, Gradle wrapper 9.4.1 and Android SDK 37.

| Check | Result |
|---|---|
| Architecture boundaries across 8 modules | PASS |
| DiaryWriter unit tests | 5 passed, 0 failures |
| Android Room persistence test | 1 passed, 0 failures |
| Android Compose navigation/recreation test | 1 passed, 0 failures |
| Android lint | PASS, no errors; dependency update and launcher-icon recommendations remain |
| Debug APK compilation | PASS |
| Release APK with R8/resource shrinking | PASS, unsigned |
| Debug install and cold launch | PASS on Android 17 emulator |

Runtime tests used the existing nokjor_test AVD in a headless session on emulator-5580. It was started for this verification only.

The Compose test writes text with Emoji and #, waits for actual save, opens it from the calendar, and recreates the Activity. The Room test closes and reopens a database after upserting the same day.

After the runtime tests, only theme colors/navigation-bar color were adjusted. Debug, Release and lint were rerun. The strengthened writer ordering assertion was also rerun successfully.

Artifacts at verification:

- Debug APK: 12,611,281 bytes; signed with the local Android debug key.
- Release APK: 1,500,964 bytes; unsigned, not an installable release deliverable.
- These sizes describe the foundation only. Media, PIN, reminders and encrypted backup are not implemented.

No physical-device performance measurement, minimum-API runtime test, process-death guarantee, release-signing setup or complete V1 acceptance is claimed.

## Commands

```powershell
pwsh -File scripts/Verify-Architecture.ps1
.\gradlew.bat :core:domain:test testDebugUnitTest lintDebug :app:assembleRelease :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest --no-watch-fs
.\gradlew.bat :app:assembleDebug :app:assembleRelease lintDebug --no-watch-fs
.\gradlew.bat :core:domain:test
```

Private machine-specific paths remain in ignored local.properties. Intermediate logs and screenshots are in ignored work/.
