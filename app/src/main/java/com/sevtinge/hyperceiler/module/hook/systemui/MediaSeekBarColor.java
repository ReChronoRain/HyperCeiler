package com.sevtinge.hyperceiler.module.hook.systemui;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.widget.SeekBar;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class MediaSeekBarColor extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        int progressColor = mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_color", -1);
        int thumbColor = mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_thumb_color", -1);
        findAndHookMethod("com.android.systemui.media.controls.models.player.SeekBarObserver",
                "onChanged", Object.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Object holder = XposedHelpers.getObjectField(param.thisObject, "holder");
                        SeekBar seekBar = (SeekBar) XposedHelpers.getObjectField(holder, "seekBar");
                        if (progressColor != -1)
                            seekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(progressColor, PorterDuff.Mode.SRC_IN));
                        if (thumbColor != -1)
                            seekBar.getThumb().setColorFilter(new PorterDuffColorFilter(thumbColor, PorterDuff.Mode.SRC_IN));
                    }
                }
        );
    }
}
