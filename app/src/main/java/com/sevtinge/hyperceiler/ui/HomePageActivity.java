package com.sevtinge.hyperceiler.ui;

import static android.os.Process.killProcess;
import static android.os.Process.myPid;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSmallVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isVersionListed;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import fan.appcompat.app.AlertDialog;
import fan.appcompat.app.AppCompatActivity;
import fan.preference.PreferenceFragment;
import fan.provider.Settings;
import fan.provision.OobeUtils;
import fan.viewpager.widget.ViewPager;
import fan.viewpager2.widget.ViewPager2;

public class HomePageActivity extends AppCompatActivity
    implements ActivityCallback, IResult,
    PreferenceFragment.OnPreferenceStartFragmentCallback {

    private static final String STATE_CURRENT_PAGE = "home_current_page";

    public ViewPager mViewPager;
    public HomeContentAdapter mContentAdapter;

    public SwitchManager mSwitchManager;
    private boolean mIsUnsupportedVersionExiting;

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
        if (!isVersionListed()) {
            showUnsupportedVersionDialog();
            return;
        }
        // Activity 启动阶段，绑定 UI 任务（如签名校验弹窗、公告展示）
        AppInitializer.initOnActivityCreate(this, this);
        setContentView(R.layout.activity_home);
        setupNavigation();
        restoreCurrentPage(savedInstanceState);
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
        if (isFinishing() || isDestroyed()) {
            return;
        }
        // 首页应用名和页面上下文都依赖当前 locale，直接重建能避免旧 Context 残留。
        IconTitleLoader.clearLabelCache();
        recreate();
    }

    private void restoreCurrentPage(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null || mViewPager == null) {
            return;
        }
        int currentItem = savedInstanceState.getInt(STATE_CURRENT_PAGE, 0);
        mViewPager.setCurrentItem(currentItem, false);
        if (mSwitchManager != null) {
            mSwitchManager.setSelectedPosition(currentItem, false);
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mViewPager != null) {
            outState.putInt(STATE_CURRENT_PAGE, mViewPager.getCurrentItem());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void showUnsupportedVersionDialog() {
        String versionText = getString(
            R.string.homepage_unsupported_version_current,
            getAndroidVersion(),
            formatHyperOsVersion()
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(R.string.warn)
            .setMessage(getString(R.string.homepage_unsupported_version_message, versionText))
            .setPositiveButton(R.string.exit, (d, which) -> exitForUnsupportedVersion())
            .create();

        dialog.show();

        final var button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (button == null) {
            exitForUnsupportedVersion();
            return;
        }

        button.setText(getString(R.string.exit) + " (30)");

        new CountDownTimer(30_000L, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                button.setText(getString(R.string.exit) + " (" + (millisUntilFinished / 1000L) + ")");
            }

            @Override
            public void onFinish() {
                if (!isFinishing() && !isDestroyed()) {
                    dialog.dismiss();
                }
                exitForUnsupportedVersion();
            }
        }.start();
    }

    private String formatHyperOsVersion() {
        float smallVersion = getSmallVersion();
        if (smallVersion > 0f) {
            return String.valueOf(smallVersion);
        }
        return String.valueOf(getHyperOSVersion());
    }

    private void exitForUnsupportedVersion() {
        if (mIsUnsupportedVersionExiting) {
            return;
        }
        mIsUnsupportedVersionExiting = true;
        finishAffinity();
        killProcess(myPid());
    }
}
