# PIE 0.7.0 — Gallery Feed

Implements the approved [R3 Gallery Feed](../design/GALLERY_FEED_R3.md) in Kotlin/Compose. Version code 13, package `com.ntdpz.pie`. The previous 0.6.x day pager/story sheet is replaced within the existing memory route.

## Behavior

- The Gallery cover/date overview and hidden calendar remain. Opening a cover positions a continuous LazyColumn at that date. Back navigation restores the overview position.
- Original-ratio media, attachment counter and complete diary prose share one vertical reading flow. Text-only days have no empty media stage. No story popup, independent reader scroll or forced day snapping.
- Horizontal attachment paging animates the media container's height. Header-only dimension reads run on IO and account for image EXIF orientation and video rotation. Feed images keep `ContentScale.Fit` with no black backing; Gallery cover thumbnails retain cropping.
- Read complete prose for visible dates and one adjacent date on either side; do not decode all photographs or load the entire diary text history. Per-entry read errors have retry controls. A failed attachment remains swipeable so the other attachments stay reachable.
- Photo zoom/reset stays available. Video requires explicit play, starts muted, and stops when its media area leaves the viewport, the attachment changes or the app backgrounds.
- Edit callbacks carry each displayed entry's date. No database, write pipeline, encryption or Merge rule changes.

## Verification

- Gradle `testDebugUnitTest :core:domain:test lintDebug assembleDebug :app:assembleDebugAndroidTest` passed: 36 JVM/domain cases, Android lint, APK and instrumentation assembly.
- The architecture boundary script passed for all 8 modules.
- The emulator UI suite passed 12 cases. It covers theme/PIN, editor persistence, backup/Merge, reminders, loaders and the new Feed scenarios.
- After the final attachment-failure UI adjustment, all 3 Gallery/Feed cases passed again. One Light Feed case also passed at 320 dp width; the emulator resolution was restored.
- Feed fixtures exercise 16:9, 4:3, 9:16 and EXIF-rotated 3:4 images in both themes, a long complete entry, a text-only day, scrolling between dates, selected-date editing, return-to-Gallery position and explicit video/mute/background/offscreen behavior.
- Reviewed runtime Light and Dark screenshots. Test media and notes are emulator fixtures; instrumentation did not run against physical S25 data.
- APK signing verification passed with the same debug certificate as 0.6.1. Download/output copies use the same SHA256.

Logs (local, ignored): `work/feed-verification.log`, `work/feed-all-ui-tests.log`, `work/feed-final-gallery-tests.log`, `work/feed-small-ui-tests.log`. Android data tests were not repeated because persistence did not change; the 20-case 0.6.0 record remains historical evidence.

Broad physical-device/IME/font-scale/TalkBack acceptance, release signing/performance measurements, local usage metrics and V2 Drive sync remain pending.

Installed on S25 (SM-S938B) with an in-place update; package version 0.7.0 / code 13 verified and MainActivity launched. No app data was cleared.
