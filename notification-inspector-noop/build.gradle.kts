plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "io.github.easyhooon.notificationinspector"
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
        artifactId = "notification-inspector-noop",
        version = libs.versions.notification.inspector.get(),
    )

    pom {
        name.set("Notification Inspector No-Op")
        description.set("No-op Notification Inspector implementation for release builds")
        inceptionYear.set("2026")
        url.set("https://github.com/easyhooon/notification-inspector")

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
            url.set("https://github.com/easyhooon/notification-inspector")
            connection.set("scm:git:git://github.com/easyhooon/notification-inspector.git")
            developerConnection.set("scm:git:ssh://git@github.com/easyhooon/notification-inspector.git")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}
