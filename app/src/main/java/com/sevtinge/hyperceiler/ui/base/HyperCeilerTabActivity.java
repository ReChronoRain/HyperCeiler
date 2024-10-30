package com.sevtinge.hyperceiler.ui.base;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.SubSettings;
import com.sevtinge.hyperceiler.ui.fragment.base.ContentFragment;

import fan.appcompat.app.Fragment;
import fan.navigator.Navigator;
import fan.navigator.NavigatorStrategy;
import fan.navigator.navigatorinfo.NavigatorInfoProvider;
import fan.navigator.navigatorinfo.UpdateFragmentNavInfo;
import fan.preference.PreferenceFragment;

public class HyperCeilerTabActivity extends NaviBaseActivity
        implements PreferenceFragment.OnPreferenceStartFragmentCallback {

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
        navigatorStrategy.setLargeMode(Navigator.Mode.C);
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
        UpdateFragmentNavInfo navInfoToHome = getUpdateFragmentNavInfo(0);
        UpdateFragmentNavInfo navInfoToWidget = getUpdateFragmentNavInfo(1);
        UpdateFragmentNavInfo navInfoToList = getUpdateFragmentNavInfo(2);
        newLabel(getString(R.string.navigation_home_title), navInfoToHome);
        newLabel(getString(R.string.navigation_settings_title), navInfoToWidget);
        newLabel(getString(R.string.navigation_about_title), navInfoToList);
        navigator.navigate(navInfoToHome);
    }

    @Override
    public void onCreateOtherNavigation(Navigator navigator, Bundle bundle) {

    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfo(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt("page", position);
        return new UpdateFragmentNavInfo(position, getDefaultContentFragment(), bundle);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        mProxy.onStartSettingsForArguments(SubSettings.class, pref, false);
        return true;
    }
}
