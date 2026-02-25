package com.sevtinge.hyperceiler.ui;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPagerCompat;
import androidx.viewpager2.widget.ViewPager2;

import fan.viewpager.widget.ViewPager;

public class SwitchMediator {

    private final SwitchManager mManager;
    private final ViewPager mViewPager;
    private final boolean mSmoothScroll;
    private boolean mIsInternalClick = false; // 防止循环触发

    public SwitchMediator(@NonNull SwitchManager manager, @NonNull ViewPager viewPager) {
        this(manager, viewPager, true);
    }

    public SwitchMediator(@NonNull SwitchManager manager, @NonNull ViewPager viewPager, boolean smoothScroll) {
        mManager = manager;
        mViewPager = viewPager;
        mSmoothScroll = smoothScroll;
    }

    public void attach() {
        mManager.setOnSwitchChangeListener((position, itemId) -> {
            mIsInternalClick = true;
            mViewPager.setCurrentItem(position, mSmoothScroll);
            mIsInternalClick = false;
        });
        mViewPager.addOnPageChangeListener(new ViewPagerCompat.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (!mIsInternalClick) {
                    // 调用我们之前写的选中方法，notify 传 false 避免反向触发
                    mManager.setSelectedPosition(position, false);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
}
