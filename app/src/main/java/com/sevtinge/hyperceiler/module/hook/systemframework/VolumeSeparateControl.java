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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemframework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.os.Build;
import android.provider.Settings;
import android.util.SparseIntArray;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

public class VolumeSeparateControl extends BaseHook {

    Class<?> mAudioService;

    @Override
    public void init() {

        mAudioService = findClassIfExists("com.android.server.audio.AudioService");

        findAndHookMethod(mAudioService, "updateStreamVolumeAlias", boolean.class, String.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                int[] mStreamVolumeAlias =
                    (int[]) (isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ?
                        XposedHelpers.getStaticObjectField(mAudioService, "mStreamVolumeAlias") :
                        XposedHelpers.getObjectField(param.thisObject, "mStreamVolumeAlias"));
                mStreamVolumeAlias[1] = 1;
                mStreamVolumeAlias[5] = 5;

                if (isAndroidVersion(33)) {
                    XposedHelpers.setStaticObjectField(mAudioService, "mStreamVolumeAlias", mStreamVolumeAlias);
                } else {
                    XposedHelpers.setObjectField(param.thisObject, "mStreamVolumeAlias", mStreamVolumeAlias);
                }
            }
        });

        findAndHookMethod("com.android.server.audio.AudioService$VolumeStreamState", "readSettings", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {

                int mStreamType = XposedHelpers.getIntField(param.thisObject, "mStreamType");
                if (mStreamType != 1) return;

                synchronized (param.method.getDeclaringClass()) {
                    Class<?> audioSystem = XposedHelpers.findClassIfExists("android.media.AudioSystem", lpparam.classLoader);
                    Set<Integer> DEVICE_OUT_ALL = (Set<Integer>) XposedHelpers.getStaticObjectField(audioSystem, "DEVICE_OUT_ALL_SET");
                    int DEVICE_OUT_DEFAULT = XposedHelpers.getStaticIntField(audioSystem, "DEVICE_OUT_DEFAULT");
                    int[] DEFAULT_STREAM_VOLUME = (int[]) XposedHelpers.getStaticObjectField(audioSystem, "DEFAULT_STREAM_VOLUME");
                    Object mContentResolver = XposedHelpers.getObjectField(XposedHelpers.getSurroundingThis(param.thisObject), "mContentResolver");
                    SparseIntArray mIndexMap = (SparseIntArray) XposedHelpers.getObjectField(param.thisObject, "mIndexMap");

                    for (Integer deviceType : DEVICE_OUT_ALL) {
                        int device = deviceType;
                        String name = (String) XposedHelpers.callMethod(param.thisObject, "getSettingNameForDevice", device);
                        int index = (int) XposedHelpers.callStaticMethod(Settings.System.class, "getIntForUser", mContentResolver, name, device == DEVICE_OUT_DEFAULT ? DEFAULT_STREAM_VOLUME[mStreamType] : -1, -2);
                        if (index != -1) {
                            mIndexMap.put(device, (int) XposedHelpers.callMethod(param.thisObject, "getValidIndex", 10 * index, true));
                        }
                    }
                    XposedHelpers.setObjectField(param.thisObject, "mIndexMap", mIndexMap);
                }
                param.setResult(null);
            }
        });

        findAndHookMethodSilently(mAudioService, "shouldZenMuteStream", int.class, new MethodHook() {
            protected void after(MethodHookParam param) throws Throwable {
                int mStreamType = (int) param.args[0];
                if (mStreamType == 5 && !(boolean) param.getResult()) {
                    int mZenMode = (int) XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mNm"), "getZenMode");
                    if (mZenMode == 1) param.setResult(true);
                }
            }
        });
    }
}
