/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model;

import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.getModuleRes;
import static com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.dp2px;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;

import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class DualRowSignalHook extends BaseHook {
    private final int rightMargin = mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_right_margin", 8) - 8;
    private final int leftMargin = mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_left_margin", 8) - 8;
    private final int iconScale = mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_size", 10); // 图标缩放
    private final int verticalOffset = mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_vertical_offset", 8);
    private final boolean mobileTypeSingle = mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable"); // 移动网络类型单独显示
    private final String selectedIconStyle = mPrefsMap.getString("system_ui_status_mobile_network_icon_style", ""); // 图标样式
    private final int selectedIconTheme = mPrefsMap.getStringAsInt("system_ui_statusbar_iconmanage_mobile_network_icon_theme", 1); // 图标主题
    // 移动网络和WiFi网络都移动到左侧
    private  final boolean moveSignalLeft = (mPrefsMap.getBoolean("system_ui_status_bar_wifi_at_left") || mPrefsMap.getBoolean("system_ui_status_bar_mobile_network_at_left"));

    @Override
    public void init() {
         if (!mobileTypeSingle) {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_type_half_to_top_distance", 3);
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_left_inout_over_strength", 0);
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_type_middle_to_strength_start", -0.4f);
        }

        HashMap<String, Integer> dualSignalResMap = new HashMap<>();
        String[] colorModeList = {"", "dark", "tint"};
        // String[] iconStyles = {"", "thick", "theme"};

        findAndHookMethod("com.android.systemui.SystemUIApplication", "onCreate", new MethodHook() {
            private boolean isHooked = false;

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (!isHooked) {
                    isHooked = true;
                    Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                    Resources modRes = getModuleRes(mContext);
                    for (int slot = 1; slot <= 2; slot++) {
                        for (int lvl = 0; lvl <= 5; lvl++) {
                            for (String colorMode : colorModeList) {
                                String colorModeEq = !colorMode.isEmpty() ? ("_" + colorMode) : "";
                                if (selectedIconTheme == 1) {
                                    String dualIconResName = "statusbar_signal_classic_" + slot + "_" + lvl + colorModeEq;
                                    int iconResId = modRes.getIdentifier(dualIconResName, "drawable", ProjectApi.mAppModulePkg);
                                    dualSignalResMap.put(dualIconResName, iconResId);
                                } else if (selectedIconTheme == 2) {
                                    if (!selectedIconStyle.equals("theme") || !colorMode.equals("tint")) {
                                        String dualIconResName = "statusbar_signal_oa_" + slot + "_" + lvl + colorModeEq + (!selectedIconStyle.isEmpty() ? ("_" + selectedIconStyle) : "");
                                        int iconResId = modRes.getIdentifier(dualIconResName, "drawable", ProjectApi.mAppModulePkg);
                                        dualSignalResMap.put(dualIconResName, iconResId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        setDualRowIcon(); // 设置双排图标
        getMobileLevel(); // 获取信号强度及隐藏小 hd
        resetImageDrawable(dualSignalResMap); // 刷新图标视图
        setDualRowStyle(); // 调整双排移动网络位置及缩放
    }

    private void setDualRowIcon() {
        SparseIntArray signalResToLevelMap = new SparseIntArray();
        String ControllerImplName = moveSignalLeft ? "MiuiDripLeftStatusBarIconControllerImpl" : "StatusBarIconControllerImpl";
        hookAllMethods("com.android.systemui.statusbar.phone." + ControllerImplName, lpparam.classLoader, "setMobileIcons", new MethodHook() {
            private boolean isHooked = false;

            @Override
            @SuppressLint("DiscouragedApi")
            protected void before(MethodHookParam param) {
                if (!isHooked) {
                    isHooked = true;
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    Resources res = mContext.getResources();
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_0", "drawable", lpparam.packageName), 0);
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_1", "drawable", lpparam.packageName), 1);
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_2", "drawable", lpparam.packageName), 2);
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_3", "drawable", lpparam.packageName), 3);
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_4", "drawable", lpparam.packageName), 4);
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_5", "drawable", lpparam.packageName), 5);
                    signalResToLevelMap.put(res.getIdentifier("stat_sys_signal_null", "drawable", lpparam.packageName), 6);
                }
                List<?> iconStates = (List<?>) param.args[1];
                if (iconStates.size() == 2) {
                    Object mainIconState = iconStates.get(0);
                    Object subIconState = iconStates.get(1);
                    boolean subDataConnected = (boolean) XposedHelpers.getObjectField(subIconState, "dataConnected");
                    XposedHelpers.setObjectField(subIconState, "visible", false);
                    int mainSignalResId = (int) XposedHelpers.getObjectField(mainIconState, "strengthId");
                    int subSignalResId = (int) XposedHelpers.getObjectField(subIconState, "strengthId");
                    int mainLevel = signalResToLevelMap.get(mainSignalResId);
                    int subLevel = signalResToLevelMap.get(subSignalResId);
                    int level;
                    if (subDataConnected) {
                        level = subLevel * 10 + mainLevel;
                        String[] syncFields = {"showName", "activityIn", "activityOut"};
                        for (String field : syncFields) {
                            XposedHelpers.setObjectField(mainIconState, field, XposedHelpers.getObjectField(subIconState, field));
                        }
                        XposedHelpers.setObjectField(mainIconState, "dataConnected", true);
                    } else {
                        level = mainLevel * 10 + subLevel;
                    }
                    XposedHelpers.setObjectField(mainIconState, "strengthId", level);
                    param.args[1] = iconStates;
                }
            }
        });
    }

    private void getMobileLevel() {
        if (isHyperOSVersion(1f)) {
            MethodHook stateUpdateHook = new MethodHook() {
                @Override
                protected void before(final MethodHookParam param) {
                    Object mobileIconState = param.args[0];
                    boolean visible = (boolean) XposedHelpers.getObjectField(mobileIconState, "visible");
                    boolean airplane = (boolean) XposedHelpers.getObjectField(mobileIconState, "airplane");
                    int level = (int) XposedHelpers.getObjectField(mobileIconState, "strengthId");
                    if (!visible || airplane || level == 0 || level > 100) {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "subStrengthId", -1);
                    } else {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "subStrengthId", level % 10);
                    }
                }

                @Override
                protected void after(final MethodHookParam param) {
                    int subStrengthId = (int) XposedHelpers.getAdditionalInstanceField(param.thisObject, "subStrengthId");
                    if (subStrengthId < 0) return;
                    Object mSmallHd = XposedHelpers.getObjectField(param.thisObject, "mSmallHd");
                    XposedHelpers.callMethod(mSmallHd, "setVisibility", 8);
                    Object mSmallRoaming = XposedHelpers.getObjectField(param.thisObject, "mSmallRoaming");
                    XposedHelpers.callMethod(mSmallRoaming, "setVisibility", 0);
                }
            };

            hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "initViewState", stateUpdateHook);
            hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "updateState", stateUpdateHook);
        }

        MethodHook beforeUpdate = new MethodHook() {
            @Override
            protected void before(final MethodHookParam param) {
                Object mobileIconState = param.args[0];
                boolean visible = (boolean) XposedHelpers.getObjectField(mobileIconState, "visible");
                boolean airplane = (boolean) XposedHelpers.getObjectField(mobileIconState, "airplane");
                int level = (int) XposedHelpers.getObjectField(mobileIconState, "strengthId");
                if (!visible || airplane || level == 0 || level > 100) {
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "subStrengthId", -1);
                } else {
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "subStrengthId", level % 10);
                    XposedHelpers.setObjectField(mobileIconState, "fiveGDrawableId", 0);
                }
            }
        };
        MethodHook afterUpdate = new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) {
                int subStrengthId = (int) XposedHelpers.getAdditionalInstanceField(param.thisObject, "subStrengthId");
                if (subStrengthId < 0) return;
                Object mSmallHd = XposedHelpers.getObjectField(param.thisObject, "mSmallHd");
                XposedHelpers.callMethod(mSmallHd, "setVisibility", 8);
                Object mSmallRoaming = XposedHelpers.getObjectField(param.thisObject, "mSmallRoaming");
                XposedHelpers.callMethod(mSmallRoaming, "setVisibility", 0);
            }
        };
        hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "applyMobileState", beforeUpdate);
        hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "applyMobileState", afterUpdate);
    }

    private void resetImageDrawable(HashMap<String, Integer> dualSignalResMap) {
        MethodHook resetImageDrawable = new MethodHook() {
            @Override
            protected void before(final MethodHookParam param) {
                int subStrengthId = (int) XposedHelpers.getAdditionalInstanceField(param.thisObject, "subStrengthId");
                if (subStrengthId < 0) return;
                if (subStrengthId == 6) subStrengthId = 0;
                Object mobileIconState = XposedHelpers.getObjectField(param.thisObject, "mState");
                int level1 = (int) XposedHelpers.getObjectField(mobileIconState, "strengthId");
                level1 = level1 / 10;
                if (level1 == 6) level1 = 0;
                boolean mLight = (boolean) XposedHelpers.getObjectField(param.thisObject, "mLight");
                boolean mUseTint = (boolean) XposedHelpers.getObjectField(param.thisObject, "mUseTint");
                Object mSmallRoaming = XposedHelpers.getObjectField(param.thisObject, "mSmallRoaming");
                Object mMobile = XposedHelpers.getObjectField(param.thisObject, "mMobile");
                String colorMode = "";
                if (mUseTint && !selectedIconStyle.equals("theme")) {
                    colorMode = "_tint";
                } else if (!mLight) {
                    colorMode = "_dark";
                }
                String iconStyle = "";
                if (!selectedIconStyle.isEmpty()) {
                    iconStyle = "_" + selectedIconStyle;
                }
                String sim1IconId;
                String sim2IconId;
                if (selectedIconTheme == 1) {
                    sim1IconId = "statusbar_signal_classic_1_" + level1 + colorMode;
                    sim2IconId = "statusbar_signal_classic_2_" + subStrengthId + colorMode;
                } else if (selectedIconTheme == 2) {
                    sim1IconId = "statusbar_signal_oa_1_" + level1 + colorMode + iconStyle;
                    sim2IconId = "statusbar_signal_oa_2_" + subStrengthId + colorMode + iconStyle;
                } else {
                    throw new RuntimeException("Cannot get selectedIconTheme.");
                }
                int sim1ResId = dualSignalResMap.get(sim1IconId);
                int sim2ResId = dualSignalResMap.get(sim2IconId);
                XposedHelpers.callMethod(mMobile, "setImageResource", sim1ResId);
                XposedHelpers.callMethod(mSmallRoaming, "setImageResource", sim2ResId);
            }
        };
        findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "applyDarknessInternal", resetImageDrawable);
    }

    private void setDualRowStyleMargin(View mView, View mMobile, View mSmallRoaming) {
        int rightSpacing =  dp2px(rightMargin * 0.5f);
        int leftSpacing = dp2px(leftMargin * 0.5f);
        mView.setPadding(leftSpacing, 0, rightSpacing, 0);

        if (verticalOffset != 8) {
            float marginTop = dp2px((verticalOffset - 8) * 0.5f);
            FrameLayout mobileIcon = (FrameLayout) mMobile.getParent();
            mobileIcon.setTranslationY(marginTop);
        }

        if (iconScale != 10) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mMobile.getLayoutParams();
            int mIconHeight = dp2px(20 * iconScale / 10f);

            if (layoutParams == null) {
                layoutParams = new FrameLayout.LayoutParams(-2, mIconHeight);
            } else {
                layoutParams.height = mIconHeight;
            }
            layoutParams.gravity = Gravity.CENTER;
            mMobile.setLayoutParams(layoutParams);
            mSmallRoaming.setLayoutParams(layoutParams);
        }
    }

    private void getDualRowView(XC_MethodHook.MethodHookParam param) {
        LinearLayout mobileView;
        View mMobile;
        View mSmallRoaming;

        if (isHyperOSVersion(1f)) {
            mobileView = (LinearLayout) param.getResult();
            mMobile = (View) XposedHelpers.getObjectField(param.getResult(), "mMobile");
            mSmallRoaming = (View) XposedHelpers.getObjectField(param.getResult(), "mSmallRoaming");

            setDualRowStyleMargin(mobileView, mMobile, mSmallRoaming); // 边距设置
        } else {
            mobileView = (LinearLayout) param.thisObject;
            mMobile = (View) XposedHelpers.getObjectField(param.thisObject, "mMobile");
            mSmallRoaming = (View) XposedHelpers.getObjectField(param.thisObject, "mSmallRoaming");

            setDualRowStyleMargin(mobileView, mMobile, mSmallRoaming); // 边距设置
        }
    }

    private void setDualRowStyle() {
        if (rightMargin != 0 || leftMargin != 0 || iconScale != 10 || verticalOffset != 8) {
            MethodHook styleHook = new MethodHook() {
                @Override
                protected void after(final MethodHookParam param) {
                    getDualRowView(param);
                }
            };

            if (isHyperOSVersion(1f)) {
                findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", "fromContext", Context.class, String.class, styleHook);
            } else {
                findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", "init", styleHook);
            }
        }
    }
}
