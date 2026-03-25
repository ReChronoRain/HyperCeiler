package com.sevtinge.hyperceiler.sub;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.core.R;

import fan.preference.RadioButtonPreference;

public class AppSelectorRadioButtonPreference extends RadioButtonPreference {

    private ImageView mArrowView;
    private onArrowViewClickListener mOnArrowViewClickListener;

    public AppSelectorRadioButtonPreference(@NonNull Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.app_selector_arrow);
    }

    public AppSelectorRadioButtonPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.app_selector_arrow);
    }

    public AppSelectorRadioButtonPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWidgetLayoutResource(R.layout.app_selector_arrow);
    }

    @Override
    public void onBindViewHolderAfter() {
        super.onBindViewHolderAfter();
        mArrowView = mItemView.findViewById(R.id.arrow_view);
        if (mArrowView != null) {
            mArrowView.setEnabled(isChecked());
            mArrowView.setAlpha(mArrowView.isEnabled() ? 1.0f : 0.4f);
            mArrowView.setOnClickListener(v -> {
                if (mOnArrowViewClickListener != null) {
                    mOnArrowViewClickListener.onClick();
                }
            });
        }
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (mArrowView != null) {
            mArrowView.setEnabled(checked);
        }
    }

    public void setArrowViewClickListener(onArrowViewClickListener listener) {
        mOnArrowViewClickListener = listener;
    }

    public interface onArrowViewClickListener {
        void onClick();
    }
}
