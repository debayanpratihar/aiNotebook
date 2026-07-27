plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The on-device inference (llama.cpp) native build turns on automatically once llama.cpp is vendored
// at data/src/main/cpp/llama.cpp. Until then the app builds normally with AI gracefully unavailable.
val llamaNativeDir = file("src/main/cpp/llama.cpp")
val llamaNativeEnabled = llamaNativeDir.exists()

android {
    namespace = "com.debayan.ainotebook.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        if (llamaNativeEnabled) {
            ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
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

    // Networking + serialization (remote model configuration)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // On-device OCR
    implementation(libs.mlkit.text.recognition)

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

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.room.testing)
}
