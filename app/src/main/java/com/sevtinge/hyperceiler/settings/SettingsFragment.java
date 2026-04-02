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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.base.BasePreferenceFragment;
import com.sevtinge.hyperceiler.common.log.LogLevelManager;
import com.sevtinge.hyperceiler.common.utils.AppSettingsStore;
import com.sevtinge.hyperceiler.common.utils.PermissionUtils;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.home.utils.HeaderManager;
import com.sevtinge.hyperceiler.libhook.utils.api.BackupUtils;
import com.sevtinge.hyperceiler.search.SearchHelper;
import com.sevtinge.hyperceiler.sub.ScopePickerActivity;
import com.sevtinge.hyperceiler.ui.HomePageActivity;
import com.sevtinge.hyperceiler.ui.LauncherActivity;
import com.sevtinge.hyperceiler.ui.SplashActivity;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.LanguageHelper;
import com.sevtinge.hyperceiler.utils.ScopeManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import fan.appcompat.app.AlertDialog;
import fan.internal.utils.ViewUtils;
import fan.preference.DropDownPreference;
import fan.provision.OobeUtils;

public class SettingsFragment extends BasePreferenceFragment
    implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    SwitchPreference mFloatBottomPreference;
    DropDownPreference mIconModePreference;
    DropDownPreference mIconModeValue;
    SwitchPreference mHideAppIcon;

    DropDownPreference mLogLevel;
    DropDownPreference mLanguage;
    SwitchPreference mScopeSyncPreference;
    Preference mScopePreference;

    // 记录当前操作类型：0-备份，1-恢复
    private final int currentAction = -1;
    private static final int ACTION_BACKUP = 0;
    private static final int ACTION_RESTORE = 1;
    private static final int REQUEST_GET_INSTALLED_APPS = 1204;
    private Uri mPendingRestoreUri;

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
        int mIconMode = AppSettingsStore.getIconIndex(requireContext());
        int iconModeStyle = AppSettingsStore.getIconModeIndex(requireContext());
        int languageIndex = LanguageHelper.getCurrentLanguageIndex(requireContext());
        boolean hideAppIconEnabled = AppSettingsStore.isHideAppIconEnabled(requireContext());
        boolean isFloating = AppSettingsStore.isFloatNavEnabled(requireContext());
        boolean scopeSyncEnabled = AppSettingsStore.isScopeSyncEnabled(requireContext());

        mIconModePreference = findPreference("prefs_key_settings_icon");
        mIconModeValue = findPreference("prefs_key_settings_icon_mode");
        mLanguage = findPreference("prefs_key_settings_app_language");
        mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
        mFloatBottomPreference = findPreference("prefs_key_settings_float_nav");
        mLogLevel = findPreference("prefs_key_log_level_v2");
        mScopeSyncPreference = findPreference("prefs_key_settings_scope_sync");
        mScopePreference = findPreference("prefs_key_settings_scope");

        if (mHideAppIcon != null) {
            mHideAppIcon.setPersistent(false);
        }
        if (mFloatBottomPreference != null) {
            mFloatBottomPreference.setPersistent(false);
        }
        if (mScopeSyncPreference != null) {
            mScopeSyncPreference.setPersistent(false);
        }
        if (mIconModePreference != null) {
            mIconModePreference.setPersistent(false);
        }
        if (mIconModeValue != null) {
            mIconModeValue.setPersistent(false);
        }
        if (mLanguage != null) {
            mLanguage.setPersistent(false);
            mLanguage.setEntries(LanguageHelper.getLanguageEntries(requireContext()));
            mLanguage.setEntryValues(LanguageHelper.getLanguageEntryValues());
        }

        if (mHideAppIcon != null) {
            mHideAppIcon.setChecked(hideAppIconEnabled);
        }
        if (mFloatBottomPreference != null) {
            mFloatBottomPreference.setChecked(isFloating);
        }
        if (mScopeSyncPreference != null) {
            mScopeSyncPreference.setChecked(scopeSyncEnabled);
        }
        if (mIconModePreference != null) {
            mIconModePreference.setValueIndex(mIconMode);
        }
        if (mIconModeValue != null) {
            mIconModeValue.setValueIndex(iconModeStyle);
        }
        if (mLanguage != null) {
            mLanguage.setValueIndex(languageIndex);
        }

        setIconMode(mIconMode);
        mIconModePreference.setOnPreferenceChangeListener(this);

        mLanguage.setOnPreferenceChangeListener((preference, o) -> {
            int index = Integer.parseInt((String) o);
            LanguageHelper.setIndexLanguage(getActivity(), index, false);
            SearchHelper.initIndex(requireContext(), true);
            if (getActivity() instanceof HomePageActivity activity) {
                activity.reloadPagesForLanguageChange();
            }
            return true;
        });

        mIconModeValue.setOnPreferenceChangeListener((preference, newValue) -> {
            AppSettingsStore.setIconModeIndex(requireContext(), Integer.parseInt((String) newValue));
            return true;
        });

        // 根据构建类型设置日志等级选项
        setupLogLevelPreference();

        if (mHideAppIcon != null) {
            mHideAppIcon.setOnPreferenceChangeListener((preference, o) -> {
                boolean enabled = Boolean.TRUE.equals(o);
                AppSettingsStore.setHideAppIconEnabled(requireContext(), enabled);

                PackageManager pm = requireActivity().getPackageManager();
                int mComponentEnabledState;

                if (enabled) {
                    mComponentEnabledState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                } else {
                    mComponentEnabledState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                }

                pm.setComponentEnabledSetting(new ComponentName(requireActivity(), LauncherActivity.class), mComponentEnabledState, PackageManager.DONT_KILL_APP);
                return true;
            });
        }

        mFloatBottomPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            AppSettingsStore.setFloatNavEnabled(requireContext(), enabled);
            return true;
        });

        if (mScopeSyncPreference != null) {
            updateScopePreferenceTitle(scopeSyncEnabled);
            mScopeSyncPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = Boolean.TRUE.equals(newValue);
                AppSettingsStore.setScopeSyncEnabled(requireContext(), enabled);
                if (enabled) {
                    HeaderManager.syncHeaderPreferencesToCurrentScope(requireContext());
                }
                updateScopePreferenceTitle(enabled);
                return true;
            });
        } else {
            updateScopePreferenceTitle(false);
        }

        if (mScopePreference != null) {
            mScopePreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(requireContext(), ScopePickerActivity.class);
                if (HeaderManager.isScopeSyncEnabled()) {
                    intent.putStringArrayListExtra(
                        ScopePickerActivity.EXTRA_EXCLUDED_PACKAGES,
                        new ArrayList<>(HeaderManager.getHomeManagedPackages(requireContext()))
                    );
                }
                startActivity(intent);
                return true;
            });
        }

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
                DialogHelper.showDialog(
                    requireActivity(),
                    getString(com.sevtinge.hyperceiler.core.R.string.reset_title),
                    getString(com.sevtinge.hyperceiler.core.R.string.reset_desc),
                    (dialog, which) -> {
                        PrefsBridge.clearAllByApp();
                        AppSettingsStore.resetGlobalToDefaults(requireContext());
                        OobeUtils.resetOobeState(requireContext());
                        LanguageHelper.clearLanguage(requireContext());
                        SearchHelper.initIndex(requireContext(), true);
                        Toast.makeText(getActivity(), com.sevtinge.hyperceiler.core.R.string.reset_okay, Toast.LENGTH_LONG).show();
                    }
                );
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mIconModePreference) {
            int index = Integer.parseInt((String) o);
            AppSettingsStore.setIconIndex(requireContext(), index);
            setIconMode(index);
        }
        return true;
    }

    /**
     * 日志等级统一为三档：
     * 0: 禁用日志输出
     * 1: 仅输出 error
     * 2: 输出全部日志
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

        String currentValue = mLogLevel.getValue();
        if (currentValue != null) {
            try {
                currentValue = String.valueOf(LogLevelManager.getEffectiveLogLevel(Integer.parseInt(currentValue)));
            } catch (NumberFormatException e) {
                currentValue = defaultValue;
            }
        }
        boolean isValidValue = false;
        if (currentValue != null) {
            for (CharSequence value : entryValues) {
                if (value.toString().equals(currentValue)) {
                    isValidValue = true;
                    break;
                }
            }
        }
        if (!isValidValue) {
            mLogLevel.setValue(defaultValue);
        } else {
            mLogLevel.setValue(currentValue);
        }

        mLogLevel.setOnPreferenceChangeListener((preference, o) -> {
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
            AppSettingsStore.syncGlobalFromPrefs(requireContext());
            if (!HeaderManager.isScopeSyncEnabled()) {
                showDialog(
                    getString(com.sevtinge.hyperceiler.core.R.string.rest_success),
                    getString(com.sevtinge.hyperceiler.core.R.string.rest_success_message),
                    (dialog, which) -> restartApp()
                );
                return;
            }
            if (!PermissionUtils.canReadInstalledApps(requireContext())) {
                mPendingRestoreUri = uri;
                requestPermissions(
                    new String[]{PermissionUtils.PERMISSION_GET_INSTALLED_APPS},
                    REQUEST_GET_INSTALLED_APPS
                );
                return;
            }

            Set<String> currentSelected = ScopeManager.normalizeScopePackages(
                HeaderManager.getCurrentScopeManagedPackages(requireContext())
            );
            Set<String> restoredSelected = ScopeManager.normalizeScopePackages(
                HeaderManager.getStoredScopeManagedPackages(requireContext())
            );
            Set<String> targetSelected = filterInstalledScopePackages(restoredSelected);
            Set<String> skippedPackages = new LinkedHashSet<>(restoredSelected);
            skippedPackages.removeAll(targetSelected);

            Set<String> newlyAddedScopes = new LinkedHashSet<>(targetSelected);
            newlyAddedScopes.removeAll(currentSelected);
            if (!newlyAddedScopes.isEmpty()) {
                showScopeAuthorizationNotice(newlyAddedScopes, () ->
                    applyRestoredScopeDiff(currentSelected, targetSelected, skippedPackages)
                );
            } else {
                applyRestoredScopeDiff(currentSelected, targetSelected, skippedPackages);
            }
        } catch (Exception e) {
            showDialog(getString(com.sevtinge.hyperceiler.core.R.string.rest_failed), e.getMessage());
        }
    }

    private void applyRestoredScopeDiff(
        Set<String> currentSelected,
        Set<String> targetSelected,
        Set<String> skippedPackages
    ) {
        ScopeManager.applyScopeDiffAsync(requireContext(), currentSelected, targetSelected, (success, message) -> {
            if (!isAdded()) {
                return;
            }

            String restoreMessage = getString(com.sevtinge.hyperceiler.core.R.string.rest_success_message);
            if (!skippedPackages.isEmpty()) {
                restoreMessage = restoreMessage + "\n\n" + getString(
                    com.sevtinge.hyperceiler.core.R.string.rest_scope_skip_uninstalled,
                    TextUtils.join("、", skippedPackages)
                );
            }
            if (!success && message != null && !message.isEmpty()) {
                restoreMessage = restoreMessage + "\n\n" + message;
            }
            showDialog(
                getString(com.sevtinge.hyperceiler.core.R.string.rest_success),
                restoreMessage,
                (dialog, which) -> restartApp()
            );
        });
    }

    private Set<String> filterInstalledScopePackages(Set<String> packages) {
        Set<String> installedPackages = new LinkedHashSet<>();
        if (packages == null || packages.isEmpty()) {
            return installedPackages;
        }

        for (String packageName : packages) {
            if (isScopePackageInstalled(packageName)) {
                installedPackages.add(packageName);
            }
        }
        return installedPackages;
    }

    private boolean isScopePackageInstalled(String packageName) {
        if (ScopeManager.isSystemScopePackage(packageName)) {
            return true;
        }

        try {
            requireContext().getPackageManager().getPackageInfo(packageName, PackageManager.MATCH_ALL);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void showScopeAuthorizationNotice(Set<String> newlyAddedScopes, Runnable onConfirmed) {
        String scopeList = TextUtils.join("、", newlyAddedScopes);
        new AlertDialog.Builder(requireContext())
            .setTitle(com.sevtinge.hyperceiler.core.R.string.rest_scope_authorize_title)
            .setMessage(getString(com.sevtinge.hyperceiler.core.R.string.rest_scope_authorize_notice, scopeList))
            .setPositiveButton(android.R.string.ok, (dialog, which) -> onConfirmed.run())
            .show();
    }

    private void showDialog(String title, String message) {
        showDialog(title, message, null);
    }

    private void showDialog(String title, String message, android.content.DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, onClickListener)
            .show();
    }

    private void updateScopePreferenceTitle(boolean scopeSyncEnabled) {
        if (mScopePreference == null) {
            return;
        }
        mScopePreference.setTitle(scopeSyncEnabled
            ? com.sevtinge.hyperceiler.core.R.string.settings_scope_extra
            : com.sevtinge.hyperceiler.core.R.string.settings_scope);
    }

    private void restartApp() {
        Context context = requireContext().getApplicationContext();
        Intent restartIntent = new Intent(context, SplashActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            restartIntent,
            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 200, pendingIntent);
        } else {
            context.startActivity(restartIntent);
        }

        Activity activity = getActivity();
        if (activity != null) {
            activity.finishAffinity();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_GET_INSTALLED_APPS) {
            return;
        }

        Uri pendingRestoreUri = mPendingRestoreUri;
        mPendingRestoreUri = null;
        if (pendingRestoreUri == null) {
            return;
        }

        if (PermissionUtils.canReadInstalledApps(requireContext())
            || PermissionUtils.isInstalledAppsPermissionGranted(permissions, grantResults)) {
            processRestore(pendingRestoreUri);
            return;
        }

        showDialog(
            getString(com.sevtinge.hyperceiler.core.R.string.rest_failed),
            getString(com.sevtinge.hyperceiler.core.R.string.rest_permission)
        );
    }
}
