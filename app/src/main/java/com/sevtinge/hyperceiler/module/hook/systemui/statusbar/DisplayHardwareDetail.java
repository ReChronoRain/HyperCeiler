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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar;

import static com.sevtinge.hyperceiler.utils.api.NekoQiqiApisKt.isNewNetworkStyle;
import static com.sevtinge.hyperceiler.utils.devicesdk.AppUtilsKt.dp2px;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static de.robv.android.xposed.XposedHelpers.callMethod;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.tool.ResourcesTool;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

import de.robv.android.xposed.XposedHelpers;

public class DisplayHardwareDetail extends BaseHook {
    boolean showDeviceTemp;
    boolean showBatteryDetail;
    boolean isTempAtRight;
    boolean isBatteryAtRight;
    /*boolean hasRightIcon = false;
    boolean hasLeftIcon = false;*/

    Class<?> mDependency;
    Class<?> mChargeUtils;
    Class<?> mIconManager;
    Class<?> mNetworkSpeedView;
    Class<?> mDarkIconDispatcher;
    Class<?> mStatusBarIconHolder;
    Class<?> mNetworkSpeedController;
    Class<?> mMiuiCollapsedStatusBarFragment;

    private static int mStatusbarTextIconLayoutResId;
    private final static int textIconTagId = ResourcesTool.getFakeResId("text_icon_tag");
    private static final ArrayList<View> mStatusbarTextIcons = new ArrayList<>();

    private String getSlotNameByType(int mIconType) {
        String slotName = "";
        if (mIconType == 91) {
            slotName = "battery_detail";
        } else if (mIconType == 92) {
            slotName = "device_temp";
        }
        return slotName;
    }

    static class TextIcon {
        public final boolean atRight;
        public final int iconType;

        public TextIcon(boolean mAtRight, int mIconType) {
            atRight = mAtRight;
            iconType = mIconType;
        }
    }

    static class TextIconInfo {
        public boolean iconShow;
        public int iconType;
        public String iconText;
    }

    @Override
    public void init() {
        showBatteryDetail = mPrefsMap.getBoolean("system_ui_statusbar_battery_enable"); // 电池相关
        showDeviceTemp = mPrefsMap.getBoolean("system_ui_statusbar_temp_enable"); // 温度相关

        isTempAtRight = mPrefsMap.getBoolean("system_ui_statusbar_temp_right_show");
        isBatteryAtRight = mPrefsMap.getBoolean("system_ui_statusbar_battery_right_show");

        if (isNewNetworkStyle()) {
            mStatusbarTextIconLayoutResId = R.layout.statusbar_text_icon_new;
        } else {
            mStatusbarTextIconLayoutResId = R.layout.statusbar_text_icon;
        }

        mDependency = findClassIfExists("com.android.systemui.Dependency");
        if (showBatteryDetail) {
            if (isMoreAndroidVersion(34)) {
                mChargeUtils = findClassIfExists("com.miui.charge.ChargeUtils");
            } else {
                mChargeUtils = findClassIfExists("com.android.keyguard.charge.ChargeUtils");
            }
        } else {
            mChargeUtils = null;
        }
        mIconManager = findClassIfExists("com.android.systemui.statusbar.phone.StatusBarIconController$IconManager");
        mNetworkSpeedView = findClassIfExists("com.android.systemui.statusbar.views.NetworkSpeedView");
        mDarkIconDispatcher = findClassIfExists("com.android.systemui.plugins.DarkIconDispatcher");
        mStatusBarIconHolder = findClassIfExists("com.android.systemui.statusbar.phone.StatusBarIconHolder");
        mNetworkSpeedController = findClassIfExists("com.android.systemui.statusbar.policy.NetworkSpeedController");
        mMiuiCollapsedStatusBarFragment = findClassIfExists("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment");

        Class<?> mFinalChargeUtils = mChargeUtils;

        ArrayList<TextIcon> mTextIcons = new ArrayList<>();
        if (showBatteryDetail) {
            mTextIcons.add(new TextIcon(isBatteryAtRight, 91));
        }
        if (showDeviceTemp) {
            mTextIcons.add(new TextIcon(isTempAtRight, 92));
        }

        boolean hasRightIcon = false;
        boolean hasLeftIcon = false;
        for (TextIcon ti : mTextIcons) {
            if (ti.atRight) {
                hasRightIcon = true;
            } else {
                hasLeftIcon = true;
            }
        }

        if (hasRightIcon) {
            hookAllConstructors("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    Object iconController = XposedHelpers.getObjectField(param.thisObject, "mStatusBarIconController");
                    for (TextIcon ti : mTextIcons) {
                        if (ti.atRight) {
                            int slotIndex = (int) callMethod(iconController, "getSlotIndex", getSlotNameByType(ti.iconType));
                            Object iconHolder = callMethod(iconController, "getIcon", slotIndex, 0);
                            if (iconHolder == null) {
                                iconHolder = XposedHelpers.newInstance(mStatusBarIconHolder);
                                XposedHelpers.setObjectField(iconHolder, "mType", ti.iconType);
                                callMethod(iconController, "setIcon", slotIndex, iconHolder);
                            }
                        }
                    }
                }
            });

