# ProGuard rules
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.aifactory.appmessagecapture.data.** { *; }

# Birthday entities live in birthday.data and are serialized via Gson reflection
# (BackupManager import/export); R8 fullMode would rename their field names and
# break round-tripping of exported JSON backups.
-keep class com.aifactory.appmessagecapture.birthday.data.** { *; }

# Gson uses Enum.name()/valueOf() for enums stored in entities (frequency, reminder type)
-keepclassmembers enum com.aifactory.appmessagecapture.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
