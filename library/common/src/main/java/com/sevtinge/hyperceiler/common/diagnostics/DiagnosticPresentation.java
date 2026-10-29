/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.diagnostics;

/** Comma-only page copy; log dialogs, stored evidence and exported identifiers keep their punctuation. */
public final class DiagnosticPresentation {
    private DiagnosticPresentation() {}
    public static String prose(String text){
        if(text==null)return "";
        StringBuilder out=new StringBuilder();
        for(int at=0;at<text.length();){
            int point=text.codePointAt(at);at+=Character.charCount(point);
            int kind=Character.getType(point);
            if((kind>=Character.DASH_PUNCTUATION&&kind<=Character.OTHER_PUNCTUATION)
                ||kind==Character.INITIAL_QUOTE_PUNCTUATION||kind==Character.FINAL_QUOTE_PUNCTUATION
                ||(kind>=Character.MATH_SYMBOL&&kind<=Character.OTHER_SYMBOL)){
                int last=out.length()-1;while(last>=0&&out.charAt(last)==' ')last--;
                if(last>=0&&out.charAt(last)!='，'&&out.charAt(last)!='\n')out.append('，');
            }else out.appendCodePoint(point);
        }
        return out.toString().replaceAll("，[ \\t]*\\n","\n").replaceAll("[，\\s]+$","");
    }
    public static String label(DiagnosticRules.State state){return switch(state){
        case ACTIVE->"正常";case FAILED->"需要处理";case DEGRADED->"部分异常";
        case INSTALLED,WAITING->"等待检查";case INITIALIZING->"准备中";
        case UNKNOWN->"信息不足";case STALE->"上次运行记录";
        case DISABLED->"已关闭";case NOT_APPLICABLE->"未参与检查";
    };}
    public static String explanation(DiagnosticRules.Finding f){
        if(f.state()==DiagnosticRules.State.STALE)return "这是上次运行或旧版本留下的记录，不代表现在的状态";
        if(f.state()==DiagnosticRules.State.UNKNOWN)return switch(f.rule()){
            case "PROCESS_UNVERIFIED"->"暂时无法确认这条记录是否来自当前桌面";
            case "CLOCK_UNVERIFIED"->"运行记录的时间与手机时间不一致，暂时无法判断";
            case "INJECTION_PROCESS_UNVERIFIED"->"记录中出现模块加载问题，但无法确认是否影响当前桌面";
            default->"现有信息不足以判断这项功能，暂不计为正常或异常";
        };
        if(f.state()==DiagnosticRules.State.DISABLED)return "运行记录显示这项功能当时处于关闭状态";
        if(f.state()==DiagnosticRules.State.NOT_APPLICABLE)return "这条功能路径没有启用，记录未说明是开关关闭还是当前桌面不适用";
        if(f.state()==DiagnosticRules.State.ACTIVE)return "已记录到这项功能运行，当前未发现相关异常";
        return switch(f.code()){
            case "ROOT_UNAVAILABLE"->"没有获得读取诊断日志所需的 Root 权限，暂时无法完成检查";
            case "LOGS_INCOMPLETE"->"部分诊断日志没有读取成功，检查结果可能不全";
            case "HOME_RECORDS_MISSING"->"没有采集到桌面的运行记录，暂时无法检查具体功能";
            case "COLLECTION_READY"->"诊断日志读取完成";
            case "JAVA_ENTRY_UNVERIFIED"->"还没有找到完整的模块加载记录，暂时无法确认加载情况";
            case "JAVA_ENTRY","NATIVE_ENTRY"->"已找到当前桌面的模块加载记录";
            case "CONFIG_READY"->"已经读取到桌面功能设置";
            case "MODULE_LOAD_FAILED"->"框架没有成功加载模块，这次记录中的功能可能无法使用";
            case "CLASS_NOT_FOUND","METHOD_NOT_FOUND","FIELD_NOT_FOUND","RESOLVE_FAILED","AMBIGUOUS_TARGET","VALIDATION_FAILED","ABI_UNSUPPORTED","DEXKIT_FAILED"->"当前桌面的相关结构没有通过适配检查，这条功能路径没有启用";
            case "CLASSLOADER_OR_TYPE_MISMATCH"->"桌面返回的数据类型与功能预期不一致";
            case "HOOK_LOST"->"这项功能之前已加载，后来检测到加载状态失效";
            case "FOREIGN_MODIFICATION"->"相关位置出现其他修改，这条功能路径已停止覆盖以避免冲突";
            case "INSTALL_FAILED","CONTINUATION_INVALID","MEMORY_PROTECTION_FAILED"->"这条功能路径没有成功加载";
            case "CONFIG_READ_FAILED"->"没有成功读取功能设置，暂时无法按设置运行";
            case "DEPENDENCY_FAILED","NATIVE_DEPENDENCY_FAILED"->"功能需要的组件没有就绪";
            case "CALLBACK_EXCEPTION_OBSERVED"->"运行过程中出现异常，现有记录无法确认是否由本功能引起";
            case "RUNTIME_EXCEPTION"->"这条功能路径运行时出现异常";
            case "HOT_RELOAD_FAILED"->"更新后的模块没有成功加载到桌面";
            default->switch(f.state()){
                case INSTALLED->"还没有记录到对应操作，需要使用一次这项功能再检查";
                case WAITING,INITIALIZING->"功能仍在等待操作或所需组件，暂时不能判断结果";
                default->"检测到相关异常，需要结合诊断日志进一步确认";
            };
        };
    }
    public static String advice(DiagnosticRules.Finding f){
        if(f.state()==DiagnosticRules.State.STALE)return "先查看当前结果，这些旧记录只供排查时参考";
        if(f.state()==DiagnosticRules.State.UNKNOWN)return "返回桌面操作这项功能，再点重新诊断，仍无法判断时导出报告反馈";
        if(f.state()==DiagnosticRules.State.ACTIVE)return "无需处理，如实际效果仍有问题，请复现后重新诊断并导出报告";
        if(f.state()==DiagnosticRules.State.DISABLED||f.state()==DiagnosticRules.State.NOT_APPLICABLE)
            return "若需要使用，请检查对应设置是否开启，已开启仍无效果时导出报告反馈";
        return switch(f.code()){
            case "ROOT_UNAVAILABLE"->"在 Root 管理器中允许 HyperCeiler 读取日志，再点重新诊断";
            case "LOGS_INCOMPLETE"->"先点重新诊断，仍提示不完整时导出报告反馈，这不表示桌面功能已经失效";
            case "HOME_RECORDS_MISSING"->"返回桌面操作要检查的功能，再点重新诊断，仍无记录时检查系统桌面作用域";
            case "JAVA_ENTRY_UNVERIFIED","MODULE_LOAD_FAILED"->"确认框架已启用 HyperCeiler 并勾选系统桌面作用域，重新加载桌面后再诊断";
            case "CLASS_NOT_FOUND","METHOD_NOT_FOUND","FIELD_NOT_FOUND","RESOLVE_FAILED","AMBIGUOUS_TARGET","VALIDATION_FAILED","ABI_UNSUPPORTED","DEXKIT_FAILED","CLASSLOADER_OR_TYPE_MISMATCH"->"导出诊断报告反馈给开发者，并说明桌面版本及具体操作";
            case "HOOK_LOST","FOREIGN_MODIFICATION"->"检查是否有其他模块同时修改这项功能，保留报告后逐个排查";
            case "HOT_RELOAD_FAILED"->"重新加载系统桌面后再诊断，仍异常时导出报告反馈";
            case "COLLECTION_READY","JAVA_ENTRY","NATIVE_ENTRY","CONFIG_READY"->"无需处理，继续查看下方具体功能的结果";
            case "CONFIG_READ_FAILED","DEPENDENCY_FAILED","NATIVE_DEPENDENCY_FAILED"->"确认模块与框架正常运行后重新诊断，仍异常时导出报告反馈";
            default->switch(f.state()){
                case INSTALLED,WAITING,INITIALIZING->operation(f.feature())+"，然后回到这里点重新诊断";
                default->"复现问题后重新诊断，并导出报告反馈给开发者";
            };
        };
    }
    public static String operation(String feature){
        if(feature.contains("无限滚动"))return "滑到桌面最后一页，再继续向左滑动";
        if(feature.contains("最近任务"))return "进入最近任务并操作要检查的卡片或按钮";
        if(feature.contains("文件夹"))return "打开并关闭对应大小的文件夹";
        if(feature.contains("小组件"))return "切换到放有小组件的桌面页面";
        if(feature.contains("抽屉"))return "打开应用抽屉并查看对应区域";
        if(feature.contains("手势"))return "在桌面或应用中使用一次对应手势";
        if(feature.contains("图标")||feature.contains("标题"))return "返回桌面查看图标，再打开一个应用并返回";
        if(feature.contains("Dock")||feature.contains("底栏"))return "返回桌面查看底栏，并切换一次桌面页面";
        return "返回桌面使用一次这项功能";
    }

}
