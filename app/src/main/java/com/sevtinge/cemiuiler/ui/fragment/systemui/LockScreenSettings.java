package com.sevtinge.cemiuiler.ui.fragment.systemui;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class LockScreenSettings extends SettingsPreferenceFragment {
    SwitchPreference mBlurButton; // 锁屏按钮背景模糊
    SwitchPreference mForceSystemFonts; // 时钟使用系统字体

    @Override
    public int getContentResId() {
        return R.xml.system_ui_lock_screen;
    }

    @Override
    public void initPrefs() {
        mBlurButton = findPreference("prefs_key_system_ui_lock_screen_blur_button");
        mForceSystemFonts = findPreference("prefs_key_system_ui_lock_screen_force_system_fonts");

        mBlurButton.setVisible(!isAndroidR());
        mForceSystemFonts.setVisible(!isAndroidR());
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }
}
