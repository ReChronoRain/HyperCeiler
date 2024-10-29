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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.systemui.plugin;

import static com.sevtinge.hyperceiler.module.base.tool.HookTool.getObjectFieldSilently;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.tool.HookTool;

import de.robv.android.xposed.XposedHelpers;

public class VolumeTimerValuesHook {
    public static void initVolumeTimerValuesHook(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("com.android.systemui.miui.volume.MiuiVolumeTimerDrawableHelper", classLoader, "initTimerString", new HookTool.MethodHook() {
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

        XposedHelpers.findAndHookMethod("com.android.systemui.miui.volume.TimerItem", classLoader, "getTimePos", int.class, new HookTool.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object timer = XposedHelpers.getObjectField(param.thisObject, "mTimerTime");
                float halfTimerWidth = ((int) XposedHelpers.callMethod(timer, "getWidth")) / 2.0f;
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                Object mTimerSeekbarWidth = getObjectFieldSilently(param.thisObject, "mTimerSeekbarWidth");
                int seekbarWidthResId;
                if ("ObjectFieldNotExist".equals(mTimerSeekbarWidth)) {
                    seekbarWidthResId = mContext.getResources().getIdentifier("miui_volume_timer_seelbar_width", "dimen", "miui.systemui.plugin");
                } else {
                    seekbarWidthResId = (int) mTimerSeekbarWidth;
                }
                int mTimerSeekbarMarginLeft = mContext.getResources().getIdentifier("miui_volume_timer_seekbar_margin_left", "dimen", "miui.systemui.plugin");
                float seekWidth = mContext.getResources().getDimension(seekbarWidthResId);
                int marginLeft = mContext.getResources().getDimensionPixelSize(mTimerSeekbarMarginLeft);
                int seg = (int) XposedHelpers.getObjectField(param.thisObject, "mDeterminedSegment");
                param.setResult(seekWidth / 10 * seg + marginLeft - halfTimerWidth);
            }
        });

        XposedHelpers.findAndHookMethod("com.android.systemui.miui.volume.MiuiVolumeTimerDrawableHelper", classLoader, "updateDrawables", new HookTool.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                int mCurrentSegment = XposedHelpers.getIntField(param.thisObject, "mCurrentSegment");
                if (mCurrentSegment < 3 || (mCurrentSegment == 3 && XposedHelpers.getIntField(param.thisObject, "mDeterminedSegment") == 3)) {
                    XposedHelpers.setIntField(param.thisObject, "mCurrentSegment", 0);
                }
                param.setObjectExtra("originalValue", mCurrentSegment);
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.setIntField(param.thisObject, "mCurrentSegment", (int) param.getObjectExtra("originalValue"));
            }
        });
    }
}
