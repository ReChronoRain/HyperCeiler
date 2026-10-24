/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.log;
import com.sevtinge.hyperceiler.log.db.*;
import com.sevtinge.hyperceiler.common.utils.ShellUtils;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import java.util.*;
public class LogSyncDiagnosticsTest {
    static class Dao implements LogDao {
        final List<LogEntry> rows=new ArrayList<>(); boolean fail;
        public LogEntry latestSyncDiagnostic(String tag){
            if(fail)throw new IllegalStateException("database unavailable");
            for(int i=rows.size()-1;i>=0;i--)if(rows.get(i).tag.equals(tag))return rows.get(i);
            return null;
        }
        public void insert(LogEntry row){rows.add(row);}
    }
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        var dao=new Dao();
        // No AndroidLog / user log-level implementation is even present on this classpath.
        ShellUtils.result=1;check(!LogSyncDiagnostics.checkRoot(dao));
        check(dao.rows.size()==1);var denied=dao.rows.get(0);
        check(denied.module.equals("Xposed")&&denied.level.equals("E")&&denied.source.equals("diagnostic"));
        check(denied.message.contains("ROOT_UNAVAILABLE"));
        check(!LogSyncDiagnostics.checkRoot(dao)&&dao.rows.size()==1);
        ShellUtils.result=0;check(LogSyncDiagnostics.checkRoot(dao));
        check(dao.rows.size()==2&&dao.rows.get(1).level.equals("I"));
        ShellUtils.result=1;check(!LogSyncDiagnostics.checkRoot(dao)&&dao.rows.size()==3);
        ShellUtils.fail=true;check(!LogSyncDiagnostics.checkRoot(dao));
        check(dao.rows.get(3).message.contains("ROOT_CHECK_FAILED"));ShellUtils.fail=false;
        LogSyncDiagnostics.record(dao,"LogSync.Logcat","E","LOGCAT_IMPORT_FAILED: permission denied");
        LogSyncDiagnostics.record(dao,"LogSync.Logcat","E","LOGCAT_IMPORT_FAILED: permission denied");
        check(dao.rows.size()==5);
        LogSyncDiagnostics.record(dao,"LogSync.Logcat","I","LOGCAT_IMPORT_OK");check(dao.rows.size()==6);
        LogSyncDiagnostics.context(new android.content.Context(),dao);
        check(dao.rows.get(6).message.contains("launcher=com.miui.home/fixture(4779)"));
        check(dao.rows.get(6).message.contains("sdk=37"));
        dao.fail=true;LogSyncDiagnostics.record(dao,"LogSync.Logcat","E","db failure");
        check(ModuleLog.failures==1&&dao.rows.size()==7); // fallback cannot recurse into the DB.
        System.out.println("persistent log diagnostics: PASS (Root denial/exception/recovery, dedup, metadata, DB failure, logging disabled)");
    }
}
