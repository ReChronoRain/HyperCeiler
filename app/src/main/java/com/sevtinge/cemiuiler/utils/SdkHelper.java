package com.sevtinge.cemiuiler.utils;

import moralnorm.os.Build;
import moralnorm.os.SdkVersion;

public class SdkHelper {

    public static final boolean ATLEAST_R;
    public static final boolean ATLEAST_S;
    public static final boolean ATLEAST_T;

    public static final boolean IS_MIUI;
    public static final boolean IS_MIUI_13;
    public static final boolean IS_MIUI_14;
    public static final int PROP_MIUI_VERSION_CODE;


    static {
        ATLEAST_R = SdkVersion.isAndroidR;
        ATLEAST_S = SdkVersion.isAndroidS;
        ATLEAST_T = SdkVersion.isAndroidT;

        IS_MIUI = Build.IS_MIUI;
        PROP_MIUI_VERSION_CODE = IS_MIUI ? Integer.parseInt(Build.getMiuiVersionCode()) : 0;

        IS_MIUI_13 = PROP_MIUI_VERSION_CODE == 13;
        IS_MIUI_14 = PROP_MIUI_VERSION_CODE == 14;
    }
}
