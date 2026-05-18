# Kotlinx serialization
-keepclassmembers class **$Companion { *; }
-keepclasseswithmembers class **$$serializer { *; }
-keep,includedescriptorclasses class ai.aligned.net.dto.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Hilt
-keep class dagger.hilt.android.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep,allowobfuscation @interface dagger.hilt.android.HiltAndroidApp
-keep,allowobfuscation @interface dagger.hilt.android.AndroidEntryPoint

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose
-keep class androidx.compose.runtime.** { *; }
