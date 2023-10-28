package com.sevtinge.hyperceiler.module.hook.home.title;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.Helpers;

import de.robv.android.xposed.XposedHelpers;

public class IconScaleHook extends BaseHook {
    @Override
    public void init() {
        Helpers.findAndHookMethod("com.miui.home.launcher.ShortcutIcon", lpparam.classLoader, "restoreToInitState", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                ViewGroup mIconContainer = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mIconContainer");
                if (mIconContainer == null || mIconContainer.getChildAt(0) == null) return;
                float multx = (float) Math.sqrt(mPrefsMap.getInt("home_title_icon_scale", 100) / 100f);
                mIconContainer.getChildAt(0).setScaleX(multx);
                mIconContainer.getChildAt(0).setScaleY(multx);
            }
        });

        Helpers.findAndHookMethod("com.miui.home.launcher.ItemIcon", lpparam.classLoader, "onFinishInflate", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                float multx = (float) Math.sqrt(mPrefsMap.getInt("home_title_icon_scale", 100) / 100f);

                ViewGroup mIconContainer = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mIconContainer");
                if (mIconContainer != null && mIconContainer.getChildAt(0) != null) {
                    mIconContainer.getChildAt(0).setScaleX(multx);
                    mIconContainer.getChildAt(0).setScaleY(multx);
                    mIconContainer.setClipToPadding(false);
                    mIconContainer.setClipChildren(false);
                }

                if (multx > 1) {
                    final TextView mMessage = (TextView) XposedHelpers.getObjectField(param.thisObject, "mMessage");
                    if (mMessage != null)
                        mMessage.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                            }

                            @Override
                            @SuppressLint("DiscouragedApi")
                            public void afterTextChanged(Editable s) {
                                int maxWidth = mMessage.getResources().getDimensionPixelSize(mMessage.getResources().getIdentifier("icon_message_max_width", "dimen", lpparam.packageName));
                                mMessage.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST));
                                mMessage.setTranslationX(-mMessage.getMeasuredWidth() * (multx - 1) / 2f);
                                mMessage.setTranslationY(mMessage.getMeasuredHeight() * (multx - 1) / 2f);
                            }
                        });
                }

                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mMessageAnimationOrig", XposedHelpers.getObjectField(param.thisObject, "mMessageAnimation"));
                XposedHelpers.setObjectField(param.thisObject, "mMessageAnimation", (Runnable) () -> {
                    try {
                        Runnable mMessageAnimationOrig = (Runnable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mMessageAnimationOrig");
                        mMessageAnimationOrig.run();
                        boolean mIsShowMessageAnimation = XposedHelpers.getBooleanField(param.thisObject, "mIsShowMessageAnimation");
                        if (mIsShowMessageAnimation) {
                            View mMessage = (View) XposedHelpers.getObjectField(param.thisObject, "mMessage");
                            mMessage.animate().cancel();
                            mMessage.animate().scaleX(multx).scaleY(multx).setStartDelay(0).start();
                        }
                    } catch (Throwable t) {
                        logE(TAG, IconScaleHook.this.lpparam.packageName, "", t);
                    }
                });

//				if (mult <= 1) return;
//				TextView mMessage = (TextView)XposedHelpers.getObjectField(param.thisObject, "mMessage");
//				if (mMessage != null) {
//					int width = mMessage.getResources().getDimensionPixelSize(mMessage.getResources().getIdentifier("icon_message_max_width", "dimen", lpparam.packageName));
//					mMessage.setTranslationX(-width/2f * (1f - 1f / mult));
//					mMessage.setTranslationY(width/2f * (1f - 1f / mult));
//				}
            }
        });

        Helpers.findAndHookMethod("com.miui.home.launcher.ItemIcon", lpparam.classLoader, "getIconLocation", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                float multx = (float) Math.sqrt(mPrefsMap.getInt("home_title_icon_scale", 100) / 100f);
                Rect rect = (Rect) param.getResult();
                if (rect == null) return;
                rect.right = rect.left + Math.round(rect.width() * multx);
                rect.bottom = rect.top + Math.round(rect.height() * multx);
                param.setResult(rect);
            }
        });

        Helpers.findAndHookMethodSilently("com.miui.home.launcher.gadget.ClearButton", lpparam.classLoader, "onCreate", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                ViewGroup mIconContainer = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mIconContainer");
                if (mIconContainer == null || mIconContainer.getChildAt(0) == null) return;
                float multi = (float) Math.sqrt(mPrefsMap.getInt("home_title_icon_scale", 100) / 100f);
                mIconContainer.getChildAt(0).setScaleX(multi);
                mIconContainer.getChildAt(0).setScaleY(multi);
            }
        });

//		Helpers.findAndHookMethod("com.miui.home.launcher.Folder", lpparam.classLoader, "onOpen", boolean.class, new MethodHook() {
//			@Override
//			protected void after(final MethodHookParam param) throws Throwable {
//				XposedHelpers.setFloatField(param.thisObject, "mItemIconToPreviewIconScale", -1.0f);
//			}
//		});
//
//		Helpers.findAndHookMethod("com.miui.home.launcher.Folder", lpparam.classLoader, "changeItemsInFolderDuringOpenAndCloseAnimation", float.class, new MethodHook() {
//			@Override
//			protected void after(final MethodHookParam param) throws Throwable {
//				float multx = (float)Math.sqrt(MainModule.mPrefs.getInt("launcher_icon_scale", 100) / 100f);
//				ViewGroup mContent = (ViewGroup)XposedHelpers.getObjectField(param.thisObject, "mContent");
//				for (int i = 0; i < mContent.getChildCount(); i++) {
//					String cls = mContent.getChildAt(i).getClass().getSimpleName();
//					if ("ItemIcon".equals(cls) || "ShortcutIcon".equals(cls) || "FolderIcon".equals(cls)) {
//						View iconContainer = (View)XposedHelpers.callMethod(mContent.getChildAt(i), "getIconContainer");
//						float mult = (float)param.args[0] * multx;
//						iconContainer.setScaleX(mult);
//						iconContainer.setScaleY(mult);
//					}
//				}
//			}
//		});
    }
}
