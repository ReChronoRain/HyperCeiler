/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.shell;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.callback.IResult;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;

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
 * 请在适当的时机调用 {@link ShellExec#close} 用来释放资源。
 *
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

    public ShellExec(boolean root) {
        this("", root, false, null);
    }

    public ShellExec(boolean root, @Nullable IResult listen) {
        this("", root, false, listen);
    }

    public ShellExec(boolean root, boolean result) {
        this("", root, result, null);
    }

    public ShellExec(boolean root, boolean result, @Nullable IResult listen) {
        this("", root, result, listen);
    }

    public ShellExec(String command, boolean root, boolean result, @Nullable IResult listen) {
        try {
            OutPut.setOutputListen(listen);
            Error.setOutputListen(listen);
            Check.setOutputListen(listen);
            this.result = result;
            boolean need = command != null && !(command.isEmpty());
            process = Runtime.getRuntime().exec(root ? "su" : "sh");
            if (root) {
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
        }
    }

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
            AndroidLog.e(TAG, "The shell command is executed!", e);
        }
        return this;
    }

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
            }
        } catch (IOException e) {
            AndroidLog.e(TAG, "Error appending shell command!", e);
        }
        return this;
    }

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
            AndroidLog.e(TAG, "Failed to sync shell!", e);
        }
        return this;
    }

    public synchronized boolean ready() {
        return init;
    }

    public synchronized boolean isDestroy() {
        return destroy;
    }

    public synchronized boolean isRoot() {
        return !noRoot;
    }

    protected synchronized void cancelSync() {
        try {
            this.notify();
        } catch (IllegalMonitorStateException e) {
        }
        close();
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

    public synchronized boolean isResult() {
        return setResult == 0;
    }

    public synchronized int getResult() {
        return setResult;
    }

    public synchronized ArrayList<String> getOutPut() {
        return outPut;
    }

    public synchronized ArrayList<String> getError() {
        return error;
    }

    private void done(int count) {
        try {
            isFilter = true;
            os.writeBytes("result=$?; string=\"The execution of command <" + count + "> is complete. Return value: <$result>\"; " +
                    "if [[ $result != 0 ]]; then echo $string 1>&2; else echo $string 2>/dev/null; fi");
            os.writeBytes("\n");
            os.flush();
        } catch (IOException e) {
            AndroidLog.e(TAG, "Error executing the end shell command!", e);
        }
    }

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
            AndroidLog.e(TAG, "Failed to close the shell data flow!", e);
        } catch (InterruptedException f) {
            AndroidLog.e(TAG, "Failed to get return value at the end!", f);
        }
        destroy = true;
        return result;
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
                AndroidLog.e(TAG, "Failed while checking shell status!", e);
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
                    AndroidLog.i(TAG, "OutPut thread has been terminated!");
                    return;
                }
                while ((line = br.readLine()) != null) {
                    if (filter.filter(line))
                        continue;
                    shellExec.outPut.add(line);
                    if (use) mIResult.readOutput(line, false);
                }
            } catch (IOException e) {
                AndroidLog.e(TAG, "Error reading stdd-output stream data!", e);
            } catch (NumberFormatException f) {
                AndroidLog.e(TAG, "Failed to get the return value in the stdout stream!", f);
            }
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
                    AndroidLog.i(TAG, "Error thread has been terminated!");
                    return;
                }
                while ((line = br.readLine()) != null) {
                    if (filter.filter(line))
                        continue;
                    shellExec.error.add(line);
                    if (use) mIResult.readError(line);
                }
            } catch (IOException e) {
                AndroidLog.e(TAG, "Failed to read standard error stream data!", e);
            } catch (NumberFormatException f) {
                AndroidLog.e(TAG, "Failed to get return value in standard error flow!", f);
            }
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

