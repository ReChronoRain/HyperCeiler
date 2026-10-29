/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.diagnostics;

import java.util.*;
import static com.sevtinge.hyperceiler.common.diagnostics.DiagnosticRules.State;

/** User-facing projection only. Raw findings, process identity and failure precedence stay intact. */
public final class DiagnosticSummary {
    public record Feature(String key, String name, State state, List<DiagnosticRules.Finding> findings) {
        public DiagnosticRules.Finding representative() { return findings.get(0); }
        public String explanation() { return DiagnosticPresentation.explanation(representative()); }
        public String advice() { return DiagnosticPresentation.advice(representative()); }
    }
    private final DiagnosticRules.Report report;
    private final List<Feature> features, preparation;
    private final List<DiagnosticRules.Finding> history;
    private DiagnosticSummary(DiagnosticRules.Report report, List<Feature> features, List<Feature> preparation,
                              List<DiagnosticRules.Finding> history) {
        this.report=report;this.features=features;this.preparation=preparation;this.history=history;
    }
    public static DiagnosticSummary from(DiagnosticRules.Report report) {
        Map<String,List<DiagnosticRules.Finding>> current=new LinkedHashMap<>(),checks=new LinkedHashMap<>();
        List<DiagnosticRules.Finding> history=new ArrayList<>();
        for(var finding:report.findings()) {
            if(finding.state()==State.STALE){history.add(finding);continue;}
            boolean setup=isPreparation(finding);
            // Only identical, named semantic features can share a user-facing row.
            // Unattributed records must never become evidence for another unnamed feature.
            String key=finding.feature();
            if(key.contains("未知")||key.startsWith("未命名"))key+="/"+finding.id();
            (setup?checks:current).computeIfAbsent(key,k->new ArrayList<>()).add(finding);
        }
        return new DiagnosticSummary(report,group(current),group(checks),List.copyOf(history));
    }
    private static boolean isPreparation(DiagnosticRules.Finding finding) {
        return finding.type().equals("COLLECTION")||finding.type().equals("INJECTION")
            ||finding.events().stream().anyMatch(e->e.hookId().startsWith("module."));
    }
    private static List<Feature> group(Map<String,List<DiagnosticRules.Finding>> groups) {
        List<Feature> out=new ArrayList<>();
        for(var entry:groups.entrySet()) {
            var findings=new ArrayList<>(entry.getValue());
            findings.sort(Comparator.comparingInt((DiagnosticRules.Finding f)->priority(f.state()))
                .thenComparing(Comparator.comparingLong(DiagnosticRules.Finding::time).reversed()));
            var first=findings.get(0);
            out.add(new Feature(entry.getKey(),first.feature(),first.state(),List.copyOf(findings)));
        }
        out.sort(Comparator.comparingInt((Feature f)->priority(f.state())).thenComparing(Feature::name));
        return List.copyOf(out);
    }
    private static int priority(State state) { return switch(state) {
        case FAILED->0;case DEGRADED->1;case ACTIVE->2;case INSTALLED,WAITING,INITIALIZING->3;
        case UNKNOWN->4;case DISABLED->5;case NOT_APPLICABLE->6;case STALE->7;
    }; }
    public List<Feature> features(){return features;}
    public List<Feature> preparation(){return preparation;}
    public List<DiagnosticRules.Finding> history(){return history;}
    public DiagnosticRules.Counts counts(){
        long success=0,abnormal=0,pending=0,unknown=0;
        for(var feature:features)switch(feature.state()){
            case ACTIVE->success++;
            case FAILED,DEGRADED->abnormal++;
            case INSTALLED,WAITING,INITIALIZING->pending++;
            case UNKNOWN->unknown++;
            default->{}
        }
        return new DiagnosticRules.Counts(success,abnormal,pending,unknown);
    }
    public boolean preparationIncomplete(){
        return !report.collectionComplete()||preparation.stream().anyMatch(f->switch(f.state()){
            case FAILED,DEGRADED,UNKNOWN->true;default->f.representative().code().equals("JAVA_ENTRY_UNVERIFIED")
                ||f.representative().code().equals("HOME_RECORDS_MISSING");
        });
    }
    public String preparationSummary(){
        if(!preparationIncomplete())return "运行记录已读取，点此查看检查范围和准备情况";
        for(var check:preparation) {
            String code=check.representative().code();
            if(check.state()==State.FAILED||check.state()==State.DEGRADED||check.state()==State.UNKNOWN
                ||code.equals("JAVA_ENTRY_UNVERIFIED")||code.equals("HOME_RECORDS_MISSING"))
                return check.explanation()+"\n"+check.advice();
        }
        return "还未确认完整的桌面运行信息，点此查看检查方法";
    }
    public String headline(){
        var counts=counts();
        if(counts.abnormal()>0)return "有 "+counts.abnormal()+" 项功能需要处理";
        if(preparationIncomplete())return "诊断信息还不完整";
        if(features.isEmpty())return "还没有可确认的功能结果";
        if(counts.pending()+counts.unknown()>0)return "还有功能等待确认";
        if(counts.success()>0)return "已检查的功能运行正常";
        return "当前记录中的功能未启用或不适用";
    }
    public String nextStep(){
        var counts=counts();
        if(counts.abnormal()>0)return "先查看需要处理的功能，按提示操作后重新诊断";
        if(preparationIncomplete())return "先查看诊断准备中的提示，补齐信息后重新诊断";
        if(counts.pending()+counts.unknown()>0||features.isEmpty())return "返回桌面操作要检查的功能，再点重新诊断";
        if(counts.success()>0)return "如仍有异常，请复现问题后重新诊断并导出报告";
        return "如需检查某项功能，先在对应设置页开启并使用一次";
    }
    public String overview(){
        return headline()+"\n"+nextStep()+"\n只统计本次有记录的功能，同一功能只计一次，上次运行记录不计入结果";
    }
}
