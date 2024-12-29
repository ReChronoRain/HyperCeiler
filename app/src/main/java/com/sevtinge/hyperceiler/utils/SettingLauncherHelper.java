/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.os.Bundle;

import fan.appcompat.app.Fragment;
import fan.navigator.Navigator;
import fan.navigator.navigatorinfo.UpdateDetailFragmentNavInfo;

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
