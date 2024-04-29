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

        hideMobileHD(); // 隐藏小 HD、单卡 HD 以及双卡 HD
    }

    private void hideMobileHD() {
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

        findAndHookMethod(mHDController, "update", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                int opt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_new_hd", 0);
                if (opt > 0 && !isMoreHyperOSVersion(1f)) {
                    XposedHelpers.setBooleanField(param.thisObject, "mWifiAvailable", opt == 2);
                } else if (opt == 2 && isMoreHyperOSVersion(1f)) {
                    XposedHelpers.setBooleanField(param.thisObject, "mWifiAvailable", true);
                }
            }
        });
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
