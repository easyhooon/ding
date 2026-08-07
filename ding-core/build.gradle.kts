import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

val appleFrameworkName = "Ding"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    explicitApi()

    val appleXCFramework = XCFramework(appleFrameworkName)

    androidLibrary {
        namespace = "io.github.easyhooon.ding.core"
        compileSdk = 36
        minSdk = 29

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        withHostTest {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = appleFrameworkName
            isStatic = true
            appleXCFramework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        iosMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

val releaseXCFramework = layout.buildDirectory.dir(
    "XCFrameworks/release/$appleFrameworkName.xcframework",
)
val swiftPackageReleaseDirectory = layout.buildDirectory.dir("swiftpm/release")
val swiftPackageArchive = swiftPackageReleaseDirectory.map {
    it.file("$appleFrameworkName.xcframework.zip")
}

val zipDingXCFramework by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Archives the release Ding XCFramework for SwiftPM distribution."
    dependsOn("assemble${appleFrameworkName}ReleaseXCFramework")
    from(releaseXCFramework) {
        into("$appleFrameworkName.xcframework")
    }
    archiveFileName.set("$appleFrameworkName.xcframework.zip")
    destinationDirectory.set(swiftPackageReleaseDirectory)
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val packageDingSwiftPM by tasks.registering {
    group = "distribution"
    description = "Builds the SwiftPM XCFramework archive and checksum."
    dependsOn(zipDingXCFramework)
    val checksumFile = swiftPackageReleaseDirectory.map {
        it.file("$appleFrameworkName.xcframework.zip.sha256")
    }
    inputs.file(swiftPackageArchive)
    outputs.file(checksumFile)
    doLast {
        val checksum = providers.exec {
            commandLine(
                "swift",
                "package",
                "compute-checksum",
                swiftPackageArchive.get().asFile.absolutePath,
            )
        }.standardOutput.asText.get().trim()
        checksumFile.get().asFile.writeText("$checksum\n")
    }
}

val prepareDingLocalSwiftPackage by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Prepares a local Swift package containing the release XCFramework."
    dependsOn("assemble${appleFrameworkName}ReleaseXCFramework")
    from(rootProject.layout.projectDirectory.file("swiftpm/Package.swift"))
    from(releaseXCFramework) {
        into("Artifacts/$appleFrameworkName.xcframework")
    }
    into(layout.buildDirectory.dir("swiftpm/local"))
}

val verifyDingSwiftImport by tasks.registering(Exec::class) {
    group = "verification"
    description = "Type-checks Ding's exported API with the Swift compiler."
    dependsOn(prepareDingLocalSwiftPackage)
    val simulatorSdkPath = providers.exec {
        commandLine("xcrun", "--sdk", "iphonesimulator", "--show-sdk-path")
    }.standardOutput.asText.map { it.trim() }
    doFirst {
        commandLine(
            "xcrun",
            "swiftc",
            "-typecheck",
            "-target",
            "arm64-apple-ios13.0-simulator",
            "-sdk",
            simulatorSdkPath.get(),
            "-F",
            layout.buildDirectory
                .dir("swiftpm/local/Artifacts/$appleFrameworkName.xcframework/ios-arm64-simulator")
                .get()
                .asFile
                .absolutePath,
            rootProject.layout.projectDirectory.file("swiftpm/Smoke.swift").asFile.absolutePath,
        )
    }
}

tasks.register<Exec>("verifyDingLocalSwiftPackage") {
    group = "verification"
    description = "Verifies that SwiftPM resolves the local Ding binary target."
    dependsOn(prepareDingLocalSwiftPackage, verifyDingSwiftImport)
    commandLine(
        "swift",
        "package",
        "describe",
        "--package-path",
        layout.buildDirectory.dir("swiftpm/local").get().asFile.absolutePath,
    )
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
