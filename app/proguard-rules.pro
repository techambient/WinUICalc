# Aggressive R8 / ProGuard rules for ultra-tiny APK size and tracker stripping

-repackageclasses ''
-allowaccessmodification
-optimizationpasses 5

-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable,!RuntimeVisibleAnnotations,!RuntimeInvisibleAnnotations,!KotlinMetadata

# Completely strip and drop OpenCensus, telemetry, and tracking classes
-assumenosideeffects class io.opencensus.** { *; }
-dontwarn io.opencensus.**

-keep public class com.example.MainActivity {
    public *;
}

-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}
