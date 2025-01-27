package com.sevtinge.hyperceiler.ui.app.main.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;

import fan.viewpager.widget.ViewPager;

public class DraggableViewPager extends ViewPager {

    private boolean mCanDrag = true;

    public DraggableViewPager(@NonNull Context context) {
        super(context);
    }

    public DraggableViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private void init(Context context) {
        try {
            Field field = ViewPager.class.getDeclaredField("mScroller");
            field.setAccessible(true);
            ViewPagerScroller scroller = new ViewPagerScroller(context, new DecelerateInterpolator());
            scroller.setDuration(350);
            field.set(this, scroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setDraggable(boolean isDrag) {
        mCanDrag = isDrag;
    }

    /*@Override
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
            return mCanDrag && super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }*/

    public class ViewPagerScroller extends Scroller {

        private int mDuration = 1000;

        public ViewPagerScroller(Context context) {
            super(context);
        }

        public ViewPagerScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        public ViewPagerScroller(Context context, Interpolator interpolator, boolean flywheel) {
            super(context, interpolator, flywheel);
        }

        public void setDuration(int duration) {
            mDuration = duration;
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, mDuration);
        }
    }
}
