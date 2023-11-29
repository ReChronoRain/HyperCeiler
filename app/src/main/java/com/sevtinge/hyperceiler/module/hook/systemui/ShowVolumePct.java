package com.sevtinge.hyperceiler.module.hook.systemui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.sevtinge.hyperceiler.utils.XposedUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ShowVolumePct extends XposedUtils {
    public static void init(ClassLoader classLoader) {
        Class<?> MiuiVolumeDialogImpl = XposedHelpers.findClassIfExists("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", classLoader);
        XposedHelpers.findAndHookMethod(MiuiVolumeDialogImpl, "showVolumeDialogH", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View mDialogView = (View) XposedHelpers.getObjectField(param.thisObject, "mDialogView");
                FrameLayout windowView = (FrameLayout) mDialogView.getParent();
                initPct(windowView, 3, windowView.getContext());
            }
        });

        XposedHelpers.findAndHookMethod(MiuiVolumeDialogImpl, "dismissH", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                removePct(mPct);
            }

        });

        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.android.systemui.miui.volume.MiuiVolumeDialogImpl$VolumeSeekBarChangeListener",
                classLoader),
            "onProgressChanged", new XC_MethodHook() {
                private int nowLevel = -233;

                @SuppressLint("SetTextI18n")
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (nowLevel == (int) param.args[1]) return;
                    int pctTag = 0;
                    if (mPct != null && mPct.getTag() != null) {
                        pctTag = (int) mPct.getTag();
                    }
                    if (pctTag != 3 || mPct == null) return;
                    Object mColumn = XposedHelpers.getObjectField(param.thisObject, "mColumn");
                    Object ss = XposedHelpers.getObjectField(mColumn, "ss");
                    if (ss == null) return;
                    if (XposedHelpers.getIntField(mColumn, "stream") == 10) return;

                    boolean fromUser = (boolean) param.args[2];
                    int currentLevel;
                    if (fromUser) {
                        currentLevel = (int) param.args[1];
                    } else {
                        ObjectAnimator anim = (ObjectAnimator) XposedHelpers.getObjectField(mColumn, "anim");
                        if (anim == null || !anim.isRunning()) return;
                        currentLevel = XposedHelpers.getIntField(mColumn, "animTargetProgress");
                    }
                    nowLevel = currentLevel;
                    mPct.setVisibility(View.VISIBLE);
                    int levelMin = XposedHelpers.getIntField(ss, "levelMin");
                    if (levelMin > 0 && currentLevel < levelMin * 1000) {
                        currentLevel = levelMin * 1000;
                    }
                    SeekBar seekBar = (SeekBar) param.args[0];
                    int max = seekBar.getMax();
                    int maxLevel = max / 1000;
                    if (currentLevel != 0) {
                        int i3 = maxLevel - 1;
                        currentLevel = currentLevel == max ? maxLevel : (currentLevel * i3 / max) + 1;
                    }
                    if (((currentLevel * 100) / maxLevel) == 100 && mPrefsMap.getBoolean("system_ui_unlock_super_volume")) mPct.setText("200%"); else mPct.setText(((currentLevel * 100) / maxLevel) + "%");
                }
            }
        );
    }
}