            hookAllMethods(mIconManager, "addHolder", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    if (param.args.length != 4) return;
                    Object iconHolder = param.args[3];
                    int type = (int) callMethod(iconHolder, "getType");
                    if (type == 91 || type == 92) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) callMethod(param.thisObject, "onCreateLayoutParams");
                        TextIcon createIcon = null;
                        for (TextIcon ti : mTextIcons) {
                            if (ti.iconType == type) {
                                createIcon = ti;
                                break;
                            }
                        }
                        View iconView = createStatusbarTextIcon(mContext, lp, createIcon);
                        int i = (int) param.args[0];
                        ViewGroup mGroup = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mGroup");
                        mGroup.addView(iconView, i);
                        mStatusbarTextIcons.add(iconView);
                        param.setResult(iconView);
                    }
                }
            });
        }

        if (hasLeftIcon) {
            findAndHookMethod(mMiuiCollapsedStatusBarFragment, "initMiuiViewsOnViewCreated", View.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    Context mContext = (Context) callMethod(param.thisObject, "getContext");
                    Object DarkIconDispatcher = XposedHelpers.callStaticMethod(mDependency, "get", mDarkIconDispatcher);
                    View baseAnchor;
                    if (isNewNetworkStyle()) {
                        baseAnchor = (View) XposedHelpers.getObjectField(param.thisObject, "mClockView");
                    } else {
                        baseAnchor = (View) XposedHelpers.getObjectField(param.thisObject, "mDripNetworkSpeedSplitter");
                    }
                    ViewGroup mStatusBarLeftContainer = (ViewGroup) baseAnchor.getParent();
                    int bvIndex = mStatusBarLeftContainer.indexOfChild(baseAnchor);
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) baseAnchor.getLayoutParams();
                    for (TextIcon ti : mTextIcons) {
                        if (!ti.atRight) {
                            View iconView = createStatusbarTextIcon(mContext, lp, ti);
                            mStatusBarLeftContainer.addView(iconView, bvIndex + 1);
                            mStatusbarTextIcons.add(iconView);
                            XposedHelpers.callMethod(DarkIconDispatcher, "addDarkReceiver", iconView);
                        }
                    }
                }
            });

            findAndHookMethod(mMiuiCollapsedStatusBarFragment, "showSystemIconArea", boolean.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    for (View iconView : mStatusbarTextIcons) {
                        Object tagData = iconView.getTag(textIconTagId);
                        if (tagData != null) {
                            TextIcon ti = (TextIcon) tagData;
                            if (!ti.atRight) {
                                XposedHelpers.callMethod(iconView, "setVisibilityByController", true);
                            }
                        }
                    }
                }
            });

            findAndHookMethod(mMiuiCollapsedStatusBarFragment, "hideSystemIconArea", boolean.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    for (View iconView : mStatusbarTextIcons) {
                        Object tagData = iconView.getTag(textIconTagId);
                        if (tagData != null) {
                            TextIcon ti = (TextIcon) tagData;
                            if (!ti.atRight) {
                                XposedHelpers.callMethod(iconView, "setVisibilityByController", false);
                            }
                        }
                    }
                }
            });
        }

        findAndHookMethod(mNetworkSpeedView, "getSlot", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                View nsView = (View) param.thisObject;
                Object tagData = nsView.getTag(textIconTagId);
                if (tagData != null) {
                    TextIcon ti = (TextIcon) tagData;
                    param.setResult(getSlotNameByType(ti.iconType));
                }
            }
        });

        hookAllConstructors("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, new MethodHook() {
            Handler mBgHandler;

            @Override
            protected void after(MethodHookParam param) {
                Context mContext = (Context) param.args[0];
                final Handler mHandler = new Handler(Looper.getMainLooper()) {
                    public void handleMessage(Message message) {
                        if (message.what == 100021) {
                            TextIconInfo tii = (TextIconInfo) message.obj;
                            for (View tv : mStatusbarTextIcons) {
                                Object tagData = tv.getTag(textIconTagId);
                                if (tagData != null) {
                                    TextIcon ti = (TextIcon) tagData;
                                    if (tii.iconType == ti.iconType) {
                                        XposedHelpers.callMethod(tv, "setBlocked", !tii.iconShow);
                                        if (tii.iconShow) {
                                            if (isNewNetworkStyle()) {
                                                XposedHelpers.callMethod(tv, "setNetworkSpeed", tii.iconText, "");
                                            } else {
                                                XposedHelpers.callMethod(tv, "setNetworkSpeed", tii.iconText);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                };
                mBgHandler = new Handler((Looper) param.args[1]) {
                    @SuppressLint("DefaultLocale")
                    public void handleMessage(@NonNull Message message) {
                        String subKey = "";
                        if (message.what == 200021) {
                            String batteryInfo = "";
                            String deviceInfo = "";
                            boolean showBatteryInfo = showBatteryDetail;
                            if (showBatteryInfo && mPrefsMap.getBoolean("system_ui_statusbar_battery_only_changing_show") && mFinalChargeUtils != null) {
                                Object batteryStatus = getStaticObjectFieldSilently(mFinalChargeUtils, "sBatteryStatus");
                                if (batteryStatus == null) {
                                    showBatteryInfo = false;
                                } else {
                                    showBatteryInfo = (boolean) XposedHelpers.callMethod(batteryStatus, "isCharging");
                                }
                            }
                            if (showBatteryInfo || showDeviceTemp) {
                                Properties props = null;
                                String cpuProps = null;
                                PowerManager powerMgr = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                                boolean isScreenOn = powerMgr.isInteractive();
                                if (isScreenOn) {
                                    FileInputStream fis = null;
                                    RandomAccessFile cpuReader = null;
                                    try {
                                        fis = new FileInputStream("/sys/class/power_supply/battery/uevent");
                                        props = new Properties();
                                        props.load(fis);
                                        if (showDeviceTemp) {
                                            cpuReader = new RandomAccessFile("/sys/devices/virtual/thermal/thermal_zone0/temp", "r");
                                            if (!Objects.equals(mPrefsMap.getString("system_ui_statusbar_temp_fix_cpu_get", "0"), "")) {
                                                try {
                                                    cpuReader = new RandomAccessFile("/sys/devices/virtual/thermal/thermal_zone" + mPrefsMap.getString("system_ui_statusbar_temp_fix_cpu_get", "0") + "/temp", "r");
                                                } catch (FileNotFoundException e) {
                                                    logI(TAG, DisplayHardwareDetail.this.lpparam.packageName, "get /sys/devices/virtual/thermal/thermal_zone*/temp (" + mPrefsMap.getString("system_ui_statusbar_temp_fix_cpu_get", "0") + ") failed: " + e);
                                                }
                                            }
                                            cpuProps = cpuReader.readLine();
                                        }
                                    } catch (Throwable ignored) {
                                    } finally {
                                        try {
                                            if (fis != null) {
                                                fis.close();
                                            }
                                            if (cpuReader != null) {
                                                cpuReader.close();
                                            }
                                        } catch (Throwable ignored) {
                                        }
                                    }
                                }
                                if (showBatteryInfo && props != null) {
                                    String currVal;
                                    int rawCurr = 0;
                                    try {
                                        rawCurr = -1 * Math.round(Integer.parseInt(props.getProperty("POWER_SUPPLY_CURRENT_NOW")) / 1000f);// 概率fc
                                    } catch (NumberFormatException e) {
                                        logE(TAG, DisplayHardwareDetail.this.lpparam.packageName, "get POWER_SUPPLY_CURRENT_NOW failed", e);
                                    }
                                    String preferred = "mA";
                                    if (mPrefsMap.getBoolean("system_ui_statusbar_battery_electric_current")) { // 电流始终显示正值
                                        rawCurr = Math.abs(rawCurr);
                                    }
                                    if (Math.abs(rawCurr) > 999) {
                                        currVal = String.format("%.2f", rawCurr / 1000f);
                                        preferred = "A";
                                    } else {
                                        currVal = "" + rawCurr;
                                    }
                                    int opt = mPrefsMap.getStringAsInt("system_ui_statusbar_battery_show", 1); // 电池显示内容
                                    int hideUnit = mPrefsMap.getStringAsInt("system_ui_statusbar_battery_disable", 0);
                                    String powerUnit = (hideUnit == 1 || hideUnit == 2) ? "" : "W";
                                    String currUnit = (hideUnit == 1 || hideUnit == 3) ? "" : preferred;
                                    if (opt == 1) {
                                        float voltVal = 0f;
                                        String powerNow;
                                        try {
                                            powerNow = props.getProperty("POWER_SUPPLY_VOLTAGE_NOW");
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                        if (powerNow != null)
                                            voltVal = Integer.parseInt(powerNow) / 1000f / 1000f;
                                        String simpleWatt = String.format(Locale.getDefault(), "%.2f", Math.abs(voltVal * rawCurr) / 1000);
                                        String splitChar = mPrefsMap.getBoolean("system_ui_statusbar_battery_line_show")
                                            ? " " : "\n";
                                        batteryInfo = simpleWatt + powerUnit + splitChar + currVal + currUnit;
                                        if (mPrefsMap.getBoolean("system_ui_statusbar_battery_opposite")) {
                                            batteryInfo = currVal + currUnit + splitChar + simpleWatt + powerUnit;
                                        }
                                    } else if (opt == 2) {
                                        float voltVal = 0f;
                                        String powerNow;
                                        try {
                                            powerNow = props.getProperty("POWER_SUPPLY_VOLTAGE_NOW");
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                        if (powerNow != null)
                                            voltVal = Integer.parseInt(powerNow) / 1000f / 1000f;
                                        String simpleWatt = String.format(Locale.getDefault(), "%.2f", Math.abs(voltVal * rawCurr) / 1000);
                                        batteryInfo = simpleWatt + powerUnit;
                                    } else {
                                        batteryInfo = currVal + currUnit;
                                    }
                                }
                                if (showDeviceTemp && props != null && cpuProps != null) {
                                    int batteryTempVal = 0;
                                    String powerTempNow;
                                    try {
                                        powerTempNow = props.getProperty("POWER_SUPPLY_TEMP");
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    if (powerTempNow != null)
                                        batteryTempVal = Integer.parseInt(powerTempNow);
                                    int cpuTempVal = Integer.parseInt(cpuProps);
                                    boolean DecimalPlacesOr = mPrefsMap.getBoolean("system_ui_statusbar_temp_decimal_places");
                                    String DecimalPlaces = DecimalPlacesOr ? "%.0f" : "%.1f";
                                    String simpleBatteryTemp = String.format(Locale.getDefault(), DecimalPlaces, batteryTempVal / 10f);
                                    String simpleCpuTemp = String.format(Locale.getDefault(), DecimalPlaces, cpuTempVal / 1000f);
                                    int opt = mPrefsMap.getStringAsInt("system_ui_statusbar_temp_show", 1);
                                    boolean hideUnit = mPrefsMap.getBoolean("system_ui_statusbar_temp_disable");
                                    String tempUnit = hideUnit ? "" : "℃";
                                    if (opt == 1) {
                                        String splitChar = mPrefsMap.getBoolean("system_ui_statusbar_temp_line_show")
                                            ? " " : "\n";
                                        deviceInfo = simpleBatteryTemp + tempUnit + splitChar + simpleCpuTemp + tempUnit;
                                        if (mPrefsMap.getBoolean("system_ui_statusbar_temp_opposite")) {
                                            deviceInfo = simpleCpuTemp + tempUnit + splitChar + simpleBatteryTemp + tempUnit;
                                        }
                                    } else if (opt == 2) {
                                        deviceInfo = simpleBatteryTemp + tempUnit;
                                    } else {
                                        deviceInfo = simpleCpuTemp + tempUnit;
                                    }
                                }
                            }
                            if (showBatteryDetail) {
                                TextIconInfo tii = new TextIconInfo();
                                tii.iconShow = showBatteryInfo;
                                tii.iconText = batteryInfo;
                                tii.iconType = 91;
                                subKey = "battery";
                                mHandler.obtainMessage(100021, tii).sendToTarget();
                            }
                            if (showDeviceTemp) {
                                TextIconInfo tii = new TextIconInfo();
                                tii.iconShow = showDeviceTemp;
                                tii.iconText = deviceInfo;
                                tii.iconType = 92;
                                subKey = "temp";
                                mHandler.obtainMessage(100021, tii).sendToTarget();
                            }
                        }
                        mBgHandler.removeMessages(200021);
                        mBgHandler.sendEmptyMessageDelayed(200021, mPrefsMap.getInt("system_ui_statusbar_" + subKey + "_update_spacing", 2) * 1000L);
                    }
                };
                mBgHandler.sendEmptyMessage(200021);
            }
        });
    }

    private static TextView getIconTextView(View iconView) {
        if (isNewNetworkStyle()) {
            return (TextView) XposedHelpers.getObjectField(iconView, "mNetworkSpeedNumberText");
        }
        return (TextView) iconView;
    }

    private static View createStatusbarTextIcon(Context mContext, LinearLayout.LayoutParams lp, TextIcon ti) {
        View iconView = LayoutInflater.from(mContext).inflate(mStatusbarTextIconLayoutResId, null);
        iconView.setTag(textIconTagId, ti);
        if (!isNewNetworkStyle()) {
            XposedHelpers.setObjectField(iconView, "mVisibilityByDisableInfo", 0);
        } else {
            View mNumber = iconView.findViewWithTag("network_speed_number");
            XposedHelpers.setObjectField(iconView, "mNetworkSpeedNumberText", mNumber);
            View mUnit = iconView.findViewWithTag("network_speed_unit");
            XposedHelpers.setObjectField(iconView, "mNetworkSpeedUnitText", mUnit);
        }
        initStatusbarTextIcon(mContext, lp, ti, iconView);
        return iconView;
    }

    @SuppressLint("DiscouragedApi")
    private static void initStatusbarTextIcon(Context mContext, LinearLayout.LayoutParams lp, TextIcon ti, View iconView) {
        XposedHelpers.setObjectField(iconView, "mVisibleByController", true);
        XposedHelpers.setObjectField(iconView, "mShown", true);
        TextView iconTextView = getIconTextView(iconView);
        Resources res = mContext.getResources();
        int styleId = res.getIdentifier("TextAppearance.StatusBar.Clock", "style", "com.android.systemui");
        iconTextView.setTextAppearance(styleId);
        String subKey = "";
        if (ti.iconType == 91) {
            subKey = "battery";
        } else if (ti.iconType == 92) {
            subKey = "temp";
        }
        float fontSize = mPrefsMap.getInt("system_ui_statusbar_" + subKey + "_size", 13);
        int opt = mPrefsMap.getStringAsInt("system_ui_statusbar_" + subKey + "_show", 1);
        if (!mPrefsMap.getBoolean("system_ui_statusbar_" + subKey + "_line_show") || mPrefsMap.getStringAsInt("system_ui_statusbar_" + subKey + "_show", 1) != 1) {
            fontSize = (float) (fontSize * 0.5);
        }
        // logI(TAG, this.lpparam.packageName, "fontsize = " + fontSize);
        int align = mPrefsMap.getStringAsInt("system_ui_status_bar_" + subKey + "_align", 1);
        int fixedWidth = mPrefsMap.getInt("system_ui_statusbar_" + subKey + "_fixedcontent_width", 10);
        int leftMargin = mPrefsMap.getInt("system_ui_statusbar_" + subKey + "_left_margin", 4);
        int rightMargin = mPrefsMap.getInt("system_ui_statusbar_" + subKey + "_right_margin", 4);
        int vertical = mPrefsMap.getInt("system_ui_statusbar_" + subKey + "_vertical_offset", 8);
        boolean isSingleRow = mPrefsMap.getBoolean("system_ui_statusbar_" + subKey + "_line_show");
        boolean isFontBold = mPrefsMap.getBoolean("system_ui_statusbar_" + subKey + "_bold");

        if (opt == 1 && !isSingleRow) {
            iconTextView.setSingleLine(false);
            iconTextView.setMaxLines(2);
            iconTextView.setLineSpacing(0, fontSize > 8.5f ? 0.85f : 0.9f);
        }
        // 设置字体大小
        iconTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        // 设置字体是否加粗
        if (isFontBold) {
            iconTextView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        // 设置边距
        leftMargin = dp2px(leftMargin * 0.5f);
        rightMargin = dp2px(rightMargin * 0.5f);
        int topMargin = 0;
        if (vertical != 8) {
            topMargin = dp2px((vertical - 8) * 0.5f);
        }
        iconTextView.setPaddingRelative(leftMargin, topMargin, rightMargin, 0);
        if (fixedWidth > 10) {
            lp.width = dp2px(fixedWidth);
        }
        iconTextView.setLayoutParams(lp);

        switch (align) {
            case 2 -> iconTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            case 3 -> iconTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            case 4 -> iconTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        }
    }
}
