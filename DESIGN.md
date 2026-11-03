# PIE — R3 visual system (proposal)

<!-- Hallmark · pre-emit critique: P4 H4 E4 S4 R4 V4 -->

Direction chosen by the user: **สงบ เหมือนสมุดส่วนตัว**. The R3 visual layer and Gallery extension are implemented in the 0.6.0 debug preview. See docs/R3_GALLERY_V0_6_0.md for runtime behavior and validation.

## Intent

Make the diary feel like opening a personal page. The first screen should invite a small action, the editor should foreground the user's own words and photographs, and administrative tasks should be easy to scan. Reducing repeated explanations and showing details when requested are design hypotheses to review with actual use, not a claim of measured psychological benefit.

## Shared structure

- Phone artboards: 390 × 844 content area, without fabricated OS or device chrome.
- Horizontal margin 24; spacing 4 / 8 / 12 / 16 / 24 / 32.
- Noto Sans Thai: regular for prose, medium for headings, semibold for primary actions; original PIE symbol and uppercase wordmark retained.
- Headings 28; editor prose 20; normal controls 14–17; secondary metadata 12–13.
- One prominent action in the lower reachable area. Three fixed destinations: วันนี้ / ความทรงจำ / ตั้งค่า; the calendar is secondary.
- Flat setting rows, thin separators, small theme tiles. Reading and writing screens have a different rhythm from administration screens.
- Existing navigation, local-first behavior, PIN, media, autosave, backup encryption and Merge rules stay owned by their current modules.

## Tokens

| Role | Light | Dark |
|---|---|---|
| Background | #F7F3EC | #1D1B19 |
| Paper | #FFFCF7 | #292622 |
| Ink | #302B28 | #F1EADF |
| Muted text | #746B62 | #B8AD9D |
| PIE accent | #A64E3C | #E7AD93 |
| Selection tint | #F0DDD1 | #49352B |
| Divider | #E4DCD1 | #484037 |
| Saved / success | #52664E | #B3C3A3 |
| Saved surface | #E3E9DD | #333E2E |
| Destructive | #A93632 | #E8998F |

Small text on selection tints uses the ink token, rather than accent text with insufficient contrast. Ink/background, muted/background, primary-label/button and saved-label/surface combinations are checked numerically. Minimum normal-text target is 4.5:1.

## Content rules

- Keep one short prompt on the home screen; remove repeated instruction paragraphs.
- Show saving status once in the editor.
- Use the user's actual diary text and media as the visual focus, not motivational paragraphs around them.
- No streak penalties, guilt, required mood logging or invented engagement statistics.
- Preview before Merge remains. Keep replacement counts explicit at confirmation; details are available in a sheet instead of always filling the screen.
- Keep meaningful safety text at the decision: separate backup password from PIN, show replacement count, state what deletion removes.
- A long entry expands to full reading text. The two-line example in a mockup is not permission to truncate real content permanently.

## Interaction handoff

Primary actions have a 56 dp visual height. Small icons must receive at least a 48 dp runtime hit region. Dialog/sheet dismissal, keyboard and system insets, long text, font scaling, TalkBack and reduced motion must be implemented and verified in Compose. These static artboards do not claim runtime accessibility acceptance.

Keep current operation state and real loaders. Use short, restrained transitions; decorative motion must not delay save/import completion or block buttons. System animator duration scale zero remains supported.

## References

- [R3 screen inventory and review notes](design/PIE-R3.md)
- Native source: `design/PIE-Kalka.fig`, pages beginning `R3 · สมุดส่วนตัว`.
- Original `.pen` files and earlier Kalka pages remain as archives for comparison.
