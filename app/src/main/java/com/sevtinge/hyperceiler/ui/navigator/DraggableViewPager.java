package com.sevtinge.hyperceiler.ui.navigator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fan.viewpager.widget.ViewPager;

public class DraggableViewPager extends ViewPager {

    private boolean mCanDrag = true;

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
            return mCanDrag && super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return mCanDrag && super.onTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }
}
