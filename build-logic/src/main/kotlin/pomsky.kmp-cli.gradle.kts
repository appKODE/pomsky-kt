import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("pomsky.kmp-library")
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries {
            executable {
                entryPoint = "ru.kode.pomskykt.cli.main"
                baseName = "pomsky"
            }
        }
    }
}
