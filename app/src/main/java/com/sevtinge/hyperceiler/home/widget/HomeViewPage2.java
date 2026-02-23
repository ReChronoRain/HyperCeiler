package com.sevtinge.hyperceiler.home.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fan.viewpager2.widget.ViewPager2;

public class HomeViewPage2 extends ViewPager2 {

    public HomeViewPage2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean onInterceptTouchEvent;
        try {
            onInterceptTouchEvent = super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException ex) {
            onInterceptTouchEvent = false;
        }
        return !onInterceptTouchEvent && super.onInterceptTouchEvent(ev);
    }
}
