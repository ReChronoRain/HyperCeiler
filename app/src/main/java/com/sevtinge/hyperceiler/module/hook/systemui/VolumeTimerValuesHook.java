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
package com.sevtinge.hyperceiler.module.hook.systemui;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.module.base.BaseHook;

public class VolumeTimerValuesHook extends BaseHook {

    private static ClassLoader pluginLoader = null;

    @Override
    public void init() {
        VolumeTimerValuesRes();

        /*final boolean[] isHooked = {false};
        findAndHookMethod("com.android.systemui.shared.plugins.PluginManagerImpl", lpparam.classLoader, "getClassLoader", ApplicationInfo.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo appInfo = (ApplicationInfo) param.args[0];
                if ("miui.systemui.plugin".equals(appInfo.packageName) && !isHooked[0]) {
                    isHooked[0] = true;
                    if (pluginLoader == null) {
                        pluginLoader = (ClassLoader) param.getResult();
                    }

                    findAndHookMethod("com.android.systemui.miui.volume.MiuiVolumeTimerDrawableHelper", pluginLoader, "initTimerString", new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            String[] mTimeSegmentTitle = new String[11];
                            int timerOffId = mContext.getResources().getIdentifier("timer_off", "string", "miui.systemui.plugin");
                            int minuteId = mContext.getResources().getIdentifier("timer_30_minutes", "string", "miui.systemui.plugin");
                            int hourId = mContext.getResources().getIdentifier("timer_1_hour", "string", "miui.systemui.plugin");
                            mTimeSegmentTitle[0] = mContext.getResources().getString(timerOffId);
                            mTimeSegmentTitle[1] = mContext.getResources().getString(minuteId, 30);
                            mTimeSegmentTitle[2] = mContext.getResources().getString(hourId, 1);
                            mTimeSegmentTitle[3] = mContext.getResources().getString(hourId, 2);
                            mTimeSegmentTitle[4] = mContext.getResources().getString(hourId, 3);
                            mTimeSegmentTitle[5] = mContext.getResources().getString(hourId, 4);
                            mTimeSegmentTitle[6] = mContext.getResources().getString(hourId, 5);
                            mTimeSegmentTitle[7] = mContext.getResources().getString(hourId, 6);
                            mTimeSegmentTitle[8] = mContext.getResources().getString(hourId, 8);
                            mTimeSegmentTitle[9] = mContext.getResources().getString(hourId, 10);
                            mTimeSegmentTitle[10] = mContext.getResources().getString(hourId, 12);
                            XposedHelpers.setObjectField(param.thisObject, "mTimeSegmentTitle", mTimeSegmentTitle);
                        }
                    });
                    findAndHookMethod("com.android.systemui.miui.volume.TimerItem", pluginLoader, "getTimePos", int.class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            Object timer = XposedHelpers.getObjectField(param.thisObject, "mTimerTime");
                            float halfTimerWidth = ((int) XposedHelpers.callMethod(timer, "getWidth")) / 2.0f;
                            Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            float seekWidth = mContext.getResources().getDimension(mContext.getResources().getIdentifier("miui_volume_timer_seelbar_width", "dimen", "miui.systemui.plugin"));
                            int marginLeft = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("miui_volume_timer_seekbar_margin_left", "dimen", "miui.systemui.plugin"));
                            int seg = (int) XposedHelpers.getObjectField(param.thisObject, "mDeterminedSegment");
                            param.setResult(seekWidth / 10 * seg + marginLeft - halfTimerWidth);
                        }
                    });
                    findAndHookMethod("com.android.systemui.miui.volume.TimerItem", pluginLoader, "updateDrawables", int.class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            int mCurrentSegment = XposedHelpers.getIntField(param.thisObject, "mCurrentSegment");
                            param.setObjectExtra("originalValue", mCurrentSegment);
                            if (mCurrentSegment < 3 || (mCurrentSegment == 3 && XposedHelpers.getIntField(param.thisObject, "mDeterminedSegment") == 3)) {
                                XposedHelpers.setIntField(param.thisObject, "mCurrentSegment", 0);
                                param.setResult(null);
                            }
                            param.setResult(null);
                        }

                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            XposedHelpers.setIntField(param.thisObject, "mCurrentSegment", (int) param.getObjectExtra("originalValue"));
                        }
                    });
                }
            }
        });*/

    }

    public static void VolumeTimerValuesRes() {
        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "array", "miui_volume_timer_segments", R.array.miui_volume_timer_segments);
    }
}
