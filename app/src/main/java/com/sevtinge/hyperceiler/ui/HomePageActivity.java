package com.sevtinge.hyperceiler.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.about.AboutPageFragment;
import com.sevtinge.hyperceiler.about.AboutSettingsFragment;
import com.sevtinge.hyperceiler.common.utils.AppSettingsStore;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.common.utils.shell.IResult;
import com.sevtinge.hyperceiler.dashboard.base.ActivityCallback;
import com.sevtinge.hyperceiler.home.HomePageFragment;
import com.sevtinge.hyperceiler.home.IconTitleLoader;
import com.sevtinge.hyperceiler.home.adapter.HomeContentAdapter;
import com.sevtinge.hyperceiler.home.manager.PageDecorator;
import com.sevtinge.hyperceiler.home.task.AppInitializer;
import com.sevtinge.hyperceiler.home.widget.NavigationStyle;
import com.sevtinge.hyperceiler.home.widget.SwitchManager;
import com.sevtinge.hyperceiler.home.widget.SwitchMediator;
import com.sevtinge.hyperceiler.settings.SettingsFragment;
import com.sevtinge.hyperceiler.settings.SettingsPageFragment;
import com.sevtinge.hyperceiler.utils.PersistConfig;

import fan.appcompat.app.AppCompatActivity;
import fan.preference.PreferenceFragment;
import fan.provider.Settings;
import fan.provision.OobeUtils;
import fan.viewpager.widget.ViewPager;
import fan.viewpager2.widget.ViewPager2;

public class HomePageActivity extends AppCompatActivity
    implements ActivityCallback, IResult,
    PreferenceFragment.OnPreferenceStartFragmentCallback {

    private static final String TAG = "HomePageActivity";

    public ViewPager mViewPager;
    public HomeContentAdapter mContentAdapter;

    public SwitchManager mSwitchManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.sevtinge.hyperceiler.utils.LanguageHelper.wrapContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PersistConfig.isAprilFoolsThemeView) setTheme(R.style.HomePageAprilFoolsTheme);
        if (!OobeUtils.isProvisioned(this) && !OobeUtils.isDebugOobeMode(this)) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
            return;
        }
        // Activity 启动阶段，绑定 UI 任务（如签名校验弹窗、公告展示）
        AppInitializer.initOnActivityCreate(this, this);
        setContentView(R.layout.activity_home);
        setupNavigation();
    }

    private void setupNavigation() {
        mSwitchManager = new SwitchManager(findViewById(R.id.container));

        boolean isFloating = AppSettingsStore.isFloatNavEnabled(this);

        NavigationStyle initialStyle = isFloating ? NavigationStyle.CAPSULE_ICON : NavigationStyle.BOTTOM_LABEL;
        mSwitchManager.addSwitchView(R.menu.bottom_nav_menu, initialStyle);

        // 后续变化通过 LiveData 监听
        LiveData<Boolean> isFloatNavEnabled = Settings.Global.getBooleanLiveData(
            this,
            AppSettingsStore.KEY_FLOAT_NAV,
            false
        );
        isFloatNavEnabled.observe(this, isEnabled -> {
            // Hook/备份链路仍依赖 prefs，保持镜像同步。
            PrefsBridge.putByApp(AppSettingsStore.PREF_FLOAT_NAV, isEnabled);
            mSwitchManager.setFloatingStyle(isEnabled);
        });

        mViewPager = findViewById(R.id.vp_fragments);
        rebuildContentPages();
        new SwitchMediator(mSwitchManager, mViewPager, true).attach();
    }

    private void rebuildContentPages() {
        mContentAdapter = new HomeContentAdapter(this);
        mContentAdapter.addFragment(new HomePageFragment());
        mContentAdapter.addFragment(new SettingsPageFragment());
        mContentAdapter.addFragment(new AboutPageFragment());

        mViewPager.setAdapter(mContentAdapter);
        mViewPager.setOffscreenPageLimit(3);
    }

    public void reloadPagesForLanguageChange() {
        if (mViewPager == null) {
            return;
        }
        int currentItem = mViewPager.getCurrentItem();
        // 首页条目标题会异步按包名覆盖，切语言时先清掉旧 locale 的 label 缓存。
        IconTitleLoader.clearLabelCache();
        mViewPager.setAdapter(null);
        clearContentFragments();
        rebuildContentPages();
        if (mSwitchManager != null) {
            mSwitchManager.setSelectedPosition(currentItem, false);
        }
        mViewPager.setCurrentItem(currentItem, false);
    }

    private void clearContentFragments() {
        if (mContentAdapter == null) {
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        boolean hasChanges = false;
        // 这里必须移除旧 fragment，避免 ViewPager 复用旧实例导致首页列表不刷新语言。
        for (int i = 0; i < mContentAdapter.getCount(); i++) {
            Fragment fragment = fragmentManager.findFragmentByTag(mContentAdapter.getFragmentTag(i));
            if (fragment != null) {
                transaction.remove(fragment);
                hasChanges = true;
            }
        }
        if (hasChanges) {
            transaction.commitNowAllowingStateLoss();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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
