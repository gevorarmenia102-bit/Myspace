# MySpace Personal Hub — Release Checklist & Deployment Guide

This document provides step-by-step instructions for versioning, signing, obfuscating, and generating production-ready release builds of **MySpace Personal Hub**.

---

## 1. Versioning Strategy

App versioning is declared in [app/build.gradle.kts](file:///Users/xagor/Desktop/personal%20apps/app/build.gradle.kts) inside the `defaultConfig` block:

```kotlin
defaultConfig {
    applicationId = "com.example.myspacepersonalhub"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"
}
```

### Incrementing Rules
- **`versionCode`**: Must be a strictly increasing integer for every build submitted to testing tracks or the Google Play Console (e.g., `1` -> `2` -> `3`).
- **`versionName`**: Follows Semantic Versioning (`MAJOR.MINOR.PATCH`):
  - `PATCH`: Bug fixes, security patches, cosmetic tweaks (e.g., `1.0.1`).
  - `MINOR`: New features or modules added in a backwards-compatible manner (e.g., `1.1.0`).
  - `MAJOR`: Significant architectural shifts or breaking database migration revisions (e.g., `2.0.0`).

---

## 2. Release Signing Configuration

> [!CAUTION]
> **Never commit your release keystore file, store password, or key password to version control.**

### Step 1: Generate a Production Keystore
Generate a private keystore using Java's `keytool`:
```bash
keytool -genkey -v -keystore myspace-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias myspace-key
```

### Step 2: Store Credentials Securely
Create a local properties file `keystore.properties` in the project root (ensure it is in `.gitignore`):
```properties
storeFile=/path/to/myspace-release-key.jks
storePassword=YOUR_SECURE_STORE_PASSWORD
keyAlias=myspace-key
keyPassword=YOUR_SECURE_KEY_PASSWORD
```

### Step 3: Configure `app/build.gradle.kts`
Wire the signing config into Gradle:
```kotlin
import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## 3. R8 / ProGuard Configuration

The production build enables minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`). Ensure rules in `app/proguard-rules.pro` preserve Room, Kotlinx Serialization, and Coroutines:

```proguard
# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Biometrics
-keep class androidx.biometric.** { *; }
```

---

## 4. Building the Production Artifacts

### Option A: Via Command Line (Gradle)
Export environment variables for Java 21 and Android SDK:
```bash
export JAVA_HOME=/Users/xagor/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home
export ANDROID_HOME=/Users/xagor/Library/Android/sdk

# Build Release APK
./gradlew assembleRelease

# Build Android App Bundle (for Play Store)
./gradlew bundleRelease
```

Generated outputs:
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release Bundle: `app/build/outputs/bundle/release/app-release.aab`

### Option B: Via Android Studio IDE
1. Open Android Studio.
2. Select **File > Open** and choose `/Users/xagor/Desktop/personal apps`.
3. Wait for Gradle Sync to complete.
4. From the top menu, select **Build > Generate Signed Bundle / APK...**.
5. Choose **Android App Bundle** (or **APK** for direct sideloading).
6. Point to your `myspace-release-key.jks`, enter credentials, and select **release** build variant.
7. Click **Create**. The signed artifact will be generated in the destination folder.

---

## 5. Pre-Release Verification Checklist

Before releasing or updating a build:
- [x] All unit tests pass cleanly: `./gradlew testDebugUnitTest`
- [x] Room schema versioning verified: Entities match DB version `1`
- [x] Encrypted backup export and import verified with correct and invalid passwords
- [x] Private Vault biometric authentication unlocks and AES-256-GCM decodes correctly
- [x] Task reminder alarms fire at scheduled times without duplicating
- [x] Offline local mode functions without network connection
- [x] Zero hard-coded credentials, API keys, or private test records in repository
- [x] `docs/SECURITY.md`, `docs/ARCHITECTURE.md`, and `docs/FIREBASE_SETUP.md` are up to date
