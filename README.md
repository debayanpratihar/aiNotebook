# AI Notebook

Offline-first, on-device AI handwriting notebook for Android. See the specification in
`../AI-NOTEBOOK-SPEC/` (single source of truth).

## Status

**Phase 1 — Project Foundation: complete.** Build, modules, DI, navigation, database, preferences,
background work, theme.

**Phase 2 — Canvas, drawing engine & notebook storage: complete.** Infinite world-coordinate canvas
with pan/pinch-zoom (10%–1000%), vector stroke rendering (viewport-culled + path-cached),
Catmull-Rom smoothing, pen/pencil/marker/highlighter + stroke eraser, transactional stroke/page/layer
persistence, per-stroke autosave, and persistent command-based undo/redo.

**Phase 3 — OCR, AI engine, model manager & settings: complete.** Remote config + model catalog,
device-compatibility recommendation, resumable/verified model downloads (WorkManager), on-device OCR
(ML Kit) → search index, the AI engine architecture with streaming generation + prompt building
(native llama.cpp behind a graceful-degradation boundary — see `data/src/main/cpp/README.md`),
DataStore-backed settings, and Model Manager / Settings / Search / AI-panel UI.

**Phase 4 — export/import, performance, security & testing: complete.** Lossless native `.ainb`
package (validated, id-remapped import), PDF + PNG/JPEG export with share-sheet, canvas viewport
culling + path caching, and a test suite (unit tests, Room v1→v2 migration test, Hilt test runner).

Remaining: Play Store release preparation (Phase 5).

## Two documented follow-ups (require a device/native build)
1. **Native inference** — vendor llama.cpp + NDK build to enable on-device generation.
2. **AI → handwriting strokes** — render AI text as editable vector strokes on the canvas
   (currently shown in the AI panel).

## Tech stack

Kotlin · Jetpack Compose · Material 3 · MVVM + Clean Architecture · Hilt · Room · DataStore ·
WorkManager · Coroutines/Flow. Min SDK 26, target/compile SDK 35, JDK 17.

## Module map

| Module | Type | Responsibility |
|--------|------|----------------|
| `:app` | Android app | Composition root: `Application`, `MainActivity`, navigation, theme wiring |
| `:common` | Android lib (Compose) | Material 3 design system + reusable UI components |
| `:domain` | Kotlin/JVM | Models, repository interfaces, use-case base classes (no Android) |
| `:data` | Android lib | Room, DataStore, repository impls, WorkManager, Hilt modules |
| `:core` | Kotlin/JVM | Framework-free primitives: dispatchers, `AppResult`, `AppError`, logging, time |

Dependency direction: `app → {common, data, domain, core}`, `data → {domain, core}`,
`common → (compose only)`, `domain → core`. `:core` and `:domain` never depend on Android.

## Building

The Gradle **wrapper jar** is not checked in (binary). To generate it, either:

- Open the project in Android Studio (Ladybug+), which provisions the wrapper automatically, **or**
- Run `gradle wrapper --gradle-version 8.9` with a local Gradle, then `./gradlew assembleDebug`.

A launcher icon is intentionally deferred to the Play Store release phase.

## Conventions

- Compose only — no XML layouts (the single host theme in `res/values` is not a layout).
- Immutable UI state exposed as `StateFlow`; business logic never lives in composables.
- All persistence is off the main thread; repositories return `AppResult` for typed failures.
- Room migrations are explicit only — no destructive fallback.
