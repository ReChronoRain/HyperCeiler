package com.sevtinge.cemiuiler.ui.main.fragment;

import android.os.Bundle;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;

public class MainFragment extends SubFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getContentResId() {
        return R.xml.prefs_main;
    }
}
