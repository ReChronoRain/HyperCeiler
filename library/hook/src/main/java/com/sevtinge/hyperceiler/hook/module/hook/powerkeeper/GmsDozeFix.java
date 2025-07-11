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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.hook.powerkeeper;

import android.content.Context;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.lang.reflect.Field;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class GmsDozeFix extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Class<?> GmsObserver = findClassIfExists("com.miui.powerkeeper.utils.GmsObserver", lpparam.classLoader);
        findAndHookMethod(GmsObserver, "initGmsControl", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(null);
            }
        });

        findAndHookMethod(GmsObserver, "updateGmsNetWork", boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(null);
            }
        });

        findAndHookMethod(GmsObserver, "onGoogleReachabilityChanged", boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.args[0] = true;
            }
        });

        try {
            Class<?> MilletPolicy = findClass("com.miui.powerkeeper.millet.MilletPolicy", lpparam.classLoader);
            findAndHookConstructor("com.miui.powerkeeper.millet.MilletPolicy", Context.class, new MethodHook() {
                protected void before(MethodHookParam methodHookParam) throws Throwable {
                    super.after(methodHookParam);
                    boolean mSystemBlackList = false;
                    boolean whiteApps = false;
                    boolean mDataWhiteList = false;

                    for (Field field : MilletPolicy.getDeclaredFields()) {
                        if (field.getName().equals("mSystemBlackList")) {
                            mSystemBlackList = true;
                        } else if (field.getName().equals("whiteApps")) {
                            whiteApps = true;
                        } else if (field.getName().equals("mDataWhiteList")) {
                            mDataWhiteList = true;
                        }
                    }

                    if (mSystemBlackList) {
                        List blackList = (List) XposedHelpers.getObjectField(methodHookParam.thisObject, "mSystemBlackList");
                        blackList.remove("com.google.android.gms");
                        XposedHelpers.setObjectField(methodHookParam.thisObject, "mSystemBlackList", blackList);
                    }
                    if (whiteApps) {
                        List whiteAppList = (List) XposedHelpers.getObjectField(methodHookParam.thisObject, "whiteApps");
                        whiteAppList.remove("com.google.android.gms");
                        whiteAppList.remove("com.google.android.ext.services");
                        XposedHelpers.setObjectField(methodHookParam.thisObject, "whiteApps", whiteAppList);
                    }
                    if (mDataWhiteList) {
                        List dataWhiteList = (List) XposedHelpers.getObjectField(methodHookParam.thisObject, "mDataWhiteList");
                        dataWhiteList.add("com.google.android.gms");

                        XposedHelpers.setObjectField(methodHookParam.thisObject, "mDataWhiteList", dataWhiteList);
                    }

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            logE(TAG, this.lpparam.packageName, "Hook failed: "+ e);
        }
    }
}
