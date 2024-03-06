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
package com.sevtinge.hyperceiler.ui.base;

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.provider.SharedPrefsProvider;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.util.Set;

import moralnorm.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected BaseSettingsProxy mProxy;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mProxy = new SettingsProxy(this);
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.enableEdgeToEdge(getWindow());
        EdgeToEdgeHelper.disableContrastEnforcement(getWindow());
        initActionBar();
        registerObserver();
    }

    protected void initActionBar() {
        setDisplayHomeAsUpEnabled(!(this instanceof NavigationActivity));
    }

    public void setDisplayHomeAsUpEnabled(boolean isEnable) {
        getAppCompatActionBar().setDisplayHomeAsUpEnabled(isEnable);
    }

    public void setActionBarEndView(View view) {
        getAppCompatActionBar().setEndView(view);
    }

    public void setActionBarEndIcon(@DrawableRes int resId, View.OnClickListener listener) {
        ImageView mRestartView = new ImageView(this);
        mRestartView.setImageResource(resId);
        mRestartView.setOnClickListener(listener);
        setActionBarEndView(mRestartView);
    }

    public void setRestartView(View.OnClickListener listener) {
        if (listener != null) setActionBarEndIcon(R.drawable.ic_reboot_small, listener);
    }

    private void registerObserver() {
        PrefsUtils.mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        Helpers.fixPermissionsAsync(getApplicationContext());
        registerFileObserver();
    }

    SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = (sharedPreferences, s) -> {
        Log.i("prefs", "Changed: " + s);
        requestBackup();
        Object val = sharedPreferences.getAll().get(s);
        String path = "";
        if (val instanceof String)
            path = "string/";
        else if (val instanceof Set<?>)
            path = "stringset/";
        else if (val instanceof Integer)
            path = "integer/";
        else if (val instanceof Boolean)
            path = "boolean/";
        getContentResolver().notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/" + path + s), null);
        if (!path.equals("")) getContentResolver().notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/pref/" + path + s), null);
    };

    private void registerFileObserver() {
        try {
            FileObserver mFileObserver = new FileObserver(PrefsUtils.getSharedPrefsPath(), FileObserver.CLOSE_WRITE) {
                @Override
                public void onEvent(int event, String path) {
                    Helpers.fixPermissionsAsync(getApplicationContext());
                }
            };
            mFileObserver.startWatching();
        } catch (Throwable t) {
            Log.e("prefs", "Failed to start FileObserver!");
        }
    }

    public void requestBackup() {
        new BackupManager(getApplicationContext()).dataChanged();
    }
}
