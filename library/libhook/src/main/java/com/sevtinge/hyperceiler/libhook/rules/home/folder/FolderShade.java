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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.home.folder;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class FolderShade extends HomeBaseHookNew {

    private Class<?> mWallpaperUtilsCls = null;
    private boolean isLight = false;

    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        hook("com.miui.home.folder.FolderCling");
    }

    @Override
    public void initBase() {
        hook("com.miui.home.launcher.FolderCling");
    }

    private void hook(String clazz) {
        mWallpaperUtilsCls = findClassIfExists("com.miui.home.launcher.WallpaperUtils");

        IMethodHook hook = new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                View folder = (View) param.getThisObject();
                new Thread(() -> {
                    try {
                        Context context = folder.getContext();
                        int opt = Integer.parseInt(PrefsUtils.getSharedStringPrefs(context, "prefs_key_home_folder_shade", "0"));
                        int level = PrefsUtils.getSharedIntPrefs(context, "prefs_key_home_folder_shade_level", 40);

                        if (mWallpaperUtilsCls != null) {
                            try {
                                isLight = (boolean) callStaticMethod(mWallpaperUtilsCls, "hasAppliedLightWallpaper");
                            } catch (Throwable tr) {
                                XposedLog.w(TAG, getPackageName(), "isLight is abnormal", tr);
                            }
                        }

                        Drawable bkg;
                        if (opt == 1) {
                            int bgColor = (isLight ? 0x00ffffff : 0x00000000) | (Math.round(255 * level / 100f) * 0x1000000);
                            bkg = new ColorDrawable(bgColor);
                        } else if (opt == 2) {
                            bkg = getPaintDrawable(level);
                        } else {
                            bkg = null;
                        }
                        new Handler(context.getMainLooper()).post(() -> {
                            mPrefsMap.put("prefs_key_home_folder_shade", String.valueOf(opt));
                            mPrefsMap.put("prefs_key_home_folder_shade_level", level);
                            folder.setBackground(bkg);
                        });
                    } catch (Throwable t) {
                        XposedLog.w(TAG, getPackageName(), t);
                    }
                }).start();
            }
        };

        hookAllConstructors(clazz, hook);
        findAndHookMethod(clazz, "onWallpaperColorChanged", hook);
        findAndHookMethod(clazz, "updateLayout", boolean.class, hook);

        findAndHookMethod("com.miui.home.launcher.Folder", "setBackgroundAlpha", float.class, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                int opt = mPrefsMap.getStringAsInt("home_folder_shade", 0);
                Object mLauncher = getObjectField(param.getThisObject(), "mLauncher");
                View folderCling = (View) callMethod(mLauncher, "getFolderCling");
                if (opt == 1 || mLauncher == null || folderCling == null) return;
                Drawable bkg = folderCling.getBackground();
                if (bkg != null) bkg.setAlpha(Math.round((float) param.getArgs()[0] * 255));
            }
        });
    }

    @NonNull
    private PaintDrawable getPaintDrawable(int level) {
        PaintDrawable pd = new PaintDrawable();
        pd.setShape(new RectShape());
        pd.setShaderFactory(new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                int bgColor1 = (isLight ? 0x00ffffff : 0x00000000) | (Math.round(255 / 6f * level / 100f) * 0x1000000);
                int bgColor2 = (isLight ? 0x00ffffff : 0x00000000) | (Math.round(255 * level / 100f) * 0x1000000);
                return new LinearGradient(
                    0, 0, 0, height,
                    new int[]{bgColor1, bgColor2, bgColor2, bgColor1},
                    new float[]{0.0f, 0.25f, 0.65f, 1.0f},
                    Shader.TileMode.CLAMP
                );
            }
        });
        return pd;
    }
}
