package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class ContentExtensionFragment extends SettingsPreferenceFragment {
    SwitchPreference mUnlockTaplus;

    @Override
    public int getContentResId() {
        return R.xml.content_extension;
    }

    @Override
    public void initPrefs() {
        mUnlockTaplus= findPreference("prefs_key_content_extension_unlock_taplus");

        mUnlockTaplus.setVisible(!isAndroidVersion(30));
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.content_extension),
            "com.miui.contentextension"
        );
    }
}
