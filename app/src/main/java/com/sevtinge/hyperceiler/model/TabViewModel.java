/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.model;

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
