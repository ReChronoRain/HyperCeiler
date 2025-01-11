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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.widget.ImageButton;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.UserHandle;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class MediaButton extends BaseHook {
    Class<?> MediaControlPanel;

    private final int type = mPrefsMap.getInt("system_ui_control_center_media_control_media_button", 140);
    private final int typeCustom = mPrefsMap.getInt("system_ui_control_center_media_control_media_button_custom", 140);

    @Override
    public void init() throws NoSuchMethodException {

        if (isMoreHyperOSVersion(2f)) {
            MediaControlPanel = findClassIfExists("com.android.systemui.media.controls.ui.controller.MediaControlPanel", lpparam.classLoader);
        } else {
            MediaControlPanel = findClassIfExists("com.android.systemui.media.controls.ui.MediaControlPanel", lpparam.classLoader);
        }
        Class<?> DrawableUtils = findClassIfExists("com.miui.utils.DrawableUtils", lpparam.classLoader);

        hookAllMethods(MediaControlPanel, "bindButtonCommon", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object mediaAction = param.args[1];
                String desc = (String) XposedHelpers.getObjectField(mediaAction, "contentDescription");
                if ((typeCustom != 140) && 
                    !desc.contains("Play") && !desc.contains("Pause") && 
                    !desc.contains("Previous track") && !desc.contains("Next track")) {
                    ImageButton button = (ImageButton) param.args[0];
                    Drawable loadDrawable = (Drawable) XposedHelpers.getObjectField(mediaAction, "icon");
                    Method method = DrawableUtils.getDeclaredMethod("drawable2Bitmap", Drawable.class);
                    Bitmap bitmap = (Bitmap) method.invoke(null, loadDrawable);
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, typeCustom, typeCustom, true);
                    XposedHelpers.setObjectField(mediaAction, "icon", new BitmapDrawable(button.getContext().getResources(), scaledBitmap));
                } else if (type != 140) {
                    ImageButton button = (ImageButton) param.args[0];
                    Drawable loadDrawable = (Drawable) XposedHelpers.getObjectField(mediaAction, "icon");
                    Method method = DrawableUtils.getDeclaredMethod("drawable2Bitmap", Drawable.class);
                    Bitmap bitmap = (Bitmap) method.invoke(null, loadDrawable);
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, type, type, true);
                    XposedHelpers.setObjectField(mediaAction, "icon", new BitmapDrawable(button.getContext().getResources(), scaledBitmap));
                }
            }
        });

        /* HyperOS 1.0
        findAndHookMethod("com.android.systemui.media.controls.pipeline.MediaDataManager",
                "createActionsFromState", String.class, MediaController.class, UserHandle.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        pkg = (String) param.args[0];
                    }
                }
        );

        findAndHookMethod("com.android.systemui.media.controls.pipeline.MediaDataManager$createActionsFromState$customActions$1",
                "invoke", Object.class, new MethodHook() {

                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        if (typeCustom != 140) {
                            PlaybackState.CustomAction customAction = (PlaybackState.CustomAction) param.args[0];
                            Object MediaAction = param.getResult();
                            Object mediaDataManager = XposedHelpers.getObjectField(param.thisObject, "this$0");
                            Context context = (Context) XposedHelpers.getObjectField(mediaDataManager, "context");
                            Icon createWithResource = Icon.createWithResource(pkg, customAction.getIcon());
                            Drawable loadDrawable = createWithResource.loadDrawable(context);
                            Class<?> DrawableUtils = findClassIfExists("com.miui.utils.DrawableUtils", lpparam.classLoader);
                            Method method = DrawableUtils.getDeclaredMethod("drawable2Bitmap", Drawable.class);
                            Bitmap bitmap = (Bitmap) method.invoke(null, loadDrawable);
                            loadDrawable = new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap,
                                    typeCustom, typeCustom, true));
                            XposedHelpers.setObjectField(MediaAction, "icon", loadDrawable);
                        }
                    }
                }
        );

        findAndHookMethod("com.android.systemui.media.controls.pipeline.MediaDataManager",
                "getStandardAction", MediaController.class, long.class, long.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        if (type != 140) {
                            Object MediaAction = param.getResult();
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "context");
                            Drawable drawable = (Drawable) XposedHelpers.getObjectField(MediaAction, "icon");
                            Class<?> DrawableUtils = findClassIfExists("com.miui.utils.DrawableUtils", lpparam.classLoader);
                            Method method = DrawableUtils.getDeclaredMethod("drawable2Bitmap", Drawable.class);
                            Bitmap bitmap = (Bitmap) method.invoke(null, drawable);
                            drawable = new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap,
                                    type, type, true));
                            XposedHelpers.setObjectField(MediaAction, "icon", drawable);
                        }
                    }
                }
        ); 
        */
    }
}
