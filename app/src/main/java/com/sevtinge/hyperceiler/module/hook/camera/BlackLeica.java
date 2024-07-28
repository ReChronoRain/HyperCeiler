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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.camera;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class BlackLeica extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Class<?> clazz2 = (Class<?>) DexKit.getDexKitBridge("TextColorMakerClazz", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create()
                                .usingStrings("get(ColorSpace.Named.SRGB)", "bitmap")
                        )).singleOrNull();
                return clazzData.getInstance(lpparam.classLoader);
            }
        });
        Method method1 = (Method) DexKit.getDexKitBridge("WaterMakerLeica", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .paramTypes(int.class, int.class, float.class, String.class, String.class, String.class, boolean.class, String.class, boolean.class, Drawable.class)
                        )).singleOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        Class<?> clazz1 = (Class<?>) DexKit.getDexKitBridge("DescStringColorMakerClazz", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create()
                                .addMethod(MethodMatcher.create()
                                        .paramTypes(int.class, int.class, float.class, String.class, String.class, String.class, boolean.class, String.class, boolean.class, Drawable.class)
                                )
                        )).singleOrNull();
                return clazzData.getInstance(lpparam.classLoader);
            }
        });
        // Class<?> clazz1 = method1.getClass();
        Method method2 = (Method) DexKit.getDexKitBridge("TextPainter", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .paramTypes(Typeface.class, float.class, int.class)
                                .returnType(TextPaint.class)
                        )).singleOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        Method method3 = (Method) DexKit.getDexKitBridge("TextColorMaker", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(clazz2)
                                .paramTypes(int.class)
                                .returnType(clazz2)
                        )).singleOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        Field field1 = (Field) DexKit.getDexKitBridge("DescStringColor", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass(clazz1)
                                .type(int.class)
                        )).singleOrNull();
                return fieldData.getFieldInstance(lpparam.classLoader);
            }
        });
        Field field2 = (Field) DexKit.getDexKitBridge("LeicaPendantColor", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("#33000000", "ISWN")
                                )
                                .type(int.class)
                        )).singleOrNull();
                return fieldData.getFieldInstance(lpparam.classLoader);
            }
        });

        XposedHelpers.setStaticIntField(field1.getDeclaringClass(), field1.getName(), Color.parseColor("#8CFFFFFF"));
        XposedHelpers.setStaticIntField(field2.getDeclaringClass(), field2.getName(), Color.parseColor("#33FFFFFF"));
        hookMethod(method1, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                hookMethod(method2, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        if ((int) param.args[2] == -16777216) param.args[2] = -1;
                    }
                });
                hookMethod(method3, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.args[0] = 1048576;
                    }
                });
            }
        });
    }
}
