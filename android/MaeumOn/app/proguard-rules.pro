# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


-keepnames @com.google.android.gms.common.annotation.KeepName class
    com.google.android.gms.**,
    com.google.ads.**

-keepclassmembernames class
    com.google.android.gms.**,
    com.google.ads.** {
    @com.google.android.gms.common.annotation.KeepName *;
}

# Authentication
-keepattributes *Annotation*

-keepattributes Signature
-keepclassmembers class * {
    public static <fields>;
    public *;
}

# Proguard config for project using GMS

-keepnames @com.google.android.gms.common.annotation.KeepName class
    com.google.android.gms.**,
    com.google.ads.**

-keepclassmembernames class
    com.google.android.gms.**,
    com.google.ads.** {
    @com.google.android.gms.common.annotation.KeepName *;
}

# Firebase
-keepattributes AutoValue

-keep class com.google.firebase.installations.** {
  *;
}

-keep interface com.google.firebase.installations.** {
  *;
}

# Firebase KTX shim referenced by plugins; suppress missing class warning from R8
-dontwarn com.google.firebase.ktx.Firebase

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# AndroidX Test - suppress missing class warnings
-dontwarn androidx.concurrent.futures.SuspendToFutureAdapter

# Release 빌드에서 Log 제거
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
