import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("pomsky.kmp-library")
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries {
            sharedLib {
                baseName = "pomsky"
            }
            staticLib {
                baseName = "pomsky"
            }
        }
    }
}
