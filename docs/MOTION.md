# PIE motion · v0.3.0

Motion follows the Pen R2 surfaces and spacing. The aim is a responsive, calm app with a little spring in deliberate actions.

| Interaction | Motion |
|---|---|
| Primary actions | 0.97 press scale, damped spring release, system haptic confirmation |
| Bottom tabs | One sliding peach indicator, spring icon emphasis, animated ink, selection haptic |
| Navigation | Short directional travel + fade; nested pages return in the opposite direction |
| Home | Staggered 16 dp entrances and one soft light sweep on the invitation |
| Calendar | Directional month transition, selected-day background and ink transition |
| Settings | Staggered section entrances |
| Save status | Crossfade between actual persistence phases; revisions never restart the animation |
| PIN | Spring pulse and ink change on each filled dot; opaque gate remains immediate |

## Ownership

`core:designsystem/motion` owns timing, easing, press feedback, entrances and the finite sweep. `app/navigation` owns route direction. Features choose where to apply these primitives. Business state, persistence and callbacks remain independent of animation completion.

The reveal, scale and sweep read animation values during drawing/layer updates. They do not relayout the diary text each frame. There are no infinite decorative loops, animation assets or new dependencies. System Compose animation duration scale is respected, including disabled animations. Haptics use the platform API and honor system feedback settings.

The editor text and PIN security overlay are deliberately immediate. The save indicator continues to reflect actual persistence results. No delay is added before navigation, saving or unlocking.

Primary reference: [Compose animation quick guide](https://developer.android.com/develop/ui/compose/animation/quick-guide).

## Validation

See `MOTION_V0_3_VERIFICATION.md` for the checks performed on this build. Emulator functional checks do not establish frame-time performance on every device.
