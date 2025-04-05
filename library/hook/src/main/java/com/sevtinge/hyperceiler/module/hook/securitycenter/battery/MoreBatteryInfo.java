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

package com.sevtinge.hyperceiler.module.hook.securitycenter.battery;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.os.Bundle;
import android.os.Message;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKitList;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.BaseDataList;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.FieldDataList;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class MoreBatteryInfo extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        List<Field> fieldList = DexKit.findMemberList("BatteryInfoCategoryAll", new IDexKitList() {
            @Override
            public BaseDataList<FieldData> dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldDataList fieldData = bridge.findField(FindField.create()
                    .matcher(FieldMatcher.create()
                        .declaredClass("com.miui.powercenter.nightcharge.ChargeProtectFragment")
                        .type("miuix.preference.PreferenceCategory")
                    ));
                return fieldData;
            }
        });

        Field field = DexKit.findMember("BatteryInfoCategory", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                    .matcher(FieldMatcher.create()
                        .addReadMethod(MethodMatcher.create()
                            .usingStrings("getOnceOnProtect:")
                        )
                        .declaredClass("com.miui.powercenter.nightcharge.ChargeProtectFragment")
                        .type("miuix.preference.PreferenceCategory")
                    )).singleOrNull();
                return fieldData;
            }
        });

        Method method1 = DexKit.findMember("GetErpSupport", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("persist.vendor.batter.erp")
                    )).singleOrNull();
                return methodData;
            }
        });

        Method method2 = DexKit.findMember("GetErpAndInternationalSupport", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(ClassMatcher.create()
                            .usingStrings("persist.vendor.battery.erp"))
                        .addCaller(MethodMatcher.create()
                            .declaredClass("com.miui.powercenter.nightcharge.ChargeProtectFragment")
                            .name("onCreatePreferences"))
                        .addCaller(MethodMatcher.create()
                            .declaredClass("com.miui.powercenter.nightcharge.ChargeProtectFragment$d")
                            .name("handleMessage"))
                        .returnType(boolean.class)
                        .paramCount(0)
                    )).singleOrNull();
                return methodData;
            }
        });

        findAndHookMethod("com.miui.powercenter.nightcharge.ChargeProtectFragment$d", "handleMessage", Message.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                hookMethod(method2, new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });

            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                callMethod(param.thisObject, "a");
                callMethod(param.thisObject, "b");
            }
        });

        findAndHookMethod("com.miui.powercenter.nightcharge.ChargeProtectFragment", "onCreatePreferences", Bundle.class, String.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object t = param.thisObject;
                findAndHookMethod("androidx.preference.PreferenceGroup", "removePreference", "androidx.preference.Preference", new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        for (Field f : fieldList) {
                            if (f != field) {
                                /*Context context = AndroidAppHelper.currentApplication().getApplicationContext();
                                BatteryManager mBatteryManager = context.getSystemService(BatteryManager.class);
                                // BATTERY_PROPERTY_MANUFACTURING_DATE = 7
                                // BATTERY_PROPERTY_FIRST_USAGE_DATE = 8
                                final long mD = mBatteryManager.getLongProperty(7);
                                AndroidLogUtils.logE(TAG, "Property: " + mD);*/
                                Object c = getObjectField(t, f.getName());
                                if (param.thisObject == c) {
                                    param.setResult(false);
                                }
                            }
                        }
                    }
                });
            }
        });
    }
}
