/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.diagnostics;
import java.util.regex.*;
/** Strict adapters for known project producers. Unrecognised text is never classified by keyword count. */
public final class LegacyDiagnosticAdapter {
    private static final Pattern BIND=Pattern.compile("layout feature=([a-z0-9_]+) runtime-contract=unavailable stage=([0-9]+) reason=(.{1,1400})(?:; stock retained)?");
    private static final Pattern SCALE=Pattern.compile("feature=([a-z0-9_]+) (?:hooks=(active|inactive)|applied (.{1,1000}))");
    private LegacyDiagnosticAdapter(){}
    public static DiagnosticEvent parse(String tag,String text,String source,long rawId,long time,int pid,DiagnosticEvent anchor){
        String id="",feature="",phase="",stage="",code="",type="NATIVE_INLINE";
        text=DiagnosticEvent.bound(text,4000);
        if(tag.equals("LSPosed.ModuleLoad")&&text.contains("MODULE_APK_LOAD_FAILED:")&&text.contains("/com.sevtinge.hyperceiler-")&&text.contains("/base.apk")){
            id="module.apk";feature="模块 APK 加载";phase="FAILED";stage="MODULE_LOAD";code="MODULE_LOAD_FAILED";type="INJECTION";
        } else if(tag.equals("LogSync.Root")&&text.startsWith("ROOT_READY:")){
            id="collection.root";feature="诊断数据采集";phase="INITIALIZED";stage="COLLECTION";code="COLLECTION_READY";pid=0;type="COLLECTION";
        } else if(tag.equals("LogSync.Root")&&text.startsWith("ROOT_UNAVAILABLE:")){
            id="collection.root";feature="诊断数据采集";phase="DEGRADED";stage="COLLECTION";code="ROOT_UNAVAILABLE";pid=0;type="COLLECTION";
        } else if(tag.equals("LogSync.HomeCoverage")&&text.startsWith("NO_HOME_RECORDS:")){
            id="collection."+tag;feature="诊断数据采集";phase="WAITING";stage="COLLECTION";code="HOME_RECORDS_MISSING";pid=0;type="COLLECTION";
        } else if((tag.equals("LogSync.HomeCoverage")&&text.startsWith("HOME_RECORDS_PRESENT:"))
            ||((tag.equals("LogSync.Logcat")||tag.equals("LogSync.HookDiagnostics"))&&text.startsWith("LOGCAT_IMPORT_OK:"))){
            id="collection."+tag;feature="诊断数据采集";phase="INITIALIZED";stage="COLLECTION";code="COLLECTION_READY";pid=0;type="COLLECTION";
        } else if(tag.startsWith("LogSync.")&&(text.startsWith("LOGCAT_IMPORT_PARTIAL:")||text.startsWith("LOGCAT_IMPORT_FAILED:")||text.startsWith("ROOT_CHECK_FAILED:")||text.startsWith("LSPOSED_SYNC_START_FAILED:"))){
            id="collection."+tag;feature="诊断数据采集";phase="DEGRADED";stage="COLLECTION";code="LOGS_INCOMPLETE";pid=0;type="COLLECTION";
        } else if(tag.equals("HyperCeiler.HomeLayout")){
            Matcher bind=BIND.matcher(text),scale=SCALE.matcher(text);
            if(bind.matches()){id="home."+bind.group(1);feature=label(bind.group(1));phase="DEGRADED";stage="RESOLVE_VALIDATE/"+bind.group(2);code="VALIDATION_FAILED";}
            else if(scale.matches()){
                id="home."+scale.group(1);feature=label(scale.group(1));stage="RUNTIME";
                if(scale.group(3)!=null){phase="APPLIED";code="INTERNAL_APPLIED";}
                else if("active".equals(scale.group(2))){phase="INSTALLED";code="INSTALL_OK";}
                else {phase="WAITING";code="HOOK_NOT_READY";}
            }else return null;
        }else return null;
        String boot=anchor==null?"":anchor.bootId(),start=anchor==null?"":anchor.processStart();
        String process=anchor==null?(pid==0?"":"com.miui.home"):anchor.processName();
        String pkg=anchor==null?(pid==0?"":"com.miui.home"):anchor.packageName();
        if(type.equals("INJECTION")&&anchor==null){pkg="";process="";pid=0;}
        return new DiagnosticEvent(id,feature,type,pkg,process,pid,boot,start,anchor==null?"legacy-unverified":anchor.generation(),anchor==null?"unknown":anchor.moduleVersion(),anchor==null?"unknown":anchor.systemVersion(),time,phase,stage,code,text,source,rawId);
    }
    public static String label(String id){return switch(id){
        case "icon_scale","icon_render"->"普通图标缩放";
        case "small_folder","small_folder_scale"->"小文件夹缩放";
        case "big_folder","big_folder_scale","big_folder_size"->"大文件夹缩放";
        case "widget_scale","widget"->"小组件缩放";
        case "folder_preview_return"->"文件夹预览归位动画";
        case "folder_close_animation"->"文件夹关闭缩放动画";
        case "folder_scale"->"大小文件夹背景与预览缩放";
        case "folder_app_return_scale"->"应用返回文件夹缩放动画";
        case "columns"->"桌面行列";
        case "back_region"->"返回手势区域高度与宽度";
        case "dock_margin"->"Dock 底部边距";
        case "folder_row_spacing"->"文件夹图标行间距";
        case "animation_ratio"->"桌面动画时长比例";
        case "animation_speed"->"桌面动画速度";
        case "new_install_appearance"->"隐藏新安装应用标记";
        case "drop_geometry"->"图标拖放落点与桌面边距";
        case "folder_geometry"->"文件夹归位坐标与桌面边距";
        case "grid_autofit"->"桌面行列自适应";
        case "indicator_position"->"桌面分页指示器底部边距";
        case "recent_hide_clear"->"隐藏最近任务清理按钮";
        case "recent_disable_clear"->"禁用最近任务清理按钮";
        case "drawer_search"->"隐藏抽屉搜索框";
        case "prestart"->"禁止应用预加载";
        case "dock_title"->"Dock 显示应用标题";
        default->"未命名功能，规则 "+id;
    };}
}
