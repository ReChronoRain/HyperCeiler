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
package com.sevtinge.hyperceiler.module.hook.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.Context;
import android.os.Handler;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.PrefsChangeObserver;
import com.sevtinge.hyperceiler.utils.PrefsUtils;


public class MonetThemeOverlay extends BaseHook {

    Class<?> THEME_CLASS_AOSP;
    Context mContext;
    Handler mHandler;

    @Override
    public void init() {
        THEME_CLASS_AOSP = findClassIfExists("com.android.systemui.theme.ThemeOverlayController");
        String mMethodName;
        if (isMoreAndroidVersion(34)) mMethodName ="createOverlays"; else mMethodName = "getOverlay";

        hookAllConstructors(THEME_CLASS_AOSP, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                mContext = (Context) param.args[0];
                /*mHandler = (Handler) param.args[2];*/
            }
        });

        hookAllMethods(THEME_CLASS_AOSP, mMethodName, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                mHandler = new Handler(mContext.getMainLooper());
                new PrefsChangeObserver(mContext, mHandler, "prefs_key_system_ui_monet_overlay_custom_color", -1) {
                    @Override
                    public void onChange(String name, int defValue) {
                        mPrefsMap.put(name, PrefsUtils.getSharedIntPrefs(mContext, name, defValue));
                        logI(TAG, MonetThemeOverlay.this.lpparam.packageName, name + "： " + PrefsUtils.getSharedIntPrefs(mContext, name, defValue));
                    }
                };

                param.args[0] = mPrefsMap.getInt("system_ui_monet_overlay_custom_color", -1);
            }
        });
    }
}
