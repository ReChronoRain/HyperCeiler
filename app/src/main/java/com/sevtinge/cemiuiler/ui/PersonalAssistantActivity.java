package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.BuildConfig;
import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

import moralnorm.preference.SwitchPreference;

public class PersonalAssistantActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new PersonalAssistantFragment();
    }

    public static class PersonalAssistantFragment extends SubFragment {

        SwitchPreference mWidgetCrack;

        @Override
        public int getContentResId() {
            return R.xml.various_personal_assistant;
        }

        @Override
        public void initPrefs() {
            mWidgetCrack = findPreference("prefs_key_personal_assistant_widget_crack");

            if (false) {
                mWidgetCrack.setVisible(BuildConfig.DEBUG);
            }
        }
    }
}
