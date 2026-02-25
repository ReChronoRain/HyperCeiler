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
package com.sevtinge.hyperceiler.libhook.app.Home.os2;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.home.AnimDurationRatio;
import com.sevtinge.hyperceiler.libhook.rules.home.DisablePrestart;
import com.sevtinge.hyperceiler.libhook.rules.home.FreeFormCountForHome;
import com.sevtinge.hyperceiler.libhook.rules.home.HomePortraitReverse;
import com.sevtinge.hyperceiler.libhook.rules.home.LockApp;
import com.sevtinge.hyperceiler.libhook.rules.home.MaxFreeForm;
import com.sevtinge.hyperceiler.libhook.rules.home.ScreenSwipe;
import com.sevtinge.hyperceiler.libhook.rules.home.SeekPoints;
import com.sevtinge.hyperceiler.libhook.rules.home.SetDeviceLevel;
import com.sevtinge.hyperceiler.libhook.rules.home.StickyFloatingWindowsForHome;
import com.sevtinge.hyperceiler.libhook.rules.home.ToastSlideAgain;
import com.sevtinge.hyperceiler.libhook.rules.home.UnlockHotseatIcon;
import com.sevtinge.hyperceiler.libhook.rules.home.WidgetCornerRadius;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DisableRecentsIcon;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockCustom;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockCustomNew;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.FoldDock;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.HideDock;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.ShowDockIconTitle;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.SlideUpOnlyShowDock;
import com.sevtinge.hyperceiler.libhook.rules.home.drawer.AllAppsContainerViewBlur;
import com.sevtinge.hyperceiler.libhook.rules.home.drawer.AllAppsContainerViewSuperBlur;
import com.sevtinge.hyperceiler.libhook.rules.home.drawer.AppDrawer;
import com.sevtinge.hyperceiler.libhook.rules.home.drawer.PinyinArrangement;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.BigFolderIcon;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.BigFolderIconBlur;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.BigFolderIconBlur1x2;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.BigFolderIconBlur2x1;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.BigFolderItemMaxCount;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.FolderAnimation;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.FolderAutoClose;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.FolderColumns;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.FolderShade;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.FolderVerticalSpacing;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.SmallFolderIconBlur;
import com.sevtinge.hyperceiler.libhook.rules.home.folder.UnlockBlurSupported;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.CornerSlide;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.DoubleTap;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HotSeatSwipe;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.QuickBack;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.ShakeDevice;
import com.sevtinge.hyperceiler.libhook.rules.home.layout.HotSeatsHeight;
import com.sevtinge.hyperceiler.libhook.rules.home.layout.HotSeatsMarginBottom;
import com.sevtinge.hyperceiler.libhook.rules.home.layout.HotSeatsMarginTop;
import com.sevtinge.hyperceiler.libhook.rules.home.layout.IndicatorMarginBottom;
import com.sevtinge.hyperceiler.libhook.rules.home.layout.WorkspacePadding;
import com.sevtinge.hyperceiler.libhook.rules.home.mipad.EnableHideGestureLine;
import com.sevtinge.hyperceiler.libhook.rules.home.mipad.EnableMoreSetting;
import com.sevtinge.hyperceiler.libhook.rules.home.mipad.SetGestureNeedFingerNum;
import com.sevtinge.hyperceiler.libhook.rules.home.navigation.BackGestureAreaHeight;
import com.sevtinge.hyperceiler.libhook.rules.home.navigation.BackGestureAreaWidth;
import com.sevtinge.hyperceiler.libhook.rules.home.navigation.HideNavigationBar;
import com.sevtinge.hyperceiler.libhook.rules.home.other.AlwaysBlurWallpaper;
import com.sevtinge.hyperceiler.libhook.rules.home.other.BlurRadius;
import com.sevtinge.hyperceiler.libhook.rules.home.other.BlurWhenShowShortcutMenu;
import com.sevtinge.hyperceiler.libhook.rules.home.other.FreeformShortcutMenu;
import com.sevtinge.hyperceiler.libhook.rules.home.other.HomeMode;
import com.sevtinge.hyperceiler.libhook.rules.home.other.InfiniteScroll;
import com.sevtinge.hyperceiler.libhook.rules.home.other.OptAppLaunchDelay;
import com.sevtinge.hyperceiler.libhook.rules.home.other.ShortcutItemCount;
import com.sevtinge.hyperceiler.libhook.rules.home.other.ShowAllHideApp;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.AlwaysShowCleanUp;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.BackgroundBlur;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.CardTextColor;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.CardTextSize;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.FreeformCardBackgroundColor;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.HideFreeform;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.HideRecentCard;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.HideStatusBarWhenEnterRecent;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.MemInfoShow;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.RealMemory;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.RecentResource;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.RecentText;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.RemoveCardAnim;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.RemoveIcon;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.RemoveLeftShare;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.ShowLaunch;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.TaskViewHeaderOffset;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.TaskViewHorizontal;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.TaskViewVertical;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.UnlockPin;
import com.sevtinge.hyperceiler.libhook.rules.home.title.BigIconCorner;
import com.sevtinge.hyperceiler.libhook.rules.home.title.DisableHideApp;
import com.sevtinge.hyperceiler.libhook.rules.home.title.DisableHideTheme;
import com.sevtinge.hyperceiler.libhook.rules.home.title.DownloadAnimation;
import com.sevtinge.hyperceiler.libhook.rules.home.title.EnableIconMonetColor;
import com.sevtinge.hyperceiler.libhook.rules.home.title.EnableIconMonoChrome;
import com.sevtinge.hyperceiler.libhook.rules.home.title.FakeNonDefaultIcon;
import com.sevtinge.hyperceiler.libhook.rules.home.title.HiddenAllTitle;
import com.sevtinge.hyperceiler.libhook.rules.home.title.HideNewInstallIndicator;
import com.sevtinge.hyperceiler.libhook.rules.home.title.IconMessageColorCustom;
import com.sevtinge.hyperceiler.libhook.rules.home.title.IconSize;
import com.sevtinge.hyperceiler.libhook.rules.home.title.IconTitleColor;
import com.sevtinge.hyperceiler.libhook.rules.home.title.IconTitleCustomization;
import com.sevtinge.hyperceiler.libhook.rules.home.title.LargeIconCornerRadius;
import com.sevtinge.hyperceiler.libhook.rules.home.title.TitleFontSize;
import com.sevtinge.hyperceiler.libhook.rules.home.title.TitleMarquee;
import com.sevtinge.hyperceiler.libhook.rules.home.widget.AllWidgetAnimation;
import com.sevtinge.hyperceiler.libhook.rules.home.widget.AllowMoveAllWidgetToMinus;
import com.sevtinge.hyperceiler.libhook.rules.home.widget.AlwaysShowMiuiWidget;
import com.sevtinge.hyperceiler.libhook.rules.home.widget.ResizableWidgets;
import com.sevtinge.hyperceiler.libhook.rules.home.widget.WidgetBlurOpt;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import java.util.Objects;

