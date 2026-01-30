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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * @author 焕晨HChen
 */
public class ScLockApp extends BaseHook {
    boolean isListen = false;
    boolean isLock = false;

    int value = 0;

    @Override
    public void init() {
        Method method = DexKit.findMember("StartRegionSampling", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("startRegionSampling")
                                )
                                .name("dispatchTouchEvent")
                        )).singleOrNull();
                return methodData;
            }
        });
        Class<?> clazz = DexKit.findMember("StartRegionSamplingClazz", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create()
                                .usingStrings("startRegionSampling")
                        )).singleOrNull();
                return clazzData;
            }
        });
        Field field = null;
        if (method == null) {
            value = 1;
            method = DexKit.findMember("SidebarTouchListener", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    MethodData methodData = bridge.findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                    .declaredClass(ClassMatcher.create()
                                            .usingStrings("SidebarTouchListener")
                                    )
                                    .name("onTouch")
                            )).singleOrNull();
                    return methodData;
                }
            });
            clazz = DexKit.findMember("OnTouchClazz", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    ClassData clazzData = bridge.findClass(FindClass.create()
                            .matcher(ClassMatcher.create()
                                    .usingStrings("onTouch: \taction = ")
                            )).singleOrNull();
                    return clazzData;
                }
            });

       DexKit.findMember("OnTouchField", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    return bridge.findField(FindField.create()
                            .matcher(FieldMatcher.create()
                                    .declaredClass(ClassMatcher.create()
                                            .usingStrings("onTouch: \taction = ")
                                    )
                                    .type(View.class)
                            )).singleOrNull();
                }
            });
        }
        // XposedLog.e(TAG, "dispatchTouchEvent: " + methodData + " Constructor: " + data + " class: " + data.getInstance(lpparam.classLoader) + " f: " + fieldData.getFieldInstance(lpparam.classLoader));
        if (clazz == null) {
            XposedLog.e(TAG, "Class is null");
            return;
        }
        if (field == null && value == 1) {
            XposedLog.e(TAG, "Field is null");
            return;
        }
        // XposedLog.e(TAG, "data: " + data + " fieldData: " + fieldData + " methodData: " + methodData);
        Field finalField = field;
        hookAllConstructors(clazz, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Context context = null;
                if (value == 1) {
                    try {
                        if (finalField == null) {
                            XposedLog.e(TAG, "finalField is null!");
                            return;
                        }
                        context = ((View) finalField.get(param.getThisObject())).getContext();
                    } catch (IllegalAccessException e) {
                        XposedLog.e(TAG, "getContext E: " + e);
                    }
                } else {
                    context = (Context) param.getArgs()[0];
                }
                if (context == null) {
                    XposedLog.e(TAG, "Context is null");
                    return;
                }
                if (!isListen) {
                    Context finalContext = context;
                    ContentObserver contentObserver = new ContentObserver(new Handler(finalContext.getMainLooper())) {
                        @Override
                        public void onChange(boolean selfChange) {
                            isLock = getLockApp(finalContext) != -1;
                        }
                    };
                    context.getContentResolver().registerContentObserver(
                            Settings.Global.getUriFor("key_lock_app"),
                            false, contentObserver);
                    isListen = true;
                }
            }
        });

        if (method == null) {
            XposedLog.e(TAG, "Method is null");
            return;
        }
        hookMethod(method,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (isLock) {
                            param.setResult(false);
                        }
                    }
                }
        );
    }

    public static int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "key_lock_app");
        } catch (Settings.SettingNotFoundException e) {
            XposedLog.e("LockApp", "com.miui.securitycenter",  "getInt hyceiler_lock_app will set E: " + e);
        }
        return -1;
    }
}
