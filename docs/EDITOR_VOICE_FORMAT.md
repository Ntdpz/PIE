# Editor · voice and formatting · 2026-11-05

## Approved source

`design/PIE-Kalka.fig`, page `913025165:493`: direction B, Apple Notes style.
The writing and expanded Aa artboards are `913025165:963` and `913025165:988`.

The bottom toolbar uses paperclip, Aa, emoji and microphone icons. Aa reveals
heading, normal text, bold and bullet controls above the toolbar. The panel fades
and expands using the shared `PieMotion` timings. Controls have 48 dp touch targets
and spoken labels; selected formatting is exposed through Compose semantics.

## Behavior

- Formatting is separate metadata with UTF-16 selection offsets. It does not add
  Markdown markers to existing notes. Heading formatting acts on paragraphs;
  bold acts on the selected text, or the current paragraph without a selection.
- Text and formatting travel through the same app-scoped serialized diary writer.
  Reopening a pending failed draft retains both. The editor keeps its selected date.
- Room migration 3 → 4 adds a non-null `formatting` column defaulting to `[]`.
  Existing text, media and import receipts are retained; schema 4 is checked in.
- Feed prose and import conflict previews render the saved formatting. Archive
  manifests include optional formatting metadata; old archives default to plain
  text. Merge receipts distinguish formatting changes, and Keep Both shifts the
  incoming ranges with the appended text.
- Voice typing uses Android's **on-device** SpeechRecognizer (Android 12+), with
  Thai locale. There is no network recognition fallback or INTERNET permission.
  Availability depends on the installed recognition service and Thai language model.
- Interim recognition is an unsaved inline preview. Final recognition replaces
  the selection and can be undone. Cancel, backgrounding and disposal invalidate
  callbacks; partial text is discarded. Missing service/model and permission errors
  show an explanation. Recognition accuracy still needs a spoken Thai device check.

## Device testing without changing the user's diary

Build app instrumentation with `-Ppie.deviceTest=true`. Debug builds then use
`com.ntdpz.pie.devicetest` and separate app storage. The regular build retains
`com.ntdpz.pie`. Never run the editor replacement tests against the regular app on
a user's device: those tests intentionally replace today's text.

Use an explicit S25 serial for every ADB command. The local `nokjor_test` AVD was
stopped and deleted at the user's request; do not recreate or launch an emulator.

## Verification

Samsung SM-S938B (USB serial `R5CXC3DMXLD`), Android 17:

- 18 data instrumentation cases passed: migrations, formatting manifests/receipts
  and merge behavior.
- 3 backup round-trip cases passed, including the large document-provider archive.
- 8 app cases passed in the isolated package: formatting survives reopening and
  further typing, Thai selection/recreation, Feed light/dark/video and import review.
- 2 voice UI cases passed: on-device listening starts, cancellation and backgrounding
  leave the existing text unchanged and do not save interim recognition.
- Inspected device screenshots for the expanded formatting panel and listening state.

The first combined data run was interrupted by an ADB disconnect; the direct S25
reruns above completed. The editor test now waits for the writer barrier and resets
its formatting fixture; the calendar bounds assertion allows 0.01 dp for fractional
Samsung density rounding around the actual 48 dp control.

`testDebugUnitTest :core:domain:test lintDebug assembleDebug` and
`scripts/Verify-Architecture.ps1` passed. Natural spoken Thai recognition accuracy,
long dictation and real Samsung IME interaction still need hands-on acceptance.
The regular `com.ntdpz.pie` APK was updated with `adb install -r` and launched on
S25 at 12:19 Bangkok time. The existing app was not uninstalled or cleared.
This increment does not claim complete V1 or broad IME/accessibility coverage.
