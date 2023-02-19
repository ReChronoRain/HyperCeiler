package com.sevtinge.cemiuiler.utils;

import android.content.Context;

public class Settings {

    public static void putInt(Context context, String name, int value) {
        android.provider.Settings.Secure.putInt(context.getContentResolver(), name, value);
    }

    public static void putString(Context context, String name, String value) {
        android.provider.Settings.Secure.putString(context.getContentResolver(), name, value);
    }

    public static void putBoolean(Context context, String name, boolean value) {
        putInt(context, name, value ? 1 : 0);
    }


    public static int getInt(Context context, String name, int def) {
        return android.provider.Settings.Secure.getInt(context.getContentResolver(), name, def);
    }

    public static String getString(Context context, String name) {
        return android.provider.Settings.Secure.getString(context.getContentResolver(), name);
    }

    public static boolean getBoolean(Context context, String name) {
        return getInt(context, name, 0) == 0 ? false : true;
    }

}
