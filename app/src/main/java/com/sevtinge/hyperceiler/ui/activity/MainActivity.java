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
package com.sevtinge.hyperceiler.ui.activity;

import static com.sevtinge.hyperceiler.utils.Helpers.isModuleActive;
import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.log.LogManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.utils.log.LogManager.LOGGER_CHECKER_ERR_CODE;
import static com.sevtinge.hyperceiler.utils.log.LogManager.isLoggerAlive;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.callback.IResult;
import com.sevtinge.hyperceiler.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.safe.CrashData;
import com.sevtinge.hyperceiler.ui.activity.base.HyperCeilerTabActivity;
import com.sevtinge.hyperceiler.utils.BackupUtils;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.LanguageHelper;
import com.sevtinge.hyperceiler.utils.PropUtils;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.utils.search.SearchHelper;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fan.appcompat.app.AlertDialog;

public class MainActivity extends HyperCeilerTabActivity implements IResult {

    private Handler handler;
    private Context context;

    private ArrayList<String> appCrash = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        IS_LOGGER_ALIVE = isLoggerAlive();
        SharedPreferences mPrefs = PrefsUtils.mSharedPreferences;
        int count = Integer.parseInt(mPrefs.getString("prefs_key_settings_app_language", "-1"));
        if (count != -1) {
            LanguageHelper.setIndexLanguage(this, count, false);
        }
        handler = new Handler(this.getMainLooper());
        context = this;
        int def = Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_log_level", "3"));
        super.onCreate(savedInstanceState);
        new Thread(() -> SearchHelper.getAllMods(MainActivity.this, savedInstanceState != null)).start();
        Helpers.checkXposedActivateState(this);
        if (!IS_LOGGER_ALIVE && isModuleActive && BuildConfig.BUILD_TYPE != "release" && !mPrefs.getBoolean("prefs_key_development_close_log_alert_dialog", false)) {
            handler.post(() -> new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle(getResources().getString(R.string.warn))
                    .setMessage(getResources().getString(R.string.headtip_notice_dead_logger_errcode, LOGGER_CHECKER_ERR_CODE))
                    .setHapticFeedbackEnabled(true)
                    .setPositiveButton(android.R.string.ok, null)
                    .show());
        }
        ShellInit.init(this);
        PropUtils.setProp("persist.hyperceiler.log.level", ProjectApi.isCanary() ? (def != 3 && def != 4 ? 3 : def) : def);
        appCrash = CrashData.toPkgList();
        handler.postDelayed(() -> {
            if (haveCrashReport()) {
                Map<String, String> appNameMap = new HashMap<>();
                appNameMap.put("com.android.systemui", getString(R.string.system_ui));
                appNameMap.put("com.android.settings", getString(R.string.system_settings));
                appNameMap.put("com.miui.home", getString(R.string.mihome));
                appNameMap.put("com.hchen.demo", getString(R.string.demo));
                if (isMoreHyperOSVersion(1f)) {
                    appNameMap.put("com.miui.securitycenter", getString(R.string.security_center_hyperos));
                } else if (isPad()) {
                    appNameMap.put("com.miui.securitycenter", getString(R.string.security_center_pad));
                } else {
                    appNameMap.put("com.miui.securitycenter", getString(R.string.security_center));
                }
                ArrayList<String> appList = new ArrayList<>();
                for (String element : appCrash) {
                    if (appNameMap.containsKey(element)) appList.add(appNameMap.get(element) + " (" + element + ")");
                }
                String appName = appList.toString();
                String msg = getString(R.string.safe_mode_later_desc, " " + appName + " ");
                msg = msg.replace("  ", " ");
                msg = msg.replace("， ", "，");
                msg = msg.replace("、 ", "、");
                msg = msg.replace("[", "");
                msg = msg.replace("]", "");
                msg = msg.replaceAll("^\\s+|\\s+$", "");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.safe_mode_later_title)
                        .setMessage(msg)
                        .setHapticFeedbackEnabled(true)
                        .setCancelable(false)
                        .setPositiveButton(R.string.safe_mode_cancel, (dialog, which) -> {
                            ShellInit.getShell().run("setprop persist.hyperceiler.crash.report \"\"").sync();
                            PrefsUtils.mSharedPreferences.edit().remove("prefs_key_system_ui_safe_mode_enable").apply();
                            PrefsUtils.mSharedPreferences.edit().remove("prefs_key_home_safe_mode_enable").apply();
                            PrefsUtils.mSharedPreferences.edit().remove("prefs_key_system_settings_safe_mode_enable").apply();
                            PrefsUtils.mSharedPreferences.edit().remove("prefs_key_security_center_safe_mode_enable").apply();
                            PrefsUtils.mSharedPreferences.edit().remove("prefs_key_demo_safe_mode_enable").apply();
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.safe_mode_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }, 600);
    }

    @Override
    public void error(String reason) {
        handler.post(() -> new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(getResources().getString(R.string.tip))
                .setMessage(getResources().getString(R.string.root))
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, null)
                .show());
    }


    private boolean haveCrashReport() {
        return !appCrash.isEmpty();
    }

    @Override
    public void onDestroy() {
        ShellInit.destroy();
        ThreadPoolManager.shutdown();
        PreferenceHeader.mUninstallApp.clear();
        PreferenceHeader.mDisableOrHiddenApp.clear();
        super.onDestroy();
    }

    public void test() {
        /*boolean ls = shellExec.append("ls").sync().isResult();
        AndroidLogUtils.LogI(ITAG.TAG, "ls: " + ls);
        AndroidLogUtils.LogI(ITAG.TAG, shellExec.getOutPut().toString() + shellExec.getError().toString());
        boolean f = shellExec.append("for i in $(seq 1 500); do echo $i; done").isResult();
        AndroidLogUtils.LogI(ITAG.TAG, "for: " + f);
        AndroidLogUtils.LogI(ITAG.TAG, shellExec.getOutPut().toString());
        boolean k = shellExec.append("for i in $(seq 1 500); do echo $i; done").sync().isResult();
        AndroidLogUtils.LogI(ITAG.TAG, "fork: " + k);
        AndroidLogUtils.LogI(ITAG.TAG, shellExec.getOutPut().toString());*/
    }

    private void requestCta() {
        /*if (!CtaUtils.isCtaEnabled(this)) {
            CtaUtils.showCtaDialog(this, REQUEST_CODE);
        }*/
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        requestCta();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            switch (requestCode) {
                case BackupUtils.CREATE_DOCUMENT_CODE -> {
                    BackupUtils.handleCreateDocument(this, data.getData());
                    alert.setTitle(R.string.backup_success);
                }
                case BackupUtils.OPEN_DOCUMENT_CODE -> {
                    BackupUtils.handleReadDocument(this, data.getData());
                    alert.setTitle(R.string.rest_success);
                }
                default -> {
                    return;
                }
            }
            alert.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            });
            alert.show();
        } catch (Exception e) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            switch (requestCode) {
                case BackupUtils.CREATE_DOCUMENT_CODE -> alert.setTitle(R.string.backup_failed);
                case BackupUtils.OPEN_DOCUMENT_CODE -> alert.setTitle(R.string.rest_failed);
            }
            alert.setMessage(e.toString());
            alert.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            });
            alert.show();
        }
    }

}
