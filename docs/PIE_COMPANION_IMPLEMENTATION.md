# PIE in-app companion

This increment implements the PET portion of the V1.2.0 plan, including an opt-in
outside-app mode added on 2026-11-06. The full
theme redesign and release version bump are separate; the app version remains
0.7.0. Existing archived bird/overlay notes in PET_RUNTIME.md are not this implementation.

## Behavior

- PIE floats on Today, Gallery and Settings. Dragging persists a bounded,
  normalized position, which adapts to the available content area after rotation.
- Tap plays a short reaction without changing the date-selected catalog message.
  The first foreground greeting of a local day uses ReminderMessages.defaultForDay.
- After 45 seconds without interaction the companion enters its quieter rest
  animation. Tap wakes it. No diary-content analysis or online service is used.
- Long press exposes hide/settings. Accessibility actions also expose hide,
  settings and movement in four directions. Settings can show PIE again, reduce
  motion, or reset its position.
- A save celebration occurs once after leaving an edited entry, when its text
  write is confirmed successful. Ordinary autosaves while typing do not celebrate.
  A pending write waits for the selected date's successful state. Failed writes,
  cancellation, previously saved entries and unrelated dates do not claim success.
- Media-only edits report success only after the existing import/delete operation
  returns successfully. A failed/pending text save still prevents celebration.
- PET is absent in Editor, memory reader, PIN, backup and its own settings route.
  Dialogs dismiss on stop. Animation work stops when backgrounded or locked;
  reduced motion and system animator scale zero use still poses. A late completed
  write from a paused session is not replayed as a greeting or success event.

## Ownership and persistence

- core:domain: preferences/position contract, timing policy and save-visit policy.
- core:data: LocalPetRepository, with separate pie_companion Preferences DataStore.
  Field updates are atomic and independent; position coordinates are sanitized on read.
- core:designsystem: sprite atlas, visual player and system animation-scale observer.
- feature:pet: immutable UI state, ViewModel, draggable companion and settings.
- app: explicit constructor wiring and route/session visibility. Editor sends a
  completion callback for successful media operations without importing feature:pet.

No diary Room schema, archive format, serialized writer ordering or INTERNET
permission changed. Pet preferences are local and are not part of diary
backup files. Accepted preference commands drain through the application scope
even if the settings ViewModel is cleared; failures leave previous stored values intact.

## Artwork

The five user-supplied seated-character sheets are deterministically processed by
scripts/prepare_pet_sprites.py. No new character was drawn. The preparation crops
grid borders and baked greeting speech, removes pale neutral background pixels
connected to each frame's outside edge, then registers each strip with one shared
crop/scale. Enclosed white eyes/clothing are retained. The atlas is transparent
RGBA, 1536 x 1040: five rows of eight 192 x 208 frames, ordered Idle, Tap, Greeting,
Saved, Rest. It is an Android-specific atlas, not a ChatGPT Pets v2 upload sheet.

Regenerate with Python + Pillow:

```powershell
python scripts/prepare_pet_sprites.py --source-dir D:/Downloads --output core/designsystem/src/main/res/drawable-nodpi/pie_companion.png --preview work/companion/contact.png
```

The generated contact.json records source filenames/hashes, source frame choices
and each strip's registration. The contact sheet and an emulator runtime screenshot
were visually inspected. Final physical-device animation smoothness and edge
appearance still require hands-on acceptance.

## Verification

- Required Gradle unit/domain/lint/APK checks passed; 67 JVM tests, no failures.
  New coverage includes eight domain cases, five PetViewModel cases and an editor
  media-completion case. The existing writer/date/failure tests remain passing.
- Verify-Architecture.ps1 passed for all nine modules.
- Existing emulator emulator-5554, Android API 34, isolated app
  com.ntdpz.pie.devicetest: two companion UI cases passed across focused runs
  (drag/recreation/navigation; stable message/hide/re-enable/recreation).
- The existing PIN background/incorrect-code/re-unlock case passed with an added
  assertion that PET is absent below the locked semantics tree.
- Two core:data instrumentation cases passed: reopen retains position, visibility
  and motion; invalid stored coordinates are sanitized. Tests use temporary
  preference files, not the regular application's settings or diary.
- The first UI harness needed a Unit-return correction; the recreation assertion
  was corrected to respect restored navigation to PET Settings before returning.
  Production behavior passed the corrected focused reruns.
