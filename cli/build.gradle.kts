plugins {
    id("pomsky.kmp-cli")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib"))
            implementation(project(":decompiler"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.clikt)
            implementation(libs.mordant)
        }
    }
}
