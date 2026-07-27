# Release Checklist — AI Notebook

Semantic versioning `MAJOR.MINOR.PATCH`. Current: `versionName 1.0.0`, `versionCode 1` (in `app/build.gradle.kts`).

## Build variants
- **debug** — `applicationIdSuffix .debug`, `versionNameSuffix -debug`, not minified. Installs side-by-side with release.
- **release** — minified + resource-shrunk + obfuscated (R8), signed. Distribute as an **App Bundle (.aab)**.

Large GGUF models are **never bundled** — they download at runtime into `files/models`.

## Signing
1. Generate a key (once):
   `keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ai-notebook`
2. Copy `keystore.properties.template` → `keystore.properties`, fill in values, place `release.jks` at the repo root.
3. Both files are gitignored. If `keystore.properties` is absent the release build is produced **unsigned** (use with Play App Signing).

## Build commands
- `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
- `./gradlew :app:assembleDebug` for a debuggable APK.
- `./gradlew test` (unit) and `./gradlew connectedAndroidTest` (instrumented: migration + Hilt) on a device.

## Quality gates (must pass before release)
- [ ] `./gradlew lint` clean of errors
- [ ] Unit tests pass (`./gradlew test`)
- [ ] Instrumented tests pass (migration v1→v2, UI)
- [ ] R8 release build succeeds and runs (verifies serialization keep rules)
- [ ] No data-loss / migration regressions
- [ ] Docs + release notes updated

## R8 / obfuscation
Keep rules in `app/proguard-rules.pro` preserve kotlinx.serialization (config + `.ainb`) and the llama.cpp JNI boundary. Room / Hilt / Compose / ML Kit ship their own consumer rules.

## Staged rollout
5% → 20% → 50% → 100%, monitoring crash rate / ANRs between steps.

## Rollback if
Crash-rate spike, data corruption, critical AI failure, or severe perf regression. Notebooks remain compatible across versions (versioned DB migrations + `.ainb` manifest).

## Post-release validation (smoke test)
Install → cold start (splash) → create notebook → draw / erase / undo-redo → rename / delete → export + share → import → Model Manager (list + download + activate) → Settings persist across restart → Search.

## Known follow-ups
- Native llama.cpp `.so` (on-device inference) — see `data/src/main/cpp/README.md`.
- AI response → editable handwriting strokes on the canvas.
