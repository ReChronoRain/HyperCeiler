package com.fan.common.logviewer;

// BaseLogger.java
public abstract class BaseLogger {
    protected String mModuleName;
    protected LogManager mLogManager;

    // 不再需要持有LogManager实例
    public BaseLogger(String moduleName) {
        this.mModuleName = moduleName;
    }

    public void verbose(String message) {
        log("V", message, true);
    }

    public void debug(String message) {
        log("D", message, true);
    }

    public void info(String message) {
        log("I", message, true);
    }

    public void warn(String message) {
        log("W", message, true);
    }

    public void error(String message) {
        log("E", message, true);
    }

    public void log(String level, String message, boolean newLine) {
        // 直接使用LogManager.getInstance()
        LogEntry logEntry = new LogEntry(level, mModuleName, message, mModuleName, newLine);
        LogManager.getInstance().addLog(logEntry);

        // 同时输出到Android Logcat
        switch (level) {
            case "V": android.util.Log.v(mModuleName, message); break;
            case "D": android.util.Log.d(mModuleName, message); break;
            case "I": android.util.Log.i(mModuleName, message); break;
            case "W": android.util.Log.w(mModuleName, message); break;
            case "E": android.util.Log.e(mModuleName, message); break;
        }
    }
}
