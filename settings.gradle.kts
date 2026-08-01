rootProject.name = "vitrine"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":vitrine-annotations")
include(":vitrine-processor")
// Not published: the end-to-end proof that generated code compiles. See sample/build.gradle.kts.
include(":sample")
// Runnable host for :sample's catalog - the README screenshots come from here.
include(":sample-app")
