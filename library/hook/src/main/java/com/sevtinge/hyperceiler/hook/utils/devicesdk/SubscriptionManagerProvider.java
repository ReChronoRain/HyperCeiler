package com.sevtinge.hyperceiler.hook.utils.devicesdk;

import android.content.Context;

import com.sevtinge.hyperceiler.hook.utils.InvokeUtils;

public class SubscriptionManagerProvider {
    private static final String CLASS = "android.telephony.SubscriptionManager";

    private final Object subscriptionManager;

    public SubscriptionManagerProvider(Context context) {
        subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    public int[] getActiveSubscriptionIdList(boolean visibleOnly) {
        return InvokeUtils.callMethod(CLASS, subscriptionManager, "getActiveSubscriptionIdList", new Class[]{boolean.class}, visibleOnly);
    }
}
