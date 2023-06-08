package com.sevtinge.cemiuiler.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sevtinge.cemiuiler.R;

import java.util.IllegalFormatException;

import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceViewHolder;

public class SeekBarPreferenceEx extends Preference {

    private int mDefaultValue;
    private int mMinValue;
    private int mMaxValue;
    private int mStepValue;
    private int mNegativeShift;

    private int mDisplayDividerValue;
    private boolean mUseDisplayDividerValue;
    private boolean mShowPlus;

    private String mFormat;
    private String mNote;
    private String mDefaultValueText;

    private int mSteppedMinValue;
    private int mSteppedMaxValue;

    private TextView mValue;
    private SeekBar mSeekBar;

    private SeekBar.OnSeekBarChangeListener mListener;

    public SeekBarPreferenceEx(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarPreferenceExStyle);
    }

    public SeekBarPreferenceEx(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mListener = null;

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SeekBarPreferenceEx);

            mMinValue = a.getInt(R.styleable.SeekBarPreferenceEx_minValue, 0);
            mMaxValue = a.getInt(R.styleable.SeekBarPreferenceEx_maxValue, 10);
            mStepValue = a.getInt(R.styleable.SeekBarPreferenceEx_stepValue, 1);
            mDefaultValue = a.getInt(R.styleable.SeekBarPreferenceEx_android_defaultValue, 0);
            mNegativeShift = a.getInt(R.styleable.SeekBarPreferenceEx_negativeShift, 0);
            mShowPlus = a.getBoolean(R.styleable.SeekBarPreferenceEx_showPlus, false);
            mUseDisplayDividerValue = a.hasValue(R.styleable.SeekBarPreferenceEx_displayDividerValue);
            mDisplayDividerValue = mUseDisplayDividerValue ? a.getInt(R.styleable.SeekBarPreferenceEx_displayDividerValue, 1) : 1;

            if (mMinValue < 0) mMinValue = 0;
            if (mMaxValue <= mMinValue) mMaxValue = mMinValue + 1;

            if (mDefaultValue < mMinValue)
                mDefaultValue = mMinValue;
            else if (mDefaultValue > mMaxValue)
                mDefaultValue = mMaxValue;

            if (mStepValue <= 0) mStepValue = 1;

            mFormat = a.getString(R.styleable.SeekBarPreferenceEx_format);
            mNote = a.getString(R.styleable.SeekBarPreferenceEx_note);
            mDefaultValueText = a.getString(R.styleable.SeekBarPreferenceEx_defaultValueText);
            a.recycle();
        } else {
            mMinValue = 0;
            mMaxValue = 10;
            mStepValue = 1;
            mDefaultValue = 0;
        }

        mSteppedMinValue = Math.round((float) mMinValue / mStepValue);
        mSteppedMaxValue = Math.round((float) mMaxValue / mStepValue);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        TextView mNoteView = (TextView) view.findViewById(android.R.id.text2);
        if (!TextUtils.isEmpty(mNote)) mNoteView.setText(mNote);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
        mValue = (TextView) view.findViewById(R.id.seekbar_value);

        mSeekBar.setMax(mSteppedMaxValue - mSteppedMinValue);
        setValue(getPersistedInt(mDefaultValue));
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mListener != null) mListener.onStopTrackingTouch(seekBar);
                saveValue();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mListener != null) mListener.onStartTrackingTouch(seekBar);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mListener != null) mListener.onProgressChanged(seekBar, getValue(), fromUser);
                updateDisplay(progress);
            }
        });
        view.setDividerAllowedAbove(false);
    }

    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener listener) {
        mListener = listener;
    }

    public int getMinValue() {
        return mMinValue;
    }

    public void setMinValue(int value) {
        mMinValue = value;
        updateAllValues();
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(int value) {
        mMaxValue = value;
        updateAllValues();
    }

    public int getStepValue() {
        return mStepValue;
    }

    public void setStepValue(int value) {
        mStepValue = value;
        updateAllValues();
    }

    public String getFormat() {
        return mFormat;
    }

    private void setFormat(String format) {
        mFormat = format;
        updateDisplay();
    }

    public void setFormat(int formatResId) {
        setFormat(getContext().getResources().getString(formatResId));
    }

    public int getValue() {
        return mSeekBar == null ? mDefaultValue : (mSeekBar.getProgress() + mSteppedMinValue) * mStepValue;
    }

    public void setValue(int value) {
        setValue(value, false);
    }

    public void setValue(int value, boolean save) {
        value = getBoundedValue(value) - mSteppedMinValue;
        mSeekBar.setProgress(value);
        updateDisplay(value);
        if (save) {
            saveValue();
        }
    }

    public void setDefaultValue(int value) {
        mDefaultValue = value;
    }

    private void updateAllValues() {
        int currentValue = getValue();
        if (mMaxValue <= mMinValue) mMaxValue = mMinValue + 1;
        mSteppedMinValue = Math.round((float) mMinValue / mStepValue);
        mSteppedMaxValue = Math.round((float) mMaxValue / mStepValue);

        if (mSeekBar != null) mSeekBar.setMax(mSteppedMaxValue - mSteppedMinValue);

        currentValue = getBoundedValue(currentValue) - mSteppedMinValue;

        if (mSeekBar != null) {
            mSeekBar.setProgress(currentValue);
            updateDisplay(currentValue);
        }
    }

    private int getBoundedValue(int value) {
        value = Math.round((float) value / mStepValue);
        if (value < mSteppedMinValue) value = mSteppedMinValue;
        if (value > mSteppedMaxValue) value = mSteppedMaxValue;
        return value;
    }

    private void updateDisplay() {
        updateDisplay(mSeekBar.getProgress());
    }

    private void updateDisplay(int value) {
        if (!TextUtils.isEmpty(mFormat)) {
            mValue.setVisibility(View.VISIBLE);
            value = (value + mSteppedMinValue) * mStepValue;
            String text;
            if (value == mDefaultValue && mDefaultValueText != null) {
                mValue.setText(mDefaultValueText);
                return;
            }

            if (mNegativeShift > 0) value -= mNegativeShift;

            try {
                if (mUseDisplayDividerValue) {
                    float floatValue = (float) value / (float) mDisplayDividerValue;
                    text = String.format(mFormat, floatValue);
                } else {
                    text = String.format(mFormat, value);
                }
            } catch (IllegalFormatException e) {
                e.printStackTrace();
                text = Integer.toString(value);
            }
            if (mShowPlus && value > 0) text = text + "+";
            mValue.setText(text);
        } else {
            mValue.setVisibility(View.GONE);
        }
    }

    private void saveValue() {
        persistInt(getValue());
        notifyChanged();
        callChangeListener(getValue());
    }

}
