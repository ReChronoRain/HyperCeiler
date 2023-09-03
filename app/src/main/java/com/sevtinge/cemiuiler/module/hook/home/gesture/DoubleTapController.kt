package com.sevtinge.cemiuiler.module.hook.home.gesture

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration

class DoubleTapController internal constructor(mContext: Context) {

    private val maxDuration: Long = 500
    private var mActionDownRawX: Float = 0f
    private var mActionDownRawY: Float = 0f
    private var mClickCount: Int = 0
    private var mFirstClickRawX: Float = 0f
    private var mFirstClickRawY: Float = 0f
    private var mLastClickTime: Long = 0
    private val mTouchSlop: Int = ViewConfiguration.get(mContext).scaledTouchSlop * 2

    fun isDoubleTapEvent(motionEvent: MotionEvent): Boolean {
        val action = motionEvent.actionMasked
        return when {
            action == MotionEvent.ACTION_DOWN -> {
                mActionDownRawX = motionEvent.rawX
                mActionDownRawY = motionEvent.rawY
                false
            }

            action != MotionEvent.ACTION_UP -> false
            else -> {
                val rawX = motionEvent.rawX
                val rawY = motionEvent.rawY
                if (kotlin.math.abs(rawX - mActionDownRawX) <= mTouchSlop.toFloat() && kotlin.math.abs(
                        rawY - mActionDownRawY
                    ) <= mTouchSlop.toFloat()
                ) {
                    if (SystemClock.elapsedRealtime() - mLastClickTime > maxDuration || rawY - mFirstClickRawY > mTouchSlop.toFloat() || rawX - mFirstClickRawX > mTouchSlop.toFloat()) mClickCount =
                        0
                    mClickCount++
                    if (mClickCount == 1) {
                        mFirstClickRawX = rawX
                        mFirstClickRawY = rawY
                        mLastClickTime = SystemClock.elapsedRealtime()
                        return false
                    } else if (kotlin.math.abs(rawY - mFirstClickRawY) <= mTouchSlop.toFloat() && kotlin.math.abs(
                            rawX - mFirstClickRawX
                        ) <= mTouchSlop.toFloat() && SystemClock.elapsedRealtime() - mLastClickTime <= maxDuration
                    ) {
                        mClickCount = 0
                        return true
                    }
                }
                mClickCount = 0
                false
            }
        }

    }
}
