# Gallery memories — R3 proposal

The memory tab opens a date-based gallery. The calendar moves behind the calendar button in its header. Every day stays reachable, including days containing text only.

## Proposed interaction

- Gallery: newest days first; tap a day's cover to open its memory. Text-only days use a paper/story tile. Keep the previous grid scroll position when returning.
- Photo view: swipe horizontally through that day's attachments; show the current photo index. Swipe vertically in the media area to move between days.
- Story panel: preview a few lines at the bottom; tap “อ่านต่อ” or drag the handle to expand. Expanded text scrolls independently. Vertical day navigation pauses while the reader is expanded, preventing accidental day changes.
- Calendar: tap the small calendar button, choose a date, and open that day's memory. The calendar remains available as a secondary route.
- Editing: the selected date stays fixed when opening “แก้บันทึก”. Existing autosave and PIN behavior remain in effect.

## Media and privacy

Keep all original media and support viewing the complete image, zooming and accessibility labels in implementation. Gallery thumbnails may crop; originals must not be modified. Videos require an explicit play action; stop playback when changing days and preserve a quiet default. Content stays personal and local-first under the existing data rules.

## Artboards

Four new states in each theme: **16 · Gallery ความทรงจำ**, **17 · ภาพและเรื่องเล่า**, **18 · อ่านความทรงจำ**, **19 · ปฏิทินที่ซ่อนไว้**. They live on `R3 · Gallery · Light` and `R3 · Gallery · Dark`. The overview is on `R3 · Gallery · เริ่มดูที่นี่`.

Diary prose, photo counts and dates are mockup fixtures, using the existing sample design photos. The proposal is implemented in the 0.6.0 preview; see [runtime verification](../docs/R3_GALLERY_V0_6_0.md). Gesture arbitration, media fit/zoom, long-entry scrolling and video lifecycle have emulator coverage. Wider device and accessibility acceptance remains pending.
