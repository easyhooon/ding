import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "io.github.easyhooon.ding.core"
        compileSdk = 36
        minSdk = 29

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = "io.github.easyhooon",
        artifactId = "ding-core",
        version = libs.versions.ding.get(),
    )

    pom {
        name.set("Ding Core")
        description.set("Kotlin Multiplatform notification snapshot core for Ding")
        inceptionYear.set("2026")
        url.set("https://github.com/easyhooon/ding")

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("easyhooon")
                name.set("Lee jihun")
                email.set("mraz3068@gmail.com")
            }
        }

        scm {
            url.set("https://github.com/easyhooon/ding")
            connection.set("scm:git:git://github.com/easyhooon/ding.git")
            developerConnection.set("scm:git:ssh://git@github.com/easyhooon/ding.git")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}
