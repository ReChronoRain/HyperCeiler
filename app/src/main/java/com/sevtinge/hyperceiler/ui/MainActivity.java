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
package com.sevtinge.hyperceiler.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.callback.IResult;
import com.sevtinge.hyperceiler.ui.base.NavigationActivity;
import com.sevtinge.hyperceiler.utils.BackupUtils;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.PropUtils;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.utils.search.SearchHelper;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import moralnorm.appcompat.app.AlertDialog;

public class MainActivity extends NavigationActivity implements IResult {
    private Handler handler;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        handler = new Handler(this.getMainLooper());
        context = this;
        int def = Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_log_level", "2"));
        super.onCreate(savedInstanceState);
        new Thread(() -> SearchHelper.getAllMods(MainActivity.this, savedInstanceState != null)).start();
        Helpers.checkXposedActivateState(this);
        ShellInit.init(this);
        PropUtils.setProp("persist.hyperceiler.log.level",
            (ProjectApi.isRelease() ? def : ProjectApi.isCanary() ? (def == 0 ? 3 : 4) : def));
        // test();
    }

    @Override
    public void error(String reason) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle(getResources().getString(R.string.tip))
                    .setMessage(getResources().getString(R.string.root))
                    .setHapticFeedbackEnabled(true)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        ShellInit.destroy();
        ThreadPoolManager.shutdown();
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
