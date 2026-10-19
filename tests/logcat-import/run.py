"""Host tests for logcat parsing and the database refresh/export retention contract."""
from pathlib import Path
import re
import sqlite3
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
parser = root / 'app/src/main/java/com/sevtinge/hyperceiler/log/LogcatParser.java'
with tempfile.TemporaryDirectory(prefix='hc-logcat-test-') as temp:
    subprocess.run(['javac', '-d', temp, str(parser), str(Path(__file__).with_name('LogcatParserTest.java'))], check=True)
    subprocess.run(['java', '-ea', '-cp', temp, 'com.sevtinge.hyperceiler.log.LogcatParserTest'], check=True)

# Execute the actual DAO SQL to ensure a LSPosed refresh does not erase imported logs
# and the existing current-log export includes both sources without schema migration.
dao = (root / 'app/src/main/java/com/sevtinge/hyperceiler/log/db/LogDao.java').read_text()
queries = {name: ''.join(re.findall(r'"([^"\n]*)"', query))
           for query, name in re.findall(r'@Query\((.*?)\)\s*(?:List<LogEntry>|LogEntry|void)\s+(\w+)\(', dao, re.S)}
db = sqlite3.connect(':memory:')
db.execute('CREATE TABLE logs(id INTEGER PRIMARY KEY AUTOINCREMENT,module TEXT,source_group TEXT,timestamp INTEGER)')
db.executemany('INSERT INTO logs VALUES(?,?,?,?)', [
    (1, 'Xposed', 'log', 1), (2, 'Xposed', 'log.old', 2),
    (-3, 'Xposed', 'logcat', 3), (4, 'App', 'log', 4), (-5, 'App', 'logcat', 5),
    (6, 'Xposed', 'diagnostic', 6)])
db.execute(queries['deleteSyncedXposedLogs'], {'module': 'Xposed'})
assert db.execute('SELECT COUNT(*) FROM logs').fetchone()[0] == 4
assert db.execute(queries['getLogsByModuleAndSourceGroupPageForExport'],
                  dict(module='Xposed', sourceGroup='log', limit=500, offset=0)).fetchall() == [(-3, 'Xposed', 'logcat', 3), (6, 'Xposed', 'diagnostic', 6)]
assert db.execute(queries['getLogsByModuleAndSourceGroupPageForExport'],
                  dict(module='Xposed', sourceGroup='log.old', limit=500, offset=0)).fetchall() == []
db.execute('INSERT OR IGNORE INTO logs VALUES(-3,"Xposed","logcat",3)')
assert db.execute('SELECT COUNT(*) FROM logs').fetchone()[0] == 4
db.execute('INSERT INTO logs(module,source_group,timestamp) VALUES("App","log",6)')
assert db.execute('SELECT MAX(id) FROM logs').fetchone()[0] == 7
print('Logcat database: PASS (repeat sync, positive/negative IDs, LSPosed/native/diagnostic retention, export grouping)')
