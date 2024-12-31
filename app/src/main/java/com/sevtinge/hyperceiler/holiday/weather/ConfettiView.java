package com.sevtinge.hyperceiler.holiday.weather;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.holiday.weather.confetto.Confetto;

import java.util.List;

import fan.core.utils.MiuixUIUtils;

/**
 * A helper temporary view that helps render the confetti. This view will attach itself to the
 * view root, perform the animation, and then once all of the confetti has completed its animation,
 * it will automatically remove itself from the parent.
 */
public class ConfettiView extends View implements View.OnLayoutChangeListener {
    private List<Confetto> confetti;
    private boolean terminated;

    private boolean touchEnabled;
    private Confetto draggedConfetto;

    public static ConfettiView newInstance(Context context) {
        final ConfettiView confettiView = new ConfettiView(context, null);
        confettiView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final int defaultElevation = MiuixUIUtils.dp2px(context, 999);
        confettiView.setElevation(defaultElevation);

        return confettiView;
    }

    public ConfettiView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the list of confetti to be animated by this view.
     *
     * @param confetti the list of confetti to be animated.
     */
    public void bind(List<Confetto> confetti) {
        this.confetti = confetti;
    }

    /**
     * @see ConfettiManager#setTouchEnabled(boolean)
     *
     * @param touchEnabled whether or not to enable touch
     */
    public void setTouchEnabled(boolean touchEnabled) {
        this.touchEnabled = touchEnabled;
    }

    /**
     * Terminate the current running animation (if any) and remove this view from the parent.
     */
    public void terminate() {
        if (!terminated) {
            this.terminated = true;
            getParent().requestLayout();
        }
    }

    /**
     * Reset the internal state of this view to allow for a new confetti animation.
     */
    public void reset() {
        this.terminated = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        final ViewGroup parent = (ViewGroup) getParent();
        parent.removeOnLayoutChangeListener(this);
        parent.addOnLayoutChangeListener(this);

        // If we did not bind before attaching to the window, that means this ConfettiView no longer
        // has a ConfettiManager backing it and should just be terminated.
        if (confetti == null) {
            terminate();
        }
    }

    @Override
    public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6,
                               int i7) {
        if (terminated) {
            final ViewParent parent = getParent();
            if (parent != null && parent instanceof ViewGroup) {
                final ViewGroup vg = (ViewGroup) parent;
                vg.removeViewInLayout(this);
                vg.removeOnLayoutChangeListener(this);
                vg.invalidate();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!terminated) {
            canvas.save();
            for (Confetto confetto : this.confetti) {
                confetto.draw(canvas);
            }
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        if (touchEnabled) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    for (Confetto confetto : confetti) {
                        if (confetto.onTouchDown(event)) {
                            draggedConfetto = confetto;
                            handled = true;
                            break;
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (draggedConfetto != null) {
                        draggedConfetto.onTouchMove(event);
                        handled = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (draggedConfetto != null) {
                        draggedConfetto.onTouchUp(event);
                        draggedConfetto = null;
                        handled = true;
                    }
                    break;
            }
        }

        return handled || super.onTouchEvent(event);
    }
}