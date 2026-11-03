# PIE 0.6.0 — R3 UI and Gallery memories

The R3 calm journal design is now implemented in Compose. The memory destination opens a gallery instead of the calendar. Tap a day to open its media and story. The calendar remains available through the calendar button; an empty date opens the editor with that exact date.

## Implemented behavior

- Shared warm-paper Light/Dark palettes, reduced repeated copy, smaller PIE header, compact navigation indicator and theme tiles.
- Home has one writing action and an illustrated paper invitation; an existing entry becomes the main preview. Editor remains free-form with autosave, emoji, # and media.
- Settings, reminder controls, PIN and backup screens follow the quieter hierarchy. Reminder multi-time scheduling, custom messages, test notification and saving feedback remain functional.
- Merge keeps explicit final confirmation and replacement counts. Secondary counts move to a details sheet. Comparison text can expand for full reading; no original content is removed by visual truncation.
- Gallery uses a cover per date and retains text-only days. The grid restores its scroll position after returning from a memory.
- Horizontal swipes change attachments; vertical swipes in the media area change days. The story panel opens on tap or upward drag. The expanded reader owns scrolling and disables day swipes. Text-only days open the reader.
- Photos fit their original aspect ratio and support pinch/double-tap zoom with reset. Zooming pauses day/attachment swipes. Gallery covers may crop visually; source files are unchanged.
- Videos require an explicit play action, start muted and stop/release on background, day/attachment change or story expansion.
- Editing from the viewer passes the displayed date to the existing editor. Media, text and settings remain local. No database migration is needed; schema stays at 3.
- Calendar, story, import-detail and PIN help dialogs close on stop before session locking.

## Responsibilities

`DiaryMediaRepository.observeAllMedia` supplies grouped local metadata, ordered by date and stable timestamp/ID. It does not decode every file. `MemoryDay` combines summaries and attachments; full prose is observed only for the selected date. Feature ViewModels own read/error/retry state, composables own pager and reader presentation, and `MemoryAttachment` owns local media rendering/lifecycle. App navigation wires the feature without feature-to-feature dependencies.

## Validation — 3 November 2026

- 34 JVM/unit cases passed, including timeline ordering, text-only days, full selected-day text, live updates, deletion and retry.
- 20 Android data cases passed on the emulator, including the new all-media ordering/deletion query and existing persistence, migration and encrypted-backup cases.
- 12 Android UI cases passed, including Light/Dark Gallery flows, horizontal/vertical gestures, long-story scrolling without day changes, text-only days, editing the selected date, hidden calendar, explicit video play/mute/background behavior, PIN, theme recreation, autosave and encrypted Merge.
- Android lint, debug assembly, APK signature verification and architecture boundary checks passed.

The large-backup regression fixture selects 384 MiB when at least 2 GiB is available, otherwise 192 MiB, with an explicit storage assumption before setup. This emulator ran the 192 MiB case because it had about 1.2 GiB free. The earlier physical S25 384 MiB result is recorded in `BACKUP_STREAMING_V0_5_1.md`.

This delivery is a signed **debug preview APK**, provided for manual installation as requested. It was not installed on the S25 in this task. Usage metrics, Drive sync, release signing/performance profiling and wider device/accessibility acceptance remain separate work.
