package com.sevtinge.hyperceiler.module.hook.systemui.statusbar;

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
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

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

    int mStatusbarTextIconLayoutResId;
    static final ArrayList<TextView> mBatteryDetailViews = new ArrayList<>();

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
        public boolean atRight;
        public int iconType;

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

        mStatusbarTextIconLayoutResId = mResHook.addResource("statusbar_text_icon", R.layout.statusbar_text_icon);
        mDependency = findClassIfExists("com.android.systemui.Dependency");
        mChargeUtils = showBatteryDetail ? findClassIfExists("com.android.keyguard.charge.ChargeUtils") : null;
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
            findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", "initMiuiViewsOnViewCreated", View.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
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
                protected void before(MethodHookParam param) throws Throwable {
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
                        TextView batteryView = createBatteryDetailView(mContext, lp, createIcon);
                        int i = (int) param.args[0];
                        ViewGroup mGroup = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mGroup");
                        mGroup.addView(batteryView, i);
                        mBatteryDetailViews.add(batteryView);
                        param.setResult(batteryView);
                    }
                }
            });
        }

        if (hasLeftIcon) {
            findAndHookMethod(mMiuiCollapsedStatusBarFragment, "initMiuiViewsOnViewCreated", View.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Context mContext = (Context) callMethod(param.thisObject, "getContext");
                    Object DarkIconDispatcher = XposedHelpers.callStaticMethod(mDependency, "get", mDarkIconDispatcher);
                    TextView mSplitter = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDripNetworkSpeedSplitter");
                    ViewGroup batteryViewContainer = (ViewGroup) mSplitter.getParent();
                    int bvIndex = batteryViewContainer.indexOfChild(mSplitter);
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mSplitter.getLayoutParams();
                    for (TextIcon ti : mTextIcons) {
                        if (!ti.atRight) {
                            TextView batteryView = createBatteryDetailView(mContext, lp, ti);
                            batteryViewContainer.addView(batteryView, bvIndex + 1);
                            mBatteryDetailViews.add(batteryView);
                            callMethod(DarkIconDispatcher, "addDarkReceiver", batteryView);
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, getSlotNameByType(ti.iconType), batteryView);
                        }
                    }
                }
            });

            findAndHookMethod(mMiuiCollapsedStatusBarFragment, "showSystemIconArea", boolean.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    for (TextIcon ti : mTextIcons) {
                        if (!ti.atRight) {
                            Object bv = XposedHelpers.getAdditionalInstanceField(param.thisObject, getSlotNameByType(ti.iconType));
                            if (bv != null) {
                                callMethod(bv, "setVisibilityByController", true);
                            }
                        }
                    }
                }
            });

            findAndHookMethod(mMiuiCollapsedStatusBarFragment, "hideSystemIconArea", boolean.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    for (TextIcon ti : mTextIcons) {
                        if (!ti.atRight) {
                            Object bv = XposedHelpers.getAdditionalInstanceField(param.thisObject, getSlotNameByType((ti.iconType)));
                            if (bv != null) {
                                callMethod(bv, "setVisibilityByController", false);
                            }
                        }
                    }
                }
            });
        }

        findAndHookMethod(mNetworkSpeedView, "getSlot", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object customSlot = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCustomSlot");
                if (customSlot != null) {
                    param.setResult(customSlot);
                }
            }
        });

        Helpers.hookAllConstructors("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, new Helpers.MethodHook() {
            Handler mBgHandler;

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context) param.args[0];
                final Handler mHandler = new Handler(Looper.getMainLooper()) {
                    public void handleMessage(Message message) {
                        if (message.what == 100021) {
                            TextIconInfo tii = (TextIconInfo) message.obj;
                            String slotName = getSlotNameByType(tii.iconType);
                            for (TextView tv : mBatteryDetailViews) {
                                if (slotName.equals(XposedHelpers.getAdditionalInstanceField(tv, "mCustomSlot"))) {
                                    XposedHelpers.callMethod(tv, "setBlocked", !tii.iconShow);
                                    XposedHelpers.callMethod(tv, "setNetworkSpeed", tii.iconText);
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
                                Object batteryStatus = Helpers.getStaticObjectFieldSilently(mFinalChargeUtils, "sBatteryStatus");
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
                                                    XposedLogUtils.logI("get /sys/devices/virtual/thermal/thermal_zone*/temp (" + mPrefsMap.getString("system_ui_statusbar_temp_fix_cpu_get", "0") + ") failed: " + e);
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
                                        XposedLogUtils.logI("get POWER_SUPPLY_CURRENT_NOW failed: " + e);
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

    @SuppressLint("DiscouragedApi")
    private TextView createBatteryDetailView(Context mContext, LinearLayout.LayoutParams lp, TextIcon ti) {
        Resources res = mContext.getResources();
        TextView batteryView = (TextView) LayoutInflater.from(mContext).inflate(res.getIdentifier("network_speed", "layout", "com.android.systemui"), (ViewGroup) null);
        batteryView.setTag("slot_text_icon");
        batteryView.setVisibility(View.VISIBLE);
        XposedHelpers.setObjectField(batteryView, "mVisibilityByDisableInfo", 0);
        XposedHelpers.setObjectField(batteryView, "mVisibleByController", true);
        XposedHelpers.setObjectField(batteryView, "mShown", true);
        XposedHelpers.setAdditionalInstanceField(batteryView, "mCustomSlot", getSlotNameByType(ti.iconType));
        int styleId = res.getIdentifier("TextAppearance.StatusBar.Clock", "style", "com.android.systemui");
        batteryView.setTextAppearance(styleId);
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
        XposedLogUtils.logI("fontsize = " + fontSize);
        int align = mPrefsMap.getStringAsInt("system_ui_status_bar_" + subKey + "_align", 1);
        int fixedWidth = mPrefsMap.getInt("system_ui_statusbar_" + subKey + "_fixedcontent_width", 10);
        int leftMargin = mPrefsMap.getInt("system_ui_statusbar_" + subKey + "_left_margin", 4);
        int rightMargin = mPrefsMap.getInt("system_ui_statusbar_" + subKey + "_right_margin", 4);
        int vertical = mPrefsMap.getInt("system_ui_statusbar_" + subKey + "_vertical_offset", 8);
        boolean isSingleRow = mPrefsMap.getBoolean("system_ui_statusbar_" + subKey + "_line_show");
        boolean isFontBold = mPrefsMap.getBoolean("system_ui_statusbar_" + subKey + "_bold");
        if (opt == 1 && !isSingleRow) {
            batteryView.setSingleLine(false);
            batteryView.setMaxLines(2);
            batteryView.setLineSpacing(0, fontSize > 8.5f ? 0.85f : 0.85f);
        }
        batteryView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        if (isFontBold) batteryView.setTypeface(Typeface.DEFAULT_BOLD);
        switch (align) {
            case 2 -> batteryView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            case 3 -> batteryView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            case 4 -> batteryView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        }
        if (fixedWidth > 10)
            lp.width = (int) (batteryView.getResources().getDisplayMetrics().density * fixedWidth);
        leftMargin = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            leftMargin,
            res.getDisplayMetrics()
        );
        int topMargin = 0;
        if (vertical != 0) {
            float marginTop = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (vertical - 8) * 0.5f,
                res.getDisplayMetrics()
            );
            topMargin = (int) marginTop;
        }
        rightMargin = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            rightMargin,
            res.getDisplayMetrics()
        );
        batteryView.setPaddingRelative(leftMargin, topMargin, rightMargin, 0);
        batteryView.setLayoutParams(lp);
        return batteryView;
    }
}
