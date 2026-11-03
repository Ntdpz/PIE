# PIE v0.4.0 — appearance and PIN

- PIN keypad moves to the lower half using flexible space, with scroll fallback on short viewports. Security gate remains opaque and callbacks immediate.
- Settings offers System / Light / Dark. Theme preference persists separately in DataStore and retains existing reminder/PIN preferences.
- Uppercase PIE wordmark uses the original exported heart symbol and editable theme-aware text. Editor background, saved-state colors and Android system icons follow the theme.
- Unit checks, lint and Debug build passed. All 7 app UI cases passed, including theme persistence across recreation and keypad geometry in both themes. After correcting the PIN title color, both appearance cases passed again.
- All 5 data instrumentation cases passed, including theme preference retention and unchanged reminder/PIN state. Architecture boundaries passed.
- S25 installation has not been performed for this version; the user disconnected the device.
- Pen previews were exported for light/dark PIN, home and settings. The user saved the original Pen document on 3 November 2026; that saved file is bundled as `design/PIE-v0.4.pen`. It includes themed variables and light/dark PIN variants. The older `design/PIE.pen` is retained as the v0.3 reference. The v0.4 file retains the original remote mockup photo URLs; the logo is local and both photo copies remain available in `design/assets/`.

No database schema, encryption or diary-writing behavior changed. Broad device, font-scale, TalkBack and frame-pacing acceptance remain outstanding.
