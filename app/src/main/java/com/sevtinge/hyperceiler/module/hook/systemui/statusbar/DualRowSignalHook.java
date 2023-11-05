package com.sevtinge.hyperceiler.module.hook.systemui.statusbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.Helpers;

import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class DualRowSignalHook extends BaseHook {

    @Override
    public void init() {
        boolean mobileTypeSingle = mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable"); // 移动网络类型单独显示
        if (!mobileTypeSingle) {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_type_half_to_top_distance", 3);
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_left_inout_over_strength", 0);
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_type_middle_to_strength_start", -0.4f);
        }

        HashMap<String, Integer> dualSignalResMap = new HashMap<String, Integer>();
        String[] colorModeList = {"", "dark", "tint"};
//        String[] iconStyles = {"", "thick", "theme"};
        String selectedIconStyle = mPrefsMap.getString("system_ui_status_mobile_network_icon_style", ""); // 图标样式

        findAndHookMethod("com.android.systemui.SystemUIApplication", lpparam.classLoader, "onCreate", new MethodHook() {
            private boolean isHooked = false;

            @Override
            @SuppressLint("DiscouragedApi")
            protected void after(MethodHookParam param) throws Throwable {
                if (!isHooked) {
                    isHooked = true;
                    Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                    Resources modRes = Helpers.getModuleRes(mContext);
                    for (int slot = 1; slot <= 2; slot++) {
                        for (int lvl = 0; lvl <= 5; lvl++) {
                            for (String colorMode : colorModeList) {
                                if (!selectedIconStyle.equals("theme") || !colorMode.equals("tint")) {
                                    String dualIconResName = "statusbar_signal_" + slot + "_" + lvl + (!colorMode.equals("") ? ("_" + colorMode) : "") + (!selectedIconStyle.equals("") ? ("_" + selectedIconStyle) : "");
                                    int iconResId = modRes.getIdentifier(dualIconResName, "drawable", Helpers.mAppModulePkg);
                                    dualSignalResMap.put(dualIconResName, mResHook.addResource(dualIconResName, iconResId));
                                }
                            }
                        }
                    }
                }
            }
        });

        SparseIntArray signalResToLevelMap = new SparseIntArray();
        // 移动网络和WiFi网络都移动到左侧
        boolean moveSignalLeft = (mPrefsMap.getBoolean("system_ui_status_bar_wifi_at_left") || mPrefsMap.getBoolean("system_ui_status_bar_mobile_network_at_left"));
        String ControllerImplName = moveSignalLeft ? "MiuiDripLeftStatusBarIconControllerImpl" : "StatusBarIconControllerImpl";
        hookAllMethods("com.android.systemui.statusbar.phone." + ControllerImplName, lpparam.classLoader, "setMobileIcons", new MethodHook() {
            private boolean isHooked = false;

            @Override
            @SuppressLint("DiscouragedApi")
            protected void before(MethodHookParam param) throws Throwable {
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

        MethodHook beforeUpdate = new MethodHook() {
            @Override
            protected void before(final MethodHookParam param) throws Throwable {
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
            protected void after(final MethodHookParam param) throws Throwable {
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

        MethodHook resetImageDrawable = new MethodHook() {
            @Override
            protected void before(final MethodHookParam param) throws Throwable {
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
                if (!selectedIconStyle.equals("")) {
                    iconStyle = "_" + selectedIconStyle;
                }
                String sim1IconId = "statusbar_signal_1_" + level1 + colorMode + iconStyle;
                String sim2IconId = "statusbar_signal_2_" + subStrengthId + colorMode + iconStyle;
                int sim1ResId = dualSignalResMap.get(sim1IconId);
                int sim2ResId = dualSignalResMap.get(sim2IconId);
                XposedHelpers.callMethod(mMobile, "setImageResource", sim1ResId);
                XposedHelpers.callMethod(mSmallRoaming, "setImageResource", sim2ResId);
            }
        };
        findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "applyDarknessInternal", resetImageDrawable);
        int rightMargin = mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_right_margin", 0);
        int leftMargin = mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_left_margin", 0);
        int iconScale = mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_size", 10); // 图标缩放
        int verticalOffset = mPrefsMap.getInt("system_ui_statusbar_mobile_network_icon_vertical_offset", 8);
        if (rightMargin > 0 || leftMargin > 0 || iconScale != 10 || verticalOffset != 8) {
            findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", lpparam.classLoader, "init", new MethodHook() {
                @Override
                protected void after(final MethodHookParam param) throws Throwable {
                    LinearLayout mobileView = (LinearLayout) param.thisObject;
                    Context mContext = mobileView.getContext();
                    Resources res = mContext.getResources();
                    int rightSpacing = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        rightMargin * 0.5f,
                        res.getDisplayMetrics()
                    );
                    int leftSpacing = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        leftMargin * 0.5f,
                        res.getDisplayMetrics()
                    );
                    mobileView.setPadding(leftSpacing, 0, rightSpacing, 0);
                    View mMobile = (View) XposedHelpers.getObjectField(param.thisObject, "mMobile");
                    if (verticalOffset != 8) {
                        float marginTop = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            (verticalOffset - 8) * 0.5f,
                            res.getDisplayMetrics()
                        );
                        FrameLayout mobileIcon = (FrameLayout) mMobile.getParent();
                        mobileIcon.setTranslationY(marginTop);
                    }
                    if (iconScale != 10) {
                        View mSmallRoaming = (View) XposedHelpers.getObjectField(param.thisObject, "mSmallRoaming");
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mMobile.getLayoutParams();
                        int mIconHeight = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            20 * iconScale / 10f,
                            res.getDisplayMetrics()
                        );
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
            });
        }
    }
}
