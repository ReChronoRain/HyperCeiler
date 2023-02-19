package com.sevtinge.cemiuiler.module.various;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.sevtinge.cemiuiler.XposedInit;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.BlurUtils;
import com.sevtinge.cemiuiler.utils.DisplayUtils;
import com.sevtinge.cemiuiler.utils.LogUtils;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class DialogCustom extends BaseHook {

    Context mContext;
    View mParentPanel = null;

    Class<?> mAlertControllerCls;
    Class<?> mDialogParentPanelCls;

    int mDialogGravity;
    int mDialogHorizontalMargin;
    int mDialogBottomMargin;

    @Override
    public void init() {

        if (lpparam.packageName.equals("com.miui.home")) {
            mAlertControllerCls = findClassIfExists("miui.home.lib.dialog.AlertController");
        } else {
            mAlertControllerCls = findClassIfExists("miuix.appcompat.app.AlertController");
        }
        mDialogParentPanelCls = findClassIfExists("miuix.internal.widget.DialogParentPanel");

        List<Method> mAllMethodList = new LinkedList<>();

        if (mPrefsMap.getBoolean("various_dialog_window_blur")) {
            hookAllConstructors(mAlertControllerCls, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Window mWindow = (Window) XposedHelpers.getObjectField(param.thisObject, "mWindow");
                    mWindow.getAttributes().setBlurBehindRadius(mPrefsMap.getInt("various_dialog_window_blur_radius", 60)); //android.R.styleable.Window_windowBlurBehindRadius
                    mWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                }
            });
        }

        if (mAlertControllerCls != null) {
            boolean oldMethodFound = false;

            for (Method method : mAlertControllerCls.getDeclaredMethods()) {
                if (method.getName().equals("setupDialogPanel")) {
                    oldMethodFound = true;
                    LogUtils.log(method.getName());
                }
                mAllMethodList.add(method);
            }

            mDialogGravity = XposedInit.mPrefsMap.getStringAsInt("various_dialog_gravity", 0);
            mDialogHorizontalMargin = XposedInit.mPrefsMap.getInt("various_dialog_margin_horizontal", 0);
            mDialogBottomMargin = XposedInit.mPrefsMap.getInt("various_dialog_margin_bottom", 0);

        }

        hookAllMethods(mAlertControllerCls,"updateDialogPanel", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");
                mContext = mParentPanel.getContext();

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mParentPanel.getLayoutParams();

                if (mDialogGravity != 0) {

                    layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;

                    layoutParams.gravity = mDialogGravity == 1 ? Gravity.CENTER : Gravity.BOTTOM|Gravity.CENTER;

                    layoutParams.setMarginStart(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dip2px(mContext,mDialogHorizontalMargin));
                    layoutParams.setMarginEnd(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dip2px(mContext,mDialogHorizontalMargin));
                    layoutParams.bottomMargin = mDialogGravity == 1 ? 0 : DisplayUtils.dip2px(mContext,mDialogBottomMargin);
                }

                mParentPanel.setLayoutParams(layoutParams);
                new BlurUtils(mParentPanel, "various_dialog_bg_blur");
            }
        });

        try {
            findAndHookMethod(mAlertControllerCls,"updateParentPanelMarginByWindowInsets", WindowInsets.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");

                    mContext = mParentPanel.getContext();
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mParentPanel.getLayoutParams();
                    if (mDialogGravity != 0) {
                        layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;

                        layoutParams.gravity = mDialogGravity == 1 ? Gravity.CENTER : Gravity.BOTTOM|Gravity.CENTER;

                        layoutParams.setMarginStart(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dip2px(mContext,mDialogHorizontalMargin));
                        layoutParams.setMarginEnd(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dip2px(mContext,mDialogHorizontalMargin));
                        layoutParams.bottomMargin = mDialogGravity == 1 ? 0 : DisplayUtils.dip2px(mContext,mDialogBottomMargin);
                    }
                    mParentPanel.setLayoutParams(layoutParams);

                }
            });
        } catch (Exception e) {
            LogUtils.logXp(TAG, e);
        }


    }

}
