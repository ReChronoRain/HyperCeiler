package com.sevtinge.hyperceiler.ui.fragment.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.PreferenceCategory;

public class SystemUIOtherSettings extends SettingsPreferenceFragment {

    PreferenceCategory mMonetOverlay;

    @Override
    public int getContentResId() {
        return R.xml.system_ui_other;
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
        mMonetOverlay = findPreference("prefs_key_system_ui_monet");
        mMonetOverlay.setVisible(!isAndroidR());
    }
}
