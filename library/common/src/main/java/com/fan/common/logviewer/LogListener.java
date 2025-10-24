package com.fan.common.logviewer;

// 日志监听接口
public interface LogListener {
    void onLogAdded(LogEntry entry);
    void onLogsCleared();
    void onModuleFilterChanged(String module);
}
