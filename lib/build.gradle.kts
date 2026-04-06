plugins {
    id("pomsky.kmp-library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.vanniktech.maven.publish)
    alias(libs.plugins.dokka)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":syntax"))
            implementation(libs.kotlinx.serialization.json)
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.junit5.engine)
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

mavenPublishing {
    coordinates(artifactId = "lib")

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

        name.set("Pomsky-Kt Lib")
        description.set("Pomsky-Kt regex language compiler")
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
