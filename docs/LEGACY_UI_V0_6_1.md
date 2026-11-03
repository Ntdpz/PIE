# PIE 0.6.1 — secondary screen consistency

<!-- Hallmark · pre-emit critique: P4 H4 E4 S4 R4 V4 -->

Continue the approved R3 personal-journal system in DESIGN.md. No route, database schema or archive-format changes.

- Material surface/outline/error roles now use warm paper and brown neutrals in Light and Dark, including menus and sheets that previously inherited purple-grey defaults.
- Calendar sheet uses a compact, vertically aligned header, centred month, transparent unselected cells and 48 dp cell height. Dates without entries still open the editor for that exact date.
- PIN options/help and diary/media deletion decisions share a paper dialog with smaller heading and scrollable details. Destructive actions use the error token. Data-loss and backup-password guidance remains present.
- Reminder time editing uses a themed 24-hour Compose input. Saving still updates the existing time set; cancelling/backgrounding closes without saving.
- Emoji selection wraps according to available width with 48 dp targets. Selected emoji still inserts at the editor selection.
- Historical-date delete/open labels no longer imply that every displayed page is today's entry.

Verification: Gradle unit/domain tests, Android lint, debug assembly and the architecture boundary script passed. The emulator UI suite passed 12 cases. After the final emoji adjustment, 3 affected editor/settings/PIN cases passed at 320 dp width (840 physical pixels at 420 dpi); the emulator size was restored. No instrumentation tests run against S25 diary data.

The settings/reminder/backup case also passed separately in system Dark mode; Light and Dark time-input screenshots were visually reviewed. Night mode and screen-size overrides were restored. Version 0.6.1 (code 12) was installed over the existing S25 application with `install -r`, package version verified and MainActivity launched.

Evidence logs (local, ignored): `work/legacy-ui-verification.log`, `work/legacy-ui-device-tests.log`, `work/legacy-ui-small-device-tests.log`, `work/legacy-ui-dark-tests.log`. Broad font-scale, TalkBack, IME and device acceptance remain open.
