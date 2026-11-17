# Upload keystore — Sabeel

**Status:** generated locally on 2026-11-17 for the v1.0 production-readiness pass.
**Never commit the keystore or its passwords.** They are gitignored (`keystore/`,
`keystore.properties`, `*.jks`, `*.keystore`); CI builds use the unsigned path.

## 1. What exists (local machine only)

| Item | Path | Committed? |
|---|---|---|
| Upload keystore (PKCS#12, RSA 4096) | `keystore/sabeel-upload.jks` | **No** (gitignored) |
| Signing properties (file, alias, passwords) | `keystore.properties` | **No** (gitignored) |
| Public upload certificate (PEM) | `docs/release/upload-certificate.pem` | Yes — public material only |
| This document | `docs/release/KEYSTORE.md` | Yes |

**Certificate identity**
- Alias: `sabeel-upload`
- Key algorithm/size: RSA 4096
- Validity: 10000 days (until 2054-04-04)
- Subject DN: `CN=Sabeel Upload, OU=Mobile, O=Sabeel, L=Baghdad, ST=Baghdad, C=IQ`
- **SHA-256 fingerprint:**
  `7B:12:AD:6B:FA:F0:78:17:E7:6A:25:39:95:4F:7B:4B:DE:2E:13:F6:B3:34:54:82:48:30:10:25:66:B6:15:2D`

Inspect it without printing secrets (keytool prompts for the password):

    keytool -list -v -keystore keystore/sabeel-upload.jks -alias sabeel-upload

The fingerprint above must match the certificate Google Play shows for the
upload key after registration.

## 2. How the Gradle build uses it

`app/build.gradle.kts` reads `keystore.properties` **only if it exists** and
only when the referenced keystore file is present. Consequences:

- Local machine with the keystore: `:app:assembleRelease` and
  `:app:bundleRelease` are signed with this upload key.
- CI or a fresh clone without the file: the same tasks succeed and produce an
  unsigned APK/AAB (the CI workflow relies on this; never add signing secrets to
  CI for this project).

## 3. Backup (do this before the first upload)

The upload key is the identity of the app on Play. Losing it without Play App
Signing means you can never update the same package again.

1. Create a password-manager entry containing: the keystore password, the key
   password, the alias `sabeel-upload`, the SHA-256 fingerprint above, the
   certificate DN, and the date of creation.
2. Store the `sabeel-upload.jks` file in two encrypted copies on separate media
   (for example an encrypted archive on an offline USB drive kept in a different
   physical location, plus one encrypted cloud object in a zero-knowledge
   container). Do **not** store it in the repository or a plain cloud folder.
3. Keep the public certificate `docs/release/upload-certificate.pem` with the
   backups so the fingerprint can be checked without opening the keystore.
4. Record a rotation/backup date in the release log each time the key is used
   for a production upload.

## 4. Play App Signing registration (owner action)

1. Create the app in Play Console with the package `com.sabeel.steps` and
   choose **Play App Signing** (Google manages the app signing key; this
   keystore becomes the **upload key**).
2. Open **Setup → App signing**. Upload
   `docs/release/upload-certificate.pem` where the console asks for the upload
   certificate, and confirm the SHA-256 fingerprint matches the value above.
3. Upload the signed AAB produced with this keystore. Every later upload must be
   signed by the same upload key until it is rotated.
4. Play shows two certificates: the **app signing certificate** (Google) and the
   **upload certificate** (this one). Data-safety/store metadata must be
   consistent, but only the upload certificate is ours to manage.

## 5. Rotation

- **Upload key rotation** (lost/compromised key, Play App Signing enabled): use
  the Play Console "request upload key reset" flow — generate a new keystore with
  the same DN and a fresh random password, store it like the original, register
  the new certificate, then upload the next AAB signed by the new key. Existing
  installs are unaffected because Google re-signs with the app signing key.
- **Do not rotate casually**: every rotation is a console flow and a new backup;
  record the date and fingerprint in this file (append a row) and in the release
  log.
- If the app signing key itself ever needs rotation, that is a Play-managed
  process with user-impact constraints; not expected for v1.

## 6. If the keystore is lost and Play App Signing is NOT enabled

Google Play has no recovery path. The package `com.sabeel.steps` can never
receive another update signed by the original key, so the only option is to
publish a **new application with a new package name** — losing installs,
ratings, reviews and the listing's history. This is why Play App Signing is a
precondition, not an optimisation.

## 7. Never do this

- Never commit `keystore/`, `keystore.properties`, `*.jks`, `*.keystore`.
- Never paste the keystore or key password into a chat, issue, CI log or commit.
- Never send the keystore file by email or store it in an unencrypted archive.
- Never add signing secrets to the GitHub workflow; releases are signed locally
  by the owner.
