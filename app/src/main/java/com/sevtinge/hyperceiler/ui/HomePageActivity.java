package com.sevtinge.hyperceiler.ui;

import static com.sevtinge.hyperceiler.common.utils.DialogHelper.showUserAgreeDialog;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.CtaUtils;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.dashboard.base.activity.ActivityCallback;
import com.sevtinge.hyperceiler.home.manager.PageDecorator;
import com.sevtinge.hyperceiler.home.task.AppInitializer;
import com.sevtinge.hyperceiler.home.task.AppTaskManager;
import com.sevtinge.hyperceiler.libhook.callback.IResult;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashScope;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.pkg.CheckModifyUtils;
import com.sevtinge.hyperceiler.ui.adapter.HomeContentAdapter;
import com.sevtinge.hyperceiler.ui.page.AboutPageFragment;
import com.sevtinge.hyperceiler.ui.page.AboutSettingsFragment;
import com.sevtinge.hyperceiler.ui.page.HomePageFragment;
import com.sevtinge.hyperceiler.ui.page.SettingsFragment;
import com.sevtinge.hyperceiler.ui.page.SettingsPageFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import fan.appcompat.app.AppCompatActivity;
import fan.preference.PreferenceFragment;
import fan.provider.Settings;
import fan.provision.OobeUtils;
import fan.viewpager2.widget.ViewPager2;

public class HomePageActivity extends AppCompatActivity
    implements ActivityCallback, IResult,
    PreferenceFragment.OnPreferenceStartFragmentCallback {

    private static final String TAG = "HomePageActivity";

    public ViewPager2 mViewPager;
    public HomeContentAdapter mContentAdapter;

    public SwitchManager mSwitchManager;
    public ViewPagerChangeListener mPageChangeCallback = new ViewPagerChangeListener();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Activity 启动阶段，绑定 UI 任务（如签名校验弹窗、公告展示）
        AppInitializer.initOnActivityCreate(this);
        setContentView(R.layout.activity_home);

        setupNavigation();
    }

    private void setupNavigation() {
        mSwitchManager = new SwitchManager(findViewById(R.id.container));
        mSwitchManager.addSwitchView(R.menu.bottom_nav_menu, NavigationStyle.BOTTOM_LABEL);

        LiveData<Boolean> isFloatNavEnabled = Settings.Global.getBooleanLiveData(this, "settings_float_nav", false);
        isFloatNavEnabled.observe(this, isEnabled -> mSwitchManager.setFloatingStyle(isEnabled));
        mSwitchManager.setOnSwitchChangeListener((position, itemId) -> mViewPager.setCurrentItem(position, true));

        mContentAdapter = new HomeContentAdapter(this);
        mContentAdapter.addFragment(new HomePageFragment());
        mContentAdapter.addFragment(new SettingsPageFragment());
        mContentAdapter.addFragment(new AboutPageFragment());

        mViewPager = findViewById(R.id.vp_fragments);
        mViewPager.setAdapter(mContentAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setSpringEnabled(false);
        mViewPager.registerOnPageChangeCallback(mPageChangeCallback);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppTaskManager.requestCta(this);
    }

    public SwitchManager getSwitchManager() {
        return mSwitchManager;
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        if (caller instanceof SettingsFragment || caller instanceof AboutSettingsFragment) {
            onStartSubSettingsForArguments(this, pref, false);
            return true;
        }
        return false;
    }

    public class ViewPagerChangeListener extends ViewPager2.OnPageChangeCallback {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            mSwitchManager.setSelectedPosition(position, true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PageDecorator.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PageDecorator.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
