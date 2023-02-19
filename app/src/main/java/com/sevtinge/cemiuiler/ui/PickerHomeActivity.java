package com.sevtinge.cemiuiler.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;
import com.sevtinge.cemiuiler.ui.sub.AppPickerFragment;
import com.sevtinge.cemiuiler.ui.sub.AppPickerFragment2;
import com.sevtinge.cemiuiler.ui.sub.CustomBackgroundSettings;
import com.sevtinge.cemiuiler.ui.sub.MultiAction;

public class PickerHomeActivity extends BaseAppCompatActivity {

    public Fragment mArgFragment;
    public Bundle args;
    public static String mTitle = null;
    public static String mKey = null;

    public enum Actions {
        Home, StatusBar, Blur, Apps, Apps2
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setFragment();
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        setTitle(mTitle);
    }

    private void setFragment() {
        Bundle args = getIntent().getExtras();
        Actions actions = Actions.values()[args.getInt("actions")];
        mTitle = args.getString("title");
        switch (actions) {
            case Home:
                mArgFragment = new MultiAction();
                break;

            case Blur:
                /*mArgFragment = new CustomBlurSettings();*/
                mArgFragment = new CustomBackgroundSettings();
                break;

            case Apps:
                mArgFragment = new AppPickerFragment();
                break;

            case Apps2:
                mArgFragment = new AppPickerFragment2();
                break;

            default:
                mArgFragment = null;
                break;
        }
    }

    @Override
    public Fragment initFragment() {
        return mArgFragment;
    }
}
