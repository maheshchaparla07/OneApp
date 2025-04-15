plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id ("kotlin-kapt")
}

android {
    namespace = "com.example.multiscreenapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.multiscreenapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildFeatures {
            viewBinding = true
            viewBinding = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Glide for image loading
    implementation ("com.github.bumptech.glide:glide:4.14.2")
    implementation(libs.firebase.appdistribution.gradle)
    kapt ("com.github.bumptech.glide:compiler:4.14.2")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Material Design
    implementation ("com.google.android.material:material:1.9.0")

    //  QR code scanning
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

    // For loading images from URLs
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")

    // For JSON parsing if needed
    implementation ("com.google.code.gson:gson:2.8.8")

    implementation ("com.google.android.gms:play-services-auth:21.3.0")
    implementation ("com.google.firebase:firebase-auth-ktx:22.3.0")
    implementation (platform("com.google.firebase:firebase-bom:32.2.0"))
    implementation ("com.google.android.material:material:1.9.0")
    implementation(libs.material)
    implementation(libs.material.v161)
    implementation (libs.androidx.core.ktx.v190)
    implementation (libs.androidx.appcompat.v161)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.mediation.test.suite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(kotlin("script-runtime"))
}




