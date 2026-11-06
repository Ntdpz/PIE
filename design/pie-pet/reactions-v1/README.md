# PIE reaction strips V1

Original character reference: the user's seated dog-ear chibi girl sheet.
`sources/` contains eight separate image_gen animation strips, not sliced stills
from the concept board. `generation.json` records the built-in tool's prompts and
references. No ChatGPT Pets service was used for these Android assets.

Each source is transparent RGBA, 2172 × 724, with eight chronological poses in
one horizontal row. Runtime order is wink, ears_up, head_tilt, nod, shy, cheer,
stretch, peekaboo. The existing heart action stays in the original atlas.

Rebuild deterministically with Python + Pillow:

```powershell
python scripts/prepare_pet_reactions.py --source design/pie-pet/reactions-v1/sources --output core/designsystem/src/main/res/drawable-nodpi/pie_reactions.png --review design/pie-pet/reactions-v1
```

The result is eight rows of eight 256 × 256 cells. Registration reads the actual
original idle silhouette from `pie_companion.png` and fits both neutral width and
height, preserving its center and foot baseline. The old nominal 176-pixel
width-only fit made new poses approximately 15–23% taller and has been replaced.
Neutral size/baseline assertions guard against this regression; alpha below 100
is ignored only for these measurements. `size-comparison.png` shows the old idle
and all eight neutral poses at the same rendering scale.

One transform per row preserves
the head tilt, nod and cheer's vertical displacement; no frame is independently
stretched to fill a cell. `registration.json` records source/atlas hashes, shared
crops, scale, unique-frame counts and all frame bounds. No frame touches an atlas
cell edge. `contact.png` was inspected for identity, complete limbs and distinct
temporal poses. `preview.gif` displays the eight rows at 128 px with a shared
140 ms preview cadence; runtime uses each action's own timing.

The virtual rendering canvas is 256 square. The original 192 × 208 artwork is
centered and bottom-aligned inside it, so increasing the interaction tile to
128 dp does not enlarge the original visible character. New strips have room
for hands and a hop. Seven focused cases passed after S25 reconnected, including
all-action previews and outside-touch no-repeat/click delivery. The regular APK
was installed as an update and the overlay re-enabled; no battery measurements
or broad accessibility coverage are claimed.
