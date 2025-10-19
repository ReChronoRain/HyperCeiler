package com.fan.common.logviewer;

import android.app.Application;

public class LogAppProxy {

    public static void onCreate(Application app) {
        LogManager.getInstance(app);
    }
}
