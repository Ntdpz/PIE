# PIE Pet · R5 implementation · 2026-11-05

> Archived: the user requested a return to the pre-mascot version. Pet runtime,
> widgets, overlay permissions, navigation and home slots have been removed from
> the active app. Source files are preserved under `work/pet-disabled/` and design
> proposals remain archived. The notes below describe the removed implementation.

## Active motion revision

The user requested frequent, continuous motion rather than sitting or automatic
sleep. The default now cycles Wave / Happy / Peek poses every 900 ms, with a
continuous 1.8-second sway/hop cycle (7-degree rotation, visible bounce and small
horizontal movement) in the app and overlay. The 45-second automatic sleep rule
is removed. Explicit one-hour snooze, lifecycle/lock hiding, reduced motion and
Android animator-scale zero are retained; GIFs no longer auto-freeze after idle.
The saved-only Hug pose remains exclusive to successful saves. Widgets remain
static. Overlay artwork is cached per pose rather than decoded every cycle.

The behavior description below records the earlier quiet-motion revision; this
section supersedes its automatic-sleep and timing rules.

Active-motion validation: required host checks passed. S25 passed the updated
playful-pose/tap test, reduced-motion persistence and overlay snooze/resume/stop
test. Overlay window titles now include the application ID so the test cannot
mistake a running production Pet for the isolated test Pet. The first overlay
assertion failed because of that ambiguous title, then passed with isolation.

Design source: `design/PIE-Pet-R5.fig`, page `1245698939:520`.
The approved six-pose artwork is exported without redrawing into seven visual
resources (six original tiles plus a generated closed-eye blink frame).

## Ownership

- `core:domain/pet`: immutable preferences, repository contract, inactivity,
  celebration and overlay visibility rules. Kotlin/JVM only.
- `core:data/pet`: separate Preferences DataStore and bounded local image import.
  Diary schema, archives, migrations and write ordering are unchanged.
- `core:designsystem`: reusable artwork resources and resource/bitmap access.
- `feature:pet`: ViewModel, settings UI, in-app motion, native overlay controller
  and RemoteViews rendering. No dependency on another feature or core:data.
- `app`: constructor wiring, widget receivers, service, PendingIntents and routes.
  Home receives its companion as a composable slot, not a feature import.

## Behavior

The in-app bird breathes gently, blinks about every 5.5 seconds, waves for 1.4
seconds after a tap and sleeps after 45 seconds without a tap. Only a new
`SaveState.Saved` revision can trigger the short hug celebration. Existing saved
rows, Saving and Failed do not. Activity pause stops animation; reduced motion
and Android animator scale zero use still images. Pet can be disabled in Settings.

Two resizable, static widgets (2×2 and 4×2) show a mascot and a Write Today action.
No diary content is read by the widget. The PendingIntent resolves today's date
when opened and follows the existing PIN/session gate. Widget settings/image
changes request an update; periodic updates refresh the displayed date. Adding
requires the Android launcher's confirmation.

Floating Pet is off by default. The user explicitly grants overlay access, then
starts the special-use foreground service. There is no boot-start receiver. The
notification and tap menu expose snooze/stop controls. Snooze lasts one hour and
is persisted; the service remains paused so it can resume. It hides inside PIE,
when the screen is off, and while Android keyguard is locked. It does not use
accessibility services or inspect other apps. Dragging snaps to the nearest edge;
a visible bottom × target closes it. GIF animation pauses when resting/hidden.

Local image/GIF import copies a bounded 16 MiB file, validates dimensions, builds
a sampled first-frame PNG and commits immutable asset IDs only after both files
are ready. Invalid input leaves the selected image/preferences intact. In-app
and overlay GIF playback uses Android ImageDecoder on API 28+, with a static
fallback on Android 8 and in widgets. Pet images/settings are local and are not
included in the existing diary backup format.

## Verified so far

- Required host command: `gradlew testDebugUnitTest :core:domain:test lintDebug
  assembleDebug`; architecture verification covers all nine modules.
- Four new domain behavior tests and two ViewModel tests passed: sleep/wake,
  genuine completed-save events, reduced motion, rollback-safe time handling,
  snooze/lock visibility, cancellation and failed import state.
- Two data instrumentation tests passed on physical Samsung SM-S938B / Android
  17, serial R5CXC3DMXLD: failed image import preserves the current asset and
  reopening preserves snooze/opt-out settings.

## S25 follow-up after unlocking

Five UI/service cases now passed on the physical S25 across the focused reruns:
live 45-second idle-to-sleep and tap-to-wake; reduced-motion persistence and
chooser dismissal on stop; both real widget bindings/RemoteViews click actions;
external widget entry behind the PIN gate; and the foreground overlay's actual
outside-app window, snooze, resume and stop. Together with the two data cases,
seven Pet device cases passed. An idle screenshot from the test package was
visually inspected.

The external-entry tests exposed an ActivityScenario lifecycle tracking issue
after `onNewIntent` replaced the original launch Intent. The activity now consumes
the requested action without replacing its original Intent, and uses singleTop.
Real PendingIntent clicks and real Home/return transitions passed on rerun.
Window presence is checked through WindowManager because secure overlays may not
appear in the accessibility window inventory. The isolated overlay app-op was
restored at test completion.

Required host checks passed again. The regular app was updated with
`adb install -r` at 16:15 Bangkok time on 5 November 2026; installation returned
Success and cold launch returned Status: ok (393 ms). No user diary data was
cleared, and production overlay access remains an explicit user choice.

Launcher visual sizing, physical drag/GIF appearance, battery measurements and
broad accessibility coverage remain hands-on acceptance items.

## Earlier locked-device attempt

The regular `com.ntdpz.pie` debug APK was updated on S25 with `adb install -r`
at 15:39 Bangkok time on 5 November 2026. Installation returned Success and the
activity launch command returned Status: ok, behind the existing device keyguard.
This is not a visual acceptance result. Version remains 0.7.0 / code 13. The
regular package's overlay access was not granted or enabled automatically.

The first UI instrumentation attempt encountered the S25 secure lock screen
(`NotificationShade`, dreaming keyguard) and no accessible Compose hierarchy.
It did not verify the UI. The isolated test process was stopped; the regular
diary package was not cleared or uninstalled.

`PetFlowTest` covers live sleep/wake, preference recreation, chooser dismissal,
both widget layouts and PIN entry. `PetOverlayFlowTest` covers the actual
foreground service, outside-app visibility, snooze, resume and stop; it grants
overlay access only to the isolated test package and restores the app-op afterward.
The tests subsequently passed after unlocking and the fixes described above.
No claim of complete V1 acceptance is made.
