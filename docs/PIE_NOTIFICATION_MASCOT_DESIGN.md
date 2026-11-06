# Mascot notification identity — 2026-11-06

Approved and implemented afterward; see [runtime and S25 verification](PIE_NOTIFICATION_MASCOT_IMPLEMENTATION.md).
The notes below record the design proposal and its checks.

<!-- Hallmark pre-emit critique: Philosophy 4, Hierarchy 4, Execution 4, Specificity 4, Restraint 5, Variety 3. -->

Design proposal only, requested before Android implementation. Uses the same
`design/PIE-V1.2.0-Kalka.fig`, page `0:20`,
`V1.2.0 · PIE Companion · 60 / 30 / 10`.

| Board | Node | Geometry | Preview |
|---|---|---|---|
| 14 · Notifications Light | `435921011:500` | 0,3060 / 390×844 | [Light](../design/notification-ui/PIE-Notification-Light.png) |
| 15 · Notifications Dark | `435921011:547` | 470,3060 / 390×844 | [Dark](../design/notification-ui/PIE-Notification-Dark.png) |
| 16 · Small icon specification | `435921011:594` | 940,3060 / 390×844 | [Icon](../design/notification-ui/PIE-Notification-Icon.png) |
| 17 · Nine poses matching message tone | `435921011:640` | 1410,3060 / 1049×844 | [Pose examples](../design/notification-ui/PIE-Notification-Poses.png) |

## Proposed change

Replace the generic Android icons currently used by the outside-app PET service
and diary reminders with a head cropped directly from the actual PET artwork.
The user explicitly rejected the separately drawn vector icon. Its old SVG is
superseded and must not be used for implementation. The specification board
shows the exact full-color head and source-derived 16, 24 and 32 unit monochrome
previews on both backgrounds. This does not replace the existing app logo.

Canonical asset: [exact PET head PNG](../design/notification-ui/pie_pet_head.png).
Android small-icon mask: [white alpha head PNG](../design/notification-ui/pie_pet_head_white.png).
Source and mask coordinates: [provenance](../design/notification-ui/pie_pet_head_source.json).
The source is `design/companion-ui/assets/idle.png`, a prepared crop of the supplied
PET. A deterministic mask removes neck and clothing while preserving the bob and
ears. All 16,666 retained pixels were checked against source RGBA without changes.
Transparent pixels have zero RGB. No image generation or character redraw.
Monochrome variants keep that exact alpha mask and replace RGB only, as required
for Android small-icon tinting; facial colors/details remain in the color asset.

Replaced all 24 separately drawn icon instances in boards 14–17 with exact-source
head-image instances. Original vector layers remain hidden for history. The PET
service's large portraits and board 16 also use the clean head crop. The nine
pose examples remain available separately, preserving the requested action variety.

Use the original full-color mascot artwork as the optional large icon on the
notification card. Ongoing PET service uses the existing idle portrait. Reminder
cards use a static full-body pose so hand and ear gestures remain visible. All
artwork comes from existing transparent assets without redrawing the character.

## Revision: varied poses matching message tone

The user requested more variety instead of the same idle artwork for every
message. Board 17 now shows nine examples using exact copy from the existing
`ReminderPromptCatalog`; newlines are layout only. The Light/Dark reminder examples
now use Wink, while both states of the same PET notification keep Idle.

| Existing prompt group | Shown pose | Proposed compatible pool |
|---|---|---|
| smallJoys | Wink | Wink, EarsUp, Cheer |
| tiredDays | Heart | Heart, Nod |
| feelings | HeadTilt | HeadTilt, Nod |
| selfKindness | Shy | Shy, Heart |
| smallSteps | Cheer | Cheer, Peekaboo |
| windingDown | Stretch | Stretch, Nod |
| smallJoys — listening to sounds | EarsUp | EarsUp, Wink |
| tiredDays — listening without pressure | Nod | Nod, Heart |
| smallSteps — discovering something new | Peekaboo | Peekaboo, EarsUp |

The two other existing catalog groups can use Nod/EarsUp for everydayWork and
Heart/Wink for connections. This covers the nine available actions across
compatible pools. These mappings are design proposals, not implemented rules.
Keep explicit group/tone metadata with catalog prompts during implementation;
do not infer the user's current emotional state from a reminder prompt.

Choose on creation of a new notification event, favor a different compatible
pose from the previous event when possible, and retain the chosen pose when the
same notification is refreshed or expanded. Do not reroll the daily message.
Unknown/custom text should use a calm listening default (Nod) rather than an
unreliable tone guess. Do not use celebratory gestures for tired-day text.
The mono small icon stays stable for recognition; variety belongs to the large
mascot image. Notification artwork is static; opening the app may play its
matching animation. This does not add new notifications or change their timing.

Light/dark boards show collapsed, expanded and daily-reminder examples. Keep the
existing PET title, explanatory content, tap-to-open behavior and stop action.
The reminder sample uses the existing PIE title; its daily text is a design
fixture, not a request to change reminder scheduling or message selection.
No additional action duplicating the notification body tap is introduced.

## Platform boundaries

Android controls the notification template and small-icon tint. The status-bar
icon is monochrome; the full-color portrait belongs to the optional large icon.
Use standard templates rather than promising custom notification chrome.
The neutral light/dark surfaces illustrate system UI; the app's 60/30/10 area
ratio does not control the notification shade. Cream portrait tiles and rose
icon accents connect the proposal to PIE. One UI may choose different spacing,
backgrounds, icon treatment, accent colors and collapsed content.

Reference: [Android notification anatomy and templates](https://developer.android.com/design/ui/mobile/guides/home-screen/notifications).

## Verification

Reviewed the native Kalka board screenshots. Final color-contrast and
minimum-text-size lint for these boards reports zero findings; this is not a
full accessibility audit or touch-target acceptance test. Visible top-level
overlap analysis reports zero overlaps. Archived two duplicate import drafts
left by the earlier Companion state-board timeout without deleting their layers.
Also archived the delayed HTML notification import when it eventually appeared;
the final visible top-level overlap check returned zero findings.

Revision: expanded the same board 17 from six to all nine existing actions,
retaining its root ID and its original six card IDs. The three added examples
are EarsUp, Nod and Peekaboo. Reflowed into three columns within the existing page;
no new page or Android code was created. Reviewed the nine-message screenshot;
its contrast/minimum-text lint and
the page's visible top-level overlap check also report zero findings. Exported
the new pose board and refreshed both Light/Dark PNGs at 2×.

Saved the active .fig and verified SHA-256 equality with the repository mirror.
Exported the three PNGs at 2×. No Android source changes, deployment, Gradle tests,
commit or push in this design step. Actual monochrome recognition, template
rendering and image cropping still require S25 verification after implementation.
