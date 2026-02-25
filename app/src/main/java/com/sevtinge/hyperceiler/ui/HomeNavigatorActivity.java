package com.sevtinge.hyperceiler.ui;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.sevtinge.hyperceiler.R;

import fan.appcompat.app.Fragment;
import fan.core.utils.AttributeResolver;
import fan.navigator.Navigator;
import fan.navigator.NavigatorStrategy;
import fan.navigator.app.NavigatorActivity;
import fan.navigator.navigatorinfo.NavigatorInfo;
import fan.navigator.navigatorinfo.NavigatorInfoProvider;
import fan.navigator.navigatorinfo.UpdateFragmentNavInfo;

public class HomeNavigatorActivity extends NavigatorActivity {

    private static final String TAG = "HomeNavigatorActivity";

    public FragmentActivity getActivity() {
        return this;
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
        Bundle bundle = new Bundle();
        bundle.putParcelable("miuix:navigatorStrategy", createNavigatorStrategy());
        return bundle;
    }

    public final NavigatorStrategy createNavigatorStrategy() {
        NavigatorStrategy navigatorStrategy = new NavigatorStrategy();
        navigatorStrategy.setCompactMode(Navigator.Mode.C);
        navigatorStrategy.setRegularMode(Navigator.Mode.C);
        navigatorStrategy.setLargeMode(Navigator.Mode.C);
        return navigatorStrategy;
    }

    @Override
    public NavigatorInfoProvider getBottomTabMenuNavInfoProvider() {
        return id -> {
            Bundle args = new Bundle();
            if (id == 1000 || id == 1001 || id == 1002) {
                args.putInt("page", id - 1000);
                return new UpdateFragmentNavInfo(id, getDefaultContentFragment(), args);
            }
            return null;
        };
    }

    @Override
    public Class<? extends Fragment> getDefaultContentFragment() {
        return HomeContentFragment.class;
    }

    @Override
    public void onCreatePrimaryNavigation(Navigator navigator, Bundle bundle) {
        NavigatorInfo navInfoToHome = newLabel(R.string.navigation_home_title, R.drawable.ic_navigation_home, 1000);
        newLabel(R.string.navigation_settings_title, R.drawable.ic_navigation_settings, 1001);
        newLabel(R.string.navigation_about_title, R.drawable.ic_navigation_about, 1002);
        navigator.navigate(navInfoToHome);
    }

    @Override
    public void onCreateOtherNavigation(Navigator navigator, Bundle bundle) {

    }

    public final NavigatorInfo newLabel(int title, int iconResId, int id) {
        Bundle args = new Bundle();
        args.putInt("page", id - 1000);
        UpdateFragmentNavInfo navInfo = new UpdateFragmentNavInfo(id, getDefaultContentFragment(), args);
        newLabel(getString(title), iconResId, navInfo);
        return navInfo;
    }
}
