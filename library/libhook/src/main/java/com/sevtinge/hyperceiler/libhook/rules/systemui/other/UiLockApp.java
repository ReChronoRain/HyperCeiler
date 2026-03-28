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
package com.sevtinge.hyperceiler.libhook.rules.systemui.other;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/**
 * @author 焕晨HChen
 */
public class UiLockApp extends BaseHook {
    private static final String SETTING_KEY_LOCK_APP = "key_lock_app";
    private static final String[] STATUS_BAR_WINDOW_CONTROLLER_CLASS_CANDIDATES = new String[] {
        "com.android.systemui.statusbar.window.StatusBarWindowControllerImpl",
        "com.android.systemui.statusbar.window.StatusBarWindowController"
    };
    private static final String[] GESTURE_HANDLE_CLASS_CANDIDATES = new String[] {
        "com.android.systemui.navigationbar.gestural.NavigationHandle",
        "com.android.systemui.navigationbar.gestural.QuickswitchOrientedNavHandle",
        "com.android.systemui.navigationbar.views.NavigationHandle",
        "com.android.systemui.navigationbar.gestural.GestureHandleView",
        "com.android.systemui.navigationbar.gestural.HomeHandle"
    };
    private static final String[] NAVIGATION_BAR_CLASS_CANDIDATES = new String[] {
        "com.android.systemui.navigationbar.views.NavigationBar",
        "com.android.systemui.navigationbar.NavigationBar"
    };
    private static final String[] TASKBAR_DELEGATE_CLASS_CANDIDATES = new String[] {
        "com.android.systemui.navigationbar.TaskbarDelegate"
    };

