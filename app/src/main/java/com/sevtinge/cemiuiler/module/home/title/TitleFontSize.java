package com.sevtinge.cemiuiler.module.home.title;

import android.util.TypedValue;
import android.widget.TextView;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class TitleFontSize extends BaseHook {

    @Override
    public void init() {
        hookAllMethods("com.miui.home.launcher.common.Utilities", "adaptTitleStyleToWallpaper", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                TextView mTitle = (TextView) param.args[1];
                if (mTitle != null && mTitle.getId() == mTitle.getResources().getIdentifier("icon_title", "id", "com.miui.home")) {
                    mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mPrefsMap.getInt("home_title_font_size",12));
                }
            }
        });
    }
}
