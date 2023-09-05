package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreMiuiVersion;

import android.view.View;
import android.widget.SeekBar;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import java.io.IOException;

import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class SystemSettingsFragment extends SettingsPreferenceFragment {
    PreferenceCategory mNewNfc; // 新版 NFC 界面
    SwitchPreference mHighMode; // 极致模式
    SwitchPreference mAreaScreenshot; // 区域截屏
    SwitchPreference mKnuckleFunction; // 指关节相关
    SwitchPreference mNoveltyHaptic; // 新版触感调节页面

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

        mHighMode.setVisible(!isAndroidR());
        mAreaScreenshot.setVisible(isAndroidR());
        mKnuckleFunction.setVisible(isMoreMiuiVersion(13f));
        mNewNfc.setVisible(isMoreMiuiVersion(14f) && isMoreAndroidVersion(33));
        mNoveltyHaptic.setVisible(isMoreMiuiVersion(14f) && isMoreAndroidVersion(31));
        animationScale();
    }

    public void animationScale() {
        SeekBarPreferenceEx seekBarPreferenceWn = findPreference("prefs_key_system_settings_window_animation_scale");
        /*ShellUtils.CommandResult commandWn = ShellUtils.execCommand("settings get global window_animation_scale", true, true);
        if (commandWn.result == 0) {
            seekBarPreferenceWn.setValue(((int) Float.parseFloat(commandWn.successMsg) * 100));
        }*/
        setOnSeekBarChangeListener(seekBarPreferenceWn, "su -c settings put global window_animation_scale ");

        SeekBarPreferenceEx seekBarPreferenceTr = findPreference("prefs_key_system_settings_transition_animation_scale");
        setOnSeekBarChangeListener(seekBarPreferenceTr, "su -c settings put global transition_animation_scale ");

        SeekBarPreferenceEx seekBarPreferenceAn = findPreference("prefs_key_system_settings_animator_duration_scale");
        setOnSeekBarChangeListener(seekBarPreferenceAn, "su -c settings put global animator_duration_scale ");
    }

    public void setOnSeekBarChangeListener(SeekBarPreferenceEx mySeekBarPreference, String shell) {
        mySeekBarPreference.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setAnimator(i, shell);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void setAnimator(int i, String shell) {
        float mFloat = (float) i;
        mFloat = mFloat / 100;
        try {
            Runtime.getRuntime().exec(shell + mFloat);
        } catch (IOException e) {
        }
    }
}
