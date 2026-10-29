/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.base;

import android.os.Build;
import android.os.Process;
import com.sevtinge.hyperceiler.common.diagnostics.DiagnosticEvent;
import com.sevtinge.hyperceiler.common.diagnostics.DiagnosticRules;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IReplaceHook;

/** Observation only: never installs a second hook, changes a return value or consumes an exception. */
public final class HookDiagnostics {
    private static volatile String process="",boot="",start="",generation="",module="";
    private static volatile boolean deferred,enabled;
    private static final ThreadLocal<String> OWNER=new ThreadLocal<>();
    private static final LinkedHashMap<String,String> LAST=new LinkedHashMap<>();
    private HookDiagnostics(){}
    public static void bootstrap(String processName,String modulePath){
        try{
            process=processName==null?"":processName;
            enabled=isDesktopProcess(process);
            if(!enabled)return;
            try { boot=new String(Files.readAllBytes(new File("/proc/sys/kernel/random/boot_id").toPath()), StandardCharsets.UTF_8).trim(); } catch (Exception unavailable) { boot=""; }
            try { String stat=new String(Files.readAllBytes(new File("/proc/self/stat").toPath()), StandardCharsets.UTF_8);
            start=stat.substring(stat.lastIndexOf(')')+2).split(" ")[19]; } catch (Exception unavailable) { start=""; }
            generation=UUID.randomUUID().toString();
            module="apk-source:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(modulePath.getBytes(StandardCharsets.UTF_8)));
            synchronized(LAST){LAST.clear();}
            event("module.java","Java 模块入口","PROCESS_STARTED","MODULE_ENTRY","JAVA_ENTRY","Java entry executed; effect unverified");
        }catch(Throwable ignored){/* Diagnostics cannot interfere with module bootstrap. */}
    }
    public static boolean isDesktopProcess(String name){return "com.miui.home".equals(name)||(name!=null&&name.startsWith("com.miui.home:"));}
    public static void deferred(boolean value){deferred=value;}
    public static final class Scope implements AutoCloseable {
        private final String previous;
        Scope(String id){previous=OWNER.get();OWNER.set(id);}
        @Override public void close(){if(previous==null)OWNER.remove();else OWNER.set(previous);}
    }
    public static Scope scope(BaseHook hook){return new Scope(hook.getClass().getName());}
    public static String owner(){String id=OWNER.get();return id==null?"java.unattributed":id;}
    public static void lifecycle(BaseHook hook,String phase,String stage,String code){
        if(enabled&&hook!=null)event(hook.getClass().getName(),hook.TAG,phase,stage,code,"");
    }
    public static void failure(String id,String stage,Throwable error){
        try{
            StringBuilder summary=new StringBuilder(stage+": "+error.getClass().getName());
            // Stack frames only; exception messages may contain account data or method arguments.
            for(var frame:error.getStackTrace()){if(summary.length()>1000)break;summary.append("\n at ").append(frame);}
            event(id,id,"FAILED",stage,DiagnosticRules.errorCode(error,stage),summary.toString());
        }catch(Throwable ignored){}
    }
    public static void event(String id,String feature,String phase,String stage,String code,String detail){
        if(!enabled)return;
        try{
            String key=id+"/"+phase+"/"+code;
            synchronized(LAST){
                if(Objects.equals(LAST.get(key),detail))return;
                if(LAST.size()>=512)LAST.remove(LAST.keySet().iterator().next());
                LAST.put(key,detail);
            }
            String pkg=BaseLoad.getPackageName();
            if(pkg==null||pkg.isEmpty())pkg=process.contains(":")?process.substring(0,process.indexOf(':')):process;
            if(pkg.equals("system_server")||pkg.equals("system"))pkg="android";
            var e=new DiagnosticEvent(id,feature,"JAVA",pkg,process,Process.myPid(),boot,start,generation,module,
                Build.DISPLAY+"/SDK"+Build.VERSION.SDK_INT,System.currentTimeMillis(),phase,stage,code,detail,"java",0);
            ModuleLog.i("HyperCeiler.HookDiagnostics",e.wire());
        }catch(Throwable ignored){}
    }
    public static final class Observation {
        final String id;
        final AtomicBoolean ran=new AtomicBoolean(),failed=new AtomicBoolean();
        Observation(String suffix){id=owner()+"/"+suffix;}
        void ran(){if(!ran.get()&&ran.compareAndSet(false,true))event(id,id,"EXECUTED","CALLBACK","EXECUTED","Hook callback entered; no final-effect assertion");}
        void failed(Throwable error){if(!failed.get()&&failed.compareAndSet(false,true))failure(id,"CALLBACK",error);}
        void installed(Object result){event(id,id,result==null?"FAILED":deferred?"WAITING":"INSTALLED","INSTALL",result==null?"INSTALL_FAILED":deferred?"HOT_RELOAD_PENDING":"INSTALL_OK","Framework returned "+(result==null?"no handle":"a handle; effect unverified"));}
    }
    public static Observation observation(String name){return new Observation(name);}
    public static IMethodHook observe(Observation o,IMethodHook callback){
        if(callback==null||!enabled)return callback;
        final boolean beforeObserved=overrides(callback,"before"),afterObserved=overrides(callback,"after");
        return new IMethodHook(){
            @Override public void before(HookParam p){if(beforeObserved)o.ran();try{callback.before(p);}catch(Throwable e){o.failed(e);HookDiagnostics.<RuntimeException>rethrow(e);}}
            @Override public void after(HookParam p){if(afterObserved)o.ran();try{callback.after(p);}catch(Throwable e){o.failed(e);HookDiagnostics.<RuntimeException>rethrow(e);}}
            @Override public void setObjectExtra(HookParam p,String k,Object v){callback.setObjectExtra(p,k,v);}
            @Override public Object getObjectExtra(HookParam p,String k){return callback.getObjectExtra(p,k);}
        };
    }
    private static boolean overrides(IMethodHook callback,String phase){
        try{return callback.getClass().getMethod(phase,HookParam.class).getDeclaringClass()!=IMethodHook.class;}
        catch(Throwable unavailable){return false;} // Lack of introspection is not execution evidence.
    }
    public static IReplaceHook observe(Observation o,IReplaceHook callback){if(callback==null||!enabled)return callback;return p->{o.ran();try{return callback.replace(p);}catch(Throwable e){o.failed(e);return HookDiagnostics.<RuntimeException,Object>throwValue(e);}};}
    public static XposedInterface.Hooker observe(Observation o,XposedInterface.Hooker callback){if(callback==null||!enabled)return callback;return chain->{o.ran();try{return callback.intercept(chain);}catch(Throwable e){if(!o.failed.get()&&o.failed.compareAndSet(false,true))event(o.id,o.id,"DEGRADED","CALLBACK","CALLBACK_EXCEPTION_OBSERVED","Exception propagated through intercept; may originate in the original target: "+e.getClass().getName());throw e;}};}
    public static Object[] observeArgs(Observation o,Object[] args){
        Object[] out=args.clone();if(out.length==0)return out;
        Object last=out[out.length-1];
        if(last instanceof IMethodHook hook)out[out.length-1]=observe(o,hook);
        else if(last instanceof IReplaceHook hook)out[out.length-1]=observe(o,hook);
        else if(last instanceof XposedInterface.Hooker hook)out[out.length-1]=observe(o,hook);
        return out;
    }
    public static <T>T install(Observation o,Supplier<T> installer){
        if(!enabled)return installer.get();
        try{T result=installer.get();o.installed(result);return result;}
        catch(Throwable e){failure(o.id,"INSTALL",e);return HookDiagnostics.<RuntimeException,T>throwValue(e);}
    }
    @SuppressWarnings("unchecked") private static <E extends Throwable>void rethrow(Throwable e)throws E{throw(E)e;}
    @SuppressWarnings("unchecked") private static <E extends Throwable,T>T throwValue(Throwable e)throws E{throw(E)e;}
}
