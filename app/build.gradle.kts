plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.xraiassistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xrai.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // AdMob manifest placeholder (overridden per buildType below)
        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Production AdMob IDs — replace before Play Store submission
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX/AAAAAAAAAA\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX/BBBBBBBBBB\"")
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("boolean", "FORCE_PREMIUM", "false")
            buildConfigField("boolean", "AD_DEBUG_LOGS", "false")
            buildConfigField("int", "INTERSTITIAL_INTERVAL_SECONDS", "480")
            buildConfigField("int", "SCENES_BEFORE_INTERSTITIAL", "3")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Google public test IDs — safe to commit
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("boolean", "ADS_ENABLED", "false")
            buildConfigField("boolean", "FORCE_PREMIUM", "false")
            buildConfigField("boolean", "AD_DEBUG_LOGS", "true")
            buildConfigField("int", "INTERSTITIAL_INTERVAL_SECONDS", "30")
            buildConfigField("int", "SCENES_BEFORE_INTERSTITIAL", "1")
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
