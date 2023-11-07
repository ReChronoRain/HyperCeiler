package com.sevtinge.hyperceiler.ui.fragment.systemui;

import static com.sevtinge.hyperceiler.utils.api.LinQiqiApisKt.isDeviceEncrypted;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.os.Build;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import moralnorm.preference.SeekBarPreferenceEx;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class LockScreenSettings extends SettingsPreferenceFragment {
    SwitchPreference mBlurButton; // 锁屏按钮背景模糊
    SwitchPreference mForceSystemFonts; // 时钟使用系统字体
    SwitchPreference mPasswordFree; // 开机免输入密码
    SeekBarPreferenceEx mChangingCVTime; // 充电信息显示刷新间隔

    @Override
    public int getContentResId() {
        return R.xml.system_ui_lock_screen;
    }

    @Override
    public void initPrefs() {
        mBlurButton = findPreference("prefs_key_system_ui_lock_screen_blur_button");
        mForceSystemFonts = findPreference("prefs_key_system_ui_lock_screen_force_system_fonts");
        mPasswordFree = findPreference("prefs_key_system_ui_lock_screen_password_free");
        mChangingCVTime = findPreference("prefs_key_system_ui_lock_screen_show_spacing");

        mBlurButton.setVisible(!isAndroidVersion(30));
        mForceSystemFonts.setVisible(!isAndroidVersion(30));
        mChangingCVTime.setVisible(isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU));

        if (isDeviceEncrypted(getContext())) {
            mPasswordFree.setChecked(false);
            mPasswordFree.setEnabled(false);
            mPasswordFree.setSummary(R.string.system_ui_lock_screen_password_free_tip);
        }
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }
}
