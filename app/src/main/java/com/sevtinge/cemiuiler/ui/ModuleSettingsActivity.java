package com.sevtinge.cemiuiler.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SettingsActivity;
import com.sevtinge.cemiuiler.ui.fragment.ModuleSettingsFragment;
import com.sevtinge.cemiuiler.utils.BackupUtils;
import com.sevtinge.cemiuiler.utils.Helpers;

import moralnorm.appcompat.app.AlertDialog;

public class ModuleSettingsActivity extends SettingsActivity {

    ModuleSettingsFragment mModuleSettingsFragment = new ModuleSettingsFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFragment(mModuleSettingsFragment);
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + this.getPackageName()));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length == 0) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        switch (requestCode) {
            case Helpers.REQUEST_PERMISSIONS_BACKUP -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mModuleSettingsFragment.backupSettings(this);
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, R.string.backup_ask, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.backup_permission, Toast.LENGTH_LONG).show();
                }
            }
            case Helpers.REQUEST_PERMISSIONS_RESTORE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mModuleSettingsFragment.restoreSettings(this);
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, R.string.rest_ask, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.rest_permission, Toast.LENGTH_LONG).show();
                }
            }
            default -> super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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
                default -> { return; }
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
