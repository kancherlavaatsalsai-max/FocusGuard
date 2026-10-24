# Add project specific ProGuard rules here.
-keep class com.focusguard.app.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
