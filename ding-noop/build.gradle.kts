plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "io.github.easyhooon.ding"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(platform(libs.firebase.bom))
    api(libs.firebase.messaging)

    detektPlugins(libs.detekt.formatting)
}

mavenPublishing {
    coordinates(
        groupId = "io.github.easyhooon",
        artifactId = "ding-noop",
        version = libs.versions.ding.get(),
    )

    pom {
        name.set("Ding No-Op")
        description.set("No-op Ding implementation for release builds")
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
