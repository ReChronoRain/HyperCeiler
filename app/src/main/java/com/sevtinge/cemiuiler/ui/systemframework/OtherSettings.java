package com.sevtinge.cemiuiler.ui.systemframework;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.provider.SharedPrefsProvider;
import com.sevtinge.cemiuiler.ui.PickerHomeActivity;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemframework.base.BaseSystemFrameWorkActivity;

import moralnorm.appcompat.app.AlertDialog;
import moralnorm.os.SdkVersion;
import moralnorm.preference.Preference;
import moralnorm.preference.SwitchPreference;

public class OtherSettings extends BaseSystemFrameWorkActivity {

    public OtherFragment mOtherSettings = new OtherFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setImmersionMenuEnabled(true);
    }

    @Override
    public Fragment initFragment() {
        return mOtherSettings;
    }

    public static class OtherFragment extends SubFragment implements Preference.OnPreferenceClickListener {

        private Preference mCleanShareApps;
        private Preference mCleanOpenApps;

        SwitchPreference mAppLinkVerify;

        @Override
        public int getContentResId() {
            return R.xml.system_framework_other;
        }

        @Override
        public void initPrefs() {
            mCleanShareApps = findPreference("prefs_key_system_framework_clean_share_apps");
            mCleanShareApps.setOnPreferenceClickListener(this);
            mCleanOpenApps = findPreference("prefs_key_system_framework_clean_open_apps");
            mCleanOpenApps.setOnPreferenceClickListener(this);
            mAppLinkVerify = findPreference("prefs_key_system_framework_disable_app_link_verify");
            mAppLinkVerify.setVisible(SdkVersion.isAndroidT||SdkVersion.isAndroidS);
            mAppLinkVerify.setOnPreferenceChangeListener((preference, o) -> true);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference == mCleanShareApps) {
                openMultiAction(findPreference("prefs_key_system_framework_clean_share_apps"), null, PickerHomeActivity.Actions.Apps);
            }
            if (preference == mCleanOpenApps) {
                openMultiAction(findPreference("prefs_key_system_framework_clean_open_apps"), null, PickerHomeActivity.Actions.Apps2);
            }
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_system_framework_other, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.system_framework_share_menu_test:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Cemiuiler is the best!");
                sendIntent.setType("*/*");
                startActivity(Intent.createChooser(sendIntent, null));
                break;

            case R.id.system_framework_open_with_menu_test:
                Intent viewIntent = new Intent();
                viewIntent.setAction(Intent.ACTION_VIEW);
                new AlertDialog.Builder(this)
                        .setTitle("请选择要测试的数据类型")
                        .setItems(R.array.open_with_test, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String type = "*/*";
                                if (which == 0) {
                                    type = "image/*";
                                } else if (which == 1) {
                                    type = "audio/*";
                                } else if (which == 2) {
                                    type = "video/*";
                                } else if (which == 3) {
                                    type = "text/*";
                                } else if (which == 4) {
                                    type = "application/zip";
                                }
                                viewIntent.setDataAndType(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/test/" + which), type);
                                startActivity(Intent.createChooser(viewIntent, null));
                            }
                        })
                        .setNeutralButton(android.R.string.cancel, (dialog, which) -> {})
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
