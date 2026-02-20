package com.sevtinge.hyperceiler.ui.page;

import static com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isBeta;
import static com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isCanary;
import static com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isRelease;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.fan.common.base.BasePreferenceFragment;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.common.utils.LanguageHelper;
import com.sevtinge.hyperceiler.libhook.utils.api.BackupUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.LogManager;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.oldui.ui.LauncherActivity;
import com.sevtinge.hyperceiler.ui.HomePageActivity;
import com.sevtinge.hyperceiler.ui.SwitchManager;

import fan.appcompat.app.AppCompatActivity;
import fan.internal.utils.ViewUtils;
import fan.preference.DropDownPreference;
import fan.provider.Settings;

public class SettingsFragment extends BasePreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    SwitchPreference mFloatBottomPreference;
    DropDownPreference mIconModePreference;
    DropDownPreference mIconModeValue;
    SwitchPreference mHideAppIcon;

    DropDownPreference mLogLevel;
    DropDownPreference mLanguage;

    @Override
    public int getPreferenceScreenResId() {
        return com.sevtinge.hyperceiler.R.xml.prefs_settings;
    }

    @Override
    public void initPrefs() {
        int mIconMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_settings_icon", "0"));
        mIconModePreference = findPreference("prefs_key_settings_icon");
        mIconModeValue = findPreference("prefs_key_settings_icon_mode");
        mLanguage = findPreference("prefs_key_settings_app_language");
        mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
        mFloatBottomPreference = findPreference("prefs_key_settings_float_nav");
        mLogLevel = findPreference("prefs_key_log_level");

        setIconMode(mIconMode);
        mIconModePreference.setOnPreferenceChangeListener(this);
        String language = LanguageHelper.getLanguage(requireContext());
        int value = LanguageHelper.resultIndex(LanguageHelper.APP_LANGUAGES, language);
        mLanguage.setValueIndex(value);

        mLanguage.setOnPreferenceChangeListener((preference, o) -> {
            LanguageHelper.setIndexLanguage(getActivity(), Integer.parseInt((String) o), true);
            return true;
        });

        // 根据构建类型设置日志等级选项
        setupLogLevelPreference();

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

        mFloatBottomPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            //getSwitchManager().setFloatingStyle((Boolean) newValue);
            Settings.Global.putBoolean(getContext().getContentResolver(), "settings_float_nav", (Boolean) newValue);
            return true;
        });

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
            DialogHelper.showDialog(getActivity(), com.sevtinge.hyperceiler.core.R.string.reset_title, com.sevtinge.hyperceiler.core.R.string.reset_desc, (dialog, which) -> {
                PrefsUtils.mSharedPreferences.edit().clear().apply();
                Toast.makeText(getActivity(), com.sevtinge.hyperceiler.core.R.string.reset_okay, Toast.LENGTH_LONG).show();
            });
            return true;
        });
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mIconModePreference) {
            setIconMode(Integer.parseInt((String) o));
        }
        return true;
    }

    public SwitchManager getSwitchManager() {
        if (getActivity() instanceof HomePageActivity) {
            return ((HomePageActivity) getActivity()).getSwitchManager();
        }
        return null;
    }

    private void setLogLevel(int level) {
        LogManager.setLogLevel(level, requireContext().getApplicationInfo().dataDir);
    }

    /**
     * 根据构建类型设置日志等级选项
     * Release: Disable, Error
     * Beta: Error, Debug
     * Canary: Info, Debug
     * Debug: Disable, Error, Warn, Info, Debug
     */
    private void setupLogLevelPreference() {
        CharSequence[] entries;
        CharSequence[] entryValues;
        String defaultValue;

        if (isRelease()) {
            entries = new CharSequence[]{"Disable", "Error"};
            entryValues = new CharSequence[]{"0", "1"};
            defaultValue = "1";
        } else if (isBeta()) {
            entries = new CharSequence[]{"Error", "Debug"};
            entryValues = new CharSequence[]{"1", "4"};
            defaultValue = "4";
        } else if (isCanary()) {
            entries = new CharSequence[]{"Info", "Debug"};
            entryValues = new CharSequence[]{"3", "4"};
            defaultValue = "3";
        } else {
            // Debug 构建类型：全部选项
            entries = new CharSequence[]{"Disable", "Error", "Warn", "Info", "Debug"};
            entryValues = new CharSequence[]{"0", "1", "2", "3", "4"};
            defaultValue = "4";
        }

        mLogLevel.setEntries(entries);
        mLogLevel.setEntryValues(entryValues);
        mLogLevel.setDefaultValue(defaultValue);

        // 如果当前值不在允许的范围内，重置为默认值
        String currentValue = mLogLevel.getValue();
        boolean isValidValue = false;
        for (CharSequence value : entryValues) {
            if (value.toString().equals(currentValue)) {
                isValidValue = true;
                break;
            }
        }
        if (!isValidValue || currentValue == null) {
            mLogLevel.setValue(defaultValue);
        }

        mLogLevel.setOnPreferenceChangeListener((preference, o) -> {
            setLogLevel(Integer.parseInt((String) o));
            return true;
        });
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

    @Override
    public void onContentInsetChanged(Rect rect) {
        super.onContentInsetChanged(rect);
        if (getListView() != null) {
            ViewUtils.RelativePadding relativePadding = new ViewUtils.RelativePadding(getListView());
            boolean isLayoutRtl = ViewUtils.isLayoutRtl(getListView());
            relativePadding.start += isLayoutRtl ? rect.right : rect.left;
            relativePadding.end += isLayoutRtl ? rect.left : rect.right;
            relativePadding.bottom = rect.top;
            relativePadding.applyToView(getListView());
        }
    }
}
