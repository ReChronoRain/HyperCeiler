package com.sevtinge.cemiuiler.ui.fragment;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class PersonalAssistantFragment extends SettingsPreferenceFragment {

    SwitchPreference mWidgetCrack;

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
        mWidgetCrack = findPreference("prefs_key_personal_assistant_widget_crack");

        if (!getSharedPreferences().getBoolean("prefs_key_hidden_function", false)) {
            mWidgetCrack.setVisible(false);
        }
    }
}
