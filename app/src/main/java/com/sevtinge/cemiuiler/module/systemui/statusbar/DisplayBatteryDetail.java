package com.sevtinge.cemiuiler.module.systemui.statusbar;

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
import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.XposedInit;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.io.RandomAccessFile;
import java.util.Properties;

public class DisplayBatteryDetail extends BaseHook {
    private static int statusbarTextIconLayoutResId = 0;

    static class TextIcon {
        public boolean atRight;
        public int iconType;
        public TextIcon(boolean mAtRight, int mIconType) {
            atRight = mAtRight;
            iconType = mIconType;
        }
    }

    private static String getSlotNameByType(int mIconType) {
        String slotName = "";
        if (mIconType == 91) {
            slotName = "battery_info";
        }
        else if (mIconType == 92) {
            slotName = "device_temp";
        }
        return slotName;
    }

    @Override
    public void init() {
        class TextIconInfo {
            public boolean iconShow;
            public int iconType;
            public String iconText;
        }
        boolean showBatteryDetail = MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent");
        boolean showDeviceTemp = MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature");
        statusbarTextIconLayoutResId = XposedInit.mResHook.addResource("statusbar_text_icon", R.layout.statusbar_text_icon);
        Class <?> ChargeUtilsClass = null;
        if (showBatteryDetail) {
            ChargeUtilsClass = findClass("com.android.keyguard.charge.ChargeUtils", lpparam.classLoader);
        }
        Class <?> DarkIconDispatcherClass = XposedHelpers.findClass("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader);
        Class <?> Dependency = findClass("com.android.systemui.Dependency", lpparam.classLoader);
        Class <?> StatusBarIconHolder = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarIconHolder", lpparam.classLoader);
        boolean atRight = mPrefsMap.getBoolean("system_ui_statusbar_battery_right_show");



        Class<?> finalChargeUtilsClass = ChargeUtilsClass;

        boolean batteryAtRight = MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent_atright");
        boolean tempAtRight = MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature_atright");
        ArrayList<TextIcon> textIcons = new ArrayList<>();
        if (showBatteryDetail) {
            textIcons.add(new TextIcon(batteryAtRight, 91));
        }
        if (showDeviceTemp) {
            textIcons.add(new TextIcon(tempAtRight, 92));
        }

        boolean hasRightIcon = false;
        boolean hasLeftIcon = false;
        for (TextIcon ti:textIcons) {
            if (ti.atRight) {
                hasRightIcon = true;
            }
            else {
                hasLeftIcon = true;
            }
        }
        if (hasRightIcon) {
            Helpers.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "initMiuiViewsOnViewCreated", View.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Object iconController = XposedHelpers.getObjectField(param.thisObject, "mStatusBarIconController");
                    for (TextIcon ti:textIcons) {
                        if (ti.atRight) {
                            int slotIndex = (int) XposedHelpers.callMethod(iconController, "getSlotIndex", getSlotNameByType(ti.iconType));
                            Object iconHolder = XposedHelpers.callMethod(iconController, "getIcon", slotIndex, 0);
                            if (iconHolder == null) {
                                iconHolder = XposedHelpers.newInstance(StatusBarIconHolder);
                                XposedHelpers.setObjectField(iconHolder, "mType", ti.iconType);
                                XposedHelpers.callMethod(iconController, "setIcon", slotIndex, iconHolder);
                            }
                        }
                    }
                }
            });

