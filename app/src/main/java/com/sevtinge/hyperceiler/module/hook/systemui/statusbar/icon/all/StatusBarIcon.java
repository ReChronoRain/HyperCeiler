package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class StatusBarIcon extends BaseHook {

    /*public boolean isHyper;
    ArrayList<mArray> icon = new ArrayList<>();*/

    @Override
    public void init() {
        try {
            findClass("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl").getDeclaredMethod(
                "setIconVisibility", String.class, boolean.class);
            findAndHookMethod("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl",
                "setIconVisibility", String.class, boolean.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        // logE(TAG, "MiuiDripLeftStatusBarIconControllerImpl: " + param.args[0]);
                        switch (checkSlot((String) param.args[0])) {
                            case 1 -> {
                                param.args[1] = true;
                            }
                            case 2 -> {
                                param.args[1] = false;
                            }
                            default -> {
                            }
                        }
                    }
                }
            );
        } catch (Throwable ignored) {
        }

        findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
            "setIconVisibility", String.class, boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    switch (checkSlot((String) param.args[0])) {
                        case 1 -> param.args[1] = true;
                        case 2 -> param.args[1] = false;
                        default -> {
                        }
                    }
                }
            }
        );

        /*findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarIconController",
            "getIconHideList", Context.class, String.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(new ArraySet());
                }
            }
        );

        hookAllMethods("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
                "handleSet", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        Object iconHolder = XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mStatusBarIconList"),
                            "getViewIndex", XposedHelpers.getIntField(param.args[1], "mTag"), param.args[0]);
                    }
                }
            );
        hookAllMethods("com.android.systemui.statusbar.StatusBarIconView",
            "set", new MethodHook() {
                // boolean isId = false;

                @Override
                protected void before(MethodHookParam param) {
                    if (!icon.isEmpty()) {
                        mArray mStatusBarIcon = icon.get(0);
                        if (param.args[0] == mStatusBarIcon.getMStatusBarIcon()) {
                            isId = true;
                        }
                        logE(TAG, "mResId: " + param.args[0] + " int: " + mStatusBarIcon.getMInteger() + " all: " + mStatusBarIcon.getMStatusBarIcon());
                    }
                    // else
                    // logE(TAG, "icon is empty");
                }

                @Override
                protected void after(MethodHookParam param) {
                    int resId = (int) XposedHelpers.callMethod(XposedHelpers.getObjectField(param.args[0], "icon"), "getResId");
                    if (!icon.isEmpty()) {
                        for (int i = 0; i < icon.size(); i++) {
                            mArray mIcon = icon.get(i);
                            ArrayMap<Integer, String> name = mIcon.getMResIdSlot();
                            String mName = name.get(resId);
                            if (mName != null) {
                                if (checkSlot(mName) != -1) {
                                    switch (mIcon.getMInteger()) {
                                        case 1 ->
                                            XposedHelpers.callMethod(param.thisObject, "setVisibility", 0);
                                        case 2 ->
                                            XposedHelpers.callMethod(param.thisObject, "setVisibility", 8);
                                        default -> {
                                        }
                                    }
                                }
                            }
                        }
                        mArray mStatusBarIcon = icon.get(0);
                        if (isId) {
                            switch (mStatusBarIcon.getMInteger()) {
                                case 1 ->
                                    XposedHelpers.callMethod(param.thisObject, "setVisibility", 0);
                                case 2 ->
                                    XposedHelpers.callMethod(param.thisObject, "setVisibility", 8);
                                default -> {
                                }
                            }
                            isId = false;
                        }
                        icon.clear();
                    }
                }
            }
        );

        hookAllMethods("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
            "handleSet", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    ArrayList<mArray> slot = new ArrayList<>();
                    logE(TAG, "StatusBarIconControllerImpl: " + param.args[0]);
                    if (!slot.isEmpty()) {
                        slot.clear();
                    }
                    switch (checkSlot((String) param.args[0])) {
                        case 1 -> slot.add(new mArray(true, 1, new ArrayMap<>()));
                        case 2 -> slot.add(new mArray(true, 2, new ArrayMap<>()));
                        default -> slot.add(new mArray(false, -1, new ArrayMap<>()));
                    }
                    mArray mSlot = slot.get(0);
                    if (mSlot.getMBoolean()) {
                        int resId = (int) XposedHelpers.callMethod(
                            XposedHelpers.getObjectField(
                                XposedHelpers.getObjectField(
                                    param.args[1],
                                    "mIcon"),
                                "icon"),
                            "getResId");
                        // if (!icon.isEmpty()) {
                        //     icon.clear();
                        // }
                        // Object statusBarIcon = XposedHelpers.getObjectField(param.args[0], "mIcon");
                        ArrayMap<Integer, String> resIdSlot = new ArrayMap<>();
                        resIdSlot.put(resId, (String) param.args[0]);
                        if (!icon.isEmpty()) {
                            for (int i = 0; i < icon.size(); i++) {
                                mArray time = icon.get(i);
                                ArrayMap<Integer, String> name = time.getMResIdSlot();
                                if (name.get(resId) == null) {
                                    icon.add(new mArray(true, mSlot.getMInteger(), resIdSlot));
                                }
                            }
                        } else {
                            icon.add(new mArray(true, mSlot.getMInteger(), resIdSlot));
                        }
                        logE(TAG, "resId: " + resIdSlot + " int: " + mSlot.getMInteger());
                    }
                }
            }
        );*/

    }

    private int checkSlot(String slotName) {
        int vpn = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_vpn", 0);
        int alarmClock = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_alarm_clock", 0);
        int nfc = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_nfc", 0);
        int zen = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_zen", 0);
        int volume = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_volume", 0);
        int wifi = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi", 0);
        int wifi_slave = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_slave", 0);
        int airplane = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_airplane", 0);
        int location = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_location", 0);
        int hotspot = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_hotspot", 0);
        int headset = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_headset", 0);
        switch (slotName) {
            case "vpn" -> {
                return vpn;
            }
            case "alarm_clock" -> {
                return alarmClock;
            }
            case "nfc" -> {
                return nfc;
            }
            case "zen" -> {
                return zen;
            }
            case "volume" -> {
                return volume;
            }
            case "wifi" -> {
                return wifi;
            }
            case "wifi_slave" -> {
                return wifi_slave;
            }
            case "airplane" -> {
                return airplane;
            }
            case "location" -> {
                return location;
            }
            case "hotspot" -> {
                return hotspot;
            }
            case "headset" -> {
                return headset;
            }
            default -> {
                return -1;
            }
        }
    }

    /*public static class mArray {
        private final boolean myBoolean;
        private final int myInteger;

        private final ArrayMap<Integer, String> resIdSlot;

        public mArray(boolean myBoolean, int myInteger, ArrayMap<Integer, String> resIdSlot) {
            this.myBoolean = myBoolean;
            this.myInteger = myInteger;
            this.resIdSlot = resIdSlot;
        }

        public boolean getMBoolean() {
            return myBoolean;
        }

        public int getMInteger() {
            return myInteger;
        }

        public ArrayMap<Integer, String> getMResIdSlot() {
            return resIdSlot;
        }
    }*/
}
