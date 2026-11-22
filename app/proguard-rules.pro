# Keep Firestore @Keep models and no-arg constructors for R8/ProGuard release builds
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-keepclassmembers class com.example.model.** {
    *;
}
-keep @androidx.annotation.Keep class * {
    *;
}
