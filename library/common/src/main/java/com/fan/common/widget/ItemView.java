package com.fan.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.fan.common.R;

import fan.animation.Folme;

public class ItemView extends LinearLayout {

    private String mTitle;
    private String mSummary;
    private final String mValue;
    private int mWidgetLayout;
    private final boolean mEnabled;

    private int mBackgroundRes;

    private final int mTitleTextColor;
    private final int mSummaryTextColor;

    private boolean mShowArrowRight;

    private View mItemView;
    private TextView mTitleView;
    private TextView mSummaryView;
    private LinearLayout mWidgetFrameView;
    private ImageView mArrowRightView;

    public ItemView(Context context) {
        this(context, null);
    }

    public ItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public ItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.ItemViewStyle_DayNight);
    }

    public ItemView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ItemView, defStyleAttr, defStyleRes);
        mTitle = a.getString(R.styleable.ItemView_android_title);
        mSummary = a.getString(R.styleable.ItemView_android_summary);
        mValue = a.getString(R.styleable.ItemView_android_value);
        mWidgetLayout = a.getResourceId(R.styleable.ItemView_android_widgetLayout, 0);

        mEnabled = a.getBoolean(R.styleable.ItemView_android_enabled, true);

        mTitleTextColor = a.getColor(R.styleable.ItemView_titleTextColor,
            getContext().getColor(R.color.item_view_title_color_light));

        mSummaryTextColor = a.getColor(R.styleable.ItemView_summaryTextColor,
            getContext().getColor(R.color.item_view_summary_color_light));

        mShowArrowRight = a.getBoolean(R.styleable.ItemView_arrowRightShow, true);

        a.recycle();
        initView(context);
    }

    private void initView(Context context) {
        mItemView = LayoutInflater.from(context).inflate(R.layout.item_view, this, true);
        mTitleView = findViewById(R.id.tv_title);
        mSummaryView = findViewById(R.id.tv_summary);
        mWidgetFrameView = findViewById(R.id.widget_frame);
        mArrowRightView = findViewById(R.id.arrow_right);

        setTitle(mTitle);
        setSummary(mSummary);
        setArrowRightVisible(mShowArrowRight);
        Folme.useAt(mItemView).touch().setScale(1.0f).setBackgroundColor(0.08f, 0.0f, 0.0f, 0.0f).handleTouchOf(mItemView);
    }

    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            mTitle = title;
            mTitleView.setText(title);
            mTitleView.setTextColor(mTitleTextColor);
            if (mTitleView.getVisibility() != View.VISIBLE) {
                mTitleView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mTitleView.getVisibility() != View.GONE) {
                mTitleView.setVisibility(View.GONE);
            }
        }
    }

    public void setTitle(@StringRes int titleResId) {
        mTitleView.setText(titleResId);
    }

    public void setSummary(String summary) {
        if (!TextUtils.isEmpty(summary)) {
            mSummary = summary;
            mSummaryView.setText(summary);
            mSummaryView.setTextColor(mSummaryTextColor);
            if (mSummaryView.getVisibility() != View.VISIBLE) {
                mSummaryView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mSummaryView.getVisibility() != View.GONE) {
                mSummaryView.setVisibility(View.GONE);
            }
        }
    }

    public void setSummary(@StringRes int summaryResId) {
        mSummaryView.setText(summaryResId);
    }

    public void setSummaryVisible(boolean visible) {
        mSummaryView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    protected int getWidgetFrameLayout() {
        return 0;
    }

    public void setWidgetFrameLayout(@LayoutRes int layoutResId) {
        if (layoutResId != 0) {
            mWidgetLayout = layoutResId;
            if (mWidgetFrameView.getChildCount() != 0) {
                mWidgetFrameView.removeAllViews();
            }
            mWidgetFrameView.addView(LayoutInflater.from(getContext()).inflate(layoutResId, mWidgetFrameView, false));
            if (mWidgetFrameView.getVisibility() != View.VISIBLE) {
                mWidgetFrameView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mWidgetFrameView.getVisibility() != View.GONE) {
                mWidgetFrameView.setVisibility(View.GONE);
            }
        }
    }

    protected View getWidgetFrameView() {
        return null;
    }

    public void setWidgetFrameView(View v) {
        mWidgetFrameView.removeAllViews();
        mWidgetFrameView.addView(v);
    }

    public void setWidgetFrameVisible(boolean visible) {
        mWidgetFrameView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public boolean isArrowRightVisible() {
        return mShowArrowRight;
    }

    public void setArrowRightVisible(boolean visible) {
        if (mShowArrowRight != visible) {
            mShowArrowRight = visible;
            if (visible) {
                if (mArrowRightView.getVisibility() != View.VISIBLE) {
                    mArrowRightView.setVisibility(View.VISIBLE);
                }
            } else {
                if (mArrowRightView.getVisibility() != View.GONE) {
                    mArrowRightView.setVisibility(View.GONE);
                }
            }
        }
    }

    public void setArrowRightViewVisible(boolean visible) {
        mArrowRightView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setViewEnable(boolean enabled) {
        setEnabled(enabled);
        setClickable(enabled);
        mTitleView.setEnabled(enabled);
        mSummaryView.setEnabled(enabled);
        mArrowRightView.setEnabled(enabled);
    }
}
