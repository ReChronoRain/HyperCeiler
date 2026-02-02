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
package com.sevtinge.hyperceiler.libhook.rules.various.system;

import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class MiuiAppNoOverScroll extends BaseHook {


    @Override
    public void init() {

        Class<?> mSpringBackCls = findClassIfExists("miuix.springback.view.SpringBackLayout");
        Class<?> mRemixRvCls = findClassIfExists("androidx.recyclerview.widget.RemixRecyclerView");

        try {
            IMethodHook hookParam = new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    EzxHelpUtils.setBooleanField(param.getThisObject(), "mSpringBackEnable", false);
                    param.getArgs()[0] = false;
                }
            };

            if (mSpringBackCls != null) {

                hookAllConstructors(mSpringBackCls, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        EzxHelpUtils.setBooleanField(param.getThisObject(), "mSpringBackEnable", false);
                    }
                });

                findAndHookMethod(mSpringBackCls, "setSpringBackEnable", boolean.class, hookParam);
            }


            if (mRemixRvCls != null) {
                hookAllConstructors(mRemixRvCls, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        ((View) param.getThisObject()).setOverScrollMode(View.OVER_SCROLL_NEVER);
                        EzxHelpUtils.setBooleanField(param.getThisObject(), "mSpringBackEnable", false);
                    }
                });
                findAndHookMethod(mRemixRvCls, "setSpringEnabled", boolean.class, hookParam);
            }
        } catch (Exception e) {
            XposedLog.e(TAG, "TAG" + getPackageName(), e);
        }
    }
}
