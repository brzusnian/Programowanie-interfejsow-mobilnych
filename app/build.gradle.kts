
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.skaneralergenow"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.skaneralergenow"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}


dependencies {
    implementation(libs.androidxActivityKtx)
    implementation(libs.androidxAppcompat)
    implementation(libs.androidxConstraintlayout)
    implementation(libs.androidxCoreKtx)
    implementation(libs.material)

    // CameraX
    implementation(libs.cameraCore)
    implementation(libs.cameraCamera2)
    implementation(libs.cameraLifecycle)
    implementation(libs.cameraView)

    // Google ML Kit
    implementation(libs.mlKitTextRecognition)

    // OpenCV
    implementation(libs.opencv)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxEspressoCore)
    androidTestImplementation(libs.androidxJunit)
}