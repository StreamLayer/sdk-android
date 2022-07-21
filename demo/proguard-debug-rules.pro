# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/tomislav/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class label to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
# Platform used when running on RoboVM on iOS. Will not be used at runtime.
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

#fabric
-keepattributes *Annotation*

# will keep line numbers and file label obfuscation
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-keep public class * extends java.lang.Exception
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

#Picasso
-dontwarn com.squareup.okhttp3.**

#okio
-dontwarn okio.**

#Parceler
# Parceler library
-keep interface org.parceler.Parcel
-keep @org.parceler.Parcel class * { *; }
-keep class **$$Parcelable { *; }


# DBFlow
-keep class * extends com.raizlabs.android.dbflow.config.DatabaseHolder { *; }


-keep class org.ocpsoft.prettytime.i18n.**

#Google People API annotation
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

