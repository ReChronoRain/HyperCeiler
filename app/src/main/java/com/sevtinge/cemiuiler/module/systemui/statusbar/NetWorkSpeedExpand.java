package com.sevtinge.cemiuiler.module.systemui.statusbar;

import static java.lang.System.nanoTime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.net.NetworkInterface;
import java.util.Enumeration;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class NetWorkSpeedExpand extends BaseHook {

    private static long measureTime = 0;
    private static long txBytesTotal = 0;
    private static long rxBytesTotal = 0;
    private static long txSpeed = 0;
    private static long rxSpeed = 0;

    private static Pair<Long, Long> getTrafficBytes(Object thisObject) {
        long tx = -1L;
        long rx = -1L;

        try {
            for (Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces(); list.hasMoreElements(); ) {
                NetworkInterface iface = list.nextElement();
                if (iface.isUp() && !iface.isVirtual() && !iface.isLoopback() && !iface.isPointToPoint() && !"".equals(iface.getName())) {
                    tx += (long) XposedHelpers.callStaticMethod(TrafficStats.class, "getTxBytes", iface.getName());
                    rx += (long) XposedHelpers.callStaticMethod(TrafficStats.class, "getRxBytes", iface.getName());
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
            tx = TrafficStats.getTotalTxBytes();
            rx = TrafficStats.getTotalRxBytes();
        }

        return new Pair<Long, Long>(tx, rx);
    }

//  网速隐藏 B/s 单位
    @SuppressLint("DefaultLocale")
    private static String humanReadableByteCount(Context ctx, long bytes) {
        try {
            Resources modRes = Helpers.getModuleRes(ctx);
            boolean hideSecUnit = mPrefsMap.getBoolean("system_ui_statusbar_network_speed_sec_unit");
            String unitSuffix = modRes.getString(R.string.system_ui_statusbar_network_speed_Bs);
            if (hideSecUnit) {
                unitSuffix = "";
            }
            float f = (bytes) / 1024.0f;
            int expIndex = 0;
            if (f > 999.0f) {
                expIndex = 1;
                f /= 1024.0f;
            }
            char pre = modRes.getString(R.string.system_ui_statusbar_network_speed_speedunits).charAt(expIndex);
            return (f < 100.0f ? String.format("%.1f", f) : String.format("%.0f", f)) + String.format("%s" + unitSuffix, pre);
        } catch (Throwable t) {
            Helpers.log(t);
            return "";
        }
    }

    @Override
    public void init() {
        Class<?> nscCls = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader);

//      网速更新间隔
        Helpers.findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController", lpparam.classLoader, "postUpdateNetworkSpeedDelay", long.class, new MethodHook() {
            @Override
            protected void before(final MethodHookParam param) {
                long originInterval = (long) param.args[0];
                if (originInterval == 4000L) {
                    long newInterval = mPrefsMap.getInt("system_ui_statusbar_network_speed_update_spacing", 4) * 1000L;
                    param.args[0] = newInterval;
                }
            }
        });


//      固定宽度以防相邻元素左右防抖
        if (mPrefsMap.getInt("system_ui_statusbar_network_speed_fixedcontent_width", 10) > 10) {
            Helpers.hookAllMethods("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader, "applyNetworkSpeedState", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    TextView meter = (TextView) param.thisObject;
                    if ((meter.getTag() == null || !"slot_text_icon".equals(meter.getTag()))) {
                        XposedHelpers.getAdditionalInstanceField(param.thisObject, "inited");
                    }
                }
            });
        }

