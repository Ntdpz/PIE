# PIE · 240 daily messages

The original Thai copy lives in [ReminderPromptCatalog.kt](../core/domain/src/main/kotlin/com/ntdpz/pie/core/domain/settings/ReminderPromptCatalog.kt). Keep this Kotlin catalog as the single source when editing copy.

| Theme | Count |
|---|---:|
| Small everyday joys | 30 |
| Tired days and venting | 30 |
| Naming feelings | 30 |
| Being kind to yourself | 30 |
| Work, study and daily routines | 30 |
| People and relationships | 30 |
| Small steps and encouragement | 30 |
| Winding down and remembering | 30 |

Themes are interleaved, rather than showing 30 days of the same tone. The catalog contains 240 distinct short messages. Copy invites the user to write and leaves room for difficult days without requiring gratitude or a positive ending.

## Behavior

- The home greeting and automatic reminder preview/delivery share the daily message selected from the device's local date.
- The selection is deterministic: opening the app again or scheduling multiple reminders on one date does not reshuffle it.
- Every message appears once in a 240-day cycle before the cycle repeats. Extending or rearranging the catalog changes future day-to-message mappings.
- A nonblank custom reminder takes precedence for notifications. The home greeting keeps its default daily message. Blank custom text falls back to the catalog.
- Nothing is downloaded, inferred from diary content or uploaded. This feature does not request notification permissions by itself or change the reminder schedule.

## Verification · v0.3.2

`ReminderMessagesTest` covers a complete unique cycle, short copy, day stability across reminder times, custom-mode precedence, blank fallback, negative epoch dates and the year boundary. UI coverage checks that the home greeting matches the default notification preview and displays the catalog count. See [the version verification report](MESSAGES_V0_3_2_VERIFICATION.md).
