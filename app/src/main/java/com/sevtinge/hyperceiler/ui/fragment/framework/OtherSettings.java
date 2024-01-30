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
import android.os.Handler;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.SubPickerActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.KillAppUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class OtherSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    Preference mCleanShareApps;
    Preference mCleanOpenApps;
    Preference mClipboardWhitelistApps;
    SwitchPreference mEntry;
    SwitchPreference mAppLinkVerify;
    SwitchPreference mUseOriginalAnim;
    SwitchPreference mVerifyDisable;
    SwitchPreference mDisableDeviceLog; // 关闭访问设备日志确认
    SwitchPreference mLockApp;
    SwitchPreference mLockAppSc;
    SwitchPreference mLockAppScreen;
    Handler handler;
    ExecutorService executorService;

    @Override
    public int getContentResId() {
        return R.xml.framework_other;
    }

    @Override
    public void initPrefs() {
        mCleanShareApps = findPreference("prefs_key_system_framework_clean_share_apps");
        mCleanOpenApps = findPreference("prefs_key_system_framework_clean_open_apps");
        mClipboardWhitelistApps =findPreference("prefs_key_system_framework_clipboard_whitelist_apps");
        mAppLinkVerify = findPreference("prefs_key_system_framework_disable_app_link_verify");
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

        mAppLinkVerify.setVisible(!isAndroidVersion(30));
        // mVerifyDisable.setVisible(isMoreHyperOSVersion(1f));
        mEntry.setVisible(isMoreHyperOSVersion(1f));
        mUseOriginalAnim.setVisible(!isAndroidVersion(33));
        mDisableDeviceLog.setVisible(isMoreAndroidVersion(33));

        mCleanShareApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("is_app_selector", false);
            intent.putExtra("need_mode", 2);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });

        mCleanOpenApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("is_app_selector", false);
            intent.putExtra("need_mode", 2);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });

        mClipboardWhitelistApps.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("is_app_selector", true);
            intent.putExtra("need_mode", 2);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        handler = new Handler();
    }

    public void initApp(ExecutorService executorService, Runnable runnable) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                });
            }
        });
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        switch (preference.getKey()) {
            case "prefs_key_system_framework_guided_access" -> {
                initApp(executorService, () -> {
                    KillAppUtils.pidKill(new String[]{"com.miui.home", "com.android.systemui"});
                });
            }
            case "prefs_key_system_framework_guided_access_sc" -> {
                initApp(executorService, () -> KillAppUtils.pKill("com.miui.securitycenter"));
            }
            case "prefs_key_system_framework_guided_access_screen" -> {
                initApp(executorService, () -> KillAppUtils.pKill("com.android.systemui"));
            }
        }
        return true;
    }
}
