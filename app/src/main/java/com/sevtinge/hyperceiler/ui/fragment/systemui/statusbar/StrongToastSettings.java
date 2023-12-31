package com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.api.miuiStringToast.MiuiStringToast;

import moralnorm.preference.Preference;

public class StrongToastSettings extends SettingsPreferenceFragment {
    Preference mShortToast;
    Preference mLongToast;
    Preference mVideoToast;

    @Override
    public int getContentResId() { return R.xml.system_ui_status_bar_strong_toast; }

    @Override
    public void initPrefs() {
        mShortToast = findPreference("prefs_key_system_ui_status_bar_strong_toast_test_short_text");
        mLongToast = findPreference("prefs_key_system_ui_status_bar_strong_toast_test_long_text");
        mVideoToast = findPreference("prefs_key_system_ui_status_bar_strong_toast_test_video");

        mShortToast.setOnPreferenceClickListener(preference -> {
            MiuiStringToast.INSTANCE.showStringToast(requireActivity(), getResources().getString(R.string.system_ui_status_bar_strong_toast_test_short_text_0), 1);
            return true;
        });
        mLongToast.setOnPreferenceClickListener(preference -> {
            MiuiStringToast.INSTANCE.showStringToast(requireActivity(), getResources().getString(R.string.system_ui_status_bar_strong_toast_test_long_text_1), 1);
            return true;
        });
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }
}
