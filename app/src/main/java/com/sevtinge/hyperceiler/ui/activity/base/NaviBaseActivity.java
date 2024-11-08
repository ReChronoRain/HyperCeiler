package com.sevtinge.hyperceiler.ui.activity.base;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.core.utils.AttributeResolver;
import fan.navigator.app.NavigatorActivity;

public abstract class NaviBaseActivity extends NavigatorActivity {

    protected BaseSettingsProxy mProxy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        checkTheme();
        mProxy = new SettingsProxy(this);
        super.onCreate(savedInstanceState);
        registerObserver();
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
        PrefsUtils.registerOnSharedPreferenceChangeListener(getApplicationContext());
        Helpers.fixPermissionsAsync(getApplicationContext());
        Helpers.registerFileObserver(getApplicationContext());
    }
}
