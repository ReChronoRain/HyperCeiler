package com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class AddSideBarExpandReceiver extends BaseHook {
    @Override
    public void init() {
        final boolean[] isHooked = {false, false};
        boolean enableSideBar = mPrefsMap.getBoolean("security_center_leave_open");
        if (!enableSideBar) {
            mResHook.setDensityReplacement("com.miui.securitycenter", "dimen", "sidebar_height_default", 8);
            mResHook.setDensityReplacement("com.miui.securitycenter", "dimen", "sidebar_height_vertical", 8);
        }
        Class <?> RegionSamplingHelper = findClassIfExists("com.android.systemui.navigationbar.gestural.RegionSamplingHelper", lpparam.classLoader);
        if (RegionSamplingHelper == null) {
            logI("failed to find RegionSamplingHelper");
        }
        hookAllConstructors(RegionSamplingHelper, new MethodHook() {
            private int originDockLocation = -1;
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (!isHooked[0]) {
                    isHooked[0] = true;
                    View view = (View) param.args[0];
                    if (originDockLocation == -1) {
                        originDockLocation = view.getContext().getSharedPreferences("sp_video_box", 0).getInt("dock_line_location", 0);;
                    }
                    BroadcastReceiver showReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            Bundle bundle = intent.getBundleExtra("actionInfo");
                            int pos = originDockLocation;
                            if (bundle != null) {
                                pos = bundle.getInt("inDirection", 0);
                                view.getContext().getSharedPreferences("sp_video_box", 0).edit().putInt("dock_line_location", pos).commit();
                            }
                            showSideBar(view, pos);
                        }
                    };
                    view.getContext().registerReceiver(showReceiver, new IntentFilter(ACTION_PREFIX + "ShowSideBar"));
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "showReceiver", showReceiver);

                    if (!isHooked[1]) {
                        isHooked[1] = true;
                        Handler myhandler = new Handler(Looper.myLooper());
                        Runnable removeBg = new Runnable() {
                            @Override
                            public void run() {
                                myhandler.removeCallbacks(this);
                                if (!enableSideBar) {
                                    Object li = XposedHelpers.getObjectField(view, "mListenerInfo");
                                    Object mOnTouchListener = XposedHelpers.getObjectField(li, "mOnTouchListener");
                                    findAndHookMethod(mOnTouchListener.getClass(), "onTouch", View.class, MotionEvent.class, new MethodHook() {
                                        @Override
                                        protected void before(MethodHookParam param) throws Throwable {
                                            MotionEvent me = (MotionEvent) param.args[1];
                                            if (me.getSource() != 9999) {
                                                param.setResult(false);
                                            }
                                        }
                                    });
                                }
                                Class <?> bgDrawable = view.getBackground().getClass();
                                findAndHookMethod(bgDrawable, "draw", Canvas.class, new MethodHook() {
                                    @Override
                                    protected void before(MethodHookParam param) throws Throwable {
                                        param.setResult(null);
                                    }
                                });
                                view.setBackground(null);
                            }
                        };
                        myhandler.postDelayed(removeBg, 150);
                    }
                }
            }
        });
        findAndHookMethod(RegionSamplingHelper, "onViewDetachedFromWindow", android.view.View.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                isHooked[0] = false;
                BroadcastReceiver showReceiver = (BroadcastReceiver) XposedHelpers.getAdditionalInstanceField(param.thisObject, "showReceiver");
                if (showReceiver != null) {
                    View view = (View) param.args[0];
                    view.getContext().unregisterReceiver(showReceiver);
                    XposedHelpers.removeAdditionalInstanceField(param.thisObject, "showReceiver");
                }
            }
        });
        Method[] methods = XposedHelpers.findMethodsByExactParameters(RegionSamplingHelper, void.class, Rect.class);
        if (methods.length == 0) {
            logI("Cannot find appropriate start method");
            return;
        }
        hookMethod(methods[0], new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }

    private static void showSideBar(View view, int dockLocation) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int y = location[1];
        long uptimeMillis = SystemClock.uptimeMillis();
        MotionEvent downEvent, moveEvent, upEvent;
        if (dockLocation == 0) { // left
            downEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis, MotionEvent.ACTION_DOWN,  4, y + 15, 0);
            moveEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis + 20, MotionEvent.ACTION_MOVE, 160, y + 15, 0);
            upEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis + 21, MotionEvent.ACTION_UP, 160, y + 15, 0);
        }
        else {
            int x = location[0];
            downEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis, MotionEvent.ACTION_DOWN, x - 4, y + 15, 0);
            moveEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis + 20, MotionEvent.ACTION_MOVE, x - 160, y + 15, 0);
            upEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis + 21, MotionEvent.ACTION_UP, x - 160, y + 15, 0);
        }
        downEvent.setSource(9999);
        moveEvent.setSource(9999);
        upEvent.setSource(9999);
        view.dispatchTouchEvent(downEvent);
        view.dispatchTouchEvent(moveEvent);
        view.dispatchTouchEvent(upEvent);
        downEvent.recycle();
        moveEvent.recycle();
        upEvent.recycle();
    }
}