    private boolean isObserverRegistered = false;
    private View mStatusBarView;
    private final List<WeakReference<View>> mGestureHandleViews = new ArrayList<>();
    private final List<WeakReference<Object>> mNavigationBars = new ArrayList<>();
    private final List<WeakReference<Object>> mTaskbarDelegates = new ArrayList<>();
    private final Map<View, Integer> mHandleVisibilityBackup = new WeakHashMap<>();
    private final Map<View, Float> mHandleAlphaBackup = new WeakHashMap<>();
    private Boolean mLastLockedState = null;

    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.SystemUIApplication",
            "onCreate",
            new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    try {
                        Context context = (Context) callMethod(param.getThisObject(), "getApplicationContext");
                        registerObserverIfNeeded(context);
                    } catch (Throwable e) {
                        XposedLog.w(TAG, "SystemUIApplication onCreate hook E: " + e);
                    }
                }
            }
        );

        for (String className : STATUS_BAR_WINDOW_CONTROLLER_CLASS_CANDIDATES) {
            hookStatusBarWindowControllerClass(className);
        }

        for (String className : GESTURE_HANDLE_CLASS_CANDIDATES) {
            hookGestureHandleClass(className);
        }
        for (String className : NAVIGATION_BAR_CLASS_CANDIDATES) {
            hookNavigationBarClass(className);
        }
        for (String className : TASKBAR_DELEGATE_CLASS_CANDIDATES) {
            hookTaskbarDelegateClass(className);
        }
    }

    private void updateStatusBarVisibility(Context context) {
        boolean isLocked = getLockApp(context) != -1;
        if (mLastLockedState == null || mLastLockedState != isLocked) {
            mLastLockedState = isLocked;
            XposedLog.d(TAG, "updateStatusBarVisibility locked=" + isLocked
                + " handles=" + mGestureHandleViews.size());
        }
        if (mStatusBarView != null) {
            mStatusBarView.setVisibility(isLocked ? View.GONE : View.VISIBLE);
        }
        updateGestureHandleVisibility(isLocked);
        if (!isLocked) {
            refreshNavigationBarPinningState();
            refreshTaskbarPinningState();
        }
    }

    private void hookStatusBarWindowControllerClass(String className) {
        Class<?> controllerClass = findClassIfExists(className);
        if (controllerClass == null) return;
        if (controllerClass.isInterface()) return;

        hookAllConstructors(controllerClass, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                try {
                    Context context = (Context) getObjectField(param.getThisObject(), "mContext");
                    if (context == null) return;

                    Object statusBarWindowView = getObjectField(param.getThisObject(), "mStatusBarWindowView");
                    if (statusBarWindowView instanceof FrameLayout) {
                        mStatusBarView = (FrameLayout) statusBarWindowView;
                    }
                    if (mStatusBarView == null) {
                        XposedLog.d(TAG, "mStatusBarView is null, keep observer fallback");
                    }

                    registerObserverIfNeeded(context);
                    updateStatusBarVisibility(context);
                } catch (Throwable e) {
                    XposedLog.w(TAG, "StatusBarWindowController hook E: " + e);
                }
            }
        });
    }

    private void hookGestureHandleClass(String className) {
        Class<?> gestureHandleClass = findClassIfExists(className);
        if (gestureHandleClass == null) return;

        hookAllConstructors(gestureHandleClass, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                if (!(param.getThisObject() instanceof View)) return;
                View handleView = (View) param.getThisObject();
                registerGestureHandleView(handleView);
            }
        });

        hookAllMethods(gestureHandleClass, "setVisibility", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (!(param.getThisObject() instanceof View)) return;
                View handleView = (View) param.getThisObject();
                Context context = handleView.getContext();
                if (context == null) return;
                if (getLockApp(context) != -1) {
                    param.getArgs()[0] = View.GONE;
                }
            }
        });

        hookAllMethods(gestureHandleClass, "setAlpha", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (!(param.getThisObject() instanceof View)) return;
                View handleView = (View) param.getThisObject();
                Context context = handleView.getContext();
                if (context == null) return;
                if (getLockApp(context) != -1) {
                    param.getArgs()[0] = 0f;
                }
            }
        });

        hookAllMethods(gestureHandleClass, "onDraw", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (!(param.getThisObject() instanceof View)) return;
                View handleView = (View) param.getThisObject();
                Context context = handleView.getContext();
                if (context == null) return;
                if (getLockApp(context) != -1) {
                    param.setResult(null);
                }
            }
        });
    }

    private void registerGestureHandleView(View view) {
        for (WeakReference<View> reference : mGestureHandleViews) {
            View exist = reference.get();
            if (exist == view) return;
        }
        mGestureHandleViews.add(new WeakReference<>(view));

        Context context = view.getContext();
        if (context == null) return;
        registerObserverIfNeeded(context);
        updateGestureHandleVisibility(getLockApp(context) != -1);
    }

    private void updateGestureHandleVisibility(boolean isLocked) {
        Iterator<WeakReference<View>> iterator = mGestureHandleViews.iterator();
        while (iterator.hasNext()) {
            View handleView = iterator.next().get();
            if (handleView == null) {
                iterator.remove();
                continue;
            }
            if (isLocked) {
                if (!mHandleVisibilityBackup.containsKey(handleView)) {
                    mHandleVisibilityBackup.put(handleView, handleView.getVisibility());
                }
                if (!mHandleAlphaBackup.containsKey(handleView)) {
                    mHandleAlphaBackup.put(handleView, handleView.getAlpha());
                }
                if (handleView.getVisibility() != View.GONE) {
                    handleView.setVisibility(View.GONE);
                }
                if (handleView.getAlpha() != 0f) {
                    handleView.setAlpha(0f);
                }
            } else {
                Float oldAlpha = mHandleAlphaBackup.remove(handleView);
                if (oldAlpha != null && handleView.getAlpha() != oldAlpha) {
                    handleView.setAlpha(oldAlpha);
                }
                Integer oldVisibility = mHandleVisibilityBackup.remove(handleView);
                if (oldVisibility != null && handleView.getVisibility() != oldVisibility) {
                    handleView.setVisibility(oldVisibility);
                }
            }
        }
    }

    private void hookNavigationBarClass(String className) {
        Class<?> navigationBarClass = findClassIfExists(className);
        if (navigationBarClass == null) return;

        hookAllConstructors(navigationBarClass, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                registerNavigationBar(param.getThisObject());
            }
        });

        hookAllMethods(navigationBarClass, "updateScreenPinningGestures", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                registerNavigationBar(param.getThisObject());
            }
        });
    }

    private void registerNavigationBar(Object navigationBar) {
        if (navigationBar == null) return;
        for (WeakReference<Object> reference : mNavigationBars) {
            Object exist = reference.get();
            if (exist == navigationBar) return;
        }
        mNavigationBars.add(new WeakReference<>(navigationBar));
    }

    private void refreshNavigationBarPinningState() {
        Iterator<WeakReference<Object>> iterator = mNavigationBars.iterator();
        while (iterator.hasNext()) {
            Object navigationBar = iterator.next().get();
            if (navigationBar == null) {
                iterator.remove();
                continue;
            }
            try {
                setObjectField(navigationBar, "mScreenPinningActive", false);
                clearScreenPinningSysUiFlag(navigationBar);
                Object navView = getObjectField(navigationBar, "mView");
                if (navView != null) {
                callMethod(navView, "setInScreenPinning", false);
                }
                callMethod(navigationBar, "updateScreenPinningGestures");
                callMethod(navigationBar, "updateSystemUiStateFlags");
                XposedLog.d(TAG, "refreshNavigationBarPinningState applied");
            } catch (Throwable ignored) {
            }
        }
    }

    private void clearScreenPinningSysUiFlag(Object navigationBar) {
        if (navigationBar == null) return;
        try {
            Object sysUiState = getObjectField(navigationBar, "mSysUiFlagsContainer");
            if (sysUiState == null) return;
            Object chain = callMethod(sysUiState, "setFlag", 1L, false);
            if (chain != null) {
                callMethod(chain, "commitUpdate");
            } else {
                callMethod(sysUiState, "commitUpdate");
            }
        } catch (Throwable ignored) {
        }
    }

    private void hookTaskbarDelegateClass(String className) {
        Class<?> taskbarDelegateClass = findClassIfExists(className);
        if (taskbarDelegateClass == null) return;

        hookAllConstructors(taskbarDelegateClass, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                registerTaskbarDelegate(param.getThisObject());
            }
        });

        hookAllMethods(taskbarDelegateClass, "setWindowState", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                registerTaskbarDelegate(param.getThisObject());
            }
        });
    }

    private void registerTaskbarDelegate(Object taskbarDelegate) {
        if (taskbarDelegate == null) return;
        for (WeakReference<Object> reference : mTaskbarDelegates) {
            Object exist = reference.get();
            if (exist == taskbarDelegate) return;
        }
        mTaskbarDelegates.add(new WeakReference<>(taskbarDelegate));
    }

    private void refreshTaskbarPinningState() {
        Iterator<WeakReference<Object>> iterator = mTaskbarDelegates.iterator();
        while (iterator.hasNext()) {
            Object taskbarDelegate = iterator.next().get();
            if (taskbarDelegate == null) {
                iterator.remove();
                continue;
            }
            try {
                Object sysUiState = getObjectField(taskbarDelegate, "mSysUiState");
                if (sysUiState == null) continue;
                Object chain = callMethod(sysUiState, "setFlag", 1L, false);
                if (chain != null) {
                    callMethod(chain, "commitUpdate");
                } else {
                    callMethod(sysUiState, "commitUpdate");
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public static int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), SETTING_KEY_LOCK_APP);
        } catch (Settings.SettingNotFoundException e) {
            XposedLog.w("LockApp", "getInt hyceiler_lock_app e: " + e);
        }
        return -1;
    }

    private void registerObserverIfNeeded(Context context) {
        if (context == null || isObserverRegistered) return;
        ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                XposedLog.d(TAG, "observer onChange key_lock_app");
                updateStatusBarVisibility(context);
            }
        };
        context.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(SETTING_KEY_LOCK_APP),
            false,
            contentObserver
        );
        isObserverRegistered = true;
        XposedLog.d(TAG, "observer registered");
    }
}
