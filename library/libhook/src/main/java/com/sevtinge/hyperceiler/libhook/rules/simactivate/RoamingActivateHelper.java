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

package com.sevtinge.hyperceiler.libhook.rules.simactivate;

import static com.sevtinge.hyperceiler.libhook.utils.api.TelephonyUtils.isRoaming;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.RequiresPermission;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;



public class RoamingActivateHelper extends BaseHook {

    @Override
    protected boolean useDexKit() {
        return true;
    }
    private final List<String> mChinaIccidStartsWithList = List.of("8986");
    private final List<String> mChinaTeleZoneCodeList = List.of("+86", "86", "0086");
    private final int slotId = 1;
    private final boolean isRadical = PrefsBridge.getBoolean("sim_activation_service_disable_activate_when_roaming_radical");
    private Method mStartActivateSimMethod;
    private Method mCustomGetterMethod;
    private Method mSlotIdGetterMethod;
    private Method mStartActivateSimImsMethod;
    private Field mActivateSimSubIdField;
    private Field mActivateSimContextField;


    @Override
    protected boolean initDexKit() {
        mStartActivateSimMethod = requiredMember("StartActivateSim", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("IOException", "cloud control not allow this mccmnc activate with method ")
                        )).singleOrNull();
                return methodData;
            }
        });
        mCustomGetterMethod = requiredMember("CustomGetter", new IDexKit() {
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
        mSlotIdGetterMethod = requiredMember("SlotIdGetter", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("MiuiSysImpl", "Illegal slotId ")
                        )).singleOrNull();
                return methodData;
            }
        });
        mStartActivateSimImsMethod = requiredMember("StartActivateSimIms", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("ImsActivateTask", "cloud control not allow this mccmnc activate with method ")
                    )).singleOrNull();
                return methodData;
            }
        });
        mActivateSimSubIdField = requiredMember("ActivateSimSubId", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("subId:", "cloud control not allow this mccmnc activate with method ")
                                )
                                .addReadMethod(MethodMatcher.create()
                                        .usingNumbers(30000L)
                                )
                                .type(int.class)
                                .modifiers(Modifier.PUBLIC | Modifier.FINAL)
                        )).singleOrNull();
                return fieldData;
            }
        });
        mActivateSimContextField = requiredMember("ActivateSimContext", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("IOException", "cloud control not allow this mccmnc activate with method ")
                                )
                                .type(Context.class)
                                .modifiers(Modifier.PROTECTED)
                        )).singleOrNull();
                return fieldData;
            }
        });
        return true;
    }

    @Override
    public void init() {
        hookMethod(mStartActivateSimMethod, new IMethodHook() {
            @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.READ_PRIVILEGED_PHONE_STATE"})
            @Override
            public void before(HookParam param) throws InvocationTargetException, IllegalAccessException {
                int subId = (int) getObjectField(param.getThisObject(), mActivateSimSubIdField.getName());
                Object contextGetter = callStaticMethod(mCustomGetterMethod.getDeclaringClass(), mCustomGetterMethod.getName());
                Object originSlotId = EzxHelpUtils.findMethodBestMatch(mSlotIdGetterMethod.getDeclaringClass(), mSlotIdGetterMethod.getName(), subId).invoke(contextGetter, subId);
                int slotId = (int) originSlotId;
                Context context = (Context) getObjectField(param.getThisObject(), mActivateSimContextField.getName());
                if (isRoaming(context, slotId, mChinaTeleZoneCodeList, mChinaIccidStartsWithList, isRadical)) {
                    XposedLog.d(TAG, getPackageName(), "Roaming SIM, skip activate.");
                    param.setResult(null);
                }
            }
        });

        //findAndHookMethod("com.xiaomi.activate.simactivate.q", "C", new IMethodHook() {
        hookMethod(mStartActivateSimImsMethod, new IMethodHook() {
            @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.READ_PRIVILEGED_PHONE_STATE"})
            @Override
            public void before(HookParam param) throws InvocationTargetException, IllegalAccessException {
                int subId = (int) getObjectField(param.getThisObject(), mActivateSimSubIdField.getName());
                //XposedLog.d(TAG, getPackageName(), "IMS SIM " + subId);
                Object contextGetter = callStaticMethod(mCustomGetterMethod.getDeclaringClass(), mCustomGetterMethod.getName());
                Object originSlotId = EzxHelpUtils.findMethodBestMatch(mSlotIdGetterMethod.getDeclaringClass(), mSlotIdGetterMethod.getName(), subId).invoke(contextGetter, subId);
                int slotId = (int) originSlotId;
                Context context = (Context) getObjectField(param.getThisObject(), mActivateSimContextField.getName());
                if (isRoaming(context, slotId, mChinaTeleZoneCodeList, mChinaIccidStartsWithList, isRadical)) {
                    XposedLog.d(TAG, getPackageName(), "IMS Roaming SIM, skip activate.");
                    param.setResult(null);
                }
            }
        });

        findAndHookMethod("com.xiaomi.activate.ActivationSmsReceiver", "onReceive", Context.class, Intent.class, new IMethodHook() {
            @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.READ_PRIVILEGED_PHONE_STATE"})
            @Override
            public void before(HookParam param) {
                int slotId = ((Intent) param.getArgs()[1]).getIntExtra("extra_sim_index", -1);
                if (isRoaming((Context) param.getArgs()[0], slotId, mChinaTeleZoneCodeList, mChinaIccidStartsWithList, isRadical)) param.setResult(null);
            }
        });

        findAndHookMethod("com.xiaomi.accountsdk.activate.ActivateStatusReceiver", "onReceive", Context.class, Intent.class, new IMethodHook() {
            @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.READ_PRIVILEGED_PHONE_STATE"})
            @Override
            public void before(HookParam param) {
                int slotId = ((Intent) param.getArgs()[1]).getIntExtra("extra_sim_index", -1);
                if (isRoaming((Context) param.getArgs()[0], slotId, mChinaTeleZoneCodeList, mChinaIccidStartsWithList, isRadical)) param.setResult(null);
            }
        });

        findAndHookMethod("com.xiaomi.activate.SimStateReceiver", "onReceive", Context.class, Intent.class, new IMethodHook() {
            @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.READ_PRIVILEGED_PHONE_STATE"})
            @Override
            public void before(HookParam param) {
                int slotId = ((Intent) param.getArgs()[1]).getIntExtra("slot_id", -1);
                if (isRoaming((Context) param.getArgs()[0], slotId, mChinaTeleZoneCodeList, mChinaIccidStartsWithList, isRadical)) param.setResult(null);
            }
        });

    }
}
