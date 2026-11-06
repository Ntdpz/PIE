# PIE Pet · R5 design proposal

Implementation follow-up: see `../../docs/PET_RUNTIME.md`. The verification notes
below describe the original design-only handoff, not later runtime acceptance.

Native source: `../PIE-Pet-R5.fig`, Kalka page `1245698939:520`.
The original application design source is retained. This file is an additive
design snapshot, not a runtime release.

The proposed mascot name is **น้องพาย**: an original cream bird with sage wings,
terracotta neckerchief, peach cheeks and two head feathers. The user-supplied bird
sticker sheet informed mood and proportions; the new asset was generated for this
proposal. `mascot-poses-v1.png` has transparency and a 3-column × 2-row grid:
idle, wave, hug diary, sleep, happy hop, edge peek. These are six concept poses,
not consecutive animation frames or a finished animated GIF.

## Artboards

| ID | Design |
|---|---|
| 1245698939:521 | In-app Pet, Light |
| 1245698939:542 | In-app Pet, Dark |
| 1245698939:563 | Home-screen widget, 2×2 and 4×2 concepts |
| 1245698939:589 | Floating Pet preview and permission entry |
| 1245698939:611 | Companion settings for all three surfaces |
| 1245698939:655 | Character sheet and six behavior directions |
| 1245698939:695 | Built-in mascot, local image and GIF selection |
| 1245698939:715 | Overlay tap menu, drag, snooze and dismissal |

Text, surfaces and controls remain editable layers. Mascot layers use the same
embedded transparent sheet with editable clipping frames; artwork is raster.
The design inherits PIE colors, Noto Sans Thai and Lucide navigation icons.

## Behavior handoff

- In-app: idle breathing/blink, a short wave on tap, a one-shot happy gesture only
  after a genuine completed save. No streak penalties or demanding messages.
- Widgets: static image first, opens the app through the existing session gate;
  no diary prose on the launcher. Grid sizes are conceptual: validate actual
  Samsung launcher dimensions and any animation support on S25.
- Floating Pet: opt-in overlay permission, draggable edge placement, tap menu,
  snooze and hide. Hide during device lock/PIN gate and avoid editable content.
  A foreground-service notification and platform lifecycle behavior need an
  implementation review before promising availability outside the app.
- Local image/GIF selection: preview before apply, local assets, still-image
  fallback for widgets, reduced motion and a way to disable each surface.

All eight artboards were visually inspected in Kalka. Font diagnostics reported
faithful rendering with no substitution issues. No runtime widgets, overlay
permission requests, animation assets, device tests or application code changes
were implemented in this design-only increment.
