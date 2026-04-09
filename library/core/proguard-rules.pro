# --- AndroidX ---
-keep class androidx.preference.** { *; }

# --- HyperCeiler ---
-keep class com.sevtinge.hyperceiler.hooker.** { *; }
-keep class com.sevtinge.hyperceiler.sub.** { *; }
-keep class com.sevtinge.hyperceiler.holiday.** { *; }
-keep class * extends com.sevtinge.hyperceiler.common.base.BasePreferenceFragment { *; }

# --- UI / fan ---
-keep class fan.** { *; }

# --- Warnings ---
-dontwarn miui.**
-dontwarn javax.annotation.**
-dontwarn com.android.internal.view.menu.MenuBuilder
