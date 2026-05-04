# Add project specific ProGuard rules here.
-keep class com.wechat.editor.model.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
