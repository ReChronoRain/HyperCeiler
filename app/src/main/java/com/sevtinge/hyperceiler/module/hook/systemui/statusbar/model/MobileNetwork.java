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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;
import android.widget.TextView;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class MobileNetwork extends BaseHook {
    Class<?> mStatusBarMobileView;
    Class<?> mMobileIconState;
    Class<?> mHDController;

    @Override
    public void init() {
        mStatusBarMobileView = findClassIfExists("com.android.systemui.statusbar.StatusBarMobileView");
        mMobileIconState = findClassIfExists("com.android.systemui.statusbar.phone.StatusBarSignalPolicy$MobileIconState");

        mHDController = findClassIfExists("com.android.systemui.statusbar.policy.HDController");

        try {
            mStatusBarMobileView.getDeclaredMethod("initViewState", mMobileIconState);
            findAndHookMethod(mStatusBarMobileView, "initViewState", mMobileIconState, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    updateIconState(param, "mSmallHd", "system_ui_status_bar_icon_small_hd");
                    updateIconState(param, "mVolte", "system_ui_status_bar_icon_big_hd");
                }
            });
        } catch (NoSuchMethodException e) {
            try {
                mStatusBarMobileView.getDeclaredMethod("applyMobileState", mMobileIconState);
                findAndHookMethod(mStatusBarMobileView, "applyMobileState", mMobileIconState, new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            updateIconState(param, "mSmallHd", "system_ui_status_bar_icon_small_hd");
                            updateIconState(param, "mVolte", "system_ui_status_bar_icon_big_hd");
                        }
                    }
                );
            } catch (NoSuchMethodException f) {
                logE(TAG, "initViewState and applyMobileState dont have");
            }
        }

        findAndHookMethod(mStatusBarMobileView, "updateState", mMobileIconState, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                updateIconState(param, "mSmallHd", "system_ui_status_bar_icon_small_hd");
                updateIconState(param, "mVolte", "system_ui_status_bar_icon_big_hd");
            }
        });

        hookAllMethods(mStatusBarMobileView, "applyMobileState", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                int qpt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_mobile_network_type", 0);
                boolean singleMobileType = mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable");
                boolean hideIndicator = mPrefsMap.getBoolean("system_ui_status_bar_mobile_indicator");
                View mMobileType = getMobileType(param, qpt, singleMobileType);
                // 隐藏移动网络活动指示器
                View mLeftInOut = (View) XposedHelpers.getObjectField(param.thisObject, "mLeftInOut");
                if (hideIndicator) {
                    View mRightInOut = (View) XposedHelpers.getObjectField(param.thisObject, "mRightInOut");
                    mLeftInOut.setVisibility(View.GONE);
                    mRightInOut.setVisibility(View.GONE);
                }
                if (mMobileType.getVisibility() == View.GONE && mLeftInOut.getVisibility() == View.GONE) {
                    View mMobileLeftContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mMobileLeftContainer");
                    mMobileLeftContainer.setVisibility(View.GONE);
                }
            }
        });

        findAndHookMethod(mHDController, "update", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                int opt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_new_hd", 0);
                if (opt > 0 && !isMoreHyperOSVersion(1f)) {
                    XposedHelpers.setBooleanField(param.thisObject, "mWifiAvailable", opt == 2);
                }
            }
        });


        // 信号
        /*hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", "applyMobileState", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.callMethod(param.thisObject, "setVisibility", View.GONE);
            }
        });*/
    }

    private static View getMobileType(MethodHookParam param, int qpt, boolean singleMobileType) {
        View mMobileType = (View) XposedHelpers.getObjectField(param.thisObject, "mMobileType");
        boolean dataConnected = (boolean) XposedHelpers.getObjectField(param.args[0], "dataConnected");

        if (qpt > 0) {
            TextView mMobileTypeSingle = singleMobileType ? (TextView) XposedHelpers.getObjectField(param.thisObject, "mMobileTypeSingle") : null;
            int visibility = (qpt == 1 || (qpt == 3 && !dataConnected)) ? View.VISIBLE : View.GONE;

            if (singleMobileType && mMobileTypeSingle != null) {
                mMobileTypeSingle.setVisibility(visibility);
            } else {
                mMobileType.setVisibility(visibility);
            }
        }
        return mMobileType;
    }

    private void updateIconState(MethodHookParam param, String fieldName, String key) {
        boolean isMobileConnected = false;
        int opt = mPrefsMap.getStringAsInt(key, 0);
        if (opt != 0) {
            View view = (View) XposedHelpers.getObjectField(param.thisObject, fieldName);
            switch (opt) {
                case 1 -> view.setVisibility(View.VISIBLE);
                case 2 -> view.setVisibility(View.GONE);
                case 3 -> {
                    view.setVisibility(View.GONE);
                    isMobileConnected = (boolean) XposedHelpers.getObjectField(param.args[0], "dataConnected");
                }
            }

            if (isMobileConnected) {
                view.setVisibility(View.VISIBLE);
            }
        }

    }
}
