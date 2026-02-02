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
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.moduleload.GlobalActions;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class HotSeatSwipe extends BaseHook {

    private GestureDetector mDetectorHorizontal;

    @Override
    public void init() {
        if (isPad()) {
            findAndHookMethod("com.miui.home.launcher.dock.DockContainerView", "dispatchTouchEvent", MotionEvent.class, hook);
        } else {
            findAndHookMethod("com.miui.home.launcher.hotseats.HotSeats", "dispatchTouchEvent", MotionEvent.class, hook);
        }
    }

    IMethodHook hook = new IMethodHook() {
        @Override
        public void before(final BeforeHookParam param) {
            MotionEvent ev = (MotionEvent) param.getArgs()[0];
            if (ev == null) return;

            ViewGroup hotSeat = (ViewGroup) param.getThisObject();
            Context helperContext = hotSeat.getContext();
            if (helperContext == null) return;
            if (mDetectorHorizontal == null)
                mDetectorHorizontal = new GestureDetector(helperContext, new SwipeListenerHorizontal(hotSeat));
            mDetectorHorizontal.onTouchEvent(ev);
        }
    };


    private static class SwipeListenerHorizontal extends GestureDetector.SimpleOnGestureListener {

        private final int SWIPE_MIN_DISTANCE_HORIZ;
        private final int SWIPE_THRESHOLD_VELOCITY;

        final Context helperContext;

        SwipeListenerHorizontal(Object cellLayout) {
            helperContext = ((ViewGroup) cellLayout).getContext();
            float density = helperContext.getResources().getDisplayMetrics().density;
            SWIPE_MIN_DISTANCE_HORIZ = Math.round(75 * density);
            SWIPE_THRESHOLD_VELOCITY = Math.round(33 * density);
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;

            if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE_HORIZ && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                return GlobalActions.handleAction(helperContext, "home_gesture_right_swipe");

            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE_HORIZ && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                return GlobalActions.handleAction(helperContext, "home_gesture_left_swipe");

            return false;
        }
    }
}
