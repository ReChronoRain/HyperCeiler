package com.sevtinge.hyperceiler.ui;

import static android.os.Process.killProcess;
import static android.os.Process.myPid;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.SUPPORT_FULL;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.SUPPORT_PARTIAL;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSmallVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getVersionListText;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isVersionListed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.about.AboutPageFragment;
import com.sevtinge.hyperceiler.about.AboutSettingsFragment;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
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
import com.sevtinge.hyperceiler.provision.utils.NoticeProvider;
import com.sevtinge.hyperceiler.provision.utils.OobeTransitionHelper;
import com.sevtinge.hyperceiler.provision.utils.ProvisionManager;
import com.sevtinge.hyperceiler.settings.SettingsFragment;
import com.sevtinge.hyperceiler.settings.SettingsPageFragment;
import com.sevtinge.hyperceiler.utils.NoticeProcessor;
import com.sevtinge.hyperceiler.utils.PersistConfig;

import java.util.ArrayList;
import java.util.List;

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
    private static final float OOBE_HOME_START_SCALE = 1.4f;
    private static final long OOBE_HOME_ALPHA_DELAY_MS = 60L;
    private static final long OOBE_HOME_ALPHA_DURATION_MS = 230L;
    private static final long OOBE_HOME_SCALE_DURATION_MS = 619L;
    private static final TimeInterpolator OOBE_HOME_ALPHA_INTERPOLATOR = fraction ->
        (float) Math.sin(fraction * Math.PI / 2d);
    private static final TimeInterpolator OOBE_HOME_SPRING_INTERPOLATOR = fraction -> {
        if (fraction <= 0f) return 0f;
        if (fraction >= 1f) return 1f;
        double dampingRatio = 0.65d;
        double angularFrequency = 10.5d;
        double dampingRoot = Math.sqrt(1d - dampingRatio * dampingRatio);
        double dampedFrequency = angularFrequency * dampingRoot;
        double envelope = Math.exp(-dampingRatio * angularFrequency * fraction);
        return (float) (1d - envelope * (
            Math.cos(dampedFrequency * fraction) +
                dampingRatio / dampingRoot * Math.sin(dampedFrequency * fraction)
        ));
    };
    public ViewPager mViewPager;
    public HomeContentAdapter mContentAdapter;

    public SwitchManager mSwitchManager;
    private boolean mIsUnsupportedVersionExiting;
    private View mHomeRoot;
    private AnimatorSet mHomeRevealAnimator;
    private ViewTreeObserver.OnPreDrawListener mHomeRevealPreDrawListener;
    private ViewTreeObserver.OnPreDrawListener mHomeReadyPreDrawListener;
    private int mHomeRootLayerType = View.LAYER_TYPE_NONE;
    private boolean mCheckVersionAfterReveal;
    private boolean mLaunchOobeWhenReady;
    private final Runnable mScheduleHomeRevealRunnable = this::scheduleHomeReveal;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.sevtinge.hyperceiler.utils.LanguageHelper.wrapContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PersistConfig.isAprilFoolsThemeView) setTheme(R.style.HomePageAprilFoolsTheme);
        boolean prepareHomeForOobe = getIntent().getBooleanExtra(
            OobeTransitionHelper.EXTRA_PREPARE_HOME,
            false
        );
        if (!OobeUtils.isProvisioned(this) && !OobeUtils.isDebugOobeMode(this) &&
            !prepareHomeForOobe) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
            return;
        }
        if (!prepareHomeForOobe && !isVersionListed()) {
            showUnsupportedVersionDialog();
            return;
        }
        OobeTransitionHelper.resetHomeReady();
        // Activity 启动阶段，绑定 UI 任务（如签名校验弹窗、公告展示）
        AppInitializer.initOnActivityCreate(this, this);
        setContentView(R.layout.activity_home);
        mHomeRoot = findViewById(R.id.container);
        setupNavigation();
        restoreCurrentPage(savedInstanceState);

        ProvisionManager.setProvider(context -> {
            NoticeProcessor.NoticeResult result = NoticeProcessor.process(context);
            List<Integer> list = new ArrayList<>();
            if (result != null) {
                list.add(result.protocolVersion());
                list.add(result.privacyVersion());
            }
            return list;
        });
        mLaunchOobeWhenReady = prepareHomeForOobe && savedInstanceState == null;
        scheduleHomeReady();
    }

    private void launchOobe() {
        Intent intent = new Intent(
            this,
            com.sevtinge.hyperceiler.provision.activity.DefaultActivity.class
        );
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void scheduleHomeReady() {
        if (mHomeRoot == null || mHomeReadyPreDrawListener != null) return;
        mHomeReadyPreDrawListener = () -> {
            removeHomeReadyPreDrawListener();
            if (mLaunchOobeWhenReady) {
                mLaunchOobeWhenReady = false;
                OobeTransitionHelper.markHomeReady();
                launchOobe();
                return false;
            }
            mHomeRoot.postOnAnimation(() ->
                mHomeRoot.postOnAnimation(OobeTransitionHelper::markHomeReady)
            );
            return true;
        };
        mHomeRoot.getViewTreeObserver().addOnPreDrawListener(mHomeReadyPreDrawListener);
        mHomeRoot.invalidate();
    }

    private void prepareHomeReveal() {
        if (mHomeRoot == null) return;
        mHomeRoot.setScaleX(OOBE_HOME_START_SCALE);
        mHomeRoot.setScaleY(OOBE_HOME_START_SCALE);
        mHomeRoot.setAlpha(0f);
    }

    private void scheduleHomeReveal() {
        if (mHomeRoot == null || mHomeRevealPreDrawListener != null) return;
        mHomeRevealPreDrawListener = () -> {
            removeHomeRevealPreDrawListener();
            startHomeReveal();
            return true;
        };
        mHomeRoot.getViewTreeObserver().addOnPreDrawListener(mHomeRevealPreDrawListener);
        mHomeRoot.invalidate();
    }

    private void startHomeReveal() {
        if (mHomeRoot == null || isFinishing() || isDestroyed()) return;
        if (mHomeRoot.getWidth() <= 0 || mHomeRoot.getHeight() <= 0) {
            scheduleHomeReveal();
            return;
        }

        mHomeRoot.setPivotX(mHomeRoot.getWidth() / 2f);
        mHomeRoot.setPivotY(mHomeRoot.getHeight() / 2f);
        mHomeRootLayerType = mHomeRoot.getLayerType();
        mHomeRoot.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(
            mHomeRoot,
            View.SCALE_X,
            OOBE_HOME_START_SCALE,
            1f
        );
        scaleX.setDuration(OOBE_HOME_SCALE_DURATION_MS);
        scaleX.setInterpolator(OOBE_HOME_SPRING_INTERPOLATOR);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(
            mHomeRoot,
            View.SCALE_Y,
            OOBE_HOME_START_SCALE,
            1f
        );
        scaleY.setDuration(OOBE_HOME_SCALE_DURATION_MS);
        scaleY.setInterpolator(OOBE_HOME_SPRING_INTERPOLATOR);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(
            mHomeRoot,
            View.ALPHA,
            0f,
            1f
        );
        alpha.setStartDelay(OOBE_HOME_ALPHA_DELAY_MS);
        alpha.setDuration(OOBE_HOME_ALPHA_DURATION_MS);
        alpha.setInterpolator(OOBE_HOME_ALPHA_INTERPOLATOR);

        AnimatorSet animator = new AnimatorSet();
        animator.playTogether(scaleX, scaleY, alpha);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mHomeRevealAnimator != animator) return;
                mHomeRevealAnimator = null;
                restoreHomeTransform();
                if (mCheckVersionAfterReveal) {
                    mCheckVersionAfterReveal = false;
                    if (!isVersionListed()) showUnsupportedVersionDialog();
                }
            }
        });
        mHomeRevealAnimator = animator;
        animator.start();
    }

    private void removeHomeRevealPreDrawListener() {
        if (mHomeRoot == null || mHomeRevealPreDrawListener == null) return;
        ViewTreeObserver observer = mHomeRoot.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnPreDrawListener(mHomeRevealPreDrawListener);
        }
        mHomeRevealPreDrawListener = null;
    }

    private void removeHomeReadyPreDrawListener() {
        if (mHomeRoot == null || mHomeReadyPreDrawListener == null) return;
        ViewTreeObserver observer = mHomeRoot.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnPreDrawListener(mHomeReadyPreDrawListener);
        }
        mHomeReadyPreDrawListener = null;
    }

    private void cancelHomeReveal() {
        AnimatorSet animator = mHomeRevealAnimator;
        mHomeRevealAnimator = null;
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
        }
        if (mHomeRoot != null) {
            mHomeRoot.removeCallbacks(mScheduleHomeRevealRunnable);
        }
        removeHomeRevealPreDrawListener();
        removeHomeReadyPreDrawListener();
        restoreHomeTransform();
    }

    private void restoreHomeTransform() {
        if (mHomeRoot == null) return;
        mHomeRoot.setScaleX(1f);
        mHomeRoot.setScaleY(1f);
        mHomeRoot.setAlpha(1f);
        mHomeRoot.setLayerType(mHomeRootLayerType, null);
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
        setIntent(intent);
        if (intent.getBooleanExtra(OobeTransitionHelper.EXTRA_HOME_REVEAL, false)) {
            intent.removeExtra(OobeTransitionHelper.EXTRA_HOME_REVEAL);
            mCheckVersionAfterReveal = true;
            prepareHomeReveal();
            if (mHomeRoot != null) {
                mHomeRoot.removeCallbacks(mScheduleHomeRevealRunnable);
                mHomeRoot.postOnAnimation(mScheduleHomeRevealRunnable);
            }
        }
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
        cancelHomeReveal();
        super.onDestroy();
    }

    private void showUnsupportedVersionDialog() {
        String versionText = getString(
            R.string.homepage_unsupported_version_current,
            getAndroidVersion(),
            formatHyperOsVersion()
        );
        String supportedVersionText = getVersionListText(SUPPORT_FULL);
        String partialSupportedVersionText = getVersionListText(SUPPORT_PARTIAL);
        if (!partialSupportedVersionText.isEmpty()) {
            supportedVersionText = supportedVersionText.isEmpty()
                ? partialSupportedVersionText
                : supportedVersionText + "\n" + partialSupportedVersionText;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(R.string.warn)
            .setMessage(getString(R.string.homepage_unsupported_version_message, versionText, supportedVersionText))
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
