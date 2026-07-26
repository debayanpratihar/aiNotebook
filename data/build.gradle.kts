plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.debayan.ainotebook.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // ---------------------------------------------------------------------------------------------
    // On-device inference (llama.cpp) native build. Intentionally DISABLED: enabling it before
    // vendoring llama.cpp would fail the build. See data/src/main/cpp/README.md to turn it on.
    //
    // defaultConfig {
    //     ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    // }
    // externalNativeBuild {
    //     cmake {
    //         path = file("src/main/cpp/CMakeLists.txt")
    //         version = "3.22.1"
    //     }
    // }
    // ---------------------------------------------------------------------------------------------
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
}
