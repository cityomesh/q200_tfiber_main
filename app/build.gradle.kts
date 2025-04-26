plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    kotlin("kapt") // ✅ Enables `kapt(...)` usage in dependencies
}

android {
    namespace = "tv.tfiber.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "tv.tfiber.launcher"
        minSdk = 21
        targetSdk = 35
        versionCode = 38
        versionName = "1.38"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Optionally, you can add specific configurations for the debug build type
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("\\Users\\omeshchintha95gmailcom\\Tfiber_Launcher-Q200Pro\\keystore\\tfiberv1.0.jks") // Replace with your keystore path
            storePassword = "Ulka123@pwd" // Replace with your keystore password
            keyAlias = "key0" // Replace with your key alias
            keyPassword = "Ulka123@pwd" // Replace with your key password
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}


dependencies {
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
