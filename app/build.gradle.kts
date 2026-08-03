import java.util.Properties

plugins {
    id("com.android.application")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasUploadKeystore: Boolean = keystorePropertiesFile.exists()

android {
    namespace = "com.eretik.heretic"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.eretik.heretic"
        minSdk = 21
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        if (hasUploadKeystore) {
            create("upload") {
                val p = Properties().apply { load(keystorePropertiesFile.reader()) }
                val storeRelative = p.getProperty("storeFile")
                    ?: error("keystore.properties: missing storeFile")
                storeFile = rootProject.file(storeRelative)
                storePassword = p.getProperty("storePassword")
                    ?: error("keystore.properties: missing storePassword")
                keyAlias = p.getProperty("keyAlias")
                    ?: error("keystore.properties: missing keyAlias")
                keyPassword = p.getProperty("keyPassword")
                    ?: error("keystore.properties: missing keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Upload keystore (keystore.properties) → release signing for store publication.
            // Without it, release uses the debug keystore so local/CI APKs still install.
            signingConfig = if (hasUploadKeystore) {
                signingConfigs.getByName("upload")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    externalNativeBuild {
        ndkBuild {
            path = File("src/main/jni/Android.mk")
        }
    }
}
