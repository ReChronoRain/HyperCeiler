/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.log;

import android.content.Context;
import android.os.Build;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.common.utils.ShellUtils;
import com.sevtinge.hyperceiler.log.db.LogDao;
import com.sevtinge.hyperceiler.log.db.LogEntry;

/** Collection failures must remain visible even with logging disabled or logd unavailable. */
final class LogSyncDiagnostics {
    static final String SOURCE = "diagnostic";
    private LogSyncDiagnostics() {}

    static synchronized void record(LogDao dao, String tag, String level, String message) {
        try {
            LogEntry previous = dao.latestSyncDiagnostic(tag);
            if (previous != null && level.equals(previous.getLevel()) && message.equals(previous.getMessage())) return;
            dao.insert(new LogEntry("Xposed", level, tag, message, System.currentTimeMillis(), SOURCE, ""));
        } catch (RuntimeException failure) {
            // A failing DAO cannot be diagnosed through that same DAO or AndroidLog's listener.
            ModuleLog.e("HyperCeiler.LogSync", "Diagnostic database write failed: " + failure.getClass().getSimpleName(), failure);
        }
    }

    static boolean checkRoot(LogDao dao) {
        try {
            if (ShellUtils.checkRootPermission() == 0) { rootReady(dao); return true; }
            rootUnavailable(dao);
        } catch (RuntimeException | LinkageError failure) {
            record(dao, "LogSync.Root", "E", "ROOT_CHECK_FAILED: " + failure.getClass().getSimpleName()
                + ": " + failure.getMessage() + ". Grant Root to HyperCeiler and refresh again.");
        }
        return false;
    }

    static void rootUnavailable(LogDao dao) {
        record(dao, "LogSync.Root", "E", "ROOT_UNAVAILABLE: HyperCeiler has no usable Root permission; "
            + "Xposed/native log collection did not run. Grant Root to HyperCeiler and refresh again.");
    }

    static void rootReady(LogDao dao) {
        record(dao, "LogSync.Root", "I", "ROOT_READY: Root access available for Xposed/native log collection.");
    }

    static void context(Context context, LogDao dao) {
        record(dao, "LogSync.Environment", "I", "DIAGNOSTIC_CONTEXT: sdk=" + Build.VERSION.SDK_INT
            + " device=" + Build.MANUFACTURER + "/" + Build.MODEL + " build=" + Build.DISPLAY
            + " module=" + packageVersion(context, context.getPackageName())
            + " launcher=" + packageVersion(context, "com.miui.home")
            + ". Installed APK versions do not prove that running hooked processes have reloaded them.");
    }

    private static String packageVersion(Context context, String name) {
        try {
            var info = context.getPackageManager().getPackageInfo(name, 0);
            return name + "/" + info.versionName + "(" + info.getLongVersionCode() + ") updated=" + info.lastUpdateTime;
        } catch (Exception unavailable) {
            return name + "/unavailable(" + unavailable.getClass().getSimpleName() + ")";
        }
    }
}
