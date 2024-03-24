package com.sevtinge.hyperceiler.module.hook.systemui;

import android.graphics.drawable.Drawable;
import android.widget.SeekBar;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class SquigglyProgress extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookConstructor("com.android.systemui.media.controls.models.player.MediaViewHolder",
                android.view.View.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        SeekBar seekBar = (SeekBar) XposedHelpers.getObjectField(param.thisObject, "seekBar");
                        Object squigglyProgress = XposedHelpers.newInstance(
                                findClassIfExists("com.android.systemui.media.controls.ui.SquigglyProgress"));
                        seekBar.setProgressDrawable((Drawable) squigglyProgress);
                    }
                }
        );
    }
}
