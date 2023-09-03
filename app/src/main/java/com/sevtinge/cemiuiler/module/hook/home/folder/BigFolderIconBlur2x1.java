package com.sevtinge.cemiuiler.module.hook.home.folder;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.BlurUtils;
import com.sevtinge.cemiuiler.utils.DisplayUtils;

import de.robv.android.xposed.XposedHelpers;

public class BigFolderIconBlur2x1 extends BaseHook {

    Class<?> mFolderIcon2x1;

    boolean isShowEditPanel;
    Class<?> mLauncher;
    Class<?> mFolderInfo;
    Class<?> mFolderIcon;
    Class<?> mLauncherState;
    Class<?> mDragView;

    @Override
    public void init() {
        mFolderIcon2x1 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon2x1");

        mLauncher = findClassIfExists("com.miui.home.launcher.Launcher");
        mFolderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo");
        mFolderIcon = findClassIfExists("com.miui.home.launcher.FolderIcon");
        mLauncherState = findClassIfExists("com.miui.home.launcher.LauncherState");
        mDragView = findClassIfExists("com.miui.home.launcher.DragView");

        MethodHook mBigFolderIconBlur = new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                int mFolderWidth = DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_big_folder_icon_bg_width_2x1", 145));
                int mFolderHeight = DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_big_folder_icon_bg_height_2x1", 62));
                ImageView mIconImageView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mIconImageView");
                FrameLayout mIconContainer = (FrameLayout) mIconImageView.getParent();
                FrameLayout mDockBlur = (FrameLayout) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mDockBlur");
                FrameLayout view = new FrameLayout(mIconImageView.getContext());

                mIconImageView.setVisibility(View.GONE);
                mDockBlur.addView(view);

                new BlurUtils(mDockBlur, "home_big_folder_icon_bg_2x1_custom");

                mIconContainer.addView(mDockBlur, 0);
                FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams) mDockBlur.getLayoutParams();
                lp1.gravity = Gravity.CENTER;
                lp1.width = mFolderWidth;
                lp1.height = mFolderHeight;
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

        findAndHookMethod(mFolderIcon2x1, "onFinishInflate", mBigFolderIconBlur);
    }
}
