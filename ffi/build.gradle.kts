plugins {
    id("pomsky.kmp-ffi")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib"))
            implementation(project(":decompiler"))
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
