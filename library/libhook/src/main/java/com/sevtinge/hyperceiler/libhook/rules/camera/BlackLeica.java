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

package com.sevtinge.hyperceiler.libhook.rules.camera;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

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
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class BlackLeica extends BaseHook {
    private Class<?> mTextColorMakerClazz;
    private Method mWaterMakerLeicaMethod;
    private Class<?> mDescStringColorMakerClazz;
    private Method mTextPainterMethod;
    private Method mTextColorMakerMethod;
    private Field mDescStringColorField;
    private Field mLeicaPendantColorField;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mTextColorMakerClazz = requiredMember("TextColorMakerClazz", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create()
                                .usingStrings("get(ColorSpace.Named.SRGB)", "bitmap")
                        )).singleOrNull();
                return clazzData;
            }
        });

        mWaterMakerLeicaMethod = optionalMember("WaterMakerLeicaNew", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("deviceNameLengthType")
                        .returnType(mTextColorMakerClazz)
                    )).singleOrNull();
                return methodData;
            }
        });
        if (mWaterMakerLeicaMethod == null) {
            mWaterMakerLeicaMethod = requiredMember("WaterMakerLeica", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .paramTypes(int.class, int.class, float.class, String.class, String.class, String.class, boolean.class, String.class, boolean.class, Drawable.class)
                        )).singleOrNull();
                    return methodData;
                }
            });
        }

        mDescStringColorMakerClazz = optionalMember("DescStringColorMakerClazzNew", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create()
                        .addMethod(MethodMatcher.create()
                            .usingStrings("deviceNameLengthType")
                            .returnType(mTextColorMakerClazz)
                        )
                    )).singleOrNull();
                return clazzData;
            }
        });
        if (mDescStringColorMakerClazz == null) {
            mDescStringColorMakerClazz = requiredMember("DescStringColorMakerClazz", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    ClassData clazzData = bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create()
                            .addMethod(MethodMatcher.create()
                                .paramTypes(int.class, int.class, float.class, String.class, String.class, String.class, boolean.class, String.class, boolean.class, Drawable.class)
                            )
                        )).singleOrNull();
                    return clazzData;
                }
            });
        }

        // Class<?> clazz1 = method1.getClass();
        mTextPainterMethod = requiredMember("TextPainter", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .paramTypes(Typeface.class, float.class, int.class)
                                .returnType(TextPaint.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        mTextColorMakerMethod = requiredMember("TextColorMaker", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(mTextColorMakerClazz)
                                .paramTypes(int.class)
                                .returnType(mTextColorMakerClazz)
                        )).singleOrNull();
                return methodData;
            }
        });
        mDescStringColorField = requiredMember("DescStringColor", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass(mDescStringColorMakerClazz)
                                .type(int.class)
                                .addReadMethod(MethodMatcher.create().name(mWaterMakerLeicaMethod.getName()))
                        )).singleOrNull();
                return fieldData;
            }
        });
        mLeicaPendantColorField = requiredMember("LeicaPendantColor", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("#33000000", "ISWN")
                                )
                                .type(int.class)
                        )).singleOrNull();
                return fieldData;
            }
        });
        return true;
    }

    @Override
    public void init() {
        EzxHelpUtils.setStaticIntField(mDescStringColorField.getDeclaringClass(), mDescStringColorField.getName(), Color.parseColor("#8CFFFFFF"));
        EzxHelpUtils.setStaticIntField(mLeicaPendantColorField.getDeclaringClass(), mLeicaPendantColorField.getName(), Color.parseColor("#33FFFFFF"));
        hookMethod(mWaterMakerLeicaMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                hookMethod(mTextPainterMethod, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        if ((int) param.getArgs()[2] == -16777216) param.getArgs()[2] = -1;
                    }
                });
                hookMethod(mTextColorMakerMethod, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.getArgs()[0] = 1048576;
                    }
                });
            }
        });
    }
}
