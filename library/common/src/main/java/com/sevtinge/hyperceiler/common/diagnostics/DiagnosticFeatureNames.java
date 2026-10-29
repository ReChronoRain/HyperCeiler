/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.diagnostics;

/** Feature names come from semantic IDs or a verified legacy registry, never from failure keywords. */
public final class DiagnosticFeatureNames {
    private DiagnosticFeatureNames() {}
    public static String forEvent(DiagnosticEvent event){
        String name=event.feature(),id=event.hookId();
        if(id.startsWith("java.unattributed/"))return "用途未知的 Java 挂钩";
        if(id.startsWith("home.layout.slot.")&&(name.isBlank()||name.equals("桌面布局")||name.equals("最近任务"))){
            try {String known=LegacyLayoutNamesV1.get(Integer.parseInt(id.substring("home.layout.slot.".length())));if(known!=null)return known;}
            catch(NumberFormatException unavailable){}
            return "用途未知的历史原生挂钩";
        }
        if(id.startsWith("com.sevtinge.hyperceiler.libhook.rules.home.")){
            String owner=id.split("/",2)[0];String simple=owner.substring(owner.lastIndexOf('.')+1).split("\\$",2)[0];
            String translated=switch(simple){
                case "HomeSettingsOS4"->"桌面模式与标题及内存信息设置";
                case "HomeGestureOS4"->"桌面手势动作配置";
                case "HomePortraitReverse"->"桌面反向竖屏";
                case "HomeQuickSwitchOS4"->"手势切换上一个应用";
                case "HomeGestureRegionsOS4"->"返回与上滑手势区域";
                case "HomeExtraGesturesOS4"->"桌面双击与长按手势";
                case "HomeDockWindow"->"Dock 玻璃效果与底栏配置服务";
                case "ShowDockIconTitle"->"Dock 显示应用标题";
                case "DisablePrestart"->"禁止应用预加载";
                case "TaskViewHeaderOffset"->"最近任务卡片标题偏移";
                case "TaskViewHeaderTitlePadding"->"最近任务卡片标题边距";
                case "TaskViewHeight"->"最近任务卡片高度";
                case "CardTextSize"->"最近任务卡片文字大小";
                case "CardTextColor"->"最近任务卡片文字颜色";
                case "FreeformCardBackgroundColor"->"最近任务小窗按钮背景颜色";
                case "TaskViewHorizontal"->"最近任务横向卡片缩放";
                case "TaskViewVertical"->"最近任务纵向卡片缩放";
                case "AlwaysShowCleanUp"->"始终显示最近任务清理按钮";
                case "RemoveCardAnim"->"最近任务移除卡片动画";
                case "HomeMode"->"桌面模式";
                case "AnimDurationRatio"->"桌面动画时长比例";
                case "WidgetCornerRadius"->"小组件圆角";
                case "DoubleTap","DoubleTapController"->"桌面双击手势";
                case "QuickBack"->"返回手势快速触发";
                case "BackGestureHaptic"->"返回手势触感反馈";
                case "ShakeDevice"->"摇一摇手势";
                default->null;
            };
            if(translated!=null)return translated;
            if(name.equals(id)||name.equals(owner)||name.equals(simple)||name.isBlank())return "未命名功能，规则 "+simple;
        }
        if(name.equals("桌面布局")||name.isBlank())return "挂钩用途未知";
        if(name.equals("最近任务"))return "最近任务卡片参数项目未知";
        return name;
    }
}
