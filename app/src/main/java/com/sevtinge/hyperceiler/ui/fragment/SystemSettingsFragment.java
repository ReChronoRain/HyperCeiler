package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;

import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class SystemSettingsFragment extends SettingsPreferenceFragment {
    SwitchPreference mPad; // 解锁平板分区

    @Override
    public int getContentResId() {
        return R.xml.system_settings;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.system_settings),
            "com.android.settings"
        );
    }

    @Override
    public void initPrefs() {
        mPad = findPreference("prefs_key_system_settings_enable_pad_area");
        mPad.setVisible(isPad());
        animationScale();
    }

    public void animationScale() {
        SeekBarPreferenceEx seekBarPreferenceWn = findPreference("prefs_key_system_settings_window_animation_scale");
        setOnSeekBarChangeListener(seekBarPreferenceWn, "window_animation_scale");

        SeekBarPreferenceEx seekBarPreferenceTr = findPreference("prefs_key_system_settings_transition_animation_scale");
        setOnSeekBarChangeListener(seekBarPreferenceTr, "transition_animation_scale");

        SeekBarPreferenceEx seekBarPreferenceAn = findPreference("prefs_key_system_settings_animator_duration_scale");
        setOnSeekBarChangeListener(seekBarPreferenceAn, "animator_duration_scale");
    }

    public void setOnSeekBarChangeListener(SeekBarPreferenceEx mySeekBarPreference, String name) {
        mySeekBarPreference.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setAnimator(progress, name);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void setAnimator(int i, String name) {
        float mFloat = ((float) i) / 100;
        try {
            Settings.Global.putFloat(getContext().getContentResolver(), name, mFloat);
        } catch (Throwable e) {
            AndroidLogUtils.LogE("setAnimator", "set: " + name + " float: " + mFloat, e);
        }
    }
}
