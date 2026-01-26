# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Socket.IO classes
-keep class io.socket.** { *; }
-keep class org.java_websocket.** { *; }

# Keep kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data models
-keep class com.homesoil.app.data.models.** { *; }
-keepclassmembers class com.homesoil.app.data.models.** { *; }

# Keep Compose
-dontwarn androidx.compose.**

# Keep Vico charts
-keep class com.patrykandpatrick.vico.** { *; }
