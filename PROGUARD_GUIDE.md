# ProGuard & R8 Optimization Documentation

> **Oistars Billings - Security & Code Shrinking Guide**  
> Compliant with OpenSSF Secure Supply Chain & Google Play Store Policies

---

## 📌 Executive Summary

Oistars Billings enforces **R8 full-mode shrinking, optimization, and obfuscation** on all production release builds. This ensures:
1. **Attack Surface Reduction**: Stripping unused methods, metadata, and debugging interfaces prevents reverse engineering.
2. **APK & Binary Size Optimization**: Unused code paths and unreferenced resources are purged by R8.
3. **Data Protection**: Internal database structures, cryptographic logic, and proprietary billing models cannot be inspected via standard decompiler tools.
4. **Stack Trace Preservation**: Debugging line number tables (`LineNumberTable`) and original source filenames are preserved via mapping files for production crash reporting.

---

## ⚙️ Gradle Build Configuration

In `app/build.gradle.kts`:

```kotlin
android {
    buildTypes {
        release {
            isCrunchPngs = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }
}
```

---

## 🛡️ Rule Breakdown in `proguard-rules.pro`

### 1. Stack Trace & Crash Diagnostics
```pro
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
```
- **Rationale**: When release crashes occur in production, R8 mapping files convert the obfuscated stack traces back to precise Kotlin line numbers.

### 2. Room Database & Local Ledger Entities
```pro
-keep class com.example.data.local.** { *; }
-keepclassmembers class com.example.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
```
- **Rationale**: Room generates SQLite query bridges and tables via reflection and annotation processing. Keeping entity properties prevents field name mismatch in SQLite tables.

### 3. Moshi & JSON Serialization
```pro
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
```
- **Rationale**: JSON adapters require field annotations to serialize/deserialize API payloads without corruption.

### 4. Firebase Firestore & Authentication
```pro
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.auth.** { *; }
```
- **Rationale**: Cloud Firestore document mapping utilizes JavaBean reflection to map remote documents to Kotlin data classes.

### 5. Jetpack Compose & State Management
```pro
-keep class androidx.compose.runtime.** { *; }
-keep class com.example.ui.viewmodel.** { *; }
```
- **Rationale**: StateFlow emissions and Compose `@Composable` functions rely on stability markers.

---

## 🔍 Verification & Re-mapping

When building release bundles via:
```bash
gradle assembleRelease
```
The output directory contains:
- `app/build/outputs/mapping/release/mapping.txt`: The de-obfuscation mapping table.
- `app/build/outputs/apk/release/app-release.apk`: The minimized, production-ready APK.

To de-obfuscate a production crash trace:
```bash
retrace.sh mapping.txt stacktrace.txt
```
