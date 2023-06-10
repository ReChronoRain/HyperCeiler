package com.sevtinge.cemiuiler.ui.fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class PersonalAssistantFragment extends SettingsPreferenceFragment {

    SwitchPreference mWidgetCrack;

    @Override
    public int getContentResId() {
        return R.xml.personal_assistant;
    }

    @Override
    public void initPrefs() {
        mWidgetCrack = findPreference("prefs_key_personal_assistant_widget_crack");

        if (!getSharedPreferences().getBoolean("prefs_key_hidden_function", false)) {
            mWidgetCrack.setVisible(false);
        }
    }
}
