/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.settings;

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
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.base.BasePreferenceFragment;
import com.sevtinge.hyperceiler.common.log.LogLevelManager;
import com.sevtinge.hyperceiler.common.log.LogStatusManager;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.api.BackupUtils;
import com.sevtinge.hyperceiler.ui.LauncherActivity;
import com.sevtinge.hyperceiler.utils.LanguageHelper;

import fan.appcompat.app.AlertDialog;
import fan.internal.utils.ViewUtils;
import fan.preference.DropDownPreference;
import fan.provider.Settings;
import fan.provision.OobeUtils;

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
        return R.xml.prefs_settings;
    }

    @Override
    public void initPrefs() {
        int mIconMode = PrefsBridge.getStringAsInt("prefs_key_settings_icon", 0);
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
                PrefsBridge.clearAllByApp();
                OobeUtils.resetOobeState(requireContext());
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
        LogStatusManager.setLogLevel(level, requireContext().getApplicationInfo().dataDir);
    }

    /**
     * 底层日志等级统一为三档：
     * 0: 禁用日志输出
     * 1: 一般日志（error + crash）
     * 2: 详细日志（全部等级）
     * 设置页按构建类型裁剪展示：
     * release -> 0 / 1
     * others  -> 1 / 2
     */
    private void setupLogLevelPreference() {
        boolean isRelease = ProjectApi.isRelease();
        CharSequence[] entries = isRelease
            ? new CharSequence[]{
                getString(R.string.log_level_none),
                getString(R.string.log_level_general)
            }
            : new CharSequence[]{
                getString(R.string.log_level_general),
                getString(R.string.log_level_detailed)
            };
        CharSequence[] entryValues = isRelease
            ? new CharSequence[]{"0", "1"}
            : new CharSequence[]{"1", "2"};
        String defaultValue = String.valueOf(LogLevelManager.getDefaultLogLevel());

        mLogLevel.setEntries(entries);
        mLogLevel.setEntryValues(entryValues);
        mLogLevel.setDefaultValue(defaultValue);

        // 如果当前值不在允许的范围内，重置为默认值
        String currentValue = mLogLevel.getValue();
        if (currentValue != null) {
            currentValue = String.valueOf(LogLevelManager.getEffectiveLogLevel(Integer.parseInt(currentValue)));
        }
        boolean isValidValue = false;
        for (CharSequence value : entryValues) {
            if (value.toString().equals(currentValue)) {
                isValidValue = true;
                break;
            }
        }
        if (!isValidValue || currentValue == null) {
            mLogLevel.setValue(defaultValue);
        } else {
            mLogLevel.setValue(currentValue);
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
