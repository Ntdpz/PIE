# Merge import · v0.5.0

The approved Kalka flow is implemented in Kotlin/Compose: overview, per-day text choices, final confirmation and completion. Preparing a file does not change the diary. Only final confirmation commits the import.

## Rules

- New dates are added. An empty existing day can receive backup text.
- Identical text is skipped. Different non-empty text defaults to keeping the device text.
- Each conflicting day supports keeping the device text, replacing it with backup text, or appending both with a visible divider.
- Media is deduplicated by date and SHA-256 of file bytes. Newly accepted media receives a fresh ID; existing media is retained for every text choice.
- Import receipts prevent an accepted backup text from being appended again on subsequent imports. Keeping the device text allows reconsidering the backup later. Deleting a day clears its receipts so an intentional restore is possible.
- Device PIN, theme and reminder settings remain unchanged. Settings shows the last successful import and export separately.
- A changed device snapshot invalidates the preview instead of overwriting newer content.

## Responsibilities

`DiaryMergePlanner` owns pure text decisions. `BackupArchiveReader` authenticates and validates the archive into staging. `LocalMergeImportRepository` owns preparation, media identity and transactional commit. `DatabaseMutationGate` serializes diary/media/archive mutations and recovers interrupted staging work. The ViewModel owns operation state; Compose owns the review navigation and presentation.

Room schema 3 adds import receipts and history through migration 2 → 3, retaining existing text and media. Promoted media filenames are journaled before filesystem changes; a failed transaction removes unreferenced files. Restart recovery preserves files referenced by committed database rows.

## Verification

On 3 November 2026, unit tests, Android lint, debug assembly and the architecture boundary check passed. Emulator data tests cover migrations 1 → 2, 2 → 3 and 1 → 3, wrong passwords, corruption, path traversal, stale previews, rollback, restart recovery, cancellation, repeated import and media deduplication. App tests exercise an actual encrypted backup through review and commit.

These checks use emulator fixtures. Physical S25 installation is pending reconnection; no S25 test is claimed for this version.
