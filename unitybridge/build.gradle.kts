plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.nshutiplanner.unity"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Firebase — needed by UnityBridge.fetchLocationByEmail
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    
    // Explicitly add common and play services for 'await()' support
    implementation("com.google.firebase:firebase-common-ktx:21.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
