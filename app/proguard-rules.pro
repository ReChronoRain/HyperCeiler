# --- Kotlin / Java ---
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

# --- App ---
-keep class com.sevtinge.hyperceiler.ui.** { *; }
-keep class com.sevtinge.hyperceiler.main.** { *; }
-keep class com.fan.** { *; }

# --- Obfuscation ---
-keepattributes *Annotation*,RuntimeVisibleAnnotations,LineNumberTable,SourceFile,Signature
-renamesourcefileattribute SourceFile
-repackageclasses
-allowaccessmodification
#-obfuscationdictionary          dict.txt
#-classobfuscationdictionary     dict.txt
#-packageobfuscationdictionary   dict.txt

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class com.sevtinge.hyperceiler.search.data.ModEntity {
    <fields>;
}
-keep interface com.sevtinge.hyperceiler.search.data.ModDao { *; }
-keep class * extends androidx.room.RoomOpenHelper { *; }
-keep interface androidx.sqlite.db.SupportSQLiteOpenHelper { *; }
-keep interface androidx.sqlite.db.SupportSQLiteDatabase { *; }
-keep class androidx.room.util.TableInfo { *; }
-keep class androidx.room.util.TableInfo$Column { *; }
-keep class androidx.room.util.TableInfo$Index { *; }
-keep class androidx.room.util.TableInfo$ForeignKey { *; }
-dontwarn androidx.room.**

# --- fan.provider ---
-keep class fan.provider.Settings** { *; }
-keep class fan.provider.SettingsLiveData** { *; }
