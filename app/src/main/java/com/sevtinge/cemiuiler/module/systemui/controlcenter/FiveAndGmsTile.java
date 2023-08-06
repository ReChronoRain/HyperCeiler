package com.sevtinge.cemiuiler.module.systemui.controlcenter;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.ArrayMap;
import android.view.View;
import android.widget.Switch;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import de.robv.android.xposed.XposedHelpers;
import miui.telephony.TelephonyManager;

public class FiveAndGmsTile extends BaseHook {
    Class<?> mResourceIcon;
    Class<?> mQSFactory;
    Class<?> mNfcTile;
    String mQSFactoryClsName;
    String mNfcTileClsName;
    String CheckGms = "com.google.android.gms";

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
        boolean enable5G = mPrefsMap.getBoolean("system_control_center_5g_tile");
        boolean enableGMS = mPrefsMap.getBoolean("security_center_gms_open");
        mQSFactoryClsName = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.qs.tileimpl.MiuiQSFactory" :
            "com.android.systemui.qs.tileimpl.QSFactoryImpl";
        mNfcTileClsName = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.qs.tiles.MiuiNfcTile" :
            "com.android.systemui.qs.tiles.NfcTile";
        mQSFactory = findClassIfExists(mQSFactoryClsName);
        mNfcTile = findClassIfExists(mNfcTileClsName);
        mResourceIcon = findClass("com.android.systemui.qs.tileimpl.QSTileImpl$ResourceIcon");
        findAndHookMethod("com.android.systemui.SystemUIApplication", "onCreate", new MethodHook() {
            private boolean isListened = false;

            @Override
            protected void after(MethodHookParam param) {
                if (!isListened) {
                    isListened = true;
                    // 获取Context
                    Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                    // 获取miui_quick_settings_tiles_stock字符串的值
                    @SuppressLint("DiscouragedApi") int stockTilesResId = mContext.getResources().getIdentifier("miui_quick_settings_tiles_stock", "string", lpparam.packageName);
                    String stockTiles = mContext.getString(stockTilesResId); // 追加自定义的磁贴
                    if (enable5G) {
                        stockTiles = stockTiles + ",custom_5G";
                    } else if (enableGMS) {
                        stockTiles = stockTiles + ",custom_GMS";
                    }
                    // 将拼接后的字符串分别替换下面原有的字符串。
                    mResHook.setObjectReplacement("com.android.systemui", "string", "miui_quick_settings_tiles_stock", stockTiles);
                    mResHook.setObjectReplacement("miui.systemui.plugin", "string", "miui_quick_settings_tiles_stock", stockTiles);
                    mResHook.setObjectReplacement("miui.systemui.plugin", "string", "quick_settings_tiles_stock", stockTiles);
                }
            }
        });

