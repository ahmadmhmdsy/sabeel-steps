# Contributing to Sabeel (سبيل)

Sabeel is an original, offline, ad-free step counter and walk/run tracker for Android. The
repository is public so the privacy claims can be checked at the source: the merged manifest has
**no `INTERNET` permission**, the app ships no accounts, ads, analytics, crash-reporting or
cloud-sync SDKs, and `android:allowBackup="false"`.

## Project status — read this first

- **v1.3.0 (versionCode 3) is built, tested and linted. Distribution is GitHub Releases only —
  there is no Google Play listing.** No physical-device validation of v1.3 exists, and the Arabic
  strings have not had native review. The measured evidence and the open items are in
  [README › Verification status](README.md#verification-status) and
  [README › Honest limitations](README.md#honest-limitations).
- The project is a **single-owner release effort**. The owner is closing blockers that cannot be
  fixed by code (privacy-policy hosting, name clearance, Play declarations) — see
  `docs/legal/BLOCKERS.md`.
- Issues and focused pull requests are welcome. **Open an issue before starting anything
  non-trivial** so scope is agreed first; merges may be deferred until the release blockers close.
- Never write a claim the repository cannot prove: [Claims discipline](#claims-discipline) is
  enforced in review, not aspirational.

## Ground rules, condensed

| Never | Why |
|---|---|
| Add the `INTERNET` permission, a networking library, a WebView that loads remote content, or any `http(s)` call | "Offline" is the app's only provable claim. `ACCESS_NETWORK_STATE` is deliberately removed with `tools:node="remove"` — keep it removed. |
| Add analytics, crash reporting, ads, tracking, attribution or cloud sync | No data leaves the device. There is no exception, "just for development" included. |
| Add accounts, sign-in, sync, subscriptions or in-app purchases | Out of scope for the product. |
| Write to Health Connect, or add a Health Connect write permission | Health Connect is an **opt-in, read-only** step source. |
| Add background location, map tiles, geocoding or address search | Location exists only for a user-started walk and its foreground route. |
| Weaken privacy or platform defaults (`allowBackup`, data-extraction rules, exported components, foreground-service types, boot receiver filters) | These are the documented security surface — see [SECURITY.md](SECURITY.md). |
| Commit keystore material, passwords, tokens, personal data or `.env`-style secrets | Signing material is gitignored by design (`keystore/`, `keystore.properties`, `*.jks`, `*.keystore`); CI builds unsigned on purpose. |
| Copy code, strings, icons, illustrations or layouts from another product | Clean-room work only: public product research is fine, decompilation or copying is not. The local research copies under `.external/` are never published. |
| Bump `versionName`/`versionCode`, change `applicationId`, or rename the app | `com.sabeel.steps` and versionCode 3 / 1.3.0 are the frozen identity of the next upload. Versioning is owner-only. |
| Claim Play Store availability, a physical-device run of v1.3, or a completed native Arabic review | None of those are true today; writing them falsifies the thing the project exists to prove. |
| Commit machine-translated Arabic | Arabic must be real, well-formed MSA; the app's Arabic UI is gated on native review before release. |
| Add medical/health advice or medical-device claims | Sabeel counts steps. It is not health advice, a diagnosis, or a medical device. |

## Prerequisites

| Tool | Version used for every measured build | Notes |
|---|---|---|
| JDK | **21** (Temurin 21.0.13) | Set `JAVA_HOME`. The module compiles Java/Kotlin with target 17 (`sourceCompatibility`/`jvmTarget`), but the toolchain that produced the measured artifacts is JDK 21. |
| Android SDK | Platform **36** + build-tools **36.0.0** | Set `ANDROID_HOME`/`ANDROID_SDK_ROOT`; Android Studio writes `local.properties` (gitignored). `minSdk` 26, `compileSdk`/`targetSdk` 36. |
| Gradle | **Wrapper 8.11.1** (committed — do not install Gradle separately) | AGP 8.10.1, Kotlin 2.0.21, KSP 2.0.21-1.0.28 (`build.gradle.kts`). |
| Android Studio | Any version that supports AGP 8.10.1 | Optional; all commands below work from a terminal. |
| `adb` / an emulator | Optional | API 36 image for smoke tests. The ATD image has **no hardware step counter or detector**, so real counting cannot be validated there. |

## Build, test and lint

All commands are run from the repository root. Windows PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:ANDROID_HOME='C:\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME

.\gradlew.bat --console=plain :app:assembleDebug          # debug APK
.\gradlew.bat --console=plain :app:testDebugUnitTest      # JVM unit tests
.\gradlew.bat --console=plain :app:lintDebug              # lint gate (must exit 0)
.\gradlew.bat --console=plain :app:assembleRelease        # release APK (signed only if keystore.properties exists)
.\gradlew.bat --console=plain :app:bundleRelease          # AAB for Play
```

Linux/macOS:

```bash
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME

./gradlew --console=plain :app:assembleDebug
./gradlew --console=plain :app:testDebugUnitTest
./gradlew --console=plain :app:lintDebug
./gradlew --console=plain :app:assembleRelease
./gradlew --console=plain :app:bundleRelease
```

Notes:

- The gate runs recorded in `README.md` used `--no-daemon --console=plain`. Use the same flags
  when you need a clean, reproducible run.
- **Lint is a hard gate: build with exit 0 and no errors.** At v1.3.0 the baseline was
  0 errors / 80 warnings / 2 hints (2026-11-23). Warnings are not failures, but do not add errors
  and do not silence checks wholesale without a comment explaining why.
- **The unit-test suite must stay green.** At v1.3.0 the baseline was 205 tests in 18 suites,
  0 failures (2026-11-23). Test reports: `app/build/reports/tests/testDebugUnitTest/index.html`;
  lint report: `app/build/reports/lint-results-debug.html`.
- Building **release** does not require the upload key. Without `keystore.properties` the release
  APK/AAB are simply unsigned, and that is how CI builds them. Never add signing secrets to CI or
  to any committed file; the procedure is in `docs/release/KEYSTORE.md`.
- The release build is R8-minified with resource shrinking. If you change keep rules, call it out
  in the PR and say what you built and ran.

### Emulator QA and the debug step injector

On a **debug** build only, a broadcast receiver injects steps through the same snapshot path the
service uses, so counting UI and math can be exercised on an image with no step sensor:

```powershell
adb shell am broadcast -a com.sabeel.steps.DEBUG_STEPS --ei steps 100 -n com.sabeel.steps/.debug.DebugStepReceiver
```

That receiver lives in `app/src/debug/` and is never merged into a release build — keep it that
way. Emulator screenshots from the ATD image may be black; prefer `adb exec-out screencap -p` from
the host. Real counting (hardware counter/detector, Health Connect) still requires a physical phone.

## Project layout

```
app/src/main/java/com/sabeel/steps/
  domain/     pure Kotlin: models, step math/pipeline, window and session projection rules
  data/       Room v3 + DAOs, repositories, DataStore, CSV codec/IO, CSV/GPX export builders
  service/    StepCounterService (health FGS), WalkTrackingService (location|health FGS),
              WalkTracker, CountingController, SessionConfig
  platform/   permissions, notifications, reminders/workers, BootReceiver, CountingStore
  ui/         Compose theme/components/screens, view models, EN/AR strings
app/src/debug/  DebugStepReceiver — debug-only step injector (never shipped)
app/src/test/   JVM unit tests (pipeline, math, CSV, session projection/export, cadence)
app/schemas/    exported Room schemas (must be committed when the DB changes)
```

There is no `androidTest` source set: instrumented tests would need a device/emulator whose
sensors cannot produce real steps, so behaviour is covered by JVM tests plus manual smoke runs.

## Code style

- `.editorconfig` is the mechanical rule: UTF-8, LF, final newline, no trailing whitespace,
  4-space indent for Kotlin/KTS/Java/XML, 2-space for YAML/JSON, Markdown trailing spaces preserved.
- Kotlin follows the official style (`kotlin.code.style=official` in `gradle.properties`). No
  formatter or ktlint task is enforced in CI, so **match the file you are editing** and keep diffs
  small. Do not reformat untouched code in a behaviour PR.
- User-visible text belongs in string resources (`values/strings.xml` and the Arabic
  `values-ar/` copies) instead of being hardcoded in composables, and every new key needs both
  languages (see the pull-request checklist). Arabic text must be real MSA; mark it as awaiting
  native review.
- Comments explain *why*, especially for the step pipeline, receivers and anything that touches
  privacy or persistence. Do not add a comment that restates the code.
- Keep the privacy-critical code paths obvious: if a change touches permissions, exported
  components, backup rules or Health Connect, say so in the PR and add a test where practical.

## Commit conventions

The history follows the Conventional Commits shape: a lowercase type, an optional scope in
parentheses, then an imperative subject.

```text
type(scope): imperative subject

Body wrapped at about 80 columns: what changed and why, honest limits, then the evidence.
```

Examples taken from this repository's own history:

```text
feat(steps): count manifold with crash/boot recovery and HC floor
fix(ui): honest status cards, RTL-safe charts, EN/AR parity, walk resume and reliable delete-all
build(release): signed R8 release path, Room schema, CI, legal/owner documentation
docs(review): qiyas council review of external TrueStep prototypes
chore(strings): drop keys orphaned by the Settings rework
```

Rules:

- Types in use: `feat`, `fix`, `docs`, `build`, `chore`. `refactor`, `perf`, `test`, `style`,
  `ci` and `revert` are fine too when they describe the change honestly.
- Scopes in use: `steps`, `core`, `ui`, `i18n`, `settings`, `walk`, `brand`, `strings`, `git`,
  `design`, `release`, `verification`, `assets`. Invent a clear one when none fits.
- Subject: imperative mood, lowercase, no trailing period, ~72 characters max.
- Body: blank line after the subject, wrapped ~80 columns. Say what changed and why; list evidence
  (commands and their real output/numbers) for behaviour, build, version or claim changes; state
  what is still unverified instead of implying it is done.
- One logical change per commit. Do not mix formatting/renames with behaviour changes.
- The maintainer is the only committer; PRs may be squashed into one conventional commit.

## Pull requests

- Open an issue first for anything beyond a typo or a small, obvious fix.
- Keep the PR focused: one concern, no opportunistic refactors.
- CI (`.github/workflows/android.yml`) must pass: `assembleDebug`, `testDebugUnitTest`,
  `lintDebug`, `bundleRelease` (unsigned) on `ubuntu-latest` with JDK 21. Run the same tasks
  locally on your final tree — "BUILD SUCCESSFUL" is not evidence if you did not read the output.
- New or changed behaviour needs JVM tests in `app/src/test/`. Do not delete, ignore or weaken
  existing tests to make a change pass; explain honestly if a test must change.
- Room schema changes must include the exported schema JSON under `app/schemas/` and a real
  migration — there is no destructive fallback.
- User-visible changes need strings in **both** `app/src/main/res/values/strings.xml` and
  `app/src/main/res/values-ar/strings.xml` (and the `strings_sensor.xml` pair), with key parity.
  Mark new Arabic strings as awaiting native review; do not claim they are reviewed.
- Accessibility and RTL are part of the definition of done: content descriptions, TalkBack roles
  where the UI is interactive, touch targets of at least 48dp, and a layout that survives RTL.
- The reviewer checks scope, the privacy invariants, evidence and tests. Review is best-effort by a
  single maintainer: expect questions, and accept that a correct PR can still be deferred until the
  release blockers close. No merge timeline is promised.
- Documentation is bilingual: `README.md`/`README.ar.md` and the EN/AR release notes are kept
  in sync. If you edit one language, edit both, and mark new Arabic text as awaiting native review.
- Fill in the pull request template completely — especially the evidence and privacy checklists.

## Claims discipline

Every number, size, hash, date or behaviour statement in this repository must be produced by a
command someone actually ran. Review rejects:

- a count/hash/size/date copied from an earlier run or another branch and presented as current;
- a claim of Play Store availability, of physical-device validation of v1.3, or of completed native
  Arabic review — none of these are true today;
- emulator evidence described as device evidence (the API 36 ATD image has no step sensor);
- a SHA-256 prefix presented as a hash — cite the full 64-character value;
- a "fixed" or "verified" statement for something that was only unit-tested;
- broken promises hiding in prose: if something is not done, write "Not done", "Pending" or
  "Not released" and link the blocker.

When you change measured evidence, update the table(s) it belongs to (README *Verification status*,
CHANGELOG, `docs/release/CLAIMS-AUDIT.md`) in the same PR and say which command produced each row.
Never rebuild-and-cite different artifacts in one table without saying so.

## Licensing and attribution

- Sabeel is licensed under the **Apache License 2.0** (`LICENSE`). Contributions are accepted under
  the same license (**inbound = outbound**). There is **no CLA and no DCO sign-off requirement**,
  and no copyright assignment: keep your own copyright on what you write.
- Do not copy code or assets whose license is incompatible with Apache-2.0. If you add a
  dependency, check its license, keep it permissive, and record it in
  `app/src/main/assets/THIRD-PARTY-NOTICES.txt` (and `NOTICE` where required). Do not remove
  existing attribution.
- The app name "Sabeel" and the icon are original to this project (see `NOTICE`). Do not use them
  to imply endorsement of a fork or another product.

## Security and conduct

- Report vulnerabilities privately through [SECURITY.md](SECURITY.md) — never in a public issue or
  PR.
- Participation is covered by [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) (Contributor Covenant 2.1).
  Conduct reports go to the contact in that file, not to the security channel.

## Questions

Open an issue using the templates in `.github/ISSUE_TEMPLATE/`. If a template does not fit, say so
in the issue instead of guessing at our process.
