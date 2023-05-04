package com.sevtinge.cemiuiler.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;

import com.sevtinge.cemiuiler.ui.base.SubFragment;


public class SystemFrameworkActivity extends BaseAppCompatActivity {

    @Override
    public Fragment initFragment() {
        return new SystemFrameworkFragment();
    }

    public static class SystemFrameworkFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.system_framework;
        }
    }
}
