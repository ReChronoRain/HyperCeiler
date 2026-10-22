package com.sevtinge.hyperceiler.common.utils;
public final class PrefsBridge{public static boolean enabled=true;public static boolean getBoolean(String key){if(!key.equals("system_ui_lock_screen_unlock_tip"))throw new AssertionError(key);return enabled;}}
