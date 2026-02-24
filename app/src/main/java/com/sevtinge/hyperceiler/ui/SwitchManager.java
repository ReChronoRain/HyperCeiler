package com.sevtinge.hyperceiler.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sevtinge.hyperceiler.R;

import fan.appcompat.app.floatingactivity.FloatingABOLayoutSpec;
import fan.cardview.HyperCardView;
import fan.core.utils.HyperMaterialUtils;
import fan.core.utils.MaterialConfig;
import fan.core.utils.MaterialDayNightConfig;
import fan.core.utils.RomUtils;
import fan.internal.utils.ViewUtils;
import fan.theme.token.BloomStrokeToken;
import fan.theme.token.ColorBlendToken;
import fan.theme.token.MaterialDayNightToken;
import fan.theme.token.MaterialToken;
import fan.theme.token.hypermaterial.Mask;

public class SwitchManager {

    private Context mContext;
    private ViewGroup mParent;             // 宿主布局
    private HyperCardView mCardWrapper;    // 负责材质和圆角的包裹层
    private SwitchView mSwitchView;        // 负责 Tab 逻辑的组件
    private OnSwitchChangeListener mUserListener;

    public SwitchManager(ViewGroup parent) {
        mParent = parent;
        mContext = parent.getContext();
    }

    /**
     * 初始化并添加视图
     */
    public void addSwitchView(int menuRes, NavigationStyle style) {
        if (mCardWrapper == null) {
            // 创建并配置 HyperCardView
            mCardWrapper = (HyperCardView) LayoutInflater.from(mContext).inflate(R.layout.switch_card_view, null);

            // 创建 SwitchView 并同步监听器
            mSwitchView = new SwitchView(mContext, null);
            if (mUserListener != null) mSwitchView.setOnSwitchChangeListener(mUserListener);

            mCardWrapper.addView(mSwitchView, new FrameLayout.LayoutParams(-1, -1));
            mParent.addView(mCardWrapper);
        }

        mSwitchView.inflateMenu(menuRes);
        setFloatingStyle(style == NavigationStyle.CAPSULE_ICON);
    }

    /**
     * 【新入口】通过布尔值统一设置导航样式
     *
     * @param useFloating true: 悬浮药丸样式, false: 底部贴地样式
     */
    public void setFloatingStyle(boolean useFloating) {
        if (useFloating) {
            applyCapsuleStyle();
        } else {
            applyBottomNavStyle();
        }
    }

    /**
     * 场景 1: 应用药丸悬浮样式
     */
    private void applyCapsuleStyle() {
        if (mCardWrapper == null || mSwitchView == null) return;
        setCardHyperMaterial(getBloomStrokeDayNightConfig());
        mSwitchView.updateStyle(NavigationStyle.CAPSULE_ICON);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mCardWrapper.getLayoutParams();
        lp.width = mContext.getResources().getDimensionPixelSize(R.dimen.switch_view_width);
        lp.height = mContext.getResources().getDimensionPixelSize(R.dimen.switch_view_height);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = mContext.getResources().getDimensionPixelSize(R.dimen.switch_view_margin_bottom); // 底部留白，产生悬浮感

        mCardWrapper.setLayoutParams(lp);
        mCardWrapper.setRadius(mContext.getResources().getDimensionPixelSize(R.dimen.switch_card_view_radius)); // 大圆角
        mCardWrapper.setBackgroundResource(R.drawable.switch_view_capsule_bg);

        // 悬浮模式下，移除 Insets 监听或将 padding 置 0
        ViewCompat.setOnApplyWindowInsetsListener(mCardWrapper, null);
        mSwitchView.setPadding(0, 0, 0, 0);

        refreshSwitchViewPosition();
    }

    /**
     * 场景 2: 应用底部贴地样式
     */
    private void applyBottomNavStyle() {
        if (mCardWrapper == null || mSwitchView == null) return;
        setCardHyperMaterial(getDayNightConfig());
        mSwitchView.updateStyle(NavigationStyle.BOTTOM_LABEL);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mCardWrapper.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        lp.bottomMargin = 0; // 贴地，无间距

        mCardWrapper.setLayoutParams(lp);
        mCardWrapper.setRadius(0); // 矩形
        mCardWrapper.setBackgroundResource(R.drawable.switch_view_bg);

        // --- 边到边适配核心代码 ---
        ViewCompat.setOnApplyWindowInsetsListener(mCardWrapper, (v, insets) -> {
            // 获取系统导航栏的高度
            int systemBarBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            // 为 SwitchView 设置底部 Padding，确保内容不被手势横条遮挡
            // 标准高度(56dp) + 系统栏高度
            mSwitchView.setPadding(0, 0, 0, systemBarBottom);

            return insets;
        });

        // 强制触发一次 Insets 应用
        ViewCompat.requestApplyInsets(mCardWrapper);

        refreshSwitchViewPosition();
    }

    /**
     * 外部控制选中：按索引
     */
    public void setSelectedPosition(int position, boolean notify) {
        if (mSwitchView != null) mSwitchView.setSelectedTab(position, notify);
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
     * 设置监听器（代理给 SwitchView）
     */
    public void setOnSwitchChangeListener(OnSwitchChangeListener listener) {
        mUserListener = listener;
        if (mSwitchView != null) mSwitchView.setOnSwitchChangeListener(listener);
    }

    public void refreshSwitchViewPosition() {
        if (mSwitchView != null) {
            mSwitchView.post(() -> mSwitchView.setSelectedTab(mSwitchView.getSelectedPosition(), false));
        }
    }

    public void show() {
        if (mCardWrapper != null) mCardWrapper.setVisibility(View.VISIBLE);
    }

    public void hide() {
        if (mCardWrapper != null) mCardWrapper.setVisibility(View.GONE);
    }

    private int dpToPx(int dp) {
        return (int) (dp * mParent.getContext().getResources().getDisplayMetrics().density);
    }


    public final void setCardHyperMaterial(MaterialDayNightConfig config) {
        if (HyperMaterialUtils.isFeatureEnable(mContext) && RomUtils.getHyperOsVersion() >= 2) {
            mCardWrapper.setMaterial(config);
        }
    }

    public MaterialDayNightConfig getBloomStrokeDayNightConfig() {
        MaterialToken lightToken = new MaterialToken.Builder(30, "frosted-pured-regular", "light")
            .setBlur(1, 1, 0, 40)
            .setColorBlend(ColorBlendToken.Pured_Regular_Light)
            .setBloomStroke(BloomStrokeToken.Glass_Stroke_Small_Light)
            .build();

        MaterialToken darkToken = new MaterialToken.Builder(30, "frosted-pured-extra-thick", "dark")
            .setBlur(1, 1, 0, 40)
            .setColorBlend(ColorBlendToken.Pured_Extra_Thick_Dark)
            .setBloomStroke(BloomStrokeToken.Glass_Stroke_Small_Dark)
            .build();

        return MaterialDayNightConfig.create(new MaterialDayNightToken(lightToken, darkToken));
    }

    public MaterialDayNightConfig getDayNightConfig() {
        return MaterialDayNightConfig.create(Mask.Pured_Regular);
    }
}
