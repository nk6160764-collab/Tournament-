# Keep Firestore @Keep models and no-arg constructors for R8/ProGuard release builds
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-keepclassmembers class com.example.model.** {
    *;
}
-keep @androidx.annotation.Keep class * {
    *;
}

# Firebase & Google Credential Manager release safety
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
