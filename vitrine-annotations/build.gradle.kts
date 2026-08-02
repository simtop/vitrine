plugins {
    // A plain JVM library, not an Android one: nothing here touches an Android API — it is two
    // annotations, two types and a ServiceLoader call. Publishing a jar rather than an AAR drops
    // the minSdk floor entirely and, the reason this changed, lets
    // binary-compatibility-validator see the module. BCV keys off the Kotlin JVM plugin and cannot
    // see a module compiled by AGP 9's built-in Kotlin, so as an AAR the actual consumer contract
    // was the one thing left unguarded.
    alias(libs.plugins.kotlin.jvm)
    // @Composable appears in VitrineItem's signature, so this module needs the Compose compiler
    // even though it draws nothing. Same arrangement Compose Desktop uses.
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
}

// The module consumers actually import, so its public surface is the API contract.
kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    // api, not implementation: VitrineItem exposes @Composable in its public signature,
    // so consumers compile against the Compose runtime transitively. Left unversioned here and
    // pinned by the catalog — Gradle metadata resolves the right variant per consumer, so an
    // Android consumer still gets runtime-android from this jar's dependency.
    api(libs.compose.runtime.get().module.toString() + ":" + libs.versions.composeRuntime.get())
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
