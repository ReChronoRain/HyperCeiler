package com.sevtinge.cemiuiler.ui.sub;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.PickerHomeActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.Preference;
import moralnorm.preference.RadioButtonPreference;
import moralnorm.preference.ValuePreference;

public class MultiAction extends SubFragment {

    Bundle args;
    String mKey = null;
    String mActionKey;
    String mAppValue;

    RadioButtonPreference mClearMemoryPreference;
    RadioButtonPreference mInvertColorsPreference;
    RadioButtonPreference mNoActionPreference;
    RadioButtonPreference mOpenNotificationCenterPreference;
    RadioButtonPreference mScreenLockPreference;
    RadioButtonPreference mScreenSleepPreference;
    RadioButtonPreference mScreenCapturePreference;
    RadioButtonPreference mOpenPowerMenuPreference;
    RadioButtonPreference mScreenRecentsPreference;
    RadioButtonPreference mVolumeDialogPreference;
    RadioButtonPreference mOpenAppPreference;
    ValuePreference mOpenAppSelectorPreference;

    @Override
    public int getContentResId() {
        return R.xml.home_multi_action;
    }

    @Override
    public void initPrefs() {
        args = getActivity().getIntent().getExtras();
        mKey = args.getString("key");
        mActionKey = mKey + "_action";

        mClearMemoryPreference = findPreference("prefs_key_clear_memory");
        mInvertColorsPreference = findPreference("prefs_key_invert_colors");
        mNoActionPreference = findPreference("prefs_key_no_action");
        mOpenNotificationCenterPreference = findPreference("prefs_key_open_notification_center");
        mScreenLockPreference = findPreference("prefs_key_screen_lock");
        mScreenSleepPreference = findPreference("prefs_key_screen_sleep");
        mScreenCapturePreference = findPreference("prefs_key_screen_capture");
        mOpenPowerMenuPreference = findPreference("prefs_key_open_powermenu");
        mScreenRecentsPreference = findPreference("prefs_key_screen_recents");
        mVolumeDialogPreference = findPreference("prefs_key_volume_dialog");

        mOpenAppPreference = findPreference("prefs_key_open_app");
        mOpenAppSelectorPreference = findPreference("prefs_key_open_app_selector");


        setValue();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference != null) {
            updateControls(preference.getKey(), mActionKey);
        }
        return true;
    }

    public void updateControls(String actionKey, String key) {
        switch (actionKey) {
            case "prefs_key_no_action" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 0).apply();
            case "prefs_key_open_notification_center" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 1).apply();
            case "prefs_key_clear_memory" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 2).apply();
            case "prefs_key_invert_colors" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 3).apply();
            case "prefs_key_screen_lock" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 4).apply();
            case "prefs_key_screen_sleep" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 5).apply();
            case "prefs_key_screen_capture" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 6).apply();
            case "prefs_key_screen_recents" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 7).apply();
            case "prefs_key_volume_dialog" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 8).apply();
            case "prefs_key_open_powermenu" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 12).apply();
            case "prefs_key_open_app" ->
                    PrefsUtils.mSharedPreferences.edit().putInt(key, 13).apply();
            case "prefs_key_open_app_selector" -> {
                Bundle args = new Bundle();
                args.putString("title", getResources().getString(R.string.home_gesture_multi_choose_app));
                args.putBoolean("app_selector", true);
                args.putString("app_selector_key", mKey);
                openSubFragment(args, PickerHomeActivity.Actions.Apps);
            }
        }
    }

    private void setValue() {
        int value = 0;
        if (getSharedPreferences().contains(mActionKey)) {
            value = PrefsUtils.getSharedIntPrefs(getContext(), mActionKey, 0);
        }
        switch (value) {
            case 0 -> mNoActionPreference.setChecked(true);
            case 1 -> mOpenNotificationCenterPreference.setChecked(true);
            case 2 -> mClearMemoryPreference.setChecked(true);
            case 3 -> mInvertColorsPreference.setChecked(true);
            case 4 -> mScreenLockPreference.setChecked(true);
            case 5 -> mScreenSleepPreference.setChecked(true);
            case 6 -> mScreenCapturePreference.setChecked(true);
            case 7 -> mScreenRecentsPreference.setChecked(true);
            case 8 -> mVolumeDialogPreference.setChecked(true);
            case 12 -> mOpenPowerMenuPreference.setChecked(true);
            case 13 -> {
                mOpenAppPreference.setChecked(true);
                updateAppTitle(getAppName(getContext(), PrefsUtils.mSharedPreferences.getString(mKey + "_app", "")));
            }
        }
    }
    String a;

    public void updateAppTitle(String title) {
        if (PrefsUtils.contains(mKey + "_app")) {
            mOpenAppSelectorPreference.setTitle(title);
        }
    }

    private boolean getKey() {
        if (!TextUtils.isEmpty(mKey)) {
            return true;
        } else {
            Toast.makeText(getContext(), "getKey is Null", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static String getAppName(Context context, String pkgActName) {
        return getAppName(context, pkgActName, false);
    }

    public static String getAppName(Context context, String pkgActName, boolean forcePkg) {
        PackageManager pm = context.getPackageManager();
        String not_selected = "None";
        String[] pkgActArray = pkgActName.split("\\|");
        ApplicationInfo ai;

        if (!pkgActName.equals(not_selected)) {
            if (pkgActArray.length >= 1 && pkgActArray[0] != null) try {
                if (!forcePkg && pkgActArray.length >= 2 && pkgActArray[1] != null && !pkgActArray[1].trim().equals("")) {
                    return pm.getActivityInfo(new ComponentName(pkgActArray[0], pkgActArray[1]), 0).loadLabel(pm).toString();
                } else if (!pkgActArray[0].trim().equals("")) {
                    ai = pm.getApplicationInfo(pkgActArray[0], 0);
                    return pm.getApplicationLabel(ai).toString();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
