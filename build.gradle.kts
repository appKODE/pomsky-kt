plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

group = providers.gradleProperty("pomGroupId").get()
version = providers.gradleProperty("versionName").get()

subprojects {
    group = rootProject.group
    version = rootProject.version
}
