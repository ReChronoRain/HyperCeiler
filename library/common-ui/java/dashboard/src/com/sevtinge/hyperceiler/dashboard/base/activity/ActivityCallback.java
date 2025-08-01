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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.dashboard.base.activity;

import static com.sevtinge.hyperceiler.common.utils.PersistConfig.isNeedGrayView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.prefs.XmlPreference;
import com.sevtinge.hyperceiler.common.utils.SettingLauncherHelper;
import com.sevtinge.hyperceiler.dashboard.SubSettings;
import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

public interface ActivityCallback {

    default void registerObserver(Context context) {
        PrefsUtils.registerOnSharedPreferenceChangeListener(context);
        AppsTool.fixPermissionsAsync(context);
        AppsTool.registerFileObserver(context);
    }

    default void applyGrayScaleFilter(Activity activity) {
        if (isNeedGrayView) {
            View decorView = activity.getWindow().getDecorView();
            Paint paint = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            paint.setColorFilter(new ColorMatrixColorFilter(cm));
            decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        }
    }

    default void replaceFragment(Fragment fragment, String tag) {
        //mFragmentManager.beginTransaction().replace(R.id.frame_content, fragment, tag).commit();
    }

    default Bundle getArguments(Intent intent) {
        Bundle args = intent.getBundleExtra(":settings:show_fragment_args");
        String showFragmentTitle = intent.getStringExtra(":settings:show_fragment_title");
        int showFragmentTitleResId = intent.getIntExtra(":settings:show_fragment_title_resid", 0);
        args.putString(":fragment:show_title", showFragmentTitle);
        args.putInt(":fragment:show_title_resid", showFragmentTitleResId);
        return args;
    }

    default String getInitialFragmentName(Intent intent) {
        return intent.getStringExtra(":settings:show_fragment");
    }

    default Fragment getTargetFragment(Context context, String initialFragmentName, Bundle savedInstanceState) {
        try {
            return Fragment.instantiate(context, initialFragmentName, savedInstanceState);
        } catch (Exception e) {
            Log.e("Settings", "Unable to get target fragment", e);
            return null;
        }
    }

    default void onStartSubSettingsForArguments(Context context, Preference preference, boolean isAddPreferenceKey) {
        onStartSettingsForArguments(context, SubSettings.class, preference, isAddPreferenceKey);
    }

    default void onStartSettingsForArguments(Context context, Class<?> cls, Preference preference, boolean isAddPreferenceKey) {
        Bundle args = null;

        if (isAddPreferenceKey) {
            args = new Bundle();
            args.putString("key", preference.getKey());
        }

        if (preference instanceof XmlPreference xmlPreference) {
            if (args == null) args = new Bundle();
            args.putInt(":settings:fragment_resId", xmlPreference.getInflatedXml());
        } else {
            Intent intent = preference.getIntent();
            if (intent != null) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String xmlPath = (String) bundle.get("inflatedXml");
                    if (!TextUtils.isEmpty(xmlPath)) {
                        if (args == null) args = new Bundle();
                        String[] split = xmlPath.split("\\/");

                        String[] split2 = split[2].split("\\.");
                        if (split.length == 3) {
                            args.putInt(":settings:fragment_resId", context.getResources().getIdentifier(split2[0], split[1], context.getPackageName()));
                        }
                    }
                }
            }
        }

        String mFragmentName = preference.getFragment();
        String mTitle = preference.getTitle().toString();
        SettingLauncherHelper.onStartSettingsForArguments(context, cls, mFragmentName, args, mTitle);
    }
}
