package com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class ClockIndicatorSettings extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    DropDownPreference mClockModePreference;
    PreferenceCategory mDefault;
    PreferenceCategory mGeek;
    SwitchPreference mDisableAnim;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_status_bar_clock_indicator;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    @Override
    public void initPrefs() {
        int mClockMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_statusbar_clock_mode", "0"));
        mClockModePreference = findPreference("prefs_key_system_ui_statusbar_clock_mode");
        mDefault = findPreference("prefs_key_system_ui_statusbar_clock_default");
        mGeek = findPreference("prefs_key_system_ui_statusbar_clock_geek");
        mDisableAnim = findPreference("prefs_key_system_ui_disable_clock_anim");

        if (mDisableAnim != null) {
            mDisableAnim.setVisible(isMoreHyperOSVersion(1f));
        }
        setClockMode(mClockMode);
        setIsEnableClockMode(mClockMode);
        mClockModePreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mClockModePreference) {
            setClockMode(Integer.parseInt((String) o));
            setIsEnableClockMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setClockMode(int mode) {
        mDefault.setVisible(mode == 1);
        mGeek.setVisible(mode == 2);
    }

    private void setIsEnableClockMode(int mode) {
        mDisableAnim.setEnabled(mode == 0);
        mDisableAnim.setChecked(mode != 0);
    }
}
