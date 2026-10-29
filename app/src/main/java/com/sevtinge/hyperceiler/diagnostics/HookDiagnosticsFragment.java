/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.diagnostics;

import android.widget.Toast;
import android.os.Bundle;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.base.BasePreferenceFragment;
import com.sevtinge.hyperceiler.common.diagnostics.*;
import com.sevtinge.hyperceiler.log.db.LogRepository;
import java.util.*;
import fan.appcompat.app.AlertDialog;

/** MIUIX preference list/card groups, using the same fragment base as project settings. */
public class HookDiagnosticsFragment extends BasePreferenceFragment {
    private DiagnosticsOverviewPreference summary;
    private Preference environment,refresh,export,history;
    private DiagnosticsRepository repository;
    private DiagnosticRules.Report report;
    private String pendingExport;
    private DiagnosticSummary assessment;
    private final ActivityResultLauncher<String> createReport=registerForActivityResult(new ActivityResultContracts.CreateDocument("text/plain"),uri->{
        var context=requireContext().getApplicationContext();String token=pendingExport;pendingExport=null;
        LogRepository.getInstance().getIoExecutor().execute(()->{
            boolean success=false;
            if(uri!=null)try(var out=context.getContentResolver().openOutputStream(uri)){
                if(out!=null){DiagnosticExport.copy(context.getCacheDir(),token,out);success=true;}
            }catch(Exception unavailable){}
            DiagnosticExport.discard(context.getCacheDir(),token);
            boolean done=success;runOnUiThreadIfAlive(()->{export.setEnabled(report!=null);if(uri!=null)Toast.makeText(requireContext(),done?R.string.hook_diagnostics_export_done:R.string.hook_diagnostics_export_failed,Toast.LENGTH_SHORT).show();});
        });
    });
    @Override public void onCreatePreferencesBefore(Bundle saved,String key){
        if(saved!=null)pendingExport=saved.getString("diagnostics_export_token");
        super.onCreatePreferencesBefore(saved,key);
    }
    @Override public void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("diagnostics_export_token",pendingExport);}
    @Override public int getPreferenceScreenResId(){return R.xml.prefs_hook_diagnostics;}
    @Override public void initPrefs(){
        setTitle(R.string.hook_diagnostics_title);
        summary=findPreference("diagnostics_summary");environment=findPreference("diagnostics_environment");
        refresh=findPreference("diagnostics_refresh");export=findPreference("diagnostics_export");history=findPreference("diagnostics_history");history.setVisible(false);
        summary.setOnMetricClickListener(index->{
            String key=switch(index){case 0->"executed";case 1->"faults";case 2->"pending";default->"unknown";};
            PreferenceCategory category=findPreference("diagnostics_"+key);
            if(category!=null&&category.isVisible()&&category.getPreferenceCount()>0)scrollToPreference(category.getPreference(0));
        });
        repository=new DiagnosticsRepository(requireContext());
        refresh.setOnPreferenceClickListener(p->{diagnose(true);return true;});
        export.setOnPreferenceClickListener(p->{prepareExport();return true;});
        environment.setOnPreferenceClickListener(p->{preparation();return true;});
        history.setOnPreferenceClickListener(p->{history();return true;});
        diagnose(false);
    }
    private void prepareExport(){
        if(report==null||pendingExport!=null)return;
        var snapshot=report;var context=requireContext().getApplicationContext();export.setEnabled(false);
        LogRepository.getInstance().getIoExecutor().execute(()->{
            try{
                String token=DiagnosticExport.stage(context.getCacheDir(),DiagnosticRules.reportText(snapshot));
                if(!isFragmentContextAvailable()){DiagnosticExport.discard(context.getCacheDir(),token);return;}
                runOnUiThreadIfAlive(()->{pendingExport=token;createReport.launch("hyperceiler-home-hook-diagnostics.txt");});
            }catch(Exception unavailable){runOnUiThreadIfAlive(()->{export.setEnabled(report!=null);Toast.makeText(requireContext(),R.string.hook_diagnostics_export_failed,Toast.LENGTH_SHORT).show();});}
        });
    }
    private void diagnose(boolean collect){
        refresh.setEnabled(false);export.setEnabled(false);summary.beginRefresh();summary.setSummary(R.string.hook_diagnostics_loading);
        repository.refresh(collect,result->runOnUiThreadIfAlive(()->{report=result;render();refresh.setEnabled(true);export.setEnabled(pendingExport==null);}),
            error->runOnUiThreadIfAlive(()->{summary.refreshFailed();summary.setSummary(DiagnosticPresentation.prose(error));refresh.setEnabled(true);export.setEnabled(report!=null&&pendingExport==null);}));
    }
    private void render(){
        assessment=DiagnosticSummary.from(report);
        var counts=assessment.counts();
        summary.setCounts(counts.success(),counts.abnormal(),counts.pending(),counts.unknown());
        summary.setSummary(DiagnosticPresentation.prose(assessment.overview()));
        environment.setSummary(DiagnosticPresentation.prose(assessment.preparationSummary()));
        history.setVisible(!assessment.history().isEmpty());
        history.setSummary("保留了 "+assessment.history().size()+" 条旧记录，仅供排查参考，不计入上方功能数量");
        Map<String,PreferenceCategory> categories=new HashMap<>();
        for(String key:List.of("faults","pending","unknown","executed","inactive")){
            PreferenceCategory category=findPreference("diagnostics_"+key);category.removeAll();categories.put(key,category);
        }
        if(assessment.features().isEmpty()){
            Preference empty=new Preference(getPrefContext());empty.setPersistent(false);empty.setSelectable(false);
            empty.setSummary(R.string.hook_diagnostics_empty);categories.get("pending").addPreference(empty);
        }
        for(var feature:assessment.features()){
            Preference row=new Preference(getPrefContext());row.setPersistent(false);row.setKey("diagnosis_"+feature.key());
            row.setTitle(DiagnosticPresentation.prose(feature.name()+"，"+DiagnosticPresentation.label(feature.state())));
            row.setSummary(DiagnosticPresentation.prose(feature.explanation()+"\n"+feature.advice()));
            row.setOnPreferenceClickListener(p->{detail(feature);return true;});
            String category=switch(feature.state()){
                case FAILED,DEGRADED->"faults";case ACTIVE->"executed";case DISABLED,NOT_APPLICABLE->"inactive";
                case UNKNOWN,STALE->"unknown";default->"pending";
            };
            categories.get(category).addPreference(row);
        }
        for(var category:categories.values())category.setVisible(category.getPreferenceCount()>0);
    }
    private void detail(DiagnosticSummary.Feature feature){
        String text="当前情况\n"+feature.explanation()+"\n\n接下来怎么做\n"+feature.advice();
        if(report!=null&&!report.collectionComplete())text+="\n\n本次诊断信息不完整，这里的结果只代表已经读取到的记录";
        new AlertDialog.Builder(requireContext()).setTitle(DiagnosticPresentation.prose(feature.name()))
            .setMessage(DiagnosticPresentation.prose(text)).setPositiveButton(android.R.string.ok,null)
            .setNeutralButton(R.string.hook_diagnostics_evidence,(d,w)->logs(feature.findings())).show();
    }
    private void preparation(){
        if(assessment==null)return;
        StringBuilder text=new StringBuilder("检查范围\n只检查系统桌面中已经产生运行记录的功能，列表没有出现的功能尚未检查\n");
        if(assessment.preparation().isEmpty())text.append("\n尚未取得诊断准备记录，返回桌面操作后重新诊断\n");
        for(var check:assessment.preparation())text.append("\n").append(preparationName(check.name())).append("\n")
            .append(check.explanation()).append("\n").append(check.advice()).append("\n");
        if(!report.collectionComplete())text.append("\n部分运行信息尚未读取成功，请先重新诊断，仍不完整时检查框架连接和系统桌面作用域，再导出报告反馈");
        new AlertDialog.Builder(requireContext()).setTitle(R.string.hook_diagnostics_environment)
            .setMessage(DiagnosticPresentation.prose(text.toString())).setPositiveButton(android.R.string.ok,null)
            .setNeutralButton(R.string.hook_diagnostics_technical,(d,w)->new AlertDialog.Builder(requireContext())
                .setTitle(R.string.hook_diagnostics_technical).setMessage(report.environment()+"\n\n无法解析的记录："+report.malformed()+"\n来源未通过检查的记录："+report.rejected())
                .setPositiveButton(android.R.string.ok,null).show()).show();
    }
    private static String preparationName(String name){return switch(name){
        case "Java 模块入口","Java 入口待核实"->"模块加载检查";
        case "Native 模块入口","Native 入口"->"桌面运行环境检查";
        default->name;
    };}
    private void history(){
        if(assessment==null)return;
        var saved=assessment.history();
        String[] names=saved.stream().map(f->DiagnosticPresentation.prose(f.feature()+"，上次运行记录")).toArray(String[]::new);
        new AlertDialog.Builder(requireContext()).setTitle(R.string.hook_diagnostics_history)
            .setItems(names,(d,which)->detail(new DiagnosticSummary.Feature(saved.get(which).id(),saved.get(which).feature(),saved.get(which).state(),List.of(saved.get(which)))))
            .setPositiveButton(android.R.string.ok,null).show();
    }
    private void logs(List<DiagnosticRules.Finding> findings){
        String[] names=new String[findings.size()+1];names[0]=getString(R.string.hook_diagnostics_technical);
        for(int i=0;i<findings.size();++i)names[i+1]="关联日志，记录 "+(i+1);
        new AlertDialog.Builder(requireContext()).setTitle(R.string.hook_diagnostics_evidence).setItems(names,(d,which)->{
            if(which>0){evidence(findings.get(which-1));return;}
            StringBuilder text=new StringBuilder();
            for(var f:findings)text.append("功能：").append(f.feature()).append("\n配置记录：").append(f.configuration())
                .append("\n记录状态：").append(DiagnosticRules.stateLabel(f.state())).append("\n分类：").append(f.code())
                .append("\n规则：").append(f.rule()).append("\n阶段：").append(f.failureStage())
                .append("\n记录来源：").append(f.evidence()).append("\n目标：").append(f.packageName()).append('/').append(f.processName())
                .append("\nPID：").append(f.pid()).append("\n代际：").append(f.generation()).append("\n运行模块：").append(f.moduleVersion())
                .append("\n关联记录数：").append(f.occurrences()).append("\n原始说明：").append(f.explanation()).append("\n\n");
            new AlertDialog.Builder(requireContext()).setTitle(R.string.hook_diagnostics_technical).setMessage(technicalText(text.toString())).setPositiveButton(android.R.string.ok,null).show();
        }).show();
    }
    private static String technicalText(String text){
        return text.length()>24000?text.substring(0,24000)+"\n\n更多诊断记录请导出报告查看":text;
    }
    private void evidence(DiagnosticRules.Finding f){
        var events=f.events();String[] names=events.stream().map(e->new Date(e.time())+" "+e.code()+"，记录编号 "+e.rawId()).toArray(String[]::new);
        new AlertDialog.Builder(requireContext()).setTitle(R.string.hook_diagnostics_evidence).setItems(names,(d,which)->{
            long id=events.get(which).rawId();
            LogRepository.getInstance().getIoExecutor().execute(()->{
                var row=LogRepository.getInstance().getDiagnosticDao().raw(id);
                String text=row==null?"原始日志已轮转或被清除，结构化证据仍保留在本次快照":row.getFormattedTime()+" "+row.getTag()+"\n"+row.getMessage();
                runOnUiThreadIfAlive(()->new AlertDialog.Builder(requireContext()).setTitle(R.string.hook_diagnostics_evidence).setMessage(text).setPositiveButton(android.R.string.ok,null).show());
            });
        }).show();
    }
}