- Instrumentation APKs compile. Tests were installed only into isolated packages;
  the regular diary app was not installed, cleared, uninstalled or modified.

Runtime preview: work/companion/runtime-preview.png. Logs: work/companion-build.log,
work/companion-isolated-build.log and work/companion-*-device.log (local ignored artifacts).
Physical S25, broader IME/TalkBack/font-scale coverage, animator-scale changes on
device and battery measurements are not claimed by these initial tests. Subsequent
S25 coverage is recorded below; publication is tracked in Git history.

## Outside-app mode — 2026-11-06

- PET Settings offers a system overlay permission link, explicit Start and Stop.
  Notification permission is requested before Start so a visible stop control is
  available. Start is user initiated from the foreground; no boot/autostart path.
- `feature:pet` owns an abstract PetOverlayService; the app subclass only provides
  PetRepository and the application write scope. `core:designsystem` owns the
  native atlas-rendering View. No dependency on diary content, editor, or SQL.
- The small TYPE_APPLICATION_OVERLAY window consumes touches only in its own
  bounds, supports tap, drag, accessibility click/long-click, and long-press stop.
  It uses the same saved normalized position and reduced-motion setting as the
  in-app companion. Outside mode is independent of the in-app visibility toggle.
- Outside animations are Idle, Tap and Rest after 45 seconds; saved-entry messages
  and diary text are never displayed over another app. The window is removed on
  return to PIE, screen off or keyguard lock. Detached rendering stops callbacks.
- A specialUse foreground service declares SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE
  and FOREGROUND_SERVICE_SPECIAL_USE. Its ongoing notification opens PIE or stops
  the service. Explicit stop, task removal or process death do not auto-restart it.
- Overlay permission loss stops the service; startForeground is acknowledged before
  checking permission to handle a revocation between UI and service startup.
  Android can also terminate the process when permission is revoked.
- S25 (SM-S938B, Android API 37), isolated `.devicetest` package: focused tests
  passed for outside visibility/return-to-app/notification stop command, drag and
  saved position across service restart/long-click stop, explicit stop, and denied
  permission. Test permissions are configured before instrumentation, since
  revoking overlay permission inside its process can terminate the test itself.
- An initial S25 visibility wait timed out; the focused rerun passed. The final
  combined S25 run passed five cases (three overlay plus two in-app companion
  regressions); the denied-permission case passed separately. Logs are in
  `work/companion-overlay-s25-*.log`. Required Gradle and architecture checks passed.
- From the user's instruction onward, only S25 is used. Do not run LD/emulator
  commands for this task. Screen-lock cycling, long battery measurements and Play
  Store special-use review are not claimed by these focused tests.

### Outside-touch interaction

The overlay combines FLAG_WATCH_OUTSIDE_TOUCH with FLAG_NOT_TOUCH_MODAL. An
ACTION_OUTSIDE starts a short visible reaction and refreshes the inactivity timer;
repeated touches restart it even when the previous reaction is still playing.
Only the signal is used: no outside coordinates, app contents, gesture paths or
touch history are read or persisted. The small overlay does not cover the screen
or intercept the underlying window's gesture. Reduced motion still uses a pose.

This is a start-of-gesture notification, not every MOVE/UP or every finger in a
multi-touch gesture. Higher system windows (including IME/status areas), hidden
overlays and protected screens are outside the guarantee. No Accessibility service
or additional permission is added. See Android's
[FLAG_WATCH_OUTSIDE_TOUCH reference](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_WATCH_OUTSIDE_TOUCH).

The S25 focused test performs three real taps outside PET, confirms each reaction,
and verifies all three clicks reach a separate-process counter window. That probe
is non-exported and enabled only with `-Ppie.deviceTest=true`; release has no probe,
and the regular debug APK disables it. Initial test harness issues were resolved:
the separate test APK cannot load the target APK's deduplicated Kotlin runtime, so
the probe uses a separate process in the isolated target APK; native Button case
transformation was disabled to make its counter assertion stable. The production
outside-touch handler itself passed the corrected test on S25.
The final combined S25 run passed all four overlay cases (outside touches,
visibility/notification stop, explicit stop, drag/restart/long press). Required
Gradle unit/domain/lint/APK checks and the nine-module architecture check passed.
The regular APK was installed as an update on S25; isolated test packages are
removed after verification. Logs: `work/companion-touch-final-build.log` and
`work/companion-touch-s25-final.log`.
