# Keep annotations
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions

# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# App data models — Gson needs these intact
-keep class com.alhabibifeast.app.data.model.** { *; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin serialization
-keepclassmembers class kotlin.Metadata { *; }

# Navigation
-keep class androidx.navigation.** { *; }

# Material / AppCompat
-keep class com.google.android.material.** { *; }

# Keep R class
-keep class **.R$* { *; }

# WebSocket interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Prevent crash from missing classes
-dontwarn java.lang.invoke.StringConcatFactory
