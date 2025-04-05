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
package com.sevtinge.hyperceiler.utils.shell;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.callback.IResult;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 可以执行多条 Shell 命令并实时获取结果的 Shell 工具。
 * 本工具使用简单的方法延续 Su/Sh 命令执行窗口，使得调用者无须频繁的执行 Su。
 * 调用示例:
 * <pre> {@code
 * Example 1:
 * new ShellExec("ls", true, true, new IResult() {
 *             @Override
 *             public void readOutput(String out, boolean finish) {
 *                 Log.LogI(TAG, "out: " + out + " finish: " + finish);
 *             }
 *
 *             @Override
 *             public void readError(String out) {
 *                 Log.LogI(TAG, "error: " + out);
 *             }
 *
 *             @Override
 *             public void result(String command, int result) {
 *                 Log.LogI(TAG, "command: " + command + " result: " + result);
 *             }
 *         })
 *             .sync()
 *             .run("echo done")
 *             .sync()
 *             .run("touch /data/adb/2")
 *             .sync()
 *             .close();
 *
 * ------------------------------------------------
 *
 * Example 2:
 * ShellExec shell = new ShellExec(true, true, null);
 *
 * public void test(){
 *     shell.run("ls").sync();
 *     Log.LogI(TAG, "result: " + shell.isResult());
 *     Log.LogI(TAG, "out: " + shell.getOutPut.toString() + " error: " + shell.getError.toString());
 *     shell.close();
 * }
 * }
 * 请在适当的时机调用 {@link ShellExec#close} 用来释放资源。
 * @author 焕晨HChen
 * @noinspection UnusedReturnValue
 */
public class ShellExec {
    private final static String TAG = "ShellExec";
    private Process process;
    private DataOutputStream os;
    private static IPassCommands pass0;
    private static IPassCommands pass1;
    private Error mError;
    private OutPut mOutPut;
    private final ArrayList<String> outPut = new ArrayList<>();
    private final ArrayList<String> error = new ArrayList<>();
    private final ArrayList<String> cList = new ArrayList<>();

    private boolean result;
    private boolean init;
    private boolean destroy;
    private boolean appending = false;
    private boolean isFilter = false;
    private boolean noRoot = false;
    protected int setResult = -1;
    private int count = 1;

    protected static void setICommand(IPassCommands pass, int mode) {
        switch (mode) {
            case 0 -> pass0 = pass;
            case 1 -> pass1 = pass;
        }
    }

    /**
     * 参考 {@link ShellExec#ShellExec(String, boolean, boolean, IResult)}
     */
    public ShellExec(boolean root) {
        this("", root, false, null);
    }

    /**
     * 参考 {@link ShellExec#ShellExec(String, boolean, boolean, IResult)}
     */
    public ShellExec(boolean root, @Nullable IResult listen) {
        this("", root, false, listen);
    }

    /**
     * 参考 {@link ShellExec#ShellExec(String, boolean, boolean, IResult)}
     */
    public ShellExec(boolean root, boolean result) {
        this("", root, result, null);
    }

    /**
     * 参考 {@link ShellExec#ShellExec(String, boolean, boolean, IResult)}
     */
    public ShellExec(boolean root, boolean result, @Nullable IResult listen) {
        this("", root, result, listen);
    }

    /**
     * 构造函数，完成初始化等任务。
     *
     * @param command 需要执行的第一个命令，可以留空。
     * @param root    是否使用 Root 身份执行。
     * @param result  是否需要获取每条命令的返回值。
     * @param listen  回调方法，可以是 null，类有能力处理。
     */
    public ShellExec(String command, boolean root, boolean result, @Nullable IResult listen) {
        try {
            OutPut.setOutputListen(listen);
            Error.setOutputListen(listen);
            Check.setOutputListen(listen);
            this.result = result;
            boolean need = command != null && !(command.isEmpty());
            process = Runtime.getRuntime().exec(root ? "su" : "sh");
            // 注意处理
            if (root) {
                // boolean r = process.waitFor(600, TimeUnit.MILLISECONDS);
                // if (r) {
                //     process.destroy();
                //     throw new RuntimeException("Root permission not obtained!");
                // }
                Check check = new Check(process, this);
                check.start();
            }
            os = new DataOutputStream(process.getOutputStream());
            if (need) write(command);
            if (result) {
                mError = new Error(process.getErrorStream(), this, listen != null);
                mOutPut = new OutPut(process.getInputStream(), this, listen != null);
                clear();
                pass(command);
                mError.start();
                mOutPut.start();
                if (need) done(0);
            }
            init = true;
            destroy = false;
        } catch (IOException e) {
            init = false;
            // throw new RuntimeException("ShellExec boot failed!!\n" + e);
            // AndroidLogUtils.logE(TAG, "ShellExec E", e);
            // init = false;
        }
    }

    /**
     * 需要执行的命令，一次性可执行完毕的。
     *
     * @param command 命令
     * @return this
     */
    public synchronized ShellExec run(String command) {
        if (!init) return this;
        if (noRoot) return this;
        if (destroy) throw new RuntimeException("This shell has been destroyed!");
        if (appending) {
            throw new RuntimeException("Shell is in append mode!");
        }
        try {
            if (result) {
                clear();
                pass(command);
            }
            write(command);
            if (result) {
                done(count);
                count = count + 1;
            }
        } catch (IOException e) {
            AndroidLogUtils.logE(TAG, "The shell command is executed!", e);
        }
        return this;
    }

    /**
     * 进入追加模式，在这个模式你可以逐行输入命令，并请在结束时显性调用 over() 方法。
     *
     * @param command 追加命令
     * @return this
     */
    public synchronized ShellExec add(String command) {
        if (!init) return this;
        if (noRoot) return this;
        if (destroy) throw new RuntimeException("This shell has been destroyed!");
        appending = true;
        clear();
        try {
            write(command);
            if (result) {
                cList.add(command);
                // count = count + 1;
            }
        } catch (IOException e) {
            AndroidLogUtils.logE(TAG, "Error appending shell command!", e);
        }
        return this;
    }

    /**
     * 结束追加模式。
     *
     * @return this
     */
    public synchronized ShellExec over() {
        if (!init) return this;
        if (noRoot) return this;
        if (destroy) throw new RuntimeException("This shell has been destroyed!");
        appending = false;
        if (result) {
            pass(cList.toString());
            done(count);
            count = count + 1;
        }
        return this;
    }

    /**
     * 同步命令。
     * 进程将会在该条命令完全执行完毕并输出结束内容前等待。
     * 如果你需要获取命令的返回值，输出内容，请务必使用！
     *
     * @return this
     */
    public synchronized ShellExec sync() {
        if (!init) return this;
        if (noRoot) return this;
        if (destroy) throw new RuntimeException("This shell has been destroyed!");
        if (appending) {
            throw new RuntimeException("Shell is in append mode!");
        }
        try {
            this.wait();
        } catch (InterruptedException e) {
            AndroidLogUtils.logE(TAG, "Failed to sync shell!", e);
        }
        return this;
    }

    /**
     * 返回 Shell 工具是否初始化完成。
     *
     * @return 是否初始化完成。
     */
    public synchronized boolean ready() {
        return init;
    }

    /**
     * 获取本 Shell 是否已经销毁。
     *
     * @return Shell 状态
     */
    public synchronized boolean isDestroy() {
        return destroy;
    }

    /**
     * 返回是否拥有 Root 权限。
     * 具有滞后性。
     *
     * @return 是否拥有 Root 权限
     */
    public synchronized boolean isRoot() {
        return !noRoot;
    }

    /**
     * 解除进程同步，并关闭 Shell 数据流。
     */
    protected synchronized void cancelSync() {
        try {
            this.notify();
        } catch (IllegalMonitorStateException e) {
        }
        close();
        // throw new RuntimeException("Shell process exited abnormally, possibly due to lack of Root permission!!");
    }

    private void clear() {
        setResult = -1;
        outPut.clear();
        error.clear();
    }

    private void pass(String command) {
        if (pass0 != null)
            pass0.passCommands(command);
        if (pass1 != null)
            pass1.passCommands(command);
    }

    private synchronized void write(String command) throws IOException {
        os.write(command.getBytes());
        os.writeBytes("\n");
        os.flush();
    }

    /**
     * 返回当前命令的执行结果，建议搭配 sync，否则可能错位。
     *
     * @return 执行结果
     */
    public synchronized boolean isResult() {
        return setResult == 0;
    }

    /**
     * 返回当前命令的执行返回值，建议搭配 sync，否则可能错位。
     *
     * @return 执行返回值
     */
    public synchronized int getResult() {
        return setResult;
    }

    /**
     * 获取输出数据的 List 集合，强烈建议和 sync() 一同使用！
     *
     * @return 输出数据的集合
     */
    public synchronized ArrayList<String> getOutPut() {
        return outPut;
    }

    /**
     * 获取输出错误数据的 List 集合，强烈建议和 sync() 一同使用！
     *
     * @return 输出错误数据的集合
     */
    public synchronized ArrayList<String> getError() {
        return error;
    }

    private void done(int count) {
        try {
            isFilter = true;
            os.writeBytes("result=$?; string=\"The execution of command <" + count + "> is complete. Return value: <$result>\"; " +
                    "if [[ $result != 0 ]]; then echo $string 1>&2; else echo $string 2>/dev/null; fi");
            // os.writeBytes("echo \"The execution of command <" + count + "> is complete. Return value: <$?>\" 1>&2 2>&1");
            os.writeBytes("\n");
            os.flush();
        } catch (IOException e) {
            AndroidLogUtils.logE(TAG, "Error executing the end shell command!", e);
        }
    }

    /**
     * 关闭 ShellWindow。
     *
     * @return 本 process 的最终执行结果
     */
    public synchronized int close() {
        if (!init) return -1;
        if (destroy) throw new RuntimeException("This shell has been destroyed!");
        int result = -1;
        try {
            clear();
            write("exit");
            result = process.waitFor();
            process.destroy();
            os.close();
            if (mError != null && mOutPut != null) {
                mError.interrupt();
                mOutPut.interrupt();
                mOutPut = null;
                mError = null;
            }
        } catch (IOException e) {
            AndroidLogUtils.logE(TAG, "Failed to close the shell data flow!", e);
        } catch (InterruptedException f) {
            AndroidLogUtils.logE(TAG, "Failed to get return value at the end!", f);
        }
        destroy = true;
        return result;
    }

    private void log(String log) {
        AndroidLogUtils.logI(TAG, log);
    }

    protected interface IPassCommands {
        void passCommands(String command);
    }

    private static class Check extends Thread {
        private final Process process;
        private final ShellExec shellExec;
        private static IResult mIResult;

        public Check(Process process, ShellExec shellExec) {
            this.process = process;
            this.shellExec = shellExec;
        }

        public static void setOutputListen(IResult iResult) {
            mIResult = iResult;
        }

        @Override
        public void run() {
            boolean result = false;
            try {
                result = process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                AndroidLogUtils.logE(TAG, "Failed while checking shell status!", e);
            }
            if (result) {
                try {
                    shellExec.notify();
                } catch (IllegalMonitorStateException e) {
                }
                if (!shellExec.isDestroy()) {
                    if (mIResult != null) mIResult.error("No Root!");
                    shellExec.noRoot = true;
                    shellExec.cancelSync();
                }
            }
        }
    }

    private static class OutPut extends Thread {
        private final InputStream mInput;
        private final Pattern pattern;
        private final Command command;
        private final String contrast;
        private final ShellExec shellExec;

        private static IResult mIResult;

        public OutPut(InputStream inputStream, ShellExec shellExec, boolean listen) {
            contrast = "The execution of command <";
            pattern = Pattern.compile(".*<(\\d+)>.*<(\\d+)>.*");
            if (listen) command = new Command(0);
            else command = null;
            this.shellExec = shellExec;
            mInput = inputStream;
        }

        public static void setOutputListen(IResult iResult) {
            mIResult = iResult;
        }

        @Override
        public void run() {
            boolean use = mIResult != null;
            Filter filter = new Filter(shellExec, contrast, pattern, command, mIResult, use, true);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(mInput))) {
                String line;
                if (Thread.currentThread().isInterrupted()) {
                    log("OutPut thread has been terminated!");
                    return;
                }
                while ((line = br.readLine()) != null) {
                    // Log.LogI(TAG, "out: " + line);
                    if (filter.filter(line))
                        continue;
                    shellExec.outPut.add(line);
                    if (use) mIResult.readOutput(line, false);
                }
            } catch (IOException e) {
                AndroidLogUtils.logE(TAG, "Error reading stdd-output stream data!", e);
            } catch (NumberFormatException f) {
                AndroidLogUtils.logE(TAG, "Failed to get the return value in the stdout stream!", f);
            }
        }

        private void log(String log) {
            AndroidLogUtils.logI(TAG, log);
        }
    }

    private static class Error extends Thread {
        private final InputStream mInput;
        private final ShellExec shellExec;
        private final Pattern pattern;
        private final Command command;
        private final String contrast;
        private static IResult mIResult;

        public Error(InputStream inputStream, ShellExec shellExec, boolean listen) {
            contrast = "The execution of command <";
            pattern = Pattern.compile(".*<(\\d+)>.*<(\\d+)>.*");
            if (listen) command = new Command(1);
            else command = null;
            mInput = inputStream;
            this.shellExec = shellExec;
        }

        public static void setOutputListen(IResult iResult) {
            mIResult = iResult;
        }

        @Override
        public void run() {
            boolean use = mIResult != null;
            Filter filter = new Filter(shellExec, contrast, pattern, command, mIResult, use, false);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(mInput))) {
                String line;
                if (Thread.currentThread().isInterrupted()) {
                    log("Error thread has been terminated!");
                    return;
                }
                while ((line = br.readLine()) != null) {
                    // Log.LogI(TAG, "error: " + line);
                    if (filter.filter(line))
                        continue;
                    shellExec.error.add(line);
                    if (use) mIResult.readError(line);
                }
            } catch (IOException e) {
                AndroidLogUtils.logE(TAG, "Failed to read standard error stream data!", e);
            } catch (NumberFormatException f) {
                AndroidLogUtils.logE(TAG, "Failed to get return value in standard error flow!", f);
            }
        }

        private void log(String log) {
            AndroidLogUtils.logI(TAG, log);
        }
    }

    private record Filter(ShellExec shellExec, String contrast, Pattern pattern,
                          Command command, IResult mIResult, boolean use, boolean finish) {
        public boolean filter(String line) throws NumberFormatException {
            if (shellExec.isFilter) {
                if (line.contains(contrast)) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String count = matcher.group(1);
                        String result = matcher.group(2);
                        if (result != null && count != null) {
                            if (use) {
                                mIResult.result(command.passCommands.get(Integer.parseInt(count)),
                                        Integer.parseInt(result));
                                if (finish) mIResult.readOutput("Finish!!", true);
                            }
                            shellExec.setResult = Integer.parseInt(result);
                            synchronized (shellExec) {
                                try {
                                    shellExec.notify();
                                } catch (IllegalMonitorStateException e) {
                                }
                            }
                            shellExec.isFilter = false;
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    protected static class Command implements IPassCommands {
        public ArrayList<String> passCommands = new ArrayList<>();

        public Command(int mode) {
            setICommand(this, mode);
        }

        @Override
        public void passCommands(String command) {
            passCommands.add(command);
        }
    }
}
