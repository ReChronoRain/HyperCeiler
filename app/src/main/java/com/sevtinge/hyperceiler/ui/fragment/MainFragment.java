package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import java.util.Random;

import moralnorm.preference.Preference;

public class MainFragment extends SettingsPreferenceFragment {

    Preference mPowerSetting;
    Preference mMTB;
    Preference mSecurityCenter;
    Preference mSecurityCenterPad;
    Preference mMiLink;
    Preference mMiLinkHyperOS;
    Preference mAod;
    Preference mAodHyperOS;
    Preference mGuardProvider;
    Preference mGuardProviderHyperOS;
    Preference mMirror;
    Preference mMirrorHyperOS;
    Preference mTip;
    Random r = new Random();

    int[] tips = new int[]{
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
        mMiLink = findPreference("prefs_key_milink");
        mMiLinkHyperOS = findPreference("prefs_key_milink_hyperos");
        mAod = findPreference("prefs_key_aod");
        mAodHyperOS = findPreference("prefs_key_aod_hyperos");
        mGuardProvider = findPreference("prefs_key_guardprovider");
        mGuardProviderHyperOS = findPreference("prefs_key_guardprovider_hyperos");
        mMirror = findPreference("prefs_key_mirror");
        mMirrorHyperOS = findPreference("prefs_key_mirror_hyperos");
        mTip = findPreference("prefs_key_tip");

        mPowerSetting.setVisible(!isAndroidVersion(30));
        mMTB.setVisible(!isAndroidVersion(30));

        mSecurityCenter.setVisible(!isPad());
        mSecurityCenterPad.setVisible(isPad());

        mMiLink.setVisible(!isMoreHyperOSVersion(1f));
        mMiLinkHyperOS.setVisible(isMoreHyperOSVersion(1f));

        mAod.setVisible(!isMoreHyperOSVersion(1f));
        mAodHyperOS.setVisible(isMoreHyperOSVersion(1f));

        mGuardProvider.setVisible(!isMoreHyperOSVersion(1f));
        mGuardProviderHyperOS.setVisible(isMoreHyperOSVersion(1f));

        mMirror.setVisible(!isMoreHyperOSVersion(1f));
        mMirrorHyperOS.setVisible(isMoreHyperOSVersion(1f));

        mTip.setSummary(tips[randomTip]);
    }
}
