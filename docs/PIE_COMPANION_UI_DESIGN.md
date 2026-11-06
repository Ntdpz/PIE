# PIE Companion UI — 2026-11-06

Approved by the user and implemented afterward; see
[Companion runtime and S25 verification](PIE_COMPANION_UI_IMPLEMENTATION.md).
The notes below record the design-stage scope and checks.

Design-only revision in the existing `design/PIE-V1.2.0-Kalka.fig`, page
`0:20` — `V1.2.0 · PIE Companion · 60 / 30 / 10`.
The ten existing phone artboards retain their IDs and arrangement. Three new
boards sit below them at y=2040. This document describes the proposed interface;
it does not claim that these screens have been implemented in Android.

## Editable boards and previews

| Board | Node | Position / size | Preview |
|---|---|---|---|
| 11 · เพื่อนของฉัน · PIE | `435921011:8` | 0,2040 / 390×844 | [Main](../design/companion-ui/PIE-Companion.png) |
| 12 · ลองท่าของ PIE | `435921011:202` | 470,2040 / 390×844 | [Poses](../design/companion-ui/PIE-Poses.png) |
| 13 · Companion · สถานะและเหตุผล UX | `435921011:333` | 940,2040 / 710×844 | [States](../design/companion-ui/PIE-States.png) |

## Visual system

- Cream `#FFF4E8`: primary background and reading surfaces, 60% role.
- Caramel `#E6C7A6`: companion preview and secondary surfaces, 30% role.
- Rose `#C64F78`: main action, selected pose outline and active switch, 10% role.
- Charcoal `#302A32`: text and icons. Noto Sans Thai; Lucide back/chevron icons.
- 60/30/10 describes intended visual emphasis, not a measured pixel-area ratio.
- Exact character crops from current `pie_companion.png` and `pie_reactions.png`;
  no character redraw. Transparent crops are in `design/companion-ui/assets/`.

## UX decisions

Show PIE and a visible tap instruction first, then frequent controls, then the
outside-app mode. Keep one prominent rose action per screen. Separate the in-app
switch from outside-app activation so their scopes can be understood independently.
Put the permission explanation beside the action that requests it.

The main screen previews three poses. “ดูทุกท่า” reveals all nine with an image
and a Thai name, reducing the need to remember an unlabeled gesture or icon.
Selecting a pose previews it without changing preferences. The return action sits
near the bottom; back and reset remain explicit. These are design hypotheses,
not measured claims about user performance.

References: [recognition rather than recall](https://www.nngroup.com/articles/recognition-and-recall/)
and [Android accessible touch targets](https://support.google.com/accessibility/android/answer/7101858?hl=en).
Interactive hit areas are designed at least 48 units; the phone canvas uses
logical design units and still needs device verification.

## Interaction handoff

- Tapping the hero plays a short reaction, then returns to idle. “คืนตำแหน่ง”
  resets the companion position; it must not reset diary data or preferences.
- In-app visibility and reduced motion reflect persisted settings. Reduced motion
  uses a static pose; preview behavior must respect this preference as well.
- The daily message remains stable for its day. Preview/tap does not reroll it.
- Genuine save confirmation may trigger celebration once for that save action;
  repeated autosaves must not repeatedly celebrate.
- Outside mode: off → permission when needed → starting → running. If permission
  is already available, skip the permission detour. Returning from Android settings
  requires rechecking the actual grant. Cancellation remains off.
- Starting disables repeated requests. Running exposes “หยุด PIE นอกแอป”. Failure
  shows a clear retry action. Status text accompanies color; never show running
  before the service confirms it. The reference board illustrates these states,
  not a working prototype or Android system permission dialog.
- All nine pose options: wink, ears up, head tilt, nod, shy, cheer, stretch,
  peekaboo and heart. Thai labels appear on the pose board.

## Verification and remaining work

Reviewed all three screenshots after final edits. Kalka reports faithful font
availability and zero visible top-level overlaps on the existing page.
Accessibility lint on the two phone boards reports one contrast finding and no
other findings: cream on rose is 4.04:1 on the main action. The label is deliberately
20 px / 700; the lint uses a 4.5:1 threshold and does not account for its large-bold
classification. Do not report this as a zero-finding lint run. The same palette
and bold action typography are used on the state reference board.

Saved the active .fig and mirrored it into the repository with SHA-256 equality.
PNGs are exported at 2×. The original ten screens were not redesigned in this pass.
Runtime interaction, scrolling/insets, large fonts, TalkBack, focus order,
permission-return behavior and S25 acceptance remain for implementation/testing.
No Android source edits, Gradle/device checks, commit or push were performed for
this design-only revision. Existing companion code changes in the worktree are
separate work and were preserved.
