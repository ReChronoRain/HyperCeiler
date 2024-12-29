package com.sevtinge.hyperceiler.ui.activity.base;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.main.ContentFragment;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.appcompat.app.Fragment;
import fan.core.utils.AttributeResolver;
import fan.navigator.Navigator;
import fan.navigator.NavigatorStrategy;
import fan.navigator.app.NavigatorActivity;
import fan.navigator.navigatorinfo.NavigatorInfoProvider;
import fan.navigator.navigatorinfo.UpdateFragmentNavInfo;

public class NaviBaseActivity extends NavigatorActivity {

    protected BaseSettingsProxy mProxy;

    public void checkTheme() {
        if (AttributeResolver.resolve(this, fan.navigator.R.attr.isNavigatorTheme) < 0) {
            Log.d("NotesNaviActivityTAG", "reset Theme");
            setTheme(R.style.NavigatorActivityTheme);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        checkTheme();
        mProxy = new SettingsProxy(this);
        super.onCreate(savedInstanceState);
        registerObserver();
    }

    private void registerObserver() {
        PrefsUtils.registerOnSharedPreferenceChangeListener(getApplicationContext());
        Helpers.fixPermissionsAsync(getApplicationContext());
        Helpers.registerFileObserver(getApplicationContext());
    }

    @Override
    public int getBottomTabMenu() {
        return R.menu.bottom_nav_menu;
    }

    @Override
    public int getNavigationOptionMenu() {
        return 0;
    }

    @Override
    public Bundle getNavigatorInitArgs() {
        NavigatorStrategy navigatorStrategy = new NavigatorStrategy();
        navigatorStrategy.setCompactMode(Navigator.Mode.C);
        navigatorStrategy.setRegularMode(Navigator.Mode.C);
        navigatorStrategy.setLargeMode(Navigator.Mode.NLC);
        Bundle bundle = new Bundle();
        bundle.putParcelable("miuix:navigatorStrategy", navigatorStrategy);
        return bundle;
    }

    @Override
    public NavigatorInfoProvider getBottomTabMenuNavInfoProvider() {
        return id -> {
            Bundle bundle = new Bundle();
            if (id == 1000) {
                bundle.putInt("page", 0);
            } else if (id == 1001) {
                bundle.putInt("page", 1);
            } else if (id == 1002) {
                bundle.putInt("page", 2);
            } else {
                return null;
            }
            return new UpdateFragmentNavInfo(id, getDefaultContentFragment(), bundle);
        };
    }

    @Override
    public Class<? extends Fragment> getDefaultContentFragment() {
        return ContentFragment.class;
    }

    @Override
    public void onCreatePrimaryNavigation(Navigator navigator, Bundle bundle) {
        UpdateFragmentNavInfo navInfoToHome = getUpdateFragmentNavInfo(0, 1000);
        UpdateFragmentNavInfo navInfoToSettings = getUpdateFragmentNavInfo(1, 1001);
        UpdateFragmentNavInfo navInfoToAbout = getUpdateFragmentNavInfo(2, 1002);
        newLabel(getString(R.string.navigation_home_title), R.drawable.ic_navigation_home, navInfoToHome);
        newLabel(getString(R.string.navigation_settings_title), R.drawable.ic_navigation_settings, navInfoToSettings);
        newLabel(getString(R.string.navigation_about_title), R.drawable.ic_navigation_about, navInfoToAbout);
        navigator.navigate(navInfoToHome);
    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfo(int position, int id) {
        Bundle bundle = new Bundle();
        bundle.putInt("page", position);
        return new UpdateFragmentNavInfo(id, getDefaultContentFragment(), bundle);
    }

    @Override
    public void onCreateOtherNavigation(Navigator navigator, Bundle bundle) {

    }
}
