package com.sevtinge.cemiuiler.ui.fragment.framework;

import android.view.View;
import android.widget.SeekBar;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.prefs.SeekBarPreferenceEx;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

public class DisplaySettings extends SettingsPreferenceFragment {

    SeekBarPreferenceEx minBrightness;
    SeekBarPreferenceEx maxBrightness;

    @Override
    public int getContentResId() {
        return R.xml.framework_display;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartSystemDialog();
    }

    @Override
    public void initPrefs() {
        maxBrightness = findPreference("pref_key_system_ui_auto_brightness_max");
        minBrightness = findPreference("pref_key_system_ui_auto_brightness_min");
        assert minBrightness != null;
        minBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (maxBrightness.getValue() <= progress) maxBrightness.setValue(progress + 1);
                maxBrightness.setMinValue(progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }
}
