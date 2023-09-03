package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.util.ArrayMap;

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
        // 获取设置是否支持5G
        param.setResult(TelephonyManager.getDefault().isFiveGCapable());
    }

    @Override
    public void tileLongClickIntent(MethodHookParam param, String tileName) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.settings.MiuiFiveGNetworkSetting"));
        // 原活动是 com.android.phone.settings.PreferredNetworkTypeListPreference
        param.setResult(intent);
    }

    @Override
    public void tileClick(MethodHookParam param, String tileName) {
        TelephonyManager manager = TelephonyManager.getDefault();
        // 切换5G状态
        manager.setUserFiveGEnabled(!manager.isUserFiveGEnabled());
        // 更新磁贴状态
        XposedHelpers.callMethod(param.thisObject, "refreshState");
    }

    @Override
    public ArrayMap<String, Integer> tileUpdateState(MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        boolean isEnable;
        TelephonyManager manager = TelephonyManager.getDefault();
        isEnable = manager.isUserFiveGEnabled();
        ArrayMap<String, Integer> tileResMap = new ArrayMap<>();
        tileResMap.put("custom_5G_Enable", isEnable ? 1 : 0);
        tileResMap.put("custom_5G_ON", mResHook.addResource("ic_control_center_5g_toggle_on", R.drawable.ic_control_center_5g_toggle_on));
        tileResMap.put("custom_5G_OFF", mResHook.addResource("ic_control_center_5g_toggle_off", R.drawable.ic_control_center_5g_toggle_off));
        return tileResMap;
    }
}
