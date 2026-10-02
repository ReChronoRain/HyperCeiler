"""Compile LogRepository with minimal Android/Room stubs and exercise its IO queue."""

from pathlib import Path
import os
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(os.environ.get(
    "HC_ISSUE1_SOURCE",
    ROOT / "app/src/main/java/com/sevtinge/hyperceiler/log/db/LogRepository.java",
))

STUBS = {
    "android/content/Context.java": """
package android.content;
import java.io.File;
public class Context {
    private final File root;
    public Context(File root) { this.root = root; }
    public Context getApplicationContext() { return this; }
    public File getDatabasePath(String name) { return new File(new File(root, "databases"), name); }
}
""",
    "android/util/Log.java": """
package android.util;
public class Log {
    public static int errors;
    public static int e(String tag, String text, Throwable error) { errors++; return 0; }
}
""",
    "androidx/room/Room.java": """
package androidx.room;
import android.content.Context;
import com.sevtinge.hyperceiler.log.db.LogDatabase;
public class Room {
    public static <T> Builder<T> databaseBuilder(Context context, Class<T> type, String name) {
        if (!context.getDatabasePath(name).getParentFile().isDirectory())
            throw new AssertionError("database parent missing at Room initialization");
        return new Builder<>();
    }
    public static class Builder<T> {
        public Builder<T> fallbackToDestructiveMigration() { return this; }
        @SuppressWarnings("unchecked")
        public T build() { return (T) new LogDatabase(); }
    }
}
""",
    "com/sevtinge/hyperceiler/common/log/AndroidLog.java": """
package com.sevtinge.hyperceiler.common.log;
public class AndroidLog {
    public static int errors;
    public static void e(String tag, String text) { errors++; }
    public static void e(String tag, String text, Throwable error) { errors++; }
}
""",
    "com/sevtinge/hyperceiler/log/XposedLogLoader.java": """
package com.sevtinge.hyperceiler.log;
import android.content.Context;
public class XposedLogLoader {
    public static void syncLogsToDatabase(Context context) { throw new RuntimeException("sync failed"); }
}
""",
    "com/sevtinge/hyperceiler/log/db/LogEntry.java": """
package com.sevtinge.hyperceiler.log.db;
public class LogEntry {}
""",
    "com/sevtinge/hyperceiler/log/db/LogDao.java": """
package com.sevtinge.hyperceiler.log.db;
import java.util.Collections;
import java.util.List;
public class LogDao {
    public int insertAttempts;
    public void insert(LogEntry entry) {
        insertAttempts++;
        if (insertAttempts == 1) throw new RuntimeException("SQLITE_CANTOPEN");
    }
    public void deleteByModule(String module) { throw new RuntimeException("delete failed"); }
    public void clearAll() { throw new RuntimeException("clear failed"); }
    public void autoTrim() { throw new RuntimeException("trim failed"); }
    public List<LogEntry> getLogsByModuleForExport(String module) { return Collections.emptyList(); }
    public List<LogEntry> getLogsByModulePageForExport(String module, int limit, int offset) { return Collections.emptyList(); }
    public List<LogEntry> getLogsByModuleAndSourceGroupPageForExport(String module, String group, int limit, int offset) { return Collections.emptyList(); }
}
""",
    "com/sevtinge/hyperceiler/log/db/LogDatabase.java": """
package com.sevtinge.hyperceiler.log.db;
public class LogDatabase {
    public static final LogDao DAO = new LogDao();
    public LogDao logDao() { return DAO; }
}
""",
    "TestMain.java": """
import android.content.Context;
import android.util.Log;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.log.db.LogDatabase;
import com.sevtinge.hyperceiler.log.db.LogEntry;
import com.sevtinge.hyperceiler.log.db.LogRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
public class TestMain {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("hc-log-db-test-");
        LogRepository.init(new Context(root.toFile()));
        if (!Files.isDirectory(root.resolve("databases"))) throw new AssertionError("database directory missing");
        LogRepository repo = LogRepository.getInstance();
        repo.insertLog(new LogEntry()); // simulated SQLITE_CANTOPEN
        repo.deleteLogsByModule("module");
        repo.clearAllLogs();
        repo.autoTrim();
        repo.syncXposedLogs();
        repo.clearLogs(null);
        repo.trimDatabase();
        repo.insertLog(new LogEntry()); // proves executor survived all failures
        repo.getIoExecutor().submit(() -> {}).get(5, TimeUnit.SECONDS);
        if (LogDatabase.DAO.insertAttempts != 2) throw new AssertionError("executor did not survive");
        if (Log.errors != 7) throw new AssertionError("expected seven direct error logs, got " + Log.errors);
        if (AndroidLog.errors != 0) throw new AssertionError("database error recursively logged through repository");
        repo.getIoExecutor().shutdownNow();
        System.out.println("PASS: directory created; seven IO failures contained; executor survived; no recursive logging");
    }
}
""",
}

with tempfile.TemporaryDirectory(prefix="hc-issue1-") as temporary:
    work = Path(temporary)
    for relative, source in STUBS.items():
        target = work / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding="utf-8")
    target = work / "com/sevtinge/hyperceiler/log/db/LogRepository.java"
    target.write_text(SOURCE.read_text(encoding="utf-8"), encoding="utf-8")
    files = [str(path) for path in work.rglob("*.java")]
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(work / "classes"), *files], check=True)
    subprocess.run(["java", "-cp", str(work / "classes"), "TestMain"], check=True)
