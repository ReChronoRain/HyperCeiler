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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import com.sevtinge.hyperceiler.utils.shell.ShellUtils;

public class KillAppUtils {
    public static ShellUtils.CommandResult killAll(String pkg, boolean needResult) {
        return ShellUtils.execCommand("killall -s 9 " + "\"" + pkg + "\"", true, needResult);
    }

    public static ShellUtils.CommandResult pKill(String pkg, boolean needResult) {
        return ShellUtils.execCommand("pkill -l 9 -f " + "\"" + pkg + "\"", true, needResult);
    }

    public static ShellUtils.CommandResult pidKill(String pkg, boolean needResult) {
        return ShellUtils.execCommand("pid=$(pgrep -f \"" + pkg + "\" | grep -v $$); " +
                "if [[ $pid != \"\" ]]; then for i in $pid; do kill -s 9 $i &>/dev/null; done; fi",
            true, needResult);
    }

    public static void killAll(String pkg) {
        killAll(pkg, false);
    }

    public static void pKill(String pkg) {
        pKill(pkg, false);
    }

    public static void pidKill(String pkg) {
        pidKill(pkg, false);
    }

    public static void killAll(String[] pkgs) {
        for (String pkg : pkgs) {
            if ("".equals(pkg)) continue;
            killAll(pkg);
        }
    }

    public static void pKill(String[] pkgs) {
        for (String pkg : pkgs) {
            if ("".equals(pkg)) continue;
            pKill(pkg);
        }
    }

    public static void pidKill(String[] pkgs) {
        for (String pkg : pkgs) {
            if ("".equals(pkg)) continue;
            pidKill(pkg);
        }
    }
}
