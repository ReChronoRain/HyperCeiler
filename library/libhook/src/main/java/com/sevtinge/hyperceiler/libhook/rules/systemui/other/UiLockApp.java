/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterface;

/**
 * @author 焕晨HChen
 * @co-author LingQiqi & Codex(GPT-5.3-Codex)
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

    private boolean mObserverRegistered = false;
    private View mStatusBarView;
    private Boolean mLastLockedState = null;

    private final List<WeakReference<View>> mGestureHandleViews = new ArrayList<>();
    private final List<WeakReference<Object>> mNavigationBars = new ArrayList<>();
    private final List<WeakReference<Object>> mTaskbarDelegates = new ArrayList<>();
    private final Map<View, Integer> mHandleVisibilityBackup = new WeakHashMap<>();
    private final Map<View, Float> mHandleAlphaBackup = new WeakHashMap<>();

    @Override
    public void init() {
        findAndChainMethod("com.android.systemui.SystemUIApplication",
            "onCreate",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    try {
                        Context context = (Context) callMethod(chain.getThisObject(), "getApplicationContext");
                        registerObserverIfNeeded(context);
                    } catch (Throwable e) {
                        XposedLog.w(TAG, "SystemUIApplication onCreate hook E: " + e);
                    }
                    return result;
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
        hookScreenPinningDialogBlock();
    }

    private void hookStatusBarWindowControllerClass(String className) {
        Class<?> controllerClass = findClassIfExists(className);
        if (controllerClass == null || controllerClass.isInterface()) return;

        chainAllConstructors(controllerClass, new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Object result = chain.proceed();
                try {
                    Context context = (Context) getObjectField(chain.getThisObject(), "mContext");
                    if (context == null) return result;

                    Object statusBarWindowView = getObjectField(chain.getThisObject(), "mStatusBarWindowView");
                    if (statusBarWindowView instanceof FrameLayout) {
                        mStatusBarView = (FrameLayout) statusBarWindowView;
                    }
                    registerObserverIfNeeded(context);
                    updateStatusBarVisibility(context);
                } catch (Throwable e) {
                    XposedLog.w(TAG, "StatusBarWindowController hook E: " + e);
                }
                return result;
            }
        });
    }

    private void hookGestureHandleClass(String className) {
        Class<?> gestureHandleClass = findClassIfExists(className);
        if (gestureHandleClass == null) return;

        chainAllConstructors(gestureHandleClass, new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Object result = chain.proceed();
                if (chain.getThisObject() instanceof View) {
                    View handleView = (View) chain.getThisObject();
                    registerGestureHandleView(handleView);
                }
                return result;
            }
        });

        chainAllMethods(gestureHandleClass, "setVisibility", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                if (!(chain.getThisObject() instanceof View)) return chain.proceed();
                View handleView = (View) chain.getThisObject();
                Context context = handleView.getContext();
                if (context == null || getLockApp(context) == -1) return chain.proceed();
                Object[] args = chain.getArgs().toArray();
                args[0] = View.GONE;
                return chain.proceed(args);
            }
        });

        chainAllMethods(gestureHandleClass, "setAlpha", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                if (!(chain.getThisObject() instanceof View)) return chain.proceed();
                View handleView = (View) chain.getThisObject();
                Context context = handleView.getContext();
                if (context == null || getLockApp(context) == -1) return chain.proceed();
                Object[] args = chain.getArgs().toArray();
                args[0] = 0f;
                return chain.proceed(args);
            }
        });

        chainAllMethods(gestureHandleClass, "onDraw", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                if (!(chain.getThisObject() instanceof View)) return chain.proceed();
                View handleView = (View) chain.getThisObject();
                Context context = handleView.getContext();
                if (context == null || getLockApp(context) == -1) return chain.proceed();
                return null;
            }
        });
    }

    private void registerGestureHandleView(View view) {
        for (WeakReference<View> reference : mGestureHandleViews) {
            if (reference.get() == view) return;
        }
        mGestureHandleViews.add(new WeakReference<>(view));
        Context context = view.getContext();
        if (context == null) return;
        registerObserverIfNeeded(context);
        updateGestureHandleVisibility(getLockApp(context) != -1);
    }

    private void hookNavigationBarClass(String className) {
        Class<?> navigationBarClass = findClassIfExists(className);
        if (navigationBarClass == null) return;

        chainAllConstructors(navigationBarClass, new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Object result = chain.proceed();
                registerNavigationBar(chain.getThisObject());
                return result;
            }
        });

        chainAllMethods(navigationBarClass, "updateScreenPinningGestures", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                registerNavigationBar(chain.getThisObject());
                return chain.proceed();
            }
        });
    }

    private void registerNavigationBar(Object navigationBar) {
        if (navigationBar == null) return;
        for (WeakReference<Object> reference : mNavigationBars) {
            if (reference.get() == navigationBar) return;
        }
        mNavigationBars.add(new WeakReference<>(navigationBar));
    }

    private void hookTaskbarDelegateClass(String className) {
        Class<?> taskbarDelegateClass = findClassIfExists(className);
        if (taskbarDelegateClass == null) return;

        chainAllConstructors(taskbarDelegateClass, new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Object result = chain.proceed();
                registerTaskbarDelegate(chain.getThisObject());
                return result;
            }
        });
    }

    private void registerTaskbarDelegate(Object taskbarDelegate) {
        if (taskbarDelegate == null) return;
        for (WeakReference<Object> reference : mTaskbarDelegates) {
            if (reference.get() == taskbarDelegate) return;
        }
        mTaskbarDelegates.add(new WeakReference<>(taskbarDelegate));
        Context context = resolveContext(taskbarDelegate);
        if (context != null) {
            registerObserverIfNeeded(context);
        }
    }

    private void hookScreenPinningDialogBlock() {
        Class<?> centralSurfacesCallbacksClass = findClassIfExists(
            "com.android.systemui.statusbar.phone.CentralSurfacesCommandQueueCallbacks");
        if (centralSurfacesCallbacksClass != null) {
            chainAllMethods(centralSurfacesCallbacksClass, "showScreenPinningRequest", new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    int taskId = readIntArg(chain.getArgs().toArray(), 0, -1);
                    XposedLog.d(TAG, "trace CentralSurfaces.showScreenPinningRequest taskId=" + taskId);
                    startSystemLockTaskModeByTaskId(chain.getThisObject(), taskId);
                    return null;
                }
            });
        }
    }

    private void startSystemLockTaskModeByTaskId(Object callbacks, int taskId) {
        if (taskId < 0) return;
        Context context = resolveContext(callbacks);
        if (context != null && getLockApp(context) != -1) {
            return;
        }
        try {
            Class<?> activityTaskManagerClass = findClassIfExists("android.app.ActivityTaskManager");
            if (activityTaskManagerClass == null) return;
            Object service = callStaticMethod(activityTaskManagerClass, "getService");
            callMethod(service, "startSystemLockTaskMode", taskId);
            XposedLog.d(TAG, "trace startSystemLockTaskMode taskId=" + taskId);
        } catch (Throwable e) {
            XposedLog.w(TAG, "startSystemLockTaskMode from dialog hook E: " + e);
        }
    }

    private void updateStatusBarVisibility(Context context) {
        boolean isLocked = getLockApp(context) != -1;
        boolean stateChanged = mLastLockedState == null || mLastLockedState != isLocked;
        if (stateChanged) {
            mLastLockedState = isLocked;
            XposedLog.d(TAG, "lockState locked=" + isLocked);
        }

        if (mStatusBarView != null) {
            mStatusBarView.setVisibility(isLocked ? View.GONE : View.VISIBLE);
        }

        updateGestureHandleVisibility(isLocked);

        if (!isLocked) {
            refreshNavigationBarPinningState();
            refreshTaskbarPinningState();
            refreshNavigationTransientState();
            refreshTaskbarTransientState();
        }
        if (stateChanged && !isLocked) {
            refreshStatusBarDisableFlags(context);
        }
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

    private void refreshNavigationTransientState() {
        Iterator<WeakReference<Object>> iterator = mNavigationBars.iterator();
        while (iterator.hasNext()) {
            Object navigationBar = iterator.next().get();
            if (navigationBar == null) {
                iterator.remove();
                continue;
            }
            try {
                setObjectField(navigationBar, "mTransientShown", false);
            } catch (Throwable ignored) {
            }
            try {
                setObjectField(navigationBar, "mTransientShownFromGestureOnSystemBar", false);
            } catch (Throwable ignored) {
            }
            try {
                Object edgeBack = getObjectField(navigationBar, "mEdgeBackGestureHandler");
                if (edgeBack != null) {
                    setObjectField(edgeBack, "mIsNavBarShownTransiently", false);
                }
            } catch (Throwable ignored) {
            }
            try {
                callMethod(navigationBar, "updateSystemUiStateFlags");
            } catch (Throwable ignored) {
            }
        }
    }

    private void refreshTaskbarTransientState() {
        Iterator<WeakReference<Object>> iterator = mTaskbarDelegates.iterator();
        while (iterator.hasNext()) {
            Object taskbarDelegate = iterator.next().get();
            if (taskbarDelegate == null) {
                iterator.remove();
                continue;
            }
            try {
                setObjectField(taskbarDelegate, "mTaskbarTransientShowing", false);
            } catch (Throwable ignored) {
            }
            try {
                Object edgeBack = getObjectField(taskbarDelegate, "mEdgeBackGestureHandler");
                if (edgeBack != null) {
                    setObjectField(edgeBack, "mIsNavBarShownTransiently", false);
                }
            } catch (Throwable ignored) {
            }
            try {
                callMethod(taskbarDelegate, "updateSysuiFlags");
            } catch (Throwable ignored) {
            }
        }
    }

    private void refreshStatusBarDisableFlags(Context context) {
        if (context == null) return;
        try {
            Class<?> dependencyClass = findClassIfExists("com.android.systemui.Dependency");
            Class<?> commandQueueClass = findClassIfExists("com.android.systemui.statusbar.CommandQueue");
            if (dependencyClass == null || commandQueueClass == null) return;

            Object commandQueue = null;
            try {
                Object dependency = getStaticObjectField(dependencyClass, "sDependency");
                if (dependency != null) {
                    commandQueue = callMethod(dependency, "getDependencyInner", commandQueueClass);
                }
            } catch (Throwable ignored) {
            }
            if (commandQueue == null) {
                try {
                    commandQueue = callStaticMethod(dependencyClass, "get", commandQueueClass);
                } catch (Throwable ignored) {
                }
            }
            if (commandQueue == null) return;

            int displayId = resolveDisplayId(context);
            callMethod(commandQueue, "recomputeDisableFlags", displayId, true);
        } catch (Throwable ignored) {
        }
    }

    private int resolveDisplayId(Context context) {
        if (context == null) return 0;
        try {
            Object display = callMethod(context, "getDisplay");
            if (display != null) {
                Object displayId = callMethod(display, "getDisplayId");
                if (displayId instanceof Integer) {
                    return (Integer) displayId;
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private Context resolveContext(Object target) {
        if (target instanceof Context) return (Context) target;
        if (target instanceof View) return ((View) target).getContext();
        if (target == null) return null;
        try {
            Object context = getObjectField(target, "mContext");
            if (context instanceof Context) return (Context) context;
        } catch (Throwable ignored) {
        }
        try {
            Object context = getObjectField(target, "context");
            if (context instanceof Context) return (Context) context;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private int readIntArg(Object[] args, int index, int defValue) {
        if (args == null || index < 0 || index >= args.length) return defValue;
        Object value = args[index];
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Boolean) return (Boolean) value ? 1 : 0;
        return defValue;
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
        if (context == null || mObserverRegistered) return;
        ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updateStatusBarVisibility(context);
            }
        };
        context.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(SETTING_KEY_LOCK_APP),
            false,
            contentObserver
        );
        mObserverRegistered = true;
    }
}
