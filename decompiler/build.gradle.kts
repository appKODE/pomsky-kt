plugins {
    id("pomsky.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":lib"))
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.junit5.engine)
            }
            // Include lib's test resources (testcases/) for round-trip tests
            resources.srcDir(project(":lib").file("src/jvmTest/resources"))
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
