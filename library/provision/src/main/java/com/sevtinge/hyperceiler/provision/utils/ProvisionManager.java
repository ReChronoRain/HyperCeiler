package com.sevtinge.hyperceiler.provision.utils;

public class ProvisionManager {

    private static NoticeProvider provider;

    public static void setProvider(NoticeProvider p) {
        provider = p;
    }

    public static NoticeProvider getProvider() {
        return provider;
    }
}