            Helpers.hookAllMethods("com.android.systemui.statusbar.phone.StatusBarIconController$IconManager", lpparam.classLoader, "addHolder", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    if (param.args.length != 4) return;
                    Object iconHolder = param.args[3];
                    int type = (int) XposedHelpers.callMethod(iconHolder, "getType");
                    if (type == 91 || type == 92) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) XposedHelpers.callMethod(param.thisObject, "onCreateLayoutParams");
                        TextIcon createIcon = null;
                        for (TextIcon ti:textIcons) {
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
            Helpers.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "initMiuiViewsOnViewCreated", View.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                    Object DarkIconDispatcher = XposedHelpers.callStaticMethod(Dependency, "get", DarkIconDispatcherClass);
                    TextView mSplitter = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDripNetworkSpeedSplitter");
                    ViewGroup batteryViewContainer = (ViewGroup) mSplitter.getParent();
                    int bvIndex = batteryViewContainer.indexOfChild(mSplitter);
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mSplitter.getLayoutParams();
                    for (TextIcon ti:textIcons) {
                        if (!ti.atRight) {
                            TextView batteryView = createBatteryDetailView(mContext, lp, ti);
                            batteryViewContainer.addView(batteryView, bvIndex + 1);
                            mBatteryDetailViews.add(batteryView);
                            XposedHelpers.callMethod(DarkIconDispatcher, "addDarkReceiver", batteryView);
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, getSlotNameByType(ti.iconType), batteryView);
                        }
                    }
                }
            });
            Helpers.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "showSystemIconArea", boolean.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    for (TextIcon ti:textIcons) {
                        if (!ti.atRight) {
                            Object bv = XposedHelpers.getAdditionalInstanceField(param.thisObject, getSlotNameByType(ti.iconType));
                            if (bv != null) {
                                XposedHelpers.callMethod(bv, "setVisibilityByController", true);
                            }
                        }
                    }
                }
            });
            Helpers.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "hideSystemIconArea", boolean.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    for (TextIcon ti:textIcons) {
                        if (!ti.atRight) {
                            Object bv = XposedHelpers.getAdditionalInstanceField(param.thisObject, getSlotNameByType((ti.iconType)));
                            if (bv != null) {
                                XposedHelpers.callMethod(bv, "setVisibilityByController", false);
                            }
                        }
                    }
                }
            });
        }
        Class<?> NetworkSpeedViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader);
        Helpers.findAndHookMethod(NetworkSpeedViewClass, "getSlot", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object customSlot = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCustomSlot");
                if (customSlot != null) {
                    param.setResult(customSlot);
                }
            }
        });
        Helpers.hookAllConstructors("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, new MethodHook() {
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
                    public void handleMessage(Message message) {
                        if (message.what == 200021) {
                            String batteryInfo = "";
                            String deviceInfo = "";
                            boolean showBatteryInfo = showBatteryDetail;
                            if (showBatteryInfo && MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent_incharge") && finalChargeUtilsClass != null) {
                                Object batteryStatus = Helpers.getStaticObjectFieldSilently(finalChargeUtilsClass, "sBatteryStatus");
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
                                            cpuProps = cpuReader.readLine();
                                        }
                                    } catch (Throwable ign) {
                                    } finally {
                                        try {
                                            if (fis != null) {
                                                fis.close();
                                            }
                                            if (cpuReader != null) {
                                                cpuReader.close();
                                            }
                                        } catch (Throwable ign) {
                                        }
                                    }
                                }
                                if (showBatteryInfo && props != null) {
                                    String currVal;
                                    int rawCurr = -1 * Math.round(Integer.parseInt(props.getProperty("POWER_SUPPLY_CURRENT_NOW")) / 1000f);
                                    String preferred = "mA";
                                    if (MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent_positive")) {
                                        rawCurr = Math.abs(rawCurr);
                                    }
                                    if (Math.abs(rawCurr) > 999) {
                                        currVal = String.format("%.2f", rawCurr / 1000f);
                                        preferred = "A";
                                    } else {
                                        currVal = "" + rawCurr;
                                    }
                                    int opt = MainModule.mPrefs.getStringAsInt("system_statusbar_batterytempandcurrent_content", 1);
                                    int hideUnit = MainModule.mPrefs.getStringAsInt("system_statusbar_batterytempandcurrent_hideunit", 0);
                                    String powerUnit = (hideUnit == 1 || hideUnit == 2) ? "" : "W";
                                    String currUnit = (hideUnit == 1 || hideUnit == 3) ? "" : preferred;
                                    if (opt == 1) {
                                        float voltVal = Integer.parseInt(props.getProperty("POWER_SUPPLY_VOLTAGE_NOW")) / 1000f / 1000f;
                                        String simpleWatt = String.format(Locale.getDefault(), "%.2f", Math.abs(voltVal * rawCurr) / 1000);
                                        String splitChar = MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent_singlerow")
                                                ? " " : "\n";
                                        batteryInfo = simpleWatt + powerUnit + splitChar + currVal + currUnit;
                                        if (MainModule.mPrefs.getBoolean("system_statusbar_batterytempandcurrent_reverseorder")) {
                                            batteryInfo = currVal + currUnit + splitChar + simpleWatt + powerUnit;
                                        }
                                    } else if (opt == 2) {
                                        float voltVal = Integer.parseInt(props.getProperty("POWER_SUPPLY_VOLTAGE_NOW")) / 1000f / 1000f;
                                        String simpleWatt = String.format(Locale.getDefault(), "%.2f", Math.abs(voltVal * rawCurr) / 1000);
                                        batteryInfo = simpleWatt + powerUnit;
                                    } else {
                                        batteryInfo = currVal + currUnit;
                                    }
                                }
                                if (showDeviceTemp && props != null && cpuProps != null) {
                                    int batteryTempVal = Integer.parseInt(props.getProperty("POWER_SUPPLY_TEMP"));
                                    int cpuTempVal = Integer.parseInt(cpuProps);
                                    String simpleBatteryTemp = String.format(Locale.getDefault(), "%.1f", batteryTempVal / 10f);
                                    String simpleCpuTemp = String.format(Locale.getDefault(), "%.1f", cpuTempVal / 1000f);
                                    int opt = MainModule.mPrefs.getStringAsInt("system_statusbar_showdevicetemperature_content", 1);
                                    boolean hideUnit = MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature_hideunit");
                                    String tempUnit = hideUnit ? "" : "℃";
                                    if (opt == 1) {
                                        String splitChar = MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature_singlerow")
                                                ? " " : "\n";
                                        deviceInfo = simpleBatteryTemp + tempUnit + splitChar + simpleCpuTemp + tempUnit;
                                        if (MainModule.mPrefs.getBoolean("system_statusbar_showdevicetemperature_reverseorder")) {
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
                                mHandler.obtainMessage(100021, tii).sendToTarget();
                            }
                            if (showDeviceTemp) {
                                TextIconInfo tii = new TextIconInfo();
                                tii.iconShow = showDeviceTemp;
                                tii.iconText = deviceInfo;
                                tii.iconType = 92;
                                mHandler.obtainMessage(100021, tii).sendToTarget();
                            }
                        }
                        mBgHandler.removeMessages(200021);
                        mBgHandler.sendEmptyMessageDelayed(200021, 2000);
                    }
                };
                mBgHandler.sendEmptyMessage(200021);
            }
        });
    }

    private static TextView createBatteryDetailView(Context mContext, LinearLayout.LayoutParams lp, TextIcon ti) {
        TextView batteryView = (TextView) LayoutInflater.from(mContext).inflate(statusbarTextIconLayoutResId, null);
        XposedHelpers.setObjectField(batteryView, "mVisibilityByDisableInfo", 0);
        XposedHelpers.setObjectField(batteryView, "mVisibleByController", true);
        XposedHelpers.setObjectField(batteryView, "mShown", true);
        XposedHelpers.setAdditionalInstanceField(batteryView, "mCustomSlot", getSlotNameByType(ti.iconType));
        Resources res = mContext.getResources();
        int styleId = res.getIdentifier("TextAppearance.StatusBar.Clock", "style", "com.android.systemui");
        batteryView.setTextAppearance(styleId);
        String subKey = "";
        if (ti.iconType == 91) {
            subKey = "batterytempandcurrent";
        }
        else if (ti.iconType == 92) {
            subKey = "showdevicetemperature";
        }
        float fontSize = MainModule.mPrefs.getInt("system_statusbar_" + subKey + "_fontsize", 16) * 0.5f;
        int opt = MainModule.mPrefs.getStringAsInt("system_statusbar_" + subKey + "_content", 1);
        if (opt == 1 && !MainModule.mPrefs.getBoolean("system_statusbar_" + subKey + "_singlerow")) {
            batteryView.setSingleLine(false);
            batteryView.setMaxLines(2);
            batteryView.setLineSpacing(0, fontSize > 8.5f ? 0.85f : 0.9f);
        }
        batteryView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        if (MainModule.mPrefs.getBoolean("system_statusbar_" + subKey + "_bold")) {
            batteryView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        int leftMargin = MainModule.mPrefs.getInt("system_statusbar_" + subKey + "_leftmargin", 8);
        leftMargin = (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                leftMargin * 0.5f,
                res.getDisplayMetrics()
        );
        int rightMargin = MainModule.mPrefs.getInt("system_statusbar_" + subKey + "_rightmargin", 8);
        rightMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                rightMargin * 0.5f,
                res.getDisplayMetrics()
        );
        int topMargin = 0;
        int verticalOffset = MainModule.mPrefs.getInt("system_statusbar_" + subKey + "_verticaloffset", 8);
        if (verticalOffset != 8) {
            float marginTop = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (verticalOffset - 8) * 0.5f,
                    res.getDisplayMetrics()
            );
            topMargin = (int) marginTop;
        }
        batteryView.setPaddingRelative(leftMargin, topMargin, rightMargin, 0);
        int fixedWidth = MainModule.mPrefs.getInt("system_statusbar_" + subKey + "_fixedcontent_width", 10);
        if (fixedWidth > 10) {
            lp.width = (int)(batteryView.getResources().getDisplayMetrics().density * fixedWidth);
        }
        batteryView.setLayoutParams(lp);

        int align = MainModule.mPrefs.getStringAsInt("system_statusbar_" + subKey + "_align", 1);
        if (align == 2) {
            batteryView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        }
        else if (align == 3) {
            batteryView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        else if (align == 4) {
            batteryView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        }
        return batteryView;
    }

    static final ArrayList<TextView> mBatteryDetailViews = new ArrayList<TextView>();

}
