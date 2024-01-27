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
package com.sevtinge.hyperceiler.ui.fragment.home;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.KillAppUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class HomeOtherSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    SwitchPreference mFixAndroidRS;
    SwitchPreference mEnableMoreSettings;
    SwitchPreference mLockApp;
    SwitchPreference mLockAppSc;
    SwitchPreference mLockAppScreen;
    Handler handler;
    ExecutorService executorService;

    @Override
    public int getContentResId() {
        return R.xml.home_other;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.mihome),
            "com.miui.home"
        );
    }

    @Override
    public void initPrefs() {
        mFixAndroidRS = findPreference("prefs_key_home_other_fix_android_r_s");
        mEnableMoreSettings = findPreference("prefs_key_home_other_mi_pad_enable_more_setting");
        mLockApp = findPreference("prefs_key_home_other_lock_app");
        mLockAppSc = findPreference("prefs_key_home_other_lock_app_sc");
        mLockAppScreen = findPreference("prefs_key_home_other_lock_app_screen");
        mFixAndroidRS.setVisible(!isMoreAndroidVersion(33));
        mEnableMoreSettings.setVisible(isPad());
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        handler = new Handler();
        mLockApp.setOnPreferenceChangeListener(this);
        mLockAppSc.setOnPreferenceChangeListener(this);
        mLockAppScreen.setOnPreferenceChangeListener(this);
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
            case "prefs_key_home_other_lock_app" -> {
                initApp(executorService, () -> {
                    KillAppUtils.pidKill(new String[]{"com.miui.home", "com.android.systemui"});
                });
            }
            case "prefs_key_home_other_lock_app_sc" -> {
                initApp(executorService, () -> KillAppUtils.pKill("com.miui.securitycenter"));
            }
            case "prefs_key_home_other_lock_app_screen" -> {
                initApp(executorService, () -> KillAppUtils.pKill("com.android.systemui"));
            }
        }
        return true;
    }
}
