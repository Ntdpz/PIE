# PIE edge-peeking implementation — 2026-11-06

Implements approved board 18 in `design/PIE-V1.2.0-Kalka.fig` on the existing
V1.2.0 Companion page. The eight transparent edge/corner cutouts are separate
production artwork; the concept panels with drawn screen borders are not used.

## Behavior

- Dragging near an edge snaps and persists the position. Domain-only thresholds
  use 8% entry and 14% exit hysteresis to prevent flicker.
- Idle/rest uses the corresponding grip pose after settling. Touch, greeting,
  saved and stretch reactions take priority, then return to the edge pose.
- Both Compose and native Android overlay use the same registered atlas.
  Entry fades/slides for 180 ms; gentle breathing anchors at the grip edge.
  Reduced-motion/disabled-animation settings are respected.
- Companion pose explorer adds eight edge previews alongside nine reactions.
  Preview selection never moves the saved companion. Dark/light/system themes
  remain supported.

`scripts/prepare_pet_edges.py` records one shared scale and eight anchors in
`design/pie-pet/edge-peek-v1/runtime/registered-poses.json`. The upside-down
top pose was repaired to preserve the anatomical hair-clip side. Edge artwork
is a static pose with runtime breathing/entry motion; dedicated multi-frame
grip/reaction transitions are not implemented.

## Verification

- Required `testDebugUnitTest :core:domain:test lintDebug assembleDebug` passed.
  Current module XML reports contain 82 JVM tests, zero failures/errors.
- `scripts/Verify-Architecture.ps1` passed for all nine modules.
- Ten distinct Companion/overlay device cases were exercised on S25. The first
  combined run passed eight and failed two merged-semantics test lookups. After
  correcting the lookups, those two passed. Three final preview/theme cases
  passed after the viewport/ripple adjustment. This is not a claim of one clean
  final ten-case run. Logs: `work/pet-edges-s25.log`,
  `work/pet-edges-focused-s25.log`, `work/pet-edges-preview-s25.log`.
- Installed `work/PIE-edge-peek-debug.apk` with `adb -s R5CXC3DMXLD install -r`:
  Success. Explicit MainActivity launch returned Status ok (warm, 47 ms).
  Existing app data was retained; isolated device-test packages were removed.
  No emulator was used. Version remains 0.7.0, code 13.
- APK SHA256: `17C1BD689FF5430BF116A6AF2F2BB167226B7A2D9FA0F2BC0A614591C84F6977`.

## Separate ChatGPT Pets deliverable

New custom PIE created and verified in the complete Pets list:
`pet_6aede7ac2ba08191b88f3fb8a3b366e4`. Existing pets and active selection are
unchanged. This uses the supported nine animation states plus sixteen look
directions, separate from Android's edge-position renderer.

The final 1536×2288 PNG passed bundled structural/quality gates, three-reviewer
blind cardinal checks, and service validation. Final encoded motion previews
were displayed before upload. Three intermediate directional components remain
subtle (202.5 horizontal, 112.5/292.5 vertical); all cardinal checks pass.
Continuity reports retain measured row-seam/scale warnings. A suspected missing
blind-review image was rechecked using an unmodified pair crop by all reviewers;
original and corrected reports are preserved.

Sources, prompts, generated rows, QA, previews and stable identity are retained
under `design/pie-pet/chatgpt-v2/`. Library tools were unavailable, so these
artifacts have **not** been synchronized to ChatGPT Library. No Git commit/push
was performed for this implementation.
