plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Exposed as api: core types (AppResult, AppError) and Flow appear in domain's public
    // repository/use-case signatures, so consumers must receive them transitively.
    api(project(":core"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
