/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.diagnostics;

import java.util.*;
import java.util.regex.Pattern;

/** Pure deterministic reducer. No Android, I/O, hooks, settings or automatic repairs. */
public final class DiagnosticRules {
    public enum State { DISABLED, NOT_APPLICABLE, WAITING, INITIALIZING, INSTALLED, ACTIVE, FAILED, DEGRADED, UNKNOWN, STALE }
    public record Environment(String bootId, Map<Integer,String> liveStarts, boolean processProbeComplete,
                              long now, boolean collectionComplete, Set<String> installedVersions) {}
    public record Finding(String id,String feature,String type,String packageName,String processName,int pid,
        String generation,String moduleVersion,String systemVersion,long time,State state,String failureStage,String configuration,
        String installation,String execution,String code,String rule,String severity,String evidence,
        String explanation,String advice,int occurrences,List<DiagnosticEvent> events) {}
    public record Report(List<Finding> findings,boolean collectionComplete,int malformed,int rejected,String environment) {}
    public record Counts(long success,long abnormal,long pending,long unknown) {}
    public static Counts counts(Report report){
        return DiagnosticSummary.from(report).counts();
    }

    public static String stateLabel(State state){return switch(state){
        case ACTIVE->"成功";case INSTALLED->"待执行";case FAILED->"明确异常";case DEGRADED->"部分异常";
        case DISABLED->"已关闭";case NOT_APPLICABLE->"条件不满足";case WAITING->"等待操作或依赖";
        case INITIALIZING->"初始化中";case STALE->"历史记录已过期";case UNKNOWN->"信息不足";
    };}
    private DiagnosticRules(){}
    public static String errorCode(Throwable error,String stage){
        int depth=0;for(Throwable t=error;t!=null&&depth++<16;t=t.getCause()){
            if(t instanceof ClassNotFoundException||t instanceof NoClassDefFoundError)return "CLASS_NOT_FOUND";
            if(t instanceof NoSuchMethodException||t instanceof NoSuchMethodError)return "METHOD_NOT_FOUND";
            if(t instanceof NoSuchFieldException||t instanceof NoSuchFieldError)return "FIELD_NOT_FOUND";
            if(t instanceof ClassCastException)return "CLASSLOADER_OR_TYPE_MISMATCH";
            if(t instanceof UnsatisfiedLinkError)return "NATIVE_DEPENDENCY_FAILED";
            if(t.getCause()==t)break;
        }
        return switch(stage){case "DEXKIT"->"DEXKIT_FAILED";case "CALLBACK"->"RUNTIME_EXCEPTION";
            case "PREFERENCES"->"CONFIG_READ_FAILED";case "HOT_RELOAD"->"HOT_RELOAD_FAILED";case "INSTALL"->"INSTALL_FAILED";default->"INITIALIZATION_FAILED";};
    }
    public static String advice(String code){return switch(code){
        case "MODULE_LOAD_FAILED"->"检查 LSPosed 模块和作用域、模块 APK 可访问性；可采用非增量覆盖安装，确认后手动重启目标进程。";
        case "CLASS_NOT_FOUND","METHOD_NOT_FOUND","FIELD_NOT_FOUND","RESOLVE_FAILED","AMBIGUOUS_TARGET","VALIDATION_FAILED","ABI_UNSUPPORTED","DEXKIT_FAILED"->"当前系统或目标应用版本可能改变了目标特征。保留版本与证据，交由开发者更新适配；不要强行安装。";
        case "CLASSLOADER_OR_TYPE_MISMATCH"->"核对目标进程与 ClassLoader；类型转换失败本身不能证明 ClassLoader 一定错误。";
        case "HOOK_LOST","FOREIGN_MODIFICATION"->"检查同作用域其他模块及目标镜像重载；不要覆盖未知补丁，提供冲突前后的诊断报告。";
        case "CONFIG_READ_FAILED","DEPENDENCY_FAILED","NATIVE_DEPENDENCY_FAILED"->"检查配置服务和依赖是否就绪，刷新诊断；在明确恢复前不要把旧配置当作当前值。";
        case "ROOT_UNAVAILABLE"->"授予所需 Root 权限并刷新日志，日志缺失不等于 Hook 失败";
        case "LOGS_INCOMPLETE"->"查看关联采集日志中的具体错误并重新刷新，采集失败不等于桌面功能失败";
        case "HOME_RECORDS_MISSING"->"在桌面操作对应功能后重新刷新，日志可能已轮转或不在本次采集范围内，缺少记录不等于功能失败";
        case "HOT_RELOAD_FAILED","GENERATION_CHANGED"->"当前代际证据已过期或热重载未完成；确认后手动重启对应进程并重新收集。";
        case "CRASH_OBSERVED"->"发现目标崩溃或安全模式记录，但不能据此归因到某个 Hook；结合相同进程代际的异常栈排查。";
        default->"复现对应操作后重新诊断，并导出报告。";};}
    public static Report analyze(List<DiagnosticEvent> input,Environment env,int malformed,int rejected,String environment){
        input=new ArrayList<>(input.subList(0,Math.min(12000,input.size())));
        Set<String> javaEntries=new HashSet<>();
        for(var e:input)if(e.hookId().equals("module.java")&&e.phase().equals("PROCESS_STARTED"))javaEntries.add(e.scope()+"/"+e.bootId()+"/"+e.processStart());
        List<DiagnosticEvent> missingJava=new ArrayList<>();
        for(var e:input)if(e.hookId().equals("module.native")&&e.phase().equals("PROCESS_STARTED")&&!javaEntries.contains(e.scope()+"/"+e.bootId()+"/"+e.processStart()))
            missingJava.add(new DiagnosticEvent("module.java-evidence","Java 入口待核实","INJECTION",e.packageName(),e.processName(),e.pid(),e.bootId(),e.processStart(),e.generation(),e.moduleVersion(),e.systemVersion(),e.time(),"WAITING","INJECTION","JAVA_ENTRY_UNVERIFIED","Native entry is observed; absence of Java evidence is not proof of failed injection",e.source(),e.rawId()));
        input.addAll(missingJava);
        Map<String,List<DiagnosticEvent>> groups=new LinkedHashMap<>();
        input.stream().sorted(Comparator.comparingLong(DiagnosticEvent::time).thenComparingLong(DiagnosticEvent::rawId))
            .limit(12000).forEach(e->groups.computeIfAbsent(e.identity(),k->new ArrayList<>()).add(e));
        Map<String,Long> latestGeneration=new HashMap<>();
        for(var e:input)if(e.phase().equals("PROCESS_STARTED"))latestGeneration.merge(e.scope()+"/"+e.type(),e.time(),Math::max);
        Map<String,Long> newestHookGeneration=new HashMap<>();
        for(var entries:groups.values()){
            var first=entries.get(0);
            newestHookGeneration.merge(first.scope()+"/"+first.bootId()+"/"+first.processStart()+"/"+first.type()+"/"+first.hookId(),first.time(),Math::max);
        }
        List<Finding> findings=new ArrayList<>();
        for(var entries:groups.values()){
            var last=entries.get(entries.size()-1);State state=State.UNKNOWN;
            String config="未知",install="未验证",execute="未观察到",code="UNKNOWN";
            String failureStage="";
            String rule="EVIDENCE_INSUFFICIENT",evidence="当前运行记录",explanation="证据不足，不能确认当前状态。";
            Set<String> unique=new HashSet<>();List<DiagnosticEvent> kept=new ArrayList<>();
            boolean stale=!last.bootId().isEmpty()&&!env.bootId().isEmpty()&&!last.bootId().equals(env.bootId());
            String versionPrefix=last.moduleVersion().startsWith("elf:")?"elf:":"apk-source:";
            if((last.moduleVersion().startsWith("elf:")||last.moduleVersion().startsWith("apk-source:"))&&env.installedVersions().stream().anyMatch(v->v.startsWith(versionPrefix))&&!env.installedVersions().contains(last.moduleVersion()))stale=true;
            String live=env.liveStarts().get(last.pid());
            if(last.pid()>0&&!last.processStart().isEmpty()){
                if(live!=null&&!live.equals(last.processStart()))stale=true;
                if(live==null&&env.processProbeComplete())stale=true;
            }
            long newer=latestGeneration.getOrDefault(last.scope()+"/"+last.type(),0L);
            if(newer>last.time())stale=true;
            long firstSeen=entries.get(0).time();
            if(newestHookGeneration.getOrDefault(last.scope()+"/"+last.bootId()+"/"+last.processStart()+"/"+last.type()+"/"+last.hookId(),0L)>firstSeen)stale=true;
            for(var e:entries){
                String dedup=e.phase()+"/"+e.code()+"/"+e.detail();
                if(unique.add(dedup)){kept.add(e);if(kept.size()>16)kept.remove(0);}
                switch(e.phase()){
                    case "DISABLED"->{config="关闭";state=State.DISABLED;code=e.code();}
                    case "NOT_APPLICABLE"->{state=State.NOT_APPLICABLE;code=e.code();}
                    case "INITIALIZING"->{if(state!=State.FAILED&&state!=State.DEGRADED){state=State.INITIALIZING;code="INITIALIZING";}config="已请求";}
                    case "PROCESS_STARTED","INITIALIZED","WAITING"->{if(e.code().equals("CONFIG_READY")||e.code().equals("COLLECTION_READY")){state=State.WAITING;failureStage="";}if(state==State.UNKNOWN||state==State.INITIALIZING)state=State.WAITING;if(state!=State.FAILED&&state!=State.DEGRADED)code=e.code();}
                    case "INSTALLED"->{install="已安装";
                        if(e.type().startsWith("NATIVE")&&(e.code().equals("REARMED")||e.code().equals("INSTALL_OK"))
                            &&Set.of("HOOK_LOST","FOREIGN_MODIFICATION","INSTALL_FAILED","CONTINUATION_INVALID","MEMORY_PROTECTION_FAILED","VALIDATION_FAILED").contains(code)){
                            state=State.INSTALLED;failureStage="";execute="未观察到";
                        }
                        if(state!=State.FAILED&&state!=State.DEGRADED)state=execute.equals("已执行")?State.ACTIVE:State.INSTALLED;if(state!=State.FAILED&&state!=State.DEGRADED)code=e.code();}
                    case "EXECUTED","APPLIED","EFFECT_VERIFIED"->{execute="已执行";if(state!=State.FAILED&&state!=State.DEGRADED)state=State.ACTIVE;
                        if(state!=State.FAILED&&state!=State.DEGRADED)code=e.code();}
                    case "FAILED"->{if(state!=State.FAILED||failureRank(e.code())<failureRank(code)){code=e.code();failureStage=e.stage();}state=State.FAILED;if(e.code().equals("INSTALL_FAILED"))install="安装失败";}
                    case "DEGRADED"->{if(state!=State.FAILED){state=State.DEGRADED;code=e.code();failureStage=e.stage();}}
                    case "GENERATION_CHANGED"->{stale=true;code="GENERATION_CHANGED";}
                    default->{}
                }
            }
            if(stale){state=State.STALE;rule="GENERATION_STALE";evidence="历史证据";explanation="证据属于旧启动、旧进程或旧模块代际，不能代表当前状态。";}
            else if(last.time()>env.now()+60000){state=State.UNKNOWN;rule="CLOCK_UNVERIFIED";explanation="证据时间与当前设备时间不一致，无法可靠排序当前代际。";}
            else if(last.type().equals("INJECTION")&&last.processStart().isEmpty()){state=State.UNKNOWN;rule="INJECTION_PROCESS_UNVERIFIED";explanation="存在明确的模块加载失败记录，但不能确认它是否对应当前进程；请查看原始证据。";}
            else if((live==null||last.processStart().isEmpty()||last.bootId().isEmpty()||env.bootId().isEmpty())&&last.pid()>0){evidence="证据不足（当前进程未核实）";state=State.UNKNOWN;rule="PROCESS_UNVERIFIED";explanation="保留已观察到的运行记录，但无法核实它们是否对应当前进程。";}
            else {
                rule=code;explanation=switch(state){
                    case INSTALLED->"尚未观察到功能执行，请进行对应操作后重新诊断。";
                    case ACTIVE->"功能已执行，诊断结果计为成功。";
                    case FAILED,DEGRADED->reason(code);
                    case WAITING,INITIALIZING->"已观察到初始化或依赖等待；没有安装或执行证据不能判为失败。";
                    case DISABLED->"配置或模块开关明确关闭了该路径。";
                    case NOT_APPLICABLE->"初始化条件未满足；可能是功能开关或适用条件，当前入口没有区分原因。";default->explanation;};
            }
            if(code.equals("JAVA_ENTRY_UNVERIFIED")&&state!=State.STALE)explanation="已观察到 Native 入口，但 Java 入口证据不足；特殊孵化链路或日志轮转均可能影响观测，不能判为 Java 注入失败。";
            if(code.equals("HOME_RECORDS_MISSING")&&state==State.WAITING)explanation="本次采集范围内未发现桌面原生日志，等待补充运行记录，不能据此判断桌面功能失败";
            if(code.equals("COLLECTION_READY")&&state==State.WAITING)explanation="采集步骤已完成，具体功能状态以对应运行记录为准";
            findings.add(new Finding(last.identity(),DiagnosticFeatureNames.forEvent(last),last.type(),last.packageName(),last.processName(),last.pid(),last.generation(),last.moduleVersion(),last.systemVersion(),last.time(),state,failureStage,config,install,execute,code,rule,
                state==State.FAILED?"ERROR":state==State.DEGRADED?"WARN":"INFO",evidence,explanation,advice(code),entries.size(),List.copyOf(kept)));
        }
        // Upstream module-load failures only subsume uncertain dependents in the SAME explicit
        // process/time window. Never attribute unrelated processes or generations to that failure.
        List<Finding> roots=findings.stream().filter(f->f.code().equals("MODULE_LOAD_FAILED")&&f.state()==State.FAILED).toList();
        findings.removeIf(f->(f.state()==State.UNKNOWN||f.state()==State.WAITING)&&roots.stream().anyMatch(r->r.pid()>0&&r.pid()==f.pid()&&r.processName().equals(f.processName())&&Math.abs(r.time()-f.time())<=60000));
        findings.sort(Comparator.comparingInt((Finding f)->rank(f.state())).thenComparing(Comparator.comparingLong(Finding::time).reversed()));
        if(findings.size()>512)findings=new ArrayList<>(findings.subList(0,512));
        return new Report(List.copyOf(findings),env.collectionComplete(),malformed,rejected,environment);
    }
    private static int failureRank(String code){return switch(code){case "MODULE_LOAD_FAILED"->0;case "CONFIG_READ_FAILED","NATIVE_DEPENDENCY_FAILED"->1;case "CLASS_NOT_FOUND","METHOD_NOT_FOUND","FIELD_NOT_FOUND","DEXKIT_FAILED","RESOLVE_FAILED"->2;case "VALIDATION_FAILED","ABI_UNSUPPORTED"->3;case "INSTALL_FAILED","CONTINUATION_INVALID","MEMORY_PROTECTION_FAILED"->4;default->5;};}
    private static int rank(State s){return switch(s){case FAILED->0;case DEGRADED->1;case UNKNOWN->2;case INSTALLED,WAITING,INITIALIZING->3;case ACTIVE->4;default->5;};}
    private static String reason(String code){return switch(code){
        case "CLASS_NOT_FOUND"->"目标类不存在或未能由指定加载器解析。";case "METHOD_NOT_FOUND"->"方法定位失败，系统或应用更新可能改变了签名。";
        case "FIELD_NOT_FOUND"->"字段定位失败，目标结构可能发生变化。";case "RESOLVE_FAILED"->"运行时解析没有找到可验证目标。";
        case "AMBIGUOUS_TARGET"->"解析存在多个候选，无法唯一确认，已保留原行为。";case "VALIDATION_FAILED"->"目标指令、ABI 或数据流未通过安全校验。";
        case "INSTALL_FAILED"->"Hook 后端明确返回安装失败。";case "HOOK_LOST"->"已安装补丁丢失或健康检查不再通过。";
        case "FOREIGN_MODIFICATION"->"发现未知或第三方修改，不能安全覆盖。";case "MODULE_LOAD_FAILED"->"LSPosed 明确报告模块 APK 加载失败；后续 Hook 状态无法确认。";
        case "RUNTIME_EXCEPTION"->"Hook 回调报告异常；保留原异常处理语义。";case "CALLBACK_EXCEPTION_OBSERVED"->"拦截回调传播异常，可能来自原方法，不能单凭此证据归因到 Hook。";case "DEXKIT_FAILED"->"DexKit 解析失败或未返回必要目标。";
        case "HOT_RELOAD_FAILED"->"模块热重载明确失败。";case "ROOT_UNAVAILABLE","LOGS_INCOMPLETE"->"诊断数据不完整，不能据此判断其他 Hook 成败。";
        default->"已有明确的异常或拒绝记录；请查看失败阶段与原始证据。";};}
    private static final Pattern AUTH=Pattern.compile("(?im)(?:authorization|cookie|set-cookie)\\s*[:=]\\s*[^\\r\\n]+|https?://[^\\s]+|[\"']?(?:token|password|passwd|secret|account)[\"']?\\s*[:=]\\s*[\"']?[^\"',;\\s]+",Pattern.CASE_INSENSITIVE);
    private static final Pattern SENSITIVE=Pattern.compile("(?i)(authorization|bearer|token|password|passwd|cookie|secret|account|账户|账号)([\\s:=]+)[^\\s,;]+|/data/(?:user|user_de)/\\d+/[^\\s]+|[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}|(?:\\+?86[- ]?)?1[3-9]\\d{9}");
    public static String redact(String text){return SENSITIVE.matcher(AUTH.matcher(DiagnosticEvent.bound(text,2000)).replaceAll("[已脱敏]")).replaceAll("[已脱敏]");}
    public static String reportText(Report report){
        StringBuilder s=new StringBuilder("HyperCeiler Hook Diagnostics v1\n已执行的功能计为成功，明确异常与过期记录单独统计。\n");
        var summary=DiagnosticSummary.from(report);
        var counts=summary.counts();
        s.append("成功，").append(counts.success()).append("，异常，").append(counts.abnormal()).append("，待检查，").append(counts.pending()).append("，信息不足，").append(counts.unknown()).append('\n');
        s.append(summary.overview()).append('\n');
        s.append("上次运行记录，").append(summary.history().size()).append("，不计入当前功能数量\n");
        for(var feature:summary.features()) {
            s.append("\n功能，").append(redact(feature.name())).append("，").append(DiagnosticPresentation.label(feature.state()))
                .append("\n当前情况，").append(feature.explanation()).append("\n下一步，").append(feature.advice()).append('\n');
        }
        s.append("\n诊断环境与原始技术记录\n");
        s.append(redact(report.environment())).append("\n采集完整：").append(report.collectionComplete()).append("；格式损坏：").append(report.malformed()).append("；来源校验拒绝：").append(report.rejected()).append('\n');
        for(var f:report.findings()){
            if(s.length()>1024*1024){s.append("\n[报告达到 1 MiB 上限，后续证据未导出]\n");break;}
            s.append("\n[").append(stateLabel(f.state())).append("] ").append(redact(f.feature())).append("\nID: ").append(redact(f.id())).append("\n目标: ").append(f.packageName()).append('/').append(f.processName()).append(" PID=").append(f.pid()).append("\n运行模块: ").append(redact(f.moduleVersion())).append("\n配置: ").append(f.configuration()).append("\n失败阶段: ").append(f.failureStage()).append("\n规则: ").append(f.rule()).append("；证据: ").append(f.evidence()).append("；次数: ").append(f.occurrences()).append("\n原因: ").append(f.explanation()).append("\n建议: ").append(f.advice()).append('\n');
            for(var e:f.events())s.append("  ").append(e.time()).append(' ').append(e.phase()).append('/').append(e.code()).append(" source=").append(e.source()).append(" #").append(e.rawId()).append(' ').append(redact(e.detail())).append('\n');
        }
        return s.toString();
    }
}
