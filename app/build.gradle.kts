plugins {
    id("com.android.application")
}

android {
    namespace = "dev.burixon.mockgps"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.burixon.mockgps"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-beta"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}