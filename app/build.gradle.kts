plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.machinelearning"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.machinelearning"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        viewBinding = true
    }
}

dependencies {

    implementation("com.google.mlkit:translate:17.0.3")

    // To recognize Latin script
    implementation ("com.google.mlkit:text-recognition:16.0.1")

    // To recognize Chinese script
    implementation ("com.google.mlkit:text-recognition-chinese:16.0.1")

    // To recognize Japanese script
    implementation ("com.google.mlkit:text-recognition-japanese:16.0.1")

    // To recognize Korean script
    implementation ("com.google.mlkit:text-recognition-korean:16.0.1")

    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    val exoPlayerVersion = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:${exoPlayerVersion}")
    implementation("androidx.media3:media3-exoplayer-dash:${exoPlayerVersion}")
    implementation("androidx.media3:media3-ui:${exoPlayerVersion}")

    // CameraX core library using the camera2 implementation
    val cameraxVersion = "1.4.1"
    // The following line is optional, as the core library is included indirectly by camera-camera2
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    // If you want to additionally use the CameraX Lifecycle library
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    // If you want to additionally use the CameraX View class
    implementation("androidx.camera:camera-view:${cameraxVersion}")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