//      双排网速相关
        if (nscCls == null) {
            Helpers.log("DetailedNetSpeedHook", "No NetworkSpeed view or controller");
        } else {
            Helpers.findAndHookMethod(nscCls, "getTotalByte", new MethodHook() {
                @Override
                protected void after(final MethodHookParam param) {
                    Pair<Long, Long> bytes = getTrafficBytes(param.thisObject);
                    txBytesTotal = bytes.first;
                    rxBytesTotal = bytes.second;
                    measureTime = nanoTime();
                }
            });
            Helpers.findAndHookMethod(nscCls, "updateNetworkSpeed", new MethodHook() {
                @Override
                protected void before(final MethodHookParam param) {
                    boolean isConnected = false;
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                    Network nw = mConnectivityManager.getActiveNetwork();
                    if (nw != null) {
                        NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(nw);
                        if (capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))) {
                            isConnected = true;
                        }
                    }
                    if (isConnected) {
                        long nanoTime = nanoTime();
                        long newTime = nanoTime - measureTime;
                        measureTime = nanoTime;
                        if (newTime == 0) newTime = Math.round(4 * Math.pow(10, 9));
                        Pair<Long, Long> bytes = getTrafficBytes(param.thisObject);
                        long newTxBytes = bytes.first;
                        long newRxBytes = bytes.second;
                        long newTxBytesFixed = newTxBytes - txBytesTotal;
                        long newRxBytesFixed = newRxBytes - rxBytesTotal;
                        if (newTxBytesFixed < 0 || txBytesTotal == 0) newTxBytesFixed = 0;
                        if (newRxBytesFixed < 0 || rxBytesTotal == 0) newRxBytesFixed = 0;
                        txSpeed = Math.round(newTxBytesFixed / (newTime / Math.pow(10, 9)));
                        rxSpeed = Math.round(newRxBytesFixed / (newTime / Math.pow(10, 9)));
                        txBytesTotal = newTxBytes;
                        rxBytesTotal = newRxBytes;
                    } else {
                        txSpeed = 0;
                        rxSpeed = 0;
                    }
                }
            });

            Helpers.findAndHookMethod(nscCls, "updateText", String.class, new MethodHook() {
                @Override
                protected void before(final MethodHookParam param) {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
//                  隐藏慢速
                    boolean hideLow = mPrefsMap.getBoolean("system_ui_statusbar_network_speed_hide");
//                  慢速水平
                    int lowLevel = mPrefsMap.getInt("system_ui_statusbar_network_speed_hide_slow", 1) * 1024;
//                  网速图标
                    int icons = Integer.parseInt(mPrefsMap.getString("system_ui_statusbar_network_speed_icon", "2"));

                    String txarrow = "";
                    String rxarrow = "";
                    if (icons == 2) {
                        txarrow = txSpeed < lowLevel ? "△" : "▲";
                        rxarrow = rxSpeed < lowLevel ? "▽" : "▼";
                    } else if (icons == 3) {
                        txarrow = txSpeed < lowLevel ? " ☖" : " ☗";
                        rxarrow = rxSpeed < lowLevel ? " ⛉" : " ⛊";
                    }

                    String tx = hideLow && txSpeed < lowLevel ? "" : humanReadableByteCount(mContext, txSpeed) + txarrow;
                    String rx = hideLow && rxSpeed < lowLevel ? "" : humanReadableByteCount(mContext, rxSpeed) + rxarrow;
                    param.args[0] = tx + "\n" + rx;
                }
            });

            hookAllConstructors("com.android.systemui.statusbar.views.NetworkSpeedView", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
//                  值和单位双排显示 + 上下行网速双排显示 + 字体大小调整
                    boolean dualRow = mPrefsMap.getBoolean("system_ui_statusbar_network_speed_detailed")
                            || mPrefsMap.getBoolean("system_ui_statusbar_network_speed_fakedualrow");
                    TextView meter = (TextView) param.thisObject;
                    if (meter == null) return;
                    if (meter.getTag() == null || !"slot_text_icon".equals(meter.getTag())) {
                        int fontSize = mPrefsMap.getInt("system_ui_statusbar_network_speed_font_size", 13);
                        if (dualRow) {
                            if (fontSize > 23 || fontSize == 13) fontSize = 16;
                        } else {
                            if (fontSize < 20 && fontSize != 13) fontSize = 27;
                        }
                        if (dualRow || fontSize != 13) {
                            meter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.5f);
                        }
//                      网速加粗
                        if (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_bold")) {
                            meter.setTypeface(Typeface.DEFAULT_BOLD);
                        }

                        Resources res = meter.getResources();

//                      左侧间距
                        int leftMargin = mPrefsMap.getInt("system_ui_statusbar_network_speed_left_margin", 0);
                        leftMargin = (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                leftMargin * 0.5f,
                                res.getDisplayMetrics()
                        );
//                      右侧间距
                        int rightMargin = mPrefsMap.getInt("system_ui_statusbar_network_speed_right_margin", 0);
                        rightMargin = (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                rightMargin * 0.5f,
                                res.getDisplayMetrics()
                        );
//                      上下偏移量
                        int topMargin = 0;
                        int verticalOffset = mPrefsMap.getInt("system_ui_statusbar_network_speed_vertical_offset", 8);
                        if (verticalOffset != 8) {
                            float marginTop = TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP,
                                    (verticalOffset - 8) * 0.5f,
                                    res.getDisplayMetrics()
                            );
                            topMargin = (int) (marginTop);
                        }
                        meter.setPaddingRelative(leftMargin, topMargin, rightMargin, 0);

//                      水平对齐
                        int align = mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_align", 1);
                        switch (align) {
                            case 2:
                                meter.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                                break;
                            case 3:
                                meter.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                break;
                            case 4:
                                meter.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                                break;
                        }

                        if (dualRow) {
                            float spacing = 0.9f;
                            meter.setSingleLine(false);
                            meter.setMaxLines(2);
                            if (fontSize > 8.5f) {
                                spacing = 0.85f;
                            }
                            meter.setLineSpacing(0, spacing);
                        }
                    }
                }
            });
        }
    }
}
