package com.sevtinge.cemiuiler.module.systemui.controlcenter;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.util.ArrayMap;
import android.widget.Switch;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.utils.TileUtils;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;
import miui.telephony.TelephonyManager;

public class FiveGTile extends TileUtils {
    String mNfcTileClsName = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.qs.tiles.MiuiNfcTile" :
        "com.android.systemui.qs.tiles.NfcTile";
    String mQSFactoryClsName = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.qs.tileimpl.MiuiQSFactory" :
        "com.android.systemui.qs.tileimpl.QSFactoryImpl";

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
        return findClassIfExists(mNfcTileClsName);
    }

    @Override
    public String[] customTileProvider() {
        String[] TileProvider = new String[2];
        TileProvider[0] = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "nfcTileProvider" : "mNfcTileProvider";
        TileProvider[1] = "createTileInternal";
        return TileProvider;
    }

    @Override
    public String customName() {
        return "custom_5G";
    }

    @Override
    public int customValue() {
        return R.string.system_control_center_5g_toggle_label;
    }

    @Override
    public void tileCheck(MethodHookParam param, String tileName) {
        if ("custom_5G".equals(tileName)) {
            // 获取设置是否支持5G
            param.setResult(TelephonyManager.getDefault().isFiveGCapable());
        } else {
            param.setResult(false);
        }
    }

    @Override
    public void tileListening(MethodHookParam param, String tileName) {
        if ("custom_5G".equals(tileName)) {
            param.setResult(null);
        }
    }

    @Override
    public void tileLongClickIntent(MethodHookParam param, String tileName) {
        if ("custom_5G".equals(tileName)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.settings.PreferredNetworkTypeListPreference"));
            param.setResult(intent);
        } else {
            param.setResult(null);
        }
    }

    @Override
    public void tileClick(MethodHookParam param, String tileName) {
        if ("custom_5G".equals(tileName)) {
            TelephonyManager manager = TelephonyManager.getDefault();
            // 切换5G状态
            manager.setUserFiveGEnabled(!manager.isUserFiveGEnabled());
            // 更新磁贴状态
            XposedHelpers.callMethod(param.thisObject, "refreshState");
        }
        param.setResult(null);
    }

    @Override
    public void tileUpdateState(MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        boolean isEnable;
        if ("custom_5G".equals(tileName)) {
            TelephonyManager manager = TelephonyManager.getDefault();
            isEnable = manager.isUserFiveGEnabled();

            ArrayMap<String, Integer> tileOnResMap = new ArrayMap<>();
            ArrayMap<String, Integer> tileOffResMap = new ArrayMap<>();
            if (mPrefsMap.getBoolean("system_control_center_5g_tile")) {
                tileOnResMap.put("custom_5G", mResHook.addResource("ic_control_center_5g_toggle_on", R.drawable.ic_control_center_5g_toggle_on));
                tileOffResMap.put("custom_5G", mResHook.addResource("ic_control_center_5g_toggle_off", R.drawable.ic_control_center_5g_toggle_off));
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
