# Keep Kotlinx serializers
-keepclassmembers class **$Companion { *; }
-keepclasseswithmembers class **$$serializer { *; }
-keep,includedescriptorclasses class ai.aligned.net.dto.** { *; }
