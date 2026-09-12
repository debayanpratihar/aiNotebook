import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Optional built-in cloud keys, so a fresh install can answer non-math questions before the user has
// configured anything. Read from the gitignored local.properties or the environment at build time and
// baked into BuildConfig — never committed. See `.env.example`.
//
// Two slots because a free tier is rate-limited per key: the failover coordinator treats them as two
// providers and rotates on 429. When both are blank, which is the case for any clone of this repo,
// the app simply ships with no provider configured and the UI asks the user for their own key. The
// user can disable or delete the built-in provider at any time in AI & Providers.
//
// BuildConfig is not a secret store: anything here is extractable from the APK. These slots are only
// appropriate for a free-tier key you are willing to publish. A user's own key never travels this
// path — it goes to the keystore-backed vault at runtime.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}

fun buildKey(name: String): String =
    (localProperties.getProperty(name) ?: System.getenv(name) ?: "").trim()

// The on-device llama.cpp/GGUF backend is built only when llama.cpp is vendored at
// data/src/main/cpp/llama.cpp AND the opt-out property is not set. Without it the app builds and
// runs normally: MediaPipe remains the default local backend and the GGUF path reports itself
// unavailable.
//
// The `ainotebook.native.llama` escape hatch exists because vendoring llama.cpp is a git submodule
// checkout while *compiling* it additionally needs the NDK and CMake. CI runners and contributors
// who have the submodule but not the toolchain would otherwise hit a CMake failure on a build that
// has no reason to touch native code at all. Set `-Painotebook.native.llama=false` to skip it.
val llamaNativeOptOut = (findProperty("ainotebook.native.llama") as String?)?.equals("false", ignoreCase = true) == true
val llamaNativeDir = file("src/main/cpp/llama.cpp")
val llamaNativeEnabled = llamaNativeDir.exists() && !llamaNativeOptOut

android {
    namespace = "com.debayan.ainotebook.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BUILTIN_GROQ_KEY_PRIMARY", "\"${buildKey("GROQ_API_KEY_PRIMARY")}\"")
        buildConfigField("String", "BUILTIN_GROQ_KEY_SECONDARY", "\"${buildKey("GROQ_API_KEY_SECONDARY")}\"")

        if (llamaNativeEnabled) {
            // arm64-v8a covers every phone shipped since ~2016 and is the only ABI where the int8
            // dot-product / fp16 paths that make quantized inference tolerable exist. armeabi-v7a is
            // deliberately excluded: it has no dotprod/i8mm, so a 1B model decodes at a rate that
            // reads as broken. 32-bit devices still install the app and get the offline math engine
            // plus cloud providers; only the GGUF backend is absent. x86_64 is for the emulator.
            ndk { abiFilters += listOf("arm64-v8a", "x86_64") }

            externalNativeBuild {
                cmake {
                    // Release-optimized native code even in debug builds of the app — an -O0 ggml is
                    // roughly an order of magnitude slower and would make every local-inference
                    // measurement during development meaningless.
                    arguments += listOf("-DCMAKE_BUILD_TYPE=Release")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    // Make exported Room schemas available to instrumented migration tests.
    sourceSets.getByName("androidTest").assets.srcDir(files("$projectDir/schemas"))

    // On-device inference (llama.cpp) native build — enabled only when llama.cpp is vendored.
    // See data/src/main/cpp/README.md for the one-time vendoring + NDK setup steps.
    if (llamaNativeEnabled) {
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
}

// Persist exported Room schemas for migration validation.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Networking + serialization (remote model configuration, cloud AI providers)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)

    // Handwriting recognition (primary: stroke-based) + printed-text OCR (fallback for imported images)
    implementation(libs.mlkit.digital.ink)
    implementation(libs.mlkit.text.recognition)

    // On-device generation — default backend (GPU-accelerated, no NDK build required)
    implementation(libs.mediapipe.tasks.genai)

    // WorkManager + Hilt worker support
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.room.testing)
    // Drives the cloud-provider failover tests against a real HTTP stack (429 / 5xx / timeout /
    // malformed-SSE) rather than a mocked client, so the retry logic is exercised end to end.
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.room.testing)
}
