package com.sevtinge.cemiuiler.module.systemui.statusbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.io.FileInputStream;
import java.util.Properties;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class DisplayBatteryDetail extends BaseHook {

    @Override
    public void init() {
        Class <?> ChargeUtilsClass = findClass("com.android.keyguard.charge.ChargeUtils", lpparam.classLoader);
        Class <?> DarkIconDispatcherClass = XposedHelpers.findClass("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader);
        Class <?> Dependency = findClass("com.android.systemui.Dependency", lpparam.classLoader);
        Class <?> StatusBarIconHolder = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarIconHolder", lpparam.classLoader);
        boolean atRight = mPrefsMap.getBoolean("system_statusbar_batterytempandcurrent_atright");

        class BatteryInfoMsg {
            public boolean chargeShow;
            public String batteryText;
        }
        Helpers.hookAllConstructors("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, new MethodHook() {
            Handler mBgHandler;
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context) param.args[0];
                final Handler mHandler = new Handler(Looper.getMainLooper()) {
                    public void handleMessage(Message message) {
                        if (message.what == 100021) {
                            BatteryInfoMsg bi = (BatteryInfoMsg) message.obj;
                            for (TextView tv: mBatteryDetailViews) {
                                XposedHelpers.callMethod(tv, "setBlocked", !bi.chargeShow);
                                XposedHelpers.callMethod(tv, "setNetworkSpeed", bi.batteryText);
                            }
                        }
                    }
                };
                mBgHandler = new Handler((Looper) param.args[1]) {
                    @SuppressLint("DefaultLocale")
                    public void handleMessage(Message message) {
                        if (message.what == 200021) {
                            String batteryInfo = "";
                            boolean showInfo = true;
                            //仅在充电时显示
                            if (mPrefsMap.getBoolean("system_ui_statusbar_battery_only_changing_show") && ChargeUtilsClass != null) {
                                Object batteryStatus = Helpers.getStaticObjectFieldSilently(ChargeUtilsClass, "sBatteryStatus");
                                if (batteryStatus == null) {
                                    showInfo = false;
                                }
                                else {
                                    showInfo = (boolean) XposedHelpers.callMethod(batteryStatus, "isCharging");
                                }
                            }
                            if (showInfo) {
                                Properties props = null;
                                PowerManager powerMgr = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
                                boolean isScreenOn = powerMgr.isInteractive();
                                if (isScreenOn) {
                                    FileInputStream fis = null;
                                    try {
                                        fis = new FileInputStream("/sys/class/power_supply/battery/uevent");
                                        props = new Properties();
                                        props.load(fis);
                                    }
                                    catch (Throwable ignored) {}
                                    finally {
                                        try {
                                            fis.close();
                                        }
                                        catch (Throwable ign) {
                                            throw new RuntimeException(ign);
                                        }
                                    }
                                }
                                if (props != null) {
                                    int tempVal = Integer.parseInt(props.getProperty("POWER_SUPPLY_TEMP"));
                                    String currVal;
                                    int rawCurr = -1 * Math.round(Integer.parseInt(props.getProperty("POWER_SUPPLY_CURRENT_NOW")) / 1000f);
                                    String preferred = "mA";
                                    //电流总显示正值
                                    if (mPrefsMap.getBoolean("prefs_key_system_ui_statusbar_battery_electric_current")) {
                                        rawCurr = Math.abs(rawCurr);
                                    }
                                    if (Math.abs(rawCurr) > 999) {
                                        currVal = String.format("%.2f", rawCurr / 1000f);
                                        preferred = "A";
                                    } else {
                                        currVal = "" + rawCurr;
                                    }
                                    //显示内容
                                    int opt = mPrefsMap.getStringAsInt("system_ui_statusbar_battery_show", 1);
                                    String simpleTempVal = tempVal % 10 == 0 ? (tempVal / 10 + "") : (tempVal / 10f + "");
                                    //隐藏单位
                                    int hideUnit = mPrefsMap.getStringAsInt("system_ui_statusbar_battery_disable", 1);
                                    String tempUnit = (hideUnit == 1 || hideUnit == 2) ? "" : "℃";
                                    String currUnit = (hideUnit == 1 || hideUnit == 3) ? "" : preferred;
                                    if (opt == 1) {
                                        //单排显示
                                        String splitChar = mPrefsMap.getBoolean("system_ui_statusbar_battery_line_show")
                                                ? " " : "\n";
                                        batteryInfo = simpleTempVal + tempUnit + splitChar + currVal + currUnit;
                                        //反序
                                        if (mPrefsMap.getBoolean("system_ui_statusbar_battery_opposite")) {
                                            batteryInfo = currVal + currUnit + splitChar + simpleTempVal + tempUnit;
                                        }
                                    }
                                    else if (opt == 2) {
                                        batteryInfo = simpleTempVal + tempUnit;
                                    }
                                    else {
                                        batteryInfo = currVal + currUnit;
                                    }
                                }
                            }
                            BatteryInfoMsg bi = new BatteryInfoMsg();
                            bi.chargeShow = showInfo;
                            bi.batteryText = batteryInfo;
                            mHandler.obtainMessage(100021, bi).sendToTarget();
                        }
                        mBgHandler.removeMessages(200021);
                        mBgHandler.sendEmptyMessageDelayed(200021, 2000);
                    }
                };
                mBgHandler.sendEmptyMessage(200021);
            }
        });

        if (atRight && !mPrefsMap.getBoolean("system_statusbar_dualrows")  /*不知道干啥的*/) {
            Helpers.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "initMiuiViewsOnViewCreated", View.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Object iconController = XposedHelpers.getObjectField(param.thisObject, "mStatusBarIconController");
                    int slotIndex = (int) XposedHelpers.callMethod(iconController, "getSlotIndex", "battery_detail");
                    Object iconHolder = XposedHelpers.callMethod(iconController, "getIcon", slotIndex, 0);
                    if (iconHolder == null) {
                        iconHolder = XposedHelpers.newInstance(StatusBarIconHolder);
                        XposedHelpers.setObjectField(iconHolder, "mType", 91);
                        XposedHelpers.callMethod(iconController, "setIcon", slotIndex, iconHolder);
                    }
                }
            });

            Helpers.hookAllMethods("com.android.systemui.statusbar.phone.StatusBarIconController$IconManager", lpparam.classLoader, "addHolder", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    if (param.args.length != 4) return;
                    Object iconHolder = param.args[3];
                    int type = (int) XposedHelpers.callMethod(iconHolder, "getType");
                    if (type == 91) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) XposedHelpers.callMethod(param.thisObject, "onCreateLayoutParams");
                        TextView batteryView = createBatteryDetailView(mContext, lp);
                        int i = (int) param.args[0];
                        ViewGroup mGroup = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mGroup");
                        mGroup.addView(batteryView, i);
                        mBatteryDetailViews.add(batteryView);
                        param.setResult(batteryView);
                    }
                }
            });
        }
        else if (!atRight) {
            Helpers.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "initMiuiViewsOnViewCreated", View.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                    TextView mSplitter = (TextView) XposedHelpers.getObjectField(param.thisObject, "mDripNetworkSpeedSplitter");
                    ViewGroup batteryViewContainer = (ViewGroup) mSplitter.getParent();
                    int bvIndex = batteryViewContainer.indexOfChild(mSplitter);
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mSplitter.getLayoutParams();
                    TextView batteryView = createBatteryDetailView(mContext, lp);
                    batteryViewContainer.addView(batteryView, bvIndex + 1);
                    mBatteryDetailViews.add(batteryView);
                    Object DarkIconDispatcher = XposedHelpers.callStaticMethod(Dependency, "get", DarkIconDispatcherClass);
                    XposedHelpers.callMethod(DarkIconDispatcher, "addDarkReceiver", batteryView);
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "mBatteryView", batteryView);
                }
            });
            Helpers.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "showSystemIconArea", boolean.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Object bv = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryView");
                    if (bv != null) {
                        XposedHelpers.callMethod(bv, "setVisibilityByController", true);
                    }
                }
            });
            Helpers.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "hideSystemIconArea", boolean.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Object bv = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryView");
                    if (bv != null) {
                        XposedHelpers.callMethod(bv, "setVisibilityByController", false);
                    }
                }
            });
        }

        Class <?> NetworkSpeedViewClass = XposedHelpers.findClass("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader);
        Helpers.findAndHookMethod(NetworkSpeedViewClass, "getSlot", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object customSlot = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCustomSlot");
                if (customSlot != null) {
                    param.setResult(customSlot);
                }
            }
        });
    }
}
