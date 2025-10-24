package com.fan.common.widget;

import android.content.Context;
import android.util.AttributeSet;

import fan.androidbase.widget.ClearableEditText;

public class SearchEditText extends ClearableEditText {

    private OnSearchListener mOnSearchListener;

    public interface OnSearchListener {
        void onEditClear();
    }

    public SearchEditText(Context context) {
        super(context);
    }

    public SearchEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onClearButtonClick() {
        super.onClearButtonClick();
        if (mOnSearchListener != null) {
            mOnSearchListener.onEditClear();
        }
    }

    public void setOnSearchListener(OnSearchListener listener) {
        mOnSearchListener = listener;
    }
}
