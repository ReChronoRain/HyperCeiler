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

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Field;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class AppDefaultSort extends BaseHook {

    Class<?> mAppManagerCls;
    String fragCls = null;

    @Override
    public void init() {

        mAppManagerCls = findClassIfExists("com.miui.appmanager.AppManagerMainActivity");

        findAndHookMethod(mAppManagerCls, "onCreate", Bundle.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[0] = checkBundle((Bundle) param.getArgs()[0]);
                Class<?> mFragXCls = findClassIfExists("androidx.fragment.app.Fragment");
                Field[] fields = param.getThisObject().getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (Fragment.class.isAssignableFrom(field.getType()) || (mFragXCls != null && mFragXCls.isAssignableFrom(field.getType()))) {
                        fragCls = field.getType().getCanonicalName();
                        break;
                    }
                }
                if (fragCls != null) {
                    hookAllMethods(fragCls, "onActivityCreated", new IMethodHook() {
                        @Override
                        public void before(final BeforeHookParam param) {
                            try {
                                param.getArgs()[0] = checkBundle((Bundle) param.getArgs()[0]);
                            } catch (Throwable t) {
                                XposedLog.e(TAG, getPackageName(), "", t);
                            }
                        }
                    });
                }
            }
        });
    }

    public static Bundle checkBundle(Bundle bundle) {
        if (bundle == null) bundle = new Bundle();
        int order = mPrefsMap.getStringAsInt("security_center_app_default_sort", 0);
        bundle.putInt("current_sory_type", order - 1);
        bundle.putInt("current_sort_type", order - 1);
        return bundle;
    }
}
