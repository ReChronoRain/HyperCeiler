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

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import java.util.ArrayList;

/**
 * @noinspection UnusedReturnValue
 */
public class KillApp {
    public static boolean killApps(String pkg) {
        return killApps(new String[]{pkg});
    }

    public static boolean killApps(String... pkgs) {
        if (pkgs == null) {
            AndroidLogUtils.logE(ITAG.TAG, "The list of package names cannot be null!");
            return false;
        }
        boolean result = false;
        if (pkgs.length == 0) {
            AndroidLogUtils.logE(ITAG.TAG, "The length of the packet name array cannot be 0!");
            return false;
        }
        for (String pkg : pkgs) {
            if (pkg == null) continue;
            if ("".equals(pkg)) continue;
            result =
                ShellInit.getShell().add("pid=$(pgrep -f \"" + pkg + "\" | grep -v $$)")
                    .add("if [[ $pid == \"\" ]]; then")
                    .add(" pids=\"\"")
                    .add(" pid=$(ps -A -o PID,ARGS=CMD | grep \"" + pkg + "\" | grep -v \"grep\")")
                    .add("  for i in $pid; do")
                    .add("   if [[ $(echo $i | grep '[0-9]' 2>/dev/null) != \"\" ]]; then")
                    .add("    if [[ $pids == \"\" ]]; then")
                    .add("      pids=$i")
                    .add("    else")
                    .add("      pids=\"$pids $i\"")
                    .add("    fi")
                    .add("   fi")
                    .add("  done")
                    .add("fi")
                    .add("if [[ $pids != \"\" ]]; then")
                    .add(" pid=$pids")
                    .add("fi")
                    .add("if [[ $pid != \"\" ]]; then")
                    .add(" for i in $pid; do")
                    .add("  kill -s 15 $i &>/dev/null")
                    .add(" done")
                    .add("else")
                    .add(" echo \"No Find Pid!\"")
                    .add("fi").over().sync().isResult();
            ArrayList<String> outPut = ShellInit.getShell().getOutPut();
            ArrayList<String> error = ShellInit.getShell().getError();
            if (!outPut.isEmpty()) {
                if (outPut.get(0).equals("No Find Pid!")) {
                    AndroidLogUtils.logW(ITAG.TAG, "Didn't find a pid that can kill: " + pkg);
                    result = false;
                }
            }
            if (!error.isEmpty()) {
                AndroidLogUtils.logE(ITAG.TAG, "An error message was returned:" + error);
                result = false;
            }
        }
        return result;
    }
}
