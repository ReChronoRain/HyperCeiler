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

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class MiuiAppNoOverScroll extends BaseHook {


    @Override
    public void init() {

        Class<?> mSpringBackCls = findClassIfExists("miuix.springback.view.SpringBackLayout");
        Class<?> mRemixRvCls = findClassIfExists("androidx.recyclerview.widget.RemixRecyclerView");

        try {
            MethodHook hookParam = new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    XposedHelpers.setBooleanField(param.thisObject, "mSpringBackEnable", false);
                    param.args[0] = false;
                }
            };

            if (mSpringBackCls != null) {

                hookAllConstructors(mSpringBackCls, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        XposedHelpers.setBooleanField(param.thisObject, "mSpringBackEnable", false);
                    }
                });

                findAndHookMethodSilently(mSpringBackCls, "setSpringBackEnable", boolean.class, hookParam);
            }


            if (mRemixRvCls != null) {
                hookAllConstructors(mRemixRvCls, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        ((View) param.thisObject).setOverScrollMode(View.OVER_SCROLL_NEVER);
                        XposedHelpers.setBooleanField(param.thisObject, "mSpringBackEnable", false);
                    }
                });
                findAndHookMethodSilently(mRemixRvCls, "setSpringEnabled", boolean.class, hookParam);
            }
        } catch (Exception e) {
            logE(TAG, "TAG" + lpparam.packageName, e);
        }
    }
}
