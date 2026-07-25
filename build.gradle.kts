plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.maven.publish) apply false
}

// AGP 9 ships Kotlin compilation built in — the Android module deliberately does NOT
// apply org.jetbrains.kotlin.android; applying it breaks the built-in kotlinc wiring.

// The version is normally the -SNAPSHOT in gradle.properties; the publish workflow
// overrides it from the pushed tag with -Pversion=X.Y.Z.
allprojects {
    group = providers.gradleProperty("group").get()
}
