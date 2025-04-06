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
package com.sevtinge.hyperceiler.ui.base.sub;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import fan.preference.RadioButtonPreference;

public class MultiActionSettings extends SettingsPreferenceFragment {

    Bundle args;
    String mKey = null;
    String mActionKey;
    String mAppValue;

    RadioButtonPreference mClearMemory;
    RadioButtonPreference mInvertColors;
    RadioButtonPreference mNoAction;
    RadioButtonPreference mOpenNotificationCenter;
    RadioButtonPreference mScreenLock;
    RadioButtonPreference mScreenSleep;
    RadioButtonPreference mScreenCapture;
    RadioButtonPreference mOpenPowerMenu;
    RadioButtonPreference mScreenRecents;
    RadioButtonPreference mVolumeDialog;
    RadioButtonPreference mOpenApp;
    Preference mAppSelector;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_multi_action;
    }

    @Override
    public void initPrefs() {
        args = getArguments();
        mKey = args.getString("key");
        mActionKey = mKey + "_action";

        mNoAction = findPreference("prefs_key_no_action");
        mOpenNotificationCenter = findPreference("prefs_key_open_notification_center");
        mScreenLock = findPreference("prefs_key_screen_lock");
        mScreenSleep = findPreference("prefs_key_screen_sleep");
        mScreenCapture = findPreference("prefs_key_screen_capture");
        mOpenPowerMenu = findPreference("prefs_key_open_powermenu");
        mClearMemory = findPreference("prefs_key_clear_memory");
        mInvertColors = findPreference("prefs_key_invert_colors");
        mScreenRecents = findPreference("prefs_key_screen_recents");
        mVolumeDialog = findPreference("prefs_key_volume_dialog");
        mOpenApp = findPreference("prefs_key_open_app");
        mAppSelector = findPreference("prefs_key_open_app_selector");
        updateAction();
    }

    private void updateAction() {
        int value = hasKey(mActionKey) ? PrefsUtils.getSharedIntPrefs(getContext(), mActionKey, 0) : 0;
        switch (value) {
            case 0 -> mNoAction.setChecked(true);
            case 1 -> mOpenNotificationCenter.setChecked(true);
            case 2 -> mClearMemory.setChecked(true);
            case 3 -> mInvertColors.setChecked(true);
            case 4 -> mScreenLock.setChecked(true);
            case 5 -> mScreenSleep.setChecked(true);
            case 6 -> mScreenCapture.setChecked(true);
            case 7 -> mScreenRecents.setChecked(true);
            case 8 -> mVolumeDialog.setChecked(true);
            case 12 -> mOpenPowerMenu.setChecked(true);
            case 13 -> {
                mOpenApp.setChecked(true);
                updateAppSelectorTitle();
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference != mAppSelector) {
            editActionIntSharedPrefs(getAction(preference));
        } else {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("mode", AppPicker.CALLBACK_MODE);
            startActivityForResult(intent, 0);
        }
        return true;
    }

    private int getAction(Preference preference) {
        if (preference == mOpenNotificationCenter) {
            return 1;
        } else if (preference == mClearMemory) {
            return 2;
        } else if (preference == mInvertColors) {
            return 3;
        } else if (preference == mScreenLock) {
            return 4;
        } else if (preference == mScreenSleep) {
            return 5;
        } else if (preference == mScreenCapture) {
            return 6;
        } else if (preference == mScreenRecents) {
            return 7;
        } else if (preference == mVolumeDialog) {
            return 8;
        } else if (preference == mOpenPowerMenu) {
            return 12;
        } else if (preference == mOpenApp) {
            return 13;
        } else {
            return 0;
        }
    }

    private void editActionIntSharedPrefs(int value) {
        PrefsUtils.mSharedPreferences.edit().putInt(mActionKey, value).apply();
    }

    public void updateAppSelectorTitle() {
        if (hasKey(mKey + "_app")) {
            String title = getAppName(getContext(), PrefsUtils.mSharedPreferences.getString(mKey + "_app", ""));
            mAppSelector.setTitle(title);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 1) {
            String mAppPackageName = data.getStringExtra("appPackageName");
            String mAppActivityName = data.getStringExtra("appActivityName");
            PrefsUtils.mSharedPreferences.edit().putString(mKey + "_app", mAppPackageName + "|" + mAppActivityName).apply();
            updateAppSelectorTitle();
        }
    }
}
