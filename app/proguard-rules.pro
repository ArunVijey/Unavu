# kotlinx.serialization: keep generated serializers for the app's data classes
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.unavu.** {
    *** Companion;
}
-keepclasseswithmembers class app.unavu.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.unavu.**$$serializer { *; }
# widgets / tiles / receivers are referenced from the manifest (kept by AAPT) - nothing else needed
