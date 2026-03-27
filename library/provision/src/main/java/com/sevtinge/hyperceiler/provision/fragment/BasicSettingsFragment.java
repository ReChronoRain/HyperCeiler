/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.provision.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.common.utils.AppLanguageHelper;
import com.sevtinge.hyperceiler.common.utils.AppSettingsStore;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.common.utils.PrefsConfigurator;
import com.sevtinge.hyperceiler.provision.R;

import fan.preference.DropDownPreference;
import fan.preference.PreferenceFragment;

public class BasicSettingsFragment extends PreferenceFragment {

    private static final String SCOPE_PICKER_CLASS_NAME = "com.sevtinge.hyperceiler.sub.ScopePickerActivity";
    private static final String SCOPE_PICKER_EXTRA_INITIALIZATION_MODE = "initialization_mode";
    private static final String LAUNCHER_ACTIVITY_CLASS_NAME = "com.sevtinge.hyperceiler.ui.LauncherActivity";
    private static final String PREF_APP_LANGUAGE = "prefs_key_settings_app_language";
    private static final String PREF_OOBE_LANGUAGE_SYNCED = "prefs_key_oobe_language_synced";

    private boolean mIsScrolledBottom = false;

    SwitchPreference mHideAppIcon;
    SwitchPreference mScopeSyncPreference;
    Preference mScopePreference;
    DropDownPreference mLanguagePreference;
    DropDownPreference mIconModePreference;
    DropDownPreference mIconModeValue;

    private RecyclerView mRecyclerView;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        PrefsConfigurator.setup(this);
        setPreferencesFromResource(R.xml.provision_basic_settings, rootKey);

        mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
        mScopeSyncPreference = findPreference("prefs_key_settings_scope_sync");
        mScopePreference = findPreference("prefs_key_settings_scope");
        mLanguagePreference = findPreference(PREF_APP_LANGUAGE);
        mIconModePreference = findPreference("prefs_key_settings_icon");
        mIconModeValue = findPreference("prefs_key_settings_icon_mode");

