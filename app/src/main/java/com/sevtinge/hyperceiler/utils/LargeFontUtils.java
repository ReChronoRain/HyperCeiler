package com.sevtinge.hyperceiler.utils;

import android.content.Context;

import fan.core.utils.MiuixUIUtils;

public class LargeFontUtils {

    public static boolean isLargeFontLevel(Context context) {
        return MiuixUIUtils.getFontLevel(context) == MiuixUIUtils.FONT_LEVEL_LARGE;
    }
}
