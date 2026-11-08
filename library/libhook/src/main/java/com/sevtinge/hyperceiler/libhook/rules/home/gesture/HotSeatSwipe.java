/*
 * This file is part of HyperCeiler.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.sevtinge.hyperceiler.libhook.appbase.systemframework.GlobalActionBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/** Horizontal swipe actions on the launcher hotseat/dock (OS3). */
public class HotSeatSwipe extends BaseHook {
    private GestureDetector detector;

    @Override public void init() {
        if (isPad()) {
            findAndHookMethod("com.miui.home.launcher.dock.DockContainerView",
                "dispatchTouchEvent", MotionEvent.class, hook);
        } else {
            findAndHookMethod("com.miui.home.launcher.hotseats.HotSeats",
                "dispatchTouchEvent", MotionEvent.class, hook);
        }
    }

    private final IMethodHook hook = new IMethodHook() {
        @Override public void before(HookParam param) {
            MotionEvent event = (MotionEvent) param.getArgs()[0];
            if (event == null) return;
            ViewGroup dock = (ViewGroup) param.getThisObject();
            Context context = dock.getContext();
            if (context == null) return;
            if (detector == null) detector = new GestureDetector(context, new Listener(context));
            detector.onTouchEvent(event);
        }
    };

    private static final class Listener extends GestureDetector.SimpleOnGestureListener {
        private final Context context;
        private final int minDistance, minVelocity;
        Listener(Context context) {
            this.context = context;
            float density = context.getResources().getDisplayMetrics().density;
            minDistance = Math.round(75 * density);
            minVelocity = Math.round(33 * density);
        }
        @Override public boolean onDown(@NonNull MotionEvent event) { return false; }
        @Override public boolean onFling(MotionEvent start, @NonNull MotionEvent end,
                float velocityX, float velocityY) {
            if (start == null || Math.abs(velocityX) <= minVelocity) return false;
            if (end.getX() - start.getX() > minDistance)
                return GlobalActionBridge.handleAction(context, "home_gesture_dock_right_swipe");
            if (start.getX() - end.getX() > minDistance)
                return GlobalActionBridge.handleAction(context, "home_gesture_dock_left_swipe");
            return false;
        }
    }
}
