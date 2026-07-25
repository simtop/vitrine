plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "vitrine-processor", version.toString())

    pom {
        name.set("Vitrine Processor")
        description.set(
            "KSP processor that generates an interactive Jetpack Compose component catalog " +
                "from @VitrineComponent-annotated composables."
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
