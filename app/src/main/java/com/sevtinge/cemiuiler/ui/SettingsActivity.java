package com.sevtinge.cemiuiler.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.utils.DialogHelper;
import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.PrefsUtils;
import com.sevtinge.cemiuiler.utils.ShellUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

                if ((Boolean)o) {
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
                Set<String> options = (Set<String>)o;
                for (String op : options){
                    int index = mReboot.findIndexOfValue(op);
                    mShellPackageName.add("killall " + extras[index]);
                    Toast.makeText(getActivity(), "killall " + extras[index], Toast.LENGTH_SHORT).show();
                }
                ShellUtils.execCommand(mShellPackageName,true);
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
                DialogHelper.showDialog(getActivity(),
                        "确定要重置吗？",
                        "重置模块配置后其所有数据将会被删除！",
                        (dialog, which) -> {
                            PrefsUtils.mSharedPreferences.edit().clear().apply();
                            Toast.makeText(getActivity(), "已重置模块配置", Toast.LENGTH_LONG).show();
                        });
                return true;
            });
        }

        public void backupSettings(AppCompatActivity activity) {
            String backupPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + Helpers.externalFolder;
            if (!Helpers.preparePathForBackup(activity, backupPath)) return;
            ObjectOutputStream output = null;
            try {
                output = new ObjectOutputStream(new FileOutputStream(backupPath + Helpers.backupFile + new SimpleDateFormat("_YYYY-MM-dd-HH:mm:ss").format(new java.util.Date())));
                output.writeObject(PrefsUtils.mSharedPreferences.getAll());

                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                alert.setTitle("备份成功");
                alert.setPositiveButton(android.R.string.ok, (dialog, which) -> {});
                alert.show();
            } catch (Throwable e) {
                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                alert.setTitle("备份失败");
                alert.setMessage(e.toString());
                alert.setPositiveButton(android.R.string.ok, (dialog, which) -> {});
                alert.show();
            } finally {
                try {
                    if (output != null) {
                        output.flush();
                        output.close();
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }



        public void restoreSettings(final Activity act) {
            if (!Helpers.checkStoragePerm(act, Helpers.REQUEST_PERMISSIONS_RESTORE)) return;
            if (!Helpers.checkStorageReadable(act)) return;
            ObjectInputStream input = null;
            try {
                input = new ObjectInputStream(new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + Helpers.externalFolder + Helpers.backupFile));
                Map<String, ?> entries = (Map<String, ?>)input.readObject();
                if (entries == null || entries.isEmpty()) throw new RuntimeException("Cannot read entries");

                SharedPreferences.Editor prefEdit = PrefsUtils.mSharedPreferences.edit();
                prefEdit.clear();
                for (Map.Entry<String, ?> entry: entries.entrySet()) {
                    Object val = entry.getValue();
                    String key = entry.getKey();

                    if (val instanceof Boolean)
                        prefEdit.putBoolean(key, (Boolean)val);
                    else if (val instanceof Float)
                        prefEdit.putFloat(key, (Float)val);
                    else if (val instanceof Integer)
                        prefEdit.putInt(key, (Integer)val);
                    else if (val instanceof Long)
                        prefEdit.putLong(key, (Long)val);
                    else if (val instanceof String)
                        prefEdit.putString(key, ((String)val));
                    else if (val instanceof Set<?>)
                        prefEdit.putStringSet(key, ((Set<String>)val));
                }
                prefEdit.apply();

                AlertDialog.Builder alert = new AlertDialog.Builder(act);
                alert.setTitle("恢复");
                alert.setMessage("设置恢复成功！");
                alert.setCancelable(false);
                alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        act.finish();
                        act.startActivity(act.getIntent());
                    }
                });
                alert.show();
            } catch (Throwable t) {
                t.printStackTrace();
                Toast.makeText(act, t.toString(), Toast.LENGTH_SHORT).show();
                AlertDialog.Builder alert = new AlertDialog.Builder(act);
                alert.setTitle("恢复失败");
                alert.setMessage(t.getMessage());
                alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {}
                });
                alert.show();
            } finally {
                try {
                    if (input != null) input.close();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
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
                    Toast.makeText(this, "您是否要写入备份？", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "现在您必须手动启用此选项的权限。很好！", Toast.LENGTH_LONG).show();
                }
                break;
            case Helpers.REQUEST_PERMISSIONS_RESTORE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mSettingsFragment.restoreSettings(this);
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "您是否要恢复备份？", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "现在您必须手动启用此选项的权限。很好！", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
