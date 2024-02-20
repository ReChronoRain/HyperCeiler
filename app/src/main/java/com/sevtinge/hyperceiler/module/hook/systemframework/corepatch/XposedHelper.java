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
package com.sevtinge.hyperceiler.module.hook.systemframework.corepatch;

import static com.sevtinge.hyperceiler.module.app.SystemFrameworkForCorePatch.TAG;

import android.util.Log;

import com.sevtinge.hyperceiler.BuildConfig;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class XposedHelper {
    public final String SIGNATURE = "308203c6308202aea003020102021426d148b7c65944abcf3a683b4c3dd3b139c4ec85300d06092a864886f70d01010b05003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964301e170d3139303130323138353233385a170d3439303130323138353233385a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820122300d06092a864886f70d01010105000382010f003082010a028201010087fcde48d9beaeba37b733a397ae586fb42b6c3f4ce758dc3ef1327754a049b58f738664ece587994f1c6362f98c9be5fe82c72177260c390781f74a10a8a6f05a6b5ca0c7c5826e15526d8d7f0e74f2170064896b0cf32634a388e1a975ed6bab10744d9b371cba85069834bf098f1de0205cdee8e715759d302a64d248067a15b9beea11b61305e367ac71b1a898bf2eec7342109c9c5813a579d8a1b3e6a3fe290ea82e27fdba748a663f73cca5807cff1e4ad6f3ccca7c02945926a47279d1159599d4ecf01c9d0b62e385c6320a7a1e4ddc9833f237e814b34024b9ad108a5b00786ea15593a50ca7987cbbdc203c096eed5ff4bf8a63d27d33ecc963990203010001a350304e300c0603551d13040530030101ff301d0603551d0e04160414a361efb002034d596c3a60ad7b0332012a16aee3301f0603551d23041830168014a361efb002034d596c3a60ad7b0332012a16aee3300d06092a864886f70d01010b0500038201010022ccb684a7a8706f3ee7c81d6750fd662bf39f84805862040b625ddf378eeefae5a4f1f283deea61a3c7f8e7963fd745415153a531912b82b596e7409287ba26fb80cedba18f22ae3d987466e1fdd88e440402b2ea2819db5392cadee501350e81b8791675ea1a2ed7ef7696dff273f13fb742bb9625fa12ce9c2cb0b7b3d94b21792f1252b1d9e4f7012cb341b62ff556e6864b40927e942065d8f0f51273fcda979b8832dd5562c79acf719de6be5aee2a85f89265b071bf38339e2d31041bc501d5e0c034ab1cd9c64353b10ee70b49274093d13f733eb9d3543140814c72f8e003f301c7a00b1872cc008ad55e26df2e8f07441002c4bcb7dc746745f0db";

    public static void findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            if (findClass(className, classLoader) != null) {
                XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
            }
        } catch (Throwable e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": " + Log.getStackTraceString(e));
        }
    }
    public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            if (clazz != null) {
                XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            }
        } catch (Throwable e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": " + Log.getStackTraceString(e));
        }
    }
    public static void hookAllMethods(String className, ClassLoader classLoader, String methodName, XC_MethodHook callback) {
        try {
            Class<?> packageParser = findClass(className, classLoader);
            XposedBridge.hookAllMethods(packageParser, methodName, callback);
        } catch (Throwable e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": " + Log.getStackTraceString(e));
        }

    }

    public void hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        try {
            XposedBridge.hookAllMethods(hookClass, methodName, callback);
        } catch (Throwable e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": " + Log.getStackTraceString(e));
        }
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (Throwable e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": " + Log.getStackTraceString(e));
        }
        return null;
    }

    public static void hookAllConstructors(String className, XC_MethodHook callback) {
        try {
            Class<?> packageParser = findClass(className, null);
            XposedBridge.hookAllConstructors(packageParser, callback);
        } catch (Throwable e) {
            if (BuildConfig.DEBUG)
                XposedBridge.log("[HyperCeiler][E][android]" + TAG + ": " + Log.getStackTraceString(e));
        }
    }
}
