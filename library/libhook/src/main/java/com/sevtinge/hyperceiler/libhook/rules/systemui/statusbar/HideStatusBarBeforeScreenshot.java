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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;
import android.view.SurfaceControl;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.core.java.Methods;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 截屏时隐藏状态栏。
 *
 * <p>SystemUI 通过 {@code miui.intent.TAKE_SCREENSHOT} 通知截屏流程，{@code IsFinished} 区分开始与
 * 结束：开始时隐藏状态栏，结束时恢复。</p>
 *
 * <p>隐藏状态必须整个进程共用一份，不能放在规则实例里：同一条规则可能被 {@code initHook}
 * 注册多次（每次注册都会各自挂一个 BroadcastReceiver），状态栏 view 也可能被重建，新旧 view
 * 的 receiver 会同时存活。旧实现把“隐藏前的可见性”存在实例字段中，后到的实例读到的已经是
 * 被隐藏后的 {@link View#INVISIBLE}，于是把它当成恢复值，收尾时又把状态栏设回隐藏，直到
 * SystemUI 重启才恢复。现在状态按 view 记录且进程内唯一，任何一次收尾都会恢复所有被本规则
 * 隐藏过的 view，重复注册与 view 重建都不会再互相覆盖状态。</p>
 */
public class HideStatusBarBeforeScreenshot extends BaseHook {

    private static final String LEGACY_COLLAPSED_STATUS_BAR_CLASS =
        "com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment";
    private static final String OS4_STATUS_BAR_VIEW_CLASS =
        "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView";
    private static final String ACTION_TAKE_SCREENSHOT = "miui.intent.TAKE_SCREENSHOT";
    private static final String EXTRA_IS_FINISHED = "IsFinished";
    private static final String HOT_RELOAD_VIEW_KEY =
        "HideStatusBarBeforeScreenshot.statusBarView";

    private static final Object STATE_LOCK = new Object();
    /** 被本规则隐藏过的状态栏 view（View 未重写 equals，天然按实例区分）及其恢复信息。 */
    private static final Map<View, HiddenStatusBar> HIDDEN_STATUS_BARS = new WeakHashMap<>();
    /** 已经注册过 receiver 的 view，避免同一个 view 被重复注册。 */
    private static final Set<View> REGISTERED_VIEWS =
        Collections.newSetFromMap(new WeakHashMap<View, Boolean>());
    private static volatile boolean sLastSurfaceApplySynchronous;
    private static volatile long sHideStartedAt;

    /** 单个被隐藏的状态栏 view，以及恢复它所需的信息。 */
    private static final class HiddenStatusBar {
        /** 隐藏前 view 的可见性，收尾时恢复成这个值。 */
        private final int visibilityBefore;
        /** 置为 0 alpha 的窗口 Surface，取不到时为 null。 */
        @Nullable
        private final SurfaceControl surface;

        HiddenStatusBar(int visibilityBefore, @Nullable SurfaceControl surface) {
            this.visibilityBefore = visibilityBefore;
            this.surface = surface;
        }
    }

    @Override
    public void init() {
        // 上一代注册过 receiver 的 view 直接复用，不必等下一次 onViewCreated。
        View restoredView = getHotReloadRuntimeState(HOT_RELOAD_VIEW_KEY, View.class);
        // 换代码前先把仍然藏着的状态栏放出来，避免热重载把界面留在隐藏态。
        registerHotReloadCleanup(this::releaseStatusBarForHotReload);
        if (restoredView != null) {
            registerScreenshotReceiver(restoredView);
        }

        if (Build.VERSION.SDK_INT >= 37) {
            XposedLog.d(TAG, getPackageName(),
                "HOOK_STATE=SELECTED target=" + OS4_STATUS_BAR_VIEW_CLASS
                    + "#onAttachedToWindow sdk=" + Build.VERSION.SDK_INT);
            hookAllMethods(OS4_STATUS_BAR_VIEW_CLASS, "onAttachedToWindow", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    Object thisObject = param.getThisObject();
                    if (thisObject instanceof View view) {
                        registerScreenshotReceiver(view);
                    }
                }
            });
        } else {
            XposedLog.d(TAG, getPackageName(),
                "HOOK_STATE=SELECTED target=" + LEGACY_COLLAPSED_STATUS_BAR_CLASS
                    + "#onViewCreated sdk=" + Build.VERSION.SDK_INT);
            hookAllMethods(LEGACY_COLLAPSED_STATUS_BAR_CLASS, "onViewCreated", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    View view = (View) param.getArgs()[0];
                    registerScreenshotReceiver(view);
                }
            });
        }
    }

    private void registerScreenshotReceiver(View view) {
        if (view == null) return;
        Context context = view.getContext();
        if (context == null) return;
        synchronized (STATE_LOCK) {
            if (!REGISTERED_VIEWS.add(view)) {
                XposedLog.d(TAG, getPackageName(),
                    "HOOK_STATE=SKIP_DUPLICATE_VIEW view=" + view.getClass().getName());
                return;
            }
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (!ACTION_TAKE_SCREENSHOT.equals(intent.getAction())) return;

                boolean finished = intent.getBooleanExtra(EXTRA_IS_FINISHED, true);
                if (finished) {
                    restoreHiddenStatusBars();
                } else {
                    hideStatusBar(view);
                }

                XposedLog.d(TAG, getPackageName(),
                    "HOOK_STATE=CALLBACK_HIT finished=" + finished
                        + " view=" + view.getClass().getName()
                        + " visibility=" + view.getVisibility()
                        + " hiddenViews=" + countHiddenStatusBars()
                        + " surfaceSync=" + sLastSurfaceApplySynchronous
                        + " elapsedMs=" + (finished && sHideStartedAt > 0
                            ? SystemClock.uptimeMillis() - sHideStartedAt : 0));
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_TAKE_SCREENSHOT);
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED);
        putHotReloadRuntimeState(HOT_RELOAD_VIEW_KEY, view);
        XposedLog.d(TAG, getPackageName(),
            "HOOK_STATE=INSTALLED view=" + view.getClass().getName());
        registerReceiverHotReloadCleanup(context, receiver);
    }

    /**
     * 隐藏状态栏。
     *
     * <p>“隐藏前的可见性”只在第一次隐藏该 view 时记录：同一次截屏里重复收到开始广播，或者
     * 同一条规则被注册多次而由另一个 receiver 先隐藏过，都不会把 {@link View#INVISIBLE} 覆盖成
     * 恢复值。</p>
     */
    private void hideStatusBar(View view) {
        synchronized (STATE_LOCK) {
            if (HIDDEN_STATUS_BARS.containsKey(view)) {
                view.setVisibility(View.INVISIBLE);
                return;
            }
            if (HIDDEN_STATUS_BARS.isEmpty()) {
                sHideStartedAt = SystemClock.uptimeMillis();
            }
            int visibilityBefore = view.getVisibility();
            SurfaceControl surface = findStatusBarSurface(view);
            if (!applySurfaceAlpha(surface, 0.0f)) {
                surface = null;
            }
            HIDDEN_STATUS_BARS.put(view, new HiddenStatusBar(visibilityBefore, surface));
            view.setVisibility(View.INVISIBLE);
            XposedLog.d(TAG, getPackageName(),
                "HOOK_STATE=HIDE view=" + view.getClass().getName()
                    + " visibilityBefore=" + visibilityBefore
                    + " surface=" + (surface != null));
        }
    }

    /**
     * 恢复所有被本规则隐藏过的状态栏 view。
     *
     * <p>幂等：任何一次收尾（正常结束、热重载、重复的 receiver 回调）都会清空整份记录，因此
     * 先执行的一方负责恢复，后执行的一方不会再把状态栏设回隐藏。</p>
     */
    private void restoreHiddenStatusBars() {
        List<Map.Entry<View, HiddenStatusBar>> entries;
        synchronized (STATE_LOCK) {
            if (HIDDEN_STATUS_BARS.isEmpty()) return;
            entries = new ArrayList<>(HIDDEN_STATUS_BARS.entrySet());
            HIDDEN_STATUS_BARS.clear();
            sHideStartedAt = 0L;
        }
        for (Map.Entry<View, HiddenStatusBar> entry : entries) {
            View view = entry.getKey();
            HiddenStatusBar hidden = entry.getValue();
            view.setVisibility(hidden.visibilityBefore);
            if (hidden.surface != null) {
                // 窗口 Surface 在截屏期间被重建时，旧引用已经失效，按当前 view 重新解析。
                SurfaceControl surface = hidden.surface.isValid()
                    ? hidden.surface
                    : findStatusBarSurface(view);
                applySurfaceAlpha(surface, 1.0f);
            }
            XposedLog.d(TAG, getPackageName(),
                "HOOK_STATE=RESTORE view=" + view.getClass().getName()
                    + " visibility=" + hidden.visibilityBefore);
        }
    }

    private int countHiddenStatusBars() {
        synchronized (STATE_LOCK) {
            return HIDDEN_STATUS_BARS.size();
        }
    }

    private void releaseStatusBarForHotReload() {
        restoreHiddenStatusBars();
        synchronized (STATE_LOCK) {
            REGISTERED_VIEWS.clear();
        }
    }

    @Nullable
    private SurfaceControl findStatusBarSurface(View view) {
        try {
            Object viewRoot = Methods.callMethod(view, "getViewRootImpl");
            Object surfaceObject = Methods.callMethod(viewRoot, "getSurfaceControl");
            if (surfaceObject instanceof SurfaceControl surface && surface.isValid()) {
                return surface;
            }
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "HOOK_STATE=SURFACE_LOOKUP_FAILED", t);
        }
        return null;
    }

    private boolean applySurfaceAlpha(@Nullable SurfaceControl surface, float alpha) {
        if (surface == null || !surface.isValid()) {
            sLastSurfaceApplySynchronous = false;
            return false;
        }
        try {
            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            try {
                transaction.setAlpha(surface, alpha);
                sLastSurfaceApplySynchronous = applySurfaceTransaction(transaction);
            } finally {
                transaction.close();
            }
            return true;
        } catch (Throwable t) {
            sLastSurfaceApplySynchronous = false;
            XposedLog.w(TAG, getPackageName(),
                "HOOK_STATE=SURFACE_APPLY_FAILED alpha=" + alpha, t);
            return false;
        }
    }

    private boolean applySurfaceTransaction(SurfaceControl.Transaction transaction) {
        try {
            Method applySynchronously = SurfaceControl.Transaction.class
                .getDeclaredMethod("apply", boolean.class);
            applySynchronously.setAccessible(true);
            applySynchronously.invoke(transaction, true);
            return true;
        } catch (Throwable ignored) {
            transaction.apply();
            return false;
        }
    }
}
