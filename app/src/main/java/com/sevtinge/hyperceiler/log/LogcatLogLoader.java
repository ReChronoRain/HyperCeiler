/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.log;

import android.content.Context;
import android.os.Process;

import com.sevtinge.hyperceiler.log.db.LogDao;
import com.sevtinge.hyperceiler.log.db.LogEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Called by the existing background refresh/export path, with its existing root access check. */
final class LogcatLogLoader {
    private static final String PREFS = "logcat_import";
    private static final String SOURCE = "logcat";

    private LogcatLogLoader() {}

    static void sync(Context context, LogDao dao) {
        java.lang.Process reader = null;
        File errors = null;
        try {
            LogSyncDiagnostics.context(context, dao);
            String boot = bootId(context);
            var prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long cutoff = boot.equals(prefs.getString("clear_boot", "")) ? prefs.getLong("cleared_at", 0) : 0;
            List<LogEntry> batch = new ArrayList<>(500);
            int[] imported = {0, 0}; // all module records, native launcher records
            LogcatParser parser = new LogcatParser(boot, Process.myUid(), cutoff, item -> {
                LogEntry entry = new LogEntry(item.app() ? "App" : "Xposed", item.level(), item.tag(),
                    item.message(), item.timestamp(), SOURCE, item.processIds());
                // Stable negative IDs cannot collide with Room's positive auto-generated IDs.
                entry.setId(item.id());
                imported[0]++;
                if (item.tag().startsWith("HyperCeiler.Home") || item.tag().equals("HyperCeiler.NativeHome")
                    || item.tag().equals("HyperCeiler.DockNative")) imported[1]++;
                batch.add(entry);
                if (batch.size() == 500) {
                    dao.insertImportedLogs(batch);
                    batch.clear();
                }
            });
            errors = File.createTempFile("logcat-import-", ".err", context.getCacheDir());
            reader = new ProcessBuilder("su", "-c", "logcat -b all -d -v long -v epoch -v usec -v uid")
                .redirectError(errors).start();
            java.lang.Process running = reader;
            Thread watchdog = new Thread(() -> {
                try {
                    if (!running.waitFor(20, TimeUnit.SECONDS)) running.destroyForcibly();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }, "LogcatImportTimeout");
            watchdog.setDaemon(true);
            watchdog.start();
            try (BufferedReader lines = new BufferedReader(new InputStreamReader(reader.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = lines.readLine()) != null) parser.line(line);
                parser.finish();
            }
            if (!batch.isEmpty()) dao.insertImportedLogs(batch);
            int status = reader.waitFor();
            if (status != 0) {
                String detail;
                try (var errorStream = new java.io.FileInputStream(errors)) {
                    byte[] buffer = new byte[2048];
                    int length = errorStream.read(buffer);
                    detail = length <= 0
                        ? ""
                        : new String(buffer, 0, length, StandardCharsets.UTF_8).trim();
                }
                LogSyncDiagnostics.record(dao, "LogSync.Logcat", "E", "LOGCAT_IMPORT_FAILED: exit=" + status
                    + " records=" + imported[0] + " stderr=" + detail
                    + ". Check Root/logcat availability; the capture may be incomplete.");
            } else {
                LogSyncDiagnostics.record(dao, "LogSync.Logcat", "I", "LOGCAT_IMPORT_OK: module log capture completed.");
                LogSyncDiagnostics.record(dao, "LogSync.HomeCoverage", imported[1] == 0 ? "W" : "I",
                    imported[1] == 0 ? "NO_HOME_RECORDS: No launcher-native diagnostics remain in current logcat buffers. "
                        + "Check LSPosed launcher scope, restart the launcher, reproduce and refresh promptly. "
                        + "Startup records may have expired; this alone does not prove that hooks never loaded."
                        : "HOME_RECORDS_PRESENT: Launcher-native records were found in current logcat buffers.");
            }
            dao.autoTrim();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            LogSyncDiagnostics.record(dao, "LogSync.Logcat", "E", "LOGCAT_IMPORT_INTERRUPTED: capture interrupted; refresh again.");
        } catch (Exception error) {
            LogSyncDiagnostics.record(dao, "LogSync.Logcat", "E", "LOGCAT_IMPORT_FAILED: "
                + error.getClass().getSimpleName() + ": " + error.getMessage());
        } finally {
            if (reader != null) reader.destroy();
            if (errors != null) errors.delete();
        }
    }

    static void markCleared(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("clear_boot", bootId(context)).putLong("cleared_at", System.currentTimeMillis()).commit();
    }

    private static String bootId(Context context) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            new FileInputStream("/proc/sys/kernel/random/boot_id"), StandardCharsets.UTF_8))) {
            String id = reader.readLine();
            if (id != null && !id.trim().isEmpty()) {
                return id.trim();
            }
        } catch (Exception ignored) {
        }
        return "boot-" + android.provider.Settings.Global.getInt(
            context.getContentResolver(), android.provider.Settings.Global.BOOT_COUNT, 0);
    }
}
