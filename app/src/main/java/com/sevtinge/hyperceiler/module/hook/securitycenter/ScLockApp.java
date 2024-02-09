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
package com.sevtinge.hyperceiler.module.hook.securitycenter;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Field;

/**
 * @author 焕晨HChen
 */
public class ScLockApp extends BaseHook {
    boolean isListen = false;
    boolean isLock = false;

    int value = 0;

    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData = DexKit.INSTANCE.getDexKitBridge().findMethod(
            FindMethod.create()
                .matcher(MethodMatcher.create()
                    .declaredClass(ClassMatcher.create()
                        .usingStrings("startRegionSampling")
                    )
                    .name("dispatchTouchEvent")
                )
        ).singleOrNull();
        ClassData data = DexKit.INSTANCE.getDexKitBridge().findClass(
            FindClass.create()
                .matcher(ClassMatcher.create()
                    .usingStrings("startRegionSampling")
                )
        ).singleOrNull();
        FieldData fieldData = null;
        if (methodData == null) {
            value = 1;
            methodData = DexKit.INSTANCE.getDexKitBridge().findMethod(
                FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(ClassMatcher.create()
                            .usingStrings("SidebarTouchListener")
                        )
                        .name("onTouch")
                    )
            ).singleOrNull();
            data = DexKit.INSTANCE.getDexKitBridge().findClass(
                FindClass.create()
                    .matcher(ClassMatcher.create()
                        .usingStrings("onTouch: \taction = ")
                    )
            ).singleOrNull();
            fieldData = DexKit.INSTANCE.getDexKitBridge().findField(
                FindField.create()
                    .matcher(
                        FieldMatcher.create()
                            .declaredClass(
                                ClassMatcher.create()
                                    .usingStrings("onTouch: \taction = ")
                            )
                            .type(View.class)
                    )
            ).singleOrNull();
        }
        try {
            // logE(TAG, "dispatchTouchEvent: " + methodData + " Constructor: " + data + " class: " + data.getInstance(lpparam.classLoader) + " f: " + fieldData.getFieldInstance(lpparam.classLoader));
            if (data == null) {
                logE(TAG, "Class is null");
                return;
            }
            if (fieldData == null && value == 1) {
                logE(TAG, "Field is null");
                return;
            }
            assert fieldData != null;
            Field field = fieldData.getFieldInstance(lpparam.classLoader);
            hookAllConstructors(data.getInstance(lpparam.classLoader), new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    Context context = null;
                    if (value == 1) {
                        try {
                            context = ((View) field.get(param.thisObject)).getContext();
                        } catch (IllegalAccessException e) {
                            logE(TAG, "getContext E: " + e);
                        }
                    } else {
                        context = (Context) param.args[0];
                    }
                    if (context == null) {
                        logE(TAG, "Context is null");
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
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            logE(TAG, "hook Constructor E: " + data);
        }

        if (methodData == null) {
            logE(TAG, "Method is null");
            return;
        }
        hookMethod(methodData.getMethodInstance(lpparam.classLoader),
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
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
            logE("LockApp", "getInt hyceiler_lock_app will set E: " + e);
        }
        return -1;
    }
}
