plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// A runnable host for :sample's generated catalog — this is what the README screenshots show.
// Deliberately NOT published, and deliberately separate from :sample: converting :sample into an
// application module would change what the CI gate proves (a clean library compile of generated
// code) into an unsigned-release-APK path.
android {
    namespace = "io.github.simtop.vitrine.sampleapp"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.simtop.vitrine.sampleapp"
        minSdk = libs.versions.minSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    // implementation, not ksp: the generated provider and its ServiceLoader entry live in :sample's
    // AAR, and loadVitrineItems() finds them only if the AAR is on the runtime classpath.
    implementation(project(":sample"))
    implementation(project(":vitrine-annotations"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
}
