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
package com.sevtinge.hyperceiler.hook.utils.shell;

import static com.sevtinge.hyperceiler.expansion.utils.ShellSafeUtils.isSafeCommand;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ShellUtils {
    /**
     * check whether has root permission
     *
     * @return if int = 0, then have root, else don't have.
     */
    public static int checkRootPermission() {
        Process process = null;
        int exitCode = -1;
        try {
            process = Runtime.getRuntime().exec("su -c true");
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            AndroidLogUtils.logE("checkRootPermission", "check whether has root permission error: ", e);
            return exitCode;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * execute shell command, default return result msg
     *
     * @param command command
     * @param isRoot  whether need to run with root
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(String command, boolean isRoot) {
        return execCommand(new String[]{command}, isRoot, true);
    }

    /**
     * execute shell commands, default return result msg
     *
     * @param commands command activity_wifi
     * @param isRoot   whether need to run with root
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(List<String> commands, boolean isRoot) {
        return execCommand(commands == null ? null : commands.toArray(new String[]{}), isRoot, true);
    }

    /**
     * execute shell commands, default return result msg
     *
     * @param commands command array
     * @param isRoot   whether need to run with root
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(String[] commands, boolean isRoot) {
        return execCommand(commands, isRoot, true);
    }

    /**
     * execute shell command
     *
     * @param command         command
     * @param isRoot          whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @noinspection UnusedReturnValue
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(String command, boolean isRoot, boolean isNeedResultMsg) {
        return execCommand(new String[]{command}, isRoot, isNeedResultMsg);
    }

    /**
     * execute shell commands
     *
     * @param commands        command activity_wifi
     * @param isRoot          whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(List<String> commands, boolean isRoot, boolean isNeedResultMsg) {
        return execCommand(commands == null ? null : commands.toArray(new String[]{}), isRoot, isNeedResultMsg);
    }

    /**
     * execute shell commands
     *
     * @param command command activity_wifi
     * @param isRoot  whether need to run with root
     * @return if execCommand.result is 0, then return true, else return false
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static boolean getResultBoolean(String command, boolean isRoot) {
        return execCommand(new String[]{command}, isRoot, false).result == 0;
    }

    /**
     * execute shell commands
     *
     * @param commands command activity_wifi
     * @param isRoot   whether need to run with root
     * @return if execCommand.result is 0, then return true, else return false
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static boolean getResultBoolean(List<String> commands, boolean isRoot) {
        return execCommand(commands == null ? null : commands.toArray(new String[]{}), isRoot, false).result == 0;
    }

    /**
     * execute shell commands
     *
     * @param commands        command array
     * @param isRoot          whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @return <ul>
     * <li>if isNeedResultMsg is false, {@link CommandResult#successMsg} is null and
     * {@link CommandResult#errorMsg} is null.</li>
     * <li>if {@link CommandResult#result} is -1, there maybe some excepiton.</li>
     * </ul>
     */
    public static CommandResult execCommand(String[] commands, boolean isRoot, boolean isNeedResultMsg) {
        int result = -1;
        if (commands == null || commands.length == 0) {
            return new CommandResult(result, null, null);
        }

        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;

        DataOutputStream os = null;
        try {
            // if (isRoot) {
            //     int exitCode = checkRootPermission();
            //     if (exitCode != 0) {
            //         return new CommandResult(exitCode, null, null);
            //     }
            // }
            process = Runtime.getRuntime().exec(isRoot ? "su" : "sh");
            os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (command == null) {
                    continue;
                }

                // donnot use os.writeBytes(commmand), avoid chinese charset error
                os.write(command.getBytes());
                // os.writeBytes(command);
                os.writeBytes("\n");
                os.flush();
            }
            os.writeBytes("exit\n");
            os.flush();

            result = process.waitFor();
            // get command result
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
                errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String s;
                while ((s = successResult.readLine()) != null) {
                    successMsg.append(s);
                }
                while ((s = errorResult.readLine()) != null) {
                    errorMsg.append(s);
                }
            }
        } catch (IOException | InterruptedException e) {
            AndroidLogUtils.logE("execCommand", "IOException | InterruptedException: ", e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (process != null) {
                process.destroy();
            }
        }
        return new CommandResult(result, successMsg == null ? null : successMsg.toString(), errorMsg == null ? null
                : errorMsg.toString());
    }

    /**
     * result of command
     * <ul>
     * <li>{@link CommandResult#result} means result of command, 0 means normal, else means error, same to excute in
     * linux shell</li>
     * <li>{@link CommandResult#successMsg} means success message of command result</li>
     * <li>{@link CommandResult#errorMsg} means error message of command result</li>
     * </ul>
     *
     * @author <main href="http://www.trinea.cn" target="_blank">Trinea</main> 2013-5-16
     */
    public static class CommandResult {

        /**
         * result of command
         **/
        public int result;
        /**
         * success message of command result
         **/
        public String successMsg;
        /**
         * error message of command result
         **/
        public String errorMsg;

        public CommandResult(int result) {
            this.result = result;
        }

        public CommandResult(int result, String successMsg, String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }

        @NonNull
        @Override
        public String toString() {
            return "CommandResult{" +
                "result=" + result +
                ", successMsg='" + successMsg + '\'' +
                ", errorMsg='" + errorMsg + '\'' +
                '}';
        }
    }

    // 保留旧实现方式
    /* public static String rootExecCmd(String cmd) {
        if (!isSafeCommand(cmd)) return "Cannot exec this command: Dangerous operation";
        StringBuilder result = new StringBuilder();
        ProcessBuilder pb = new ProcessBuilder("su");
        pb.redirectErrorStream(true);
        Process p = null;
        DataOutputStream dos = null;
        BufferedReader reader = null;
        try {
            p = pb.start();
            dos = new DataOutputStream(p.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            dos.writeBytes("nsenter --mount=/proc/1/ns/mnt -- " + cmd + "\n");
            dos.writeBytes("exit\n");
            dos.flush();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }
            p.waitFor();
        } catch (Exception e) {
            String errMsg = String.valueOf(e);
            if (!cmd.contains("nsenter") && errMsg.contains("nsenter: exec ")) {
                return errMsg.replace("nsenter: exec ", "");
            }
            return errMsg;
        } finally {
            String finallyErrMsg = null;
            try {
                if (dos != null) dos.close();
            } catch (IOException e) {
                String errMsg = String.valueOf(e);
                if (!cmd.contains("nsenter") && errMsg.contains("nsenter: exec ")) {
                    finallyErrMsg = errMsg.replace("nsenter: exec ", "");
                } else {
                    finallyErrMsg = errMsg;
                }
            }
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                String errMsg = String.valueOf(e);
                if (!cmd.contains("nsenter") && errMsg.contains("nsenter: exec ")) {
                    finallyErrMsg = errMsg.replace("nsenter: exec ", "");
                } else {
                    finallyErrMsg = errMsg;
                }
            }
            if (p != null) p.destroy();
            if (finallyErrMsg != null) return finallyErrMsg;
        }
        if (result.length() > 0) {
            result.setLength(result.length() - 1);
        }
        String resStr = result.toString();
        if (!cmd.contains("nsenter") && resStr.contains("nsenter: exec ")) {
            return resStr.replace("nsenter: exec ", "");
        }
        return resStr;
    } */

    public static String rootExecCmd(String cmd) {
        if (!isSafeCommand(cmd)) return "Cannot exec this command: Dangerous operation";
        final String fullCmd = "nsenter --mount=/proc/1/ns/mnt -- " + cmd;
        ProcessBuilder pb = new ProcessBuilder("su", "-c", fullCmd);
        pb.redirectErrorStream(true);

        StringBuilder result = new StringBuilder();

        try {
            Process p = pb.start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append('\n');
                }
            }

            p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String msg = e.toString();
            if (!cmd.contains("nsenter") && msg.contains("nsenter: exec ")) {
                return msg.replace("nsenter: exec ", "");
            }
            return msg;
        } catch (java.io.IOException e) {
            String msg = e.toString();
            if (!cmd.contains("nsenter") && msg.contains("nsenter: exec ")) {
                return msg.replace("nsenter: exec ", "");
            }
            return msg;
        }

        String out = result.toString();
        if (!out.isEmpty() && out.charAt(out.length() - 1) == '\n') {
            out = out.substring(0, out.length() - 1);
        }

        if (!cmd.contains("nsenter") && out.contains("nsenter: exec ")) {
            return out.replace("nsenter: exec ", "");
        }
        return out;
    }

}
