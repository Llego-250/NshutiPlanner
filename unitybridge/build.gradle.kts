plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)

    // Firebase — needed by UnityBridge.fetchLocationByEmail
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    // UnityPlayer.UnitySendMessage is provided at runtime via the Unity .aar.
    // It is NOT a compile-time dependency of this library module.
    // The Unity .aar must be present in the consuming app's libs/ folder.
    compileOnly(files("libs/unity-classes.jar")) // stub jar if available, otherwise remove
}
