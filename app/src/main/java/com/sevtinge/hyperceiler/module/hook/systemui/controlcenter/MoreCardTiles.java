package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.module.base.BaseHook;

public class MoreCardTiles extends BaseHook {

    @Override
    public void init() throws NoSuchMethodException {
        if (mPrefsMap.getStringAsInt("system_ui_control_center_more_card_tiles", 0) == 1) {
            XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "array", "card_style_tiles_mobile", R.array.card_style_tiles_less);
        } else if (mPrefsMap.getStringAsInt("system_ui_control_center_more_card_tiles", 0) == 2) {
            XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "array", "card_style_tiles_mobile", R.array.card_style_tiles_more);
        } else if (mPrefsMap.getStringAsInt("system_ui_control_center_more_card_tiles", 0) == 3) {
            XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "array", "card_style_tiles_mobile", R.array.card_style_tiles_most);
        }
    }
}
