# PIE engineering rules

Work like a production line: each station accepts explicit input, does one job, and passes a clear result.

- Kotlin + Compose, Android only. Follow official Kotlin formatting.
- A feature owns its screen, UI state and ViewModel. Features never depend on one another.
- `app` owns navigation and dependency wiring; it does not own business rules or SQL.
- `core:domain` is Kotlin/JVM only. Models, contracts and rules must not import Android, Room or Compose.
- `core:data` owns persistence. Room entities and DAOs stay internal; expose domain contracts.
- `core:designsystem` owns reusable visual components and motion, not feature behavior.
- Prefer small, named functions and explicit constructor dependencies. One responsibility per class.
- Do not split every line into a helper or create pass-through use cases without behavior.
- Use immutable UI state and events/method calls into ViewModels. Screens receive values and callbacks.
- No global service locator, feature-to-feature imports, base ViewModel, generic BaseRepository, or catch-all Utils.kt.
- Keep navigation and Android Context out of ViewModels.
- Handle loading, success and failure explicitly. Cancellation is never a user-facing save error.
- Never log diary contents, PINs, passwords, file paths to private media, or keys.
- Never use Room destructive migration for user data. Commit schema JSON and test migrations before changing the database.
- Database and media stay local in V1. No analytics uploads, INTERNET permission, login or sync SDK.
- Persist the editor's selected date; never derive it again when autosaving across midnight.
- No placeholder controls that claim to save, lock, encrypt, back up, or schedule without doing so.
- Add tests for data loss risks, ordering, failure recovery, dates, and database migrations.
- Run `./gradlew testDebugUnitTest :core:domain:test lintDebug assembleDebug` and `pwsh -File scripts/Verify-Architecture.ps1`.
- Report what was actually verified. Do not call the V1 finished while media, PIN, reminders or backup are missing.
- Do not commit or push unless the user asks.
- UI uses the approved R2 Pen references in docs/DESIGN_REFERENCE.md. Shared typography is bundled Noto Sans Thai; icons are Lucide assets.
- Close transient dialogs on stop before showing the session PIN gate.
- New database migrations, archive authentication/restore and write ordering changes require their corresponding tests.
- Version 0.2.0 is a UI preview; local usage metrics, broad device/IME/accessibility coverage and release acceptance remain.
