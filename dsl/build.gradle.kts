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
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

mavenPublishing {
    coordinates(artifactId = "dsl")

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

        name.set("Pomsky-Kt DSL")
        description.set("Type-safe Kotlin DSL for building Pomsky expressions")
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
