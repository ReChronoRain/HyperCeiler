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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
            AndroidLogUtils.LogE("checkRootPermission", "check whether has root permission error: ", e);
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
            AndroidLogUtils.LogE("execCommand", "IOException | InterruptedException: ", e);
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
    }

    /**
     * 可以执行多条 Shell 命令并实时获取结果的 Shell 工具。
     * 本工具使用简单的方法延续 Su/Sh 命令执行窗口，使得调用者无须频繁的执行 Su。
     * 调用示例:
     * <pre> {@code
     * int result = new ShellUtils.OpenShellExecWindow("ls", true, true) {
     *             @Override
     *             public void readOutput(String out) {
     *                 AndroidLogUtils.LogI("[Shell test]", "normal output: " + out);
     *             }
     *
     *             @Override
     *             public void readError(String out) {
     *                 AndroidLogUtils.LogI("[Shell test]", "error output: " + out);
     *             }
     *         }
     *             .append("ls /data")
     *             .append("mkdir -p /data/adb")
     *             .append("touch /data/adb/test")
     *             .close();
     * }
     * 请在适当的时机调用 {@link OpenShellExecWindow#close} 用来释放资源。
     * int result 值为执行全部命令后 exit 的返回值，可以用来判断命令是否执行成功。
     * @author 焕晨HChen
     */
    public static class OpenShellExecWindow implements IOutput {
        private Process process;
        private DataOutputStream os;

        public OpenShellExecWindow(String command, boolean needRoot, boolean needResultMsg) {
            try {
                OutPut.setOutputListen(this);
                Error.setOutputListen(this);
                process = Runtime.getRuntime().exec(needRoot ? "su" : "sh");
                os = new DataOutputStream(process.getOutputStream());
                os.write(command.getBytes());
                os.writeBytes("\n");
                os.flush();
                if (needResultMsg) {
                    Error error = new Error(process.getErrorStream());
                    OutPut output = new OutPut(process.getInputStream());
                    error.start();
                    output.start();
                }
            } catch (IOException e) {
                AndroidLogUtils.LogE(ITAG.TAG, "OpenShellExecWindow E", e);
            }
        }

        public OpenShellExecWindow append(String command) {
            try {
                os.write(command.getBytes());
                os.writeBytes("\n");
                os.flush();
            } catch (IOException e) {
                AndroidLogUtils.LogE(ITAG.TAG, "OpenShellExecWindow append E", e);
            }
            return this;
        }

        public int close() {
            int result = -1;
            try {
                os.writeBytes("exit\n");
                os.flush();
                result = process.waitFor();
                if (process != null) {
                    process.destroy();
                }
                if (os != null) {
                    os.close();
                }
                return result;
            } catch (IOException e) {
                AndroidLogUtils.LogE(ITAG.TAG, "OpenShellExecWindow close E", e);
            } catch (InterruptedException f) {
                AndroidLogUtils.LogE(ITAG.TAG, "OpenShellExecWindow getResult E", f);
            }
            return result;
        }

        @Override
        public void readOutput(String out) {
        }

        @Override
        public void readError(String out) {
        }
    }

    private static class OutPut extends Thread {
        private final InputStream mInput;
        private static IOutput mIOutput;

        public OutPut(InputStream inputStream) {
            mInput = inputStream;
        }

        public static void setOutputListen(IOutput iOutput) {
            mIOutput = iOutput;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(mInput))) {
                String line;
                while (Thread.currentThread().isInterrupted() && (line = br.readLine()) != null) {
                    mIOutput.readOutput(line);
                }
            } catch (IOException e) {
                AndroidLogUtils.LogE(ITAG.TAG, "Shell OutPut run E", e);
            }
        }
    }

    private static class Error extends Thread {
        private final InputStream mInput;
        private static IOutput mIOutput;

        public Error(InputStream inputStream) {
            mInput = inputStream;
        }

        public static void setOutputListen(IOutput iOutput) {
            mIOutput = iOutput;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(mInput))) {
                String line;
                while (Thread.currentThread().isInterrupted() && (line = br.readLine()) != null) {
                    mIOutput.readError(line);
                }
            } catch (IOException e) {
                AndroidLogUtils.LogE(ITAG.TAG, "Shell Error run E", e);
            }
        }
    }

    private interface IOutput {
        void readOutput(String out);

        void readError(String out);
    }
}
