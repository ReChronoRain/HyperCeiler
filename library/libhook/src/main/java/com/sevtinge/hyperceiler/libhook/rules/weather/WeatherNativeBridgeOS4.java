/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.weather;

import android.os.Parcel;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/** Independent WMS route: does not depend on Home Dock or Weather feature switches. */
public final class WeatherNativeBridgeOS4 extends BaseHook {
    private final WeatherNativeEndpointOS4 endpoint;
    public WeatherNativeBridgeOS4() { this(new WeatherNativeEndpointOS4()); }
    WeatherNativeBridgeOS4(WeatherNativeEndpointOS4 endpoint) { this.endpoint = endpoint; }
    @Override public void init() {
        findAndHookMethod("android.view.IWindowManager$Stub", "onTransact",
            int.class, Parcel.class, Parcel.class, int.class, new IMethodHook() {
                @Override public void before(HookParam param) {
                    if ((int) param.getArgs()[0] != WeatherNativeEndpointOS4.TRANSACTION_CODE) return;
                    Parcel data = (Parcel) param.getArgs()[1];
                    Parcel reply = (Parcel) param.getArgs()[2];
                    if (reply == null) { param.setResult(false); return; }
                    if (data == null) {
                        WeatherNativeEndpointOS4.writeReply(reply, null);
                        param.setResult(true);
                        return;
                    }
                    int position = data.dataPosition();
                    WeatherNativeEndpointOS4.Snapshot result = null;
                    try { result = endpoint.receive(data, (int) param.getArgs()[3]); }
                    catch (RuntimeException unavailable) { }
                    finally { data.setDataPosition(position); }
                    WeatherNativeEndpointOS4.writeReply(reply, result);
                    param.setResult(true);
                }
            });
        registerHotReloadCleanup(endpoint::retire);
        endpoint.startPrefetch();
    }
}
