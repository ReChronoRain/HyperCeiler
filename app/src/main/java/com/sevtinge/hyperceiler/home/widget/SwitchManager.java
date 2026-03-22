package com.sevtinge.hyperceiler.home.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sevtinge.hyperceiler.R;

import fan.cardview.HyperCardView;
import fan.core.utils.HyperMaterialUtils;
import fan.core.utils.MaterialDayNightConfig;
import fan.core.utils.RomUtils;
import fan.theme.token.BloomStrokeToken;
import fan.theme.token.ColorBlendToken;
import fan.theme.token.MaterialDayNightToken;
import fan.theme.token.MaterialToken;
import fan.theme.token.hypermaterial.Mask;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class SwitchManager {

    private final Context mContext;
    private final ViewGroup mParent;
    private SwitchView mSwitchView;

    private boolean isFloatingStyle;
    private OnSwitchChangeListener mUserListener;

    public SwitchManager(ViewGroup parent) {
        mParent = parent;
        mContext = parent.getContext();
    }

    public boolean isFloatingStyle() {
        return isFloatingStyle;
    }

    /**
     * 初始化并挂载视图
     */
    public void addSwitchView(int menuRes, NavigationStyle style) {
        if (mSwitchView == null) {
            mSwitchView = (SwitchView) LayoutInflater.from(mContext)
                .inflate(R.layout.switch_card_view, mParent, false);
            if (mUserListener != null) {
                mSwitchView.setOnSwitchChangeListener(mUserListener);
            }

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );

            mParent.addView(mSwitchView, lp);

            ViewCompat.requestApplyInsets(mSwitchView);
        }

        mSwitchView.inflateMenu(menuRes);
        setFloatingStyle(style == NavigationStyle.CAPSULE_ICON);
    }

    /**
     * 统一入口：切换样式
     */
    public void setFloatingStyle(boolean useFloating) {
        this.isFloatingStyle = useFloating;
        if (mSwitchView != null) {
            mSwitchView.updateStyle(useFloating ? NavigationStyle.CAPSULE_ICON : NavigationStyle.BOTTOM_LABEL);
        }
    }

    /**
     * 外部控制选中：按索引
     */
    public void setSelectedPosition(int position, boolean notify) {
        if (mSwitchView != null) {
            mSwitchView.setSelectedTab(position, notify);
        }
    }

    /**
     * 外部控制选中：按 Menu ID
     */
    public void setSelectedItemId(int itemId, boolean notify) {
        if (mSwitchView != null) {
            int pos = mSwitchView.getPositionById(itemId);
            if (pos != -1) mSwitchView.setSelectedTab(pos, notify);
        }
    }

    /**
     * 代理设置监听器
     */
    public void setOnSwitchChangeListener(OnSwitchChangeListener listener) {
        mUserListener = listener;
        if (mSwitchView != null) {
            mSwitchView.setOnSwitchChangeListener(listener);
        }
    }

    public void show() {
        if (mSwitchView != null) mSwitchView.setVisibility(View.VISIBLE);
    }

    public void hide() {
        if (mSwitchView != null) mSwitchView.setVisibility(View.GONE);
    }

    public SwitchView getSwitchView() {
        return mSwitchView;
    }
}
