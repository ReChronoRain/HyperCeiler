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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.home.gesture;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.hook.GlobalActions;

import de.robv.android.xposed.XposedHelpers;

public class DoubleTap extends BaseHook {

    Class<?> mWorkspace;

    @Override
    public void init() {
        mWorkspace = findClassIfExists("com.miui.home.launcher.Workspace");

        hookAllConstructors(mWorkspace, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Object mDoubleTapControllerEx = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mDoubleTapControllerEx");
                if (mDoubleTapControllerEx != null) return;
                mDoubleTapControllerEx = new DoubleTapController((Context) param.args[0], "prefs_key_home_gesture_double_tap");
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mDoubleTapControllerEx", mDoubleTapControllerEx);
            }
        });

        findAndHookMethod(mWorkspace, "dispatchTouchEvent", MotionEvent.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                DoubleTapController mDoubleTapControllerEx = (DoubleTapController) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mDoubleTapControllerEx");
                if (mDoubleTapControllerEx == null) return;
                if (!mDoubleTapControllerEx.isDoubleTapEvent((MotionEvent) param.args[0])) return;
                int mCurrentScreenIndex = XposedHelpers.getIntField(param.thisObject, lpparam.packageName.equals("com.miui.home") ? "mCurrentScreenIndex" : "mCurrentScreen");
                Object cellLayout = XposedHelpers.callMethod(param.thisObject, "getCellLayout", mCurrentScreenIndex);
                if ((boolean) XposedHelpers.callMethod(cellLayout, "lastDownOnOccupiedCell")) return;
                if ((boolean) XposedHelpers.callMethod(param.thisObject, "isInNormalEditingMode")) return;
                mDoubleTapControllerEx.onDoubleTapEvent();
            }
        });
    }


    public static class DoubleTapController {

        private float mActionDownRawX;
        private float mActionDownRawY;
        private int mClickCount;
        public final Context mContext;
        private final String mActionKey;
        private float mFirstClickRawX;
        private float mFirstClickRawY;
        private long mLastClickTime;
        private final int mTouchSlop;

        DoubleTapController(Context context, String actionKey) {
            this.mContext = context;
            this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop() * 2;
            this.mActionKey = actionKey;
        }

        boolean isDoubleTapEvent(MotionEvent motionEvent) {
            int action = motionEvent.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                this.mActionDownRawX = motionEvent.getRawX();
                this.mActionDownRawY = motionEvent.getRawY();
                return false;
            } else if (action != MotionEvent.ACTION_UP) {
                return false;
            } else {
                float rawX = motionEvent.getRawX();
                float rawY = motionEvent.getRawY();
                if (Math.abs(rawX - this.mActionDownRawX) <= ((float) this.mTouchSlop) && Math.abs(rawY - this.mActionDownRawY) <= ((float) this.mTouchSlop)) {
                    long MAX_DURATION = 500;
                    if (SystemClock.elapsedRealtime() - this.mLastClickTime > MAX_DURATION || rawY - this.mFirstClickRawY > (float) this.mTouchSlop || rawX - this.mFirstClickRawX > (float) this.mTouchSlop) {
                        this.mClickCount = 0;
                    }
                    this.mClickCount++;
                    if (this.mClickCount == 1) {
                        this.mFirstClickRawX = rawX;
                        this.mFirstClickRawY = rawY;
                        this.mLastClickTime = SystemClock.elapsedRealtime();
                        return false;
                    } else if (Math.abs(rawY - this.mFirstClickRawY) <= ((float) this.mTouchSlop) && Math.abs(rawX - this.mFirstClickRawX) <= ((float) this.mTouchSlop) && SystemClock.elapsedRealtime() - this.mLastClickTime <= MAX_DURATION) {
                        this.mClickCount = 0;
                        return true;
                    }
                }
                this.mClickCount = 0;
                return false;
            }
        }

        void onDoubleTapEvent() {
            GlobalActions.handleAction(mContext, mActionKey);
        }
    }
}
