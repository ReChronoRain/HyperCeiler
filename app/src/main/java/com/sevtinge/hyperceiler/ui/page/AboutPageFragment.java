/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui.page;

import static com.sevtinge.hyperceiler.hook.utils.PropUtils.getProp;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getDeviceToken;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getSystemVersionIncremental;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.common.utils.MainActivityContextHelper;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.ContentFragment.IFragmentChange;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.expansion.utils.ClickCountsUtils;
import com.sevtinge.hyperceiler.ui.page.about.view.BgEffectPainter;
import com.sevtinge.hyperceiler.ui.page.about.widget.VersionCard;
import com.sevtinge.hyperceiler.common.utils.ActionBarUtils;
import com.sevtinge.hyperceiler.widget.ListContainerView;
import com.sevtinge.hyperceiler.widget.VersionCardClickView;

import java.util.Objects;

import fan.appcompat.app.ActionBar;
import fan.appcompat.app.Fragment;
import fan.appcompat.internal.app.widget.ActionBarImpl;
import fan.appcompat.internal.app.widget.ActionBarOverlayLayout;
import fan.core.widget.NestedScrollView;
import fan.navigator.NavigatorFragmentListener;
import fan.springback.view.SpringBackLayout;

public class AboutPageFragment extends DashboardFragment
        implements View.OnScrollChangeListener, NavigatorFragmentListener, IFragmentChange {

    private int lIIlIll = 100 >>> 7;
    private final int lIIlIlI = 100 >>> 6;

    private int scrollValue = 0;

    private ListContainerView mContainerView;
    private NestedScrollView mScrollView;
    private SpringBackLayout mSpringBackView;

    private VersionCard mVersionCardView;

    private Preference mDeviceName;
    private Preference mDeviceInfoDevice;
    private Preference mDeviceInfoAndroid;
    private Preference mDeviceInfoOs;
    private Preference mDeviceInfoPadding;

    private View mBgEffectView;
    private BgEffectPainter mBgEffectPainter;
    private float startTime = (float) System.nanoTime();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    Runnable runnableBgEffect = new Runnable() {
        @Override
        public void run() {
            mBgEffectPainter.setAnimTime(((((float) System.nanoTime()) - startTime) / 1.0E9f) % 62.831852f);
            mBgEffectPainter.setResolution(new float[]{mBgEffectView.getWidth(), mBgEffectView.getHeight()});
            mBgEffectPainter.updateMaterials();
            mBgEffectView.setRenderEffect(mBgEffectPainter.getRenderEffect());
            mHandler.postDelayed(runnableBgEffect, 16L);
        }
    };

    @Override
    public int getThemeRes() {
        return R.style.Theme_Navigator_ContentChild_About;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mContainerView == null) {
            mContainerView = new ListContainerView(requireContext());
            mContainerView.addPrefsContainer(super.onCreateView(inflater, container, savedInstanceState));
            setOverlayMode();

            RecyclerView listView = getListView();
            View parent = (View) listView.getParent();
            if (parent instanceof SpringBackLayout) {
                parent.setEnabled(false);
                listView.setPaddingRelative(listView.getPaddingStart(), 0, listView.getPaddingEnd(), 0);
            }

            mContainerView.addContainerView(new VersionCardClickView(requireContext()));

            mVersionCardView = new VersionCard(requireContext());
            mContainerView.addContentView(mVersionCardView);

            mScrollView = mContainerView.getNestedScrollView();
            mSpringBackView = mContainerView.getSpringBackLayout();

            registerCoordinateScrollView(mContainerView.getContentView());
            mScrollView.setOnScrollChangeListener(this);
            mSpringBackView.setOnScrollChangeListener(this);
            setShaderBackground();
        }
        return mContainerView;
    }

    private void initCardView() {
        mVersionCardView.refreshUpdateStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        initCardView();
    }

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_about;
    }

    private void setShaderBackground() {
        setContentViewPadding();
        if (mBgEffectView == null) {
            mBgEffectView = LayoutInflater.from(getContext()).inflate(R.layout.layout_effect_bg, mContainerView, false);
            mContainerView.addView(mBgEffectView, 0);
            mBgEffectView = mContainerView.findViewById(R.id.bgEffectView);
        }
        mBgEffectView.post(() -> {
            if (getContext() != null) {
                mBgEffectPainter = new BgEffectPainter(getContext().getApplicationContext());
                mBgEffectPainter.showRuntimeShader(getContext().getApplicationContext(),
                        mBgEffectView, getAppCompatActionBar());

                mHandler.post(runnableBgEffect);
            }
        });
    }

    private void setContentViewPadding() {
        if (mContainerView == null && getActivity() != null) {
            //contentView = mContainerView.findViewById(R.id.fragment_container);
            mContainerView.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(0, 0, 0, 0);
                return insets;
            });
        }
    }

    public ActionBar getAppCompatActionBar() {
        if (getParentFragment() instanceof Fragment) {
            return ((Fragment) getParentFragment()).getActionBar();
        }
        return null;
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (mVersionCardView != null) {
            mVersionCardView.stopLogoAnimation();
            if (v.getId() == R.id.scrollview) {
                scrollValue = scrollY;
                mVersionCardView.setScrollValue(scrollY);
                mVersionCardView.setAnimation(scrollY, mBgEffectView, getAppCompatActionBar().getTitleView(0));
            } else {
                if (v.getId() == R.id.springview && scrollY >= 0) {
                    mVersionCardView.setAnimation(scrollY + scrollValue, mBgEffectView, getAppCompatActionBar().getTitleView(0));
                }
            }
        }
    }

    @Override
    public void initPrefs() {
        int lIIlllI = ClickCountsUtils.getClickCounts();
        MainActivityContextHelper mainActivityContextHelper = new MainActivityContextHelper(requireContext());
        Preference lIIllII = findPreference("prefs_key_various_enable_super_function");
        mDeviceName = findPreference("prefs_key_about_device_name");
        mDeviceInfoDevice = findPreference("prefs_key_about_device_info_device");
        mDeviceInfoAndroid = findPreference("prefs_key_about_device_info_android");
        mDeviceInfoOs = findPreference("prefs_key_about_device_info_os");
        mDeviceInfoPadding = findPreference("prefs_key_about_device_info_padding");
        String deviceName = getProp("persist.sys.device_name");
        String marketName = getProp("ro.product.marketname");
        String androidVersion = getProp("ro.build.version.release");
        String osVersion = getSystemVersionIncremental();
        if (Objects.equals(marketName, "")) marketName = android.os.Build.MODEL;
        if (Objects.equals(deviceName, "")) deviceName = marketName;
        if (Objects.equals(osVersion, "")) osVersion = androidVersion;
        mDeviceName.setTitle(deviceName);
        mDeviceInfoDevice.setTitle(marketName);
        mDeviceInfoAndroid.setTitle(androidVersion);
        mDeviceInfoOs.setTitle(osVersion);
        mDeviceInfoPadding.setTitle(getDeviceToken(mainActivityContextHelper.getAndroidId()));

        if (lIIllII != null) {
            lIIllII.setTitle(BuildConfig.VERSION_NAME + " | " + BuildConfig.BUILD_TYPE);
            lIIllII.setSummary(R.string.description_hyperos);
            lIIllII.setOnPreferenceClickListener(lIIllll -> {
                if (lIIllll instanceof SwitchPreference switchPreference) {
                    switchPreference.setChecked(!switchPreference.isChecked());
                    lIIlIll++;

                    if (switchPreference.isChecked()) {
                        if (lIIlIll >= lIIlIlI) {
                            switchPreference.setChecked(!switchPreference.isChecked());
                            lIIlIll = 100 >>> 8;
                        }
                    } else if (lIIlIll >= lIIlllI) {
                        switchPreference.setChecked(!switchPreference.isChecked());
                        lIIlIll = 100 >>> 8;
                    }
                }
                return false;
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mHandler != null) mHandler.removeCallbacks(runnableBgEffect);
        if (mContainerView != null) unregisterCoordinateScrollView(mContainerView.getNestedHeader());
        mContainerView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onEnter(ActionBar actionBar) {
        if (actionBar != null) {
            actionBar.getExpandTitle().setTitle("");
            actionBar.getActionBarView().requestFocus();
            if (mVersionCardView != null) {
                mVersionCardView.mAnimationController.setActionBarAlpha(getAppCompatActionBar().getTitleView(0));
            }
            resetActionBar(actionBar, false);
        }
    }

    @Override
    public void onLeave(ActionBar actionBar) {
        resetActionBar(actionBar, true);
    }

    public void resetActionBar(ActionBar actionBar, boolean resizable) {
        if (actionBar != null) {
            if (resizable) {
                actionBar.setResizable(true);
                actionBar.getTitleView(0).setAlpha(1.0f);
                //setActionBarBlur(actionBar, true);
            } else {
                actionBar.setExpandState(0);
                actionBar.setResizable(false);
                setActionBarBlur(actionBar, false);
            }
        }
    }

    private void setActionBarBlur(ActionBar actionBar, boolean blur) {
        ((ActionBarImpl) actionBar).getActionBarContainer().setActionBarBlur(blur);
    }
}
