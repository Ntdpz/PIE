# PIE V1.2.0 — theme and companion plan

Status: planning; user-confirmed character and behavior, 2026-11-05.
No runtime implementation or asset readiness is claimed by this document.

## Confirmed inputs

- Mascot and in-app PET share the user-supplied seated dog-ear chibi identity.
- Default character name: PIE.
- Use the supplied images; no additional original files are available.
- Theme direction: derive a 60/30/10 palette from the character.
- Existing daily messages remain deterministic by local date; tapping does not
  reshuffle them.

## Latest source sheets

All files are supplied under `D:/Downloads/`.

| State | Source filename | Intended behavior |
|---|---|---|
| Idle | สไปรต์ชีตสาวน้อยหูสุนัขชิบิ (1).png | Gentle breathing, blinking and ear movement |
| Tap | ชิบิสาวหูสุนัขกับปฏิกิริยาหัวใจ.png | Brief smile, wave or heart, then return |
| Greeting | สติ๊กเกอร์พิกเซลสวัสดีวันนี้.png | Greeting beside the existing daily message |
| Saved | สาวน้อยหูสุนัขฉลองบันทึกสำเร็จ.png | Brief celebration after confirmed persistence |
| Rest | 犬耳ちび少女の休息アニメーション.png | Less motion after inactivity |

These sheets are references and candidate frames, not yet verified production
sprites. Inspect actual alpha, frame bounds, alignment and continuity before use.
The inactivity timeout and frame timings remain implementation proposals.
Rendered greeting and save-status text should remain accessible Compose text;
select suitable frames to avoid duplicate text baked into some supplied images.

## Behavior boundaries

- Do not celebrate Saving, Failed, cancellation or an already-saved entry loaded
  from storage. Do not celebrate on every autosave; define and test the explicit
  one-shot completion boundary before implementation.
- Tap reactions do not queue indefinitely or replace the daily message.
- Pause decorative motion offscreen and in the background. Support reduced
  motion and system animator scale zero, with still-image alternatives.
- Keep PET out of keyboard, primary controls and the session PIN gate.
- No hunger, penalties, mood inference from diary text, network dependency,
  overlay, widgets or feeding/level system in this confirmed scope.

## Design next

1. Inspect original sheets and establish one consistent frame size and anchor.
2. Propose semantic Light/Dark tokens: approximately 60% cream, 30% caramel
   surfaces and 10% rose accents, with charcoal text. Exact values and contrast
   remain to be checked; proportions describe visual area, not every screen.
3. Add a V1.2.0 proposal to the approved Kalka workflow, preserving older pages.
   Start with Today empty and existing-entry states using real daily copy.
4. Review the still design, then motion preview, before expanding to Editor,
   Memories, Calendar, Settings, PIN and backup surfaces.
5. Implement within existing module ownership; screens receive values and
   callbacks. Keep visual rendering in designsystem, rules/contracts in domain,
   persistence in data and wiring in app. Features must not depend on each other.

## Existing archive

`docs/PET_RUNTIME.md` explicitly describes a removed, archived implementation.
Its bird artwork, overlay/widget scope, continuous-motion choices and historical
test results are not acceptance evidence or requirements for this new character.
Preserve existing unrelated/untracked design work.

## Acceptance before release

- Verify save success/failure/cancellation, celebration deduplication, fixed
  editor date across midnight, daily-message stability and lifecycle behavior.
- Verify settings persistence, readable Light/Dark contrast, font scaling,
  TalkBack, real IME/insets and reduced motion on device.
- Any database migration or write-order change needs corresponding tests.
- Run `./gradlew testDebugUnitTest :core:domain:test lintDebug assembleDebug`
  and `pwsh -File scripts/Verify-Architecture.ps1`.
- Report actual results separately from this plan. Do not imply broad device,
  battery or release acceptance from a design preview.
