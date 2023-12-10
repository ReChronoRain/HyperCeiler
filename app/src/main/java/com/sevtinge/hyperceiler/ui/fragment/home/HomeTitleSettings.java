package com.sevtinge.hyperceiler.ui.fragment.home;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.Intent;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.SubPickerActivity;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class HomeTitleSettings extends SettingsPreferenceFragment {

    SwitchPreference mDisableMonoChrome;
    SwitchPreference mDisableMonetColor;
    SwitchPreference mDisableHideTheme;
    Preference mIconTitleCustomization;

    @Override
    public int getContentResId() {
        return R.xml.home_title;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.mihome),
            "com.miui.home"
        );
    }

    @Override
    public void initPrefs() {
        mIconTitleCustomization = findPreference("prefs_key_home_title_title_icontitlecustomization");
        mDisableMonoChrome = findPreference("prefs_key_home_other_icon_mono_chrome");
        mDisableMonoChrome.setVisible(isMoreAndroidVersion(33));
        mDisableMonoChrome.setOnPreferenceChangeListener((preference, o) -> true);
        mDisableMonetColor = findPreference("prefs_key_home_other_icon_monet_color");
        mDisableMonetColor.setVisible(isMoreAndroidVersion(33));
        mDisableMonetColor.setOnPreferenceChangeListener((preference, o) -> true);
        mDisableHideTheme = findPreference("prefs_key_home_title_disable_hide_theme");
        mDisableHideTheme.setVisible(isPad());
        mIconTitleCustomization.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("is_app_selector", true);
            intent.putExtra("need_mode", 3);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });
    }
}
