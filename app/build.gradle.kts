plugins {
    id("com.android.application")
}

android {
    namespace = "com.pumpkin.panel"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pumpkin.panel"
        minSdk = 24
        targetSdk = 28
        versionCode = 5
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// Important note:
// targetSdk 28 is intentional. Android 10+ forbids apps with
// targetSdk >= 29 from executing files from their own data directory (SELinux W^X).
// Pumpkin runs from /data/data/com.pumpkin.panel/files/pumpkin,
// exactly like Termux runs its binary.
