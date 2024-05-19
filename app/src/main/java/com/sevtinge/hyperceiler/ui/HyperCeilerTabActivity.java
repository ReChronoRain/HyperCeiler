package com.sevtinge.hyperceiler.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.callback.IResult;
import com.sevtinge.hyperceiler.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.safe.CrashData;
import com.sevtinge.hyperceiler.ui.base.HyperCeilerTabBaseActivity;
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

import fan.appcompat.app.AlertDialog;

public class HyperCeilerTabActivity extends HyperCeilerTabBaseActivity implements IResult {

    private Handler handler;
    private Context context;

    private ArrayList<String> appCrash = new ArrayList<>();

    @Override
    protected void onCreate(Bundle bundle) {
        SharedPreferences mPrefs = PrefsUtils.mSharedPreferences;
        int count = Integer.parseInt(mPrefs.getString("prefs_key_settings_app_language", "-1"));
        if (count != -1) {
            LanguageHelper.setIndexLanguage(this, count, false);
        }
        handler = new Handler(this.getMainLooper());
        context = this;
        int def = Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_log_level", "3"));
        super.onCreate(bundle);
        new Thread(() -> SearchHelper.getAllMods(this, bundle != null)).start();
        Helpers.checkXposedActivateState(this);
        ShellInit.init(this);
        PropUtils.setProp("persist.hyperceiler.log.level", ProjectApi.isCanary() ? (def != 3 && def != 4 ? 3 : def) : def);
        appCrash = CrashData.toPkgList();
        handler.postDelayed(() -> {
            if (haveCrashReport()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.safe_mode_later_title)
                        .setMessage(appCrash.toString() + " " + getString(R.string.safe_mode_later_desc))
                        .setHapticFeedbackEnabled(true)
                        .setCancelable(false)
                        .setPositiveButton(R.string.safe_mode_cancel, (dialog, which) -> {
                            ShellInit.getShell().run("setprop persist.hyperceiler.crash.report \"\"").sync();
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
