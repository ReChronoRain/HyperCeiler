/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.various.dialog;

import android.content.Context;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.BlurUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

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

        if (getPackageName().equals("com.miui.home")) {
            mAlertControllerCls = findClassIfExists("miui.home.lib.dialog.AlertController");
        } else {
            mAlertControllerCls = findClassIfExists("miuix.appcompat.app.AlertController");
        }
        mDialogParentPanelCls = findClassIfExists("miuix.internal.widget.DialogParentPanel");

        List<Method> mAllMethodList = new LinkedList<>();

        if (mPrefsMap.getBoolean("various_dialog_window_blur")) {
            hookAllConstructors(mAlertControllerCls, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    Window mWindow = (Window) getObjectField(param.getThisObject(), "mWindow");
                    mWindow.getAttributes().setBlurBehindRadius(mPrefsMap.getInt("various_dialog_window_blur_radius", 60)); // android.R.styleable.Window_windowBlurBehindRadius
                    mWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                }
            });
        }

        boolean oldMethodFound = false;
        if (mAlertControllerCls != null) {

            for (Method method : mAlertControllerCls.getDeclaredMethods()) {
                if (method.getName().equals("setupDialogPanel")) {
                    oldMethodFound = true;
                    XposedLog.i(TAG, getPackageName(), method.getName());
                }
                mAllMethodList.add(method);
            }

            mDialogGravity = mPrefsMap.getStringAsInt("various_dialog_gravity", 0);
            mDialogHorizontalMargin = mPrefsMap.getInt("various_dialog_margin_horizontal", 0);
            mDialogBottomMargin = mPrefsMap.getInt("various_dialog_margin_bottom", 0);

        }

        if (oldMethodFound) {
            XposedLog.i(TAG, getPackageName(), "oldMethod found.");

            findAndHookMethod(mAlertControllerCls, "setupDialogPanel", Configuration.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    mParentPanel = (View) getObjectField(param.getThisObject(), "mParentPanel");
                    mContext = mParentPanel.getContext();
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mParentPanel.getLayoutParams();
                    if (mDialogGravity != 0) {
                        layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
                        layoutParams.gravity = mDialogGravity == 1 ? Gravity.CENTER : Gravity.BOTTOM | Gravity.CENTER;
                        layoutParams.setMarginStart(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dp2px(mDialogHorizontalMargin));
                        layoutParams.setMarginEnd(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dp2px(mDialogHorizontalMargin));
                        layoutParams.bottomMargin = mDialogGravity == 1 ? 0 : DisplayUtils.dp2px(mDialogBottomMargin);
                    }
                    mParentPanel.setLayoutParams(layoutParams);
                    new BlurUtils(mParentPanel, "various_dialog_bg_blur");
                }
            });

        } else {
            XposedLog.i(TAG, getPackageName(), "oldMethod not found.");
            hookAllMethods(mAlertControllerCls, "updateDialogPanel", new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    mParentPanel = (View) getObjectField(param.getThisObject(), "mParentPanel");
                    mContext = mParentPanel.getContext();
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mParentPanel.getLayoutParams();
                    if (mDialogGravity != 0) {
                        layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
                        layoutParams.gravity = mDialogGravity == 1 ? Gravity.CENTER : Gravity.BOTTOM | Gravity.CENTER;
                        layoutParams.setMarginStart(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dp2px(mDialogHorizontalMargin));
                        layoutParams.setMarginEnd(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dp2px(mDialogHorizontalMargin));
                        layoutParams.bottomMargin = mDialogGravity == 1 ? 0 : DisplayUtils.dp2px(mDialogBottomMargin);
                    }
                    mParentPanel.setLayoutParams(layoutParams);
                    new BlurUtils(mParentPanel, "various_dialog_bg_blur");
                }
            });
        }

        try {
            hookAllMethods(mAlertControllerCls, "updateParentPanelMarginByWindowInsets", new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    mParentPanel = (View) getObjectField(param.getThisObject(), "mParentPanel");

                    mContext = mParentPanel.getContext();
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mParentPanel.getLayoutParams();
                    if (mDialogGravity != 0) {
                        layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;

                        layoutParams.gravity = mDialogGravity == 1 ? Gravity.CENTER : Gravity.BOTTOM | Gravity.CENTER;

                        layoutParams.setMarginStart(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dp2px(mDialogHorizontalMargin));
                        layoutParams.setMarginEnd(mDialogHorizontalMargin == 0 ? 0 : DisplayUtils.dp2px( mDialogHorizontalMargin));
                        layoutParams.bottomMargin = mDialogGravity == 1 ? 0 : DisplayUtils.dp2px(mDialogBottomMargin);
                    }
                    mParentPanel.setLayoutParams(layoutParams);

                }
            });
        } catch (Exception e) {
            XposedLog.e(TAG, getPackageName(), e);
        }


    }

}
