# PIE · editable Pen source

Open **[PIE-v0.4.pen](PIE-v0.4.pen)** for the saved v0.4 design: uppercase PIE, lower PIN keypad and light/dark theme variables. The user saved this original document on 3 November 2026. The v0.4 mockup photos retain their original online URLs; local photo copies are available in `assets/`. [PIE.pen](PIE.pen) remains the earlier R2 reference with local image paths.

This is the original active PIE document, including the earlier explorations and the R2 screens. Node IDs match [the design reference](../docs/DESIGN_REFERENCE.md). Keep the entire `design/` directory together when copying it.

| File | Purpose |
|---|---|
| `PIE.pen` | Editable screens, layers, tokens and earlier iterations |
| `PIE-logo-v1.png` | Original generated PIE logo referenced by the canvas |
| `assets/memory-01.jpg`, `assets/memory-02.jpg` | Original mockup photos, bundled for offline image resolution |
| `assets/sources.json` | Original source URLs for those photos |

Only image URLs were changed to relative local paths; the screen layout, node IDs, text and colors were preserved. Photos are sample design imagery, not private diary entries or APK content. The app uses the user's own selected media at runtime.

The design uses Noto Sans Thai. The font and its license are already bundled in `core/designsystem/src/main/res/font/` and `docs/licenses/`. Install the font locally if your Pen environment does not have it.

The R2 PNG exports in `docs/design-reference/` remain the visual reference. Compose motion and real loading/error/security behavior live in the Android source; a `.pen` canvas is the editable visual design.

Run `pwsh -File scripts/Verify-Design.ps1` from the repo root to check the JSON, all eight approved R2 frames and referenced local image files.
