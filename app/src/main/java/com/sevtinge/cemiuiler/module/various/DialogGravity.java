package com.sevtinge.cemiuiler.module.various;

import android.content.Context;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.sevtinge.cemiuiler.XposedInit;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.BlurUtils;
import com.sevtinge.cemiuiler.utils.DisplayUtils;
import com.sevtinge.cemiuiler.utils.LogUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class DialogGravity extends BaseHook {

    public static Context mContext;
    public static View mParentPanel = null;

    Class<?> mDialogCls = XposedHelpers.findClassIfExists("miuix.appcompat.app.AlertController", lpparam.classLoader);
    Class<?> mDialogParentPanelCls = XposedHelpers.findClassIfExists("miuix.internal.widget.DialogParentPanel", lpparam.classLoader);

    List<Method> methodList = new LinkedList<>();

    @Override
    public void init() {

        if (mDialogCls != null) {
            boolean oldMethodFound = false;
            for (Method method : mDialogCls.getDeclaredMethods()) {
                if (method.getName().equals("setupDialogPanel")) oldMethodFound = true;
                methodList.add(method);
                LogUtils.log(method.getName());
            }

            int mDialogGravity = XposedInit.mPrefsMap.getStringAsInt("various_dialog_gravity", 0);

            int mDialogHorizontalMargin = XposedInit.mPrefsMap.getInt("various_dialog_horizontal_margin", 0);
            int mDialogBottomMargin = XposedInit.mPrefsMap.getInt("various_dialog_bottom_margin", 0);

            if (oldMethodFound) {

                findAndHookMethod(mDialogCls, "setupDialogPanel", Configuration.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");

                        mContext = mParentPanel.getContext();

                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mParentPanel.getLayoutParams();

                        if (mDialogGravity != 0) {

                            layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;

                            layoutParams.gravity = mDialogGravity == 1 ? Gravity.CENTER : Gravity.BOTTOM | Gravity.CENTER;

                            layoutParams.setMarginStart(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dip2px(mContext, mDialogHorizontalMargin));
                            layoutParams.setMarginEnd(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dip2px(mContext, mDialogHorizontalMargin));
                            layoutParams.bottomMargin = mDialogGravity == 1 ? 0 : DisplayUtils.dip2px(mContext, mDialogBottomMargin);
                        }

                        mParentPanel.setLayoutParams(layoutParams);

                        /*new BlurUtils(mParentPanel);*/
                        new BlurUtils(mParentPanel, "default");

                    }
                });

            }

            for (Method method : methodList) {
                if (Arrays.equals(method.getParameterTypes(), new Class[]{Configuration.class}) && method.getReturnType() == Void.TYPE && method.getModifiers() == 2 && method.getParameterCount() == 1) {
                    LogUtils.log("2222" + method.getName());
                    XposedHelpers.findAndHookMethod(mDialogCls, method.getName(), new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            Field field = XposedHelpers.findFirstFieldByExactType(mDialogCls, mDialogParentPanelCls);
                            mParentPanel = (View) field.get(param.thisObject);

                            mContext = mParentPanel.getContext();

                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mParentPanel.getLayoutParams();

                            if (mDialogGravity != 0) {
                                layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;

                                layoutParams.gravity = mDialogGravity == 1 ? Gravity.CENTER : Gravity.BOTTOM | Gravity.CENTER;

                                layoutParams.setMarginStart(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dip2px(mContext, mDialogHorizontalMargin));
                                layoutParams.setMarginEnd(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dip2px(mContext, mDialogHorizontalMargin));
                                layoutParams.bottomMargin = mDialogGravity == 1 ? 0 : DisplayUtils.dip2px(mContext, mDialogBottomMargin);
                            }

                            mParentPanel.setLayoutParams(layoutParams);

                            /*new BlurUtils(mParentPanel);*/
                            new BlurUtils(mParentPanel, "default");
                        }
                    });
                }
            }
        }

        hookAllMethods(mDialogCls, "dismiss", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                super.after(param);
                View mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");
                mParentPanel.setVisibility(View.INVISIBLE);
            }
        });
    }
}
