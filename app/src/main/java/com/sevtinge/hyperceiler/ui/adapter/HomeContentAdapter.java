package com.sevtinge.hyperceiler.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import fan.viewpager2.adapter.FragmentStateAdapter;

public class HomeContentAdapter extends FragmentStateAdapter {

    List<Fragment> mFragments = new ArrayList<>();

    public HomeContentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void addFragment(Fragment fragment) {
        mFragments.add(fragment);
    }

    public Fragment getFragment(int position) {
        if (position < 0 || position >= mFragments.size()) {
            return null;
        }
        return mFragments.get(position);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getItemCount() {
        return mFragments.size();
    }
}
