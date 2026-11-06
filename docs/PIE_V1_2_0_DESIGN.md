# PIE V1.2.0 — Kalka proposal

<!-- Hallmark · pre-emit critique: P4 H4 E4 S5 R4 V3 -->

Source: `design/PIE-V1.2.0-Kalka.fig`.
Page: `V1.2.0 · PIE Companion · 60 / 30 / 10` (`0:20`).
The active Kalka copy is `C:/Users/admin/PIE-V1.2.0-Kalka.fig`; its saved bytes
were copied to the project and verified with matching SHA-256 hashes.
Earlier pages remain in the file. The existing approved `PIE-Kalka.fig` is unchanged.

## Screen inventory

| Node | Screen |
|---|---|
| 0:503 | Today, empty |
| 0:745 | Today, continue writing |
| 0:782 | Editor |
| 0:814 | Current Gallery overview |
| 0:868 | Settings / PIE |
| 0:967 | Today, dark |
| 0:1024 | Calendar bottom sheet over Gallery |
| 0:1167 | Reminders |
| 0:1203 | Session PIN |
| 0:1241 | Encrypted export |

All ten screens use 390 × 844 content artboards without fabricated system chrome.
Text and visual groups are editable layers, not flattened screen images.
Additional boards document palette roles and five supplied pose sheets.
An exported Today comparison is `design/PIE-V1.2.0-Today.png`.

## Visual proposal

- Approximate surface proportions: cream 60%, caramel 30%, rose 10%.
- Exact core palette confirmed by the user: cream #FFF4E8, caramel #E6C7A6,
  rose #C64F78 and charcoal #302A32. No substitution of #AC365E for rose.
- Filled primary buttons use white 20 px / 700 labels. Small labels and secondary
  actions use charcoal on light surfaces. Selected calendar date is also 20 px bold.
- Dark background #302A32, cream text, derived brown surface #705449; same rose.
- Noto Sans Thai, existing symbol plus uppercase PIE as in runtime, and the
  repository's Lucide paths. Native editable vectors avoid HTML-import failures.
- Today retains the journal illustration and current headings, adding PET and
  daily copy. Gallery remains a two-column media grid with text-only rows.
- Editor restores back/date/save/menu and paperclip / Aa / emoji / mic toolbar.
  Formatting remains contextual; the sheet is not depicted as always expanded.
- Settings retains Drive, export/import, PIN, reminders and three appearance tiles,
  with a compact companion entry added. Calendar is a Gallery bottom sheet.
- Reminders retain weekday selection and message-mode chips. PIN is six digits.
  Export retains separate file-password confirmation and destination selection.
- These corrections modify the same page and the same ten artboard IDs.

## Confirmed companion behavior

PIE is the character name. Idle, tap reaction, daily greeting, genuine-save
celebration and rest follow `PIE_V1_2_0_PLAN.md`. Pose cards are still references,
not implemented animations. User-supplied artwork is embedded without redrawing.
The checked source sheet is RGB with a baked checkerboard; production assets
still require transparency/frame preparation. Some greeting frames contain
embedded text; runtime daily messages and status must be accessible text.

## Verified and remaining

- Visually inspected all ten screen screenshots, palette board and five pose
  references (Idle is also visible on Today).
- Minimum text size: no findings. Kalka's contrast lint reports five 4.38:1
  white-on-rose findings. Each affected label is 20 px / 700 (verified in node
  properties), exceeding the 3:1 large-bold-text contrast target. The tool applies
  a flat 4.5:1 threshold here; this is not reported as a zero-finding lint run.
- Visible top-level artboard overlap analysis: zero overlaps after repositioning
  pose cards. Intentional sprite clipping is not a general layout acceptance test.
- Saved the .fig and verified project-copy equality with SHA-256.
- This is a static design proposal. Full responsive variants, device/IME/font
  scaling/TalkBack/touch-target acceptance and runtime motion remain unverified.
- Diary excerpts, dates/settings and Gallery images are design fixtures, using
  approved-source media. Controls depict interfaces, not runtime implementation.
- Android source/version are unchanged. No Gradle/device checks were run for
  this design-only change; no commit, push or release was performed.

## Canvas arrangement

The same ten artboards are arranged left to right in two rows of five.
Top: Today empty, continue, Editor, Gallery, calendar sheet.
Bottom: Settings, reminders, six-digit PIN, export, dark Today.
Palette and PET references are grouped on the right. Restored explicit geometry
and pixel line heights on existing layers, then saved and reopened the same file;
all ten artboards retained 390 x 844 dimensions. The visible top-level overlap
check returned zero findings and the reopened full-page screenshot was reviewed.
