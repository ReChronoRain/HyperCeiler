package com.sevtinge.cemiuiler.module.systemui.controlcenter;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.ArrayMap;
import android.widget.Switch;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.utils.TileUtils;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class SunlightMode extends TileUtils {
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
        return findClassIfExists("com.android.systemui.qs.tiles.ScreenRecordTile");
    }

    @Override
    public String[] customTileProvider() {
        String[] TileProvider = new String[2];
        TileProvider[0] = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "screenRecordTileProvider" : "mScreenRecordTileProvider";
        TileProvider[1] = "createTileInternal";
        return TileProvider;
    }

    @Override
    public String customName() {
        return "custom_SUN";
    }

    @Override
    public int customValue() {
        return R.string.system_control_center_sunshine_mode;
    }

    @Override
    public void tileCheck(MethodHookParam param, String tileName) {
        if ("custom_SUN".equals(tileName)) {
            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            try {
                Settings.System.getInt(mContext.getContentResolver(), "sunlight_mode");
                param.setResult(true);
            } catch (Settings.SettingNotFoundException e) {
                param.setResult(false);
            }
        }
    }

    @Override
    public void tileListening(MethodHookParam param, String tileName) {
        if ("custom_SUN".equals(tileName)) {
            param.setResult(null);
        }
    }

    @Override
    public void tileLongClickIntent(MethodHookParam param, String tileName) {
        if ("custom_SUN".equals(tileName)) {
            param.setResult(null);
        }
    }

    @Override
    public void tileClick(MethodHookParam param, String tileName) {
        if ("custom_SUN".equals(tileName)) {
            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            try {
                int Now = Settings.System.getInt(mContext.getContentResolver(), "sunlight_mode");
                if (Now == 1) {
                    Settings.System.putInt(mContext.getContentResolver(), "sunlight_mode", 0);
                    XposedHelpers.callMethod(param.thisObject, "refreshState");
                } else if (Now == 0) {
                    Settings.System.putInt(mContext.getContentResolver(), "sunlight_mode", 1);
                    XposedHelpers.callMethod(param.thisObject, "refreshState");
                } else {
                    logE("ERROR Int For sunlight_mode");
                }
            } catch (Settings.SettingNotFoundException e) {
                XposedHelpers.callMethod(param.thisObject, "refreshState");
                param.setResult(null);
            }
        }
        param.setResult(null);
    }

    @Override
    public void tileUpdateState(MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        boolean isEnable = false;
        if ("custom_SUN".equals(tileName)) {
            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            try {
                int New = Settings.System.getInt(mContext.getContentResolver(), "sunlight_mode");
                if (New == 1) {
                    isEnable = true;
                } else if (New == 0) {
                    isEnable = false;
                }
            } catch (Settings.SettingNotFoundException e) {
                logE("Not Find sunlight_mode");
            }

            ArrayMap<String, Integer> tileOnResMap = new ArrayMap<>();
            ArrayMap<String, Integer> tileOffResMap = new ArrayMap<>();
            tileOnResMap.put("custom_SUN", mResHook.addResource("ic_control_center_sunlight_mode_on", R.drawable.baseline_wb_sunny_24));
            tileOffResMap.put("custom_SUN", mResHook.addResource("ic_control_center_sunlight_mode_off", R.drawable.baseline_wb_sunny_24));
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
