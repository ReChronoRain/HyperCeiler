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

package com.sevtinge.hyperceiler.module.hook.simactivate;

import static com.sevtinge.hyperceiler.utils.TelephonyUtils.isRoaming;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.RequiresPermission;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class RoamingActivateHelper extends BaseHook {
    private final List<String> mChinaIccidStartsWithList = List.of("8986");
    private final List<String> mChinaTeleZoneCodeList = List.of("+86", "86", "0086");
    private final int slotId = 1;
    private final boolean isRadical = mPrefsMap.getBoolean("sim_activation_service_disable_activate_when_roaming_radical");


    @Override
    public void init() throws NoSuchMethodException {
        Method method = DexKit.findMember("StartActivateSim", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("cloud control not allow this mccmnc activate with method ")
                        )).singleOrNull();
                return methodData;
            }
        });

        Method method2 = DexKit.findMember("CustomGetter", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("ActivateExternal", "set exception handler failed")
                                )
                                .addCaller(MethodMatcher.create()
                                        .declaredClass(findClassIfExists("com.xiaomi.activate.ActivateService"))
                                        .name("onHandleIntent"))
                        )).singleOrNull();
                return methodData;
            }
        });

        Method method3 = DexKit.findMember("SlotIdGetter", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("MiuiSysImpl", "Illegal slotId ")
                        )).singleOrNull();
                return methodData;
            }
        });

        Field field = DexKit.findMember("ActivateSimSubId", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("cloud control not allow this mccmnc activate with method ")
                                )
                                .type(int.class)
                                .modifiers(Modifier.PUBLIC)
                        )).singleOrNull();
                return fieldData;
            }
        });

        Field field2 = DexKit.findMember("ActivateSimContext", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("cloud control not allow this mccmnc activate with method ")
                                )
                                .type(Context.class)
                                .modifiers(Modifier.PROTECTED)
                        )).singleOrNull();
                return fieldData;
            }
        });

        hookMethod(method, new MethodHook(){
            @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.READ_PRIVILEGED_PHONE_STATE"})
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                int subId = (int) getObjectField(param.thisObject, field.getName());
                Object contextGetter = callStaticMethod(method2.getDeclaringClass(), method2.getName());
                Object originSlotId = findMethodBestMatch(method3.getDeclaringClass(), method3.getName(), subId).invoke(contextGetter, subId);
                int slotId = (int) originSlotId;
                Context context = (Context) getObjectField(param.thisObject, field2.getName());
                if (isRoaming(context, slotId, mChinaTeleZoneCodeList, mChinaIccidStartsWithList, isRadical)) {
                    XposedLogUtils.logD(TAG, lpparam.packageName, "Roaming SIM, skip activate.");
                    param.setResult(null);
                }
            }
        });

        findAndHookMethod("com.xiaomi.activate.ActivationSmsReceiver", "onReceive", Context.class, Intent.class, new MethodHook(){
            @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.READ_PRIVILEGED_PHONE_STATE"})
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                int slotId = ((Intent) param.args[1]).getIntExtra("extra_sim_index", -1);
                if (isRoaming((Context) param.args[0], slotId, mChinaTeleZoneCodeList, mChinaIccidStartsWithList, isRadical)) param.setResult(null);
            }
        });

        findAndHookMethod("com.xiaomi.accountsdk.activate.ActivateStatusReceiver", "onReceive", Context.class, Intent.class, new MethodHook(){
            @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.READ_PRIVILEGED_PHONE_STATE"})
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                int slotId = ((Intent) param.args[1]).getIntExtra("extra_sim_index", -1);
                if (isRoaming((Context) param.args[0], slotId, mChinaTeleZoneCodeList, mChinaIccidStartsWithList, isRadical)) param.setResult(null);
            }
        });

        findAndHookMethod("com.xiaomi.activate.SimStateReceiver", "onReceive", Context.class, Intent.class, new MethodHook(){
            @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.READ_PRIVILEGED_PHONE_STATE"})
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                int slotId = ((Intent) param.args[1]).getIntExtra("slot_id", -1);
                if (isRoaming((Context) param.args[0], slotId, mChinaTeleZoneCodeList, mChinaIccidStartsWithList, isRadical)) param.setResult(null);
            }
        });

    }
}
