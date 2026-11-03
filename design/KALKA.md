# PIE · Kalka design source

Open [PIE-Kalka.fig](PIE-Kalka.fig) in Kalka. This is the design source for future work, migrated through Kalka MCP on 3 November 2026.

| Page | Artboards |
|---|---:|
| PIE · Merge · Light & Dark | 8 |
| PIE · App UI | 10 |
| PIE · Earlier explorations | 6 |

All 24 artboards preserve the existing screen layouts. Merge includes review, per-day text choice, confirmation and completion in Light and Dark. These are design proposals; migration does not release the Android Merge feature.

Text and layout layers remain editable. Icons and existing picture assets are individual image layers, rather than editable vector paths. Screens are not flattened into a single image. Kalka's HTML importer was used because its scripted renderer was blocked by the application's CSP; security settings were not changed.

Two color collections, `PIE · Light` and `PIE · Dark`, contain nine variables each. Matching solid fills and strokes are bound to these variables. Noto Sans Thai and Fahkwang were checked through Kalka font diagnostics with no substitutions reported.

The original `.pen` files remain archived for traceability. Use [kalka-migration-map.json](kalka-migration-map.json) to map original Pen node IDs to Kalka artboards. The Android implementation still owns runtime motion, loading, device insets and accessibility behavior.
