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

// Catatan penting:
// targetSdk 28 dipilih dengan sengaja. Android 10+ melarang aplikasi dengan
// targetSdk >= 29 mengeksekusi file dari folder datanya sendiri (SELinux W^X).
// Pumpkin dijalankan dari /data/data/com.pumpkin.panel/files/pumpkin,
// persis seperti cara Termux menjalankan binary-nya.
