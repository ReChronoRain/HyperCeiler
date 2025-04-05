/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.RequiresPermission;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public class TelephonyUtils {
    private static TelephonyManager mTelephonyManager;

    public static TelephonyManager getTelephonyManager() {
        return mTelephonyManager;
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.READ_PRIVILEGED_PHONE_STATE"})
    public static boolean isRoaming(Context context, int slotId, List<String> teleZoneCodeList, List<String> iccidStartsWithList, boolean isRadical) {
        SubscriptionInfo subscriptionInfo = getSubscriptionInfo(context, slotId);
        String simPhoneNumber = getSimPhoneNumber(context, subscriptionInfo);
        boolean isNeededPhoneNumber = false;
        if (!TextUtils.isEmpty(simPhoneNumber) || simPhoneNumber != null) {
            for (String mTeleZoneCode : teleZoneCodeList) {
                if (simPhoneNumber.startsWith(mTeleZoneCode)) {
                    isNeededPhoneNumber = true;
                    break;
                }
            }
            return !isNeededPhoneNumber;
        }
        mTelephonyManager = (TelephonyManager) context.getSystemService(TelephonyManager.class);
        assert subscriptionInfo != null;
        mTelephonyManager = getTelephonyManager().createForSubscriptionId(subscriptionInfo.getSubscriptionId());
        ServiceState serviceState = getTelephonyManager().getServiceState();
        String iccidNumber = getIccidNumber();
        boolean isNeededIccid = false;
        if (!TextUtils.isEmpty(iccidNumber) || iccidNumber != null) {
            for (String mIccidStartsWith : iccidStartsWithList) {
                if (iccidNumber.startsWith(mIccidStartsWith)) {
                    isNeededIccid = true;
                    break;
                }
            }
            return !isNeededIccid;
        } else if (isRadical) {
            return true;
        }
        if (serviceState != null) {
            return serviceState.getRoaming();
        } else {
            return isRadical;
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static SubscriptionInfo getSubscriptionInfo(Context context, int i) {
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null) {
            return null;
        }
        for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
            if (subscriptionInfo.getSimSlotIndex() == i) {
                return subscriptionInfo;
            }
        }
        return null;
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS})
    public static String getSimPhoneNumber(Context context, SubscriptionInfo subscriptionInfo) {
        if (subscriptionInfo != null) {
            return getFormattedPhoneNumber(context, subscriptionInfo);
        } else {
            return null;
        }
    }

    @RequiresPermission(anyOf = {Manifest.permission.READ_PHONE_NUMBERS, "carrier privileges", "android.permission.READ_PRIVILEGED_PHONE_STATE"})
    public static String getFormattedPhoneNumber(Context context, SubscriptionInfo subscriptionInfo) {
        String str;
        if (subscriptionInfo == null) {
            return null;
        }

        try {
            SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(SubscriptionManager.class);
            str = subscriptionManager.getPhoneNumber(subscriptionInfo.getSubscriptionId());
        } catch (IllegalStateException e) {
            str = "";
        }

        if (TextUtils.isEmpty(str)) {
            return null;
        }

        try {
            @SuppressLint("publicApi") Class<?> mccTableClass = Class.forName("com.android.internal.telephony.MccTable");
            @SuppressLint({"BlockedpublicApi", "BlockedPrivateApi"}) Method countryCodeForMccMethod = mccTableClass.getDeclaredMethod("countryCodeForMcc", String.class);
            countryCodeForMccMethod.setAccessible(true);
            String countryCode = (String) countryCodeForMccMethod.invoke(null, subscriptionInfo.getMccString());
            return PhoneNumberUtils.formatNumber(str, countryCode.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static SubscriptionInfo getPhoneSubscriptionInfo(Context context, int i) {
        return SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(i);
    }

    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    public static String getIccidNumber() {
        TelephonyManager telephonyManager = getTelephonyManager();
        @SuppressLint("HardwareIds") String simSerialNumber = telephonyManager.getSimSerialNumber();
        return simSerialNumber;
    }

}
