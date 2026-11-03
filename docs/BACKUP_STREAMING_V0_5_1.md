# Backup streaming fix · v0.5.1

The S25 crash report identified `OutOfMemoryError` inside Conscrypt `OpenSSLAeadCipher.appendToBuf` during export. Platform AES-GCM `Cipher.update` accumulated the entire archive in memory, despite the surrounding 64 KiB copy loop. Failed exports left a 36-byte `PIEBKP01` header in Downloads. Such a file contains no encrypted diary payload and cannot be imported; export again from the device that still holds the original diary.

## Change

- New exports use `PIEBKP02`: 8-byte magic, 16-byte random PBKDF2 salt, followed by Google Tink AES-256-GCM-HKDF streaming ciphertext with 1 MiB segments. PBKDF2-HMAC-SHA256 remains at 210,000 iterations. The outer header is associated data.
- Tink supplies authenticated segment ordering, final-segment authentication and bounded buffering. The implementation does not define its own encryption framing.
- The full encrypted file is prepared in app-private cache before opening the destination for writing. Destination bytes are read back, fully authenticated/decrypted into a discard sink, checked for the expected plaintext length and compared with the staged ciphertext SHA-256 before recording export success. Temporary plaintext ZIP and encrypted staging files are removed after the operation.
- Valid legacy V1 files remain readable. Legacy ciphertext is capped at 32 MiB because the old platform implementation buffers the complete ciphertext. Larger V1 files require re-export from the original device using this version.
- Header-only files show an explicit incomplete-backup message. Authentication failure, unsupported format and oversized legacy files have separate explanations.
- New-format files require PIE 0.5.1 or newer to import. Diary database schema and existing app data do not change.

## Verification

Regression tests cover valid legacy decryption, wrong passwords, modified/truncated/reordered streaming segments, header-only files, Unicode passwords and the UI explanation for incomplete backups. Physical-device coverage includes exporting and preparing a 384 MiB synthetic media backup through Android's document content resolver, then deduplicating the original media. All 19 data tests passed on the physical S25. Unit tests, lint, debug assembly and the Light/Dark Merge UI flow also passed. Tests use isolated fixture data and remove their generated files.

## Sources

- [Google Tink Streaming AEAD](https://developers.google.com/tink/streaming-aead)
- [AES-GCM-HKDF streaming format](https://developers.google.com/tink/streaming-aead/aes_gcm_hkdf_streaming)
- [Tink Android setup](https://developers.google.com/tink/setup/java)
- [Apache 2.0 license](licenses/Tink-APACHE-2.0.txt), also packaged in APK assets.
