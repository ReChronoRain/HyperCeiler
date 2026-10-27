import com.sevtinge.hyperceiler.common.diagnostics.*;
import com.sevtinge.hyperceiler.diagnostics.ElfBuildId;
import java.util.*;
import java.nio.*;

public class RulesTest {
    static int checks;
    static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    static DiagnosticEvent event(String id,String phase,String code,long time){return new DiagnosticEvent(id,"测试功能 "+id,"JAVA","com.miui.home","com.miui.home",123,"boot","500","g1","apk-source:build","SDK37",time,phase,phase,code,"evidence","logcat",time);}
    static DiagnosticRules.Environment env(){return new DiagnosticRules.Environment("boot",Map.of(123,"500"),true,5000,true,Set.of("apk-source:build"));}
    static DiagnosticRules.Finding one(DiagnosticEvent...e){return DiagnosticRules.analyze(List.of(e),env(),0,0,"test").findings().get(0);}
    static DiagnosticEvent generation(DiagnosticEvent e,String g,String start){return new DiagnosticEvent(e.hookId(),e.feature(),e.type(),e.packageName(),e.processName(),e.pid(),e.bootId(),start,g,e.moduleVersion(),e.systemVersion(),e.time(),e.phase(),e.stage(),e.code(),e.detail(),e.source(),e.rawId());}
    public static void main(String[] args){
        var installed=event("h","INSTALLED","INSTALL_OK",1000);
        check(DiagnosticEvent.parse(installed.wire(),"logcat",1)!=null,"wire round trip");
        check(one(installed).state()==DiagnosticRules.State.INSTALLED,"installed never executed remains pending");
        check(DiagnosticRules.stateLabel(one(installed).state()).equals("待执行"),"install stays pending");
        var ran=event("h","EXECUTED","EXECUTED",1100);
        check(one(installed,ran).state()==DiagnosticRules.State.ACTIVE,"explicit callback evidence");
        check(DiagnosticRules.stateLabel(one(installed,ran).state()).equals("成功"),"execution is presented as success");
        check(one(ran,installed).state()==DiagnosticRules.State.ACTIVE,"time order preserves execution");
        check(one(event("h","INITIALIZED","INIT_COMPLETE",1000)).state()==DiagnosticRules.State.WAITING,"init complete not installed");
        check(!DiagnosticProvenance.matches("logcat","10111:123:123",1000,installed,10139),"reject forged UID");
        check(!DiagnosticProvenance.matches("logcat","10139:456:123",1000,installed,10139),"reject forged PID");
        check(DiagnosticProvenance.matches("logcat","u0_a139:123:123",1000,installed,10139),"numeric and symbolic logd UID");
        check(!DiagnosticProvenance.matches("log","10139:123:123",1000,installed,10139),"do not trust structured text in arbitrary file logs");
        var failure=event("h","FAILED","METHOD_NOT_FOUND",1050);
        check(one(failure).state()==DiagnosticRules.State.FAILED,"Java init failure");
        check(DiagnosticRules.errorCode(new RuntimeException(new NoSuchMethodException()),"INIT").equals("METHOD_NOT_FOUND"),"cause classification");
        check(one(event("h","FAILED","RESOLVE_FAILED",1000)).code().equals("RESOLVE_FAILED"),"Native resolver failure");
        check(one(event("h","FAILED","INSTALL_FAILED",1000)).installation().equals("安装失败"),"Native install failure");
        check(one(installed,event("h","DEGRADED","HOOK_LOST",1100)).state()==DiagnosticRules.State.DEGRADED,"patch lost");
        check(one(failure,ran).state()==DiagnosticRules.State.FAILED&&one(failure,ran).code().equals("METHOD_NOT_FOUND"),"failure not erased by callback");
        check(one(failure,event("h","DEGRADED","RUNTIME_EXCEPTION",1300)).code().equals("METHOD_NOT_FOUND"),"downstream warning does not erase direct root failure");
        var nativeInstalled=new DiagnosticEvent("n","Native","NATIVE_INLINE","com.miui.home","com.miui.home",123,"boot","500","g1","unknown","SDK",1400,"INSTALLED","INSTALL_HEALTH","REARMED","verified","logcat",1);
        var nativeLost=new DiagnosticEvent("n","Native","NATIVE_INLINE","com.miui.home","com.miui.home",123,"boot","500","g1","unknown","SDK",1300,"DEGRADED","HEALTH","HOOK_LOST","lost","logcat",2);
        check(one(nativeLost,nativeInstalled).state()==DiagnosticRules.State.INSTALLED,"verified native rearm recovers installation only");
        check(DiagnosticRules.analyze(List.of(),env(),0,0,"").findings().isEmpty(),"no logs never means healthy");
        var unavailable=new DiagnosticRules.Environment("boot",Map.of(),false,5000,false,Set.of());
        var noRoot=DiagnosticRules.analyze(List.of(installed),unavailable,0,0,"");
        check(!noRoot.collectionComplete()&&noRoot.findings().get(0).state()==DiagnosticRules.State.UNKNOWN,"root unavailable preserves unknown");
        var old=generation(installed,"old","100");
        check(DiagnosticRules.analyze(List.of(old,installed),env(),0,0,"").findings().stream().anyMatch(f->f.state()==DiagnosticRules.State.STALE),"PID reuse and mixed old/new logs");
        var reload=generation(event("h","INSTALLED","INSTALL_OK",1200),"g2","500");
        check(DiagnosticRules.analyze(List.of(installed,reload),env(),0,0,"").findings().stream().anyMatch(f->f.state()==DiagnosticRules.State.STALE),"hot reload generation must supersede old records");
        var duplicates=DiagnosticRules.analyze(List.of(installed,installed,installed),env(),0,0,"").findings();
        check(duplicates.size()==1&&duplicates.get(0).occurrences()==3&&duplicates.get(0).events().size()==1,"duplicate aggregation");
        var module=event("module.apk","FAILED","MODULE_LOAD_FAILED",1000);
        var downstream=event("child","WAITING","UNKNOWN",1200);
        var roots=DiagnosticRules.analyze(List.of(module,downstream),env(),0,0,"").findings();
        check(roots.size()==1&&roots.get(0).code().equals("MODULE_LOAD_FAILED"),"upstream root cause coalesces same-process uncertainty");
        check(LegacyDiagnosticAdapter.parse("random","hook failed success error","logcat",1,1000,0,null)==null,"unknown log format not keyword diagnosed");
        check(DiagnosticEvent.parse(installed.wire().substring(0,40),"logcat",1)==null,"truncated event");
        check(DiagnosticEvent.parse("HCD1|"+"x".repeat(10000),"logcat",1)==null,"oversized corrupt event");
        check(one(event("h","APPLIED","INTERNAL_APPLIED",1200)).state()==DiagnosticRules.State.ACTIVE,"internal apply is successful");
        check(one(event("h","EFFECT_VERIFIED","VIEW_VERIFIED",1200)).state()==DiagnosticRules.State.ACTIVE,"old explicit verification remains successful");
        var summaryReport=DiagnosticRules.analyze(List.of(
            ran,ran,installed,
            event("applied","APPLIED","INTERNAL_APPLIED",1200),
            event("verified","EFFECT_VERIFIED","VIEW_VERIFIED",1200),
            event("pending","INSTALLED","INSTALL_OK",1200),
            event("waiting","WAITING","DEPENDENCY_WAIT",1200),
            event("init","INITIALIZING","INITIALIZING",1200),
            event("bad","EXECUTED","EXECUTED",1000),event("bad","FAILED","METHOD_NOT_FOUND",1200),
            event("lost","EXECUTED","EXECUTED",1000),event("lost","DEGRADED","HOOK_LOST",1200),
            event("off","EXECUTED","EXECUTED",1000),event("off","DISABLED","DISABLED",1200),
            event("na","NOT_APPLICABLE","NOT_APPLICABLE",1200),
            generation(event("stale","EXECUTED","EXECUTED",1200),"old","100"),
            event("unknown","UNRECOGNIZED","UNKNOWN",1200)),env(),0,0,"test");
        check(DiagnosticRules.counts(summaryReport).equals(new DiagnosticRules.Counts(3,2,3,1)),
            "success is current active only, duplicate and installed records do not double count, faults and disabled excluded");
        var text=DiagnosticRules.reportText(summaryReport);
        check(text.contains("成功，3，异常，2，待检查，3，信息不足，1")&&text.contains("[成功]"),"export shares overview counts and success label");
        check(!text.contains("最终效果")&&!text.contains("分别判定")&&!text.contains("安装:")&&!text.contains("执行:")&&!text.contains("效果:"),"report removes layered prompts");
        check(text.contains("EXECUTED/EXECUTED")&&text.contains("APPLIED/INTERNAL_APPLIED"),"raw lifecycle evidence preserved");
        var uncertainRun=DiagnosticRules.analyze(List.of(ran),unavailable,0,0,"");
        check(DiagnosticRules.counts(uncertainRun).success()==0&&DiagnosticRules.counts(uncertainRun).unknown()==1,"unverified current process is not success");
        var rearmed=DiagnosticRules.analyze(List.of(
            new DiagnosticEvent("n","Native","NATIVE_INLINE","com.miui.home","com.miui.home",123,"boot","500","g1","unknown","SDK",1200,"EXECUTED","COUNTER","EXECUTED","ran","logcat",3),
            nativeLost,nativeInstalled),env(),0,0,"");
        check(DiagnosticRules.counts(rearmed).success()==0&&DiagnosticRules.counts(rearmed).pending()==1,"rearmed hook waits for new execution");
        var nativeEntry=new DiagnosticEvent("module.native","Native 入口","NATIVE_INLINE","com.miui.home","com.miui.home",123,"boot","500","native","unknown","native",1000,"PROCESS_STARTED","MODULE_ENTRY","NATIVE_ENTRY","entry","logcat",1);
        var nativeOnly=DiagnosticRules.analyze(List.of(nativeEntry),env(),0,0,"");
        check(nativeOnly.findings().stream().anyMatch(f->f.code().equals("JAVA_ENTRY_UNVERIFIED")&&f.state()!=DiagnosticRules.State.FAILED),"Native-only never proves Java injection failure");
        var unknownBoot=new DiagnosticRules.Environment("",Map.of(123,"500"),false,5000,false,Set.of());
        check(DiagnosticRules.analyze(List.of(installed),unknownBoot,0,0,"").findings().get(0).state()==DiagnosticRules.State.UNKNOWN,"missing current boot identity cannot establish current state");
        var foreignBuild=new DiagnosticRules.Environment("boot",Map.of(123,"500"),true,5000,true,Set.of("apk-source:new-build"));
        check(DiagnosticRules.analyze(List.of(installed),foreignBuild,0,0,"").findings().get(0).state()==DiagnosticRules.State.STALE,"installed APK is not the loaded module");
        var denied=LegacyDiagnosticAdapter.parse("LogSync.Root","ROOT_UNAVAILABLE: denied","diagnostic",1,1100,0,null);
        check(one(denied).state()==DiagnosticRules.State.DEGRADED,"Root denied is collection problem");
        var missingHome=LegacyDiagnosticAdapter.parse("LogSync.HomeCoverage","NO_HOME_RECORDS: bounded tail is empty","diagnostic",2,1200,0,null);
        var missingReport=DiagnosticRules.analyze(List.of(missingHome),unavailable,0,0,"");
        check(missingHome.code().equals("HOME_RECORDS_MISSING")&&one(missingHome).state()==DiagnosticRules.State.WAITING,"empty desktop tail is pending evidence, not capture failure");
        check(!missingReport.collectionComplete()&&DiagnosticRules.counts(missingReport).abnormal()==0,"missing coverage stays incomplete without adding an abnormal feature");
        check(!one(missingHome).advice().contains("Root")&&one(missingHome).explanation().contains("未发现"),"empty tail has specific explanation without false Root advice");
        var captureFailed=LegacyDiagnosticAdapter.parse("LogSync.HookDiagnostics","LOGCAT_IMPORT_FAILED: exit=1","diagnostic",3,1300,0,null);
        check(one(captureFailed).state()==DiagnosticRules.State.DEGRADED,"actual capture failure remains abnormal");
        check(!one(captureFailed).advice().contains("授予"),"capture failure does not imply Root denial");
        var captureReady=LegacyDiagnosticAdapter.parse("LogSync.HookDiagnostics","LOGCAT_IMPORT_OK: done","diagnostic",4,1400,0,null);
        check(captureReady!=null&&one(captureFailed,captureReady).state()==DiagnosticRules.State.WAITING,"successful refresh retires an old capture failure");
        var homeReady=LegacyDiagnosticAdapter.parse("LogSync.HomeCoverage","HOME_RECORDS_PRESENT: current logs found","diagnostic",5,1500,0,null);
        check(homeReady!=null&&one(missingHome,homeReady).code().equals("COLLECTION_READY"),"desktop evidence recovery retires missing coverage");
        var legacy=LegacyDiagnosticAdapter.parse("LSPosed.ModuleLoad","MODULE_APK_LOAD_FAILED: Unable to open '/data/app/com.sevtinge.hyperceiler-abc/base.apk' ENOENT","log",1,1000,42,null);
        check(legacy!=null&&legacy.code().equals("MODULE_LOAD_FAILED")&&one(legacy).state()==DiagnosticRules.State.UNKNOWN,"explicit load failure without current target must remain uncorrelated");
        String secret="Authorization: Bearer abcXYZ\n{\"token\":\"sensitiveValue\"} mail a@example.com password=pass123 https://x.test/?token=verysecret";
        String redacted=DiagnosticRules.redact(secret);
        for(String value:new String[]{"abcXYZ","sensitiveValue","a@example.com","pass123","verysecret"})check(!redacted.contains(value),"redaction "+value);
        byte[] elf=new byte[160];var b=ByteBuffer.wrap(elf).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0,0x464c457f);elf[4]=2;elf[5]=1;b.putLong(32,64);b.putShort(54,(short)56);b.putShort(56,(short)1);b.putInt(64,4);b.putLong(72,120);b.putLong(96,24);
        b.putInt(120,4);b.putInt(124,4);b.putInt(128,3);elf[132]='G';elf[133]='N';elf[134]='U';elf[136]=1;elf[137]=2;elf[138]=3;elf[139]=4;
        check(ElfBuildId.read(elf).equals("elf:01020304"),"real layout GNU note");
        b.putLong(72,Long.MAX_VALUE);check(ElfBuildId.read(elf).isEmpty(),"malformed ELF offset rejected");
        System.out.println("Hook diagnostics rules PASS: "+checks+" checks");
    }
}
