# PIE 0.2.0 — R2 UI verification

Verified 2026-11-02. This is a UI preview with working local features, not a declaration of complete V1 acceptance.

## Source of the UI

The approved [R2 design references](DESIGN_REFERENCE.md) were read and exported through PEN MCP.

Implemented matching tokens, bundled Noto Sans Thai, Lucide outline icons, the exported header logo, rounded rectangular navigation, primary buttons, card layouts and Buddhist-year date formatting.

Screens implemented:

- Home: empty and continue-writing states.
- Editor: text, save state, selection-aware Emoji/# insertion, media picker/gallery/viewer and confirmed deletion.
- Calendar: month navigation, today shortcut, entry indicators and selected-day preview.
- Settings: live PIN/reminder state, backup entry point and import only while database is empty.
- PIN: enroll/confirm/change/disable, unlock on return from background, wrong-code feedback and rate limiting.
- Reminders: runtime permission flow, weekdays, multiple times, custom/daily prompt mode, test notification and persisted state.
- Backup: password confirmation, document picker and authenticated encrypted export; import checks empty database and validates data/files before commit.

## Results

| Check | Result |
|---|---|
| Diary write ordering, delete barrier and backup barrier | 7 JVM tests passed |
| Backup cipher round trip, wrong password and tampering | 3 JVM tests passed |
| Settings write ordering and drain after navigation | 2 JVM tests passed |
| Room persistence, migration 1→2, backup round trip and media source deletion | 4 Android tests passed |
| Diary navigation/recreation and Pen UI / background PIN flows | 3 Android tests passed |
| Architecture boundaries | Passed for all 8 modules |
| Debug build and lint | Passed, no lint errors |
| R8 Release build | Passed, unsigned |
| Samsung S25 Ultra (SM-S938B) update install and launch | Passed, version 0.2.0 / code 2 |

Total: **19 passed**, no skipped tests or failures in the final reports.

Runtime tests used Android 17 on the headless nokjor_test AVD, serial emulator-5580. They were not run against the user's phone. UI preview screenshots contain only test data.

## Artifacts

- Debug APK: 17,754,066 bytes, version 0.2.0 / code 2, signed with the same local debug key as 0.1.0.
- Release APK: 3,044,651 bytes, unsigned.
- Debug SHA-256: 83f9f3c133c177ed89357b68fb2304926d45c1eaa12e832692b00124375d5023
- Room schema 2 and explicit migration 1→2 are included.
- Noto Sans Thai and Lucide licenses are packaged in APK assets.

## Remaining acceptance work

- On-device power-management delivery for reminders; AlarmManager is inexact and Android/OEM restrictions can delay it.
- Local usage metrics, minimum-API device coverage, Thai IME/selection, accessibility/large fonts and process-death/low-storage stress tests.
- Media viewer behavior across every lifecycle and supported codec.
- Very large / malformed backup stress tests and release signing.
- PIN is an access gate. The local Room database does not have app-level encryption; the exported backup does.
- No physical-device frame-time/RAM/startup benchmark is claimed.
