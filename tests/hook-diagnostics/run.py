#!/usr/bin/env python3
from pathlib import Path
import subprocess,tempfile
R=Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory(prefix='hc-hook-diagnostics-')as temp:
 sources=list((R/'library/common/src/main/java/com/sevtinge/hyperceiler/common/diagnostics').glob('*.java'))
 sources += [R/'app/src/main/java/com/sevtinge/hyperceiler/diagnostics/ElfBuildId.java',Path(__file__).with_name('RulesTest.java'),Path(__file__).with_name('ExportTest.java')]
 subprocess.run(['javac','-encoding','UTF-8','-d',temp,*map(str,sources)],check=True)
 subprocess.run(['java','-ea','-cp',temp,'RulesTest'],check=True)
 subprocess.run(['java','-ea','-cp',temp,'ExportTest'],check=True)

subprocess.run(['python3',str(R/'tests/hook-diagnostics/observer.py')],check=True)
with tempfile.TemporaryDirectory(prefix='hc-hook-native-diagnostics-')as temp:
 binary=Path(temp)/'native'
 subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-I'+str(R/'app/src/main/cpp'),str(R/'tests/hook-diagnostics/native.cpp'),'-o',str(binary)],check=True)
 subprocess.run([str(binary)],check=True)
# Execute the real migration and retention SQL; preserve existing log rows.
import sqlite3,re
with sqlite3.connect(':memory:')as db:
 db.execute('CREATE TABLE logs (id INTEGER PRIMARY KEY,module TEXT,level TEXT,tag TEXT,message TEXT,timestamp INTEGER,source_group TEXT,process_ids TEXT)')
 db.execute("INSERT INTO logs VALUES (1,'Xposed','I','fixture','existing evidence',1,'logcat','10139:123:123')")
 schema=(R/'app/src/main/java/com/sevtinge/hyperceiler/log/db/LogDatabase.java').read_text()
 sql=re.search(r'db.execSQL\("(CREATE TABLE IF NOT EXISTS hook_diagnostics.*?)"\)',schema)[1]
 db.execute(sql)
 assert db.execute('SELECT message FROM logs').fetchone()[0]=='existing evidence'
 for i in range(24):db.execute('INSERT OR REPLACE INTO hook_diagnostics VALUES (?,?,?)',(str(i),i,'bounded report'))
 dao=(R/'app/src/main/java/com/sevtinge/hyperceiler/diagnostics/DiagnosticDao.java').read_text()
 trim=re.search(r'@Query\("(DELETE FROM hook_diagnostics WHERE.*?)"\)',dao)[1]
 db.execute(trim);assert db.execute('SELECT count(*) FROM hook_diagnostics').fetchone()[0]==16
 print('Room migration/retention SQL PASS: old logs preserved, bounded 16 snapshots')

# Audit the shipped sampling owner rather than assuming an installed hook executes.
entry=(R/'app/src/main/cpp/targets/home/tweaks/entry.cpp').read_text()
hooks=(R/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
start=hooks.index('extern "C" void hc_home_diagnostic_sample() {')
end=hooks.index('\n}\n',start)+3
sample=hooks[start:end]
assert re.findall(r'android_diagnostics::counter\("([^"]+)"',sample)==[
 'home.title.custom','home.scale.icon','home.scale.widget','home.folder.preview_return',
 'home.drawer.search','home.infinite_scroll','home.prestart']
assert all(word not in sample for word in ('ensure_runtime_features','query_config','/proc/','pthread_create','hook_install'))
monitor=entry[entry.index('void MonitorLoop() {'):entry.index('void FastPatchOnLibraryLoad()')]
assert 'nextDiagnosticSample=diagnosticNow+5000;' in monitor
assert monitor.count('hc_home_diagnostic_sample();')==1
assert hooks.count('hc_home_diagnostic_sample();')==1  # forward declaration only; no competing publisher
print('Production sampling PASS: one existing monitor, neutral settings covered, 5s cap, read-only counters outside runtime locks')

repository=(R/'app/src/main/java/com/sevtinge/hyperceiler/diagnostics/DiagnosticsRepository.java').read_text()
assert repository.count('service.getScope()')==1
assert 'boolean complete=process.complete&&scopeSnapshot!=null;' in repository
print('Scope snapshot integration PASS: provenance, current-state and collection completeness use one batch snapshot')

subprocess.run(["python3",str(R/"tests/hook-diagnostics/sampler.py")],check=True)
subprocess.run(["python3",str(R/"tests/hook-diagnostics/presentation.py")],check=True)
