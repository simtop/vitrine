plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "io.github.simtop.vitrine"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // Deliberately lower than the sample's: this module is an annotation, two data types and a
        // ServiceLoader call — it touches no Android API, so there is no reason to tax consumers
        // with a higher floor. Raising it later would be a breaking change; lowering it is not.
        minSdk = libs.versions.minSdkLibrary.get().toInt()
    }

    // No `publishing { singleVariant(...) }` here: the vanniktech plugin already declares the
    // release variant (with sources + javadoc jars), and declaring it twice is a hard AGP error.
}

dependencies {
    api(platform(libs.compose.bom))
    // api, not implementation: VitrineItem exposes @Composable in its public signature,
    // so consumers compile against the Compose runtime transitively.
    api(libs.compose.runtime)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "vitrine-annotations", version.toString())

    pom {
        name.set("Vitrine Annotations")
        description.set(
            "Annotations and runtime types for Vitrine, the KSP-generated interactive " +
                "component catalog for Jetpack Compose."
        )
        inceptionYear.set("2026")
        url.set("https://github.com/simtop/vitrine")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("simtop")
                name.set("simtop")
                url.set("https://github.com/simtop")
            }
        }
        scm {
            url.set("https://github.com/simtop/vitrine")
            connection.set("scm:git:git://github.com/simtop/vitrine.git")
            developerConnection.set("scm:git:ssh://git@github.com/simtop/vitrine.git")
        }
    }
}
