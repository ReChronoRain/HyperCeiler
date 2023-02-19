package com.sevtinge.cemiuiler.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

public class XposedUtils {

    public static Context mModuleContext = null;

    public static synchronized Context getModuleContext(Context context) throws Throwable {
        return getModuleContext(context, null);
    }

    public static synchronized Context getModuleContext(Context context, Configuration config) throws Throwable {
        if (mModuleContext == null)
            mModuleContext = context.createPackageContext(Helpers.mAppModulePkg, Context.CONTEXT_IGNORE_SECURITY).createDeviceProtectedStorageContext();
        return config == null ? mModuleContext : mModuleContext.createConfigurationContext(config);
    }

    public static synchronized Resources getModuleRes(Context context) throws Throwable {
        Configuration config = context.getResources().getConfiguration();
        Context moduleContext = getModuleContext(context);
        return (config == null ? moduleContext.getResources() : moduleContext.createConfigurationContext(config).getResources());
    }
}
