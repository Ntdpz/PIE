# Mascot notifications — implementation, 2026-11-06

Implements the user-approved [Kalka notification design](PIE_NOTIFICATION_MASCOT_DESIGN.md)
in the existing Android notification channels. Scope is notification identity and
message-compatible artwork, not a complete V1.2.0 app redesign.

## Result

- Both diary reminders and the outside-app PET service use the white alpha mask
  of the exact cropped PET head for the small icon. No separately drawn vector
  is used. The launcher logo is unchanged.
- The service uses the full-color cropped head as its large icon, with its existing
  title, body, notification ID, channel, open action and stop action retained.
- Reminders use one of nine static pose images according to explicit prompt tone
  metadata. The existing eight groups map to the approved compatible pose pools.
  Day-based selection is deterministic and can be reconstructed after process
  restart. Refreshing a notification or expanding it cannot reroll its pose.
- Custom nonblank messages use Nod without guessing tone. Blank custom messages
  fall back to the usual daily message/pose. No diary text is inspected.
- The 240 original prompt strings and their interleaved order are unchanged.
  Their SHA-256 snapshot is tested. User-selected reminder times, alarm calculation,
  channel importance and normal reminder alert behavior remain unchanged.
- Standard Android notification templates and BigTextStyle render the content.
  One UI controls final layout, tint, scaling and visibility. Artwork is static.

## Ownership and compatibility

`core:domain` owns prompt tone metadata and selection rules in
`ReminderPresentation.kt`; it has no Android imports. `core:designsystem` owns the
exact image assets and bitmap decoding. `feature:pet` maps domain reactions to
visual poses and supplies service notification images. `core:data` receives the
resource ID, color and image callback explicitly; it does not depend on the design
system or a feature.

The Android receiver entry point moved to `app` for dependency wiring. Its
historical fully qualified class/component name remains
`com.ntdpz.pie.core.data.reminder.ReminderReceiver`, deliberately preserving existing
PendingIntent identities. The app manifest registers that same private receiver
and the same boot/time intent filters. `core:data/ReminderDelivery` retains the
delivery/settings/rescheduling rules. App wiring introduces no business rules,
global service locator, feature-to-feature dependency or schema migration.

## Verified

- Required `testDebugUnitTest :core:domain:test lintDebug assembleDebug` passed:
  79 JVM tests, zero failures/errors. Nine-module architecture check passed.
- New domain checks cover the unchanged 240-message/order digest, stable matching
  poses across 960 dates including pre-epoch dates, all nine actions, no immediate
  standard-day repetition, and custom/blank message behavior.
- Eleven resource PNGs match the approved source assets byte for byte. The head
  mask alpha matches the color crop; all 16,666 retained RGBA pixels match the
  original PET. No new illustration or image generation.
- Three focused tests passed on S25 `R5CXC3DMXLD`, in the separate `.devicetest`
  package: actual notification content/icon/large-image stability, custom Nod and
  private visibility; existing-component PendingIntent delivery through the real
  receiver; service head bitmap after Android resizing, all nine asset decodes,
  and the real stop PendingIntent removing the service notification.
- Android resized the large head bitmap; the bitmap comparison accounts for that
  system resize. An exploratory notification-shade capture could not locate the
  One UI notification view and was removed from the functional test. No screenshot
  of unrelated notifications was saved. Notification-object delivery is verified;
  exact shade appearance, lock-screen rendering and broad OEM coverage remain
  manual acceptance work.
- Logs: `work/notification-mascot-build.log`,
  `work/notification-mascot-domain.log`, `work/notification-mascot-s25.log`.

The regular package was installed with `adb install -r` and launched on S25;
no regular-app uninstall or data clear. Temporary test packages were removed.
APK: `work/PIE-notification-mascot-debug.apk`; SHA-256:
`511CF9ABF6BB16CD15AD746E9D9A0717A0DDBCAAF1B3B5845174935E7C1F3FAF`.
Only S25 was targeted. No LD/emulator work, new permissions, uploads, commit or push.
