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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.app;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Field;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class AppDefaultSort extends BaseHook {

    private Class<?> mAppManagerCls;
    private Class<?> mFragCls;
    private boolean mFragHooked;

    @Override
    public void init() {
        mAppManagerCls = findClassIfExists("com.miui.appmanager.AppManagerMainActivity");
        if (mAppManagerCls == null) return;

        findAndHookMethod(mAppManagerCls, "onCreate", Bundle.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.getArgs()[0] = checkBundle((Bundle) param.getArgs()[0]);
                hookFragmentOnce(param.getThisObject());
            }
        });
    }

    /**
     * Fragment 类只解析一次、{@code onActivityCreated} 也只挂一次。
     * 原实现每次 Activity onCreate 都重新 hookAllMethods，会让同一个 Fragment 方法被叠加 hook 多次。
     */
    private void hookFragmentOnce(Object activity) {
        if (mFragHooked) return;

        Class<?> fragCls = resolveFragmentClass(activity);
        if (fragCls == null) return;

        mFragCls = fragCls;
        hookAllMethods(fragCls, "onActivityCreated", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                try {
                    param.getArgs()[0] = checkBundle((Bundle) param.getArgs()[0]);
                } catch (Throwable t) {
                    XposedLog.e(TAG, getPackageName(), "", t);
                }
            }
        });
        mFragHooked = true;
    }

    private Class<?> resolveFragmentClass(Object activity) {
        Class<?> mFragXCls = findClassIfExists("androidx.fragment.app.Fragment");
        Field[] fields = activity.getClass().getDeclaredFields();
        for (Field field : fields) {
            Class<?> type = field.getType();
            if (Fragment.class.isAssignableFrom(type)
                || (mFragXCls != null && mFragXCls.isAssignableFrom(type))) {
                return type;
            }
        }
        return null;
    }

    public static Bundle checkBundle(Bundle bundle) {
        if (bundle == null) bundle = new Bundle();
        int order = PrefsBridge.getStringAsInt("security_center_app_default_sort", 0);
        bundle.putInt("current_sory_type", order - 1);
        bundle.putInt("current_sort_type", order - 1);
        return bundle;
    }
}
