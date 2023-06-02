package com.sevtinge.cemiuiler.module.home.folder;

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

import com.sevtinge.cemiuiler.XposedInit;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.LogUtils;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import de.robv.android.xposed.XposedHelpers;

public class FolderShade extends BaseHook {
    private static Class<?> wallpaperUtilsCls = null;
    private boolean isLight = false;

    @Override
    public void init() {
        wallpaperUtilsCls = XposedHelpers.findClassIfExists("com.miui.home.launcher.WallpaperUtils", lpparam.classLoader);

        MethodHook hook = new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                View folder = (View)param.thisObject;
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Context context = folder.getContext();
                            int opt = mPrefsMap.getStringAsInt("home_folder_shade", 0);
                            int level = mPrefsMap.getInt("home_folder_shade_level", 40);
                            final Drawable bkg;
                            if (opt == 1) {
                                PaintDrawable pd = new PaintDrawable();
                                pd.setShape(new RectShape());
                                pd.setShaderFactory(new ShapeDrawable.ShaderFactory() {
                                    @Override
                                    public Shader resize(int width, int height) {
                                        boolean isLight = false;
                                        if (wallpaperUtilsCls != null) try { isLight = (boolean)XposedHelpers.callStaticMethod(wallpaperUtilsCls, "hasAppliedLightWallpaper"); } catch (Throwable ignore) {}
                                        int bgcolor = (isLight ? 0x00ffffff : 0x00000000) | (Math.round(255 * level / 100f) * 0x1000000);
                                        return new LinearGradient(0, 0, 0, height,
                                                new int[]{ bgcolor, bgcolor, bgcolor, bgcolor },
                                                new float[]{ 0.0f, 0.25f, 0.65f, 1.0f },
                                                Shader.TileMode.CLAMP
                                        );
                                    }
                                });
                                bkg = pd;
                            } else if (opt == 2) {
                                PaintDrawable pd = new PaintDrawable();
                                pd.setShape(new RectShape());
                                pd.setShaderFactory(new ShapeDrawable.ShaderFactory() {
                                    @Override
                                    public Shader resize(int width, int height) {
                                        boolean isLight = false;
                                        if (wallpaperUtilsCls != null) try { isLight = (boolean)XposedHelpers.callStaticMethod(wallpaperUtilsCls, "hasAppliedLightWallpaper"); } catch (Throwable ignore) {}
                                        int bgcolor1 = (isLight ? 0x00ffffff : 0x00000000) | (Math.round(255 / 6f * level / 100f) * 0x1000000);
                                        int bgcolor2 = (isLight ? 0x00ffffff : 0x00000000) | (Math.round(255 * level / 100f) * 0x1000000);
                                        return new LinearGradient(0, 0, 0, height,
                                                new int[]{ bgcolor1, bgcolor2, bgcolor2, bgcolor1 },
                                                new float[]{ 0.0f, 0.25f, 0.65f, 1.0f },
                                                Shader.TileMode.CLAMP
                                        );
                                    }
                                });
                                bkg = pd;
                            } else bkg = null;
                            new Handler(context.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    XposedInit.mPrefsMap.put("prefs_key_home_folder_shade", String.valueOf(opt));
                                    XposedInit.mPrefsMap.put("prefs_key_home_folder_shade_level", level);
                                    folder.setBackground(bkg);
                                }
                            });
                        } catch (Throwable t) {
                            logE(t);
                        }
                    }
                }.start();
            }
        };

        hookAllConstructors("com.miui.home.launcher.FolderCling", hook);
        findAndHookMethod("com.miui.home.launcher.FolderCling", "onWallpaperColorChanged", hook);
        findAndHookMethod("com.miui.home.launcher.FolderCling", "updateLayout", boolean.class, hook);

        findAndHookMethod("com.miui.home.launcher.Folder", "setBackgroundAlpha", float.class, new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                int opt = mPrefsMap.getStringAsInt("home_folder_shade", 0);
                if (opt == 1) return;
                Object mLauncher = XposedHelpers.getObjectField(param.thisObject, "mLauncher");
                if (mLauncher == null) return;
                View folderCling = (View)XposedHelpers.callMethod(mLauncher, "getFolderCling");
                if (folderCling == null) return;
                Drawable bkg = folderCling.getBackground();
                if (bkg != null) bkg.setAlpha(Math.round((float)param.args[0] * 255));
            }
        });
    }
}
