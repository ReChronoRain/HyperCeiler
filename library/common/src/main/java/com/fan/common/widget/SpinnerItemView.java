package com.fan.common.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import androidx.annotation.Nullable;

import com.fan.common.R;

import fan.animation.Folme;
import fan.appcompat.widget.Spinner;

public class SpinnerItemView extends ItemView {

    private final Spinner mSpinner;
    private final CharSequence[] mEntries;

    public SpinnerItemView(Context context) {
        this(context, null);
    }

    public SpinnerItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpinnerItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.ItemViewStyle_DayNight);
    }

    public SpinnerItemView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SpinnerItemView, defStyleAttr, defStyleRes);
        mEntries = a.getTextArray(R.styleable.SpinnerItemView_android_entries);
        a.recycle();

        setArrowRightVisible(false);
        setWidgetFrameLayout(R.layout.item_view_widget_spinner);

        mSpinner = findViewById(R.id.spinner);
        if (mEntries != null && mEntries.length != 0) {
            ArrayAdapter<?> adapter = new ArrayAdapter<>(context, fan.appcompat.R.layout.miuix_appcompat_simple_spinner_integrated_layout, 0x01020014, mEntries);
            adapter.setDropDownViewResource(fan.appcompat.R.layout.miuix_appcompat_simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);
        }
        setSpinnerDisplayLocation(this, mSpinner);
    }


    public void setPrompt(CharSequence prompt) {
        mSpinner.setPrompt(prompt);
    }

    public CharSequence getPrompt() {
        return mSpinner.getPrompt();
    }

    public int getSelectedItemPosition() {
        return mSpinner.getSelectedItemPosition();
    }

    public void setAdapter(SpinnerAdapter adapter) {
        mSpinner.setAdapter(adapter);
    }

    public void setSelection(int position) {
        mSpinner.setSelection(position);
    }

    public Object getSelectedItem() {
        return mSpinner.getSelectedItem();
    }

    public Spinner getSpinner() {
        return mSpinner;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setSpinnerDisplayLocation(ViewGroup parent, Spinner spinner) {
        if (parent != null && spinner != null) {
            spinner.setClickable(false);
            spinner.setLongClickable(false);
            spinner.setContextClickable(false);
            spinner.setOnSpinnerDismissListener(() -> Folme.useAt(new View[]{parent}).touch().touchUp());
            parent.setOnTouchListener((v, event) -> {
                if (spinner.isEnabled()) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN -> Folme.useAt(new View[]{v}).touch().setScale(1.0f).touchDown();
                        case MotionEvent.ACTION_UP -> spinner.performClick(event.getX(), event.getY());
                        case MotionEvent.ACTION_CANCEL -> Folme.useAt(new View[]{v}).touch().touchUp();
                    }
                    return true;
                } else {
                    return false;
                }
            });
        }
    }
}
