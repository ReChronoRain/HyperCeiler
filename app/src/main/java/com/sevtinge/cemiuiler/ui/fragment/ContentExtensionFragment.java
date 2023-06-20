package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import android.view.View;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SwitchPreference;

public class ContentExtensionFragment extends SettingsPreferenceFragment {
    SwitchPreference mUnlockPad;

    @Override
    public int getContentResId() {
        return R.xml.content_extension;
    }

    @Override
    public void initPrefs() {
        mUnlockPad = findPreference("prefs_key_content_extension_unlock_pad");

        mUnlockPad.setVisible(isPad() && !isAndroidR());
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.content_extension),
            "com.miui.contentextension"
        );
    }
}
