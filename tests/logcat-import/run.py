"""Host tests for logcat parsing and the database refresh/export retention contract."""
from pathlib import Path
import re
import sqlite3
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
parser = root / 'app/src/main/java/com/sevtinge/hyperceiler/log/LogcatParser.java'

# Device runtimes can provide a newer Java language level than their Core Library.
# Keep these desktop-Java conveniences out of production sources.
for production_root in (root / 'app/src/main', root / 'library'):
    for source in [*production_root.rglob('*.java'), *production_root.rglob('*.kt')]:
        text = source.read_text(encoding='utf-8')
        assert 'Files.readString(' not in text, source
        assert '.readNBytes(' not in text, source

with tempfile.TemporaryDirectory(prefix='hc-logcat-test-') as temp:
    subprocess.run(['javac', '-encoding', 'UTF-8', '-d', temp, str(parser),
                    str(parser.with_name('HomeLayoutEngineLogNames.java')),
                    str(Path(__file__).with_name('LogcatParserTest.java'))], check=True)
    subprocess.run(['java', '-ea', '-cp', temp, 'com.sevtinge.hyperceiler.log.LogcatParserTest'], check=True)

# Compile the real Room model; only annotation declarations are host stubs.
with tempfile.TemporaryDirectory(prefix='hc-log-names-') as temp:
    work = Path(temp)
    annotations = {
        'ColumnInfo': 'String name() default ""; String defaultValue() default "";',
        'Entity': 'String tableName() default ""; Index[] indices() default {};',
        'Ignore': '', 'Index': 'String[] value();', 'PrimaryKey': 'boolean autoGenerate() default false;'
    }
    sources = []
    for name, body in annotations.items():
        source = work / (name + '.java')
        source.write_text('package androidx.room; public @interface ' + name + '{' + body + '}')
        sources.append(str(source))
    sources += [str(parser.with_name('HomeLayoutEngineLogNames.java')),
                str(parser.parent / 'db/LogEntry.java'), str(Path(__file__).with_name('HomeLayoutEngineLogNamesTest.java'))]
    subprocess.run(['javac', '-encoding', 'UTF-8', '-d', temp] + sources, check=True)
    subprocess.run(['java', '-ea', '-cp', temp, 'com.sevtinge.hyperceiler.log.HomeLayoutEngineLogNamesTest'], check=True)

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
