plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.binary.compatibility)
}

// Freezes the published API. `./gradlew apiDump` regenerates the .api files; a change that isn't
// dumped fails `apiCheck`, so a breaking change has to be an explicit, reviewable commit.
apiValidation {
    // Not published, so they have no API contract to keep.
    ignoredProjects += listOf("sample", "sample-app")
}

// ⚠️ Known gap: this covers :vitrine-processor only. BCV registers its tasks off the Kotlin
// JVM/multiplatform plugin, and :vitrine-annotations compiles through AGP 9's *built-in* Kotlin
// (applying org.jetbrains.kotlin.android on top of that breaks the build), so BCV never sees it.
// That is the wrong way round: the processor's public surface is one SPI class, while the
// annotations module is the actual consumer contract. Compensating control is explicitApi() there
// plus a surface small enough to review by eye — see vitrine-annotations/build.gradle.kts.

// AGP 9 ships Kotlin compilation built in — the Android module deliberately does NOT
// apply org.jetbrains.kotlin.android; applying it breaks the built-in kotlinc wiring.

// The version is normally the -SNAPSHOT in gradle.properties; the publish workflow
// overrides it from the pushed tag with -Pversion=X.Y.Z.
allprojects {
    group = providers.gradleProperty("group").get()
}
