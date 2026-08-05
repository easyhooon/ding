plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.easyhooon.notificationinspector.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.easyhooon.notificationinspector.sample"
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
    debugImplementation(project(":notification-inspector"))
    releaseImplementation(project(":notification-inspector-noop"))
}
