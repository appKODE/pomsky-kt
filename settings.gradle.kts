pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "pomsky-kt"

include(":syntax")
include(":lib")
include(":dsl")
include(":decompiler")
include(":ffi")
include(":cli")
