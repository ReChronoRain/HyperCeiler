package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.TileUtils;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class TaplusTile extends TileUtils {
    String mQSFactoryClsName = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.qs.tileimpl.MiuiQSFactory" :
        "com.android.systemui.qs.tileimpl.QSFactoryImpl";
    String mNightDisplayTile = "com.android.systemui.qs.tiles.NightDisplayTile";

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
        return findClassIfExists(mNightDisplayTile);
    }

    @Override
    public String[] customTileProvider() {
        String[] TileProvider = new String[4];
        TileProvider[0] = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "nightDisplayTileProvider" : "mNightDisplayTileProvider";
        TileProvider[1] = "createTileInternal";
        TileProvider[2] = "interceptCreateTile";
        TileProvider[3] = "createTile";
        return TileProvider;
    }

    @Override
    public String customName() {
        return "taplus_tile";
    }

    @Override
    public int customValue() {
        return R.string.system_control_center_taplus_label;
    }

    @Override
    public boolean needCustom() {
        return true;
    }

    @Override
    public void tileCheck(MethodHookParam param, String tileName) {
        param.setResult(true);
    }

    @Override
    public void tileLongClickIntent(MethodHookParam param, String tileName) {

    }

    @Override
    public void tileClick(MethodHookParam param, String tileName) {
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        boolean z = getTaplus(mContext);
        setTaplus(mContext, !z);
        /*Settings.System.putInt(mContext.getContentResolver(),
            "content_catcher_network_enabled_content_extension",
            !z ? 1 : 0
        );*/
    }

    @Override
    public void tileListening(MethodHookParam param, String tileName) {
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        boolean mListening = (boolean) param.args[0];
        if (mListening) {
            ContentObserver contentObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    XposedHelpers.callMethod(param.thisObject, "refreshState");
                }
            };
            mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor("key_enable_taplus"),
                false, contentObserver);
            XposedHelpers.setAdditionalInstanceField(param.thisObject, "taplusListener", contentObserver);
        } else {
            ContentObserver contentObserver = (ContentObserver) XposedHelpers.getAdditionalInstanceField(param.thisObject, "taplusListener");
            mContext.getContentResolver().unregisterContentObserver(contentObserver);
        }

    }

    @Override
    public ArrayMap<String, Integer> tileUpdateState(MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        boolean isEnable = getTaplus(mContext);
        ArrayMap<String, Integer> tileResMap = new ArrayMap<>();
        tileResMap.put("taplus_tile_Enable", isEnable ? 1 : 0);
        tileResMap.put("taplus_tile_ON", mResHook.addResource(
            "ic_control_center_taplustile_on",
            R.drawable.ic_control_center_taplustile_on));
        tileResMap.put("taplus_tile_OFF", mResHook.addResource(
            "ic_control_center_taplustile_off",
            R.drawable.ic_control_center_taplustile_off));
        return tileResMap;
    }

    public boolean getTaplus(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), "key_enable_taplus") == 1;
        } catch (Throwable throwable) {
            logE(TAG, "getTaplus: " + throwable);
            return false;
        }
    }

    public void setTaplus(Context context, boolean z) {
        try {
            Settings.System.putInt(context.getContentResolver(), "key_enable_taplus", z ? 1 : 0);
        } catch (Throwable e) {
            logE(TAG, "setTaplus: " + e);
        }
    }
}
