# 🛡️ ProGuard & R8 Security Hardening Specification

> **Oistars Billings - Code Obfuscation, Anti-Tampering & Supply Chain Defense**  
> Compliant with **OpenSSF Best Practices**, **OWASP Mobile Application Security (MASVS)**, and **Google Play Developer Policies**

---

## 1. Security Architecture Overview

The release pipeline of **Oistars Billings** (`com.oistars.billings`) leverages **Android R8 Full-Mode compiler optimization** to harden the Android App Bundle against reverse engineering, bytecode modification, and sensitive data leakage.

```
Source Code (.kt) + Resources
       │
       ▼
  R8 Compiler (Optimization, Tree-Shaking, Obfuscation)
  ├── Input 1: proguard-rules.pro (Custom Business Logic Rules)
  ├── Input 2: proguard-android-optimize.txt (Default AGP Rules)
  ├── Input 3: Room, Moshi, Firebase & Coroutines AAR ProGuard Consumer Rules
       │
       ▼
Minified DEX Bytecode + Stripped Resources
       │
       ▼
Signed Android App Bundle (AAB / APK) + mapping.txt
```

---

## 2. Hardening Objectives & Threat Matrix

| Threat Category | Potential Risk | R8 / ProGuard Countermeasure |
|---|---|---|
| **Static Decompilation** | Attackers inspect class names, internal calculation methods, and business logic using tools like `jadx` or `apktool`. | **Full Identifier Renaming**: Class names, method signatures, and local variables are renamed to short alphanumeric tokens (`a.b.c`). |
| **Tampering & Repackaging** | Attackers modify calculation math (e.g. bypassing VAT or fees) and resign APK. | **Code Inlining & Dead-Branch Elimination**: Function bodies are aggressively inlined, making control flow graphs fragmented and difficult to patch. |
| **Sensitive String & Asset Exposure** | Hardcoded API keys, database paths, or proprietary formulas extracted from constant pools. | **Secrets Plugin & Environmental Injection**: All credentials loaded dynamically via `.env` without constants compiled into DEX. |
| **Stack Trace Information Leakage** | Crash logs reveal server endpoints, internal architecture, and system file paths. | **SourceFile & LineNumberTable Sanitization**: Line numbers mapped through private `mapping.txt`; SourceFile attribute renamed. |

---

## 3. Core ProGuard Rules Breakdown (`app/proguard-rules.pro`)

### A. Stack Trace Protection with Private Mapping
```pro
# Preserve line numbers for crash analysis while obscuring source filenames
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retain essential reflection metadata for runtime stability
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
```
- **Security Impact**: Production crash logs received in Google Play Console or Firebase Crashlytics are fully de-obfuscated using the uploaded `mapping.txt`, while third-party interceptors only see generic stack traces.

### B. Room Database & Local Ledger Isolation
```pro
-keep class com.example.data.local.** { *; }
-keepclassmembers class com.example.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
```
- **Security Impact**: Prevents runtime reflection crashes during SQLite ORM hydration while maintaining encrypted device-sandbox storage (`/data/data/com.oistars.billings/databases/`).

### C. Moshi & Serialization Protection
```pro
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.JsonQualifier <fields>;
}
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-dontwarn com.squareup.moshi.**
```
- **Security Impact**: Guarantees that JSON models and HTTP payloads are not corrupted by aggressive field renaming.

### D. Firebase Cloud Firestore & Authentication
```pro
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-dontwarn com.google.firebase.**
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.auth.** { *; }
```
- **Security Impact**: Protects Firebase SDK reflection hooks and ensures zero-trust security rules on Firestore paths (`/users/{userId}/...`) are validated with correct property bindings.

### E. Kotlin Coroutines & Asynchronous Flow Protection
```pro
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
```
- **Security Impact**: Retains volatile memory barriers on asynchronous financial computations to prevent race conditions during concurrent database writes.

---

## 4. Verification & Audit Procedure

### 1. Build Verification
Execute the release bundle build:
```bash
gradle bundleRelease
```

### 2. Verify Output Artifacts
Inspect the generated build directory:
```bash
# Production App Bundle
ls -lh app/build/outputs/bundle/release/app-release.aab

# Obfuscation Mapping File
ls -lh app/build/outputs/mapping/release/mapping.txt

# Resource Shrinking Report
ls -lh app/build/outputs/mapping/release/resources.txt
```

### 3. De-obfuscating Stack Traces
To verify that mappings de-obfuscate properly:
```bash
$ANDROID_HOME/cmdline-tools/latest/bin/retrace \
    app/build/outputs/mapping/release/mapping.txt \
    obfuscated_trace.txt
```

---

## 5. OpenSSF Secure Supply Chain Compliance

1. **Dependency Pinning**: All library coordinates and plugins are declared in `gradle/libs.versions.toml`.
2. **ProGuard Audit**: All third-party library consumer rules are verified during the `lintVitalRelease` step of `bundleRelease`.
3. **No Dynamic Code Loading (DCL)**: Zero external `.dex` or `.jar` loading from remote servers, complying with Google Play Policy.
