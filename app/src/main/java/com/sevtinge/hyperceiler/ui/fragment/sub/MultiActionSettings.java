package com.sevtinge.hyperceiler.ui.fragment.sub;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.SubPickerActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import java.util.Objects;

import moralnorm.preference.Preference;
import moralnorm.preference.RadioButtonPreference;

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
    RadioButtonPreference mLockScreen;
    RadioButtonPreference mOpenApp;
    Preference mAppSelector;

    @Override
    public int getContentResId() {
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
        mLockScreen = findPreference("prefs_key_open_powermenu");
        mLockScreen.setVisible(Objects.equals(mKey, "prefs_key_home_navigation_assist_left_slide") || Objects.equals(mKey, "prefs_key_home_navigation_assist_right_slide"));
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
            case 10 -> mLockScreen.setChecked(true);
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
            intent.putExtra("is_app_selector", true);
            intent.putExtra("need_mode", 1);
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
        } else if (preference == mLockScreen){
            return 10;
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
