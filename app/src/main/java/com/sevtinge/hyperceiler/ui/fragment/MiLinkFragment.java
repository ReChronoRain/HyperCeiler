package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Intent;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.SubPickerActivity;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class MiLinkFragment extends SettingsPreferenceFragment {

    SwitchPreference mUnlockHMind;

    @Override
    public int getContentResId() {
        return R.xml.milink;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(!isMoreHyperOSVersion(1f) ? R.string.milink : R.string.milink_hyperos),
            "com.milink.service"
        );
    }

    @Override
    public void initPrefs() {
        mUnlockHMind = findPreference("prefs_key_milink_unlock_hmind");
        if (mUnlockHMind != null) {
            mUnlockHMind.setVisible(isMoreHyperOSVersion(1f));
        }
    }
}
