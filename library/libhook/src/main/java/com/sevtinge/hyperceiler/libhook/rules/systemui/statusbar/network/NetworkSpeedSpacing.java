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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.network;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class NetworkSpeedSpacing extends BaseHook {

    private static final int MSG_UPDATE_NETWORK_SPEED = 200001;
    private static final int DEFAULT_INTERVAL = 40;
    private static final int MIN_INTERVAL = 10;
    private static final int MAX_INTERVAL = 100;
    private static final long ORIGINAL_INTERVAL = 4000L;

    // Values must not retain their weak keys through a Handler's this$0 field.
    private final WeakHashMap<Object, RefreshState> refreshStates = new WeakHashMap<>();
    private final ThreadLocal<Dispatch> dispatch = new ThreadLocal<>();
    private boolean warned;

    @Override
    public void init() {
        if (tryHookPostUpdateMethod()) return;

        try {
            Class<?> nscClass = findClass("com.android.systemui.statusbar.policy.NetworkSpeedController");
            if (nscClass != null) {
                for (Class<?> innerClass : nscClass.getDeclaredClasses()) {
                    if (android.os.Handler.class.isAssignableFrom(innerClass) && tryHookHandlerClass(innerClass.getName())) {
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        if (tryHookHandlerClass("com.android.systemui.statusbar.policy.NetworkSpeedController$5")) return;
        if (tryHookHandlerClass("com.android.systemui.statusbar.policy.NetworkSpeedController$4")) return;
        tryHookHandlerClass("com.android.systemui.statusbar.policy.NetworkSpeedController$2");
    }

    private boolean tryHookPostUpdateMethod() {
        try {
            findClass("com.android.systemui.statusbar.policy.NetworkSpeedController")
                .getDeclaredMethod("postUpdateNetworkSpeedDelay", long.class);

            findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController",
                "postUpdateNetworkSpeedDelay", long.class, new PostUpdateHook());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private boolean tryHookHandlerClass(String className) {
        try {
            findAndHookMethod(className, "handleMessage", Message.class, new HandleMessageHook());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private class PostUpdateHook implements IMethodHook {
        @Override
        public void before(HookParam param) {
            long interval = (long) param.getArgs()[0];
            if (interval == ORIGINAL_INTERVAL) {
                param.getArgs()[0] = getCustomInterval();
            }
        }
    }

    private class HandleMessageHook implements IMethodHook {
        @Override
        public void before(HookParam param) {
            try {
                if (!(param.getArgs()[0] instanceof Message message)) return;
                Object target = param.getThisObject();
                Object controller = getObjectField(target, "this$0");
                Object background = getObjectField(controller, "mBgHandler");
                Object control = getObjectField(controller, "mHandler");
                if (!(background instanceof Handler bg) || !(control instanceof Handler ui)
                    || bg == ui || bg.getClass() != ui.getClass()) return;
                // This fallback observes both dispatchers through the same merged
                // class. Different classes need a separate, verified hook plan.
                boolean periodic = target == bg && message.what == MSG_UPDATE_NETWORK_SPEED;
                boolean visibility = target == ui && isVisibilityMessage(message.what);
                if (!periodic && !visibility) return;

                beginDispatch(param.getArgs(), controller, bg, ui, periodic, visibility);
            } catch (Throwable t) {
                warnOnce(t);
            }
        }

        @Override
        public void after(HookParam param) {
            Dispatch current = dispatch.get();
            // EzHook creates separate HookParam wrappers around the same argument
            // array. A skipped before callback must not consume a nested caller.
            if (current == null || current.args != param.getArgs()) return;
            if (current.previous == null) dispatch.remove();
            else dispatch.set(current.previous);
            RefreshState state = current.state;
            synchronized (state) {
                if (!current.periodic) {
                    // Also invalidate BG work begun while this UI control ran.
                    state.visibilityGeneration++;
                    return;
                }
                state.running--;
                if (param.getHasThrowable() || param.isSkipped()) return;
                state.completedCycle = current.cycle;
                state.completedGeneration = current.generation;
                state.completedAt = SystemClock.uptimeMillis();
                if (state.posted) return;
                state.posted = true;
            }
            try {
                Handler ui = state.ui.get();
                if (ui != null && ui.post(() -> retimeOnControlHandler(state))) return;
            } catch (Throwable t) {
                warnOnce(t);
            }
            // A quitting/failed UI queue leaves the original system timer alone;
            // a later successful BG cycle may retry posting.
            synchronized (state) {
                state.posted = false;
            }
        }
    }

    private void beginDispatch(Object[] args, Object controller, Handler bg, Handler ui,
                               boolean periodic, boolean visibility) {
        RefreshState state;
        synchronized (refreshStates) {
            state = refreshStates.get(controller);
            if (state == null) {
                state = new RefreshState(controller, bg, ui);
                refreshStates.put(controller, state);
            }
        }
        synchronized (state) {
            if (visibility) state.visibilityGeneration++;
            Dispatch current = new Dispatch(args, state, periodic,
                state.visibilityGeneration, periodic ? ++state.startedCycle : 0, dispatch.get());
            dispatch.set(current);
            if (periodic) state.running++;
        }
    }

    private static boolean isVisibilityMessage(int what) {
        return what == 100001 || what == 100002 || what == 100003 || what == 100005;
    }

    private void retimeOnControlHandler(RefreshState state) {
        synchronized (state) {
            state.posted = false;
        }
        try {
            Handler ui = state.ui.get();
            Handler bg = state.bg.get();
            Object controller = state.controller.get();
            if (ui == null || bg == null || controller == null
                || Looper.myLooper() != ui.getLooper()) return;
            Object speedState = getObjectField(controller, "mNetworkSpeedState");
            boolean active = getBooleanField(speedState, "visible")
                && !getBooleanField(controller, "mIsStatusBarHidden");
            // Reflection and preference IPC stay outside the module state lock.
            // Visibility only changes on this UI Looper in the target ROM.
            long interval = active ? getCustomInterval() : 0L;
            retimeCompletedCycle(state, bg, active, interval);
        } catch (Throwable t) {
            warnOnce(t);
        }
    }

    private static void retimeCompletedCycle(RefreshState state, Handler bg, boolean active, long interval) {
        synchronized (state) {
            // Module state only: never acquire Handler/MessageQueue monitors.
            // Recheck after the unlocked reads: a running original BG callback
            // will publish its own completion. Holding this short lock through
            // queue submission prevents another BG before from entering then.
            if (state.running != 0 || state.completedCycle != state.startedCycle
                || state.handledCycle == state.completedCycle) return;
            state.handledCycle = state.completedCycle;
            if (!active) {
                // The ROM BG callback checks hidden but not visible, so it can
                // itself enqueue after a UI cancellation. Clean up on the UI
                // queue, where every ROM visibility cancellation is serialized.
                bg.removeMessages(MSG_UPDATE_NETWORK_SPEED);
                return;
            }
            if (state.completedGeneration != state.visibilityGeneration) return;
            if (!bg.hasMessages(MSG_UPDATE_NETWORK_SPEED)) return;
            long elapsed = Math.max(0L, SystemClock.uptimeMillis() - state.completedAt);
            bg.removeMessages(MSG_UPDATE_NETWORK_SPEED);
            bg.sendEmptyMessageDelayed(MSG_UPDATE_NETWORK_SPEED, Math.max(0L, interval - elapsed));
        }
    }

    private synchronized void warnOnce(Throwable t) {
        if (warned) return;
        warned = true;
        Log.w("HyperCeiler", "NetworkSpeedSpacing: retaining system scheduling after hook failure", t);
    }

    private static final class RefreshState {
        final WeakReference<Object> controller;
        final WeakReference<Handler> bg;
        final WeakReference<Handler> ui;
        long visibilityGeneration;
        long startedCycle;
        long completedCycle;
        long handledCycle;
        long completedGeneration;
        long completedAt;
        int running;
        boolean posted;

        RefreshState(Object controller, Handler bg, Handler ui) {
            this.controller = new WeakReference<>(controller);
            this.bg = new WeakReference<>(bg);
            this.ui = new WeakReference<>(ui);
        }
    }

    private static final class Dispatch {
        final Object[] args;
        final RefreshState state;
        final boolean periodic;
        final long generation;
        final long cycle;
        final Dispatch previous;

        Dispatch(Object[] args, RefreshState state, boolean periodic, long generation,
                 long cycle, Dispatch previous) {
            this.args = args;
            this.state = state;
            this.periodic = periodic;
            this.generation = generation;
            this.cycle = cycle;
            this.previous = previous;
        }
    }

    private long getCustomInterval() {
        int interval = PrefsBridge.getInt("system_ui_statusbar_network_speed_update_spacings", DEFAULT_INTERVAL);
        // The settings control permits 1.0–10.0 seconds. Corrupt/imported values
        // must not turn a SystemUI timer into a zero-delay or negative-delay loop.
        if (interval < MIN_INTERVAL || interval > MAX_INTERVAL) interval = DEFAULT_INTERVAL;
        return interval * 100L;
    }
}

