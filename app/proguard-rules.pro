-keep class com.sevtinge.hyperceiler.ui.**{ *; }
-keep class com.sevtinge.hyperceiler.main.**{ *; }
-keep class com.sevtinge.hyperceiler.utils.XposedActivateHelper { *; }

-keep class com.fan.**{ *; }

-keepattributes SourceFile,LineNumberTable

-allowaccessmodification
#-obfuscationdictionary          dict.txt
#-classobfuscationdictionary     dict.txt
#-packageobfuscationdictionary   dict.txt
