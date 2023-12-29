package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;

public class FixTilesList extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        mResHook.setResReplacement("com.android.systemui", "string", "miui_quick_settings_tiles_stock", R.string.miui_quick_settings_tiles_stock);
        mResHook.setResReplacement("com.android.systemui", "string", "miui_quick_settings_tiles_stock_pad", R.string.miui_quick_settings_tiles_stock_pad);
    }
}
