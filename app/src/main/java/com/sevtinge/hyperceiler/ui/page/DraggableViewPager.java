package com.sevtinge.hyperceiler.ui.page;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DraggableViewPager extends androidx.viewpager.widget.ViewPager {

    private boolean mCanDrag = false;

    public DraggableViewPager(@NonNull Context context) {
        super(context);
    }

    public DraggableViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isDraggable() {
        return mCanDrag;
    }

    public void setDraggable(boolean isDrag) {
        mCanDrag = isDrag;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return mCanDrag ? super.onInterceptTouchEvent(ev) : false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return mCanDrag ? super.onInterceptTouchEvent(ev) : false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }
}
