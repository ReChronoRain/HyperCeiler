package com.sevtinge.hyperceiler.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.callback.IResult;
import com.sevtinge.hyperceiler.ui.navigator.ContentFragment;
import com.sevtinge.hyperceiler.ui.settings.adapter.PreferenceHeader;
import com.sevtinge.hyperceiler.ui.settings.core.SubSettingLauncher;
import com.sevtinge.hyperceiler.ui.settings.utils.SettingsFeatures;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.util.List;

import fan.appcompat.app.ActionBar;
import fan.appcompat.app.Fragment;
import fan.core.utils.AttributeResolver;
import fan.navigator.Navigator;
import fan.navigator.NavigatorStrategy;
import fan.navigator.app.NavigatorActivity;
import fan.navigator.navigatorinfo.NavigatorInfoProvider;
import fan.navigator.navigatorinfo.UpdateFragmentNavInfo;
import fan.preference.Preference;
import fan.preference.PreferenceFragment;
import fan.preference.core.PreferenceFragmentCompat;

public abstract class BaseCeilerTabActivity extends NavigatorActivity
        implements IResult, PreferenceFragment.OnPreferenceStartFragmentCallback {

    protected ViewGroup mContent;
    private String mSelectHeaderFragment = null;
    private int mCurrentSelectedHeaderIndex = -1;

    public void checkTheme() {
        if (AttributeResolver.resolve(this, R.attr.isNavigatorTheme) < 0) {
            Log.d("NotesNaviActivityTAG", "reset Theme");
            setTheme(R.style.NavigatorActivityTheme);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkTheme();
        super.onCreate(savedInstanceState);
        PrefsUtils.registerSharedPrefsObserver(this);
        initializeViews(savedInstanceState);
        hideActionBar();
    }

    private void hideActionBar() {
        ActionBar actionBar = getAppCompatActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    protected void initializeViews(Bundle savedInstanceState) {
        String fragment = getIntent().getStringExtra(":settings:show_fragment");
        if (!TextUtils.isEmpty(fragment)) {
            mSelectHeaderFragment = fragment;
        }
        if (savedInstanceState != null) {
            mSelectHeaderFragment = savedInstanceState.getString("select_header", mSelectHeaderFragment);
            mCurrentSelectedHeaderIndex = savedInstanceState.getInt("select_header_index", mCurrentSelectedHeaderIndex);
        }
    }

    public void updateHeaderList(List<PreferenceHeader> headers) {

    }

    public void onHeaderClick(PreferenceHeader header, int position) {
        if (header.fragment != null) {
            new SubSettingLauncher(this)
                    .setDestination(header.fragment)
                    .setArguments(header.fragmentArguments)
                    .setSourceMetricsCategory(0)
                    .setTitleText(header.title)
                    .setTitleRes(header.titleRes)
                    .launch();
        } else if (header.intent != null) {
            //resetPosition(i2);
            startSplitActivityIfNeed(header.intent);
        }
    }

    @Override
    public Class<? extends Fragment> getDefaultContentFragment() {
        return ContentFragment.class;
    }

    @Override
    public NavigatorInfoProvider getBottomTabMenuNavInfoProvider() {
        Bundle args = new Bundle();
        return id -> {
            switch (id) {
                case 1000 -> args.putInt("page", 0);
                case 1001 -> args.putInt("page", 1);
                case 1002 -> args.putInt("page", 2);
                default -> {
                    return null;
                }
            }
            return new UpdateFragmentNavInfo(id, getDefaultContentFragment(), args);
        };
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
    public void onCreatePrimaryNavigation(Navigator navigator, Bundle savedInstanceState) {
        UpdateFragmentNavInfo updateFragmentNavInfoToHome = getUpdateFragmentNavInfoToHome();
        UpdateFragmentNavInfo updateFragmentNavInfoToSettings = updateFragmentNavInfoToSettings();
        UpdateFragmentNavInfo updateFragmentNavInfoToAbout = getUpdateFragmentNavInfoToAbout();

        navigator.navigate(updateFragmentNavInfoToHome);

        newLabel(getString(R.string.home), updateFragmentNavInfoToHome);
        newLabel(getString(R.string.settings), updateFragmentNavInfoToSettings);
        newLabel(getString(R.string.about), updateFragmentNavInfoToAbout);
    }

    @Override
    public void onCreateOtherNavigation(Navigator navigator, Bundle bundle) {

    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfoToHome() {
        Bundle args = new Bundle();
        args.putInt("page", 0);
        return new UpdateFragmentNavInfo(1000, getDefaultContentFragment(), args);
    }

    private UpdateFragmentNavInfo updateFragmentNavInfoToSettings() {
        Bundle args = new Bundle();
        args.putInt("page", 2);
        return new UpdateFragmentNavInfo(1002, getDefaultContentFragment(), args);
    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfoToAbout() {
        Bundle args = new Bundle();
        args.putInt("page", 1);
        return new UpdateFragmentNavInfo(1001, getDefaultContentFragment(), args);
    }

    public void startSplitActivityIfNeed(Intent intent) {
        //if (SettingsFeatures.isFoldDevice()) intent.addMiuiFlags(4);
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startPreferencePanel(String str, Bundle bundle, int i, CharSequence charSequence, androidx.fragment.app.Fragment fragment, int i2) {
        startWithFragment(str, bundle, fragment, i2, i, charSequence, 0);
    }

    public void startWithFragment(String str, Bundle bundle, androidx.fragment.app.Fragment fragment, int i, int i2, int i3) {
        startWithFragment(str, bundle, fragment, i, i2, null, i3);
    }

    public void startWithFragment(String str, Bundle bundle, androidx.fragment.app.Fragment fragment, int i, int i2, CharSequence charSequence, int i3) {
        if (SettingsFeatures.isPadDevice()) {

        } else {
            new SubSettingLauncher(this)
                    .setDestination(str)
                    .setTitleRes(i2)
                    .setArguments(bundle)
                    .setResultListener(fragment, i)
                    .launch();
        }
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat preferenceFragmentCompat, @NonNull Preference preference) {
        return false;
    }
}
