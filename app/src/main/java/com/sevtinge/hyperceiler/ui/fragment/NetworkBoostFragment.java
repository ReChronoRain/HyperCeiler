package com.sevtinge.hyperceiler.ui.fragment;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class NetworkBoostFragment extends SettingsPreferenceFragment {
        @Override
        public int getContentResId() {
            return R.xml.networkboost;
        }

        @Override
        public View.OnClickListener addRestartListener() {
            return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
                getResources().getString(R.string.networkboost),
                "com.xiaomi.networkboost"
            );
        }
    }

