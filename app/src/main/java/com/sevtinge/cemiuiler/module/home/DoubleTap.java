package com.sevtinge.cemiuiler.module.home;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.sevtinge.cemiuiler.module.GlobalActions;
import com.sevtinge.cemiuiler.module.base.BaseHook;

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
                mDoubleTapControllerEx = new DoubleTapController((Context)param.args[0], "prefs_key_home_gesture_double_tap");
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mDoubleTapControllerEx", mDoubleTapControllerEx);
            }
        });

        findAndHookMethod(mWorkspace, "dispatchTouchEvent", MotionEvent.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                DoubleTapController mDoubleTapControllerEx = (DoubleTapController)XposedHelpers.getAdditionalInstanceField(param.thisObject, "mDoubleTapControllerEx");
                if (mDoubleTapControllerEx == null) return;
                if (!mDoubleTapControllerEx.isDoubleTapEvent((MotionEvent)param.args[0])) return;
                int mCurrentScreenIndex = XposedHelpers.getIntField(param.thisObject, lpparam.packageName.equals("com.miui.home") ? "mCurrentScreenIndex" : "mCurrentScreen");
                Object cellLayout = XposedHelpers.callMethod(param.thisObject, "getCellLayout", mCurrentScreenIndex);
                if ((boolean)XposedHelpers.callMethod(cellLayout, "lastDownOnOccupiedCell")) return;
                if ((boolean)XposedHelpers.callMethod(param.thisObject, "isInNormalEditingMode")) return;
                mDoubleTapControllerEx.onDoubleTapEvent();
            }
        });
    }


    public class DoubleTapController {

        private final long MAX_DURATION = 500;
        private float mActionDownRawX;
        private float mActionDownRawY;
        private int mClickCount;
        public final Context mContext;
        private final String mActionKey;
        private float mFirstClickRawX;
        private float mFirstClickRawY;
        private long mLastClickTime;
        private int mTouchSlop;

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
                    if (SystemClock.elapsedRealtime() - this.mLastClickTime > MAX_DURATION || rawY - this.mFirstClickRawY > (float)this.mTouchSlop || rawX - this.mFirstClickRawX > (float)this.mTouchSlop) {
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
