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
package com.sevtinge.hyperceiler.module.hook.various;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.blur.BlurUtils;
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

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
                protected void after(MethodHookParam param) {
                    Window mWindow = (Window) XposedHelpers.getObjectField(param.thisObject, "mWindow");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        mWindow.getAttributes().setBlurBehindRadius(mPrefsMap.getInt("various_dialog_window_blur_radius", 60)); // android.R.styleable.Window_windowBlurBehindRadius
                    }
                    mWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                }
            });
        }

        boolean oldMethodFound = false;
        if (mAlertControllerCls != null) {

            for (Method method : mAlertControllerCls.getDeclaredMethods()) {
                if (method.getName().equals("setupDialogPanel")) {
                    oldMethodFound = true;
                    logI(TAG, this.lpparam.packageName, method.getName());
                }
                mAllMethodList.add(method);
            }

            mDialogGravity = mPrefsMap.getStringAsInt("various_dialog_gravity", 0);
            mDialogHorizontalMargin = mPrefsMap.getInt("various_dialog_margin_horizontal", 0);
            mDialogBottomMargin = mPrefsMap.getInt("various_dialog_margin_bottom", 0);

        }

        if (oldMethodFound) {
            logI(TAG, this.lpparam.packageName, "oldMethod found.");

            findAndHookMethod(mAlertControllerCls, "setupDialogPanel", Configuration.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        new BlurUtils(mParentPanel, "various_dialog_bg_blur");
                    }
                }
            });

        } else {
            logI(TAG, this.lpparam.packageName, "oldMethod not found.");
            hookAllMethods(mAlertControllerCls, "updateDialogPanel", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        new BlurUtils(mParentPanel, "various_dialog_bg_blur");
                    }
                }
            });
        }

        try {
            hookAllMethods(mAlertControllerCls, "updateParentPanelMarginByWindowInsets", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");

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
            logE(TAG, this.lpparam.packageName, e);
        }


    }

}
