package com.sevtinge.hyperceiler.ui;

public interface OnSwitchChangeListener {
    /**
     * 当切换 Tab 时触发
     * @param position 选中的索引 (0, 1, 2...)
     * @param itemId 对应的 Menu Item ID (R.id.xxx)
     */
    void onSwitchChange(int position, int itemId);
}