        if (mHideAppIcon != null) {
            mHideAppIcon.setPersistent(false);
        }
        if (mScopeSyncPreference != null) {
            mScopeSyncPreference.setPersistent(false);
        }
        if (mLanguagePreference != null) {
            mLanguagePreference.setPersistent(false);
        }
        if (mIconModePreference != null) {
            mIconModePreference.setPersistent(false);
        }
        if (mIconModeValue != null) {
            mIconModeValue.setPersistent(false);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        mRecyclerView = getListView();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!mRecyclerView.canScrollVertically(1)) {
                    mIsScrolledBottom = true;
                    adjustNextView();
                }
            }
        });

        ensureOobeLanguageInitialized();

        int mIconMode = AppSettingsStore.getIconIndex(requireContext());
        int languageIndex = AppSettingsStore.getAppLanguageIndex(requireContext());
        int iconModeValue = AppSettingsStore.getIconModeIndex(requireContext());
        boolean hideAppIconEnabled = AppSettingsStore.isHideAppIconEnabled(requireContext());
        boolean scopeSyncEnabled = AppSettingsStore.isScopeSyncEnabled(requireContext());
        mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
        mScopeSyncPreference = findPreference("prefs_key_settings_scope_sync");
        mScopePreference = findPreference("prefs_key_settings_scope");
        mLanguagePreference = findPreference(PREF_APP_LANGUAGE);
        mIconModePreference = findPreference("prefs_key_settings_icon");
        mIconModeValue = findPreference("prefs_key_settings_icon_mode");

        if (mHideAppIcon != null) {
            mHideAppIcon.setChecked(hideAppIconEnabled);
        }
        if (mScopeSyncPreference != null) {
            mScopeSyncPreference.setChecked(scopeSyncEnabled);
        }
        if (mLanguagePreference != null) {
            mLanguagePreference.setValueIndex(languageIndex);
        }
        if (mIconModePreference != null) {
            mIconModePreference.setValueIndex(mIconMode);
        }
        if (mIconModeValue != null) {
            mIconModeValue.setValueIndex(iconModeValue);
        }

        setIconMode(mIconMode);
        applyLauncherIconState(hideAppIconEnabled);

        mHideAppIcon.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            AppSettingsStore.setHideAppIconEnabled(requireContext(), enabled);
            applyLauncherIconState(enabled);
            return true;
        });
        mScopeSyncPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            AppSettingsStore.setScopeSyncEnabled(requireContext(), enabled);
            if (enabled) {
                syncHeaderPreferencesToCurrentScope();
            }
            return true;
        });
        mIconModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = Integer.parseInt((String) newValue);
            AppSettingsStore.setIconIndex(requireContext(), index);
            setIconMode(index);
            return true;
        });
        mIconModeValue.setOnPreferenceChangeListener((preference, newValue) -> {
            AppSettingsStore.setIconModeIndex(requireContext(), Integer.parseInt((String) newValue));
            return true;
        });
        mLanguagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = Integer.parseInt((String) newValue);
            AppSettingsStore.setAppLanguageIndex(requireContext(), index);
            AppLanguageHelper.setIndexLanguage(requireActivity(), index, true);
            return true;
        });

        mScopePreference.setOnPreferenceClickListener(preference -> {
            launchScopePickerForInitialization();
            return true;
        });
    }

    public void adjustNextView() {
        /*if (mNextView != null && mNextView instanceof TextView) {
            if (mIsScrolledBottom) {
                ((TextView) mNextView).setText(R.string.next);
            } else {
                ((TextView) mNextView).setText(R.string.more);
            }
        }*/
    }

    private void setIconMode(int mode) {
        mIconModeValue.setVisible(mode != 0);
    }

    private void applyLauncherIconState(boolean enabled) {
        PackageManager packageManager = requireActivity().getPackageManager();
        int componentEnabledState = enabled
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        try {
            packageManager.setComponentEnabledSetting(
                new ComponentName(requireContext(), LAUNCHER_ACTIVITY_CLASS_NAME),
                componentEnabledState,
                PackageManager.DONT_KILL_APP
            );
        } catch (Exception ignored) {
        }
    }

    private void launchScopePickerForInitialization() {
        try {
            Intent intent = new Intent();
            intent.setClassName(requireContext(), SCOPE_PICKER_CLASS_NAME);
            intent.putExtra(SCOPE_PICKER_EXTRA_INITIALIZATION_MODE, true);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.provision_scope_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void syncHeaderPreferencesToCurrentScope() {
        try {
            Class<?> headerManager = Class.forName("com.sevtinge.hyperceiler.home.utils.HeaderManager");
            headerManager.getMethod("syncHeaderPreferencesToCurrentScope", android.content.Context.class)
                .invoke(null, requireContext());
        } catch (Exception ignored) {
        }
    }

    private void ensureOobeLanguageInitialized() {
        if (PrefsBridge.getBoolean(PREF_OOBE_LANGUAGE_SYNCED, false)) {
            return;
        }

        int deviceLanguageIndex = resolveDeviceLanguageIndex();
        AppSettingsStore.setAppLanguageIndex(requireContext(), deviceLanguageIndex);
        PrefsBridge.putByApp(PREF_OOBE_LANGUAGE_SYNCED, true);
        AppLanguageHelper.setIndexLanguage(requireActivity(), deviceLanguageIndex, false);
    }

    private int resolveDeviceLanguageIndex() {
        String deviceLanguage = AppLanguageHelper.getLanguage(requireContext());
        int exactIndex = AppLanguageHelper.resultIndex(AppLanguageHelper.APP_LANGUAGES, deviceLanguage);
        if (deviceLanguage.equals(AppLanguageHelper.APP_LANGUAGES[exactIndex])) {
            return exactIndex;
        }

        int splitIndex = deviceLanguage.indexOf('_');
        String languagePart = splitIndex > 0 ? deviceLanguage.substring(0, splitIndex) : deviceLanguage;
        for (int i = 0; i < AppLanguageHelper.APP_LANGUAGES.length; i++) {
            String candidate = AppLanguageHelper.APP_LANGUAGES[i];
            if (candidate.equals(languagePart) || candidate.startsWith(languagePart + "_")) {
                return i;
            }
        }
        return 0;
    }

}
