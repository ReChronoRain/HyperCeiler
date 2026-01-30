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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.powerkeeper;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Field;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

;

public class GmsDozeFix extends BaseHook {
    @Override
    public void init() {
        Class<?> GmsObserver = findClassIfExists("com.miui.powerkeeper.utils.GmsObserver");

        findAndHookMethod(GmsObserver, "updateGmsNetWork", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[0] = true;
            }
        });

        findAndHookMethod(GmsObserver, "updateGmsAlarm", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[0] = true;
            }
        });

        findAndHookMethod(GmsObserver, "updateGoogleReletivesWakelock", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[0] = true;
            }
        });

        findAndHookMethod(GmsObserver, "updateGoogleSync", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[0] = true;
            }
        });

        findAndHookMethod(GmsObserver, "updateGoogleBackup", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[0] = true;
            }
        });

        findAndHookMethod(GmsObserver, "onGoogleReachabilityChanged", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[0] = true;
            }
        });

        try {
            Class<?> MilletPolicy = findClassIfExists("com.miui.powerkeeper.millet.MilletPolicy");
            if (MilletPolicy == null) return;
            findAndHookConstructor(MilletPolicy, Context.class, new IMethodHook() {
                public void before(BeforeHookParam methodHookParam) {
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
                        List blackList = (List) getObjectField(methodHookParam.getThisObject(), "mSystemBlackList");
                        blackList.remove("com.google.android.gms");
                        setObjectField(methodHookParam.getThisObject(), "mSystemBlackList", blackList);
                    }
                    if (whiteApps) {
                        List whiteAppList = (List) getObjectField(methodHookParam.getThisObject(), "whiteApps");
                        whiteAppList.remove("com.google.android.gms");
                        whiteAppList.remove("com.google.android.ext.services");
                        setObjectField(methodHookParam.getThisObject(), "whiteApps", whiteAppList);
                    }
                    if (mDataWhiteList) {
                        List dataWhiteList = (List) getObjectField(methodHookParam.getThisObject(), "mDataWhiteList");
                        dataWhiteList.add("com.google.android.gms");

                        setObjectField(methodHookParam.getThisObject(), "mDataWhiteList", dataWhiteList);
                    }

                }
            });
        } catch (Throwable e) {
            XposedLog.e(TAG, getPackageName(), "Hook failed: " + e);
        }
    }
}
