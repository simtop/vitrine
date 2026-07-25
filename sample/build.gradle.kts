plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

// This module is the processor's end-to-end test, not a shipped artifact: it runs Vitrine over
// real annotated composables and compiles the generated code against real Compose. If code
// generation regresses — a bad import, an unresolved member, an invalid class name — this module
// fails to compile. Deliberately not published.
android {
    namespace = "io.github.simtop.vitrine.sample"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

ksp {
    arg("vitrine.generatedPackage", "io.github.simtop.vitrine.sample.generated")
    arg("vitrine.providerName", "SampleVitrineProvider")
}

dependencies {
    implementation(project(":vitrine-annotations"))
    ksp(project(":vitrine-processor"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
}
