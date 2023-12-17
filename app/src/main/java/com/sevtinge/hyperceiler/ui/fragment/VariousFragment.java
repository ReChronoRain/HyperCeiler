package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceCategory;

public class VariousFragment extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    DropDownPreference mSuperModePreference;
    PreferenceCategory mDefault;
    Preference mMipad; // 平板相关功能

    @Override
    public int getContentResId() {
        return R.xml.various;
    }

    @Override
    public void initPrefs() {
        int mode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_various_super_clipboard_e", "0"));
        mSuperModePreference = findPreference("prefs_key_various_super_clipboard_e");
        mDefault = findPreference("prefs_key_various_super_clipboard_key");
        mMipad = findPreference("prefs_key_various_mipad");

        mMipad.setVisible(isPad());

        setSuperMode(mode);
        mSuperModePreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mSuperModePreference) {
            setSuperMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setSuperMode(int mode) {
        mDefault.setVisible(mode == 1);
    }
}
