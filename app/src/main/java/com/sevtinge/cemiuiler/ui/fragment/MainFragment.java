package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;

import java.util.Random;

import moralnorm.preference.Preference;

public class MainFragment extends SettingsPreferenceFragment {

    Preference mPowerSetting;
    Preference mMTB;
    Preference mSecurityCenter;
    Preference mSecurityCenterPad;
    Preference mTip;
    Random r = new Random();

    int[] tips = new int[] {
        R.string.tip_0,
        R.string.tip_1,
        R.string.tip_2,
        R.string.tip_3,
        R.string.tip_4,
        R.string.tip_5,
        R.string.tip_6,
        R.string.tip_7,
        R.string.tip_8,
        R.string.tip_9,
        R.string.tip_10,
        R.string.tip_11,
        R.string.tip_12,
        R.string.tip_13,
        R.string.tip_14,
        R.string.tip_15,
        R.string.tip_16,
        R.string.tip_17,
        R.string.tip_18,
        R.string.tip_19,
        R.string.tip_20,
    };

    @Override
    public int getContentResId() {
        return R.xml.prefs_main;
    }

    @Override
    public void initPrefs() {
        int randomTip = r.nextInt(tips.length);
        // log("tip id is" + randomTip);

        mPowerSetting = findPreference("prefs_key_powerkeeper");
        mMTB = findPreference("prefs_key_mtb");
        mSecurityCenter = findPreference("prefs_key_security_center");
        mSecurityCenterPad = findPreference("prefs_key_security_center_pad");
        mTip = findPreference("prefs_key_tip");

        mPowerSetting.setVisible(!isAndroidR());
        mMTB.setVisible(!isAndroidR());

        mSecurityCenter.setVisible(!isPad());
        mSecurityCenterPad.setVisible(isPad());

        mTip.setSummary(tips[randomTip]);
    }
}
