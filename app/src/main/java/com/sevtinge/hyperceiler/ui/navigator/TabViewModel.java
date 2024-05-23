package com.sevtinge.hyperceiler.ui.navigator;

public class TabViewModel {
    public static final String TAB_HOME = "HOME";
    public static final String TAB_SETTINGS = "SETTINGS";
    public static final String TAB_ABOUT = "ABOUT";
    public static final String[] TABS = {TAB_HOME, TAB_SETTINGS, TAB_ABOUT};

    public static String getTabAt(int position) {
        return TABS[position];
    }

    public static String getTab(int position) {
        if (position < 0) return null;
        if (position >= TABS.length) return null;
        return TABS[position];
    }

    public static int getTabPosition(String tab) {
        for (int i = 0; i < TABS.length; i++) {
            if (getTabAt(i).equals(tab)) {
                return i;
            }
        }
        return 0;
    }
}
