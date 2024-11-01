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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui.fragment.main;

import static com.sevtinge.hyperceiler.utils.PropUtils.getProp;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getDeviceToken;
import static com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.dp2px;
import static com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.sp2px;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.expansionpacks.utils.ClickCountsUtils;
import com.sevtinge.hyperceiler.ui.MainActivityContextHelper;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.ActionBarUtils;
import com.sevtinge.hyperceiler.view.BgEffectPainter;
import com.sevtinge.hyperceiler.widget.VersionCard;

import java.lang.reflect.Field;

import fan.appcompat.app.ActionBar;
import fan.appcompat.app.Fragment;
import fan.core.widget.NestedScrollView;
import fan.preference.PreferenceFragment;
import fan.springback.view.SpringBackLayout;

public class AboutFragment extends SettingsPreferenceFragment
        implements View.OnScrollChangeListener {

    private int lIIlIll = 100 >>> 7;
    private final int lIIlIlI = 100 >>> 6;

    private int scrollValue = 0;
    private boolean isFirst = true;

    private View mRootView;
    private View scrollLayout;
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
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private FrameLayout contentView;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null) {
            //updateFragmentView(container);
        }
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_about_page, container, false);
            ViewGroup prefsContainer = mRootView.findViewById(R.id.prefs_container);
            View view = super.onCreateView(inflater, container, savedInstanceState);
            setOverlayMode();
            prefsContainer.addView(view);

            RecyclerView listView = getListView();
            View parent = (View) listView.getParent();
            if (parent instanceof SpringBackLayout) {
                parent.setEnabled(false);
                listView.setPaddingRelative(listView.getPaddingStart(), 0, listView.getPaddingEnd(), 0);
            }

            scrollLayout = mRootView.findViewById(R.id.scroll_layout);
            mVersionCardView = mRootView.findViewById(R.id.version_card_view);

            mScrollView = mRootView.findViewById(R.id.scrollview);
            mSpringBackView = mRootView.findViewById(R.id.springview);

            registerCoordinateScrollView(scrollLayout);
            mScrollView.setOnScrollChangeListener(this);
            mSpringBackView.setOnScrollChangeListener(this);
            setShaderBackground();
        }
        return mRootView;
    }

    private void initCardView() {
        mVersionCardView.refreshUpdateStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        initCardView();
        /*ActionBar appCompatActionBar = getAppCompatActionBar();
        if (appCompatActionBar != null) {
            appCompatActionBar.getExpandTitle().setTitle("");
            appCompatActionBar.setExpandState(0);
            appCompatActionBar.setResizable(false);
            appCompatActionBar.getActionBarView().requestFocus();
            if (isFirst && mVersionCardView != null) {
                isFirst = false;
                mVersionCardView.mAnimationController.setActionBarAlpha(getAppCompatActionBar().getTitleView(0));
            }
        }
        adjustBackgroundForOverlay();*/
    }

    public void updateFragmentView(View view) {
        ViewGroup actionBarOverlayLayout = ActionBarUtils.getActionBarOverlayLayout(view);
        if (actionBarOverlayLayout != null) {
            actionBarOverlayLayout.setBackgroundResource(android.R.color.transparent);
        }
    }

    private void adjustBackgroundForOverlay() {
        if (getActivity() != null) {
            getActivity().findViewById(fan.appcompat.R.id.action_bar_overlay_layout);
        }
    }

    @Override
    public int getContentResId() {
        return R.xml.prefs_about;
    }

    private void setOverlayMode() {
        try {
            Field declaredField = PreferenceFragment.class.getDeclaredField("mIsOverlayMode");
            declaredField.setAccessible(true);
            declaredField.set(this, Boolean.FALSE);
        } catch (Exception e) {
            Log.e("AboutFragment", "declaredField", e);
        }
    }

    private void setShaderBackground() {
        setContentViewPadding();
        if (mBgEffectView == null) {
            mBgEffectView = LayoutInflater.from(getContext()).inflate(R.layout.layout_effect_bg, (ViewGroup) contentView, false);
            contentView.addView(mBgEffectView, 0);
            mBgEffectView = contentView.findViewById(R.id.bgEffectView);
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
        if (contentView == null && getActivity() != null) {
            contentView = mRootView.findViewById(R.id.fragment_container);
            contentView.setOnApplyWindowInsetsListener((v, insets) -> {
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
                mVersionCardView.setAnimation(scrollY, mBgEffectView);
            } else {
                if (v.getId() == R.id.springview && scrollY >= 0) {
                    mVersionCardView.setAnimation(scrollY + scrollValue, mBgEffectView);
                }
            }
        }
    }

    @Override
    public void initPrefs() {
        int lIIlllI = ClickCountsUtils.getClickCounts();
        MainActivityContextHelper mainActivityContextHelper = new MainActivityContextHelper(requireContext());
        Preference lIIllII = findPreference("prefs_key_various_enable_super_function");
        Preference mQQGroup = findPreference("prefs_key_about_join_qq_group");
        mDeviceName = findPreference("prefs_key_about_device_name");
        mDeviceInfoDevice = findPreference("prefs_key_about_device_info_device");
        mDeviceInfoAndroid = findPreference("prefs_key_about_device_info_android");
        mDeviceInfoOs = findPreference("prefs_key_about_device_info_os");
        mDeviceInfoPadding = findPreference("prefs_key_about_device_info_padding");
        mDeviceName.setTitle(getProp("persist.sys.device_name"));
        mDeviceInfoDevice.setTitle(getProp("ro.product.marketname"));
        mDeviceInfoAndroid.setTitle(getProp("ro.build.version.release"));
        mDeviceInfoOs.setTitle(getSystemVersionIncremental());
        mDeviceInfoPadding.setTitle(getDeviceToken(mainActivityContextHelper.getAndroidId()));

        if (lIIllII != null) {
            lIIllII.setTitle(BuildConfig.VERSION_NAME + " | " + BuildConfig.BUILD_TYPE);
            if (isMoreHyperOSVersion(1f)) lIIllII.setSummary(R.string.description_hyperos); else lIIllII.setSummary(R.string.description_miui);
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

        if (mQQGroup != null) {
            mQQGroup.setOnPreferenceClickListener(preference -> {
                joinQQGroup("MF68KGcOGYEfMvkV_htdyT6D6C13We_r");
                return true;
            });
        }
    }

    /**
     * 调用 joinQQGroup() 即可发起手Q客户端申请加群
     *
     * @param key 由官网生成的key
     */
    private void joinQQGroup(String key) {
        Intent intent = new Intent();
        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));

        try {
            startActivity(intent);
        } catch (Exception e) {
            // 未安装手Q或安装的版本不支持
        }
    }
}
