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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.app.Home.Pad;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.home.AnimDurationRatio;
import com.sevtinge.hyperceiler.module.hook.home.DisablePrestart;
import com.sevtinge.hyperceiler.module.hook.home.FreeFormCountForHome;
import com.sevtinge.hyperceiler.module.hook.home.HomePortraitReverse;
import com.sevtinge.hyperceiler.module.hook.home.LockApp;
import com.sevtinge.hyperceiler.module.hook.home.MaxFreeForm;
import com.sevtinge.hyperceiler.module.hook.home.ScreenSwipe;
import com.sevtinge.hyperceiler.module.hook.home.SeekPoints;
import com.sevtinge.hyperceiler.module.hook.home.SetDeviceLevel;
import com.sevtinge.hyperceiler.module.hook.home.StickyFloatingWindowsForHome;
import com.sevtinge.hyperceiler.module.hook.home.ToastSlideAgain;
import com.sevtinge.hyperceiler.module.hook.home.UnlockHotseatIcon;
import com.sevtinge.hyperceiler.module.hook.home.UserPresentAnimation;
import com.sevtinge.hyperceiler.module.hook.home.WidgetCornerRadius;
import com.sevtinge.hyperceiler.module.hook.home.dock.DisableRecentsIcon;
import com.sevtinge.hyperceiler.module.hook.home.dock.DockCustom;
import com.sevtinge.hyperceiler.module.hook.home.dock.DockCustomNew;
import com.sevtinge.hyperceiler.module.hook.home.dock.FoldDeviceDock;
import com.sevtinge.hyperceiler.module.hook.home.dock.FoldDock;
import com.sevtinge.hyperceiler.module.hook.home.dock.HideDock;
import com.sevtinge.hyperceiler.module.hook.home.dock.ShowDockIconTitle;
import com.sevtinge.hyperceiler.module.hook.home.dock.SlideUpOnlyShowDock;
import com.sevtinge.hyperceiler.module.hook.home.drawer.AllAppsContainerViewBlur;
import com.sevtinge.hyperceiler.module.hook.home.drawer.AllAppsContainerViewSuperBlur;
import com.sevtinge.hyperceiler.module.hook.home.drawer.AppDrawer;
import com.sevtinge.hyperceiler.module.hook.home.drawer.PinyinArrangement;
import com.sevtinge.hyperceiler.module.hook.home.folder.BigFolderIcon;
import com.sevtinge.hyperceiler.module.hook.home.folder.BigFolderIconBlur;
import com.sevtinge.hyperceiler.module.hook.home.folder.BigFolderIconBlur1x2;
import com.sevtinge.hyperceiler.module.hook.home.folder.BigFolderIconBlur2x1;
import com.sevtinge.hyperceiler.module.hook.home.folder.BigFolderItemMaxCount;
import com.sevtinge.hyperceiler.module.hook.home.folder.FolderAnimation;
import com.sevtinge.hyperceiler.module.hook.home.folder.FolderAutoClose;
import com.sevtinge.hyperceiler.module.hook.home.folder.FolderBlur;
import com.sevtinge.hyperceiler.module.hook.home.folder.FolderColumns;
import com.sevtinge.hyperceiler.module.hook.home.folder.FolderShade;
import com.sevtinge.hyperceiler.module.hook.home.folder.FolderVerticalSpacing;
import com.sevtinge.hyperceiler.module.hook.home.folder.SmallFolderIconBlur;
import com.sevtinge.hyperceiler.module.hook.home.folder.UnlockBlurSupported;
import com.sevtinge.hyperceiler.module.hook.home.gesture.CornerSlide;
import com.sevtinge.hyperceiler.module.hook.home.gesture.DoubleTap;
import com.sevtinge.hyperceiler.module.hook.home.gesture.HotSeatSwipe;
import com.sevtinge.hyperceiler.module.hook.home.gesture.QuickBack;
import com.sevtinge.hyperceiler.module.hook.home.gesture.ShakeDevice;
import com.sevtinge.hyperceiler.module.hook.home.layout.HotSeatsHeight;
import com.sevtinge.hyperceiler.module.hook.home.layout.HotSeatsMarginBottom;
import com.sevtinge.hyperceiler.module.hook.home.layout.HotSeatsMarginTop;
import com.sevtinge.hyperceiler.module.hook.home.layout.IndicatorMarginBottom;
import com.sevtinge.hyperceiler.module.hook.home.layout.SearchBarMarginBottom;
import com.sevtinge.hyperceiler.module.hook.home.layout.SearchBarMarginWidth;
import com.sevtinge.hyperceiler.module.hook.home.layout.UnlockGrids;
import com.sevtinge.hyperceiler.module.hook.home.layout.WorkspacePadding;
import com.sevtinge.hyperceiler.module.hook.home.mipad.EnableHideGestureLine;
import com.sevtinge.hyperceiler.module.hook.home.mipad.EnableMoreSetting;
import com.sevtinge.hyperceiler.module.hook.home.navigation.BackGestureAreaHeight;
import com.sevtinge.hyperceiler.module.hook.home.navigation.BackGestureAreaWidth;
import com.sevtinge.hyperceiler.module.hook.home.navigation.HideNavigationBar;
import com.sevtinge.hyperceiler.module.hook.home.other.AllowShareApk;
import com.sevtinge.hyperceiler.module.hook.home.other.AlwaysBlurWallpaper;
import com.sevtinge.hyperceiler.module.hook.home.other.AlwaysShowStatusClock;
import com.sevtinge.hyperceiler.module.hook.home.other.BlurRadius;
import com.sevtinge.hyperceiler.module.hook.home.other.BlurWhenShowShortcutMenu;
import com.sevtinge.hyperceiler.module.hook.home.other.DisableHideGoogle;
import com.sevtinge.hyperceiler.module.hook.home.other.FreeformShortcutMenu;
import com.sevtinge.hyperceiler.module.hook.home.other.HomeMode;
import com.sevtinge.hyperceiler.module.hook.home.other.InfiniteScroll;
import com.sevtinge.hyperceiler.module.hook.home.other.OverlapMode;
import com.sevtinge.hyperceiler.module.hook.home.other.ShortcutItemCount;
import com.sevtinge.hyperceiler.module.hook.home.other.ShowAllHideApp;
import com.sevtinge.hyperceiler.module.hook.home.other.TasksShortcutMenu;
import com.sevtinge.hyperceiler.module.hook.home.recent.AlwaysShowCleanUp;
import com.sevtinge.hyperceiler.module.hook.home.recent.BackgroundBlur;
import com.sevtinge.hyperceiler.module.hook.home.recent.BlurLevel;
import com.sevtinge.hyperceiler.module.hook.home.recent.CardTextColor;
import com.sevtinge.hyperceiler.module.hook.home.recent.CardTextSize;
import com.sevtinge.hyperceiler.module.hook.home.recent.DisableRecentViewWallpaperDarken;
import com.sevtinge.hyperceiler.module.hook.home.recent.FreeformCardBackgroundColor;
import com.sevtinge.hyperceiler.module.hook.home.recent.HideCleanUp;
import com.sevtinge.hyperceiler.module.hook.home.recent.HideFreeform;
import com.sevtinge.hyperceiler.module.hook.home.recent.HideRecentCard;
import com.sevtinge.hyperceiler.module.hook.home.recent.HideStatusBarWhenEnterRecent;
import com.sevtinge.hyperceiler.module.hook.home.recent.MemInfoShow;
import com.sevtinge.hyperceiler.module.hook.home.recent.RealMemory;
import com.sevtinge.hyperceiler.module.hook.home.recent.RecentResource;
import com.sevtinge.hyperceiler.module.hook.home.recent.RecentText;
import com.sevtinge.hyperceiler.module.hook.home.recent.RemoveCardAnim;
import com.sevtinge.hyperceiler.module.hook.home.recent.RemoveIcon;
import com.sevtinge.hyperceiler.module.hook.home.recent.RemoveLeftShare;
import com.sevtinge.hyperceiler.module.hook.home.recent.ShowLaunch;
import com.sevtinge.hyperceiler.module.hook.home.recent.TaskViewHorizontal;
import com.sevtinge.hyperceiler.module.hook.home.recent.TaskViewVertical;
import com.sevtinge.hyperceiler.module.hook.home.recent.UnlockPin;
import com.sevtinge.hyperceiler.module.hook.home.title.AnimParamCustom;
import com.sevtinge.hyperceiler.module.hook.home.title.AppBlurAnim;
import com.sevtinge.hyperceiler.module.hook.home.title.BigIconCorner;
import com.sevtinge.hyperceiler.module.hook.home.title.DisableHideFile;
import com.sevtinge.hyperceiler.module.hook.home.title.DisableHideTheme;
import com.sevtinge.hyperceiler.module.hook.home.title.DownloadAnimation;
import com.sevtinge.hyperceiler.module.hook.home.title.EnableIconMonetColor;
import com.sevtinge.hyperceiler.module.hook.home.title.EnableIconMonoChrome;
import com.sevtinge.hyperceiler.module.hook.home.title.FakeNonDefaultIcon;
import com.sevtinge.hyperceiler.module.hook.home.title.FixAnimation;
import com.sevtinge.hyperceiler.module.hook.home.title.HiddenAllTitle;
import com.sevtinge.hyperceiler.module.hook.home.title.HideReportText;
import com.sevtinge.hyperceiler.module.hook.home.title.IconMessageColorCustom;
import com.sevtinge.hyperceiler.module.hook.home.title.IconTitleColor;
import com.sevtinge.hyperceiler.module.hook.home.title.IconTitleCustomization;
import com.sevtinge.hyperceiler.module.hook.home.title.LargeIconCornerRadius;
import com.sevtinge.hyperceiler.module.hook.home.title.NewInstallIndicator;
import com.sevtinge.hyperceiler.module.hook.home.title.PerfectIcon;
import com.sevtinge.hyperceiler.module.hook.home.title.TitleFontSize;
import com.sevtinge.hyperceiler.module.hook.home.title.TitleMarquee;
import com.sevtinge.hyperceiler.module.hook.home.widget.AllWidgetAnimation;
import com.sevtinge.hyperceiler.module.hook.home.widget.AllowMoveAllWidgetToMinus;
import com.sevtinge.hyperceiler.module.hook.home.widget.AlwaysShowMiuiWidget;
import com.sevtinge.hyperceiler.module.hook.home.widget.HideWidgetTitles;
import com.sevtinge.hyperceiler.module.hook.home.widget.ResizableWidgets;
import com.sevtinge.hyperceiler.module.hook.systemframework.mipad.SetGestureNeedFingerNum;

