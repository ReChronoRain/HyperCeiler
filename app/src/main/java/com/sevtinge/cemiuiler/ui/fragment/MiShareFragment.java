package com.sevtinge.cemiuiler.ui.fragment;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class MiShareFragment extends SettingsPreferenceFragment {

    SwitchPreference mMiShareNotAuto;

    @Override
    public int getContentResId() {
        return R.xml.mishare;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.mishare),
            "com.miui.mishare.connectivity"
        );
    }

    @Override
    public void initPrefs() {
        mMiShareNotAuto = findPreference("prefs_key_disable_mishare_auto_off");
        /*int appVersionCode = getPackageVersionCode(lpparam);

        if (appVersionCode <= 21400) {
            mMiShareNotAuto.setSummary(R.string.app_version_not_supported);
            mMiShareNotAuto.setEnabled(false);
        }*/
    }
}
