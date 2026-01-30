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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setBooleanField;

import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

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
            findAndHookMethod(mStatusBarMobileView, "initViewState", mMobileIconState, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    updateIconState(param, "mSmallHd", "system_ui_status_bar_icon_small_hd");
                    updateIconState(param, "mVolte", "system_ui_status_bar_icon_big_hd");
                }
            });
        } catch (NoSuchMethodException e) {
            try {
                mStatusBarMobileView.getDeclaredMethod("applyMobileState", mMobileIconState);
                findAndHookMethod(mStatusBarMobileView, "applyMobileState", mMobileIconState, new IMethodHook() {
                        @Override
                        public void after(AfterHookParam param) {
                            updateIconState(param, "mSmallHd", "system_ui_status_bar_icon_small_hd");
                            updateIconState(param, "mVolte", "system_ui_status_bar_icon_big_hd");
                        }
                    }
                );
            } catch (NoSuchMethodException f) {
                XposedLog.e(TAG, getPackageName(), "initViewState and applyMobileState dont have");
            }
        }

        findAndHookMethod(mStatusBarMobileView, "updateState", mMobileIconState, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                updateIconState(param, "mSmallHd", "system_ui_status_bar_icon_small_hd");
                updateIconState(param, "mVolte", "system_ui_status_bar_icon_big_hd");
            }
        });

        findAndHookMethod(mHDController, "update", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                int opt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_new_hd", 0);
                if (opt == 2) {
                    setBooleanField(param.getThisObject(), "mWifiAvailable", true);
                }
            }
        });
    }

    private void updateIconState(AfterHookParam param, String fieldName, String key) {
        boolean isMobileConnected = false;
        int opt = mPrefsMap.getStringAsInt(key, 0);
        if (opt != 0) {
            View view = (View) getObjectField(param.getThisObject(), fieldName);
            switch (opt) {
                case 1 -> view.setVisibility(View.VISIBLE);
                case 2 -> view.setVisibility(View.GONE);
                case 3 -> {
                    view.setVisibility(View.GONE);
                    isMobileConnected = (boolean) getObjectField(param.getArgs()[0], "dataConnected");
                }
            }

            if (isMobileConnected) {
                view.setVisibility(View.VISIBLE);
            }
        }
    }
}