import java.util.Objects;

@HookBase(targetPackage = "com.miui.home", isPad = true, targetSdk = 35)
public class HomeV extends BaseModule {

    @Override
    public void handleLoadPackage() {

        // 手势
        initHook(new QuickBack(), mPrefsMap.getBoolean("home_navigation_quick_back"));
        initHook(new CornerSlide(),
                mPrefsMap.getInt("home_navigation_assist_left_slide_action", 0) > 0 ||
                        mPrefsMap.getInt("home_navigation_assist_right_slide_action", 0) > 0
        );
        initHook(new DoubleTap(), mPrefsMap.getInt("home_gesture_double_tap_action", 0) > 0);
        initHook(new ScreenSwipe(), mPrefsMap.getInt("home_gesture_up_swipe_action", 0) > 0 ||
                mPrefsMap.getInt("home_gesture_down_swipe_action", 0) > 0 ||
                mPrefsMap.getInt("home_gesture_up_swipe2_action", 0) > 0 ||
                mPrefsMap.getInt("home_gesture_down_swipe2_action", 0) > 0);
        initHook(new HotSeatSwipe(), mPrefsMap.getInt("home_gesture_left_swipe_action", 0) > 0
                || mPrefsMap.getInt("home_gesture_right_swipe_action", 0) > 0);
        initHook(new ShakeDevice(), mPrefsMap.getInt("home_gesture_shake_action", 0) > 0);
        // initHook(new SwipeAndStop(), mPrefsMap.getInt("home_gesture_swipe_and_stop_action" ,0) > 0);

        initHook(new BackGestureAreaHeight(), mPrefsMap.getInt("home_navigation_back_area_height", 60) != 60);
        initHook(new BackGestureAreaWidth(), mPrefsMap.getInt("home_navigation_back_area_width", 100) != 100);

        // 布局
        initHook(new UnlockGrids(), mPrefsMap.getBoolean("home_layout_unlock_grids"));
        // initHook(new UnlockGridsNoWord(), mPrefsMap.getBoolean("home_layout_unlock_grids_no_word"));
        initHook(new WorkspacePadding(),
                mPrefsMap.getBoolean("home_layout_workspace_padding_bottom_enable") ||
                        mPrefsMap.getBoolean("home_layout_workspace_padding_top_enable") ||
                        mPrefsMap.getBoolean("home_layout_workspace_padding_horizontal_enable")
        );

        initHook(new IndicatorMarginBottom(), mPrefsMap.getBoolean("home_layout_indicator_margin_bottom_enable"));
        initHook(new HotSeatsHeight(), mPrefsMap.getBoolean("home_layout_hotseats_height_enable"));
        initHook(new HotSeatsMarginTop(), mPrefsMap.getBoolean("home_layout_hotseats_margin_top_enable"));
        initHook(new HotSeatsMarginBottom(), mPrefsMap.getBoolean("home_layout_hotseats_margin_bottom_enable"));
        initHook(new SearchBarMarginWidth(), mPrefsMap.getBoolean("home_layout_searchbar_width_enable"));
        initHook(new SearchBarMarginBottom(), (mPrefsMap.getInt("home_layout_searchbar_margin_bottom", 0) > 0) &&
                mPrefsMap.getBoolean("home_layout_searchbar_margin_bottom_enable"));


        // 文件夹
        initHook(FolderAutoClose.INSTANCE, mPrefsMap.getBoolean("home_folder_auto_close"));
        initHook(new FolderShade(), mPrefsMap.getStringAsInt("home_folder_shade", 1) > 0);
        initHook(FolderColumns.INSTANCE, mPrefsMap.getStringAsInt("home_folder_title_pos", 0) != 0 ||
                mPrefsMap.getBoolean("home_folder_width") ||
                mPrefsMap.getInt("home_folder_columns", 3) != 3);
        initHook(new FolderAnimation(), mPrefsMap.getBoolean("home_folder_animation"));
        initHook(new SmallFolderIconBlur(), mPrefsMap.getBoolean("home_small_folder_icon_bg"));
        initHook(FolderVerticalSpacing.INSTANCE, mPrefsMap.getBoolean("home_folder_vertical_spacing_enable"));

        initHook(new BigFolderIcon(), false);
        initHook(new BigFolderIconBlur2x1(), mPrefsMap.getBoolean("home_big_folder_icon_bg_2x1"));
        initHook(new BigFolderIconBlur1x2(), mPrefsMap.getBoolean("home_big_folder_icon_bg_1x2"));
        initHook(new BigFolderIconBlur(), mPrefsMap.getBoolean("home_big_folder_icon_bg"));
        initHook(new BigFolderItemMaxCount(), mPrefsMap.getBoolean("home_big_folder_item_max_count"));
        initHook(new UnlockBlurSupported(), mPrefsMap.getBoolean("home_folder_unlock_blur_supported"));
        // initHook(new RecommendAppsSwitch(), mPrefsMap.getBoolean("home_folder_recommend_apps_switch"));

        // 抽屉
        initHook(AppDrawer.INSTANCE, mPrefsMap.getBoolean("home_drawer_all") ||
                mPrefsMap.getBoolean("home_drawer_editor"));
        initHook(AllAppsContainerViewBlur.INSTANCE, mPrefsMap.getBoolean("home_drawer_blur"));
        initHook(new AllAppsContainerViewSuperBlur(), mPrefsMap.getBoolean("home_drawer_blur_super"));
        initHook(new PinyinArrangement(), mPrefsMap.getBoolean("home_drawer_pinyin"));

        // 最近任务
        initHook(BlurLevel.INSTANCE, mPrefsMap.getStringAsInt("home_recent_blur_level", 6) != 6 && !mPrefsMap.getBoolean("home_title_app_blur_enable"));
        initHook(DisableRecentViewWallpaperDarken.INSTANCE, mPrefsMap.getBoolean("home_recent_disable_wallpaper_dimming"));
        initHook(HideStatusBarWhenEnterRecent.INSTANCE, mPrefsMap.getBoolean("home_recent_hide_status_bar_in_task_view"));
        initHook(RemoveCardAnim.INSTANCE, mPrefsMap.getBoolean("home_recent_modify_animation"));
        initHook(TaskViewHorizontal.INSTANCE, true);
        initHook(TaskViewVertical.INSTANCE, true);
        initHook(HideFreeform.INSTANCE, mPrefsMap.getBoolean("home_recent_hide_freeform"));
        initHook(new HideCleanUp(), mPrefsMap.getBoolean("home_recent_hide_clean_up"));
        initHook(FreeformCardBackgroundColor.INSTANCE, true);
        initHook(CardTextColor.INSTANCE, true);
        initHook(CardTextSize.INSTANCE, true);
        initHook(UnlockPin.INSTANCE, mPrefsMap.getBoolean("home_recent_unlock_pin"));
        initHook(RecentText.INSTANCE, !Objects.equals(mPrefsMap.getString("home_recent_text", ""), ""));
        initHook(RemoveIcon.INSTANCE, mPrefsMap.getBoolean("home_recent_remove_icon"));
        initHook(RemoveLeftShare.INSTANCE, mPrefsMap.getBoolean("home_recent_hide_world_circulate"));
        initHook(RecentResource.INSTANCE, mPrefsMap.getInt("task_view_corners", 20) != 20 ||
                mPrefsMap.getInt("task_view_header_height", 40) != 40);
        initHook(RealMemory.INSTANCE, mPrefsMap.getBoolean("home_recent_show_real_memory"));
        initHook(MemInfoShow.INSTANCE, mPrefsMap.getBoolean("home_recent_show_memory_info"));
        initHook(AlwaysShowCleanUp.INSTANCE, mPrefsMap.getBoolean("always_show_clean_up"));
        initHook(new BackgroundBlur(), mPrefsMap.getBoolean("home_recent_blur"));
        initHook(new ShowLaunch(), mPrefsMap.getBoolean("home_recent_show_launch"));
        initHook(HideRecentCard.INSTANCE, !mPrefsMap.getStringSet("home_recent_hide_card").isEmpty());

        // 图标
        initHook(BigIconCorner.INSTANCE, mPrefsMap.getBoolean("home_title_big_icon_corner"));
        initHook(new DownloadAnimation(), mPrefsMap.getBoolean("home_title_download_animation"));
        initHook(DisableHideTheme.INSTANCE, mPrefsMap.getBoolean("home_title_disable_hide_theme"));
        initHook(DisableHideFile.INSTANCE, mPrefsMap.getBoolean("home_title_disable_hide_file"));
        initHook(DisableHideGoogle.INSTANCE, mPrefsMap.getBoolean("home_title_disable_hide_google"));
        initHook(new FakeNonDefaultIcon(), mPrefsMap.getBoolean("fake_non_default_icon"));
        initHook(new AnimParamCustom(), mPrefsMap.getBoolean("home_title_custom_anim_param_main"));
        initHook(AppBlurAnim.INSTANCE, mPrefsMap.getBoolean("home_title_app_blur_enable"));
        // initHook(new IconScaleHook()/*, mPrefsMap.getInt("home_title_icon_scale", 100) != 100*/);

        // 标题
        initHook(new TitleMarquee(), mPrefsMap.getBoolean("home_title_title_marquee"));
        initHook(new NewInstallIndicator(), mPrefsMap.getBoolean("home_title_title_new_install"));
        initHook(new IconTitleCustomization(), mPrefsMap.getBoolean("home_title_title_icontitlecustomization_onoff"));
        initHook(new HiddenAllTitle(), mPrefsMap.getBoolean("home_drawer_font_hidden"));
        initHook(new TitleFontSize());
        initHook(IconTitleColor.INSTANCE, true);
        initHook(new UnlockHotseatIcon(), mPrefsMap.getBoolean("home_dock_unlock_hotseat"));
        initHook(new IconMessageColorCustom(), mPrefsMap.getBoolean("home_title_notif_color"));

        // 小部件
        initHook(new AllWidgetAnimation(), mPrefsMap.getBoolean("home_widget_all_widget_animation"));
        initHook(AlwaysShowMiuiWidget.INSTANCE, mPrefsMap.getBoolean("home_widget_show_miui_widget"));
        initHook(AllowMoveAllWidgetToMinus.INSTANCE, mPrefsMap.getBoolean("home_widget_allow_moved_to_minus_one_screen"));
        initHook(new WidgetCornerRadius(), mPrefsMap.getInt("home_widget_corner_radius", 0) > 0);
        initHook(HideWidgetTitles.INSTANCE, mPrefsMap.getBoolean("home_widget_hide_title"));
        initHook(ResizableWidgets.INSTANCE, mPrefsMap.getBoolean("home_widget_resizable"));

        // 底栏
        initHook(new DockCustom(), mPrefsMap.getBoolean("home_dock_bg_custom_enable") && mPrefsMap.getStringAsInt("home_dock_add_blur", 0) == 2);
        initHook(DockCustomNew.INSTANCE, mPrefsMap.getBoolean("home_dock_bg_custom_enable") && (mPrefsMap.getStringAsInt("home_dock_add_blur", 0) == 0 || mPrefsMap.getStringAsInt("home_dock_add_blur", 0) == 1));
        initHook(new SeekPoints(), mPrefsMap.getStringAsInt("home_other_seek_points", 0) > 0);
        initHook(FoldDeviceDock.INSTANCE, mPrefsMap.getBoolean("home_dock_fold"));
        initHook(ShowDockIconTitle.INSTANCE, mPrefsMap.getBoolean("home_dock_icon_title"));
        initHook(new HideNavigationBar(), mPrefsMap.getBoolean("system_ui_hide_navigation_bar"));
        initHook(DisableRecentsIcon.INSTANCE, mPrefsMap.getBoolean("home_dock_disable_recents_icon"));
        initHook(SlideUpOnlyShowDock.INSTANCE, mPrefsMap.getBoolean("home_dock_slide_up_only_show_dock") && !mPrefsMap.getBoolean("home_dock_hide_dock"));
        initHook(HideDock.INSTANCE, mPrefsMap.getBoolean("home_dock_hide_dock"));

        // 其他
        initHook(new LockApp(), mPrefsMap.getBoolean("system_framework_guided_access"));
        initHook(new HomeMode(), mPrefsMap.getStringAsInt("home_other_home_mode", 0) > 0);
        initHook(AlwaysShowStatusClock.INSTANCE, mPrefsMap.getBoolean("home_other_show_clock"));
        initHook(new InfiniteScroll(), mPrefsMap.getBoolean("home_other_infinite_scroll"));
        initHook(new FreeformShortcutMenu(), mPrefsMap.getBoolean("home_other_freeform_shortcut_menu"));
        initHook(new TasksShortcutMenu(), mPrefsMap.getBoolean("home_other_tasks_shortcut_menu"));
        initHook(new UserPresentAnimation(), mPrefsMap.getBoolean("home_other_user_present_animation"));
        initHook(new PerfectIcon(), mPrefsMap.getBoolean("home_other_perfect_icon"));
        initHook(new EnableIconMonoChrome(), mPrefsMap.getBoolean("home_other_icon_mono_chrome"));
        initHook(new HomePortraitReverse(), mPrefsMap.getBoolean("home_other_portrait_reverse"));
        initHook(AlwaysBlurWallpaper.INSTANCE, mPrefsMap.getBoolean("home_other_always_blur_launcher_wallpaper"));
        initHook(BlurRadius.INSTANCE, mPrefsMap.getInt("home_other_blur_radius", 100) != 100);
        initHook(ShortcutItemCount.INSTANCE, mPrefsMap.getBoolean("home_other_shortcut_remove_restrictions"));
        initHook(ShowAllHideApp.INSTANCE, true); // 桌面快捷方式管理
        // initHook(new AllowShareApk(), mPrefsMap.getBoolean("home_other_allow_share_apk"));
        initHook(new HideReportText(), mPrefsMap.getBoolean("home_title_hide_report_text"));
        initHook(new DisablePrestart(), mPrefsMap.getBoolean("home_other_disable_prestart"));

        // 实验性功能
        initHook(BlurWhenShowShortcutMenu.INSTANCE, mPrefsMap.getBoolean("home_other_shortcut_background_blur"));
        initHook(FolderBlur.INSTANCE, mPrefsMap.getBoolean("home_folder_blur") && !mPrefsMap.getBoolean("home_title_app_blur_enable"));
        initHook(new FoldDock(), mPrefsMap.getBoolean("home_other_fold_dock"));
        // initHook(new AllAppsBlur); // ??
        initHook(new FixAnimation(), mPrefsMap.getBoolean("home_title_fix_animation"));
        initHook(new LargeIconCornerRadius(), mPrefsMap.getBoolean("home_large_icon_enable"));

        // 多小窗
        initHook(new FreeFormCountForHome(), mPrefsMap.getBoolean("system_framework_freeform_count"));
        initHook(new MaxFreeForm(), mPrefsMap.getBoolean("system_framework_freeform_count"));

        // Fold2样式负一屏
        initHook(new OverlapMode(), mPrefsMap.getBoolean("personal_assistant_overlap_mode"));

        // Other
        initHook(new ToastSlideAgain(), mPrefsMap.getBoolean("home_other_toast_slide_again"));
        initHook(new StickyFloatingWindowsForHome(), mPrefsMap.getBoolean("system_framework_freeform_sticky"));
        initHook(AnimDurationRatio.INSTANCE, true);
        initHook(SetDeviceLevel.INSTANCE, mPrefsMap.getBoolean("home_other_high_models"));

        // 小米/红米平板相关
        boolean mMoreSetting = mPrefsMap.getBoolean("home_other_mi_pad_enable_more_setting");
        initHook(SetGestureNeedFingerNum.INSTANCE, mPrefsMap.getBoolean("mipad_input_need_finger_num"));
        initHook(EnableMoreSetting.INSTANCE, mMoreSetting);
        initHook(EnableHideGestureLine.INSTANCE, mMoreSetting);

        // reshook
        initHook(EnableIconMonetColor.INSTANCE, mPrefsMap.getBoolean("home_other_icon_monet_color"));
        initHook(new AllowShareApk(), mPrefsMap.getBoolean("home_other_allow_share_apk"));
    }

}
