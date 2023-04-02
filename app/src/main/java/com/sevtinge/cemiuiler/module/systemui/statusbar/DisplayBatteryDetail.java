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
import de.robv.android.xposed.XposedHelpers;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;

public class DisplayBatteryDetail extends BaseHook {
    private static int statusbarTextIconLayoutResId = 0;

    @Override
    public void init() {
        statusbarTextIconLayoutResId = XposedInit.mResHook.addResource("statusbar_text_icon", R.layout.statusbar_text_icon);
        Class<?> ChargeUtilsClass = findClass("com.android.keyguard.charge.ChargeUtils", lpparam.classLoader);
        Class<?> DarkIconDispatcherClass = XposedHelpers.findClass("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader);
        Class<?> Dependency = findClass("com.android.systemui.Dependency", lpparam.classLoader);
        Class<?> StatusBarIconHolder = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarIconHolder", lpparam.classLoader);
        boolean atRight = mPrefsMap.getBoolean("system_ui_statusbar_battery_right_show");

        class BatteryInfoMsg {
            public boolean chargeShow;
            public String batteryText;
        }
        hookAllConstructors("com.android.systemui.statusbar.policy.NetworkSpeedController", new MethodHook() {
            Handler mBgHandler;

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context) param.args[0];
                final Handler mHandler = new Handler(Looper.getMainLooper()) {
                    public void handleMessage(Message message) {
                        if (message.what == 100021) {
                            BatteryInfoMsg bi = (BatteryInfoMsg) message.obj;
                            for (TextView tv : mBatteryDetailViews) {
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
                                } else {
                                    showInfo = (boolean) XposedHelpers.callMethod(batteryStatus, "isCharging");
                                }
                            }
                            if (showInfo) {
                                Properties props = null;
                                PowerManager powerMgr = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                                boolean isScreenOn = powerMgr.isInteractive();
                                if (isScreenOn) {
                                    FileInputStream fis = null;
                                    try {
                                        fis = new FileInputStream("/sys/class/power_supply/battery/uevent");
                                        props = new Properties();
                                        props.load(fis);
                                    } catch (Throwable ignored) {
                                    } finally {
                                        try {
                                            fis.close();
                                        } catch (Throwable ign) {
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
                                    if (mPrefsMap.getBoolean("system_ui_statusbar_battery_electric_current")) {
                                        rawCurr = Math.abs(rawCurr);
                                    }
                                    if (Math.abs(rawCurr) > 999) {
                                        currVal = String.format("%.2f", rawCurr / 1000f);
                                        preferred = "A";
                                    } else {
                                        currVal = "" + rawCurr;
                                    }
                                    //显示内容
                                    int opt = mPrefsMap.getStringAsInt("system_ui_statusbar_battery_show", 0);
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
                                    } else if (opt == 2) {
                                        batteryInfo = simpleTempVal + tempUnit;
                                    } else {
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
                        mBgHandler.sendEmptyMessageDelayed(200021, mPrefsMap.getInt("system_ui_statusbar_battery_update_spacing", 2) * 1000L);
                    }
                };
                mBgHandler.sendEmptyMessage(200021);
            }
        });

        if (atRight && !mPrefsMap.getBoolean("system_statusbar_dualrows")  /*双排状态栏*/) {
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
        } else if (!atRight) {
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
    }

    private static TextView createBatteryDetailView(Context mContext, LinearLayout.LayoutParams lp) {
        TextView batteryView = (TextView) LayoutInflater.from(mContext).inflate(statusbarTextIconLayoutResId, null);
        XposedHelpers.setObjectField(batteryView, "mVisibilityByDisableInfo", 0);
        XposedHelpers.setObjectField(batteryView, "mVisibleByController", true);
        XposedHelpers.setObjectField(batteryView, "mShown", true);
        XposedHelpers.setAdditionalInstanceField(batteryView, "mCustomSlot", "battery_detail");
        Resources res = mContext.getResources();
        int styleId = res.getIdentifier("TextAppearance.StatusBar.Clock", "style", "com.android.systemui");
        batteryView.setTextAppearance(styleId);
        float fontSize = mPrefsMap.getInt("system_ui_statusbar_battery_size", 13);
        if (!mPrefsMap.getBoolean("system_ui_statusbar_battery_line_show") || mPrefsMap.getStringAsInt("system_ui_statusbar_battery_show", 1) != 1) {
            fontSize = (float) (fontSize * 0.5);
        }
        int opt = mPrefsMap.getStringAsInt("system_ui_statusbar_battery_show", 0);
        if (opt == 1 && !mPrefsMap.getBoolean("system_ui_statusbar_battery_line_show")) {
            batteryView.setSingleLine(false);
            batteryView.setMaxLines(2);
            batteryView.setLineSpacing(0, fontSize > 8.5f ? 0.85f : 0.9f);
        }
        batteryView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        if (mPrefsMap.getBoolean("system_ui_statusbar_battery_bold")) {
            batteryView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        int leftMargin = mPrefsMap.getInt("system_ui_statusbar_battery_left_margin", 8);
        leftMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                leftMargin * 0.5f,
                res.getDisplayMetrics()
        );
        int rightMargin = mPrefsMap.getInt("system_ui_statusbar_battery_right_margin", 8);
        rightMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                rightMargin * 0.5f,
                res.getDisplayMetrics()
        );
        int topMargin = 0;
        int verticalOffset = mPrefsMap.getInt("system_ui_statusbar_battery_vertical_offset", 8);
        if (verticalOffset != 8) {
            float marginTop = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (verticalOffset - 8) * 0.5f,
                    res.getDisplayMetrics()
            );
            topMargin = (int) marginTop;
        }
        batteryView.setPaddingRelative(leftMargin, topMargin, rightMargin, 0);
        int fixedWidth = mPrefsMap.getInt("system_ui_statusbar_battery_fixedcontent_width", 10);
        if (fixedWidth > 10) {
            lp.width = (int) (batteryView.getResources().getDisplayMetrics().density * fixedWidth);
        }
        batteryView.setLayoutParams(lp);

        int align = mPrefsMap.getStringAsInt("system_ui_statusbar_battery_align", 1);
        if (align == 2) {
            batteryView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        } else if (align == 3) {
            batteryView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        } else if (align == 4) {
            batteryView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        }
        return batteryView;
    }

    static final ArrayList<TextView> mBatteryDetailViews = new ArrayList<TextView>();

}
