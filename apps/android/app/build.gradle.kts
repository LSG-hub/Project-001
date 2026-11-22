import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Signing for the "dev" build (sent to testers by Firebase App Distribution). The keystore
// and password live outside the repo: in local.properties on your machine, and in GitHub
// secrets (ANDROID_DEV_KEYSTORE / ANDROID_DEV_KEYSTORE_PASSWORD) in CI.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val devKeystore = System.getenv("ANDROID_DEV_KEYSTORE") ?: localProps.getProperty("devSigning.storeFile")
val devKeystorePassword =
    System.getenv("ANDROID_DEV_KEYSTORE_PASSWORD") ?: localProps.getProperty("devSigning.password")

// Firebase (sign-in) for the dev app, configured in code rather than a committed
// google-services.json. Values come from local.properties or CI env, never the repo.
fun setting(env: String, prop: String): String = System.getenv(env) ?: localProps.getProperty(prop) ?: ""
val firebaseDevApiKey = setting("FIREBASE_DEV_API_KEY", "firebase.dev.apiKey")
val firebaseDevAppId = setting("FIREBASE_DEV_APP_ID", "firebase.dev.appId")
// The dev backend (Cloud Run api-dev).
val apiDevUrl = "https://api-dev-701356728707.asia-south1.run.app"

android {
    // Package name is a working name. Pick the final one before the first Play upload:
    // it can never change after that.
    namespace = "app.project001"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "app.project001"
        minSdk = 29
        targetSdk = 36
        // CI sets BUILD_NUMBER, so every distributed build is newer than the last.
        versionCode = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 1
        versionName = "0.1.0"
        // Number our server calls from to verify a phone. Placeholder until the
        // virtual number is live.
        buildConfigField("String", "VERIFY_CALLER", "\"+919999900001\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiDevUrl\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"$firebaseDevApiKey\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"$firebaseDevAppId\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"project-001-474105\"")
    }

    signingConfigs {
        if (devKeystore != null && devKeystorePassword != null) {
            create("dev") {
                storeFile = rootProject.file(devKeystore)
                storePassword = devKeystorePassword
                keyAlias = "dev"
                keyPassword = devKeystorePassword
            }
        }
    }

    buildTypes {
        // "001 Dev": a debuggable build for testers, installed next to the store app.
        // Same test conveniences as debug (any 6-digit code signs you in).
        create("dev") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            signingConfig = signingConfigs.findByName("dev")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.libphonenumber)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
