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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.page;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.common.utils.LanguageHelper;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.LauncherActivity;
import com.sevtinge.hyperceiler.ui.ContentFragment.IFragmentChange;
import com.sevtinge.hyperceiler.hook.utils.BackupUtils;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellInit;
import com.sevtinge.hyperceiler.widget.ListContainerView;

import fan.appcompat.app.ActionBar;
import fan.appcompat.app.AppCompatActivity;
import fan.internal.utils.ViewUtils;
import fan.navigator.NavigatorFragmentListener;
import fan.preference.DropDownPreference;
import fan.springback.view.SpringBackLayout;

public class SettingsPageFragment extends DashboardFragment
        implements Preference.OnPreferenceChangeListener, NavigatorFragmentListener, IFragmentChange {

    NestedScrollView mScrollView;
    ListContainerView mContainerView;

    DropDownPreference mIconModePreference;
    DropDownPreference mIconModeValue;
    SwitchPreference mHideAppIcon;

    DropDownPreference mLogLevel;
    DropDownPreference mLanguage;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContainerView = new ListContainerView(requireContext());
        mScrollView = mContainerView.getNestedScrollView();
        mContainerView.addPrefsContainer(super.onCreateView(inflater, container, savedInstanceState));
        setOverlayMode();
        registerCoordinateScrollView(mContainerView.getNestedHeader());
        return mContainerView;
    }

    @Override
    public void onContentInsetChanged(Rect rect) {
        super.onContentInsetChanged(rect);
        if (mScrollView != null) {
            mScrollView.setClipToPadding(false);
            ViewUtils.RelativePadding relativePadding = new ViewUtils.RelativePadding(mScrollView);
            boolean isLayoutRtl = ViewUtils.isLayoutRtl(mScrollView);
            relativePadding.start += isLayoutRtl ? rect.right : rect.left;
            relativePadding.end += isLayoutRtl ? rect.left : rect.right;
            relativePadding.bottom = rect.bottom;
            relativePadding.applyToView(mScrollView);
            setRecyclerViewPadding();
        }
    }

    private void setRecyclerViewPadding() {
        RecyclerView listView = getListView();
        if (listView == null) return;
        View view = (View) listView.getParent();
        if (view instanceof SpringBackLayout) {
            view.setEnabled(false);
            listView.post(() -> listView.setPaddingRelative(
                listView.getPaddingStart(),
                0,
                listView.getPaddingEnd(),
                0)
            );
        }
    }

    @Override
    public int getPreferenceScreenResId() {
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
        String language = LanguageHelper.getLanguage(requireContext());
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
    public void onEnter(ActionBar actionBar) {

    }

    @Override
    public void onLeave(ActionBar actionBar) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterCoordinateScrollView(mContainerView.getNestedHeader());
    }
}
