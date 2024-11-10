package com.sevtinge.hyperceiler.ui.adapter;

public class TabViewModel {

    public static final String TAB_HOME = "HOME";
    public static final String TAB_SETTINGS = "SETTINGS";
    public static final String TAB_ABOUT = "ABOUT";
    public static final String[] TABS = {TAB_HOME, TAB_SETTINGS, TAB_ABOUT};
    public static final String[] TINY_SCREEN_TABS = {TAB_HOME, TAB_SETTINGS};
    public static final String[] TABS_RTL = {TAB_ABOUT, TAB_SETTINGS, TAB_HOME};

    public static int getTabCount() {
        return TABS.length;
    }

    public static String getTabAt(int i) {
        if (i < 0) return null;
        if (i >= TABS.length) return null;
        return TABS[i];
    }

    public static int getTabPosition(String str) {
        for (int i = 0; i < TABS.length; i++) {
            if (getTabAt(i).equals(str)) {
                return i;
            }
        }
        return 0;
    }
}
