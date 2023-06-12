package com.sevtinge.cemiuiler.module.systemui.controlcenter;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Switch;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import de.robv.android.xposed.XposedHelpers;
import miui.telephony.TelephonyManager;

public class QSFiveGTile extends BaseHook {

    Class<?> mResourceIcon;

    Class<?> mQSFactory;
    Class<?> mNfcTile;

    String mQSFactoryClsName;
    String mNfcTileClsName;

    @Override
    @SuppressLint("DiscouragedApi")
    public void init() {

        final boolean[] isListened = {false};

        int mFiveGIconResId = mResHook.addResource("ic_control_center_5g_toggle_on", R.drawable.ic_control_center_5g_toggle_on);
        int mFiveGIconOffResId = mResHook.addResource("ic_control_center_5g_toggle_off", R.drawable.ic_control_center_5g_toggle_off);

        mQSFactoryClsName = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.qs.tileimpl.MiuiQSFactory" :
            "com.android.systemui.qs.tileimpl.QSFactoryImpl";

        mNfcTileClsName = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.qs.tiles.MiuiNfcTile" :
            "com.android.systemui.qs.tiles.NfcTile";

        mQSFactory = findClassIfExists(mQSFactoryClsName);
        mNfcTile = findClassIfExists(mNfcTileClsName);
        mResourceIcon = findClass("com.android.systemui.qs.tileimpl.QSTileImpl$ResourceIcon");

        findAndHookMethod("com.android.systemui.SystemUIApplication", "onCreate", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (!isListened[0]) {
                    isListened[0] = true;
                    Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                    int stockTilesResId = mContext.getResources().getIdentifier("miui_quick_settings_tiles_stock", "string", lpparam.packageName);
                    String stockTiles = mContext.getString(stockTilesResId) + ",custom_5G";
                    mResHook.setObjectReplacement(lpparam.packageName, "string", "miui_quick_settings_tiles_stock", stockTiles);
                    mResHook.setObjectReplacement("miui.systemui.plugin", "string", "miui_quick_settings_tiles_stock", stockTiles);
                    mResHook.setObjectReplacement("miui.systemui.plugin", "string", "quick_settings_tiles_stock", stockTiles);
                }
            }
        });

        findAndHookMethod(mQSFactory, "createTileInternal", String.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String tileName = (String) param.args[0];
                if (tileName.startsWith("custom_")) {
                    String nfcField = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "nfcTileProvider" : "mNfcTileProvider";
                    Object provider = XposedHelpers.getObjectField(param.thisObject, nfcField);
                    Object tile = XposedHelpers.callMethod(provider, "get");
                    XposedHelpers.setAdditionalInstanceField(tile, "customName", tileName);
                    param.setResult(tile);
                }
            }
        });


        findAndHookMethod(mNfcTile, "isAvailable", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object customName = XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (customName != null) {
                    String tileName = (String) customName;
                    if ("custom_5G".equals(tileName)) {
                        param.setResult(TelephonyManager.getDefault().isFiveGCapable());
                    } else {
                        param.setResult(false);
                    }
                }
            }
        });

        findAndHookMethod(mNfcTile, "getTileLabel", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object customName = XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (customName != null) {
                    String tileName = (String) customName;
                    if ("custom_5G".equals(tileName)) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        Resources modRes = Helpers.getModuleRes(mContext);
                        param.setResult(modRes.getString(R.string.system_control_center_5g_toggle_label));
                    }
                }
            }
        });

        findAndHookMethod(mNfcTile, "handleSetListening", boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object customName = XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (customName != null) {
                    String tileName = (String) customName;
                    if ("custom_5G".equals(tileName)) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        boolean mListening = (boolean) param.args[0];
                        if (mListening) {
                            ContentObserver contentObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
                                @Override
                                public void onChange(boolean z) {
                                    XposedHelpers.callMethod(param.thisObject, "refreshState");
                                }
                            };
                            mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("fiveg_user_enable"), false, contentObserver);
                            mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("dual_nr_enabled"), false, contentObserver);
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, "tileListener", contentObserver);
                        } else {
                            ContentObserver contentObserver = (ContentObserver) XposedHelpers.getAdditionalInstanceField(param.thisObject, "tileListener");
                            mContext.getContentResolver().unregisterContentObserver(contentObserver);
                        }
                    }
                    param.setResult(null);
                }
            }
        });

        findAndHookMethod(mNfcTile, "getLongClickIntent", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object customName = XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (customName != null) {
                    String tileName = (String) customName;
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
            protected void before(MethodHookParam param) throws Throwable {
                Object customName = XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (customName != null) {
                    String tileName = (String) customName;
                    if ("custom_5G".equals(tileName)) {
                        TelephonyManager manager = TelephonyManager.getDefault();
                        manager.setUserFiveGEnabled(!manager.isUserFiveGEnabled());
                    }
                    param.setResult(null);
                }
            }
        });

        hookAllMethods(mNfcTile, "handleUpdateState", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object customName = XposedHelpers.getAdditionalInstanceField(param.thisObject, "customName");
                if (customName != null) {
                    String tileName = (String) customName;
                    if ("custom_5G".equals(tileName)) {
                        Object booleanState = param.args[0];
                        TelephonyManager manager = TelephonyManager.getDefault();
                        boolean isEnable = manager.isUserFiveGEnabled();
                        XposedHelpers.setObjectField(booleanState, "value", isEnable);
                        XposedHelpers.setObjectField(booleanState, "state", isEnable ? 2 : 1);
                        String tileLabel = (String) XposedHelpers.callMethod(param.thisObject, "getTileLabel");
                        XposedHelpers.setObjectField(booleanState, "label", tileLabel);
                        XposedHelpers.setObjectField(booleanState, "contentDescription", tileLabel);
                        XposedHelpers.setObjectField(booleanState, "expandedAccessibilityClassName", Switch.class.getName());
                        Object mIcon = XposedHelpers.callStaticMethod(mResourceIcon, "get", isEnable ? mFiveGIconResId : mFiveGIconOffResId);
                        XposedHelpers.setObjectField(booleanState, "icon", mIcon);
                    }
                    param.setResult(null);
                }
            }
        });

    }
}
