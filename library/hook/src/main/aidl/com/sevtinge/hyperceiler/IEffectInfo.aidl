// IEffectInfo.aidl
package com.sevtinge.hyperceiler;

// Declare any non-default types here with import statements
import java.util.Map;

interface IEffectInfo {
    boolean isEarphoneConnection();

    Map<String,String> getEffectSupportMap();

    Map<String,String> getEffectAvailableMap();

    Map<String,String> getEffectActiveMap();

    Map<String,String> getEffectEnabledMap();

    Map<String,String> getEffectHasControlMap();
}