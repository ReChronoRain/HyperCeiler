package com.sevtinge.hyperceiler.ui.activity;

import static com.sevtinge.hyperceiler.utils.Helpers.isModuleActive;
import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.log.LogManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.utils.log.LogManager.isLoggerAlive;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.callback.IResult;
import com.sevtinge.hyperceiler.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.safe.CrashData;
import com.sevtinge.hyperceiler.ui.activity.base.NaviBaseActivity;
import com.sevtinge.hyperceiler.ui.fragment.main.ContentFragment;
import com.sevtinge.hyperceiler.utils.BackupUtils;
import com.sevtinge.hyperceiler.utils.DialogHelper;
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
import fan.preference.PreferenceFragment;

public class HyperCeilerTabActivity extends NaviBaseActivity
        implements PreferenceFragment.OnPreferenceStartFragmentCallback, IResult {

    private Handler handler;
    private Context context;

    private ArrayList<String> appCrash = new ArrayList<>();

    @Override
    @SuppressLint("StringFormatInvalid")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        IS_LOGGER_ALIVE = isLoggerAlive();

        SharedPreferences mPrefs = PrefsUtils.mSharedPreferences;
        String languageSetting = mPrefs.getString("prefs_key_settings_app_language", "-1");
        if (!"-1".equals(languageSetting)) {
            LanguageHelper.setIndexLanguage(this, Integer.parseInt(languageSetting), false);
        }

        handler = new Handler(this.getMainLooper());
        context = this;

        int logLevel = Integer.parseInt(mPrefs.getString("prefs_key_log_level", "3"));
        super.onCreate(savedInstanceState);
        new Thread(() -> SearchHelper.getAllMods(context, savedInstanceState != null)).start();

        Helpers.checkXposedActivateState(this);

        if (!IS_LOGGER_ALIVE && isModuleActive && BuildConfig.BUILD_TYPE != "release" && !mPrefs.getBoolean("prefs_key_development_close_log_alert_dialog", false)) {
            handler.post(() -> DialogHelper.showLogServiceWarnDialog(context));
        }

        ShellInit.init(this);
        int effectiveLogLevel = ProjectApi.isCanary() ? (logLevel != 3 && logLevel != 4 ? 3 : logLevel) : logLevel;
        PropUtils.setProp("persist.hyperceiler.log.level", effectiveLogLevel);

        appCrash = CrashData.toPkgList();
        handler.postDelayed(() -> {
            if (haveCrashReport()) {
                Map<String, String> appNameMap = createAppNameMap();
                ArrayList<String> appList = getAppListWithCrashReports(appNameMap);
                String appName = String.join(", ", appList);
                String msg = getString(R.string.safe_mode_later_desc, appName);
                msg = cleanUpMessage(msg);
                DialogHelper.showSafeModeDialog(context, msg);
            }
        }, 600);
    }

    private Map<String, String> createAppNameMap() {
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
        return appNameMap;
    }

    private ArrayList<String> getAppListWithCrashReports(Map<String, String> appNameMap) {
        ArrayList<String> appList = new ArrayList<>();
        for (String pkg : appCrash) {
            if (appNameMap.containsKey(pkg)) {
                appList.add(appNameMap.get(pkg) + " (" + pkg + ")");
            }
        }
        return appList;
    }

    private String cleanUpMessage(String msg) {
        msg = msg.replace("  ", " ");
        msg = msg.replace("， ", "，");
        msg = msg.replace("、 ", "、");
        msg = msg.replace("[", "");
        msg = msg.replace("]", "");
        msg = msg.trim();
        return msg;
    }

    private boolean haveCrashReport() {
        return !appCrash.isEmpty();
    }

    @Override
    public void error(String reason) {
        handler.post(() -> DialogHelper.showNoRootPermissionDialog(this));
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        mProxy.onStartSettingsForArguments(SubSettings.class, pref, false);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        requestCta();
    }

    private void requestCta() {
        /*if (!CtaUtils.isCtaEnabled(this)) {
            CtaUtils.showCtaDialog(this, REQUEST_CODE);
        }*/
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

    @Override
    public void onDestroy() {
        ShellInit.destroy();
        ThreadPoolManager.shutdown();
        PreferenceHeader.mUninstallApp.clear();
        PreferenceHeader.mDisableOrHiddenApp.clear();
        super.onDestroy();
    }
}
