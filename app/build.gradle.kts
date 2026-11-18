plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ff6locke.gplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ff6locke.gplayer"
        minSdk = 23
        targetSdk = 28
        versionCode = 2
        versionName = "0.2-mpv-arm32"

        ndk {
            abiFilters += "armeabi-v7a"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}
