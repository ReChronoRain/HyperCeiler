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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getPackageVersionCode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class AddSideBarExpandReceiver extends BaseHook {

    private boolean isNewVersion;
    private boolean enableSideBar;
    private final boolean[] isHooked = {false, false};
    private int originDockLocation = -1;

    @Override
    public void init() {
        try {
            initVersionCheck();
            initSideBarSettings();

            Class<?> regionSamplingHelper = findClassIfExists(
                "com.android.systemui.navigationbar.gestural.RegionSamplingHelper",
                getClassLoader());

            if (regionSamplingHelper == null) {
                XposedLog.w(TAG, getPackageName(), "failed to find RegionSamplingHelper");
                return;
            }

            hookRegionSamplingHelper(regionSamplingHelper);
        } catch (Throwable e) {
            XposedLog.e(TAG, getPackageName(), "init failed", e);
        }
    }

    /**
     * 初始化版本检查
     */
    private void initVersionCheck() {
        int versionCode = getPackageVersionCode(getLpparam());
        boolean isPad = isPad();
        isNewVersion = (versionCode >= 40001000 && !isPad) || (versionCode >= 40011000 && isPad);
    }

    /**
     * 初始化侧边栏设置
     */
    private void initSideBarSettings() {
        enableSideBar = PrefsBridge.getBoolean("security_center_leave_open");
        if (!enableSideBar) {
            setDensityReplacement("com.miui.securitycenter", "dimen", "sidebar_height_default", 8);
            setDensityReplacement("com.miui.securitycenter", "dimen", "sidebar_height_vertical", 8);
        }
    }

    /**
     * Hook RegionSamplingHelper 类
     */
    private void hookRegionSamplingHelper(Class<?> regionSamplingHelper) {
        hookAllConstructors(regionSamplingHelper, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                if (!isHooked[0]) {
                    isHooked[0] = true;
                    View view = (View) param.getArgs()[0];
                    handleConstructorAfter(view, param.getThisObject());
                }
            }
        });

        hookViewDetachedFromWindow(regionSamplingHelper);
        hookStartMethod(regionSamplingHelper);
    }

    /**
     * 处理构造函数后的逻辑
     */
    private void handleConstructorAfter(View view, Object regionSamplingHelper) {
        initOriginDockLocation(view);
        registerShowSideBarReceiver(view, regionSamplingHelper);

        if (!isHooked[1]) {
            isHooked[1] = true;
            scheduleRemoveBackground(view);
        }
    }

    /**
     * 初始化原始 Dock 位置
     */
    private void initOriginDockLocation(View view) {
        if (originDockLocation == -1) {
            originDockLocation = view.getContext()
                .getSharedPreferences("sp_video_box", 0)
                .getInt("dock_line_location", 0);
        }
    }

    /**
     * 注册显示侧边栏广播接收器
     */
    private void registerShowSideBarReceiver(View view, Object regionSamplingHelper) {
        BroadcastReceiver showReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleShowSideBarIntent(view, intent);
            }
        };ContextCompat.registerReceiver(view.getContext(), showReceiver,
            new IntentFilter(ACTION_PREFIX + "ShowSideBar"),
            ContextCompat.RECEIVER_NOT_EXPORTED);

        EzxHelpUtils.setAdditionalInstanceField(regionSamplingHelper, "showReceiver", showReceiver);
    }

    /**
     * 处理显示侧边栏 Intent
     */
    private void handleShowSideBarIntent(View view, Intent intent) {
        Bundle bundle = intent.getBundleExtra("actionInfo");
        int pos = originDockLocation;

        if (bundle != null) {
            pos = bundle.getInt("inDirection", 0);
            view.getContext()
                .getSharedPreferences("sp_video_box", 0)
                .edit()
                .putInt("dock_line_location", pos)
                .apply();
        }

        showSideBar(view, pos);
    }

    /**
     * 延迟移除背景
     */
    private void scheduleRemoveBackground(View view) {
        Handler handler = new Handler(Looper.myLooper());
        handler.postDelayed(() -> removeBackground(view), 150);
    }

    /**
     * 移除背景
     */
    private void removeBackground(View view) {
        if (!enableSideBar) {
            removeOnTouchListener(view);
        }

        if (isNewVersion) {
            removeDrawableNewVersion(view);
        } else {
            removeDrawableOldVersion(view);
        }
    }

    /**
     * 移除 OnTouchListener（旧版本）
     */
    private void removeOnTouchListener(View view) {
        try {
            Object listenerInfo = EzxHelpUtils.getObjectField(view, "mListenerInfo");
            if (listenerInfo == null) {
                return;
            }

            Object onTouchListener = EzxHelpUtils.getObjectField(listenerInfo, "mOnTouchListener");
            if (onTouchListener == null) {
                return;
            }

            findAndHookMethod(onTouchListener.getClass(), "onTouch",
                View.class, MotionEvent.class, new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {MotionEvent motionEvent = (MotionEvent) param.getArgs()[1];
                        if (motionEvent.getSource() != 9999) {
                            param.setResult(false);
                        }
                    }
                });
        } catch (Throwable e) {
            XposedLog.e(TAG, getPackageName(), "removeOnTouchListener failed", e);
        }
    }

    /**
     * 移除 Drawable（新版本 10.0.0+）
     */
    private void removeDrawableNewVersion(View view) {
        try {
            Object drawable = EzxHelpUtils.callMethod(view, "getDrawable");
            if (drawable == null) {
                return;
            }

            Class<?> drawableClass = drawable.getClass();
            findAndHookMethod(drawableClass, "draw", Canvas.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(null);
                }
            });

            EzxHelpUtils.callMethod(view, "setImageDrawable", (Drawable) null);
        } catch (Throwable e) {
            XposedLog.e(TAG, getPackageName(), "removeDrawableNewVersion failed", e);
        }
    }

    /**
     * 移除 Drawable（旧版本）
     */
    private void removeDrawableOldVersion(View view) {
        try {
            Drawable background = view.getBackground();
            if (background == null) {
                return;
            }

            Class<?> drawableClass = background.getClass();
            findAndHookMethod(drawableClass, "draw", Canvas.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(null);
                }
            });

            view.setBackground(null);
        } catch (Throwable e) {
            XposedLog.e(TAG, getPackageName(), "removeDrawableOldVersion failed", e);
        }
    }

    /**
     * Hook View 从窗口分离事件
     */
    private void hookViewDetachedFromWindow(Class<?> regionSamplingHelper) {
        findAndHookMethod(regionSamplingHelper, "onViewDetachedFromWindow",
            View.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    isHooked[0] = false;

                    BroadcastReceiver showReceiver = (BroadcastReceiver)
                        EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), "showReceiver");

                    if (showReceiver != null) {
                        View view = (View) param.getArgs()[0];
                        view.getContext().unregisterReceiver(showReceiver);
                        EzxHelpUtils.removeAdditionalInstanceField(param.getThisObject(), "showReceiver");
                    }
                }
            });
    }

    /**
     * Hook 启动方法
     */
    private void hookStartMethod(Class<?> regionSamplingHelper) {
        Method[] methods = EzxHelpUtils.findMethodsByExactParameters(
            regionSamplingHelper, void.class, Rect.class);

        if (methods.length == 0) {
            XposedLog.e(TAG, getPackageName(), "Cannot find appropriate start method");
            return;
        }

        hookMethod(methods[0], new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(null);
            }
        });
    }

    /**
     * 显示侧边栏 - 模拟触摸事件
     */
    private static void showSideBar(View view, int dockLocation) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int y = location[1];
        long uptimeMillis = SystemClock.uptimeMillis();MotionEvent downEvent, moveEvent, upEvent;

        if (dockLocation == 0) { // 左侧
            downEvent = createMotionEvent(uptimeMillis, MotionEvent.ACTION_DOWN, 4, y + 15);
            moveEvent = createMotionEvent(uptimeMillis + 20, MotionEvent.ACTION_MOVE, 160, y + 15);
            upEvent = createMotionEvent(uptimeMillis + 21, MotionEvent.ACTION_UP, 160, y + 15);
        } else { // 右侧
            int x = location[0];
            downEvent = createMotionEvent(uptimeMillis, MotionEvent.ACTION_DOWN, x - 4, y + 15);
            moveEvent = createMotionEvent(uptimeMillis + 20, MotionEvent.ACTION_MOVE, x - 160, y + 15);
            upEvent = createMotionEvent(uptimeMillis + 21, MotionEvent.ACTION_UP, x - 160, y + 15);
        }

        dispatchTouchEvents(view, downEvent, moveEvent, upEvent);
    }

    /**
     * 创建 MotionEvent
     */
    private static MotionEvent createMotionEvent(long time, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(time, time, action, x, y, 0);
        event.setSource(9999);
        return event;
    }

    /**
     * 分发触摸事件
     */
    private static void dispatchTouchEvents(View view, MotionEvent downEvent,
                                            MotionEvent moveEvent, MotionEvent upEvent) {
        try {
            view.dispatchTouchEvent(downEvent);
            view.dispatchTouchEvent(moveEvent);
            view.dispatchTouchEvent(upEvent);
        } finally {
            downEvent.recycle();
            moveEvent.recycle();
            upEvent.recycle();
        }
    }
}

