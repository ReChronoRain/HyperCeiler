package com.sevtinge.cemiuiler.utils;

import android.os.Build;
import moralnorm.os.SdkVersion;

public class SdkHelper {

    public static final boolean ATLEAST_R;
    public static final boolean ATLEAST_S;
    public static final boolean ATLEAST_T;

    public static final boolean IS_MIUI;
    public static final boolean IS_MIUI_13;
    public static final boolean IS_MIUI_14;
    public static final int PROP_MIUI_VERSION_CODE;
    
    public static boolean isAndroidR(){
        return (Build.VERSION.SDK_INT ==Build.VERSION_CODES.R);
    }

    public static boolean isAndroidS(){
        return (Build.VERSION.SDK_INT ==Build.VERSION_CODES.S);
    }

    public static boolean isAndroidSv2(){
        return (Build.VERSION.SDK_INT ==Build.VERSION_CODES.S_V2);
    }

    public static boolean isAndroidTiramisu(){
        return (Build.VERSION.SDK_INT ==Build.VERSION_CODES.TIRAMISU);
    }


    static {
        ATLEAST_R = isAndroidR();
        ATLEAST_S = isAndroidS();
        ATLEAST_T = isAndroidTiramisu();

        IS_MIUI = moralnorm.os.Build.IS_MIUI;
        PROP_MIUI_VERSION_CODE = IS_MIUI ? Integer.parseInt(moralnorm.os.Build.getMiuiVersionCode()) : 0;

        IS_MIUI_13 = PROP_MIUI_VERSION_CODE == 13;
        IS_MIUI_14 = PROP_MIUI_VERSION_CODE == 14;
    }
}
