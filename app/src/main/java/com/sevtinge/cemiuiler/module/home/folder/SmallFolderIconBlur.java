package com.sevtinge.cemiuiler.module.home.folder;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.BlurUtils;
import com.sevtinge.cemiuiler.utils.DisplayUtils;

import de.robv.android.xposed.XposedHelpers;

public class SmallFolderIconBlur extends BaseHook {

    boolean isShowEditPanel;
    Class<?> mLauncher;
    Class<?> mFolderInfo;
    Class<?> mFolderIcon;
    Class<?> mFolderIcon1x1;
    Class<?> mLauncherState;
    Class<?> mDragView;

    int mFolderIconSize;
    ImageView mIconImageView;

    @Override
    public void init() {

        mLauncher = findClassIfExists("com.miui.home.launcher.Launcher");
        mFolderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo");
        mFolderIcon = findClassIfExists("com.miui.home.launcher.FolderIcon");
        mFolderIcon1x1 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon1x1");
        mLauncherState = findClassIfExists("com.miui.home.launcher.LauncherState");
        mDragView = findClassIfExists("com.miui.home.launcher.DragView");

        hookAllConstructors(mFolderIcon, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                try {
                    mIconImageView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mImageView");
                } catch (Exception e) {
                    mIconImageView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mIconImageView");
                }

                FrameLayout mDockBlur = (FrameLayout) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mDockBlur");
                if (mDockBlur != null) return;
                mDockBlur = new FrameLayout(mContext);
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mDockBlur", mDockBlur);
            }
        });

        MethodHook mDockBlur = new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                try {
                    mIconImageView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mImageView");
                } catch (Exception e) {
                    mIconImageView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mIconImageView");
                }

                Context mContext = mIconImageView.getContext();
                mFolderIconSize = DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_small_folder_icon_bg_size", 56));

                FrameLayout mIconContainer = (FrameLayout) mIconImageView.getParent();
                FrameLayout mDockBlur = (FrameLayout) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mDockBlur");
                FrameLayout view = new FrameLayout(mContext);

                mIconImageView.setVisibility(View.GONE);
                mDockBlur.addView(view);
                new BlurUtils(mDockBlur, "home_small_folder_icon_bg_custom");
                mIconContainer.addView(mDockBlur, 0);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mDockBlur.getLayoutParams();
                lp.gravity = Gravity.CENTER;
                lp.height = mFolderIconSize;
                lp.width = mFolderIconSize;

                findAndHookMethod(mLauncher, "showEditPanel", boolean.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        isShowEditPanel = (boolean) param.args[0];
                        if (isShowEditPanel) {
                            mDockBlur.setVisibility(View.GONE);
                            mIconImageView.setVisibility(View.VISIBLE);
                        } else {
                            mDockBlur.setVisibility(View.VISIBLE);
                            mIconImageView.setVisibility(View.GONE);
                        }
                    }
                });

                findAndHookMethod(mLauncher, "openFolder", mFolderInfo, View.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        mDockBlur.setVisibility(View.GONE);
                    }
                });

                findAndHookMethod(mLauncher, "closeFolder", boolean.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        if (!isShowEditPanel) mDockBlur.setVisibility(View.VISIBLE);
                    }
                });

                findAndHookMethod(mLauncher, "onStateSetStart", mLauncherState, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        if (param.args[0].getClass().getSimpleName().equals("LauncherState")) {
                            mDockBlur.setVisibility(View.VISIBLE);
                        } else {
                            mDockBlur.setVisibility(View.GONE);
                        }
                    }
                });
            }
        };


        try {
            findAndHookMethod(mFolderIcon1x1, "onFinishInflate", mDockBlur);
        } catch (Exception e) {
            findAndHookMethod(mFolderIcon, "onFinishInflate", mDockBlur);
        }

        /*hookAllMethods(mDragView, "showWithAnim", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                View dragView = (View) param.thisObject;
                dragView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                Object mDragInfo = XposedHelpers.getObjectField(param.thisObject, "mDragInfo");
            }
        });*/
    }
}
