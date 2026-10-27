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
    private Preference environment,refresh,export;
    private DiagnosticsRepository repository;
    private DiagnosticRules.Report report;
    private String pendingExport;
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
        refresh=findPreference("diagnostics_refresh");export=findPreference("diagnostics_export");
        summary.setOnMetricClickListener(index->{
            String key=switch(index){case 0->"executed";case 1->"faults";default->"pending";};
            PreferenceCategory category=findPreference("diagnostics_"+key);
            if(category!=null&&category.isVisible()&&category.getPreferenceCount()>0)scrollToPreference(category.getPreference(0));
        });
        repository=new DiagnosticsRepository(requireContext());
        refresh.setOnPreferenceClickListener(p->{diagnose(true);return true;});
        export.setOnPreferenceClickListener(p->{prepareExport();return true;});
        environment.setOnPreferenceClickListener(p->{if(report!=null)new AlertDialog.Builder(requireContext()).setTitle(R.string.hook_diagnostics_environment).setMessage(DiagnosticPresentation.prose(report.environment())).setPositiveButton(android.R.string.ok,null).show();return true;});
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
        var counts=DiagnosticRules.counts(report);
        summary.setCounts(counts.success(),counts.abnormal(),counts.pending(),counts.unknown());
        summary.setSummary(R.string.hook_diagnostics_summary);
        environment.setSummary(DiagnosticPresentation.prose((report.collectionComplete()?"采集链路可用，未观测到不等于失败":"诊断数据不完整，不能据此判断其他桌面 Hook 成败")+"\n损坏记录 "+report.malformed()+"，来源校验拒绝 "+report.rejected()));
        Map<String,PreferenceCategory> categories=new HashMap<>();
        for(String key:List.of("faults","pending","executed","inactive")){PreferenceCategory category=findPreference("diagnostics_"+key);category.removeAll();categories.put(key,category);}
        if(report.findings().isEmpty()){
            Preference empty=new Preference(getPrefContext());empty.setPersistent(false);empty.setSelectable(false);empty.setSummary(R.string.hook_diagnostics_empty);categories.get("pending").addPreference(empty);
        }
        // Keep current execution evidence visible even when retained old-process
        // findings fill the bounded list. Full evidence remains in the report.
        for(var f:report.findings().stream().sorted(Comparator.comparingInt(f->displayPriority(f.state()))).limit(200).toList()){
            Preference row=new Preference(getPrefContext());row.setPersistent(false);row.setKey("diagnosis_"+f.id());
            row.setTitle(DiagnosticPresentation.prose(f.feature()+"，"+state(f.state())));
            row.setSummary(DiagnosticPresentation.prose(f.explanation()));
            row.setOnPreferenceClickListener(p->{detail(f);return true;});
            String category=switch(f.state()){case FAILED,DEGRADED->"faults";case ACTIVE->"executed";case DISABLED,NOT_APPLICABLE->"inactive";default->"pending";};
            categories.get(category).addPreference(row);
        }
        for(var category:categories.values())category.setVisible(category.getPreferenceCount()>0);
    }
    private static int displayPriority(DiagnosticRules.State state){return switch(state){
        case FAILED,DEGRADED->0;case ACTIVE->1;case INSTALLED,WAITING,INITIALIZING->2;
        case UNKNOWN->3;case DISABLED,NOT_APPLICABLE->4;case STALE->5;
    };}
    private void detail(DiagnosticRules.Finding f){
        StringBuilder text=new StringBuilder("配置："+f.configuration()+"\n\n状态："+state(f.state())+"\n失败阶段："+f.failureStage()+"\n分类："+f.code()+"\n规则："+f.rule()+"\n证据等级："+f.evidence()+"\n目标："+f.packageName()+" / "+f.processName()+"\nPID："+f.pid()+"\n代际："+f.generation()+"\n运行模块标识："+f.moduleVersion()+"\n合并次数："+f.occurrences()+"\n\n原因："+f.explanation()+"\n建议："+f.advice());
        new AlertDialog.Builder(requireContext()).setTitle(f.feature()).setMessage(text.toString()).setPositiveButton(android.R.string.ok,null)
            .setNeutralButton(R.string.hook_diagnostics_evidence,(d,w)->evidence(f)).show();
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
    private static String state(DiagnosticRules.State s){return DiagnosticRules.stateLabel(s);}
}
