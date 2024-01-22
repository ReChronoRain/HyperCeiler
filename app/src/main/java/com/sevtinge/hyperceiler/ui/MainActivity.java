package com.sevtinge.hyperceiler.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.ui.base.NavigationActivity;
import com.sevtinge.hyperceiler.utils.BackupUtils;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.PrefsUtils;
import com.sevtinge.hyperceiler.utils.PropUtils;
import com.sevtinge.hyperceiler.utils.SearchHelper;
import com.sevtinge.hyperceiler.utils.ShellUtils;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import moralnorm.appcompat.app.AlertDialog;

public class MainActivity extends NavigationActivity {
    private static final String TAG = ITAG.TAG;
    private final String path = "/sdcard/Android/hy_crash/";
    /*ExecutorService executorService;
    Handler handler;*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int def = Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_log_level", "2"));
        super.onCreate(savedInstanceState);
        new Thread(() -> SearchHelper.getAllMods(MainActivity.this, savedInstanceState != null)).start();
        Helpers.checkXposedActivateState(this);
        if (!PropUtils.setProp("persist.hyperceiler.log.level",
            (ProjectApi.isRelease() ? def : ProjectApi.isCanary() ? (def == 0 ? 3 : 4) : def))) {
            new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(getResources().getString(R.string.tip))
                .setMessage(getResources().getString(R.string.root))
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        }
        /*executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        handler = new Handler();
        // Intent intent = new Intent(this, CrashService.class);
        // startService(intent);
        // checkCrash();*/
    }

    /*private void checkCrash() {
        executorService.submit(() -> {
            handler.post(() -> {
                ShellUtils.CommandResult commandResult = ShellUtils.execCommand("ls " + path, false, true);
                List<String> success = null;
                List<String> have = new ArrayList<>();
                List<String> get = new ArrayList<>();
                if (commandResult.result == 0) {
                    success = commandResult.successMsg;
                }
                if (success != null) {
                    for (String s : success) {
                        // AndroidLogUtils.LogI(TAG, "rss: " + s);
                        Pattern pattern = Pattern.compile("(.*)_(.*)");
                        Matcher matcher = pattern.matcher(s);
                        if (matcher.find()) {
                            if (Integer.parseInt(matcher.group(2)) >= 3) {
                                have.add(matcher.group(1));
                                get.add(s);
                            }
                        }
                    }
                }
                if (!have.isEmpty() || !get.isEmpty()) {
                    new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(getResources().getString(R.string.tip))
                        .setMessage("此作用域进入安全模式： " + have.toString() + "\n点击确定解除")
                        .setHapticFeedbackEnabled(true)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            for (String s : get) {
                                ShellUtils.execCommand("rm -rf " + path + s, false, false);
                            }
                        })
                        .show();
                }
            });
        });
    }*/

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