        findAndHookMethod(mQSFactory, "createTileInternal", String.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String tileName = (String) param.args[0];
                String nfcField = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "nfcTileProvider" : "mNfcTileProvider";
                if (tileName.equals("custom_5G") && enable5G) {
                    Object provider = XposedHelpers.getObjectField(param.thisObject, nfcField);
                    Object tile = XposedHelpers.callMethod(provider, "get");
                    XposedHelpers.setAdditionalInstanceField(tile, "customName", tileName);
                    param.setResult(tile);
                } else if (tileName.equals("custom_GMS") && enableGMS) {
                    Object provider = XposedHelpers.getObjectField(param.thisObject, nfcField);
                    Object tile = XposedHelpers.callMethod(provider, "get");
                    XposedHelpers.setAdditionalInstanceField(tile, "customName", tileName);
                    param.setResult(tile);
                }
            }
        });

        findAndHookMethod(mNfcTile, "isAvailable", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (tileName != null) {
                    if ("custom_5G".equals(tileName)) {
                        // 获取设置是否支持5G
                        param.setResult(TelephonyManager.getDefault().isFiveGCapable());
                    } else if ("custom_GMS".equals(tileName)) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        PackageManager packageManager = mContext.getPackageManager();
                        try {
                            packageManager.getPackageInfo(CheckGms, PackageManager.GET_ACTIVITIES);
                            param.setResult(true);
                        } catch (PackageManager.NameNotFoundException e) {
                            param.setResult(false);
                        }
                    } else {
                        param.setResult(false);
                    }
                }
            }
        });

        findAndHookMethod(mNfcTile, "getTileLabel", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (tileName != null) {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    Resources modRes = Helpers.getModuleRes(mContext);
                    if ("custom_5G".equals(tileName)) {
                        param.setResult(modRes.getString(R.string.system_control_center_5g_toggle_label));
                    } else if ("custom_GMS".equals(tileName)) {
                        param.setResult(modRes.getString(R.string.security_center_gms_open));
                    }
                }
            }
        });

        findAndHookMethod(mNfcTile, "handleSetListening", boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (tileName != null) {
                    if ("custom_5G".equals(tileName) || "custom_GMS".equals(tileName)) {
                        param.setResult(null);
                    }
                }
            }
        });

        findAndHookMethod(mNfcTile, "getLongClickIntent", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (tileName != null) {
                    if ("custom_5G".equals(tileName)) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.settings.PreferredNetworkTypeListPreference"));
                        param.setResult(intent);
                    } else {
                        param.setResult(null);
                    }
                }
            }
        });

        findAndHookMethod(mNfcTile, "handleClick", View.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (tileName != null) {
                    if ("custom_5G".equals(tileName)) {
                        TelephonyManager manager = TelephonyManager.getDefault();
                        // 切换5G状态
                        manager.setUserFiveGEnabled(!manager.isUserFiveGEnabled());
                        // 更新磁贴状态
                        XposedHelpers.callMethod(param.thisObject, "refreshState");
                    } else if ("custom_GMS".equals(tileName)) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        PackageManager packageManager = mContext.getPackageManager();
                        int End = packageManager.getApplicationEnabledSetting(CheckGms);
                        if (End == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                            for (String GmsAppsSystem : GmsAppsSystem) {
                                try {
                                    packageManager.getPackageInfo(GmsAppsSystem, PackageManager.GET_ACTIVITIES);
                                    packageManager.setApplicationEnabledSetting(GmsAppsSystem, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
                                } catch (PackageManager.NameNotFoundException e) {
                                    logI("Don't have Gms app :" + GmsAppsSystem);
                                }
                            }
                            XposedHelpers.callMethod(param.thisObject, "refreshState");
                        } else if (End == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                            for (String GmsAppsSystem : GmsAppsSystem) {
                                try {
                                    packageManager.getPackageInfo(GmsAppsSystem, PackageManager.GET_ACTIVITIES);
                                    packageManager.setApplicationEnabledSetting(GmsAppsSystem, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
                                } catch (PackageManager.NameNotFoundException e) {
                                    logI("Don't have Gms app :" + GmsAppsSystem);
                                }
                            }
                            XposedHelpers.callMethod(param.thisObject, "refreshState");
                        } else {
                            logI("Unknown state");
                            XposedHelpers.callMethod(param.thisObject, "refreshState");
                        }
                    }
                    param.setResult(null);
                }
            }
        });

        hookAllMethods(mNfcTile, "handleUpdateState", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                String tileName = (String) XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (tileName != null) {
                    boolean isEnable = false;
                    if ("custom_5G".equals(tileName)) {
                        // Object booleanState = param.args[0];
                        TelephonyManager manager = TelephonyManager.getDefault();
                        isEnable = manager.isUserFiveGEnabled();
                    } else if ("custom_GMS".equals(tileName)) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        PackageManager packageManager = mContext.getPackageManager();
                        int End = packageManager.getApplicationEnabledSetting(CheckGms);
                        isEnable = End == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                    }
                    if (tileName.startsWith("custom_")) {
                        ArrayMap<String, Integer> tileOnResMap = new ArrayMap<>();
                        ArrayMap<String, Integer> tileOffResMap = new ArrayMap<>();
                        if (mPrefsMap.getBoolean("system_control_center_5g_tile")) {
                            tileOnResMap.put("custom_5G", mResHook.addResource("ic_control_center_5g_toggle_on", R.drawable.ic_control_center_5g_toggle_on));
                            tileOffResMap.put("custom_5G", mResHook.addResource("ic_control_center_5g_toggle_off", R.drawable.ic_control_center_5g_toggle_off));
                        }
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
        });
    }
}
