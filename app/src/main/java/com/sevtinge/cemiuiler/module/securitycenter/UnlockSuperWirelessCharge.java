package com.sevtinge.cemiuiler.module.securitycenter;

import android.view.View;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.PrefsUtils;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UnlockSuperWirelessCharge extends BaseHook {
    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            if (bridge == null) {
                return;
            }
            Map<String, List<DexMethodDescriptor>> resultMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("SuperWirelessCharge", List.of("persist.vendor.tx.speed.control"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );

            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("SuperWirelessCharge"));
            for (DexMethodDescriptor descriptor : result) {
                Method SuperWirelessCharge = descriptor.getMethodInstance(lpparam.classLoader);
                XposedBridge.log("Cemiuiler: SuperWirelessCharge method is "+ SuperWirelessCharge);
                if (SuperWirelessCharge.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(SuperWirelessCharge, XC_MethodReplacement.returnConstant(true));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            if (bridge == null) {
                return;
            }
            Map<String, List<DexMethodDescriptor>> resultMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("SuperWirelessChargeTip", List.of("key_is_connected_super_wls_tx"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );

            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("SuperWirelessChargeTip"));
            for (DexMethodDescriptor descriptor : result) {
                Method SuperWirelessChargeTip = descriptor.getMethodInstance(lpparam.classLoader);
                XposedBridge.log("Cemiuiler: SuperWirelessChargeTip method is "+ SuperWirelessChargeTip);
                if (SuperWirelessChargeTip.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(SuperWirelessChargeTip, XC_MethodReplacement.returnConstant(true));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
