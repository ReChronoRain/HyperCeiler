package com.sevtinge.hyperceiler.ui;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.isTablet;
import static com.sevtinge.hyperceiler.oldui.Application.isModuleActivated;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hyperceiler.AppInitializer;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.prefs.XmlPreference;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.dashboard.base.activity.ActivityCallback;
import com.sevtinge.hyperceiler.home.CrashReportManager;
import com.sevtinge.hyperceiler.home.CtaManager;
import com.sevtinge.hyperceiler.libhook.callback.IResult;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.oldui.main.fragment.DetailFragment;
import com.sevtinge.hyperceiler.oldui.utils.NoticeProcessor;
import com.sevtinge.hyperceiler.search.SearchHelper;
import com.sevtinge.hyperceiler.ui.adapter.HomeContentAdapter;
import com.sevtinge.hyperceiler.ui.page.AboutPageFragment;
import com.sevtinge.hyperceiler.ui.page.AboutSettingsFragment;
import com.sevtinge.hyperceiler.ui.page.HomePageFragment;
import com.sevtinge.hyperceiler.ui.page.SettingsFragment;
import com.sevtinge.hyperceiler.ui.page.SettingsPageFragment;

import fan.appcompat.app.AppCompatActivity;
import fan.navigator.Navigator;
import fan.navigator.NavigatorFragmentListener;
import fan.navigator.navigatorinfo.UpdateDetailFragmentNavInfo;
import fan.preference.PreferenceFragment;
import fan.provider.Settings;
import fan.viewpager2.widget.ViewPager2;

public class HomePageActivity extends AppCompatActivity
    implements ActivityCallback, IResult,
    PreferenceFragment.OnPreferenceStartFragmentCallback {

    public ViewPager2 mViewPager;
    public HomeContentAdapter mContentAdapter;

    private CrashReportManager mCrashManager;
    private CtaManager mCtaManager;

    public SwitchManager mSwitchManager;
    public ViewPagerChangeListener mPageChangeCallback = new ViewPagerChangeListener();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerObserver(getApplicationContext());
        // 1. 基础 UI 初始化
        applyGrayScaleFilter(this);

        setContentView(R.layout.activity_home);

        // 启动自动化初始化流水线
        AppInitializer.start(this, (crashes, notice) -> {
            if (isFinishing()) return;

            // 1. 弹出崩溃提示
            CrashReportManager.handleSafeMode(this, crashes);

            // 2. 弹出通知公告
            if (notice != null) NoticeProcessor.showNoticeDialog(this, notice);

            // 3. 检查模块激活 & CTA 协议
            if (!isModuleActivated) DialogHelper.showXposedActivateDialog(this);
            CtaManager.launch(this);

            // 4. 极低优先级任务
            // force 传 false：只有库为空时才解析，不影响日常启动速度
            SearchHelper.initIndex(this, true);
            //SearchHelper.init(getApplicationContext(), savedInstanceState != null);
        });

        mSwitchManager = new SwitchManager(findViewById(R.id.container));
        mSwitchManager.addSwitchView(R.menu.bottom_nav_menu, NavigationStyle.BOTTOM_LABEL);
        // 1. 直接获取 LiveData 对象
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
    public void onDestroy() {
        super.onDestroy();
        AppInitializer.release(); // 统一释放资源
    }
}
