package com.sevtinge.cemiuiler.ui;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemframework.base.BaseSystemFrameWorkActivity;

public class MiWallpaperActivity extends BaseSystemFrameWorkActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.MiWallpaperActivity.MiWallpaperFragment();
    }


    public static class MiWallpaperFragment extends SubFragment {

        @Override
        public int getContentResId() {
            return R.xml.miwallpaper;
        }
    }
}

