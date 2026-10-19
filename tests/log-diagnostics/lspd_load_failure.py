"""Exercise production LSPosed import methods against mixed modules/verbose logs."""
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
loader = (root / 'app/src/main/java/com/sevtinge/hyperceiler/log/XposedLogLoader.java').read_text()
methods = loader[loader.index('    private void importDirectoryToDatabase('):loader.index('    private void clearLogs()')]
constants = loader[loader.index('    private static final String FILTERED_MODULE'):loader.index('    private static volatile XposedLogLoader')]
harness = r'''
package com.sevtinge.hyperceiler.log;
import java.io.*; import java.util.*; import java.util.regex.*; import java.text.*;
public class LspdImportTest {
    static class AndroidLog {static void e(String t,String m,Throwable e){throw new AssertionError(e);}}
    static class LogDao {List<LogEntry> entries=new ArrayList<>();void insertAll(List<LogEntry> b){entries.addAll(b);}}
    record LogEntry(String module,String level,String tag,String message,long timestamp,String sourceGroup,String processIds) {
        String getLevel(){return level;} String getTag(){return tag;} String getMessage(){return message;}
        long getTimestamp(){return timestamp;} String getProcessIds(){return processIds;}
    }
    static class LogDisplayHelper {static final String OTHER_TAG_VALUE="Other";
        static String withUidPidMeta(String m,String ids){return m;}}
    static final String TAG="fixture";
    static String prefix(String level,String tag){return "[ 2026-10-19T19:37:00.161        0:  3267:  3920 "+level+"/"+tag+" ] ";}
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args) throws Exception {
        File dir=new File(args[0]);dir.mkdirs();
        String module=prefix("I","LSPosedFramework")+"(system)[com.sevtinge.hyperceiler,HyperCeiler,1,0,1] [system][Hook]: ready\n";
        String apk="/data/app/~~fixture/com.sevtinge.hyperceiler-random==/base.apk";
        String verbose=module+prefix("W","LSPosedService")+"failed to parse "+apk+"\njava.lang.Exception: fixture\n\tat fixture.method(Source:1)\n"
            +prefix("E","lspd")+"Failed to open APK '"+apk+"': I/O error\n"
            +prefix("E","lspd")+"Failed to open APK '/data/app/com.other-module/base.apk': I/O error\n"
            +prefix("I","lspd")+"Loaded APK "+apk+"\n";
        File modules=new File(dir,"modules_fixture.log");
        java.nio.file.Files.writeString(modules.toPath(),module);
        java.nio.file.Files.writeString(new File(dir,"verbose_fixture.log").toPath(),verbose);
        LspdImportTest importer=new LspdImportTest();LogDao dao=new LogDao();List<LogEntry> batch=new ArrayList<>();
        importer.importDirectoryToDatabase(dir,"log",dao,batch);dao.insertAll(batch);
        check(dao.entries.size()==3);
        var failures=dao.entries.stream().filter(e->e.tag().equals("LSPosed.ModuleLoad")).toList();
        check(failures.size()==2);check(failures.get(0).level().equals("W"));check(failures.get(1).level().equals("E"));
        check(failures.get(0).message().contains("fixture.method(Source:1)"));
        check(failures.get(0).message().contains("MODULE_APK_LOAD_FAILED"));
        check(failures.get(0).processIds().equals("0:3267:3920"));
        check(modules.delete());dao.entries.clear();batch.clear();
        importer.importDirectoryToDatabase(dir,"log.old",dao,batch);dao.insertAll(batch);
        check(dao.entries.size()==3);check(dao.entries.get(0).sourceGroup().equals("log.old"));
        check(!LspdLoadFailure.matches(null));
        check(!LspdLoadFailure.matches("Failed to open APK '/data/app/com.other/base.apk' HyperCeiler"));
        System.out.println("LSPosed APK failure import: PASS (verbose beside modules, stack, severity, no duplicates, fallback)");
    }
}
'''
harness = harness.replace('    static final String TAG="fixture";', constants + methods + '\n    static final String TAG="fixture";')
with tempfile.TemporaryDirectory(prefix='hc-lspd-failure-') as temp:
    temp = Path(temp)
    java = temp / 'LspdImportTest.java'
    java.write_text(harness)
    subprocess.run(['javac', '-d', str(temp), str(java), str(root / 'app/src/main/java/com/sevtinge/hyperceiler/log/LspdLoadFailure.java')], check=True)
    subprocess.run(['java', '-ea', '-cp', str(temp), 'com.sevtinge.hyperceiler.log.LspdImportTest', str(temp / 'logs')], check=True)
