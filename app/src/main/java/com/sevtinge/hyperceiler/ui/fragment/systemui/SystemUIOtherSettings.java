package com.sevtinge.hyperceiler.ui.fragment.systemui;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMiuiVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class SystemUIOtherSettings extends SettingsPreferenceFragment {

    PreferenceCategory mMonetOverlay;
    SwitchPreference mMiuiMultiWinSwitch;
    SwitchPreference mDisableBluetoothRestrict; // 禁用蓝牙临时关闭

    @Override
    public int getContentResId() {
        return R.xml.system_ui_other;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    @Override
    public void initPrefs() {
        mMonetOverlay = findPreference("prefs_key_system_ui_monet");
        mDisableBluetoothRestrict = findPreference("prefs_key_system_ui_disable_bluetooth_restrict");
        mMiuiMultiWinSwitch = findPreference("prefs_key_system_ui_disable_miui_multi_win_switch");

        mMonetOverlay.setVisible(!isAndroidVersion(30));
        mDisableBluetoothRestrict.setVisible(isMiuiVersion(14f) && isMoreAndroidVersion(31));
        mMiuiMultiWinSwitch.setVisible(isMoreHyperOSVersion(1f));
    }
}
