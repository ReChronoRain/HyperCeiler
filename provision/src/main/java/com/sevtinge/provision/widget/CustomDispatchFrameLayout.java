package com.sevtinge.provision.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.provision.utils.ProvisionAnimHelper;

public class CustomDispatchFrameLayout extends FrameLayout {

    protected ProvisionAnimHelper mProvisionAnimHelper;

    public CustomDispatchFrameLayout(@NonNull Context context) {
        super(context);
    }

    public CustomDispatchFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomDispatchFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomDispatchFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isAnimEnded()) {
            return super.dispatchTouchEvent(ev);
        }
        Log.w("OobeUtil2", "anim not end, skip touch event");
        return true;
    }

    protected boolean isAnimEnded() {
        return mProvisionAnimHelper != null ? mProvisionAnimHelper.isAnimEnded() : true;
    }

}
