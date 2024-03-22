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
package com.sevtinge.hyperceiler.ui.fragment.framework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.ui.SubPickerActivity;
import com.sevtinge.hyperceiler.ui.fragment.PhoneFragment;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.ui.fragment.sub.AppPicker;
import com.sevtinge.hyperceiler.utils.KillApp;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;

import java.util.concurrent.ExecutorService;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class OtherSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    Preference mCleanShareApps;
    Preference mCleanOpenApps;
    Preference mClipboardWhitelistApps;
    SwitchPreference mEntry;
    SwitchPreference mUseOriginalAnim;
    SwitchPreference mVerifyDisable;
    SwitchPreference mDisableDeviceLog; // 关闭访问设备日志确认
    SwitchPreference mLockApp;
    SwitchPreference mLockAppSc;
    SwitchPreference mLockAppScreen;
    RecommendPreference mRecommend;
    Handler handler;

    @Override
    public int getContentResId() {
        return R.xml.framework_other;
    }

    @Override
    public void initPrefs() {
        mCleanShareApps = findPreference("prefs_key_system_framework_clean_share_apps");
        mCleanOpenApps = findPreference("prefs_key_system_framework_clean_open_apps");
        mClipboardWhitelistApps = findPreference("prefs_key_system_framework_clipboard_whitelist_apps");
        mVerifyDisable = findPreference("prefs_key_system_framework_disable_verify_can_ve_disabled");
        mUseOriginalAnim = findPreference("prefs_key_system_framework_other_use_original_animation");
        mEntry = findPreference("prefs_key_system_framework_hook_entry");
        mLockApp = findPreference("prefs_key_system_framework_guided_access");
        mLockAppSc = findPreference("prefs_key_system_framework_guided_access_sc");
        mLockAppScreen = findPreference("prefs_key_system_framework_guided_access_screen");

        mLockApp.setOnPreferenceChangeListener(this);
        mLockAppSc.setOnPreferenceChangeListener(this);
        mLockAppScreen.setOnPreferenceChangeListener(this);

        mDisableDeviceLog = findPreference("prefs_key_various_disable_access_device_logs");

        // mVerifyDisable.setVisible(isMoreHyperOSVersion(1f));
        mEntry.setVisible(isMoreHyperOSVersion(1f));
        mUseOriginalAnim.setVisible(!isAndroidVersion(33));
        mDisableDeviceLog.setVisible(isMoreAndroidVersion(33));

        mCleanShareApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("mode", AppPicker.APP_OPEN_MODE);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });

        mCleanOpenApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("mode", AppPicker.APP_OPEN_MODE);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });

        mClipboardWhitelistApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("mode", AppPicker.LAUNCHER_MODE);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });
        handler = new Handler(requireContext().getMainLooper());

        Bundle args1 = new Bundle();
        mRecommend = new RecommendPreference(getContext());
        getPreferenceScreen().addPreference(mRecommend);

        args1.putString(":settings:fragment_args_key", "prefs_key_system_ui_display_use_aosp_screenshot_enable");
        mRecommend.addRecommendView(getString(R.string.system_ui_display_use_aosp_screenshot),
                null,
                PhoneFragment.class,
                args1,
                R.string.system_framework_display_title
        );
    }

    public void initApp(ExecutorService executorService, Runnable runnable) {
        executorService.submit(() -> {
            handler.post(runnable);
        });
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        ExecutorService executorService = ThreadPoolManager.getInstance();
        switch (preference.getKey()) {
            case "prefs_key_system_framework_guided_access" -> {
                initApp(executorService, () -> {
                    KillApp.killApps("com.miui.home", "com.android.systemui");
                });
            }
            case "prefs_key_system_framework_guided_access_sc" -> {
                initApp(executorService, () -> KillApp.killApps("com.miui.securitycenter"));
            }
            case "prefs_key_system_framework_guided_access_screen" -> {
                initApp(executorService, () -> KillApp.killApps("com.android.systemui"));
            }
        }
        return true;
    }
}
