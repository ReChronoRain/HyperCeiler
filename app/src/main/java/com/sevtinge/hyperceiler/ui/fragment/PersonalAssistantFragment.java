package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import moralnorm.preference.ColorPickerPreference;
import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class PersonalAssistantFragment extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    SwitchPreference mBlurBackground;
    SeekBarPreferenceEx mBlurRadius;
    ColorPickerPreference mBlurColor;
    DropDownPreference mBlurBackgroundStyle;

    @Override
    public int getContentResId() {
        return R.xml.personal_assistant;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.personal_assistant),
            "com.miui.personalassistant"
        );
    }

    @Override
    public void initPrefs() {
        int mBlurMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_personal_assistant_value", "1"));
        mBlurBackground = findPreference("prefs_key_pa_enable");
        mBlurBackgroundStyle = findPreference("prefs_key_personal_assistant_value");
        mBlurRadius = findPreference("prefs_key_personal_assistant_blurradius");
        mBlurColor = findPreference("prefs_key_personal_assistant_color");

        mBlurBackground.setVisible(!isAndroidR()); // 负一屏背景设置

        setBlurMode(mBlurMode);
        mBlurBackgroundStyle.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mBlurBackgroundStyle) {
            setBlurMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setBlurMode(int mode) {
        mBlurRadius.setVisible(mode == 2);
        mBlurColor.setVisible(mode == 2);
    }
}
