package com.sevtinge.cemiuiler.ui.systemframework;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.systemframework.base.BaseSystemFrameWorkActivity;
import com.sevtinge.cemiuiler.utils.SdkHelper;

import moralnorm.preference.SwitchPreference;

public class FreeFormSettings extends BaseSystemFrameWorkActivity {

    @Override
    public Fragment initFragment() {
        return new com.sevtinge.cemiuiler.ui.systemframework.FreeFormSettings.FreeFormFragment();
    }

    public static class FreeFormFragment extends SubFragment {

        SwitchPreference mMoreFreeForm; //多小窗
        SwitchPreference mSmallFreeForm; //小窗气泡

        @Override
        public int getContentResId() {
            return R.xml.system_framework_freeform;
        }

        @Override
        public void initPrefs() {
            mMoreFreeForm = findPreference("prefs_key_system_framework_freeform_count");
            mSmallFreeForm = findPreference("prefs_key_system_framework_freeform_bubble");

            mMoreFreeForm.setVisible(SdkHelper.PROP_MIUI_VERSION_CODE >= 13);
            mSmallFreeForm.setVisible(!SdkHelper.isAndroidR());
        }
    }
}