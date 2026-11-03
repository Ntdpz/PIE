# R3 Gallery Feed — approved

<!-- Hallmark · pre-emit critique: P4 H4 E4 S4 R5 V4 -->

Approved revision: remove black letterboxing around differently shaped media and replace the story popup with an inline feed. Implemented in Android 0.7.0; see [runtime verification](../docs/GALLERY_FEED_V0_7_0.md).

Review in `PIE-Kalka.fig`, page **R3 · Gallery · เริ่มดูที่นี่**. The four comparison views in each theme show the existing day gallery, a landscape photograph with inline prose, a portrait photograph with inline prose, and reading further down the same feed. Editable source pages: **R3 · Gallery Feed · Light/Dark**. The former overview is retained in **Archive · Gallery R3 ก่อน Feed**.

## Reading model

- Keep Gallery as the date/cover overview and the calendar as its secondary action.
- Tapping a cover opens a continuous feed positioned at that day; returning restores the Gallery scroll position.
- Each entry is date/edit row → attachments → attachment position, when multiple → original diary prose → next date. No forced full-screen day snap, story popup, independent reader scroll or expansion gesture.
- Prose remains in document flow, including long entries, Emoji and hashtags. It is not a separate title field, generated caption or mandatory summary. A text-only day uses the date and prose directly, without a blank media box.
- Vertical scrolling reads the complete entry and continues into older days. Horizontal swiping changes attachments within the current date. The edit action always carries the displayed entry's date.

## Media sizing

- Feed media has the available content width; height = displayed width × original height / original width. Respect orientation metadata. Examples at 342 px content width: 16:9 ≈ 192 px, 4:3 = 256.5 px, 2:3 = 513 px, 9:16 = 608 px.
- The container follows each attachment's ratio. There is no fixed media stage, black/blurred letterbox, stretching or crop of the original in the feed. A tall photograph naturally places its prose below the viewport; the user scrolls to it.
- Mixed-ratio carousel changes interpolate height briefly, with reduced-motion support. Anchor the media's top position while changing attachment; do not snap to another date or move prose into an overlay.
- Thumbnails in the Gallery can retain cover cropping. Viewing the full feed must preserve the original image. The mockup's existing sample photographs are fixtures; the 4:3 reference crop demonstrates a different source ratio and does not authorize cropping real attachments.
- Video uses the same ratio rule and explicit play, muted default. Stop playback when a post leaves the visible area or the app backgrounds. No autoplay requirement is introduced.

## Visual and motion direction

Use the approved DESIGN.md tokens: warm page, 24 px content inset, 8 px photo corner, 18 px diary prose, modest date/edit rows and thin separators between days. No shadowed caption card, reader handle, reaction/share buttons or extra motivational copy around the user's entry. Only the top navigation stays fixed; media and text share one vertical reading flow.

The native mockups specify layout, not implemented gesture behavior. Before shipping, verify mixed-ratio changes, long prose, selected-date editing, retained scroll position, video lifecycle, font scaling and small-screen sizing in Compose.
