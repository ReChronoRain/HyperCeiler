package com.sevtinge.cemiuiler.ui.fragment;

import static com.sevtinge.cemiuiler.utils.Helpers.log;
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

    @Override
    public int getContentResId() {
        return R.xml.prefs_main;
    }

    @Override
    public void initPrefs() {
        int randomTip = r.nextInt(17);
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

        if (randomTip == 1) mTip.setSummary(R.string.tip_1);
        else if (randomTip == 2) mTip.setSummary(R.string.tip_2);
        else if (randomTip == 3) mTip.setSummary(R.string.tip_3);
        else if (randomTip == 4) mTip.setSummary(R.string.tip_4);
        else if (randomTip == 5) mTip.setSummary(R.string.tip_5);
        else if (randomTip == 6) mTip.setSummary(R.string.tip_6);
        else if (randomTip == 7) mTip.setSummary(R.string.tip_7);
        else if (randomTip == 8) mTip.setSummary(R.string.tip_8);
        else if (randomTip == 9) mTip.setSummary(R.string.tip_9);
        else if (randomTip == 10) mTip.setSummary(R.string.tip_10);
        else if (randomTip == 11) mTip.setSummary(R.string.tip_11);
        else if (randomTip == 12) mTip.setSummary(R.string.tip_12);
        else if (randomTip == 13) mTip.setSummary(R.string.tip_13);
        else if (randomTip == 14) mTip.setSummary(R.string.tip_14);
        else if (randomTip == 15) mTip.setSummary(R.string.tip_15);
        else if (randomTip == 16) mTip.setSummary(R.string.tip_16);
        else mTip.setSummary(R.string.tip_0);
    }
}
