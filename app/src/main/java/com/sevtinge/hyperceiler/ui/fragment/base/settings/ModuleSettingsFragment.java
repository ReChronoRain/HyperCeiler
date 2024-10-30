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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.fragment.base.settings;

import static com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.dp2px;
import static com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.sp2px;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.LauncherActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.BackupUtils;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.LanguageHelper;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import fan.appcompat.app.AppCompatActivity;
import fan.preference.DropDownPreference;

public class ModuleSettingsFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    DropDownPreference mIconModePreference;
    DropDownPreference mIconModeValue;
    SwitchPreference mHideAppIcon;

    DropDownPreference mLogLevel;
    DropDownPreference mLanguage;

    @Override
    public int getContentResId() {
        return R.xml.prefs_settings;
    }

    @Override
    public void initPrefs() {
        int mIconMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_settings_icon", "0"));
        mIconModePreference = findPreference("prefs_key_settings_icon");
        mIconModeValue = findPreference("prefs_key_settings_icon_mode");
        mLanguage = findPreference("prefs_key_settings_app_language");
        mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
        mLogLevel = findPreference("prefs_key_log_level");

        setIconMode(mIconMode);
        mIconModePreference.setOnPreferenceChangeListener(this);
        String language = LanguageHelper.getLanguage(getContext());
        int value = LanguageHelper.resultIndex(LanguageHelper.appLanguages, language);
        mLanguage.setValueIndex(value);
        mLanguage.setOnPreferenceChangeListener(
                (preference, o) -> {
                    LanguageHelper.setIndexLanguage(getActivity(), Integer.parseInt((String) o), true);
                    return true;
                }
        );

        switch (BuildConfig.BUILD_TYPE) {
            case "canary" -> {
                mLogLevel.setDefaultValue(3);
                mLogLevel.setEntries(new CharSequence[]{"Info", "Debug"});
                mLogLevel.setEntryValues(new CharSequence[]{"3", "4"});
                mLogLevel.setOnPreferenceChangeListener(
                        (preference, o) -> {
                            setLogLevel(Integer.parseInt((String) o));
                            return true;
                        }
                );
            }
            /*case "debug" -> {
                mLogLevel.setEnabled(false);
                mLogLevel.setValueIndex(4);
                mLogLevel.setSummary(R.string.disable_detailed_log_more);
            }*/
            default -> mLogLevel.setOnPreferenceChangeListener(
                    (preference, o) -> {
                        setLogLevel(Integer.parseInt((String) o));
                        return true;
                    }
            );
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
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mIconModePreference) {
            setIconMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setLogLevel(int level) {
        ShellInit.getShell().run("setprop persist.hyperceiler.log.level " + level);
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(fan.preference.R.id.recycler_view);
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets inset = Insets.max(insets.getInsets(WindowInsetsCompat.Type.systemBars()),
                        insets.getInsets(WindowInsetsCompat.Type.displayCutout()));
                // 22dp + 2dp + 12sp + 10dp + 18dp + 0.5dp + inset.bottom + 4dp(?)
                v.setPadding(inset.left, 0, inset.right, inset.bottom + dp2px(requireContext(), 56.5F) + sp2px(requireContext(), 12));
                return insets;
            }
        });
    }
}
