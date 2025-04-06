package com.sevtinge.hyperceiler.hook.utils.devicesdk;

import android.os.Build;

import com.sevtinge.hyperceiler.hook.utils.PropUtils;

public class DeviceType {

    public static final boolean IS_DEBUGGABLE;
    public static final boolean IS_FLIP;
    public static final boolean IS_FOLDABLE;
    public static final boolean IS_FOLD_INSIDE;
    public static final boolean IS_FOLD_OUTSIDE;
    public static final boolean IS_REAR;

    static {
        IS_DEBUGGABLE = PropUtils.getProp("ro.debuggable", 0) == 1;
        if (Build.VERSION.SDK_INT > 33) {
            int type = PropUtils.getProp("persist.sys.multi_display_type", 1);
            if (type > 1) {
                int i = type & 15;
                IS_REAR = i == 2;
                IS_FOLD_INSIDE = i == 3;
                IS_FLIP = i == 4;
                IS_FOLD_OUTSIDE = i == 5;
            } else {
                int i2 = PropUtils.getProp("persist.sys.muiltdisplay_type", 0);
                IS_REAR = i2 == 1;
                IS_FOLD_INSIDE = i2 == 2;
                IS_FLIP = false;
                IS_FOLD_OUTSIDE = false;
            }
        } else {
            int type = PropUtils.getProp("persist.sys.muiltdisplay_type", 0);
            IS_REAR = type == 1;
            IS_FOLD_INSIDE = type == 2;
            IS_FLIP = false;
            IS_FOLD_OUTSIDE = false;
        }
        IS_FOLDABLE = IS_FOLD_INSIDE || IS_FOLD_OUTSIDE || IS_FLIP;
    }


    public static boolean isFoldable() {
        return IS_FOLDABLE;
    }
}
