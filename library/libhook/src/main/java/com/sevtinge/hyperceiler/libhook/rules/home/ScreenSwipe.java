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
package com.sevtinge.hyperceiler.libhook.rules.home;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.moduleload.GlobalActions;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefType;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsChangeObserver;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class ScreenSwipe extends BaseHook {

    private static final String PREF_DOWN_SWIPE = "home_gesture_down_swipe";
    private static final String PREF_DOWN_SWIPE_2 = "home_gesture_down_swipe2";
    private static final String PREF_UP_SWIPE = "home_gesture_up_swipe";
    private static final String PREF_UP_SWIPE_2 = "home_gesture_up_swipe2";
    private static final String PREF_DOWN_SWIPE_ACTION = "home_gesture_down_swipe_action";
    private static final String PREF_UP_SWIPE_ACTION = "home_gesture_up_swipe_action";

    private static final int GESTURE_UP = 10;
    private static final int GESTURE_DOWN = 11;

    @Override
    public void init() {
        hookVerticalGesture();
        hookLauncherOnCreate();
        hookStatusBarSwipe();
        hookPullDownGesture();
        hookSlideUpGesture();
        hookGlobalSearch();
    }

    private void hookVerticalGesture() {
        findAndHookMethod("com.miui.home.launcher.Workspace","onVerticalGesture", int.class, MotionEvent.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if ((boolean) callMethod(param.getThisObject(), "isInNormalEditingMode")) {
                        return;
                    }

                    int gestureType = (int) param.getArgs()[0];
                    MotionEvent event = (MotionEvent) param.getArgs()[1];
                    int fingerCount = event != null ? event.getPointerCount() : 1;
                    Context context = ((ViewGroup) param.getThisObject()).getContext();

                    String key = getGestureKey(gestureType, fingerCount);
                    if (key != null && GlobalActions.handleAction(context, key)) {
                        param.setResult(true);
                    }
                }
            });
    }

    private String getGestureKey(int gestureType, int fingerCount) {
        if (gestureType == GESTURE_DOWN) {
            return fingerCount == 2 ? PREF_DOWN_SWIPE_2 : PREF_DOWN_SWIPE;
        } else if (gestureType == GESTURE_UP) {
            return fingerCount == 2 ? PREF_UP_SWIPE_2 : PREF_UP_SWIPE;
        }
        return null;
    }

    private void hookLauncherOnCreate() {
        findAndHookMethod("com.miui.home.launcher.Launcher",
            "onCreate", Bundle.class,
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    Activity activity = (Activity) param.getThisObject();
                    Handler handler = (Handler) getObjectField(activity, "mHandler");
                    new PrefsChangeObserver(activity, handler) {
                        @Override
                        public void onChange(PrefType type, Uri uri, String name, Object def) {
                            if (!name.contains(PREF_DOWN_SWIPE)) return;

                            try {
                                updatePrefsMap(activity, name, type);
                            } catch (Throwable t) {
                                AndroidLog.d(TAG, "setAction", t);
                            }
                        }
                    };
                }
            });
    }

    private void updatePrefsMap(Activity activity, String name, PrefType type) {
        Object value = switch (type) {
            case String -> mPrefsMap.getString(name, "");
            case Integer -> mPrefsMap.getInt(name, 1);
            case Boolean -> mPrefsMap.getBoolean(name, false);
            default -> null;
        };
        if (value != null) {
            mPrefsMap.put(name, value);
        }
    }

    private void hookStatusBarSwipe() {
        findAndHookMethod("com.miui.home.launcher.uioverrides.StatusBarSwipeController",
            "canInterceptTouch", MotionEvent.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (mPrefsMap.getInt("home_gesture_down_swipe_action", 0) > 0) {
                        param.setResult(false);
                    }
                }
            });
    }

    private void hookPullDownGesture() {
        findAndHookMethod("com.miui.home.launcher.allapps.LauncherMode",
            "getPullDownGesture", Context.class,
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    Context context = (Context) param.getArgs()[0];
                    if (mPrefsMap.getInt(PREF_DOWN_SWIPE_ACTION, 1) > 1) {
                        param.setResult("no_action");
                    }
                }
            });
    }

    private void hookSlideUpGesture() {
        findAndHookMethod("com.miui.home.launcher.allapps.LauncherMode",
            "getSlideUpGesture", Context.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    Context context = (Context) param.getArgs()[0];
                    if (mPrefsMap.getInt(PREF_UP_SWIPE_ACTION, 0) > 0) {
                        param.setResult("no_action");
                    }
                }
            });
    }

    private void hookGlobalSearch() {
        boolean hooked = tryHookGlobalSearchEnable();

        if (hooked) {
            hookSearchEdgeLayout();
            hookGlobalSearchBottomEffect();
        } else {
            boolean fallbackHooked = tryHookAllowedSlidingUp();
            if (!fallbackHooked && "com.miui.home".equals(getPackageName())) {
                XposedLog.i(TAG, getPackageName(), "Cannot disable swipe up search");
            }
        }
    }

    private boolean tryHookGlobalSearchEnable() {
        try {
            findAndHookMethod("com.miui.home.launcher.DeviceConfig",
                "isGlobalSearchEnable", Context.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        Context context = (Context) param.getArgs()[0];
                        if (mPrefsMap.getInt(PREF_UP_SWIPE_ACTION, 0) > 0) {
                            param.setResult(false);
                        }
                    }
                });
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void hookSearchEdgeLayout() {
        findAndHookMethod("com.miui.home.launcher.search.SearchEdgeLayout",
            "isTopSearchEnable",
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    View view = (View) param.getThisObject();
                    if (mPrefsMap.getInt(PREF_DOWN_SWIPE_ACTION, 0) > 0) {
                        param.setResult(false);
                    }
                }
            });

        findAndHookMethod("com.miui.home.launcher.search.SearchEdgeLayout",
            "isBottomGlobalSearchEnable",
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    View view = (View) param.getThisObject();
                    if (mPrefsMap.getInt(PREF_UP_SWIPE_ACTION, 0) > 0) {
                        param.setResult(false);
                    }
                }
            });
    }

    private void hookGlobalSearchBottomEffect() {
        findAndHookMethod("com.miui.home.launcher.DeviceConfig",
            "isGlobalSearchBottomEffectEnable", Context.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    Context context = (Context) param.getArgs()[0];
                    if (mPrefsMap.getInt(PREF_UP_SWIPE_ACTION, 0) > 0) {
                        param.setResult(false);
                    }
                }
            });
    }

    private boolean tryHookAllowedSlidingUp() {
        try {
            findAndHookMethod("com.miui.home.launcher.DeviceConfig",
                "allowedSlidingUpToStartGolbalSearch", Context.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        Context context = (Context) param.getArgs()[0];
                        if (mPrefsMap.getInt(PREF_UP_SWIPE_ACTION, 0) > 0) {
                            param.setResult(false);
                        }
                    }
                });
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}

