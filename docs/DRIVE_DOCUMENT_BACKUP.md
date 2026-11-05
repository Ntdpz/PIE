# Google Drive document backup · 2026-11-05

## Scope

The user selected Android's document picker instead of direct Google OAuth.
PIE does not connect to a Google account, store tokens, request INTERNET, or
implement automatic sync. The installed Google Drive document provider owns
account selection, cloud transfer, offline queues and upload status.

Settings now includes a tonal Google Drive entry. The introduction uses the R4
Expressive layout, shared PIE colors, bundled Noto Sans Thai and Lucide icons.
The primary action opens the existing encrypted export flow with Drive-specific
instructions. The user sets and confirms a backup password, then explicitly
chooses Drive and a folder in Android's CREATE_DOCUMENT picker. Other document
providers remain available. Import opens the existing OPEN_DOCUMENT and review /
merge flow; it never silently replaces the diary.

The encrypted archive is staged, written and read back using the existing
BackupDestinationWriter. Export success confirms that the provider returned
valid archive bytes, not that a remote Google server has finished uploading.
The Drive export completion message directs users to check upload status in Drive.
Cancellation clears the password fields and does not record export success.

No database, encryption format, write ordering, or merge rules changed in this
increment. Existing unrelated working-tree changes were retained.

## Design source

Supplement to the main PIE design source: `design/PIE-Drive-R4.fig`, page
`1245698939:222` (R4 · Drive · Android Expressive). The file-picker variants are
`1245698939:454` (Light) and `1245698939:486` (Dark). Earlier OAuth concepts remain
design alternatives, not implemented controls. Runtime copy includes the provider
instructions and import shortcut appropriate to the user's selected approach.

## Verification

Physical Samsung SM-S938B, Android 17, serial R5CXC3DMXLD:

- Six Drive instrumentation tests passed: navigation and import, Light/Dark,
  320 dp with 150% text, cancelled picker without diary/export mutation,
  actual system picker open/cancel, and an encrypted verified export to an
  isolated fixture destination. The success test checks the PIEBKP02 header.
- Two regression tests passed: existing settings/reminder/export navigation and
  persistent theme selection across activity recreation. Settings tests now
  scroll to controls after the R4 section reordering.
- Three BackupRoundTripTest cases passed, including the large streaming archive
  through Android's document resolver, wrong-password recovery, text/media and
  formatting restoration, and Unicode backup passwords.
- Device screenshots of the new introduction were inspected in Light and Dark.
- App tests used `com.ntdpz.pie.devicetest`; data tests used
  `com.ntdpz.pie.core.data.test`. The regular diary package was not cleared or
  uninstalled. Test archives were fixture data and cleaned up.

The Gradle connected-test launcher hit a USB reconnect/provisioning failure;
explicit-serial ADB instrumentation reruns completed on S25. One test initially
assumed CREATE_DOCUMENT adds CATEGORY_OPENABLE; that unnecessary library-specific
assertion was removed, and the cancellation case passed on rerun.

Required host checks: `gradlew testDebugUnitTest :core:domain:test lintDebug
assembleDebug` and `pwsh -File scripts/Verify-Architecture.ps1` passed.

The regular `com.ntdpz.pie` debug APK was updated using `adb install -r` on S25
at 15:01 Bangkok time on 5 November 2026. Installation returned Success and the
launcher returned Status: ok (cold start, 425 ms). Version remains 0.7.0 / code 13;
no release-version bump, commit or push was made.

Remote upload/download against the user's Google Drive account has not been
verified. The user must select their own Drive destination and confirm its
completed upload status. This increment does not claim automatic sync or complete
V1 acceptance.
