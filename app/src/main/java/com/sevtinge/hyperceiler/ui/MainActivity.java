package com.sevtinge.hyperceiler.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.NavigationActivity;
import com.sevtinge.hyperceiler.utils.BackupUtils;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.SearchHelper;
import com.sevtinge.hyperceiler.utils.ShellUtils;

import moralnorm.appcompat.app.AlertDialog;

public class MainActivity extends NavigationActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Thread(() -> SearchHelper.getAllMods(MainActivity.this, savedInstanceState != null)).start();
        Helpers.checkXposedActivateState(this);
        if (ShellUtils.checkRootPermission() != 0) {
            new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(getResources().getString(R.string.tip))
                .setMessage(getResources().getString(R.string.root))
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        }
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
