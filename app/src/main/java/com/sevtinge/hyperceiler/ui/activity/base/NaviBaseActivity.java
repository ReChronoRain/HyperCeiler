package com.sevtinge.hyperceiler.ui.activity.base;

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

import java.io.File;
import java.util.Set;

import fan.core.utils.AttributeResolver;
import fan.navigator.app.NavigatorActivity;

public abstract class NaviBaseActivity extends NavigatorActivity {

    protected BaseSettingsProxy mProxy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        checkTheme();
        mProxy = new SettingsProxy(this);
        super.onCreate(savedInstanceState);
        //initActionBar();
        registerObserver();
        //setRestartView(view -> DialogHelper.showRestartDialog(this));
    }

    public void checkTheme() {
        if (AttributeResolver.resolve(this, fan.navigator.R.attr.isNavigatorTheme) < 0) {
            Log.d("NotesNaviActivityTAG", "reset Theme");
            setTheme(R.style.NavigatorActivityTheme);
        }
    }

    protected void initActionBar() {
        setDisplayHomeAsUpEnabled(!(this instanceof NaviBaseActivity));
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
        if (!path.isEmpty()) getContentResolver().notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/pref/" + path + s), null);
    };

    private void registerFileObserver() {
        try {
            FileObserver mFileObserver = new FileObserver(new File(PrefsUtils.getSharedPrefsPath()), FileObserver.CLOSE_WRITE) {
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
