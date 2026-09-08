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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.sub;

import static com.sevtinge.hyperceiler.sub.SubPickerActivity.CALLBACK_MODE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;

import java.util.HashMap;

import fan.preference.RadioButtonPreference;
import fan.preference.RadioButtonPreferenceCategory;

public class MultiActionSettings extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener,
    AppSelectorRadioButtonPreference.onArrowViewClickListener {

    Bundle args;
    String mKey = null;
    String mActionKey;

    private int mCurrentOptimizeMode;

    int[] mMultiActionValues;
    String[] mMultiActionKeys;

    HashMap<String , Integer> mMultiActions = new HashMap<>();

    private AppSelectorRadioButtonPreference mExpertPreference;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_multi_action;
    }

    @Override
    public void initPrefs() {
        args = getArguments();
        mKey = args.getString("key");
        mActionKey = mKey + "_action";

        mCurrentOptimizeMode = getMultiActionMode();
        if (mCurrentOptimizeMode == 5) {
            mCurrentOptimizeMode = 4;
            setMultiActionMode(4);
        }
        mMultiActionValues = getResources().getIntArray(R.array.multi_action_value);
        mMultiActionKeys = getResources().getStringArray(R.array.multi_action_key);
        generateScreenColorPreference();
    }

    private int getMultiActionMode() {
        return hasKey(mActionKey) ? PrefsBridge.getInt(mActionKey, 0) : 0;
    }

    private void setMultiActionMode(int mode) {
        PrefsBridge.putByApp(mActionKey, mode);
    }

    public void updateAppSelectorTitle() {
        if (hasKey(mKey + "_app")) {
            String title = getAppName(getContext(), PrefsBridge.getString(mKey + "_app", ""));
            mExpertPreference.setSummary(title);
        }
    }

    public static String getAppName(Context context, String pkgActName) {
        return getAppName(context, pkgActName, false);
    }

    public static String getAppName(Context context, String pkgActName, boolean forcePkg) {
        PackageManager pm = context.getPackageManager();
        String notSelected = "None";
        String[] pkgActArray = pkgActName.split("\\|");
        ApplicationInfo ai;

        if (!pkgActName.equals(notSelected)) {
            if (pkgActArray.length >= 1 && pkgActArray[0] != null) try {
                if (!forcePkg && pkgActArray.length >= 2 && pkgActArray[1] != null && !pkgActArray[1].trim().isEmpty()) {
                    return pm.getActivityInfo(new ComponentName(pkgActArray[0], pkgActArray[1]), 0).loadLabel(pm).toString();
                } else if (!pkgActArray[0].trim().isEmpty()) {
                    ai = pm.getApplicationInfo(pkgActArray[0], 0);
                    return pm.getApplicationLabel(ai).toString();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private final ActivityResultLauncher<Intent> appPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == 1 && result.getData() != null) {
                Intent data = result.getData();
                String mAppPackageName = data.getStringExtra("appPackageName");
                String mAppActivityName = data.getStringExtra("appActivityName");
                PrefsBridge.putByApp(mKey + "_app", mAppPackageName + "|" + mAppActivityName);
                updateAppSelectorTitle();
            }
        });




    private void generateScreenColorPreference() {
        PreferenceCategory preferenceCategory = generateCategory();
        String[] multiActionTitles = getResources().getStringArray(R.array.multi_action_title);
        for (int i = 0; i < mMultiActionKeys.length; i++) {
            String key = mMultiActionKeys[i];
            if (!shouldShowAction(key)) {
                continue;
            }
            mMultiActions.put(key, mMultiActionValues[i]);
            RadioButtonPreferenceCategory radioButtonPreferenceCategory = new RadioButtonPreferenceCategory(getThemedContext());
            preferenceCategory.addPreference(radioButtonPreferenceCategory);
            if (key.equals("prefs_key_open_app")) {
                addExpertModeIfNeed(radioButtonPreferenceCategory, key);
            } else {
                RadioButtonPreference radioButtonPreference = new RadioButtonPreference(getThemedContext());
                radioButtonPreference.setKey(mMultiActionKeys[i]);
                radioButtonPreference.setTitle(multiActionTitles[i]);
                radioButtonPreference.setPersistent(false);
                radioButtonPreference.setLayoutResource(fan.preference.R.layout.miuix_preference_radiobutton_two_state_background);
                radioButtonPreference.setOnPreferenceChangeListener(this);
                radioButtonPreferenceCategory.addPreference(radioButtonPreference);
                radioButtonPreference.setChecked(mMultiActionValues[i] == mCurrentOptimizeMode);
            }
        }
    }

    private boolean shouldShowAction(String key) {
        if ("prefs_key_go_home".equals(key)) {
            return supportsGoHomeAction();
        }
        if ("prefs_key_force_stop_current_app".equals(key)) {
            return supportsLineLongPressAction();
        }
        if ("prefs_key_google_voice_assistant".equals(key)) {
            return true;
        }
        if ("prefs_key_super_xiaoai_circle_search".equals(key)
            || "prefs_key_google_circle_to_search".equals(key)
            ) {
            return supportsAssistantGestureAction();
        }
        return true;
    }

    private boolean supportsGoHomeAction() {
        return "prefs_key_home_gesture_line_double_click".equals(mKey)
            || "prefs_key_home_gesture_line_long_press".equals(mKey)
            || "prefs_key_home_navigation_assist_left_slide".equals(mKey)
            || "prefs_key_home_navigation_assist_right_slide".equals(mKey);
    }

    private boolean supportsLineLongPressAction() {
        return "prefs_key_home_gesture_line_long_press".equals(mKey);
    }

    private boolean supportsAssistantGestureAction() {
        return "prefs_key_home_gesture_line_long_press".equals(mKey)
            || "prefs_key_home_gesture_line_double_click".equals(mKey)
            || "prefs_key_home_navigation_assist_left_slide".equals(mKey)
            || "prefs_key_home_navigation_assist_right_slide".equals(mKey);
    }

    private void addExpertModeIfNeed(RadioButtonPreferenceCategory radioButtonPreferenceCategory, String key) {
        mExpertPreference = new AppSelectorRadioButtonPreference(getThemedContext());
        mExpertPreference.setKey(key);
        mExpertPreference.setTitle(getResources().getString(R.string.array_global_actions_launch));
        if (hasKey(mKey + "_app")) {
            String title = getAppName(getContext(), PrefsBridge.getString(mKey + "_app", ""));
            mExpertPreference.setSummary(title);
        } else {
            mExpertPreference.setSummary(getResources().getString(R.string.array_global_actions_launch_choose));
        }
        mExpertPreference.setPersistent(false);
        mExpertPreference.setLayoutResource(fan.preference.R.layout.miuix_preference_radiobutton_two_state_background);
        mExpertPreference.setOnPreferenceChangeListener(this);
        mExpertPreference.setArrowViewClickListener(this);
        radioButtonPreferenceCategory.addPreference(mExpertPreference);
        mExpertPreference.setChecked(mCurrentOptimizeMode == 13);
    }

    private PreferenceCategory generateCategory() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        PreferenceCategory preferenceCategory = new PreferenceCategory(getPrefContext());
        preferenceCategory.setKey("multi_action");
        preferenceCategory.setPersistent(false);
        preferenceScreen.addPreference(preferenceCategory);
        return preferenceCategory;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        String key = preference.getKey();
        if (preference instanceof RadioButtonPreference) {
            updateRadioButtonPreference(key);
            return true;
        }
        return false;
    }

    private void updateRadioButtonPreference(String key) {
        setMultiActionMode(mMultiActions.get(key));
        PreferenceCategory preferenceCategory = getPreferenceScreen().findPreference("multi_action");
        if (preferenceCategory != null) {
            int preferenceCount = preferenceCategory.getPreferenceCount();
            for (int i = 0; i < preferenceCount; i++) {
                RadioButtonPreferenceCategory category = (RadioButtonPreferenceCategory) preferenceCategory.getPreference(i);
                RadioButtonPreference preference = (RadioButtonPreference) category.getPreference(0);
                if (preference != null) {
                    preference.setChecked(preference.getKey().equals(key));
                }
            }
        }
    }

    @Override
    public void onClick() {
        Intent intent = new Intent(getActivity(), SubPickerActivity.class);
        intent.putExtra("mode", CALLBACK_MODE);
        intent.putExtra("key", mKey);
        appPickerLauncher.launch(intent);
    }
}
