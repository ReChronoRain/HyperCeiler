package com.sevtinge.cemiuiler.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.utils.BackupUtils;
import com.sevtinge.cemiuiler.utils.DialogHelper;
import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.PrefsUtils;
import com.sevtinge.cemiuiler.utils.ShellUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import moralnorm.appcompat.app.AlertDialog;
import moralnorm.appcompat.app.AppCompatActivity;
import moralnorm.preference.MultiSelectListPreference;
import moralnorm.preference.SwitchPreference;

public class SettingsActivity extends BaseAppCompatActivity {

    SettingsFragment mSettingsFragment = new SettingsFragment();

    @Override
    public Fragment initFragment() {
        return mSettingsFragment;
    }

    public static class SettingsFragment extends SubFragment {

        private MultiSelectListPreference mReboot;

        @Override
        public int getContentResId() {
            return R.xml.prefs_settings;
        }

        @Override
        public void initPrefs() {
            SwitchPreference mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
            mHideAppIcon.setOnPreferenceChangeListener((preference, o) -> {

                PackageManager pm = getActivity().getPackageManager();
                int mComponentEnabledState;

                if ((Boolean) o) {
                    mComponentEnabledState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                } else {
                    mComponentEnabledState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                }

                pm.setComponentEnabledSetting(new ComponentName(getActivity(), HideAppActivity.class), mComponentEnabledState, PackageManager.DONT_KILL_APP);

                return true;
            });

            String[] mRestartAllScopes = getResources().getStringArray(R.array.xposed_scope);
            List<String> mItemList = Arrays.asList(mRestartAllScopes);
            String[] mItems = new String[0];
            if (mItemList.contains("android")) {
                List<String> mItemList2 = new ArrayList<>(mItemList);
                mItemList2.remove("android");
                mItems = mItemList2.toArray(new String[mItemList2.size()]);
            }

            mReboot = findPreference("prefs_key_settings_reboot");
            mReboot.setEntries(mItems);
            mReboot.setEntryValues(mItems);
            mReboot.setOnPreferenceChangeListener((preference, o) -> {
                List<String> mShellPackageName = new ArrayList<>();
                CharSequence[] extras = mReboot.getEntries();
                Set<String> options = (Set<String>) o;
                for (String op : options) {
                    int index = mReboot.findIndexOfValue(op);
                    mShellPackageName.add("killall " + extras[index]);
                    Toast.makeText(getActivity(), "killall " + extras[index], Toast.LENGTH_SHORT).show();
                }
                ShellUtils.execCommand(mShellPackageName, true);
                return false;
            });

            findPreference("prefs_key_back").setOnPreferenceClickListener(preference -> {
                final AppCompatActivity activity = (AppCompatActivity) getActivity();
                backupSettings(activity);
                return true;
            });

            findPreference("prefs_key_rest").setOnPreferenceClickListener(preference -> {
                restoreSettings(getActivity());
                return true;
            });

            findPreference("prefs_key_reset").setOnPreferenceClickListener(preference -> {
                DialogHelper.showDialog(getActivity(), R.string.reset_title, R.string.reset_desc, (dialog, which) -> {
                    PrefsUtils.mSharedPreferences.edit().clear().apply();
                    Toast.makeText(getActivity(), R.string.reset_okay, Toast.LENGTH_LONG).show();
                });
                return true;
            });
        }

        public void backupSettings(Activity activity) {
            BackupUtils.backup(activity);
        }


        public void restoreSettings(Activity activity) {
            BackupUtils.restore(activity);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length == 0) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        switch (requestCode) {
            case Helpers.REQUEST_PERMISSIONS_BACKUP:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mSettingsFragment.backupSettings(this);
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, R.string.backup_ask, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.backup_permission, Toast.LENGTH_LONG).show();
                }
                break;
            case Helpers.REQUEST_PERMISSIONS_RESTORE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mSettingsFragment.restoreSettings(this);
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, R.string.rest_ask, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.rest_permission, Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            switch (requestCode) {
                case BackupUtils.CREATE_DOCUMENT_CODE:
                    BackupUtils.handleCreateDocument(this, data.getData());
                    alert.setTitle(R.string.backup_success);
                    break;
                case BackupUtils.OPEN_DOCUMENT_CODE:
                    BackupUtils.handleReadDocument(this, data.getData());
                    alert.setTitle(R.string.rest_success);
                    break;
                default:
                    return;
            }
            alert.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            });
            alert.show();
        } catch (Exception e) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            switch (requestCode) {
                case BackupUtils.CREATE_DOCUMENT_CODE:
                    alert.setTitle(R.string.backup_failed);
                    break;
                case BackupUtils.OPEN_DOCUMENT_CODE:
                    alert.setTitle(R.string.rest_failed);
                    break;
            }
            alert.setMessage(e.toString());
            alert.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            });
            alert.show();
        }
    }
}
