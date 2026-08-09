# Gradle Build Guide — Kunjachaya Club Android App

Focused, step-by-step guide to actually getting `./gradlew` to produce an APK. For the wider project (web app, Firebase, Cloud Functions), see [BUILD.md](./BUILD.md).

---

## 0. What you need before running anything

| Requirement | Why |
|---|---|
| JDK 17 | Required by AGP 9.1.1 |
| Android SDK, platform 36 + build-tools | `compileSdk = 36.1`, `targetSdk = 36` in `app/build.gradle.kts` |
| A debug keystore at the repo root (`debug.keystore`) | The `debug` build type's signing config points at it, and it's intentionally **not** committed (see §2) |
| Real Firebase Android API key in `app/google-services.json` | Without it, the app still builds and runs — it just stays in local-only mode (see BUILD.md §0) |

You do **not** need Gradle installed globally — the wrapper (`./gradlew`) is fully self-contained now (see §1).

---

## 1. The Gradle wrapper — already set up

`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties` (pinned to **Gradle 9.1.0**, which AGP 9.1.1 requires — a hard requirement, not a suggestion), and `gradle/wrapper/gradle-wrapper.jar` itself are all committed to this repo. `./gradlew` works immediately on any machine — Windows, macOS, Linux — with zero setup, as long as `gradlew` has execute permission on Unix-like systems:
```bash
chmod +x gradlew   # only needed if it isn't already executable after cloning/extracting
```

That's it — no separate bootstrap step needed. (Earlier revisions of this project shipped without the wrapper jar and needed a one-time `gradle wrapper --gradle-version 9.1.0` bootstrap; that's no longer necessary now that the jar itself is committed.)

---

## 2. Generate the debug keystore

`app/build.gradle.kts` defines the `debug` build type's signing config as:
```kotlin
create("debugConfig") {
    storeFile = file("${rootDir}/debug.keystore")
    storePassword = "android"
    keyAlias = "androiddebugkey"
    keyPassword = "android"
}
```
This file is correctly excluded from version control (`.gitignore`) — debug keystores are local, disposable, and never meant to be shared. Generate one that matches these exact values:

```bash
keytool -genkey -v \
  -keystore debug.keystore \
  -storepass android \
  -alias androiddebugkey \
  -keypass android \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

Run this once, at the repo root, before your first `assembleDebug`. (If you already have Android Studio's own default debug keystore at `~/.android/debug.keystore`, you can instead copy that to `debug.keystore` at the repo root — it uses the same standard alias/password by convention.)

Release builds use a separate `release` signing config that reads `KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_PASSWORD` from the environment — see §5.

---

## 3. Core Gradle tasks

| Command | What it does |
|---|---|
| `./gradlew :app:assembleDebug` | Builds the debug APK → `app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew :app:assembleRelease` | Builds the signed release APK (needs §5 env vars set) |
| `./gradlew clean` | Removes all build outputs |
| `./gradlew :app:testDebugUnitTest` | Runs JUnit + Robolectric unit tests |
| `./gradlew :app:verifyRoborazziDebug` | Compares Compose UI screenshots against stored baselines |
| `./gradlew :app:recordRoborazziDebug` | Re-records Roborazzi baselines after an intentional UI change |
| `./gradlew :app:lint` | Runs Android Lint |
| `./gradlew --stop` | Stops any running Gradle daemons (useful if a build hangs) |
| `./gradlew tasks` | Lists every available task for this project |

Full clean rebuild when something seems stale:
```bash
./gradlew clean :app:assembleDebug --stacktrace
```

---

## 4. What was cleaned up to get here

Two things were fixed in this project specifically so `./gradlew` has a chance of succeeding cleanly:

- **A stray extra `}` in `FinancialsScreen.kt`** — a real, silent brace mismatch that would have failed compilation with a confusing error pointing at the wrong location. Found via a full brace-balance sweep across all 58 Kotlin files, fixed.
- **An orphaned Hilt module (`di/NetworkModule.kt`)** — used `@Module`/`@InstallIn` (Hilt annotations) while the Hilt Gradle plugin was never actually applied anywhere in the build, which would have failed compilation. The file was also completely unused (nothing in the app injects anything from it) and pointed at a fake generic REST endpoint (`https://api.example.com/`) unrelated to this app's real Firebase-based architecture. Deleted, along with the now-unused `hilt-android`, `hilt-compiler`, `retrofit`, `okhttp`, and `moshi` dependencies in `app/build.gradle.kts`.

Both were pre-existing issues in the original scaffold, not introduced by later changes — worth knowing in case similar dead/half-wired code shows up elsewhere as the project grows.

Since then, `gradle-wrapper.jar` has also been committed directly (see §1) — the earlier "generate it yourself" bootstrap step in this guide no longer applies.

---

## 5. Release builds (signed)

Set these environment variables before running `assembleRelease` — there's no default, on purpose (a hardcoded default signing key would be a real security problem):

```bash
export KEYSTORE_PATH=/path/to/your/real-upload-key.jks
export STORE_PASSWORD=<your real store password>
export KEY_PASSWORD=<your real key password>
./gradlew :app:assembleRelease
```

`keyAlias` is hardcoded to `"upload"` in `app/build.gradle.kts` — make sure your real keystore uses that alias, or update the build file to match your actual one.

---

## 6. Troubleshooting

- **`Keystore file '.../debug.keystore' not found`** → run §2.
- **`Could not resolve all files for configuration ':app:...'` / dependency resolution errors** → this environment needs real network access to Google's and Maven Central's repositories; nothing about the dependency list itself should be broken (checked against the version catalog), but a fully offline environment can't resolve any of it.
- **Hilt / Dagger-related compile errors** → shouldn't happen anymore (see §4), but if new code reintroduces `@Module`/`@HiltAndroidApp`/`@Inject`, the Hilt Gradle plugin (`libs.plugins.hilt` is still defined in the version catalog, just unapplied) needs to be added back to both `build.gradle.kts` (root, as `apply false`) and `app/build.gradle.kts`, plus `@HiltAndroidApp` added to `MainApplication.kt`.
- **`Firebase API Key is not configured` at runtime (not a build error)** → expected until you add your real key per BUILD.md §0; the app still builds and runs in local-only mode.
- **Gradle daemon seems stuck** → `./gradlew --stop`, then retry.
