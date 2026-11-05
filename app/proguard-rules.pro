# Aggressive R8 / ProGuard rules for ultra-tiny APK size under 100KB

-repackageclasses ''
-allowaccessmodification
-optimizationpasses 5

-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable,!RuntimeVisibleAnnotations,!RuntimeInvisibleAnnotations,!KotlinMetadata

-keep public class com.example.MainActivity {
    public *;
}

-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}
