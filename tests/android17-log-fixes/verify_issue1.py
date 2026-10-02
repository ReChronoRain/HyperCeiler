"""Source-level regression checks for the Android 17 log database startup fix."""

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
repository = (ROOT / "app/src/main/java/com/sevtinge/hyperceiler/log/db/LogRepository.java").read_text(encoding="utf-8")
status = (ROOT / "library/common/src/main/java/com/sevtinge/hyperceiler/common/log/LogStatusManager.java").read_text(encoding="utf-8")

checks = {
    "database parent created before Room builder": bool(
        re.search(r"getDatabasePath\(DATABASE_NAME\).*?\.mkdirs\(\).*?Room\.databaseBuilder", repository, re.S)
    ),
    "all seven asynchronous writes use guarded dispatcher": all(
        re.search(rf"void {name}\([^)]*\)\s*\{{.*?executeIo\(", repository, re.S)
        for name in (
            "insertLog", "deleteLogsByModule", "clearAllLogs", "autoTrim",
            "syncXposedLogs", "clearLogs", "trimDatabase",
        )
    ) and repository.count("mIoExecutor.execute(") == 1,
    "dispatcher catches database task failures": bool(
        re.search(r"void executeIo\(.*?catch \(RuntimeException \w+\)", repository, re.S)
    ) and 'Log.e(TAG, "Failed to " + operation, e)' in repository,
    "health check catches failures and releases waiters": bool(
        re.search(r"LogHealthCheck", status)
        and re.search(r"catch \(Throwable \w+\).*?healthCheckLatch.countDown\(\)", status, re.S)
    ),
}

for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
sys.exit(0 if all(checks.values()) else 1)
