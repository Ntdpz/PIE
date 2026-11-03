# PIE R3 · สมุดส่วนตัว

<!-- Hallmark · pre-emit critique: P4 H4 E4 S4 R4 V4 -->

Status: approved direction, implemented in the 0.6.0 preview. The user chose **สงบ เหมือนสมุดส่วนตัว**. See [runtime behavior and verification](../docs/R3_GALLERY_V0_6_0.md).

Open `PIE-Kalka.fig`, then **R3 · เริ่มดูที่นี่** for three-screen Light/Dark overview boards. Full state references are on **R3 · สมุดส่วนตัว · Light**, **R3 · สมุดส่วนตัว · Dark**, and **R3 · จอเล็ก · 320–414**. Previous pages remain for comparison.

## Screen inventory

Each state has a Light and a Dark artboard, giving 30 primary references.

| Screen | Visual decision |
|---|---|
| 01 · วันนี้ | One invitation, a personal page motif and one reachable writing action |
| 02 · เขียนต่อ | The existing entry takes the place of an explanatory panel |
| 03 · หน้าเขียน | User prose and photo dominate; saving status appears once |
| 04 · ย้อนวัน | Calendar followed by a visual memory preview, without engagement scores |
| 05 · ตั้งค่า | Theme choices and scan-friendly rows instead of stacked descriptions |
| 06 · แจ้งเตือน | Time and weekdays lead; prompt/custom text is secondary |
| 07 · PIN มือเดียว | Keypad stays low, with a short PIN instruction |
| 08 · ส่งออก | Two password fields; retain the distinction between file password and PIN |
| 09 · นำเข้า | File type, password and file selection; no long introduction |
| 10 · ภาพรวมไฟล์ | New days, conflicting days and media are the first things seen |
| 11 · เลือกข้อความ | Two readable text examples and a concise three-choice control |
| 12 · ยืนยัน | Explicit replacement count remains visible before committing |
| 13 · สำเร็จ | Quiet success state and a compact summary |
| 14 · ยืนยันลบ | Short consequences and a distinct destructive action |
| 15 · รายละเอียด | Secondary counts and preserved settings move into an on-demand sheet |

## Content and implementation notes

Dates, diary prose, counts and timestamps are illustrative mockup data, not actual app telemetry or private diary exports. Mockup photo provenance is retained in [assets/sources.json](assets/sources.json).

This is a design proposal. Implementations must retain full entry reading, all media, autosave status/error/retry, reminder multi-time scheduling, backup validation, PIN gating and safe Merge rules. Labels shown briefly in mockups do not remove required runtime error or security information.

Small phone references cover home and text selection at 320, 375 and 414 px. Main phone references use 390 × 844. Static frame geometry is not a requirement to hard-code Compose coordinates; content must flow, scroll and respect real system/IME insets and font scaling.

Text and layout are editable Kalka layers. Icons and paper artwork are individual image layers, with editable SVG sources in [r3/assets](r3/assets/). Existing PIE logo and mockup photos were reused. The entire screen is not flattened into a screenshot.

Shared proposal rules are in [DESIGN.md](../DESIGN.md); semantic palette values are in [r3/tokens.json](r3/tokens.json). Explicit paints preserve the reviewed layout. This proposal does not claim that every paint is automatically bound to a Kalka variable.

## Verification

All four new pages report faithful Noto Sans Thai faces without substitutions. The primary pages contain 15 artboards each; compact references contain six artboards. The overview page has two presentation frames and preserves user annotations. Direct child geometry checks report no overflow on the 36 phone references after fixing imported tab-label bounds.

Normal-text contrast checks include Light muted/background at 4.72:1 and primary-label/accent at 5.43:1. Small text on selection tints uses ink; accent/tint is reserved for larger text and non-text elements. Dark equivalents also meet the normal-text target.

See [r3/verification.json](r3/verification.json) for checked sizes, text element counts and font availability. [r3/session-map.json](r3/session-map.json) records the editing session; Kalka may regenerate IDs when reopening a `.fig`, so use page and artboard names as durable references. Runtime interaction checks are recorded in the 0.6.0 report; broader accessibility acceptance remains pending.
