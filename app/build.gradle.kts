import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

/**
 * Release-only secrets: AdMob unit IDs and the upload keystore.
 *
 * Read from `local.properties` (gitignored) or `-P` / `~/.gradle/gradle.properties`,
 * so nothing real is ever committed. Keys:
 *
 *   maigexr.admob.appId, maigexr.admob.bannerId, maigexr.admob.interstitialId
 *   maigexr.signing.storeFile, maigexr.signing.storePassword,
 *   maigexr.signing.keyAlias, maigexr.signing.keyPassword
 */
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String): String? =
    (findProperty(key) as String?) ?: localProps.getProperty(key)

val isReleaseBuildRequested = gradle.startParameter.taskNames.any { it.contains("Release") }

/**
 * A release AdMob ID, or a hard failure when a release is being built without one.
 *
 * Shipping Google's sample IDs breaks AdMob policy, and shipping the old
 * `ca-app-pub-XXXX` placeholders makes the SDK reject the app ID. Neither may
 * reach the Play Console, so a release build stops here instead. Debug builds and
 * IDE syncs never ask for these.
 */
fun releaseAdId(key: String): String {
    val value = secret(key)
    if (value.isNullOrBlank() && isReleaseBuildRequested) {
        throw GradleException("Release build needs $key in local.properties. See docs/release/RESUME-HERE.md in iOSMaigeXr.")
    }
    return value ?: "unset"
}

android {
    namespace = "com.xraiassistant"
    compileSdk = 36

    defaultConfig {
        // Store identity. Must match iOS and cannot change after first publish.
        applicationId = "studio.seacloud9.maigexr"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // AdMob manifest placeholder (overridden per buildType below)
        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
    }

    signingConfigs {
        // Only defined when the keystore is configured, so debug builds and
        // fresh checkouts keep working without it.
        val storeFile = secret("maigexr.signing.storeFile")
        if (storeFile != null) {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = secret("maigexr.signing.storePassword")
                keyAlias = secret("maigexr.signing.keyAlias")
                keyPassword = secret("maigexr.signing.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }

            // Production AdMob IDs come from local.properties; see releaseAdId().
            val appId = releaseAdId("maigexr.admob.appId")
            manifestPlaceholders["ADMOB_APP_ID"] = appId
            buildConfigField("String", "ADMOB_APP_ID", "\"$appId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${releaseAdId("maigexr.admob.bannerId")}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${releaseAdId("maigexr.admob.interstitialId")}\"")
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("boolean", "FORCE_PREMIUM", "false")
            buildConfigField("boolean", "AD_DEBUG_LOGS", "false")
            buildConfigField("int", "INTERSTITIAL_INTERVAL_SECONDS", "480")
            buildConfigField("int", "SCENES_BEFORE_INTERSTITIAL", "3")
            buildConfigField("String", "UMP_DEBUG_GEOGRAPHY", "\"\"")
            buildConfigField("String", "UMP_TEST_DEVICE_ID", "\"\"")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Google public test IDs — safe to commit
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("boolean", "ADS_ENABLED", "false")
            buildConfigField("boolean", "FORCE_PREMIUM", "false")
            buildConfigField("boolean", "AD_DEBUG_LOGS", "true")
            buildConfigField("int", "INTERSTITIAL_INTERVAL_SECONDS", "30")
            buildConfigField("int", "SCENES_BEFORE_INTERSTITIAL", "1")
            // Shows the EEA consent form without travelling; see AdConsentStore.
            buildConfigField("String", "UMP_DEBUG_GEOGRAPHY", "\"${secret("maigexr.ump.debugGeography") ?: ""}\"")
            buildConfigField("String", "UMP_TEST_DEVICE_ID", "\"${secret("maigexr.ump.testDeviceId") ?: ""}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.security.crypto)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Activity & Lifecycle
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Moshi
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // Markdown
    implementation(libs.commonmark)
    implementation(libs.compose.richtext.commonmark)
    implementation(libs.compose.richtext.ui)

    // AdMob + GDPR UMP
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.play.billing)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
