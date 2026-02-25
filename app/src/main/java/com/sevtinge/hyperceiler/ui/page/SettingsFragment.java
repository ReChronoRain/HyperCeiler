package com.sevtinge.hyperceiler.ui.page;

import static com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isBeta;
import static com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isCanary;
import static com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi.isRelease;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.fan.common.base.BasePreferenceFragment;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.common.utils.LanguageHelper;
import com.sevtinge.hyperceiler.libhook.utils.api.BackupUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.LogManager;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.oldui.ui.LauncherActivity;
import com.sevtinge.hyperceiler.ui.HomePageActivity;
import com.sevtinge.hyperceiler.ui.SwitchManager;

import fan.appcompat.app.AlertDialog;
import fan.appcompat.app.AppCompatActivity;
import fan.internal.utils.ViewUtils;
import fan.preference.DropDownPreference;
import fan.provider.Settings;

public class SettingsFragment extends BasePreferenceFragment
    implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    SwitchPreference mFloatBottomPreference;
    DropDownPreference mIconModePreference;
    DropDownPreference mIconModeValue;
    SwitchPreference mHideAppIcon;

    DropDownPreference mLogLevel;
    DropDownPreference mLanguage;

    // 记录当前操作类型：0-备份，1-恢复
    private int currentAction = -1;
    private static final int ACTION_BACKUP = 0;
    private static final int ACTION_RESTORE = 1;

    private final ActivityResultLauncher<Intent> mBackupLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                processBackup(result.getData().getData());
            }
        }
    );

    // 2. 注册恢复启动器
    private final ActivityResultLauncher<Intent> mRestoreLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                processRestore(result.getData().getData());
            }
        }
    );

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

        Preference backPreference = findPreference("prefs_key_back");
        Preference restPreference = findPreference("prefs_key_rest");
        Preference resetPreference = findPreference("prefs_key_reset");

        backPreference.setOnPreferenceClickListener(this);
        restPreference.setOnPreferenceClickListener(this);
        resetPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case "prefs_key_back" -> mBackupLauncher.launch(BackupUtils.getCreateDocumentIntent());
            case "prefs_key_rest" -> mRestoreLauncher.launch(BackupUtils.getOpenDocumentIntent());
            case "prefs_key_reset" -> {
                PrefsBridge.clearAll();
                Toast.makeText(getActivity(), com.sevtinge.hyperceiler.core.R.string.reset_okay, Toast.LENGTH_LONG).show();
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mIconModePreference) {
            setIconMode(Integer.parseInt((String) o));
        }
        return true;
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

    private void processBackup(Uri uri) {
        try {
            BackupUtils.handleCreateDocument(requireContext(), uri);
            showDialog(getString(com.sevtinge.hyperceiler.core.R.string.backup_success), null);
        } catch (Exception e) {
            showDialog(getString(com.sevtinge.hyperceiler.core.R.string.backup_failed), e.getMessage());
        }
    }

    private void processRestore(Uri uri) {
        try {
            BackupUtils.handleReadDocument(requireContext(), uri);
            showDialog(getString(com.sevtinge.hyperceiler.core.R.string.rest_success), "请重启应用以使配置生效。");
        } catch (Exception e) {
            showDialog(getString(com.sevtinge.hyperceiler.core.R.string.rest_failed), e.getMessage());
        }
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }
}
