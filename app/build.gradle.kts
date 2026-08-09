plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

android {
    namespace = "com.susankhya.kisab"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.susankhya.kisab"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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

dependencies {
    implementation("com.susankhya.foundation:foundation-session-android:0.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
