package com.sevtinge.cemiuiler.module;

import android.os.Build;
import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.home.*;
import com.sevtinge.cemiuiler.module.home.drawer.AllAppsContainerViewBlur;
import com.sevtinge.cemiuiler.module.home.folder.BigFolderIcon;
import com.sevtinge.cemiuiler.module.home.folder.BigFolderIconBlur;
import com.sevtinge.cemiuiler.module.home.folder.BigFolderIconBlur1x2;
import com.sevtinge.cemiuiler.module.home.folder.BigFolderIconBlur2x1;
import com.sevtinge.cemiuiler.module.home.folder.BigFolderItemMaxCount;
import com.sevtinge.cemiuiler.module.home.folder.FolderAnimation;
import com.sevtinge.cemiuiler.module.home.folder.FolderAutoClose;
import com.sevtinge.cemiuiler.module.home.folder.FolderBlur;
import com.sevtinge.cemiuiler.module.home.folder.FolderColumns;
import com.sevtinge.cemiuiler.module.home.folder.FolderShade;
import com.sevtinge.cemiuiler.module.home.folder.SmallFolderIconBlur;
import com.sevtinge.cemiuiler.module.home.layout.HotSeatsHeight;
import com.sevtinge.cemiuiler.module.home.layout.HotSeatsMarginBottom;
import com.sevtinge.cemiuiler.module.home.layout.HotSeatsMarginTop;
import com.sevtinge.cemiuiler.module.home.layout.SearchBarMarginBottom;
import com.sevtinge.cemiuiler.module.home.layout.UnlockGrids;
import com.sevtinge.cemiuiler.module.home.layout.UnlockGridsNoWord;
import com.sevtinge.cemiuiler.module.home.layout.WorkspacePaddingBottom;
import com.sevtinge.cemiuiler.module.home.other.AlwaysBlurWallpaper;
import com.sevtinge.cemiuiler.module.home.other.AlwaysShowStatusClock;
import com.sevtinge.cemiuiler.module.home.widget.AllowMoveAllWidgetToMinus;
import com.sevtinge.cemiuiler.module.home.widget.AlwaysShowMiuiWidget;

public class Home extends BaseModule {

    @Override
    public void handleLoadPackage() {

        //手势
        initHook(new DoubleTap(), mPrefsMap.getInt("home_gesture_double_tap_action", 0) > 0);
        initHook(new ScreenSwipe(), mPrefsMap.getInt("home_gesture_up_swipe_action", 0) > 0 ||
                mPrefsMap.getInt("home_gesture_down_swipe_action", 0) > 0 ||
                mPrefsMap.getInt("home_gesture_up_swipe2_action", 0) > 0 ||
                mPrefsMap.getInt("home_gesture_down_swipe2_action", 0) > 0);
        initHook(new HotSeatSwipe(), mPrefsMap.getInt("home_gesture_left_swipe_action", 0) > 0 || mPrefsMap.getInt("home_gesture_right_swipe_action", 0) > 0);
        initHook(new ShakeDevice(), mPrefsMap.getInt("home_gesture_shake_action", 0) > 0);

        //布局
        initHook(new UnlockGrids(), mPrefsMap.getBoolean("home_layout_unlock_grids"));
        initHook(new UnlockGridsNoWord(), mPrefsMap.getBoolean("home_layout_unlock_grids_no_word"));
        initHook(new WorkspacePaddingBottom(), mPrefsMap.getBoolean("home_layout_workspace_padding_bottom_enable"));

        initHook(new HotSeatsHeight(), mPrefsMap.getBoolean("home_layout_hotseats_height_enable"));
        initHook(new HotSeatsMarginTop(), mPrefsMap.getBoolean("home_layout_hotseats_margin_top_enable"));
        initHook(new HotSeatsMarginBottom(), mPrefsMap.getBoolean("home_layout_hotseats_margin_bottom_enable"));
        initHook(new SearchBarMarginBottom(), mPrefsMap.getInt("home_layout_searchbar_margin_bottom", 0) > 0);


        //文件夹
        initHook(new FolderAutoClose(), mPrefsMap.getBoolean("home_folder_auto_close"));
        initHook(new FolderShade(), mPrefsMap.getStringAsInt("home_folder_shade", 1) > 0);
        initHook(new FolderColumns(), mPrefsMap.getInt("home_folder_columns", 3) > 1);
        initHook(new FolderBlur(), mPrefsMap.getBoolean("home_folder_blur"));
        initHook(new FolderAnimation(), mPrefsMap.getBoolean("home_folder_animation"));
        initHook(new SmallFolderIconBlur(), mPrefsMap.getBoolean("home_small_folder_icon_bg"));

        initHook(new BigFolderIcon(), false);
        initHook(new BigFolderIconBlur2x1(), mPrefsMap.getBoolean("home_big_folder_icon_bg_2x1"));
        initHook(new BigFolderIconBlur1x2(), mPrefsMap.getBoolean("home_big_folder_icon_bg_1x2"));
        initHook(new BigFolderIconBlur(), mPrefsMap.getBoolean("home_big_folder_icon_bg"));
        initHook(new BigFolderItemMaxCount(), mPrefsMap.getBoolean("home_big_folder_item_max_count"));

        //抽屉
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            initHook(AllAppsContainerViewBlur.INSTANCE, mPrefsMap.getBoolean("home_drawer_blur"));
        }

