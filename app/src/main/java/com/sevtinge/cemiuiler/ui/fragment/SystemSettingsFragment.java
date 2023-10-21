package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreMiuiVersion;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class SystemSettingsFragment extends SettingsPreferenceFragment {
    PreferenceCategory mNewNfc; // 新版 NFC 界面
    SwitchPreference mHighMode; // 极致模式
    SwitchPreference mAreaScreenshot; // 区域截屏
    SwitchPreference mKnuckleFunction; // 指关节相关
    SwitchPreference mNoveltyHaptic; // 新版触感调节页面
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
        mHighMode = findPreference("prefs_key_system_settings_develop_speed_mode");
        mAreaScreenshot = findPreference("prefs_key_system_settings_area_screenshot");
        mKnuckleFunction = findPreference("prefs_key_system_settings_knuckle_function");
        mNewNfc = findPreference("prefs_key_system_settings_connection_sharing");
        mNoveltyHaptic = findPreference("prefs_key_system_settings_novelty_haptic");
        mPad = findPreference("prefs_key_system_settings_enable_pad_area");

        mHighMode.setVisible(!isAndroidR());
        mAreaScreenshot.setVisible(isAndroidR());
        mKnuckleFunction.setVisible(isMoreMiuiVersion(13f));
        mNewNfc.setVisible(isMoreMiuiVersion(14f) && isMoreAndroidVersion(33));
        mNoveltyHaptic.setVisible(isMoreMiuiVersion(14f) && isMoreAndroidVersion(31));
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
            Log.e("setAnimator", "set: " + name + " float: " + mFloat, e);
        }
    }
}
