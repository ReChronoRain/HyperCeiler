package com.sevtinge.hyperceiler.ui.base;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.SubSettings;
import com.sevtinge.hyperceiler.ui.page.ContentFragment;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;

import fan.appcompat.app.Fragment;
import fan.navigator.Navigator;
import fan.navigator.NavigatorStrategy;
import fan.navigator.app.NavigatorActivity;
import fan.navigator.navigatorinfo.NavigatorInfoProvider;
import fan.navigator.navigatorinfo.UpdateFragmentNavInfo;
import fan.preference.Preference;
import fan.preference.PreferenceFragment;
import fan.preference.core.PreferenceFragmentCompat;

public class HyperCeilerTabBaseActivity extends NavigatorActivity
        implements PreferenceFragment.OnPreferenceStartFragmentCallback {

    @Override
    public int getBottomTabMenu() {
        return R.menu.bottom_nav_menu;
    }

    @Override
    public NavigatorInfoProvider getBottomTabMenuNavInfoProvider() {
        Bundle bundle = new Bundle();
        return i -> {
            if (i == 1000) {
                bundle.putInt("page", 0);
            } else if (i == 1001) {
                bundle.putInt("page", 1);
            } else if (i == 1002) {
                bundle.putInt("page", 2);
            } else {
                return null;
            }
            return new UpdateFragmentNavInfo(i, getDefaultContentFragment(), bundle);
        };
    }

    @Override
    public Class<? extends Fragment> getDefaultContentFragment() {
        return ContentFragment.class;
    }

    @Override
    public int getNavigationOptionMenu() {
        return 0;
    }

    @Override
    public Bundle getNavigatorInitArgs() {
        NavigatorStrategy navigatorStrategy = new NavigatorStrategy();
        navigatorStrategy.setLargeMode(Navigator.Mode.C);
        Bundle bundle = new Bundle();
        bundle.putParcelable("miuix:navigatorStrategy", navigatorStrategy);
        return bundle;
    }

    @Override
    public void onCreateOtherNavigation(Navigator navigator, Bundle bundle) {

    }

    @Override
    public void onCreatePrimaryNavigation(Navigator navigator, Bundle bundle) {
        UpdateFragmentNavInfo updateFragmentNavInfoToHome = getUpdateFragmentNavInfoToHome();
        UpdateFragmentNavInfo updateFragmentNavInfoToSettings = updateFragmentNavInfoToSettings();
        UpdateFragmentNavInfo updateFragmentNavInfoToClock = getUpdateFragmentNavInfoToAbout();
        newLabel(getString(R.string.home), updateFragmentNavInfoToHome);
        newLabel(getString(R.string.settings), updateFragmentNavInfoToSettings);
        newLabel(getString(R.string.about), updateFragmentNavInfoToClock);
        navigator.navigate(updateFragmentNavInfoToHome);
    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfoToHome() {
        Bundle bundle = new Bundle();
        bundle.putInt("page", 0);
        return new UpdateFragmentNavInfo(1000, getDefaultContentFragment(), bundle);
    }

    private UpdateFragmentNavInfo updateFragmentNavInfoToSettings() {
        Bundle bundle = new Bundle();
        bundle.putInt("page", 1);
        return new UpdateFragmentNavInfo(1001, getDefaultContentFragment(), bundle);
    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfoToAbout() {
        Bundle bundle = new Bundle();
        bundle.putInt("page", 2);
        return new UpdateFragmentNavInfo(1002, getDefaultContentFragment(), bundle);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat preferenceFragmentCompat, @NonNull Preference preference) {
        Bundle args = null;
        String mFragmentName = preference.getFragment();
        String mTitle = preference.getTitle().toString();
        SettingLauncherHelper.onStartSettingsForArguments(this, SubSettings.class, mFragmentName, args, mTitle);
        return true;
    }
}
