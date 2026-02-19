package com.sevtinge.hyperceiler.ui.page;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.fan.common.base.BasePreferenceFragment;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.about.DeviceNameCard;
import com.sevtinge.hyperceiler.about.VersionCard;
import com.sevtinge.hyperceiler.about.VersionNameCard;
import com.sevtinge.hyperceiler.about.controller.BgEffectController;
import com.sevtinge.hyperceiler.common.utils.ActionBarUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import fan.animation.Folme;
import fan.animation.base.AnimConfig;
import fan.appcompat.app.ActionBar;
import fan.device.DeviceUtils;
import fan.internal.utils.ViewUtils;
import fan.os.Build;
import fan.preference.PreferenceFragment;
import fan.springback.view.SpringBackLayout;

public class AboutSettingsFragment extends BasePreferenceFragment
    implements View.OnScrollChangeListener {

    VersionCard mVersionCardView;
    DeviceNameCard mDeviceNameCardView;
    VersionNameCard mVersionNameCardView;

    private int scrollValue = 0;

    private boolean isFirst = true;
    private boolean isReboot = false;
    private boolean isRunning = false;

    private FrameLayout mContentView;

    private View mRootView;

    private View mBgEffectView;
    private BgEffectController mBgEffectController;

    private View mVersionCardClickView;

    private View mGridViewRoot;
    private SpringBackLayout SpringBack;
    private NestedScrollView scrollView;
    private View scrollLayout;

    private TextView noteLyout;

    private ViewUtils.RelativePadding mViewInitPadding;


    private List<View> mCards = new ArrayList<>();
    private Handler mHandler = new Handler(Looper.getMainLooper());


    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null) {
            updateFragmentView(container);
        }
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.about_settings, container, false);

            ViewGroup prefsContainer = mRootView.findViewById(R.id.prefs_container);
            View onCreateView = super.onCreateView(inflater, container, savedInstanceState);
            setOverlayMode();
            prefsContainer.addView(onCreateView);

            setRecyclerViewPadding();

            mGridViewRoot = mRootView.findViewById(R.id.device_params);
            mGridViewRoot.setVisibility(View.GONE);

            scrollLayout = mRootView.findViewById(R.id.scroll_layout);

            mVersionCardClickView = mRootView.findViewById(R.id.version_card_click_view);

            mVersionCardView = mRootView.findViewById(R.id.version_card_view);
            mVersionCardView.setCardClickView(mVersionCardClickView, getAppCompatActionBar().getActionBarView());

            mDeviceNameCardView = mRootView.findViewById(R.id.device_name_card_view);
            mVersionNameCardView = mRootView.findViewById(R.id.version_name_card_view);

            //noteLyout = view.findViewById(R.id.disclaimer);
            scrollView = mRootView.findViewById(R.id.scrollview);
            SpringBack = mRootView.findViewById(R.id.springview);

            registerCoordinateScrollView(mRootView);

            scrollView.setOnScrollChangeListener(this);
            SpringBack.setOnScrollChangeListener(this);

            setShaderBackGround();
        }
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = getListView();
        if (recyclerView != null) {
            recyclerView.setFocusableInTouchMode(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initCardView();
        ActionBar actionBar = getAppCompatActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.navigation_about_title);
            actionBar.getExpandTitle().setTitle("");
            actionBar.setExpandState(0);
            actionBar.setResizable(false);
            actionBar.getActionBarView().requestFocus();

            if (isFirst && mVersionCardView != null) {
                isFirst = false;
                mVersionCardView.getAboutAnimationController().setActionBarAlpha(getAppCompatActionBar().getTitleView(0));
            }
        }
        if (mHandler != null && !isReboot) {
            startRuntimeShader();
        }
        isReboot = false;
        setShadowEffect();
    }

    @Override
    public void onResume() {
        super.onResume();

        //adjustBackgroundForOverlay();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setActionBar();
        startRuntimeShader();
        setRecyclerViewPadding();
    }

    @Override
    public void onContentInsetChanged(Rect rect) {
        super.onContentInsetChanged(rect);
        if (mRootView != null) {
            ViewUtils.RelativePadding relativePadding = new ViewUtils.RelativePadding(mViewInitPadding);
            boolean isLayoutRtl = ViewUtils.isLayoutRtl(mRootView);
            relativePadding.start += isLayoutRtl ? rect.right : rect.left;
            relativePadding.end += isLayoutRtl ? rect.left : rect.right;
            relativePadding.bottom = rect.top;
            relativePadding.applyToView(mRootView);
            setRecyclerViewPadding();
        }
    }

    public ActionBar getAppCompatActionBar() {
        Fragment parent = getParentFragment();
        if (parent instanceof fan.appcompat.app.Fragment fragment) {
            return fragment.getActionBar();
        } else {
            return null;
        }
    }

    public static void updateFragmentView(View view) {
        ViewGroup actionBarOverlayLayout = ActionBarUtils.getActionBarOverlayLayout(view);
        if (!Build.IS_TABLET && actionBarOverlayLayout != null) {
            actionBarOverlayLayout.setBackgroundResource(android.R.color.transparent);
        }
    }

    private void setOverlayMode() {
        try {
            Field declaredField = PreferenceFragment.class.getDeclaredField("mIsOverlayMode");
            declaredField.setAccessible(true);
            declaredField.set(this, Boolean.FALSE);
        } catch (Exception e) {
            Log.e("MiuiMyDeviceSettings", "declaredField", e);
        }
    }

    private void setActionBar() {
        ActionBar actionBar = getAppCompatActionBar();
        if (actionBar != null) {
            actionBar.getActionBarView().post(() -> {
                actionBar.getExpandTitle().setTitle("");
                actionBar.setExpandState(0);
                actionBar.setResizable(false);
            });
        }
    }

    public void setRecyclerViewPadding() {
        RecyclerView recyclerView = getListView();
        if (recyclerView != null) {
            View view = (View) recyclerView.getParent();
            if (view instanceof SpringBackLayout) {
                view.setEnabled(false);
                recyclerView.post(() -> {
                    if (recyclerView != null) {
                        setListViewPadding(recyclerView);
                    }
                });
            }
        }
    }

    private void setListViewPadding(RecyclerView recyclerView) {
        if (mGridViewRoot != null && mGridViewRoot.getVisibility() == View.GONE) {
            recyclerView.setPaddingRelative(
                recyclerView.getPaddingStart(),
                (int) (recyclerView.getPaddingTop() * 1.3f),
                recyclerView.getPaddingEnd(),
                (recyclerView.getPaddingBottom() / 3) + recyclerView.getResources().getDimensionPixelOffset(R.dimen.switch_view_height) + recyclerView.getResources().getDimensionPixelOffset(R.dimen.switch_view_margin_bottom)
            );
        } else {
            recyclerView.setPaddingRelative(recyclerView.getPaddingStart(), 0, recyclerView.getPaddingEnd(), 0);
        }
    }

    private void setShadowEffect() {
        float cardRadius = (float) getResources().getDimensionPixelSize(R.dimen.app_device_card_background_radius);
        for (int i = 0; i < mCards.size(); i++) {
            View view = mCards.get(i);
            if (i == 0) {
                applyShadowEffect(view, cardRadius, cardRadius, 0.0f, 0.0f);
            } if (i == mCards.size() - 1) {
                applyShadowEffect(view, 0.0f, 0.0f, cardRadius, cardRadius);
            } else {
                applyShadowEffect(view, 0.0f, 0.0f, 0.0f, 0.0f);
            }
        }
    }

    private void applyShadowEffect(View view, float leftTopRadius, float rightTopRadius, float leftBottomRadius, float rightBottomRadius) {
        Folme.useAt(view)
            .touch()
            .setTintMode(3)
            .setScale(1.0f)
            .setTouchRadius(leftTopRadius, rightTopRadius, leftBottomRadius, rightBottomRadius)
            .handleTouchOf(view, new AnimConfig[0]);
    }

    private void initCardView() {
        mCards.add(mDeviceNameCardView);
        mCards.add(mVersionNameCardView);
        mVersionCardView.refreshUpdateStatus(getAppCompatActionBar().getTitleView(0), mBgEffectView);
        mVersionCardView.refreshVersionName();
        mVersionCardView.setCardClickView(mVersionCardClickView, getAppCompatActionBar().getActionBarView());
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (mVersionCardView != null) {
            mVersionCardView.stopLogoAnimation();
            if (v.getId() == R.id.scrollview) {
                scrollValue = scrollY;
                mVersionCardView.setScrollValue(scrollY);
                mVersionCardView.setAnimation(scrollY, getAppCompatActionBar().getTitleView(0), mBgEffectView);
            } else {
                if (v.getId() == R.id.springview && scrollY >= 0) {
                    mVersionCardView.setScrollValue(scrollValue + scrollY);
                    mVersionCardView.setAnimation(scrollY + scrollValue, getAppCompatActionBar().getTitleView(0), mBgEffectView);
                }
            }
        }
    }

    private void setShaderBackGround() {
        mViewInitPadding = new ViewUtils.RelativePadding(
            ViewCompat.getPaddingStart(mRootView),
            mRootView.getPaddingTop(),
            ViewCompat.getPaddingEnd(mRootView),
            mRootView.getPaddingBottom()
        );
        setContentViewPadding();
        if (mBgEffectView == null) {
            mBgEffectView = LayoutInflater.from(getContext()).inflate(R.layout.app_about_bg, (ViewGroup) mContentView, false);
            mContentView.addView(mBgEffectView, 0);
            mBgEffectView = mContentView.findViewById(R.id.bgEffectView);
            mBgEffectController = new BgEffectController(mBgEffectView);
        }
        startRuntimeShader();
    }

    private void setContentViewPadding() {
        Fragment parent = getParentFragment();
        if (mContentView == null && parent != null) {
            View parentRootView = parent.getView();
            if (parentRootView != null) {
                mContentView = parentRootView.findViewById(R.id.content);
                mContentView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @NonNull
                    @Override
                    public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                        v.setPadding(0, 0, 0, 0);
                        return insets;
                    }
                });
            }
        }
    }

    private void startRuntimeShader() {
        if (mBgEffectView != null) {
            if (!DeviceUtils.isMiuiLiteRom()) {
                mBgEffectView.post(() -> {
                    if (getContext() != null) {
                        mBgEffectController.start();
                        mBgEffectController.setType(getContext().getApplicationContext(), mBgEffectView, getAppCompatActionBar());
                    }
                });
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mRootView != null) {
            unregisterCoordinateScrollView(mRootView);
        }
        mRootView = null;
    }

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_about;
    }
}
