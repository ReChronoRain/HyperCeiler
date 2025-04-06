package com.sevtinge.hyperceiler.ui.hooker.systemui.statusbar;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;

public class FocusNotificationLyrics extends DashboardFragment {
    SwitchPreference mHideClock;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_status_bar_music;
    }

    @Override
    public void initPrefs() {
        mHideClock = findPreference("prefs_key_system_ui_statusbar_music_hide_clock");
        mHideClock.setVisible(!isPad());
    }
}
