# Companion UI implementation — 2026-11-06

Implements the user-approved boards 11 and 12 on the same Kalka page;
[design source and rationale](PIE_COMPANION_UI_DESIGN.md). Board 13 supplies actual
service/permission states, not an extra screen in the app.

## Behavior

- My companion: tappable PIE hero with short reactions, real position reset,
  persisted in-app visibility and reduced-motion switches, separate outside-app
  card and three visual pose shortcuts.
- All poses: nine labeled visual tiles, selected outline, replayable preview,
  explicit return and Android Back support. Selection is restored on recreation;
  exploring poses does not write settings or trigger diary-save confirmation.
- Playback pauses when the screen is not resumed or the session is locked.
  Reduced motion and Android animation scale still control playback.
- A composition-scoped sprite group decodes one atlas pair for the gallery;
  thumbnails do not each allocate another copy. The hero scales the same atlas
  proportionally inside its preview without changing floating PET dimensions.
- Companion now follows the app's resolved Light/Dark/System selection, including
  content colors, surrounding scaffold and system bar icons. Existing diary colors
  remain. See the Dark Mode fix below; it supersedes the initial light-only scope.
- An explicitly injected app-scoped `PetOverlayStatus` reports service feedback.
  The UI disables start while starting and shows running only after atlas and
  preferences have loaded. Stop is confirmed by service teardown. Failure survives
  teardown, while retry clears the error. No persisted running flag or global
  service locator is introduced.
- The overlay permission result rechecks the actual grant; cancellation stays
  off. Granting continues to notification permission when needed, then starts the
  foreground service. Existing notification/long-press stop behavior remains.

## Verification

Final verification: required `testDebugUnitTest :core:domain:test lintDebug
assembleDebug` passed; 76 JVM tests, zero failures/errors. Architecture script
passed all nine modules. Logs: `work/companion-ui-build.log`.

S25: eight focused instrumentation cases passed, split by granted/denied overlay
permission. Four Companion cases cover all nine previews without settings/save
side effects, persisted switches/hiding, drag/recreation/reset, and ordinary
screen touches without blocking navigation. Two granted-permission cases verify
confirmed UI start/stop and outside visibility/notification stop. Two denied
cases verify no window without permission and cancellation versus actual grant
on returning from Settings. An initial permission-test assertion incorrectly
expected Off after a previous failure; it now correctly checks cancellation
preserves the prior inactive state, including its error. Final runs passed.
Logs: `work/companion-ui-s25.log`, `work/companion-ui-s25-permissions.log`.

Reviewed final S25 [main](../design/companion-ui/S25-Companion.png) and
[poses](../design/companion-ui/S25-Poses.png) captures. This caught and fixed inherited
light text when entering from the dark app theme and a ripple obscuring the hero.
The regular APK is `work/PIE-companion-ui-debug.apk`, installed with `adb install -r`
as an update, then launched on S25. No uninstall/data clear of the regular app.

Instrumentation uses
the separate `.devicetest` package; regular app data is not cleared. Only the S25
serial `R5CXC3DMXLD` is targeted; no LD/emulator commands are used.
The temporary test packages were removed after verification. The regular package
was confirmed resumed and focused after installation. Installed APK SHA-256:
`02A0DC9EC9E9B3B571573C7289AF39D4613E827BEF4A28947F539F81206910E3`.

Scope is this Companion implementation. It does not claim completion of the
entire V1.2.0 redesign or broad device, font-scale and TalkBack acceptance.
No database/schema or diary write ordering changes; no commit or push.

## Dark Mode correction — 2026-11-06

The initial Companion theme forced cream backgrounds and light system-bar
appearance even when the app selected Dark. `PieTheme` now exposes its resolved
dark/light choice through a design-system composition local. Companion consumes
that choice rather than independently reading the device theme, so explicit app
overrides work as well as System mode.

Dark palette: charcoal `#302A32` background, `#3B333C` cards, `#705449` warm brown
companion/pose surfaces, cream text and `#F3A8C0` rose actions/selection. The light
palette remains unchanged. Both My companion and its nine-pose explorer follow
the same local scheme. Removed the navigation callback that forced light system
bars for this route; the activity uses the resolved app theme consistently.

S25 focused regression passed: persisted Dark, Light and System overrides changed
actual captured background pixels on both screens while remaining on the route.
Reviewed [Dark companion](../design/companion-ui/S25-Companion-Dark.png) and
[Dark poses](../design/companion-ui/S25-Poses-Dark.png) screenshots for readable
headings, body copy, switches, buttons and selected-pose outlines. Logs:
`work/companion-dark-s25.log`, `work/companion-dark-build.log`.

Required Gradle unit/domain/lint/APK checks passed, as did architecture verification
for all nine modules. Installed `work/PIE-companion-dark-debug.apk` on S25 with
`adb install -r`, launched the regular app and removed the temporary test packages.
No regular-app data was cleared. This does not claim broad OEM, font-scale or
TalkBack acceptance.
