package com.sevtinge.hyperceiler.hooker;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

public class AiasstFragment extends DashboardFragment {

    SwitchPreference mSplitTranslation;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.aiasst;
    }

    @Override
    public void initPrefs() {
        mSplitTranslation = findPreference("prefs_key_aiasst_unlock_split_screen_translation");

        setFuncHints("com.xiaomi.aiasst.vision",
            rule(mSplitTranslation, APP_HINT_UNSUPPORTED, APP_MATCH_OUT_OF_RANGE, atMost(540110140))
        );
    }
}
