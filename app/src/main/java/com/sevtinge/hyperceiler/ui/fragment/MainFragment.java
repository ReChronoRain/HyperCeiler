/*
  * This file is part of HyperCeiler.
  
  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getBaseOs;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getRomAuthor;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.MainActivityContextHelper;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt;

import java.util.ArrayList;
import java.util.Objects;

import moralnorm.preference.Preference;

public class MainFragment extends SettingsPreferenceFragment {

    Preference mPowerSetting;
    Preference mMTB;
    Preference mSecurityCenter;
    Preference mMiLink;
    Preference mAod;
    Preference mGuardProvider;
    Preference mMirror;
    Preference mTip;
    Preference mHeadtipWarn;
    MainActivityContextHelper mainActivityContextHelper;

    @Override
    public int getContentResId() {
        return R.xml.prefs_main;
    }

    @Override
    public void initPrefs() {
        mPowerSetting = findPreference("prefs_key_powerkeeper");
        mMTB = findPreference("prefs_key_mtb");
        mSecurityCenter = findPreference("prefs_key_security_center");
        mMiLink = findPreference("prefs_key_milink");
        mAod = findPreference("prefs_key_aod");
        mGuardProvider = findPreference("prefs_key_guardprovider");
        mMirror = findPreference("prefs_key_mirror");
        mTip = findPreference("prefs_key_tip");
        mHeadtipWarn = findPreference("prefs_key_headtip_warn");

        mPowerSetting.setVisible(!isAndroidVersion(30));
        mMTB.setVisible(!isAndroidVersion(30));

        if (isMoreHyperOSVersion(1f)) {
            mAod.setTitle(R.string.aod_hyperos);
            mMiLink.setTitle(R.string.milink_hyperos);
            mGuardProvider.setTitle(R.string.guard_provider_hyperos);
            mMirror.setTitle(R.string.mirror_hyperos);
            mSecurityCenter.setTitle(R.string.security_center_hyperos);
        } else {
            mAod.setTitle(R.string.aod);
            mMiLink.setTitle(R.string.milink);
            mGuardProvider.setTitle(R.string.guard_provider);
            mMirror.setTitle(R.string.mirror);
            if (isPad()) {
                mSecurityCenter.setTitle(R.string.security_center_pad);
            } else {
                mSecurityCenter.setTitle(R.string.security_center);
            }
        }

        mainActivityContextHelper = new MainActivityContextHelper(requireContext());
        String randomTip = mainActivityContextHelper.getRandomTip();
        mTip.setSummary("Tip: " + randomTip);

        isOfficialRom();
        if(!getIsOfficialRom()) isSignPass();
    }

    public void isOfficialRom() {
        mHeadtipWarn.setTitle(R.string.headtip_warn_not_offical_rom);
        mHeadtipWarn.setVisible(getIsOfficialRom());
    }

    public boolean getIsOfficialRom() {
        return (!com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getBaseOs().startsWith("V") && !com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getBaseOs().isEmpty()) || !getRomAuthor().isEmpty() || Objects.equals(SystemSDKKt.getHost(), "xiaomi.eu") || !SystemSDKKt.getHost().startsWith("pangu-build-component-system");
    }

    public void isSignPass() {
        mHeadtipWarn.setTitle(R.string.headtip_warn_sign_verification_failed);
        mHeadtipWarn.setVisible(!mainActivityContextHelper.isSignCheckPass());
    }
}
