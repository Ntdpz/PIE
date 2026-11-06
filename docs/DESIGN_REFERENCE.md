# PIE — design reference

## Current runtime: R3

Approved Companion board 18 now supplies eight edge/corner grip poses for the
in-app and outside-app PET. See [edge-peeking implementation and verification](PIE_EDGE_PEEK_IMPLEMENTATION.md)
and [approved Kalka concept](PIE_EDGE_PEEK_DESIGN.md).

The approved notification identity now uses a head cropped from the actual PET,
plus tone-compatible reminder artwork from the same pose set. Kalka boards 14–17
remain on the existing V1.2.0 page. See [notification implementation and limits](PIE_NOTIFICATION_MASCOT_IMPLEMENTATION.md).

The user approved the Companion redesign on 2026-11-06 in the existing
`design/PIE-V1.2.0-Kalka.fig`, page `V1.2.0 · PIE Companion · 60 / 30 / 10`,
boards 11–13. Its scoped cream/caramel/rose visual system supplements the existing
diary screens. See [Companion UI design and handoff](PIE_COMPANION_UI_DESIGN.md)
for node IDs and [runtime verification](PIE_COMPANION_UI_IMPLEMENTATION.md).

Google Drive backup adds the approved R4 Expressive settings entry and introduction.
The user chose Android's document picker instead of direct OAuth. See
[Drive document backup](DRIVE_DOCUMENT_BACKUP.md) for the supplementary Kalka source,
implemented behavior and S25 verification limits.

The approved editor direction B adds an icon toolbar, contextual Aa formatting and
on-device voice typing. See [editor behavior and verification](EDITOR_VOICE_FORMAT.md)
for the Kalka artboard IDs, persistence changes and device acceptance limits.

The approved Gallery Feed revision is implemented in 0.7.0: natural-height media and full inline prose replace the earlier memory pager/story popup. See [current Feed verification](GALLERY_FEED_V0_7_0.md).

The approved visual system is [DESIGN.md](../DESIGN.md), with editable R3 pages in `design/PIE-Kalka.fig`. Runtime coverage is tracked in [R3 / Gallery](R3_GALLERY_V0_6_0.md). Version 0.6.1 also aligns calendar sheets, PIN options/help, deletion dialogs, reminder time input and Material surface roles with this system. The R2 tokens and Pen IDs below are historical references, not current implementation tokens.

## Archived R2

The current editable source is [design/PIE-Kalka.fig](../design/PIE-Kalka.fig) in Kalka, including all 24 artboards and the proposed Merge flow. See [migration details](../design/KALKA.md). The table below keeps the original Pen IDs for traceability.

The archived Pen source is [design/PIE-v0.4.pen](../design/PIE-v0.4.pen), with uppercase PIE, a lower PIN keypad and light/dark theme variables. [design/PIE.pen](../design/PIE.pen) retains the previous R2 reference. See [how to open the editable source](../design/README.md). PNGs were exported through PEN MCP, not redrawn from memory; newer exports are in `design-reference/v0.4/`.

| Screen / state | Pen node | Reference |
|---|---|---|
| Home, empty | OXucF | [Home](design-reference/OXucF.png) |
| Home, continue writing | CAfaV | [Continue](design-reference/CAfaV.png) |
| Editor | VGQFw | [Editor](design-reference/VGQFw.png) |
| Calendar | u8Ddh | [History](design-reference/u8Ddh.png) |
| Settings | p5QJT | [Settings](design-reference/p5QJT.png) |
| Reminders | zS2l2 | [Reminders](design-reference/zS2l2.png) |
| PIN | m16X2 | [PIN](design-reference/m16X2.png) |
| Encrypted backup | hBgUz | [Backup](design-reference/hBgUz.png) |

## Tokens

| Role | Value |
|---|---|
| Background | #F7F8FA |
| Paper | #FFFFFF |
| Text | #352D32 |
| Muted | #77717A |
| Accent | #B94F39 |
| Peach | #F8DAD0 |
| Border | #E6E4E8 |
| Saved | #466D59 |
| Typeface | Noto Sans Thai, bundled locally |
| Icon family | Lucide, bundled VectorDrawables |
| Main horizontal padding | 24 dp |
| Card radius | 16 / 20 / 24 dp according to role |
| Primary action | 56 dp minimum height, 16 dp radius |
| Bottom tab | 58 dp inside an 84 dp bar, plus actual Android navigation inset |

The full header wordmark was exported directly from the image node in Pen. Do not substitute a typed wordmark or a rounded square launcher icon in its place.

## Implementation notes

- Actual Android system bars, IME insets and system font scale replace the mock system chrome.
- The app uses the same composables for production and test screenshots.
- User content, attachment count, PIN state, reminder times and app version are real state; the sample values in Pen are not seed data.
- Home has two layouts: a fixed 430 dp invitation for the empty state and a flexible invitation after today's preview.
- No forced keyboard on editor entry. Emoji and # insert at the current selection.
- Photo/video attachment controls use the Android picker and copy into app storage.
- PIN gates the app on background/return. Dialogs are dismissed on stop so they cannot sit above the lock screen.
- Reminder delivery is inexact and may be delayed by Android/OEM power restrictions.
- Keep long screens scrollable and respect real device insets.

## Asset licenses

- [Noto Sans Thai, SIL Open Font License](licenses/NotoSansThai-OFL.txt)
- [Lucide license](licenses/Lucide-LICENSE.txt)

Copies of these licenses are included in APK assets.

