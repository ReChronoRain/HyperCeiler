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

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

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
    private static final String[] WINDOW_DECORATION_CLASS_CANDIDATES = new String[] {
        "com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.MiuiBaseWindowDecoration"
    };
    private static final String[] DOT_VIEW_CLASS_CANDIDATES = new String[] {
        "com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.decoration.MiuiDecorationDotView",
        "com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.MiuiDotView",
        "com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.MiuiDecorationRootView"
    };
    private static final String[] DECORATION_DOT_CLASS_CANDIDATES = new String[] {
        "com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.decoration.MiuiDecorationDot"
    };

    private boolean mObserverRegistered = false;
    private View mStatusBarView;
    private Boolean mLastLockedState = null;

    private final List<WeakReference<View>> mGestureHandleViews = new ArrayList<>();
    private final List<WeakReference<Object>> mNavigationBars = new ArrayList<>();
    private final List<WeakReference<Object>> mTaskbarDelegates = new ArrayList<>();
    private final List<WeakReference<Object>> mWindowDecorations = new ArrayList<>();
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
        for (String className : WINDOW_DECORATION_CLASS_CANDIDATES) {
            hookWindowDecorationClass(className);
        }
        for (String className : DOT_VIEW_CLASS_CANDIDATES) {
            hookDotViewClass(className);
        }
        for (String className : DECORATION_DOT_CLASS_CANDIDATES) {
            hookDecorationDotClass(className);
        }
        if (isPad()) {
            hookLauncherProxyStopScreenPinning();
        }
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
                if (chain.getThisObject() instanceof View handleView) {
                    registerGestureHandleView(handleView);
                }
                return result;
            }
        });

        chainAllMethods(gestureHandleClass, "setVisibility", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                if (!(chain.getThisObject() instanceof View handleView)) return chain.proceed();
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
                if (!(chain.getThisObject() instanceof View handleView)) return chain.proceed();
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
                if (!(chain.getThisObject() instanceof View handleView)) return chain.proceed();
                Context context = handleView.getContext();
                if (context == null || getLockApp(context) == -1) return chain.proceed();
                return null;
            }
        });
    }

    private void registerGestureHandleView(View view) {
        if (view == null) return;
        for (WeakReference<View> reference : mGestureHandleViews) {
            if (view.equals(reference.get())) return;
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
            if (navigationBar.equals(reference.get())) return;
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
            if (taskbarDelegate.equals(reference.get())) return;
        }
        mTaskbarDelegates.add(new WeakReference<>(taskbarDelegate));
        Context context = resolveContext(taskbarDelegate);
        if (context != null) {
            registerObserverIfNeeded(context);
        }
    }

    private void hookLauncherProxyStopScreenPinning() {
        Class<?> launcherProxyClass = findClassIfExists("com.android.systemui.recents.LauncherProxyService$1");
        if (launcherProxyClass == null) return;

        chainAllMethods(launcherProxyClass, "verifyCallerAndClearCallingIdentityPostMain", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Object[] args = chain.getArgs().toArray();
                if (args.length > 0 && "stopScreenPinning".equals(args[0])) {
                    Context context = resolveContext(chain.getThisObject());
                    if (context != null && getLockApp(context) != -1) {
                        XposedLog.d(TAG, "block LauncherProxyService.stopScreenPinning while locked");
                        return null;
                    }
                }
                return chain.proceed();
            }
        });
    }

    private void hookWindowDecorationClass(String className) {
        Class<?> decorClass = findClassIfExists(className);
        if (decorClass == null) return;

        hookWindowDecorationConstructor(decorClass);
        hookWindowDecorationShouldHideCaption(decorClass);
        hookWindowDecorationRelayout(decorClass);
        hookWindowDecorationUpdateVisibility(decorClass);
        hookWindowDecorationPointInView(decorClass);
    }

    private void hookWindowDecorationConstructor(Class<?> decorClass) {
        chainAllConstructors(decorClass, new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Object result = chain.proceed();
                registerWindowDecoration(chain.getThisObject());
                return result;
            }
        });
    }

    private void hookWindowDecorationShouldHideCaption(Class<?> decorClass) {
        chainAllMethods(decorClass, "shouldHideCaption", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Context context = (Context) getObjectField(chain.getThisObject(), "mContext");
                if (context != null && getLockApp(context) != -1) {
                    return true;
                }
                return chain.proceed();
            }
        });
    }

    private void hookWindowDecorationRelayout(Class<?> decorClass) {
        chainAllMethods(decorClass, "relayout", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Context context = (Context) getObjectField(chain.getThisObject(), "mContext");
                if (context != null && getLockApp(context) != -1) {
                    setObjectField(chain.getThisObject(), "mCaptionVisible", false);
                }
                return chain.proceed();
            }
        });
    }

    private void hookWindowDecorationUpdateVisibility(Class<?> decorClass) {
        chainAllMethods(decorClass, "updateVisibility", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                if (!chain.getArgs().isEmpty() && chain.getArgs().get(0) instanceof Boolean) {
                    Context context = (Context) getObjectField(chain.getThisObject(), "mContext");
                    if (context != null && getLockApp(context) != -1) {
                        Object[] args = chain.getArgs().toArray();
                        args[0] = false;
                        return chain.proceed(args);
                    }
                }
                return chain.proceed();
            }
        });
    }

    private void hookWindowDecorationPointInView(Class<?> decorClass) {
        chainAllMethods(decorClass, "pointInView", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Context context = (Context) getObjectField(chain.getThisObject(), "mContext");
                if (context != null && getLockApp(context) != -1) {
                    return false;
                }
                return chain.proceed();
            }
        });
    }

    private void registerWindowDecoration(Object decor) {
        if (decor == null) return;
        for (WeakReference<Object> reference : mWindowDecorations) {
            if (decor.equals(reference.get())) return;
        }
        mWindowDecorations.add(new WeakReference<>(decor));
    }

    private void hookDotViewClass(String className) {
        Class<?> dotViewClass = findClassIfExists(className);
        if (dotViewClass == null) return;

        hookDotViewVisibility(dotViewClass);
        hookDotViewOnAttached(dotViewClass);
        hookDotViewOnDraw(dotViewClass);
    }

    private void hookDotViewVisibility(Class<?> dotViewClass) {
        chainAllMethods(dotViewClass, "setVisibility", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                View view = (View) chain.getThisObject();
                if (getLockApp(view.getContext()) != -1) {
                    Object[] args = chain.getArgs().toArray();
                    args[0] = View.GONE;
                    return chain.proceed(args);
                }
                return chain.proceed();
            }
        });
    }

    private void hookDotViewOnAttached(Class<?> dotViewClass) {
        chainAllMethods(dotViewClass, "onAttachedToWindow", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                View view = (View) chain.getThisObject();
                if (getLockApp(view.getContext()) != -1) {
                    view.setVisibility(View.GONE);
                }
                return chain.proceed();
            }
        });
    }

    private void hookDotViewOnDraw(Class<?> dotViewClass) {
        chainAllMethods(dotViewClass, "onDraw", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                View view = (View) chain.getThisObject();
                if (getLockApp(view.getContext()) != -1) {
                    return null;
                }
                return chain.proceed();
            }
        });
    }

    private void hookDecorationDotClass(String className) {
        Class<?> dotClass = findClassIfExists(className);
        if (dotClass == null) return;

        chainAllMethods(dotClass, "createHandleMenu", new XposedInterface.Hooker() {
            @Override
            public Object intercept(XposedInterface.Chain chain) throws Throwable {
                Context context = resolveContext(chain.getThisObject());
                if (context != null && getLockApp(context) != -1) return null;
                return chain.proceed();
            }
        });
    }

    private void updateStatusBarVisibility(Context context) {
        boolean isLocked = getLockApp(context) != -1;
        boolean stateChanged = mLastLockedState == null || mLastLockedState != isLocked;
        if (stateChanged) {
            mLastLockedState = isLocked;
        }

        if (mStatusBarView != null) {
            mStatusBarView.setVisibility(isLocked ? View.GONE : View.VISIBLE);
        }

        updateGestureHandleVisibility(isLocked);
        updateWindowDecorVisibility(isLocked);

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
                applyLockedGestureHandle(handleView);
            } else {
                restoreGestureHandle(handleView);
            }
        }
    }

    private void applyLockedGestureHandle(View handleView) {
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
    }

    private void restoreGestureHandle(View handleView) {
        Float oldAlpha = mHandleAlphaBackup.remove(handleView);
        if (oldAlpha != null && handleView.getAlpha() != oldAlpha) {
            handleView.setAlpha(oldAlpha);
        }
        Integer oldVisibility = mHandleVisibilityBackup.remove(handleView);
        if (oldVisibility != null && handleView.getVisibility() != oldVisibility) {
            handleView.setVisibility(oldVisibility);
        }
    }

    private void updateWindowDecorVisibility(boolean isLocked) {
        Iterator<WeakReference<Object>> iterator = mWindowDecorations.iterator();
        while (iterator.hasNext()) {
            Object decor = iterator.next().get();
            if (decor == null) {
                iterator.remove();
                continue;
            }
            try {
                callMethod(decor, "setCaptionVisibility", !isLocked);
            } catch (Throwable e) {
                XposedLog.w(TAG, "updateWindowDecorVisibility error: " + e);
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
            resetNavigationFields(navigationBar);
            try {
                callMethod(navigationBar, "updateSystemUiStateFlags");
            } catch (Throwable ignored) {
            }
        }
    }

    private void resetNavigationFields(Object navigationBar) {
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
    }

    private void refreshTaskbarTransientState() {
        Iterator<WeakReference<Object>> iterator = mTaskbarDelegates.iterator();
        while (iterator.hasNext()) {
            Object taskbarDelegate = iterator.next().get();
            if (taskbarDelegate == null) {
                iterator.remove();
                continue;
            }
            resetTaskbarFields(taskbarDelegate);
            try {
                callMethod(taskbarDelegate, "updateSysuiFlags");
            } catch (Throwable ignored) {
            }
        }
    }

    private void resetTaskbarFields(Object taskbarDelegate) {
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
    }

    private void refreshStatusBarDisableFlags(Context context) {
        if (context == null) return;
        try {
            Class<?> dependencyClass = findClassIfExists("com.android.systemui.Dependency");
            Class<?> commandQueueClass = findClassIfExists("com.android.systemui.statusbar.CommandQueue");
            if (dependencyClass == null || commandQueueClass == null) return;

            Object commandQueue = getCommandQueue(dependencyClass, commandQueueClass);
            if (commandQueue == null) return;

            int displayId = resolveDisplayId(context);
            callMethod(commandQueue, "recomputeDisableFlags", displayId, true);
        } catch (Throwable ignored) {
        }
    }

    private Object getCommandQueue(Class<?> dependencyClass, Class<?> commandQueueClass) {
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
        return commandQueue;
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
        Context context = tryGetContextField(target, "mContext");
        if (context != null) return context;
        context = tryGetContextField(target, "context");
        if (context != null) return context;
        try {
            Object outer = getObjectField(target, "this$0");
            if (outer != null && outer != target) {
                return resolveContext(outer);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Context tryGetContextField(Object target, String fieldName) {
        try {
            Object context = getObjectField(target, fieldName);
            if (context instanceof Context) return (Context) context;
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), SETTING_KEY_LOCK_APP);
        } catch (Settings.SettingNotFoundException e) {
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
