/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
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
