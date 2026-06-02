# ProGuard rules
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.aifactory.appmessagecapture.data.** { *; }
