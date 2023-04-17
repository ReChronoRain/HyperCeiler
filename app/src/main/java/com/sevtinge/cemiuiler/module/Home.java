package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.home.*;
import com.sevtinge.cemiuiler.module.home.dock.*;
import com.sevtinge.cemiuiler.module.home.drawer.AllAppsContainerViewBlur;
import com.sevtinge.cemiuiler.module.home.drawer.AppDrawer;
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
import com.sevtinge.cemiuiler.module.home.other.*;
import com.sevtinge.cemiuiler.module.home.recent.*;
import com.sevtinge.cemiuiler.module.home.title.*;
import com.sevtinge.cemiuiler.module.home.widget.AllWidgetAnimation;
import com.sevtinge.cemiuiler.module.home.widget.AllowMoveAllWidgetToMinus;
import com.sevtinge.cemiuiler.module.home.widget.AlwaysShowMiuiWidget;
import com.sevtinge.cemiuiler.module.home.widget.HideWidgetTitles;
import com.sevtinge.cemiuiler.module.home.SetDeviceLevel;

import java.util.Objects;

public class Home extends BaseModule {

    @Override
    public void handleLoadPackage() {

        //手势
        initHook(new DoubleTap(), mPrefsMap.getInt("home_gesture_double_tap_action", 0) > 0);
        initHook(new ScreenSwipe(), mPrefsMap.getInt("home_gesture_up_swipe_action", 0) > 0 ||
                mPrefsMap.getInt("home_gesture_down_swipe_action", 0) > 0 ||
                mPrefsMap.getInt("home_gesture_up_swipe2_action", 0) > 0 ||
                mPrefsMap.getInt("home_gesture_down_swipe2_action", 0) > 0);
        initHook(new HotSeatSwipe(), mPrefsMap.getInt("home_gesture_left_swipe_action", 0) > 0
                || mPrefsMap.getInt("home_gesture_right_swipe_action", 0) > 0);
        initHook(new ShakeDevice(), mPrefsMap.getInt("home_gesture_shake_action", 0) > 0);

        //布局
        initHook(new UnlockGrids(), mPrefsMap.getBoolean("home_layout_unlock_grids"));
        initHook(new UnlockGridsNoWord(), mPrefsMap.getBoolean("home_layout_unlock_grids_no_word"));
        initHook(new WorkspacePaddingBottom(), mPrefsMap.getBoolean("home_layout_workspace_padding_bottom_enable"));

        initHook(new HotSeatsHeight(), mPrefsMap.getBoolean("home_layout_hotseats_height_enable"));
        initHook(new HotSeatsMarginTop(), mPrefsMap.getBoolean("home_layout_hotseats_margin_top_enable"));
        initHook(new HotSeatsMarginBottom(), mPrefsMap.getBoolean("home_layout_hotseats_margin_bottom_enable"));
        initHook(new SearchBarMarginBottom(), (mPrefsMap.getInt("home_layout_searchbar_margin_bottom", 0) > 0) &&
                mPrefsMap.getBoolean("home_layout_searchbar_margin_bottom_enable"));


        //文件夹
        initHook(FolderAutoClose.INSTANCE, mPrefsMap.getBoolean("home_folder_auto_close"));
        initHook(new FolderShade(), mPrefsMap.getStringAsInt("home_folder_shade", 1) > 0);
        initHook(FolderColumns.INSTANCE, mPrefsMap.getInt("home_folder_columns", 3) != 3 ||
                mPrefsMap.getBoolean("home_folder_width"));
        initHook(FolderBlur.INSTANCE, mPrefsMap.getBoolean("home_folder_blur"));
        initHook(new FolderAnimation(), mPrefsMap.getBoolean("home_folder_animation"));
        initHook(new SmallFolderIconBlur(), mPrefsMap.getBoolean("home_small_folder_icon_bg"));

        initHook(new BigFolderIcon(), false);
        initHook(new BigFolderIconBlur2x1(), mPrefsMap.getBoolean("home_big_folder_icon_bg_2x1"));
        initHook(new BigFolderIconBlur1x2(), mPrefsMap.getBoolean("home_big_folder_icon_bg_1x2"));
        initHook(new BigFolderIconBlur(), mPrefsMap.getBoolean("home_big_folder_icon_bg"));
        initHook(new BigFolderItemMaxCount(), mPrefsMap.getBoolean("home_big_folder_item_max_count"));

        //抽屉
        initHook(AppDrawer.INSTANCE, mPrefsMap.getBoolean("home_drawer_all") &&
                mPrefsMap.getBoolean("home_drawer_editor"));
        initHook(AllAppsContainerViewBlur.INSTANCE, mPrefsMap.getBoolean("home_drawer_blur"));

        //最近任务
        initHook(BlurLevel.INSTANCE, mPrefsMap.getStringAsInt("home_recent_blur_level", 6) != 6);
        initHook(DisableRecentViewWallpaperDarken.INSTANCE, mPrefsMap.getBoolean("home_recent_disable_wallpaper_dimming"));
        initHook(HideStatusBarWhenEnterRecent.INSTANCE, true);
        initHook(RemoveCardAnim.INSTANCE, mPrefsMap.getBoolean("home_recent_modify_animation"));
        initHook(TaskViewHorizontal.INSTANCE);
        initHook(TaskViewVertical.INSTANCE);
        initHook(HideFreeform.INSTANCE, mPrefsMap.getBoolean("home_recent_hide_freeform"));
        initHook(HideCleanUp.INSTANCE, mPrefsMap.getBoolean("home_recent_hide_clean_up"));
        initHook(FreeformCardBackgroundColor.INSTANCE);
        initHook(CardTextColor.INSTANCE);
        initHook(CardTextSize.INSTANCE);
        initHook(RecentText.INSTANCE, !Objects.equals(mPrefsMap.getString("home_recent_text", ""), ""));
        initHook(RemoveIcon.INSTANCE, mPrefsMap.getBoolean("home_recent_remove_icon"));
        initHook(RecentResource.INSTANCE);
        initHook(RealMemory.INSTANCE, mPrefsMap.getBoolean("home_recent_show_real_memory"));

        //图标
        initHook(BigIconCorner.INSTANCE, mPrefsMap.getBoolean("home_title_big_icon_corner"));
        initHook(new DownloadAnimation(), mPrefsMap.getBoolean("home_title_download_animation"));

        //标题
        initHook(new TitleMarquee(), mPrefsMap.getBoolean("home_title_title_marquee"));
        initHook(new TitleFontSize(), mPrefsMap.getInt("home_title_font_size", 12) != 12);
        initHook(IconTitleColor.INSTANCE);
        initHook(new UnlockHotseatIcon(), mPrefsMap.getBoolean("home_dock_unlock_hotseat"));

        //小部件
        initHook(new AllWidgetAnimation(), mPrefsMap.getBoolean("home_widget_all_widget_animation"));
        initHook(AlwaysShowMiuiWidget.INSTANCE, mPrefsMap.getBoolean("home_widget_show_miui_widget"));
        initHook(AllowMoveAllWidgetToMinus.INSTANCE, mPrefsMap.getBoolean("home_widget_allow_moved_to_minus_one_screen"));
        initHook(new WidgetCornerRadius(), mPrefsMap.getInt("home_widget_corner_radius", 0) > 0);
        initHook(HideWidgetTitles.INSTANCE, mPrefsMap.getBoolean("home_widget_hide_title"));

        //底栏
        initHook(new DockCustom(), mPrefsMap.getBoolean("home_dock_bg_custom_enable"));
        initHook(new SeekPoints(), mPrefsMap.getStringAsInt("home_other_seek_points", 0) > 0);
        initHook(FoldDeviceDock.INSTANCE, mPrefsMap.getBoolean("home_dock_fold"));
        initHook(HideSeekPoint.INSTANCE, mPrefsMap.getBoolean("home_dock_hide_seekpoint"));
        initHook(ShowDockIconTitle.INSTANCE, mPrefsMap.getBoolean("home_dock_icon_title"));

        //其他
        initHook(new HomeMode(), mPrefsMap.getStringAsInt("home_other_home_mode", 0) > 0);
        initHook(AlwaysShowStatusClock.INSTANCE, mPrefsMap.getBoolean("home_other_show_clock"));
        initHook(new InfiniteScroll(), mPrefsMap.getBoolean("home_other_infinite_scroll"));
        initHook(new FreeformShortcutMenu(), mPrefsMap.getBoolean("home_other_freeform_shortcut_menu"));
        initHook(new UserPresentAnimation(), mPrefsMap.getBoolean("home_other_user_present_animation"));
        initHook(new PerfectIcon(), mPrefsMap.getBoolean("home_other_perfect_icon"));
        initHook(new EnableIconMonoChrome(), mPrefsMap.getBoolean("home_other_icon_mono_chrome"));
        initHook(new HomePortraitReverse(), mPrefsMap.getBoolean("home_other_portrait_reverse"));
        initHook(AlwaysBlurWallpaper.INSTANCE, mPrefsMap.getBoolean("home_other_always_blur_launcher_wallpaper"));
        initHook(BlurRadius.INSTANCE, mPrefsMap.getInt("home_other_blur_radius", 100) != 100);
        initHook(ShortcutItemCount.INSTANCE, mPrefsMap.getBoolean("home_other_shortcut_remove_restrictions"));
        initHook(DisableHideGoogle.INSTANCE, mPrefsMap.getBoolean("home_other_disable_hide_google"));
        initHook(new ShowAllHideApp());
        initHook(FixAndroidRS.INSTANCE, mPrefsMap.getBoolean("home_other_fix_android_r_s"));

        //实验性功能
        initHook(ShortcutBackgroundBlur.INSTANCE, mPrefsMap.getBoolean("home_other_shortcut_background_blur"));
        initHook(new FoldDock(), mPrefsMap.getBoolean("home_other_fold_dock"));
        initHook(new AllAppsBlur(), true);
        initHook(new LargeIconCornerRadius(), true);
        initHook(new FixAnimation(), mPrefsMap.getBoolean("home_title_fix_animation"));

        //多小窗
        initHook(new FreeFormCountForHome(), mPrefsMap.getBoolean("system_framework_freeform_count"));
        initHook(new MaxFreeForm(), mPrefsMap.getBoolean("system_framework_freeform_count"));

        //Fold2样式负一屏
        initHook(new OverlapMode(), mPrefsMap.getBoolean("personal_assistant_overlap_mode"));

        //Other
        initHook(new StickyFloatingWindowsForHome(), mPrefsMap.getBoolean("system_framework_freeform_sticky"));
        initHook(new WidgetCrack(), mPrefsMap.getBoolean("hidden_function") && mPrefsMap.getBoolean("personal_assistant_widget_crack"));
        initHook(AnimDurationRatio.INSTANCE, true);
        initHook(SetDeviceLevel.INSTANCE, mPrefsMap.getBoolean("home_other_high_models"));
    }

}
