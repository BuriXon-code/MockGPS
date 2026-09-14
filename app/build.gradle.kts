plugins {
    id("com.android.application")
}

android {
    namespace = "dev.burixon.mockgps"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "dev.burixon.mockgps"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"
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

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    implementation("org.maplibre.gl:android-sdk:13.6.1")
}
