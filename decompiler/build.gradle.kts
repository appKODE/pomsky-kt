plugins {
    id("pomsky.kmp-library")
    alias(libs.plugins.vanniktech.maven.publish)
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

mavenPublishing {
    coordinates(artifactId = "decompiler")

    publishToMavenCentral()
    signAllPublications()

    pom {
        val pomDescription: String by project
        val pomUrl: String by project
        val pomScmUrl: String by project
        val pomScmConnection: String by project
        val pomScmDevConnection: String by project
        val pomLicenseName: String by project
        val pomLicenseUrl: String by project
        val pomLicenseDist: String by project
        val pomDeveloperId: String by project
        val pomDeveloperName: String by project

        name.set("Pomsky-Kt Decompiler")
        description.set("Regex to Pomsky DSL decompiler")
        url.set(pomUrl)

        scm {
            url.set(pomScmUrl)
            connection.set(pomScmConnection)
            developerConnection.set(pomScmDevConnection)
        }
        licenses {
            license {
                name.set(pomLicenseName)
                url.set(pomLicenseUrl)
                distribution.set(pomLicenseDist)
            }
        }
        developers {
            developer {
                id.set(pomDeveloperId)
                name.set(pomDeveloperName)
            }
        }
    }
}
