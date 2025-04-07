
-keep class com.sevtinge.hyperceiler.ui.LauncherActivity

-keep class com.sevtinge.hyperceiler.utils.XposedActivateHelper { boolean isModuleActive; }
-keep class com.sevtinge.hyperceiler.utils.XposedActivateHelper { int XposedVersion; }

-keep class cn.lyric.getter.api.**{ *; }
-keep class org.luckypray.dexkit.**{ *; }

-keepattributes SourceFile,LineNumberTable

-allowaccessmodification
-obfuscationdictionary          dict.txt
-classobfuscationdictionary     dict.txt
-packageobfuscationdictionary   dict.txt

