# Backup and Merge motion · v0.5.2

The approved screen layout now includes directional slide/fade transitions between review steps and dates, spring press/selection feedback with smoothly changing selection color, staggered summary rows, animated loader appearance and a one-shot success check drawing.

Motion uses Compose animation primitives and existing PIE timing tokens. There are no new animation dependencies, artificial operation delays, or changes to encryption, merge decisions or persistence. Buttons remain available during decorative success motion. Finite animations honor Android's animator duration scale, including zero.

Validation: unit tests, lint, debug assembly, architecture boundaries and the real encrypted Merge flow in Light/Dark. The Merge flow is also exercised with the emulator animator duration scale set to zero, then the original emulator setting is restored.
