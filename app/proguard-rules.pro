# --- Kotlin ---
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void check*(...);
	public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

# --- App ---
-keep class com.sevtinge.hyperceiler.ui.**{ *; }
-keep class com.sevtinge.hyperceiler.main.**{ *; }

-keep class com.fan.**{ *; }

# --- Obfuscation ---
-keepattributes SourceFile,LineNumberTable
-repackageclasses
-allowaccessmodification
#-obfuscationdictionary          dict.txt
#-classobfuscationdictionary     dict.txt
#-packageobfuscationdictionary   dict.txt
