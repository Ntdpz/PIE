# Backup UX · v0.4.1

The import action is now always visible in Settings. Previously it disappeared whenever diary data existed. The existing encrypted file picker and password-based restore remain available on an empty app. On a populated app, the action explains the empty-app requirement and how to restore on another device. This release does not merge or overwrite existing diaries.

Settings displays the last successful export time in the device timezone. The timestamp is persisted only after the encrypted output stream closes successfully; failed exports, picker cancellation and imports do not advance it. Existing users see “no recorded successful export on this device” until their next successful export, since previous versions recorded no timestamp. This timestamp does not prove the file still exists at its destination.

Regression coverage extends the encrypted text/media round trip to assert successful timestamp creation, unchanged timestamp on output failure and unchanged timestamp on restore. The settings UI test also checks that the import action exists.

Validation: unit checks, lint, Debug build and architecture boundaries passed. This version was installed and launched on S25. Data/UI instrumentation results are recorded in the local verification logs; no real diary or personal backup file was used for testing.

Final regression: all 5 data instrumentation cases and 7 app UI cases passed on Android 17 emulator. The installed S25 build is v0.4.1 (code 7).
