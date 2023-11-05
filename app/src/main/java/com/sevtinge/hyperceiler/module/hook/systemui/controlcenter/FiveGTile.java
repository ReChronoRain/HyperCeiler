package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.TileUtils;

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
        String[] TileProvider = new String[4];
        TileProvider[0] = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "nfcTileProvider" : "mNfcTileProvider";
        TileProvider[1] = "createTileInternal";
        TileProvider[2] = "interceptCreateTile";
        TileProvider[3] = "createTile";
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
    public boolean needCustom() {
        return true;
    }

    @Override
    public boolean needAfter() {
        return false;
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
    public void tileListening(MethodHookParam param, String tileName) {
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
