plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.startup)
    api(platform(libs.firebase.bom))
    api(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    detektPlugins(libs.detekt.formatting)
}

mavenPublishing {
    coordinates(
        groupId = "io.github.easyhooon",
        artifactId = "ding",
        version = libs.versions.ding.get(),
    )

    pom {
        name.set("Ding")
        description.set("Android notification payload inspector for debug builds")
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