@HookBase(targetPackage = "com.miui.home", deviceType = 1, maxOSVersion = 2.0F)
public class HomePadOld extends BaseLoad {

    public HomePadOld() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {

        // 手势
        initHook(new QuickBack(), PrefsBridge.getBoolean("home_navigation_quick_back"));
        initHook(new CornerSlide(),
            PrefsBridge.getInt("home_navigation_assist_left_slide_action", 0) > 0 ||
                PrefsBridge.getInt("home_navigation_assist_right_slide_action", 0) > 0
        );
        initHook(new DoubleTap(), PrefsBridge.getInt("home_gesture_double_tap_action", 0) > 0);
        initHook(new ScreenSwipe(), PrefsBridge.getInt("home_gesture_up_swipe_action", 0) > 0 ||
            PrefsBridge.getInt("home_gesture_down_swipe_action", 0) > 0 ||
            PrefsBridge.getInt("home_gesture_up_swipe2_action", 0) > 0 ||
            PrefsBridge.getInt("home_gesture_down_swipe2_action", 0) > 0);
        initHook(new HotSeatSwipe(), PrefsBridge.getInt("home_gesture_left_swipe_action", 0) > 0
            || PrefsBridge.getInt("home_gesture_right_swipe_action", 0) > 0);
        initHook(new ShakeDevice(), PrefsBridge.getInt("home_gesture_shake_action", 0) > 0);
        // initHook(new SwipeAndStop(), PrefsBridge.getInt("home_gesture_swipe_and_stop_action" ,0) > 0);

        initHook(new BackGestureAreaHeight(), PrefsBridge.getInt("home_navigation_back_area_height", 60) != 60);
        initHook(new BackGestureAreaWidth(), PrefsBridge.getInt("home_navigation_back_area_width", 100) != 100);

        // 布局
        // initHook(new UnlockGridsNoWord(), PrefsBridge.getBoolean("home_layout_unlock_grids_no_word"));
        initHook(new WorkspacePadding(),
                PrefsBridge.getBoolean("home_layout_workspace_padding_bottom_enable") ||
                        PrefsBridge.getBoolean("home_layout_workspace_padding_top_enable") ||
                        PrefsBridge.getBoolean("home_layout_workspace_padding_horizontal_enable")
        );

        initHook(new IndicatorMarginBottom(), PrefsBridge.getBoolean("home_layout_indicator_margin_bottom_enable"));
        initHook(new HotSeatsHeight(), PrefsBridge.getBoolean("home_layout_hotseats_height_enable"));
        initHook(new HotSeatsMarginTop(), PrefsBridge.getBoolean("home_layout_hotseats_margin_top_enable"));
        initHook(new HotSeatsMarginBottom(), PrefsBridge.getBoolean("home_layout_hotseats_margin_bottom_enable"));

        // 文件夹
        initHook(FolderAutoClose.INSTANCE, PrefsBridge.getBoolean("home_folder_auto_close"));
        initHook(new FolderShade(), PrefsBridge.getStringAsInt("home_folder_shade", 1) > 0);
        initHook(FolderColumns.INSTANCE, PrefsBridge.getStringAsInt("home_folder_title_pos", 0) != 0 ||
                PrefsBridge.getBoolean("home_folder_width") ||
                PrefsBridge.getInt("home_folder_columns", 3) != 3);
        initHook(new FolderAnimation(), PrefsBridge.getBoolean("home_folder_animation"));
        initHook(new SmallFolderIconBlur(), PrefsBridge.getBoolean("home_small_folder_icon_bg"));
        initHook(FolderVerticalSpacing.INSTANCE, PrefsBridge.getBoolean("home_folder_vertical_spacing_enable"));

        initHook(new BigFolderIcon(), false);
        initHook(new BigFolderIconBlur2x1(), PrefsBridge.getBoolean("home_big_folder_icon_bg_2x1"));
        initHook(new BigFolderIconBlur1x2(), PrefsBridge.getBoolean("home_big_folder_icon_bg_1x2"));
        initHook(new BigFolderIconBlur(), PrefsBridge.getBoolean("home_big_folder_icon_bg"));
        initHook(new BigFolderItemMaxCount(), PrefsBridge.getBoolean("home_big_folder_item_max_count"));
        initHook(new UnlockBlurSupported(), PrefsBridge.getBoolean("home_folder_unlock_blur_supported"));

        // 抽屉
        initHook(AppDrawer.INSTANCE, PrefsBridge.getBoolean("home_drawer_all") ||
                PrefsBridge.getBoolean("home_drawer_editor"));
        initHook(AllAppsContainerViewBlur.INSTANCE, PrefsBridge.getBoolean("home_drawer_blur"));
        initHook(new AllAppsContainerViewSuperBlur(), PrefsBridge.getBoolean("home_drawer_blur_super"));
        initHook(new PinyinArrangement(), PrefsBridge.getBoolean("home_drawer_pinyin"));

        // 最近任务
        initHook(HideStatusBarWhenEnterRecent.INSTANCE, PrefsBridge.getBoolean("home_recent_hide_status_bar_in_task_view"));
        initHook(RemoveCardAnim.INSTANCE, PrefsBridge.getBoolean("home_recent_modify_animation"));
        initHook(TaskViewHorizontal.INSTANCE, true);
        initHook(TaskViewVertical.INSTANCE, true);
        initHook(HideFreeform.INSTANCE, PrefsBridge.getBoolean("home_recent_hide_freeform"));
        initHook(FreeformCardBackgroundColor.INSTANCE, true);
        initHook(CardTextSize.INSTANCE, PrefsBridge.getInt("home_recent_text_size", -1) != -1);
        initHook(CardTextColor.INSTANCE, PrefsBridge.getInt("home_recent_text_color", -1) != -1);
        initHook(UnlockPin.INSTANCE, PrefsBridge.getBoolean("home_recent_unlock_pin"));
        initHook(RecentText.INSTANCE, !Objects.equals(PrefsBridge.getString("home_recent_text", ""), ""));
        initHook(RemoveIcon.INSTANCE, PrefsBridge.getBoolean("home_recent_remove_icon"));
        initHook(RemoveLeftShare.INSTANCE, PrefsBridge.getBoolean("home_recent_hide_world_circulate"));
        initHook(RecentResource.INSTANCE, PrefsBridge.getInt("task_view_corners", 20) != 20 ||
                PrefsBridge.getInt("task_view_header_height", 40) != 40);
        initHook(TaskViewHeaderOffset.INSTANCE, PrefsBridge.getInt("task_view_header_horizontal_offset", 30) != 30);
        initHook(RealMemory.INSTANCE, PrefsBridge.getBoolean("home_recent_show_real_memory"));
        initHook(MemInfoShow.INSTANCE, PrefsBridge.getBoolean("home_recent_show_memory_info"));
        initHook(AlwaysShowCleanUp.INSTANCE, PrefsBridge.getBoolean("always_show_clean_up") || PrefsBridge.getBoolean("home_recent_hide_clean_up"));
        initHook(new BackgroundBlur(), PrefsBridge.getBoolean("home_recent_blur"));
        initHook(new ShowLaunch(), PrefsBridge.getBoolean("home_recent_show_launch"));
        initHook(HideRecentCard.INSTANCE, !PrefsBridge.getStringSet("home_recent_hide_card").isEmpty());

        // 图标
        initHook(BigIconCorner.INSTANCE, PrefsBridge.getBoolean("home_title_big_icon_corner"));
        initHook(new DownloadAnimation(), PrefsBridge.getBoolean("home_title_download_animation"));
        initHook(DisableHideTheme.INSTANCE, PrefsBridge.getBoolean("home_title_disable_hide_theme"));
        initHook(DisableHideApp.INSTANCE, PrefsBridge.getBoolean("home_title_disable_hide_file") || PrefsBridge.getBoolean("home_title_disable_hide_google"));
        initHook(new FakeNonDefaultIcon(), PrefsBridge.getBoolean("home_title_fake_non_default_icon"));
        initHook(new IconSize(), PrefsBridge.getBoolean("home_title_icon_size_enable"));

        // 标题
        initHook(new TitleMarquee(), PrefsBridge.getBoolean("home_title_title_marquee"));
        initHook(new HideNewInstallIndicator(), PrefsBridge.getBoolean("home_title_title_new_install"));
        initHook(new IconTitleCustomization(), PrefsBridge.getBoolean("home_title_title_icontitlecustomization_onoff"));
        initHook(new HiddenAllTitle(), PrefsBridge.getBoolean("home_drawer_font_hidden"));
        initHook(new TitleFontSize());
        initHook(IconTitleColor.INSTANCE, PrefsBridge.getInt("home_title_title_color", -1) != -1);
        initHook(new UnlockHotseatIcon(), PrefsBridge.getBoolean("home_dock_unlock_hotseat"));
        initHook(new IconMessageColorCustom(), PrefsBridge.getBoolean("home_title_notif_color"));

        // 小部件
        initHook(new AllWidgetAnimation(), PrefsBridge.getBoolean("home_widget_all_widget_animation"));
        initHook(AlwaysShowMiuiWidget.INSTANCE, PrefsBridge.getBoolean("home_widget_show_miui_widget"));
        initHook(AllowMoveAllWidgetToMinus.INSTANCE, PrefsBridge.getBoolean("home_widget_allow_moved_to_minus_one_screen"));
        initHook(new WidgetCornerRadius(), PrefsBridge.getInt("home_widget_corner_radius", 0) > 0);
        initHook(ResizableWidgets.INSTANCE, PrefsBridge.getBoolean("home_widget_resizable"));
        initHook(new WidgetBlurOpt(), PrefsBridge.getBoolean("home_widget_widget_blur_opt"));

        // 底栏
        initHook(new DockCustom(), PrefsBridge.getBoolean("home_dock_bg_custom_enable") && PrefsBridge.getStringAsInt("home_dock_add_blur", 0) == 2);
        initHook(DockCustomNew.INSTANCE, PrefsBridge.getBoolean("home_dock_bg_custom_enable") && (PrefsBridge.getStringAsInt("home_dock_add_blur", 0) == 0 || PrefsBridge.getStringAsInt("home_dock_add_blur", 0) == 1));
        initHook(new SeekPoints(), PrefsBridge.getStringAsInt("home_other_seek_points", 0) > 0);
        initHook(ShowDockIconTitle.INSTANCE, PrefsBridge.getBoolean("home_dock_icon_title"));
        initHook(new HideNavigationBar(), PrefsBridge.getBoolean("system_ui_hide_navigation_bar"));
        initHook(DisableRecentsIcon.INSTANCE, PrefsBridge.getBoolean("home_dock_disable_recents_icon"));
        initHook(SlideUpOnlyShowDock.INSTANCE, PrefsBridge.getBoolean("home_dock_slide_up_only_show_dock") && !PrefsBridge.getBoolean("home_dock_hide_dock"));
        initHook(HideDock.INSTANCE, PrefsBridge.getBoolean("home_dock_hide_dock"));

        // 其他
        initHook(new LockApp(), PrefsBridge.getBoolean("system_framework_guided_access"));
        initHook(new HomeMode(), PrefsBridge.getStringAsInt("home_other_home_mode", 0) > 0);
        initHook(new InfiniteScroll(), PrefsBridge.getBoolean("home_other_infinite_scroll"));
        initHook(new FreeformShortcutMenu(), PrefsBridge.getBoolean("home_other_tasks_shortcut_menu"));
        initHook(new EnableIconMonoChrome(), PrefsBridge.getBoolean("home_other_icon_mono_chrome"));
        initHook(new HomePortraitReverse(), PrefsBridge.getBoolean("home_other_portrait_reverse"));
        initHook(AlwaysBlurWallpaper.INSTANCE, PrefsBridge.getBoolean("home_other_always_blur_launcher_wallpaper"));
        initHook(BlurRadius.INSTANCE, PrefsBridge.getInt("home_other_blur_radius", 100) != 100);
        initHook(ShortcutItemCount.INSTANCE, PrefsBridge.getBoolean("home_other_shortcut_remove_restrictions"));
        initHook(ShowAllHideApp.INSTANCE, true); // 桌面快捷方式管理
        // initHook(new AllowShareApk(), PrefsBridge.getBoolean("home_other_allow_share_apk"));
        initHook(new DisablePrestart(), PrefsBridge.getBoolean("home_other_disable_prestart"));
        initHook(new OptAppLaunchDelay(), PrefsBridge.getBoolean("home_other_opt_app_launch_delay"));

        // 实验性功能
        initHook(BlurWhenShowShortcutMenu.INSTANCE, PrefsBridge.getBoolean("home_other_shortcut_background_blur"));
        initHook(new FoldDock(), PrefsBridge.getBoolean("home_other_fold_dock"));
        // initHook(new AllAppsBlur); // ??
        initHook(new LargeIconCornerRadius(), PrefsBridge.getBoolean("home_large_icon_enable"));

        // 多小窗
        initHook(new FreeFormCountForHome(), PrefsBridge.getBoolean("system_framework_freeform_count"));
        initHook(new MaxFreeForm(), PrefsBridge.getBoolean("system_framework_freeform_count"));

        // Other
        initHook(new ToastSlideAgain(), PrefsBridge.getBoolean("home_other_toast_slide_again"));
        initHook(new StickyFloatingWindowsForHome(), PrefsBridge.getBoolean("system_framework_freeform_sticky"));
        initHook(AnimDurationRatio.INSTANCE, true);
        initHook(SetDeviceLevel.INSTANCE, PrefsBridge.getBoolean("home_other_high_models"));

        // 小米/红米平板相关
        boolean mMoreSetting = PrefsBridge.getBoolean("home_other_mi_pad_enable_more_setting");
        initHook(SetGestureNeedFingerNum.INSTANCE, PrefsBridge.getBoolean("mipad_input_need_finger_num"));
        initHook(EnableMoreSetting.INSTANCE, mMoreSetting);
        initHook(EnableHideGestureLine.INSTANCE, mMoreSetting);

        // reshook
        initHook(EnableIconMonetColor.INSTANCE, PrefsBridge.getBoolean("home_other_icon_monet_color"));
    }

}
