import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvmToolchain(17)

    jvm()

    macosArm64()
    macosX64()
    linuxX64()
    mingwX64()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val nativeMain by creating { dependsOn(commonMain) }
        val nativeTest by creating { dependsOn(commonTest) }

        val jvmMain by getting
        val jvmTest by getting

        val macosMain by creating { dependsOn(nativeMain) }
        val macosArm64Main by getting { dependsOn(macosMain) }
        val macosX64Main by getting { dependsOn(macosMain) }

        val linuxMain by creating { dependsOn(nativeMain) }
        val linuxX64Main by getting { dependsOn(linuxMain) }

        val mingwMain by creating { dependsOn(nativeMain) }
        val mingwX64Main by getting { dependsOn(mingwMain) }

        val macosTest by creating { dependsOn(nativeTest) }
        val macosArm64Test by getting { dependsOn(macosTest) }
        val macosX64Test by getting { dependsOn(macosTest) }

        val linuxTest by creating { dependsOn(nativeTest) }
        val linuxX64Test by getting { dependsOn(linuxTest) }

        val mingwTest by creating { dependsOn(nativeTest) }
        val mingwX64Test by getting { dependsOn(mingwTest) }
    }

}
