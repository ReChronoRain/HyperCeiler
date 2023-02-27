package com.sevtinge.cemiuiler.ui.home;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.home.base.BaseHomeActivity;

public class HomeOtherActivity extends BaseHomeActivity {

    @Override
    public Fragment initFragment() {
        return new HomeOtherFragment();
    }

   public static class HomeOtherFragment extends SubFragment {

       @Override
       public int getContentResId() {
           return R.xml.home_other;
       }

       @Override
       public void initPrefs() {

       }
   }
}
