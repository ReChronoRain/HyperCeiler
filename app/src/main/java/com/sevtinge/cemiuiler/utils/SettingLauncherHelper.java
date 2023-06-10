package com.sevtinge.cemiuiler.utils;

import android.content.Context;
import android.os.Bundle;

public class SettingLauncherHelper {

    public static void onStartSettingsForArguments(Context context, Class<?> cls, String fragment, String title) {
        onStartSettingsForArguments(context, cls, fragment, null, title);
    }

    public static void onStartSettingsForArguments(Context context, Class<?> cls, String fragment, int titleResId) {
        onStartSettingsForArguments(context, cls, fragment, null, titleResId);
    }

    public static void onStartSettingsForArguments(Context context, Class<?> cls, String fragment, Bundle args, String title) {
        onStartSettingsForArguments(context, cls, fragment, args, 0, title);
    }

    public static void onStartSettingsForArguments(Context context, Class<?> cls, String fragment, Bundle args, int titleResId) {
        onStartSettingsForArguments(context, cls, fragment, args, titleResId, null);
    }

    public static void onStartSettingsForArguments(Context context, Class<?> cls, String fragment, Bundle args, int titleResId, String title) {
        if (args == null) args = new Bundle();
        onStartSettings(context, cls, fragment, null, args, titleResId, title);
    }

    public static void onStartSettingsForExtras(Context context, Class<?> cls, String fragment, Bundle extras, int titleResId, String title) {
        if (extras == null) extras = new Bundle();
        onStartSettings(context, cls, fragment, extras, null, titleResId, title);
    }

    public static void onStartSettings(Context context, Class<?> cls, Class<?> fname, String title) {
        onStartSettings(context, cls, fname, null, null, 0, title);
    }

    public static void onStartSettings(Context context, Class<?> cls, Class<?> fname, Bundle extras, Bundle args, String title) {
        onStartSettings(context, cls, fname, extras, args, 0, title);
    }

    public static void onStartSettings(Context context, Class<?> cls, Class<?> fname, Bundle extras, Bundle args, int titleResId, String title) {
        if (args == null) args = new Bundle();
        onStartSettings(context, cls, fname.getName(), extras, args, titleResId, title);
    }

    public static void onStartSettings(Context context, Class<?> cls, String fragment, Bundle extras, Bundle args, int titleResId, String title) {
        new SettingLauncher(context)
            .setClass(cls)
            .setDestination(fragment)
            .setTitleText(title)
            .setTitleRes(titleResId)
            .setExtras(extras)
            .setArguments(args)
            .launch();
    }
}
