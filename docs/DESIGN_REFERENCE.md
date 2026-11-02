# PIE R2 — design reference

The approved source is the R2 set in [design/PIE.pen](../design/PIE.pen), committed with its referenced images. See [how to open the editable source](../design/README.md). These PNGs were exported through PEN MCP, not redrawn from memory.

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
