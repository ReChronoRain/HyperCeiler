package com.sevtinge.hyperceiler.module.hook.home.dock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.BlurUtils;
import com.sevtinge.hyperceiler.utils.DisplayUtils;

import de.robv.android.xposed.XposedHelpers;

public class DockCustom extends BaseHook {

    FrameLayout mDockView;

    boolean isFolderShowing;
    boolean isShowEditPanel;
    boolean isRecentShowing;

    Class<?> mLauncherCls;
    Class<?> mLauncherStateCls;
    Class<?> mDeviceConfigCls;
    Class<?> mFolderInfo;
    Class<?> mBlurUtils;

    @Override
    public void init() {
        mLauncherCls = findClassIfExists("com.miui.home.launcher.Launcher");
        mLauncherStateCls = findClassIfExists("com.miui.home.launcher.LauncherState");
        mDeviceConfigCls = findClassIfExists("com.miui.home.launcher.DeviceConfig");
        mFolderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo");
        mBlurUtils = findClassIfExists("com.miui.home.launcher.common.BlurUtils");


        findAndHookMethod(mLauncherCls, "onCreate", Bundle.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                Activity mActivity = (Activity) param.thisObject;

                FrameLayout mSearchBarContainer = (FrameLayout) XposedHelpers.callMethod(param.thisObject, "getSearchBarContainer");
                FrameLayout mSearchEdgeLayout = (FrameLayout) mSearchBarContainer.getParent();

                int mDockHeight = DisplayUtils.dip2px(mSearchBarContainer.getContext(), XposedInit.mPrefsMap.getInt("home_dock_bg_height", 80));
                int mDockMargin = DisplayUtils.dip2px(mSearchBarContainer.getContext(), XposedInit.mPrefsMap.getInt("home_dock_bg_margin_horizontal", 30));
                int mDockBottomMargin = DisplayUtils.dip2px(mSearchBarContainer.getContext(), XposedInit.mPrefsMap.getInt("home_dock_bg_margin_bottom", 30));

                mDockView = new FrameLayout(mSearchBarContainer.getContext());
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mDockHeight);
                layoutParams.gravity = Gravity.BOTTOM;
                layoutParams.setMargins(mDockMargin, 0, mDockMargin, mDockBottomMargin);
                mDockView.setLayoutParams(layoutParams);
                mSearchEdgeLayout.addView(mDockView, 0);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    new BlurUtils(mDockView, "home_dock_bg_custom");
                }


                findAndHookMethod(mLauncherCls, "isFolderShowing", new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        isFolderShowing = (boolean) param.getResult();
                    }
                });

                findAndHookMethod(mLauncherCls, "showEditPanel", boolean.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        isShowEditPanel = (boolean) param.args[0];
                        mDockView.setVisibility(isShowEditPanel ? View.GONE : View.VISIBLE);
                    }
                });

                findAndHookMethod(mLauncherCls, "openFolder", mFolderInfo, View.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        mDockView.setVisibility(View.GONE);
                    }
                });

                findAndHookMethod(mLauncherCls, "closeFolder", boolean.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        if (!isShowEditPanel) mDockView.setVisibility(View.VISIBLE);
                    }
                });

                findAndHookMethod(mBlurUtils, "fastBlurWhenEnterRecents", mLauncherCls, mLauncherStateCls, boolean.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        mDockView.setVisibility(View.GONE);
                    }
                });
            }
        });

        findAndHookMethod(mLauncherCls, "onStateSetStart", mLauncherStateCls, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                Boolean mLauncherState = param.args[0].getClass().getSimpleName().equals("LauncherState");
                Boolean mNormalState = param.args[0].getClass().getSimpleName().equals("NormalState");
                if ((mLauncherState || mNormalState) && !isFolderShowing && !isShowEditPanel) {
                    mDockView.setVisibility(View.VISIBLE);
                } else {
                    mDockView.setVisibility(View.GONE);
                }
            }
        });


        /*findAndHookMethod(mDeviceConfigCls,"calcHotSeatsMarginTop", Context.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                param.setResult(DisplayUtils.dip2px(context, XposedInit.mPrefsMap.getInt("home_dock_margin_top",25)));
            }
        });

        findAndHookMethod(mDeviceConfigCls,"calcHotSeatsMarginBottom", Context.class, boolean.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                param.setResult(DisplayUtils.dip2px(context, XposedInit.mPrefsMap.getInt("home_dock_icon_margin_bottom",90)));
            }
        });*/
    }


    public GradientDrawable getDockBackground(Context context) {
        GradientDrawable mDockBackground = new GradientDrawable();
        mDockBackground.setShape(GradientDrawable.RECTANGLE);
        mDockBackground.setColor(Color.argb(60, 255, 255, 255));
        mDockBackground.setCornerRadius(DisplayUtils.dip2px(context, 22));
        return mDockBackground;
    }

}
