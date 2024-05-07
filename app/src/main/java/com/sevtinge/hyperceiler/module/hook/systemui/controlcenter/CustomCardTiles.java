package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.module.base.tool.HookTool.mPrefsMap;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

public class CustomCardTiles {

    static List<String> mCardStyleTiles = getTileList();

    public static void initCustomCardTiles(ClassLoader classLoader) {
        findAndHookMethod("miui.systemui.controlcenter.qs.QSController", classLoader, "getCardStyleTileSpecs", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(mCardStyleTiles);
            }
        });
    }

    private static List<String> getTileList() {
        String str = mPrefsMap.getString("systemui_plugin_card_tiles", "");
        return TextUtils.isEmpty(str) ? new ArrayList<>() : Arrays.asList(str.split("\\|"));
    }
}
