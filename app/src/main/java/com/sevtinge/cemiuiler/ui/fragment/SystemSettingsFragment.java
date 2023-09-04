package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreMiuiVersion;

import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.MainActivity;
import com.sevtinge.cemiuiler.ui.base.BaseSettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.ShellUtils;

import java.io.IOException;

import de.robv.android.xposed.XposedBridge;
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
       /* ShellUtils.CommandResult commandWn = ShellUtils.execCommand("settings get global window_animation_scale", true, true);
        if (commandWn.result == 0) {
            seekBarPreferenceWn.setDefaultValue(((int) Float.parseFloat(commandWn.successMsg) * 10));
        }*/
        seekBarPreferenceWn.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float mFloat = (float) i;
                try {
                    mFloat = mFloat / 10;
                    Runtime.getRuntime().exec("su -c settings put global window_animation_scale " + mFloat);
                } catch (IOException e) {
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBarPreferenceEx seekBarPreferenceTr = findPreference("prefs_key_system_settings_transition_animation_scale");
        seekBarPreferenceTr.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float mFloat = (float) i;
                try {
                    mFloat = mFloat / 10;
                    Runtime.getRuntime().exec("su -c settings put global transition_animation_scale " + mFloat);
                } catch (IOException e) {
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBarPreferenceEx seekBarPreferenceAn = findPreference("prefs_key_system_settings_animator_duration_scale");
        seekBarPreferenceAn.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float mFloat = (float) i;
                try {
                    mFloat = mFloat / 10;
                    Runtime.getRuntime().exec("su -c settings put global animator_duration_scale " + mFloat);
                } catch (IOException e) {
                }
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
