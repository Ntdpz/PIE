# PIE reaction expansion — concept V1

Date: 2026-11-06. The original concept below is now implemented in the working
tree using eight separately generated motion strips. See the implementation
section for verified host and S25 device checks. No new Git
publication is included in this implementation step.

## Character and artwork

Keep PIE's seated dog-ear chibi girl identity, near-black bob, caramel ears, brown
dog nose, pink polo with navy collar, charcoal pants and white shoes. The reference
is the user's `สไปรต์ชีตสาวน้อยหูสุนัขชิบิ (1).png`. The original is preserved.

Concept board: `design/pie-pet/reactions-concept-v1.png` (1536 × 1024). Generated
with the built-in image_gen tool using the reference image; not a Pets-service
upload. It was visually inspected for eight distinct poses, matching character
identity, complete framing and readable labels. This board is for pose selection,
not deterministic sprite extraction or claims of animation readiness.

The selected Pets plugin creates companions in ChatGPT Work mode; PIE is an
Android app with its own renderer and state contract. No ChatGPT pet was created,
uploaded, selected or changed.

## Proposed actions

| ID | Action | Motion plan | Suggested duration | Use |
|---|---|---|---|---|
| 01 | Wink | Close one eye, lift a small V-sign, return hands to knees | 0.7–1.0 s | Common touch response |
| 02 | Ears up | Perk ears, attentive eyes, settle ears back down | 0.4–0.7 s | Frequent/rapid touches |
| 03 | Head tilt | Tilt the head, one ear follows, gently straighten | 0.8–1.1 s | Common touch response |
| 04 | Nod | One small nod and thumbs-up, return to neutral | 0.5–0.8 s | Frequent/rapid touches |
| 05 | Shy | Hands to cheeks, small blush, bashful smile | 1.0–1.4 s | Occasional touch response |
| 06 | Cheer | One tiny seated hop with spread arms, settle on the same baseline | 1.0–1.4 s | Rare playful response; not a save-success signal |
| 07 | Stretch | Clasp hands overhead, stretch gently, return to seated neutral | 1.2–1.8 s | Once during a quiet interval, before the existing 45 s rest |
| 08 | Peekaboo | Cover one eye, peek through fingers, lower the hand | 0.9–1.2 s | Occasional touch response |

## Selection and playback proposal

- Keep the existing heart/wave animation as another option. Select a whole action
  sequence, rather than treating arbitrary frames of the old sequence as new actions.
- Do not repeat the same selected action immediately. Use short ear/nod/wink
  reactions during rapid interaction so the pet stays readable while typing.
- Choose a fresh action from neutral; a touch arriving during a reaction refreshes
  activity and gives a brief visual acknowledgement, without queuing long clips.
- Outside-touch signals do not provide a reliable screen target: head tilt is an
  expressive pose, not a promise to track the exact finger location in other apps.
- Stretch is a quiet one-shot, not a loop; new touches interrupt it. Keep existing
  lifecycle hiding, system/reduced-motion behavior and rest policy.
- Save-specific effects and text remain gated by actual persistence. A playful
  cheer outside the app must never claim the diary was saved.
- All reactions must return to the same neutral scale, center and seated baseline.
  Production work needs separate transparent motion strips and normal-size QA.

## Implementation and verification

- Eight distinct image_gen strips, eight temporal frames each, are preserved in
  `design/pie-pet/reactions-v1/sources`. Their exact prompts are in `generation.json`.
- `scripts/prepare_pet_reactions.py` registers all 64 frames into the transparent
  2048 × 2048 atlas `core/designsystem/src/main/res/drawable-nodpi/pie_reactions.png`.
  It uses one crop/scale per strip, checks frame bounds, eight unique frames in
  each row and a real elevated cheer frame. Registration/hashes and a contact
  sheet/motion preview are kept with the source strips. Contact sheet reviewed.
- `core:domain` owns PetReactions: no immediate repeat, fast-touch short pool,
  sequence IDs, action expiry, one quiet stretch at 20 seconds and no stretch
  after the 45-second rest boundary. Interrupted clips are replaced, never queued.
- Both Compose and the native overlay use the same row/timing metadata and
  action selection. Original heart/greeting/save/rest assets remain available;
  real save confirmation is separate from playful cheer. Extra transparent space
  allows raised hands/hops while preserving the original artwork's visible size.
- PET Settings offers live buttons for all eight new actions and the existing
  heart. Preview state is local UI state and never claims a save occurred.
- Required Gradle unit/domain/lint/APK checks passed: 74 JVM tests, zero failures,
  errors or skips. Includes five new selector cases and two ViewModel integration
  cases. Nine-module architecture check and diff whitespace check passed.
- S25 reconnected after the host checks. The expanded build passed seven focused
  instrumentation cases in the isolated `.devicetest` package: every-action
  preview/no false save success; stable daily message/hide/re-enable; in-app
  drag/recreation; overlay hide/return/notification stop command; outside touches
  with no repeated action and underlying click delivery; explicit stop; overlay
  drag/persistence/long-press stop. Log: `work/pet-reactions-s25.log`.
