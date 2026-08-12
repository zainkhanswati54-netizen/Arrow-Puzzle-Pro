plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.arrowpuzzle.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.arrowpuzzle.game"
        minSdk = 26  // java.time without desugaring; adaptive icons everywhere
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.0"
        vectorDrawables.useSupportLibrary = true
    }

    // CI supplies the keystore through environment variables. When they are absent
    // (local builds, forks, PRs from contributors) the release build falls back to
    // the debug key so the APK is still installable for testing.
    val ciKeystore: String? = System.getenv("KEYSTORE_PATH")

    signingConfigs {
        create("release") {
            if (ciKeystore != null) {
                storeFile = file(ciKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            signingConfig = if (ciKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            // Surfaces unstable classes that would cause needless recomposition.
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                "${project.layout.buildDirectory.get()}/compose_reports",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                "${project.layout.buildDirectory.get()}/compose_metrics"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.animation.graphics)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Monetization — AdMob (banner / interstitial / rewarded / rewarded interstitial)
    // plus Google's User Messaging Platform for the EU/UK consent (GDPR) flow.
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Schedules the twice-daily "come back and play" reminder notifications
    // reliably, including across reboots.
    implementation(libs.androidx.work.runtime.ktx)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
