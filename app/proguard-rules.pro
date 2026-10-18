# MomoStack Proguard & R8 Production Hardening Rules

# 1. Attributes & Annotations
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# 2. Room SQLite Database
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.momostack.app.data.entity.** { *; }
-keep class com.momostack.app.data.model.** { *; }
-keep class com.momostack.app.data.dao.** { *; }
-keep class com.momostack.app.feature.screenshotvault.data.** { *; }

# 3. Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <init>(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# 4. ML Kit On-Device Text Recognition
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-dontwarn com.google.mlkit.**

# 5. OkHttp & Jsoup
-dontwarn org.jsoup.**
-keep class org.jsoup.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# 6. Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# 7. Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# 8. WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class com.momostack.app.worker.** { *; }

# 9. Security & Anti-Reverse Engineering Preservation
-keep class com.momostack.app.util.SecurityGuard { *; }
