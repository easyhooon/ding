plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.easyhooon.ding.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.easyhooon.ding.sample"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(project(":ding"))
    releaseImplementation(project(":ding-noop"))
}
