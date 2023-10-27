package com.sevtinge.hyperceiler.callback;

public interface IAppSelectCallback {

    void sendMsgToActivity(byte[] appIcon, String appName, String appPackageName, String appVersion, String appActivityName);

    String getMsgFromActivity(String s);
}
