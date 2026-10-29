import com.sevtinge.hyperceiler.common.diagnostics.*;
import java.util.*;

public final class SummaryTest {
    static int checks;
    static void ok(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    static DiagnosticEvent event(String id,String name,String phase,String code){
        return new DiagnosticEvent(id,name,"JAVA","com.miui.home","com.miui.home",123,"boot","500","g1","apk-source:current","SDK37",1000,phase,phase,code,"technical evidence","logcat",1000);
    }
    static DiagnosticRules.Report report(DiagnosticEvent... events){
        return DiagnosticRules.analyze(List.of(events),new DiagnosticRules.Environment("boot",Map.of(123,"500"),true,2000,true,Set.of("apk-source:current")),0,0,"");
    }
    public static void main(String[] args){
        var a=event("scale/first","普通图标缩放","EXECUTED","EXECUTED");
        var b=event("scale/second","普通图标缩放","INSTALLED","INSTALL_OK");
        var raw=report(a,b);var grouped=DiagnosticSummary.from(raw);
        ok(raw.findings().size()==2&&grouped.features().size()==1,"aggregate semantic feature without deleting raw findings");
        ok(grouped.counts().success()==1&&grouped.counts().pending()==0,"execution remains success even with another pending internal hook");
        ok(DiagnosticRules.counts(raw).equals(grouped.counts()),"overview and exported report share the feature projection");
        ok(grouped.features().get(0).findings().size()==2,"all diagnostic records available from feature detail");
        ok(grouped.headline().equals("已检查的功能运行正常"),"normal headline refers only to inspected features");
        var error=event("scale/failure","普通图标缩放","FAILED","METHOD_NOT_FOUND");
        grouped=DiagnosticSummary.from(report(a,b,error));
        ok(grouped.counts().abnormal()==1&&grouped.counts().success()==0,"a failure from any current path wins over execution");
        ok(grouped.features().get(0).representative().code().equals("METHOD_NOT_FOUND"),"advice is based on failure rather than arbitrary last record");
        ok(grouped.features().get(0).explanation().contains("适配检查")&&grouped.features().get(0).advice().contains("桌面版本"),"user explanation and useful feedback instructions");
        var stale=new DiagnosticEvent("scale/old","普通图标缩放","JAVA","com.miui.home","com.miui.home",123,"boot","500","old","apk-source:old","SDK37",900,"FAILED","INSTALL","INSTALL_FAILED","old failure","logcat",900);
        grouped=DiagnosticSummary.from(report(a,stale));
        ok(grouped.counts().success()==1&&grouped.counts().abnormal()==0&&grouped.counts().unknown()==0,"old failures never contaminate current counts");
        ok(grouped.history().size()==1&&grouped.features().get(0).findings().size()==1,"history is retained separately without sharing active advice");
        var root=LegacyDiagnosticAdapter.parse("LogSync.Root","ROOT_UNAVAILABLE: denied","diagnostic",3,1000,0,null);
        grouped=DiagnosticSummary.from(report(a,root));
        ok(grouped.counts().abnormal()==0&&grouped.counts().success()==1,"capture problem is not a broken launcher feature");
        ok(grouped.preparationIncomplete()&&grouped.preparation().size()==1&&grouped.headline().contains("不完整"),"capture limitations shown above feature results");
        ok(grouped.preparation().get(0).advice().contains("Root 管理器"),"root-specific action only when denial is observed");
        var ready=LegacyDiagnosticAdapter.parse("LogSync.Root","ROOT_READY: granted","diagnostic",4,1001,0,null);
        grouped=DiagnosticSummary.from(report(ready));
        ok(grouped.features().isEmpty()&&grouped.counts().equals(new DiagnosticRules.Counts(0,0,0,0)),"preparation readiness is never counted as a feature success or pending feature");
        ok(grouped.headline().contains("还没有")&&!grouped.headline().contains("正常"),"empty record set cannot look like all features passed");
        var missing=LegacyDiagnosticAdapter.parse("LogSync.HomeCoverage","NO_HOME_RECORDS: empty","diagnostic",5,1000,0,null);
        var instruction=DiagnosticSummary.from(report(missing)).preparation().get(0);
        ok(DiagnosticSummary.from(report(missing)).preparationSummary().contains("没有采集到"),"preparation row shows actual reason instead of a generic technical status");
        ok(instruction.advice().contains("作用域")&&!instruction.advice().contains("Root"),"missing desktop records do not invent a Root problem");
        var entry=event("module.java","Java 模块入口","PROCESS_STARTED","JAVA_ENTRY");
        grouped=DiagnosticSummary.from(report(entry,a));
        ok(grouped.counts().success()==1&&grouped.counts().pending()==0&&grouped.preparation().size()==1,"module entry kept in preparation only");
        var off=event("off","小组件缩放","DISABLED","DISABLED");
        var na=event("na","大文件夹缩放","NOT_APPLICABLE","NOT_APPLICABLE");
        grouped=DiagnosticSummary.from(report(off,na));
        ok(grouped.counts().equals(new DiagnosticRules.Counts(0,0,0,0))&&grouped.features().size()==2,"inactive features stay visible separately without inflating success");
        ok(grouped.features().stream().filter(f->f.state()==DiagnosticRules.State.NOT_APPLICABLE).findFirst().orElseThrow().explanation().contains("未说明"),"condition mismatch is not guessed to be a disabled preference");
        ok(DiagnosticPresentation.operation("小文件夹缩放").contains("打开并关闭"),"folder-specific reproduction step");
        ok(DiagnosticPresentation.operation("最近任务清理按钮").contains("最近任务"),"recents-specific reproduction step");
        ok(DiagnosticPresentation.operation("小组件缩放").contains("放有小组件"),"widget-specific reproduction step");
        var unknownA=event("unknown/first","挂钩用途未知","WHATEVER","UNKNOWN");
        var unknownB=event("unknown/second","挂钩用途未知","WHATEVER","UNKNOWN");
        grouped=DiagnosticSummary.from(report(unknownA,unknownB));
        ok(grouped.features().size()==2&&grouped.counts().unknown()==2,"unnamed unrelated records never merged as one semantic feature");
        try{grouped.features().get(0).findings().clear();ok(false,"immutable findings");}catch(UnsupportedOperationException expected){checks++;}
        String text=DiagnosticRules.reportText(report(a,b,stale));
        ok(text.contains("成功，1，异常，0，待检查，0，信息不足，0")&&text.contains("上次运行记录，1"),"report clearly separates current feature totals from history");
        ok(text.contains("下一步，")&&text.contains("原始技术记录")&&text.contains("scale/old"),"export contains user instructions and retained technical evidence");
        for(var item:DiagnosticSummary.from(report(a,b,error,off,na,root,missing,unknownA,stale)).features()) {
            var copy=DiagnosticPresentation.prose(item.explanation()+"\n"+item.advice());
            ok(!copy.contains("PID")&&!copy.contains("ClassLoader")&&!copy.contains("METHOD_NOT_FOUND"),"ordinary feature copy has no implementation jargon");
        }
        System.out.println("Diagnostic summary PASS "+checks+" checks, semantic grouping, failure precedence, history isolation, preparation separation, actionable copy and shared export counts");
    }
}
