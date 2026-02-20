package com.sevtinge.hyperceiler.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;

import java.util.ArrayList;
import java.util.List;

import fan.animation.Folme;
import fan.animation.base.AnimConfig;
import fan.animation.controller.AnimState;
import fan.animation.property.ViewProperty;

public class SwitchView extends FrameLayout {

    private View mDividerLine; // 新增分割线成员
    private LinearLayout mTabContainer;    // 存放图标和文字的容器
    private final List<View> mItemViews = new ArrayList<>(); // 所有的 Tab 视图

    private NavigationStyle mCurrentStyle = NavigationStyle.CAPSULE_ICON;
    private int mSelectedPosition = -1;
    private int mCurrentMenuRes = -1;
    private OnSwitchChangeListener mInternalListener;

    public SwitchView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 1. 创建分割线 (置于顶部)
        mDividerLine = new View(getContext());
        mDividerLine.setBackgroundColor(Color.parseColor("#1A000000")); // 浅灰色边框
        mDividerLine.setVisibility(GONE); // 默认隐藏（药丸模式不需要）
        // 高度设为 1px 而非 1dp，更精致
        addView(mDividerLine, new LayoutParams(LayoutParams.MATCH_PARENT, 1));

        // 初始化 Tab 容器
        mTabContainer = new LinearLayout(getContext());
        mTabContainer.setOrientation(LinearLayout.HORIZONTAL);
        mTabContainer.setGravity(Gravity.CENTER);
        addView(mTabContainer, new LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        ));
    }

    /**
     * 更新样式逻辑
     */
    public void updateStyle(NavigationStyle style) {
        mCurrentStyle = style;
        mTabContainer.removeAllViews();
        mItemViews.clear();

        LayoutParams containerLp = (LayoutParams) mTabContainer.getLayoutParams();

        if (style == NavigationStyle.CAPSULE_ICON) {
            updateCapsuleNavLayout(containerLp);
        } else {
            updateBottomNavLayout(containerLp);
        }

        mTabContainer.setLayoutParams(containerLp);

        // 如果已有菜单数据，根据新样式重新生成 Tab
        if (mCurrentMenuRes != -1) {
            refreshTabs();
        }
    }

    /**
     * 内部样式更新时同步线状态
     */
    private void updateBottomNavLayout(LayoutParams lp) {
        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = getContext().getResources().getDimensionPixelSize(fan.navigator.R.dimen.miuix_design_bottom_navigation_height);
        lp.gravity = Gravity.TOP;
        mTabContainer.setPadding(0, 0, 0, 0);

        // 显示分割线
        mDividerLine.setVisibility(VISIBLE);
    }

    private void updateCapsuleNavLayout(LayoutParams lp) {
        lp.width = LayoutParams.MATCH_PARENT;
        // 关键：容器本身只占 56dp 高度
        lp.height = LayoutParams.WRAP_CONTENT;
        // 关键：重心设为顶部，这样它就不会被底部的 Padding 挤到中间或顶上去
        lp.gravity = Gravity.CENTER;
        mTabContainer.setPadding(0, 0, 0, 0);

        // 隐藏分割线
        mDividerLine.setVisibility(GONE);
    }

    /**
     * 解析并渲染 Tabs
     */
    public void inflateMenu(int menuRes) {
        mCurrentMenuRes = menuRes;
        refreshTabs();
    }

    private void refreshTabs() {
        PopupMenu pm = new PopupMenu(getContext(), null);
        pm.inflate(mCurrentMenuRes);
        Menu menu = pm.getMenu();

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (mCurrentStyle == NavigationStyle.CAPSULE_ICON) {
                createCapsuleItem(item, i);
            } else {
                createBottomNavItem(item);
            }
        }

        // 恢复选中状态（不触发回调）
        if (!mItemViews.isEmpty()) {
            int pos = Math.max(0, mSelectedPosition);
            post(() -> setSelectedTab(pos, false));
        }
    }

    // 创建：纯图标药丸项
    private void createCapsuleItem(MenuItem item, int index) {
        ImageView iv = new ImageView(getContext());
        iv.setTag(item.getItemId()); // 存储 ID

        iv.setImageDrawable(item.getIcon());
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iv.setClickable(true);
        iv.setFocusable(false);

        // 布局参数：平分宽度 (weight=1)
        LinearLayout.LayoutParams childLp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);

        iv.setOnClickListener(v -> setSelectedTab(mItemViews.indexOf(iv), true));
        mTabContainer.addView(iv, childLp);
        mItemViews.add(iv);
    }

    /**
     * 创建底部导航子项：图标 + 文字 + 绝对居中
     */
    private void createBottomNavItem(MenuItem item) {
        LinearLayout itemView = new LinearLayout(getContext());
        itemView.setOrientation(LinearLayout.VERTICAL);
        // 1. 核心：确保容器内的子 View 整体居中
        itemView.setGravity(Gravity.CENTER);
        itemView.setTag(item.getItemId());

        // 2. 创建图标
        ImageView iv = new ImageView(getContext());
        iv.setImageDrawable(item.getIcon());
        // 如果图标本身有白边，可以尝试：iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int iconSize = getContext().getResources().getDimensionPixelSize(fan.navigator.R.dimen.miuix_design_bottom_navigation_icon_size);
        itemView.addView(iv, new LinearLayout.LayoutParams(iconSize, iconSize));

        // 3. 创建文字
        TextView tv = new TextView(getContext());
        tv.setText(item.getTitle());
        tv.setTextSize(12f);
        // 确保文字在 TextView 内部也是居中的
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dpToPx(2), 0, 0);

        // 将 TextView 宽度设为 MATCH_PARENT 配合 setGravity(CENTER) 实现水平对齐
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        itemView.addView(tv, tvLp);

        // 4. 设置点击监听
        itemView.setOnClickListener(v -> setSelectedTab(mItemViews.indexOf(itemView), true));

        // 5. 底部样式平分宽度
        mTabContainer.addView(itemView, new LinearLayout.LayoutParams(0, -1, 1.0f));
        mItemViews.add(itemView);
    }


    /**
     * 设置选中状态并执行 Folme 动画
     */
    public void setSelectedTab(int position, boolean notify) {
        if (position < 0 || position >= mItemViews.size()) return;
        mSelectedPosition = position;
        View target = mItemViews.get(position);

        // 未选中项半透明处理
        for (int i = 0; i < mItemViews.size(); i++) {
            mItemViews.get(i).setAlpha(i == position ? 1.0f : 0.4f);
        }

        if (notify && mInternalListener != null) {
            mInternalListener.onSwitchChange(position, (int) target.getTag());
        }
    }

    // 辅助方法：按 ID 找位置
    public int getPositionById(int itemId) {
        for (int i = 0; i < mItemViews.size(); i++) {
            if ((int) mItemViews.get(i).getTag() == itemId) return i;
        }
        return -1;
    }

    public int getSelectedPosition() { return mSelectedPosition; }
    public void setOnSwitchChangeListener(OnSwitchChangeListener l) { this.mInternalListener = l; }
    private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density); }
}
