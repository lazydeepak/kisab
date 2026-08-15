import java.security.MessageDigest

plugins {
    id("com.android.application")
}

val releaseKeystorePath = providers.environmentVariable("KISAB_KEYSTORE_PATH")
val releaseKeystorePassword = providers.environmentVariable("KISAB_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("KISAB_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("KISAB_KEY_PASSWORD")

val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { it.isPresent && it.get().isNotBlank() }

val appVersionCode = 3
val appVersionName = "0.2.0"
val configuredUpdateManifestUrl = providers.gradleProperty("kisab.privateUpdateManifestUrl")
    .orNull
    ?.trim()
    .orEmpty()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

// Private-build expiry: immutable compile-time millis for this APK (not install date).
// Override with -Pkisab.privateBuildExpiresAtEpochMillis=... and/or
// -Pkisab.privateBuildExpiryEnabled=true|false when packaging a private drop.
val dayMillis = 24L * 60L * 60L * 1000L
val buildTimeEpochMillis = System.currentTimeMillis()
val configuredExpiryEnabled = providers.gradleProperty("kisab.privateBuildExpiryEnabled")
    .map { it.equals("true", ignoreCase = true) }
val configuredExpiryMillis = providers.gradleProperty("kisab.privateBuildExpiresAtEpochMillis")
    .map { it.toLong() }

android {
    namespace = "com.susankhya.kisab"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.susankhya.kisab"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Safe defaults: expiry machinery off unless a build type opts in.
        buildConfigField("boolean", "PRIVATE_BUILD_EXPIRY_ENABLED", "false")
        buildConfigField("long", "PRIVATE_BUILD_EXPIRES_AT_EPOCH_MILLIS", "0L")
        buildConfigField("String", "PRIVATE_UPDATE_MANIFEST_URL", "\"$configuredUpdateManifestUrl\"")
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseKeystorePath.get())
                storePassword = releaseKeystorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // Developer/debug APKs: policy present but far in the future so daily work is not locked.
            val debugExpiry = configuredExpiryMillis.orNull
                ?: (buildTimeEpochMillis + 365L * dayMillis)
            val debugEnabled = configuredExpiryEnabled.orNull ?: true
            buildConfigField("boolean", "PRIVATE_BUILD_EXPIRY_ENABLED", debugEnabled.toString())
            buildConfigField("long", "PRIVATE_BUILD_EXPIRES_AT_EPOCH_MILLIS", "${debugExpiry}L")
        }
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Private/preview release drops: 90-day build lifetime unless overridden.
            // Store/production builds can pass -Pkisab.privateBuildExpiryEnabled=false.
            val releaseEnabled = configuredExpiryEnabled.orNull ?: true
            val releaseExpiry = configuredExpiryMillis.orNull
                ?: (buildTimeEpochMillis + 90L * dayMillis)
            buildConfigField("boolean", "PRIVATE_BUILD_EXPIRY_ENABLED", releaseEnabled.toString())
            buildConfigField("long", "PRIVATE_BUILD_EXPIRES_AT_EPOCH_MILLIS", "${releaseExpiry}L")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

tasks.register("verifyReleaseSigningInputs") {
    group = "verification"
    description = "Fails clearly when release signing inputs are missing or incomplete."
    doFirst {
        if (!hasReleaseSigning) {
            throw GradleException(
                "Release signing inputs are missing. Set all of KISAB_KEYSTORE_PATH, " +
                    "KISAB_KEYSTORE_PASSWORD, KISAB_KEY_ALIAS, and KISAB_KEY_PASSWORD " +
                    "(environment variables locally, repository secrets in CI) to build a " +
                    "signed release. Debug builds do not require these inputs."
            )
        }
    }
}

tasks.matching { it.name == "packageRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn("verifyReleaseSigningInputs")
}

tasks.register("printVersionInfo") {
    group = "help"
    description = "Prints the application versionName and versionCode."
    doLast {
        println("versionName=${android.defaultConfig.versionName}")
        println("versionCode=${android.defaultConfig.versionCode}")
    }
}

tasks.register("verifyReleaseMetadata") {
    group = "verification"
    description = "Validates version metadata and requires version-matched release notes without reading signing secrets."
    doLast {
        require(Regex("[0-9]+\\.[0-9]+\\.[0-9]+").matches(appVersionName)) {
            "versionName must use MAJOR.MINOR.PATCH form: $appVersionName"
        }
        require(appVersionCode > 0) { "versionCode must be positive: $appVersionCode" }
        val releaseNotes = rootProject.file("docs/release/RELEASE_NOTES_${appVersionName}.md")
        require(releaseNotes.isFile) {
            "Release notes are required at ${releaseNotes.relativeTo(rootProject.projectDir)}"
        }
    }
}

val writeLocalVerificationEvidence = tasks.register("writeLocalVerificationEvidence") {
    group = "verification"
    description = "Writes machine-readable evidence after all local CI gates pass."
    dependsOn(
        "testDebugUnitTest",
        "lintDebug",
        "assembleDebug",
        "compileDebugAndroidTestKotlin"
    )
    val debugApk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
    val evidenceFile = layout.buildDirectory.file("reports/verification/local-ci-evidence.json")
    inputs.file(debugApk)
    inputs.property("versionName", appVersionName)
    inputs.property("versionCode", appVersionCode)
    inputs.property("gradleVersion", gradle.gradleVersion)
    inputs.property("javaVersion", System.getProperty("java.version"))
    outputs.file(evidenceFile)
    doLast {
        val apk = debugApk.get().asFile
        require(apk.isFile) { "Debug APK was not produced at $apk" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(apk.readBytes())
            .joinToString("") { byte -> "%02x".format(byte) }
        val report = evidenceFile.get().asFile
        report.parentFile.mkdirs()
        report.writeText(
            """{
  "schemaVersion": 1,
  "status": "passed",
  "versionName": "$appVersionName",
  "versionCode": $appVersionCode,
  "gradleVersion": "${gradle.gradleVersion}",
  "javaVersion": "${System.getProperty("java.version")}",
  "tasks": [
    ":app:testDebugUnitTest",
    ":app:lintDebug",
    ":app:assembleDebug",
    ":app:compileDebugAndroidTestKotlin"
  ],
  "debugApk": {
    "path": "app/build/outputs/apk/debug/app-debug.apk",
    "sizeBytes": ${apk.length()},
    "sha256": "$digest"
  }
}
"""
        )
        logger.lifecycle("Local CI evidence: ${report.relativeTo(rootProject.projectDir)}")
    }
}

tasks.register("verifyLocal") {
    group = "verification"
    description = "Runs the complete repeatable local CI gate and writes evidence."
    dependsOn(writeLocalVerificationEvidence)
}

dependencies {
    implementation("com.susankhya.foundation:foundation-session-android:0.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
