package com.sevtinge.hyperceiler.home;

import java.util.List;

public class CustomOrderManager {

    private static List<Header> mCustomOrderList;

    public static void setCustomOrderList(List<Header> headers) {
        mCustomOrderList = headers;
    }

    public static List<Header> getCustomOrderList() {
        return mCustomOrderList;
    }
}
