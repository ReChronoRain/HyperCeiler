/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.diagnostics;

import android.content.Context;
import android.os.Build;
import com.sevtinge.hyperceiler.common.diagnostics.*;
import com.sevtinge.hyperceiler.log.XposedLogLoader;
import com.sevtinge.hyperceiler.log.db.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;

/** Reuses the existing log repository executor, capture and Room database. */
public final class DiagnosticsRepository {
    private final Context context;
    private boolean running;
    private Set<String> scopeSnapshot;
    private final Map<String,Integer> uidSnapshot=new HashMap<>();
    // Cache immutable decoding only. Process, generation, UID and freshness are checked on EVERY refresh.
    private final LinkedHashMap<String,DiagnosticEvent> decoded=new LinkedHashMap<>();
    public DiagnosticsRepository(Context context){this.context=context.getApplicationContext();}
    public synchronized void refresh(boolean collect,Consumer<DiagnosticRules.Report> callback,Consumer<String> failure){
        if(running)return;running=true;
        LogRepository.getInstance().getIoExecutor().execute(()->{
            try{
                if(collect){
                    XposedLogLoader.syncLogsToDatabaseSync(context);
                    // Capture failure is reported through the same private diagnostic DAO.
                    com.sevtinge.hyperceiler.log.LogcatLogLoader.syncHookDiagnostics(context,LogRepository.getInstance().getDao());
                }
                callback.accept(analyze());
            }catch(Exception e){failure.accept("这次诊断没有完成，请点重新诊断，仍失败时到日志页面导出日志反馈");}
            finally{synchronized(this){running=false;}}
        });
    }
    private DiagnosticRules.Report analyze()throws Exception{
        scopeSnapshot=null;uidSnapshot.clear();
        try {var service=com.sevtinge.hyperceiler.utils.ScopeManager.getService();if(service!=null)scopeSnapshot=Set.copyOf(service.getScope());}catch(Exception unavailable){}
        var dao=LogRepository.getInstance().getDiagnosticDao();List<LogEntry> rows=dao.inputs();
        List<DiagnosticEvent> events=new ArrayList<>();int malformed=0,rejected=0;
        Set<Integer> pids=new HashSet<>();Map<String,DiagnosticEvent> anchors=new HashMap<>();
        for(var row:rows){
            if(!row.getTag().equals("HyperCeiler.HookDiagnostics"))continue;
            String text=row.getMessage();String cacheKey=row.getId()+"/"+text.hashCode();
            // A structured record is one wire line. Ignore unrelated package evidence even
            // when an old collector accidentally appended another app's header to that row.
            var firstLine=DiagnosticEvent.parse(text.lines().findFirst().orElse(""),row.getSourceGroup(),row.getId());
            if(firstLine!=null&&!firstLine.packageName().equals("com.miui.home"))continue;
            DiagnosticEvent e=decoded.get(cacheKey);
            if(e==null){e=DiagnosticEvent.parse(text,row.getSourceGroup(),row.getId());if(e!=null){if(decoded.size()>=3000)decoded.remove(decoded.keySet().iterator().next());decoded.put(cacheKey,e);}}
            if(e==null){malformed++;continue;}
            if(!e.packageName().equals("com.miui.home"))continue;
            if(!provenance(row,e)){rejected++;continue;}
            events.add(e);pids.add(e.pid());
            if(e.phase().equals("PROCESS_STARTED")){
                var old=anchors.get(e.pid()+"/"+e.type());if(old==null||old.time()<e.time())anchors.put(e.pid()+"/"+e.type(),e);
            }
        }
        ProcessEvidence process=ProcessEvidence.query(context,pids);
        // A live process name is independent evidence. A mismatched claimed process is rejected.
        for(var it=events.iterator();it.hasNext();){var e=it.next();String live=process.names.get(e.pid());if(live!=null&&!live.equals(e.processName())){it.remove();rejected++;}}
        boolean complete=process.complete&&scopeSnapshot!=null;
        Set<String> collectionSeen=new HashSet<>();
        for(var row:rows){
            if(row.getTag().startsWith("LogSync.")&&!collectionSeen.add(row.getTag()))continue;
            if(row.getTag().equals("HyperCeiler.HookDiagnostics"))continue;
            int pid=sourcePid(row);DiagnosticEvent anchor=anchors.get(pid+"/NATIVE_INLINE");
            if(anchor!=null&&(anchor.time()>row.getTimestamp()||!anchor.processStart().equals(process.starts.get(pid))))anchor=null;
            // Legacy native records need the same logd UID/PID check as structured events.
            if(row.getTag().startsWith("HyperCeiler.Home")&&!provenance(row,new DiagnosticEvent("probe","","NATIVE_INLINE","com.miui.home","com.miui.home",pid,"","","","","",row.getTimestamp(),"","","","","",0)))continue;
            var e=LegacyDiagnosticAdapter.parse(row.getTag(),row.getMessage(),row.getSourceGroup(),row.getId(),row.getTimestamp(),pid,anchor);
            if(e!=null){events.add(e);if(e.code().equals("ROOT_UNAVAILABLE")||e.code().equals("LOGS_INCOMPLETE")||e.code().equals("HOME_RECORDS_MISSING"))complete=false;}
        }
        Set<String> installed=new HashSet<>();installed.add("apk-source:"+digest(context.getApplicationInfo().sourceDir));
        String nativeId=installedNativeId();if(!nativeId.isEmpty())installed.add(nativeId);
        String environment="本版仅诊断系统桌面（com.miui.home），其他应用不纳入结论。\nSDK "+Build.VERSION.SDK_INT+" / "+Build.DISPLAY+"\n已安装模块："+context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName+"\n安装 APK 与运行进程版本分别核实；未获得 LSPosed 启用/作用域权威状态时保持未知。";
        try {
            var service=com.sevtinge.hyperceiler.utils.ScopeManager.getService();
            if(service==null){environment+="\nLSPosed 服务：未连接，启用状态和作用域未知。";complete=false;}
            else {
                // Use the same scope snapshot as provenance and state analysis.
                // The service may connect midway through this refresh; a second
                // query would misleadingly label the earlier unknown batch complete.
                environment+="\n框架服务已连接，API="+service.getApiVersion()+"；系统桌面作用域="+(scopeSnapshot==null?"未知（本批次读取不可用，请刷新）":scopeSnapshot.contains("com.miui.home"))+"。作用域不代表桌面进程已经加载模块。";
                try {for(var target:service.getRunningTargets().stream().sorted(java.util.Comparator.comparing(io.github.libxposed.service.HookedTarget::getProcessName).thenComparingInt(io.github.libxposed.service.HookedTarget::getPid)).toList())if(target.getProcessName().equals("com.miui.home"))environment+="\n框架进程记录："+target.getProcessName()+" PID="+target.getPid()+" 状态="+target.getState()+" 运行模块 versionCode="+target.getLoadedVersionCode();}
                catch(Exception unsupported){environment+="\n运行进程列表不可用，不能用当前安装版本代替。";}
            }
        }catch(Exception unavailable){environment+="\n框架作用域读取不可用，保持未知。";complete=false;}
        var env=new DiagnosticRules.Environment(process.boot,scopeSnapshot==null?Map.of():Map.copyOf(process.starts),process.complete&&scopeSnapshot!=null,System.currentTimeMillis(),complete,installed);
        var report=DiagnosticRules.analyze(events,env,malformed,rejected,environment);
        String text=DiagnosticRules.reportText(report),fingerprint=digest(text);
        var previous=dao.latest();
        if(previous==null||!previous.fingerprint.equals(fingerprint))dao.save(new DiagnosticSnapshotEntity(fingerprint,System.currentTimeMillis(),text));
        dao.trim();return report;
    }
    private boolean provenance(LogEntry row,DiagnosticEvent e){
        try{
            if(scopeSnapshot!=null&&!scopeSnapshot.contains(e.packageName())&&!(e.packageName().equals("android")&&scopeSnapshot.contains("system")))return false;
            Integer expected=uidSnapshot.get(e.packageName());
            if(expected==null){expected=context.getPackageManager().getPackageUid(e.packageName(),0);uidSnapshot.put(e.packageName(),expected);}
            return DiagnosticProvenance.matches(row.getSourceGroup(),row.getProcessIds(),row.getTimestamp(),e,expected);
        }catch(Exception invalid){return false;}
    }
    private static int sourcePid(LogEntry row){try{String[] p=row.getProcessIds().split(":");return p.length>=2?Integer.parseInt(p[1]):0;}catch(Exception invalid){return 0;}}
    private static String digest(String text)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
    private String installedNativeId(){
        // Read this app's APK only, not a process's arbitrary memory. Match the native GNU Build ID.
        try(var zip=new java.util.zip.ZipFile(context.getApplicationInfo().sourceDir)){
            var entry=zip.getEntry("lib/arm64-v8a/libHyperCeilerNative.so");if(entry==null||entry.getSize()>32*1024*1024)return "";
            byte[] bytes;try(var input=zip.getInputStream(entry)){bytes=input.readNBytes(32*1024*1024);}
            return ElfBuildId.read(bytes);
        }catch(Exception unavailable){return "";}
    }
}
