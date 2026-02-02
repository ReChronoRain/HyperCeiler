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

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.moduleload.GlobalActions;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class DoubleTap extends BaseHook {

    Class<?> mWorkspace;

    @Override
    public void init() {
        mWorkspace = findClassIfExists("com.miui.home.launcher.Workspace");

        hookAllConstructors(mWorkspace, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                Object mDoubleTapControllerEx = EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), "mDoubleTapControllerEx");
                if (mDoubleTapControllerEx != null) return;
                mDoubleTapControllerEx = new DoubleTapController((Context) param.getArgs()[0], "home_gesture_double_tap");
                EzxHelpUtils.setAdditionalInstanceField(param.getThisObject(), "mDoubleTapControllerEx", mDoubleTapControllerEx);
            }
        });

        findAndHookMethod(mWorkspace, "dispatchTouchEvent", MotionEvent.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                DoubleTapController mDoubleTapControllerEx = (DoubleTapController) EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), "mDoubleTapControllerEx");
                if (mDoubleTapControllerEx == null) return;
                if (!mDoubleTapControllerEx.isDoubleTapEvent((MotionEvent) param.getArgs()[0])) return;
                int mCurrentScreenIndex = EzxHelpUtils.getIntField(param.getThisObject(), getPackageName().equals("com.miui.home") ? "mCurrentScreenIndex" : "mCurrentScreen");
                Object cellLayout = EzxHelpUtils.callMethod(param.getThisObject(), "getCellLayout", mCurrentScreenIndex);
                if ((boolean) EzxHelpUtils.callMethod(cellLayout, "lastDownOnOccupiedCell")) return;
                if ((boolean) EzxHelpUtils.callMethod(param.getThisObject(), "isInNormalEditingMode")) return;
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
