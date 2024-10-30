package com.sevtinge.hyperceiler.data.adapter;

import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragmentPagerAdapter extends FragmentPagerAdapter {

    public boolean indexNeedReverse = false;
    private final List<Fragment> mFragments = new ArrayList<>();

    public HomeFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getPositionFromCache(int position) {
        return super.getPositionFromCache(processingIndex(position, 2, TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1));
    }

    private static int processingIndex(int position, int size, boolean isRtl) {
        return isRtl ? (size - position) - 1 : position;
    }


    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public String makeFragmentName(int position, long id) {
        return getFragmentTag(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    public static String getFragmentTag(int position) {
        return "android:switcher::" + position;
    }

    public void addFragment(Fragment fragment) {
        mFragments.add(fragment);
    }

    public void addFragment(int position, Fragment fragment) {
        mFragments.add(position, fragment);
    }

}
