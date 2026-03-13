package com.sevtinge.hyperceiler.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.CtaUtils;
import com.sevtinge.hyperceiler.dashboard.base.ActivityCallback;
import com.sevtinge.hyperceiler.home.manager.PageDecorator;
import com.sevtinge.hyperceiler.home.task.AppInitializer;
import com.sevtinge.hyperceiler.home.task.AppTaskManager;
import com.sevtinge.hyperceiler.home.widget.NavigationStyle;
import com.sevtinge.hyperceiler.home.widget.SwitchManager;
import com.sevtinge.hyperceiler.home.widget.SwitchMediator;
import com.sevtinge.hyperceiler.libhook.callback.IResult;
import com.sevtinge.hyperceiler.home.adapter.HomeContentAdapter;
import com.sevtinge.hyperceiler.about.AboutPageFragment;
import com.sevtinge.hyperceiler.about.AboutSettingsFragment;
import com.sevtinge.hyperceiler.home.HomePageFragment;
import com.sevtinge.hyperceiler.settings.SettingsFragment;
import com.sevtinge.hyperceiler.settings.SettingsPageFragment;

import fan.appcompat.app.AppCompatActivity;
import fan.preference.PreferenceFragment;
import fan.provider.Settings;
import fan.viewpager.widget.ViewPager;
import fan.viewpager2.widget.ViewPager2;

public class HomePageActivity extends AppCompatActivity
    implements ActivityCallback, IResult,
    PreferenceFragment.OnPreferenceStartFragmentCallback {

    private static final String TAG = "HomePageActivity";

    public ViewPager mViewPager;
    public HomeContentAdapter mContentAdapter;

    public SwitchManager mSwitchManager;
    public ViewPagerChangeListener mPageChangeCallback = new ViewPagerChangeListener();

    // 必须在 onCreate 之前或初始化时定义
    public final ActivityResultLauncher<Intent> mCtaLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result != null) {
                if (result.getResultCode() != 1) {
                    finishAffinity();
                    System.exit(0);
                }
                CtaUtils.setCtaValue(getApplicationContext(), result.getResultCode() == 1);
            }
        }
    );

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

        mContentAdapter = new HomeContentAdapter(this);
        mContentAdapter.addFragment(new HomePageFragment());
        mContentAdapter.addFragment(new SettingsPageFragment());
        mContentAdapter.addFragment(new AboutPageFragment());

        mViewPager = findViewById(R.id.vp_fragments);
        mViewPager.setAdapter(mContentAdapter);
        mViewPager.setOffscreenPageLimit(3);

        new SwitchMediator(mSwitchManager, mViewPager, true).attach();
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
