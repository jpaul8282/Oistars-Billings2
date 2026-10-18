# ==============================================================================
# Oistars Billings - Production ProGuard / R8 Optimization & Obfuscation Rules
# OpenSSF Security & Play Store Compliance
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Stack Traces & Debugging Metadata
# ------------------------------------------------------------------------------
# Preserve line numbers and file names for readable crash stack traces in production
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve standard annotations and generic signatures for reflection/serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# 2. Data Models, Room Database & Entities
# ------------------------------------------------------------------------------
-keep class com.example.data.local.** { *; }
-keepclassmembers class com.example.data.local.** { *; }

-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

-keep class com.example.data.repository.** { *; }
-keepclassmembers class com.example.data.repository.** { *; }

-keep class com.oistars.billings.** { *; }
-keepclassmembers class com.oistars.billings.** { *; }

# Keep Room generated classes and DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 3. Moshi & JSON Serialization
# ------------------------------------------------------------------------------
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.JsonQualifier <fields>;
}
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-dontwarn com.squareup.moshi.**

# ------------------------------------------------------------------------------
# 4. Firebase Firestore & Firebase Auth
# ------------------------------------------------------------------------------
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-dontwarn com.google.firebase.**
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.auth.** { *; }

# ------------------------------------------------------------------------------
# 5. Jetpack Compose & UI Architecture
# ------------------------------------------------------------------------------
# Prevent R8 from stripping Compose runtime and state classes
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class com.example.ui.viewmodel.** { *; }
-keepclassmembers class com.example.ui.viewmodel.** { *; }

# ------------------------------------------------------------------------------
# 6. Kotlin Coroutines & Flow
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 7. Credential Manager & Google Sign-In
# ------------------------------------------------------------------------------
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ------------------------------------------------------------------------------
# 8. OkHttp & Retrofit Networking
# ------------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
