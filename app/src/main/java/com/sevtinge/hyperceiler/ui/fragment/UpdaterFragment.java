package com.sevtinge.hyperceiler.ui.fragment;

import android.view.View;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.EditTextPreference;
import moralnorm.preference.Preference;

public class UpdaterFragment extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    DropDownPreference mUpdateMode;
    EditTextPreference mBigVersion;

    @Override
    public int getContentResId() {
        return R.xml.updater;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.updater),
            "com.android.updater"
        );
    }

    @Override
    public void initPrefs() {
        int mMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_updater_version_mode", "1"));
        mUpdateMode = findPreference("prefs_key_updater_version_mode");
        mBigVersion = findPreference("prefs_key_various_updater_big_version");

        setMode(mMode);
        mUpdateMode.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mUpdateMode) {
            setMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setMode(int mode) {
        mBigVersion.setVisible(mode == 2);
    }
}
