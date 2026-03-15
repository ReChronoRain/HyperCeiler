package com.sevtinge.hyperceiler.log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.sevtinge.hyperceiler.home.adapter.FragmentStateAdapter;

import fan.viewpager.widget.ViewPager;

public class LogPagerAdapter extends FragmentStateAdapter {

    public LogPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity.getSupportFragmentManager());
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return LogListFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    /**
     * 获取 ViewPager 当前正在显示的 Fragment 实例
     */
    public LogListFragment getCurrentFragment(ViewPager viewPager) {
        // 利用 instantiateItem 寻找当前位置的 Fragment 实例
        Object item = instantiateItem(viewPager, viewPager.getCurrentItem());
        if (item instanceof LogListFragment) {
            return (LogListFragment) item;
        }
        return null;
    }
}
