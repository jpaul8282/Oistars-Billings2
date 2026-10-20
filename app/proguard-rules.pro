# ==============================================================================
# Oistars Billings - Robust Production ProGuard / R8 Configuration
# Keep rules for: Retrofit, Moshi, Room, Firebase, Coroutines, and Compose
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Stack Traces & Diagnostic Metadata
# ------------------------------------------------------------------------------
# Preserve line numbers and file names for readable crash stack traces in production
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve standard annotations and generic signatures for reflection and serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# 2. Retrofit 2 Keep Rules
# ------------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ------------------------------------------------------------------------------
# 3. Moshi Keep Rules
# ------------------------------------------------------------------------------
-dontwarn com.squareup.moshi.**
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.JsonQualifier <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep class * extends com.squareup.moshi.JsonAdapter$Factory { *; }

# ------------------------------------------------------------------------------
# 4. Room Database Keep Rules
# ------------------------------------------------------------------------------
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.Dao { *; }
-keep class * implements androidx.room.TypeConverter { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-keep class **_Impl { *; }

# Data Layer Entities, DAOs, and Models
-keep class com.example.data.local.** { *; }
-keepclassmembers class com.example.data.local.** { *; }
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.repository.** { *; }
-keepclassmembers class com.example.data.repository.** { *; }
-keep class com.oistars.billings.** { *; }
-keepclassmembers class com.oistars.billings.** { *; }

# ------------------------------------------------------------------------------
# 5. Firebase Keep Rules (Firestore, Auth, AppCheck, AI)
# ------------------------------------------------------------------------------
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.appcheck.** { *; }

# Preserve Firestore document field mappings
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# ------------------------------------------------------------------------------
# 6. OkHttp 3 & Okio Keep Rules
# ------------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ------------------------------------------------------------------------------
# 7. Kotlin Coroutines & Flow
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 8. Jetpack Credential Manager & Google Sign-In
# ------------------------------------------------------------------------------
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ------------------------------------------------------------------------------
# 9. Jetpack Compose UI & ViewModels
# ------------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class com.example.ui.viewmodel.** { *; }
-keepclassmembers class com.example.ui.viewmodel.** { *; }
