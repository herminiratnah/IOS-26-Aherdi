# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Model classes used with Room and Moshi serialization
-keep class com.example.model.** { *; }
-keep class com.example.service.** { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
