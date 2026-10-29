/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.diagnostics;

/** Original HCD1 home.layout.slot registry, metadata only, no function addresses.
 * Frozen for old records. If native slot order changes, version the producer ID
 * instead of reinterpreting old evidence. presentation.py checks every slot.
 * Old recent slots did not record their setting, so their exact item stays unknown.
 */
final class LegacyLayoutNamesV1 {
    private LegacyLayoutNamesV1() {}
    private static final String[] NAMES=new String[236];
    static {
        java.util.Arrays.fill(NAMES,0,1,"桌面列数");
        java.util.Arrays.fill(NAMES,1,2,"桌面行数");
        java.util.Arrays.fill(NAMES,2,3,"桌面行列配置读取");
        java.util.Arrays.fill(NAMES,3,4,"Dock 边距配置读取");
        java.util.Arrays.fill(NAMES,4,5,"Dock 底部边距");
        java.util.Arrays.fill(NAMES,5,6,"文件夹图标行间距");
        java.util.Arrays.fill(NAMES,6,7,"桌面顶部边距");
        java.util.Arrays.fill(NAMES,7,8,"桌面底部边距");
        java.util.Arrays.fill(NAMES,8,9,"桌面左右边距");
        java.util.Arrays.fill(NAMES,9,10,"桌面分页指示器底部边距");
        java.util.Arrays.fill(NAMES,10,11,"桌面搜索框底部边距");
        java.util.Arrays.fill(NAMES,11,12,"桌面搜索框宽度");
        java.util.Arrays.fill(NAMES,12,13,"桌面动画时长比例");
        java.util.Arrays.fill(NAMES,13,14,"桌面动画速度");
        java.util.Arrays.fill(NAMES,14,15,"编辑模式分页指示器位置");
        java.util.Arrays.fill(NAMES,15,20,"文件夹归位坐标与桌面边距");
        java.util.Arrays.fill(NAMES,20,22,"图标拖放落点与桌面边距");
        java.util.Arrays.fill(NAMES,22,23,"桌面图标标题颜色");
        java.util.Arrays.fill(NAMES,23,24,"抽屉图标标题大小");
        java.util.Arrays.fill(NAMES,24,25,"桌面图标标题大小");
        java.util.Arrays.fill(NAMES,25,26,"隐藏新安装应用标题标记");
        java.util.Arrays.fill(NAMES,26,27,"隐藏文件夹新安装应用标记");
        java.util.Arrays.fill(NAMES,27,28,"隐藏新安装应用高亮");
        java.util.Arrays.fill(NAMES,28,29,"桌面图标标题大小");
        java.util.Arrays.fill(NAMES,29,30,"抽屉图标标题大小");
        java.util.Arrays.fill(NAMES,30,31,"自定义应用标题");
        java.util.Arrays.fill(NAMES,31,34,"小组件尺寸适配");
        java.util.Arrays.fill(NAMES,34,35,"展开文件夹图标间距");
        java.util.Arrays.fill(NAMES,35,36,"展开文件夹内容宽度");
        java.util.Arrays.fill(NAMES,36,37,"展开文件夹左右边距");
        java.util.Arrays.fill(NAMES,37,38,"展开文件夹内部边距");
        java.util.Arrays.fill(NAMES,38,39,"展开文件夹标题对齐");
        java.util.Arrays.fill(NAMES,39,40,"编辑文件夹标题对齐");
        java.util.Arrays.fill(NAMES,40,41,"展开文件夹全屏宽度");
        java.util.Arrays.fill(NAMES,41,42,"文件夹标题居中");
        java.util.Arrays.fill(NAMES,42,44,"桌面行列自适应");
        java.util.Arrays.fill(NAMES,44,51,"文件夹预览与关闭归位动画");
        java.util.Arrays.fill(NAMES,51,52,"文件夹归位目标观测");
        java.util.Arrays.fill(NAMES,52,59,"大文件夹尺寸缩放");
        java.util.Arrays.fill(NAMES,59,61,"返回手势区域高度与宽度");
        java.util.Arrays.fill(NAMES,61,62,"编辑桌面时的 Dock 显示");
        java.util.Arrays.fill(NAMES,62,63,"文件夹标题颜色");
        java.util.Arrays.fill(NAMES,63,64,"文件夹标题大小");
        java.util.Arrays.fill(NAMES,64,65,"上滑手势触摸处理");
        java.util.Arrays.fill(NAMES,65,66,"返回手势触摸处理");
        java.util.Arrays.fill(NAMES,66,68,"桌面双击手势");
        java.util.Arrays.fill(NAMES,68,69,"桌面手势场景切换");
        java.util.Arrays.fill(NAMES,69,72,"桌面手势状态同步");
        java.util.Arrays.fill(NAMES,72,78,"普通图标缩放");
        java.util.Arrays.fill(NAMES,78,80,"桌面列数");
        java.util.Arrays.fill(NAMES,80,82,"桌面行列设置保存");
        java.util.Arrays.fill(NAMES,82,86,"安装进度图标缩放");
        java.util.Arrays.fill(NAMES,86,87,"大小文件夹背景与预览缩放");
        java.util.Arrays.fill(NAMES,87,92,"文件夹关闭缩放动画");
        java.util.Arrays.fill(NAMES,92,156,"小组件缩放");
        java.util.Arrays.fill(NAMES,156,158,"Dock 显示应用标题");
        java.util.Arrays.fill(NAMES,158,160,"应用返回文件夹缩放动画");
        java.util.Arrays.fill(NAMES,160,224,"最近任务卡片参数项目未知");
        java.util.Arrays.fill(NAMES,224,225,"禁止应用预加载");
        java.util.Arrays.fill(NAMES,225,227,"隐藏抽屉搜索框");
        java.util.Arrays.fill(NAMES,227,228,"隐藏桌面信息上报提示");
        java.util.Arrays.fill(NAMES,228,231,"大文件夹预览归位动画");
        java.util.Arrays.fill(NAMES,231,233,"底栏图标水平边距");
        java.util.Arrays.fill(NAMES,233,235,"桌面无限滚动");
        NAMES[235]="自定义抽屉应用标题";
    }
    static String get(int slot){return slot>=0&&slot<NAMES.length?NAMES[slot]:null;}
}
