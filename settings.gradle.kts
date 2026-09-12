pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Auto-provisions a JDK 17 toolchain if one is not already installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AI-Notebook"

include(":app")
include(":core")
include(":common")
include(":domain")
include(":data")
include(":feature-canvas")
include(":feature-settings")
include(":feature-models")
include(":feature-search")
include(":feature-export")
// Handwriting-training flow: teaches the app the user's glyphs so replies can be written back in
// their own hand.
include(":feature-onboarding")
// Solve sheet, recognition-correction editor, and the bring-your-own-API-key provider manager.
include(":feature-ai")