        //标题
        initHook(new TitleMarquee(), mPrefsMap.getBoolean("home_title_title_marquee"));
        initHook(new TitleFontSize(), mPrefsMap.getInt("home_title_font_size", 12) != 12);
        initHook(new UnlockHotseatIcon(), mPrefsMap.getBoolean("home_dock_unlock_hotseat"));

        //小部件
        initHook(new AllWidgetAnimation(), mPrefsMap.getBoolean("home_widget_all_widget_animation"));
        initHook(AlwaysShowMiuiWidget.INSTANCE, mPrefsMap.getBoolean("home_widget_show_miui_widget"));
        initHook(AllowMoveAllWidgetToMinus.INSTANCE, mPrefsMap.getBoolean("home_widget_allow_moved_to_minus_one_screen"));
        initHook(new WidgetCornerRadius(), mPrefsMap.getInt("home_widget_corner_radius", 0) > 0);

        //底栏
        initHook(new DockCustom(), mPrefsMap.getBoolean("home_dock_bg_custom_enable"));
        initHook(new SeekPoints(), mPrefsMap.getStringAsInt("home_other_seek_points", 0) > 0);

        //其他
        initHook(new HomeMode(), mPrefsMap.getStringAsInt("home_other_home_mode", 0) > 0);
        initHook(AlwaysShowStatusClock.INSTANCE, mPrefsMap.getBoolean("home_other_show_clock"));
        initHook(new InfiniteScroll(), mPrefsMap.getBoolean("home_other_infinite_scroll"));
        initHook(new FreeformShortcutMenu(), mPrefsMap.getBoolean("home_other_freeform_shortcut_menu"));
        initHook(new UserPresentAnimation(), mPrefsMap.getBoolean("home_other_user_present_animation"));
        initHook(new PerfectIcon(), mPrefsMap.getBoolean("home_other_perfect_icon"));
        initHook(new EnableIconMonoChrome(), mPrefsMap.getBoolean("home_other_icon_mono_chrome"));
        initHook(EnableIconMonetColor.INSTANCE, mPrefsMap.getBoolean("home_other_icon_monet_color"));
        initHook(new HomePortraitReverse(), mPrefsMap.getBoolean("home_other_portrait_reverse"));
        initHook(AlwaysBlurWallpaper.INSTANCE, mPrefsMap.getBoolean("home_other_always_blur_launcher_wallpaper"));

        initHook(new ShowAllHideApp());

        //实验性功能
        initHook(new ShortcutBackgoundBlur(), mPrefsMap.getBoolean("home_other_shortcut_backgound_blur"));
        initHook(new FoldDock(), mPrefsMap.getBoolean("home_other_fold_dock"));
        initHook(new AllAppsBlur(), true);
        initHook(new LargeIconCornerRadius(), true);

        //多小窗
        initHook(new FreeFormCountForHome(), mPrefsMap.getBoolean("system_framework_freeform_count"));
        initHook(new MaxFreeFormm(), mPrefsMap.getBoolean("system_framework_freeform_count"));

        //Fold2样式负一屏
        initHook(new OverlapMode(), mPrefsMap.getBoolean("personal_assistant_overlap_mode"));

        //Other
        initHook(new StickyFloatingWindowsForHome(), mPrefsMap.getBoolean("system_framework_freeform_sticky"));

        //小部件桌面下载
        initHook(new WidgetCrack(), mPrefsMap.getBoolean("hidden_function") && mPrefsMap.getBoolean("personal_assistant_widget_crack"));
    }


    /*public static void CleanShareMenuHook() {
        Helpers.hookAllMethods("miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner", null, "run", new Helpers.MethodHook() {
            @Override
            protected void after(XC_MethodHook.MethodHookParam param) throws Throwable {
                Intent mOriginalIntent = (Intent) XposedHelpers.getObjectField(param.thisObject, "mOriginalIntent");
                if (mOriginalIntent == null) return;
                String action = mOriginalIntent.getAction();
                if (action == null) return;
                if (!action.equals(Intent.ACTION_SEND) && !action.equals(Intent.ACTION_SENDTO) && !action.equals(Intent.ACTION_SEND_MULTIPLE)) return;
                if (mOriginalIntent.getDataString() != null && mOriginalIntent.getDataString().contains(":")) return;

                Context mContext = (Context)XposedHelpers.getObjectField(param.thisObject, "mContext");
                String mAimPackageName = (String)XposedHelpers.getObjectField(param.thisObject, "mAimPackageName");
                if (mContext == null || mAimPackageName == null) return;
                Set<String> selectedApps = PrefsUtils.getSharedStringSetPrefs(mContext, "prefs_key_system_clean_share_apps");
                View mRootView = (View)XposedHelpers.getObjectField(param.thisObject, "mRootView");
                int appResId1 = mContext.getResources().getIdentifier("app1", "id", "android.miui");
                int appResId2 = mContext.getResources().getIdentifier("app2", "id", "android.miui");
                boolean removeOriginal = selectedApps.contains(mAimPackageName) || selectedApps.contains(mAimPackageName + "|0");
                boolean removeDual = selectedApps.contains(mAimPackageName + "|999");
                View originalApp = mRootView.findViewById(appResId1);
                View dualApp = mRootView.findViewById(appResId2);
                if (removeOriginal)dualApp.performClick();
                else if (removeDual) originalApp.performClick();
            }
        });
    }*/
}
