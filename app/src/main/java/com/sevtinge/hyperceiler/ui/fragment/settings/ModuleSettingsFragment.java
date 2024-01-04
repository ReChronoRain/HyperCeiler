package com.sevtinge.hyperceiler.ui.fragment.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.LauncherActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.BackupUtils;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import moralnorm.appcompat.app.AppCompatActivity;
import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class ModuleSettingsFragment extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {
    SharedPreferences sharedPreferences = getSharedPreferences();
    DropDownPreference mIconModePreference;
    DropDownPreference mIconModeValue;
    SwitchPreference mHideAppIcon;

    DropDownPreference mLogLevel;

    @Override
    public int getContentResId() {
        return R.xml.prefs_settings;
    }

    @Override
    public void initPrefs() {
        int mIconMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_settings_icon", "0"));
        mIconModePreference = findPreference("prefs_key_settings_icon");
        mIconModeValue = findPreference("prefs_key_settings_icon_mode");
        mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
        mLogLevel = findPreference("prefs_key_log_level");

        setIconMode(mIconMode);
        mIconModePreference.setOnPreferenceChangeListener(this);

        switch (BuildConfig.BUILD_TYPE) {
            case "canary" -> {
                mLogLevel.setEnabled(false);
                mLogLevel.setValue("3");
                mLogLevel.setSummary(R.string.disable_detailed_log_more);
                setLogLevel(3);
            }
            case "debug" -> {
                mLogLevel.setEnabled(false);
                mLogLevel.setValue("4");
                mLogLevel.setSummary(R.string.disable_detailed_log_more);
                setLogLevel(4);
            }
            default -> {
                if (mLogLevel != null) {
                    mLogLevel.setOnPreferenceChangeListener(
                        (preference, o) -> {
                            if (preference == mLogLevel) {
                                setLogLevel(Integer.parseInt((String) o));
                            }
                            return true;
                        }
                    );
                }
            }
        }

        if (mHideAppIcon != null) {
            mHideAppIcon.setOnPreferenceChangeListener((preference, o) -> {

                PackageManager pm = requireActivity().getPackageManager();
                int mComponentEnabledState;

                if ((Boolean) o) {
                    mComponentEnabledState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                } else {
                    mComponentEnabledState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                }

                pm.setComponentEnabledSetting(new ComponentName(requireActivity(), LauncherActivity.class), mComponentEnabledState, PackageManager.DONT_KILL_APP);
                return true;
            });
        }

        findPreference("prefs_key_back").setOnPreferenceClickListener(preference -> {
            final AppCompatActivity activity = (AppCompatActivity) getActivity();
            backupSettings(activity);
            return true;
        });

        findPreference("prefs_key_rest").setOnPreferenceClickListener(preference -> {
            restoreSettings(getActivity());
            return true;
        });

        findPreference("prefs_key_reset").setOnPreferenceClickListener(preference -> {
            DialogHelper.showDialog(getActivity(), R.string.reset_title, R.string.reset_desc, (dialog, which) -> {
                PrefsUtils.mSharedPreferences.edit().clear().apply();
                Toast.makeText(getActivity(), R.string.reset_okay, Toast.LENGTH_LONG).show();
            });
            return true;
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mIconModePreference) {
            setIconMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setLogLevel(int level) {
        switch (level) {
            case 0:
                sharedPreferences.edit().putString("prefs_key_log_level", "0").apply();
            case 1:
                sharedPreferences.edit().putString("prefs_key_log_level", "1").apply();
            case 2:
                sharedPreferences.edit().putString("prefs_key_log_level", "2").apply();
            case 3:
                sharedPreferences.edit().putString("prefs_key_log_level", "3").apply();
            case 4:
                sharedPreferences.edit().putString("prefs_key_log_level", "4").apply();
            default:
                sharedPreferences.edit().putString("prefs_key_log_level", "2").apply();
        }
    }

    private void setIconMode(int mode) {
        mIconModeValue.setVisible(mode != 0);
    }

    public void backupSettings(Activity activity) {
        BackupUtils.backup(activity);
    }

    public void restoreSettings(Activity activity) {
        BackupUtils.restore(activity);
    }
}