- Installed the regular APK on S25 with `adb install -r`, preserving the diary,
  removed the isolated test packages and re-enabled the real overlay through PET
  Settings. Foreground service and the live overlay window were verified. No
  LD/emulator commands were used. Long battery, physical gesture timing at all
  animation scales and broad accessibility/device acceptance remain unmeasured.
- Build log: `work/pet-reactions-final-build.log`. Regular package is still
  `com.ntdpz.pie`, version 0.7.0; this is not the full V1.2.0 theme release.

### Size alignment correction

The user noticed that new actions appeared larger. Measurement confirmed the
width-only registration made their neutral poses 15–23% taller than the original
192-pixel-high idle silhouette. Registration now derives both visible dimensions,
center and foot baseline from the original idle atlas. It applies one uniform
scale/placement per action strip, keeping the nod, tilt, raised arms and jump.
Neutral-height (±2 px), maximum-width and baseline assertions run during assembly.
`design/pie-pet/reactions-v1/size-comparison.png` was visually reviewed. Source
drawings, animation selection and stored user settings are unchanged.
Required host checks passed again (74 JVM cases, lint, APK and architecture).
Two focused S25 cases passed for all-action previews and outside-touch no-repeat/
underlying click delivery (`work/pet-size-s25.log`). The corrected regular APK
was installed as an update and the live overlay re-enabled; test packages removed.

### In-app touch parity

The user found the in-app companion less reactive because only its own taps
triggered actions, while the overlay received outside-touch signals. A
feature-owned PetInteractionSurface now observes the initial pointer pass around
the app's Scaffold on unlocked Today, History and Settings routes. It never
consumes a gesture. Registered pet bounds exclude direct pet taps/drag starts
from the observer, avoiding duplicate reactions; disposal clears the bounds.

Screen touches use the same selector as direct taps/overlay, but do not open a
daily-message bubble on every interaction. The daily message stays unchanged.
Confirmed-save feedback is protected from passive touches; hidden, unloaded or
paused companions ignore these events. Editor/PIN/secondary routes retain their
existing visibility rules. Motion preference and artwork scale are unchanged.

Required host checks passed with 75 JVM tests. Six focused S25 cases passed:
real screen taps/no repeat/no message bubble, real scrolling and navigation,
all-action previews, in-app drag/recreation, daily-message/hide/re-enable,
outside-touch delivery, and the existing PIN background lock case (some checks
share a test). Logs: `work/pet-in-app-touch-build.log` and
`work/pet-in-app-touch-s25.log`. No Room or write-order changes.

## Generation prompt

Built-in image_gen, identity reference:
`D:/Downloads/สไปรต์ชีตสาวน้อยหูสุนัขชิบิ (1).png`; opaque concept-board background.

```text
Use case: identity-preserve character concept art. Create ONE polished landscape concept contact sheet for expanding PIE Android companion reactions, NOT a production animation atlas. The input is the exact character identity and pixel-art style reference: a seated chibi GIRL with short near-black bob, floppy caramel dog ears with pink interiors, glossy dark brown eyes, small round brown dog nose/muzzle, pink polo shirt with dark navy collar and placket, charcoal pants, white shoes. Preserve these recognizable details, chibi head/body proportions, pixelated crisp black outline, pixel shading, clothing and colors closely. Do not turn her into a different character or an actual dog, do not add a tail or change her haircut. Design eight DISTINCT NEW readable KEY POSES, one per cell, arranged 4 columns x 2 rows with generous equal spacing and matching character size; full body complete in each cell. Row1 left to right: 01 WINK: playful closed-eye wink and small V-sign near cheek; 02 EARS UP: ears perk slightly, attentive wide eyes, hands on knees; 03 HEAD TILT: pronounced cute sideways head tilt, one ear naturally droops, questioning expression; 04 NOD: friendly eyes-closed affirmative nod with one small thumbs-up near chest. Row2: 05 SHY: blush, both hands cupping cheeks, bashful smile; 06 CHEER: tiny joyful seated hop, both arms spread up, big smile; 07 STRETCH: relaxed seated stretch, both hands clasped overhead, eyes closed with gentle relieved expression, visibly different from energetic cheer; 08 PEEKABOO: one hand half hiding her face, peeking around fingers with mischievous smile. Minimal tiny pixel accent marks only, no big hearts or props. All remain the same seated character with consistent limb anatomy and pixel style, each pose legible at 96px. Design board background warm cream #FFF4E8, very subtle caramel #E6C7A6 panel dividers, rose #C64F78 tiny ID accents, charcoal #302A32 typography. Editorial clean layout with a small header 'PIE / REACTION EXPANSION' and beneath it '8 KEY POSES · CONCEPT V1'. Each cell has its exact two-digit ID and English name beneath the drawing. No Thai text, no UI screenshots, no watermark, no baked checkerboard. These are concept drawings to approve before making frame-by-frame animation; do not depict a motion strip or duplicate eight copies of the same pose.
```
