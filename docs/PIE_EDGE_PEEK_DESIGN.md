# PIE — screen-edge peeking concept, 2026-11-06

<!-- Hallmark pre-emit critique: Philosophy 4, Hierarchy 4, Execution 4, Specificity 4, Restraint 4, Variety 4. -->

The user's new references call for leaning around and gripping the edge, with
the body occluded, rather than only seated gaze changes. The character remains
PIE; the pony/dog references inform the gesture only. Built with the imagegen
tool using the existing PIE artwork and both supplied action references.

Updated board 18 **in the same Kalka page**, retaining root `435921011:1075`:
`design/PIE-V1.2.0-Kalka.fig`, page `0:20`,
`V1.2.0 · PIE Companion · 60 / 30 / 10`. Geometry: 0,4080 / 1049×1400.
The interrupted seated-gaze board's old layers are hidden, not deleted.

[Labeled preview](../design/pie-pet/edge-peek-v1/PIE-Edge-Peek-Concept.png) ·
[Generated source](../design/pie-pet/edge-peek-v1/pose-concept.png) ·
[Full prompt and brief](../design/pie-pet/edge-peek-v1/generation.json)

## Poses

| Position | Proposed gesture |
|---|---|
| Left | Lean inward, grip the vertical edge with both hands |
| Right | Lean inward from the opposite side, grip the vertical edge |
| Top | Playful upside-down head peek, both hands on top edge |
| Bottom | Face rises over the edge, both hands resting on it |
| Top-left / top-right | Diagonal inward lean, hands on both adjoining edges |
| Bottom-left / bottom-right | Diagonal pop-up, hands on both adjoining edges |
| Center | Neutral identity reference; retain the existing runtime idle in production |

All eight edge/corner positions are present, plus the center reference. These
are static sketches with intentionally distinct expressions, not new completed
animation actions. Screen rectangles make contact and occlusion readable.

## Proposed interaction handoff

After dragging to a screen edge, settle before entering the matching peek pose;
release the edge grip and return to normal when dragged inward. Use a small
boundary margin/dwell to prevent left/right/corner flicker. The exact distances
and timing remain tuning choices for S25 testing.

Tapping can blink, smile or briefly play peekaboo. Preserve genuine-save reaction
priority and existing reduced-motion/session-PIN/screen-lock rules. Do not simply
rotate a whole seated sprite, mirror away the hair clip, or abruptly crop existing
full-body reaction frames into edge poses. Purpose-made transition frames and
edge reaction clips are still required for implementation.

## Verification and limits

Reviewed the concept and labeled Kalka board: all eight requested edge/corner
positions are represented. Presentation-panel cropping removes next-row border
leakage, with one shared canvas; `panels.json` records those layout cuts. These
panels contain screen-border artwork and are **not transparent production sprites**.

This is newly generated pose artwork using PIE as identity reference, not an
exact-pixel copy. Before production, review and repair anatomical hair-clip side
(especially upside-down), hands/occlusion, face consistency, alpha edges, scale,
registration and animation continuity against the canonical PET.

Kalka contrast/minimum-text lint reports zero findings for board 18; visible
top-level overlap analysis reports zero findings. Saved the active .fig and
checked SHA-256 equality with the repository copy. No Android code or runtime
assets changed, no device installation, commit or push in this design step.

Pets plugin tools were inspected; they manage ChatGPT Pets, while this request
targets PIE. No pet was uploaded, created, selected or changed in ChatGPT. A v2
Pets atlas or animation-quality validation is not claimed for this concept.
