plugins {
    id("pomsky.kmp-library")
    alias(libs.plugins.vanniktech.maven.publish)
    alias(libs.plugins.dokka)
}

mavenPublishing {
    coordinates(artifactId = "syntax")

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

        name.set("Pomsky-Kt Syntax")
        description.set("Lexer, parser, and AST for the Pomsky-Kt regex language")
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
