package com.sevtinge.hyperceiler.common.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.sevtinge.hyperceiler.ui.R;

import fan.preference.PreferenceStyle;

public class LayoutPreference extends Preference implements PreferenceStyle {

    private int mLayoutResId;
    private CharSequence mTtitle;

    private Drawable mBackground;

    private View mRootView;
    private final View.OnClickListener mClickListener = view -> performClick(view);

    public LayoutPreference(@NonNull Context context, int layoutResId) {
        this(context, LayoutInflater.from(context).inflate(layoutResId, null, false));
    }

    public LayoutPreference(Context context, View v) {
        super(context);
        setView(v);
    }

    public LayoutPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public LayoutPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LayoutPreference, defStyleAttr, 0);
        mLayoutResId = a.getResourceId(R.styleable.LayoutPreference_android_layout, 0);
        if (mLayoutResId == 0) throw new IllegalArgumentException("LayoutPreference requires a layout to be defined");
        mTtitle = a.getText(R.styleable.LayoutPreference_android_title);
        mBackground = a.getDrawable(R.styleable.LayoutPreference_android_background);
        a.recycle();
        setView(LayoutInflater.from(getContext()).inflate(mLayoutResId, null, false));
    }


    private void setView(View view) {
        setLayoutResource(R.layout.preference_layout_frame);
        mRootView = view;
        setLayoutTitle(mTtitle);
        setLayoutBackground(mBackground);
        setShouldDisableView(false);
    }

    private void setLayoutTitle(CharSequence title) {
        TextView titleView = findViewById(android.R.id.title);
        if (titleView != null) {
            titleView.setText(title);
            titleView.setVisibility(View.VISIBLE);
        }
    }

    private void setLayoutBackground(Drawable background) {
        FrameLayout containerView = findViewById(R.id.container);
        if (containerView != null && background != null) {
            containerView.setBackground(background);
        }
    }

    public <T extends View> T findViewById(int id) {
        return mRootView.findViewById(id);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        View itemView = holder.itemView;
        itemView.setOnClickListener(mClickListener);
        itemView.setFocusable(isSelectable());
        itemView.setClickable(isSelectable());
        FrameLayout frameLayout = (FrameLayout) itemView;
        frameLayout.removeAllViews();
        ViewGroup viewGroup = (ViewGroup) mRootView.getParent();
        if (viewGroup != null) viewGroup.removeView(mRootView);
        frameLayout.addView(mRootView);
    }

    @Override
    public boolean isTouchAnimationEnable() {
        return true;
    }

    @Override
    public boolean isEnabledCardStyle() {
        return false;
    }
}
