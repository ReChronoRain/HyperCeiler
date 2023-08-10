package com.sevtinge.cemiuiler.module.systemui.controlcenter;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.ArrayMap;
import android.widget.Switch;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.utils.TileUtils;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class GmsTile extends TileUtils {
    String CheckGms = "com.google.android.gms";
    String mQSFactoryClsName = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.qs.tileimpl.MiuiQSFactory" :
        "com.android.systemui.qs.tileimpl.QSFactoryInjectorImpl";
    String[] GmsAppsSystem = new String[]{
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending",
        "com.google.android.syncadapters.contacts",
        "com.google.android.backuptransport",
        "com.google.android.onetimeinitializer",
        "com.google.android.partnersetup",
        "com.google.android.configupdater",
        "com.google.android.ext.shared",
        "com.google.android.printservice.recommendation"};

    @Override
    public void init() {
        super.init();
    }

    @Override
    public Class<?> customQSFactory() {
        return findClassIfExists(mQSFactoryClsName);
    }

    @Override
    public Class<?> customClass() {
        return findClassIfExists("com.android.systemui.qs.tiles.ScreenLockTile");
    }

    @Override
    public String[] customTileProvider() {
        String[] TileProvider = new String[2];
        TileProvider[0] = "screenLockTileProvider";
        TileProvider[1] = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "createTileInternal" : "interceptCreateTile";
        return TileProvider;
    }

    @Override
    public String customName() {
        return "custom_GMS";
    }

    @Override
    public int customValue() {
        return R.string.security_center_gms_open;
    }

    @Override
    public void tileCheck(MethodHookParam param, String tileName) {
        if ("custom_GMS".equals(tileName)) {
            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            PackageManager packageManager = mContext.getPackageManager();
            try {
                packageManager.getPackageInfo(CheckGms, PackageManager.GET_ACTIVITIES);
                param.setResult(true);
            } catch (PackageManager.NameNotFoundException e) {
                logI("Not Find GMS App");
                param.setResult(false);
            }
        }
    }

    @Override
    public void tileListening(MethodHookParam param, String tileName) {
        if ("custom_GMS".equals(tileName)) {
            param.setResult(null);
        }
    }

    @Override
    public void tileLongClickIntent(MethodHookParam param, String tileName) {
        if ("custom_GMS".equals(tileName)) {
            param.setResult(null);
        }
    }

    @Override
    public void tileClick(MethodHookParam param, String tileName) {
        if ("custom_GMS".equals(tileName)) {
            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            PackageManager packageManager = mContext.getPackageManager();
            int End = packageManager.getApplicationEnabledSetting(CheckGms);
            if (End == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                for (String GmsAppsSystem : GmsAppsSystem) {
                    try {
                        packageManager.getPackageInfo(GmsAppsSystem, PackageManager.GET_ACTIVITIES);
                        packageManager.setApplicationEnabledSetting(GmsAppsSystem, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
                        logI("To Enabled Gms App:" + GmsAppsSystem);
                    } catch (PackageManager.NameNotFoundException e) {
                        logI("Don't have Gms app :" + GmsAppsSystem);
                    }
                }
                XposedHelpers.callMethod(param.thisObject, "refreshState");
            } else if (End == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || End == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                for (String GmsAppsSystem : GmsAppsSystem) {
                    try {
                        packageManager.getPackageInfo(GmsAppsSystem, PackageManager.GET_ACTIVITIES);
                        packageManager.setApplicationEnabledSetting(GmsAppsSystem, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
                        logI("To Disabled Gms App:" + GmsAppsSystem);
                    } catch (PackageManager.NameNotFoundException e) {
                        logI("Don't have Gms app :" + GmsAppsSystem);
                    }
                }
                XposedHelpers.callMethod(param.thisObject, "refreshState");
            }
        }
        param.setResult(null);
    }

    @Override
    public void tileUpdateState(MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        boolean isEnable;
        if ("custom_GMS".equals(tileName)) {
            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            PackageManager packageManager = mContext.getPackageManager();
            int End = packageManager.getApplicationEnabledSetting(CheckGms);
            isEnable = End == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            ArrayMap<String, Integer> tileOnResMap = new ArrayMap<>();
            ArrayMap<String, Integer> tileOffResMap = new ArrayMap<>();
            if (mPrefsMap.getBoolean("security_center_gms_open")) {
                tileOnResMap.put("custom_GMS", mResHook.addResource("ic_control_center_gms_toggle_on", R.drawable.ic_control_center_gms_toggle_on));
                tileOffResMap.put("custom_GMS", mResHook.addResource("ic_control_center_gms_toggle_off", R.drawable.ic_control_center_gms_toggle_off));
            }
            Object booleanState = param.args[0];
            XposedHelpers.setObjectField(booleanState, "value", isEnable);
            // 测试为开关状态控制，2为开，1为关
            XposedHelpers.setObjectField(booleanState, "state", isEnable ? 2 : 1);
            String tileLabel = (String) XposedHelpers.callMethod(param.thisObject, "getTileLabel");
            XposedHelpers.setObjectField(booleanState, "label", tileLabel);
            XposedHelpers.setObjectField(booleanState, "contentDescription", tileLabel);
            XposedHelpers.setObjectField(booleanState, "expandedAccessibilityClassName", Switch.class.getName());
            Object mIcon = XposedHelpers.callStaticMethod(mResourceIcon, "get", isEnable ? tileOnResMap.get(tileName) : tileOffResMap.get(tileName));
            XposedHelpers.setObjectField(booleanState, "icon", mIcon);
        }
        param.setResult(null);
    }
}
