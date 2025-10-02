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
package com.sevtinge.hyperceiler.hook.module.app.Home;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.hook.home.AnimDurationRatio;
import com.sevtinge.hyperceiler.hook.module.hook.home.DisablePrestart;
import com.sevtinge.hyperceiler.hook.module.hook.home.ScreenSwipe;
import com.sevtinge.hyperceiler.hook.module.hook.home.SeekPoints;
import com.sevtinge.hyperceiler.hook.module.hook.home.UnlockHotseatIcon;
import com.sevtinge.hyperceiler.hook.module.hook.home.dock.DisableRecentsIcon;
import com.sevtinge.hyperceiler.hook.module.hook.home.drawer.AllAppsContainerViewBlur;
import com.sevtinge.hyperceiler.hook.module.hook.home.drawer.AllAppsContainerViewSuperBlur;
import com.sevtinge.hyperceiler.hook.module.hook.home.drawer.AppDrawer;
import com.sevtinge.hyperceiler.hook.module.hook.home.drawer.PinyinArrangement;
import com.sevtinge.hyperceiler.hook.module.hook.home.folder.BigFolderItemMaxCount;
import com.sevtinge.hyperceiler.hook.module.hook.home.folder.FolderAutoClose;
import com.sevtinge.hyperceiler.hook.module.hook.home.folder.FolderColumns;
import com.sevtinge.hyperceiler.hook.module.hook.home.folder.FolderVerticalSpacing;
import com.sevtinge.hyperceiler.hook.module.hook.home.gesture.CornerSlide;
import com.sevtinge.hyperceiler.hook.module.hook.home.gesture.DoubleTap;
import com.sevtinge.hyperceiler.hook.module.hook.home.gesture.HotSeatSwipe;
import com.sevtinge.hyperceiler.hook.module.hook.home.gesture.QuickBack;
import com.sevtinge.hyperceiler.hook.module.hook.home.gesture.ShakeDevice;
import com.sevtinge.hyperceiler.hook.module.hook.home.layout.HotSeatsHeight;
import com.sevtinge.hyperceiler.hook.module.hook.home.layout.HotSeatsMarginBottom;
import com.sevtinge.hyperceiler.hook.module.hook.home.layout.HotSeatsMarginTop;
import com.sevtinge.hyperceiler.hook.module.hook.home.layout.IndicatorMarginBottom;
import com.sevtinge.hyperceiler.hook.module.hook.home.layout.LayoutRules;
import com.sevtinge.hyperceiler.hook.module.hook.home.layout.SearchBarMarginBottom;
import com.sevtinge.hyperceiler.hook.module.hook.home.layout.SearchBarMarginWidth;
import com.sevtinge.hyperceiler.hook.module.hook.home.layout.WorkspacePadding;
import com.sevtinge.hyperceiler.hook.module.hook.home.mipad.EnableHideGestureLine;
import com.sevtinge.hyperceiler.hook.module.hook.home.mipad.EnableMoreSetting;
import com.sevtinge.hyperceiler.hook.module.hook.home.mipad.SetGestureNeedFingerNum;
import com.sevtinge.hyperceiler.hook.module.hook.home.navigation.BackGestureAreaHeight;
import com.sevtinge.hyperceiler.hook.module.hook.home.navigation.BackGestureAreaWidth;
import com.sevtinge.hyperceiler.hook.module.hook.home.other.AllowShareApk;
import com.sevtinge.hyperceiler.hook.module.hook.home.other.HomeMode;
import com.sevtinge.hyperceiler.hook.module.hook.home.other.InfiniteScroll;
import com.sevtinge.hyperceiler.hook.module.hook.home.other.ShortcutItemCount;
import com.sevtinge.hyperceiler.hook.module.hook.home.other.ShowAllHideApp;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.AlwaysShowCleanUp;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.BackgroundBlur;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.CardTextColor;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.CardTextSize;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.FreeformCardBackgroundColor;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.HideRecentCard;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.HideStatusBarWhenEnterRecent;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.MemInfoShow;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.RealMemory;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.RecentResource;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.RecentText;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.RemoveCardAnim;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.RemoveIcon;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.TaskViewHeaderOffset;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.TaskViewHeight;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.TaskViewHorizontal;
import com.sevtinge.hyperceiler.hook.module.hook.home.recent.TaskViewVertical;
import com.sevtinge.hyperceiler.hook.module.hook.home.widget.AllWidgetAnimation;
import com.sevtinge.hyperceiler.hook.module.hook.home.widget.AlwaysShowMiuiWidget;

import java.util.Objects;

@HookBase(targetPackage = "com.miui.home", isPad = 1, targetOSVersion = 3f)
public class HomePad extends BaseModule {

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
        initHook(new BackGestureAreaHeight(), mPrefsMap.getInt("home_navigation_back_area_height", 60) != 60);
        initHook(new BackGestureAreaWidth(), mPrefsMap.getInt("home_navigation_back_area_width", 100) != 100);

        // 布局
        initHook(new SeekPoints(), mPrefsMap.getStringAsInt("home_other_seek_points", 0) > 0);
        initHook(LayoutRules.INSTANCE, mPrefsMap.getBoolean("home_layout_unlock_grids_new") ||
            mPrefsMap.getBoolean("home_layout_workspace_padding_bottom_enable") ||
            mPrefsMap.getBoolean("home_layout_workspace_padding_top_enable") ||
            mPrefsMap.getBoolean("home_layout_workspace_padding_horizontal_enable"));
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
        initHook(FolderColumns.INSTANCE, mPrefsMap.getStringAsInt("home_folder_title_pos", 0) != 0 ||
            mPrefsMap.getBoolean("home_folder_width") ||
            mPrefsMap.getInt("home_folder_columns", 3) != 3);
        initHook(FolderVerticalSpacing.INSTANCE, mPrefsMap.getBoolean("home_folder_vertical_spacing_enable"));

        // 底栏
        initHook(new UnlockHotseatIcon(), mPrefsMap.getBoolean("home_dock_unlock_hotseat"));
        initHook(DisableRecentsIcon.INSTANCE, mPrefsMap.getBoolean("home_dock_disable_recents_icon"));

        // 抽屉
        initHook(AppDrawer.INSTANCE, mPrefsMap.getBoolean("home_drawer_all") ||
            mPrefsMap.getBoolean("home_drawer_editor"));
        initHook(AllAppsContainerViewBlur.INSTANCE, mPrefsMap.getBoolean("home_drawer_blur"));
        initHook(new AllAppsContainerViewSuperBlur(), mPrefsMap.getBoolean("home_drawer_blur_super"));
        initHook(new PinyinArrangement(), mPrefsMap.getBoolean("home_drawer_pinyin"));

        // 最近任务
        initHook(new BackgroundBlur(), mPrefsMap.getBoolean("home_recent_blur"));
        initHook(MemInfoShow.INSTANCE, mPrefsMap.getBoolean("home_recent_show_memory_info"));
        initHook(RealMemory.INSTANCE, mPrefsMap.getBoolean("home_recent_show_real_memory"));
        initHook(RemoveCardAnim.INSTANCE, mPrefsMap.getBoolean("home_recent_modify_animation"));
        initHook(HideRecentCard.INSTANCE, !mPrefsMap.getStringSet("home_recent_hide_card").isEmpty());
        initHook(HideStatusBarWhenEnterRecent.INSTANCE, mPrefsMap.getBoolean("home_recent_hide_status_bar_in_task_view"));
        // initHook(RemoveLeftShare.INSTANCE, mPrefsMap.getBoolean("home_recent_hide_world_circulate"));
        initHook(AlwaysShowCleanUp.INSTANCE, mPrefsMap.getBoolean("always_show_clean_up") || mPrefsMap.getBoolean("home_recent_hide_clean_up"));
        initHook(RemoveIcon.INSTANCE, mPrefsMap.getBoolean("home_recent_remove_icon"));
        initHook(RecentText.INSTANCE, !Objects.equals(mPrefsMap.getString("home_recent_text", ""), ""));
        initHook(RecentResource.INSTANCE, mPrefsMap.getInt("task_view_corners", 20) != 20 ||
            mPrefsMap.getInt("task_view_header_height", 40) != 40);
        initHook(TaskViewHeaderOffset.INSTANCE, mPrefsMap.getInt("task_view_header_horizontal_offset", 30) != 30);
        initHook(AnimDurationRatio.INSTANCE, true);
        initHook(TaskViewHeight.INSTANCE, mPrefsMap.getInt("home_recent_task_view_height", 52) != 52);
        initHook(TaskViewHorizontal.INSTANCE, true);
        initHook(TaskViewVertical.INSTANCE, true);
        initHook(CardTextSize.INSTANCE, mPrefsMap.getInt("home_recent_text_size", -1) != -1);
        initHook(CardTextColor.INSTANCE, mPrefsMap.getInt("home_recent_text_color", -1) != -1);
        initHook(FreeformCardBackgroundColor.INSTANCE, true);

        // 文件夹
        initHook(new BigFolderItemMaxCount(), mPrefsMap.getBoolean("home_big_folder_item_max_count"));
        initHook(FolderAutoClose.INSTANCE, mPrefsMap.getBoolean("home_folder_auto_close"));

        // 小部件
        initHook(new AllWidgetAnimation(), mPrefsMap.getBoolean("home_widget_all_widget_animation"));
        initHook(AlwaysShowMiuiWidget.INSTANCE, mPrefsMap.getBoolean("home_widget_show_miui_widget"));

        // 其他
        initHook(new AllowShareApk(), mPrefsMap.getBoolean("home_other_allow_share_apk"));
        initHook(ShortcutItemCount.INSTANCE, mPrefsMap.getBoolean("home_other_shortcut_remove_restrictions"));

        // initHook(SetDeviceLevel.INSTANCE, mPrefsMap.getBoolean("home_other_high_models"));
        initHook(new InfiniteScroll(), mPrefsMap.getBoolean("home_other_infinite_scroll"));
        initHook(new DisablePrestart(), mPrefsMap.getBoolean("home_other_disable_prestart"));
        initHook(new HomeMode(), mPrefsMap.getStringAsInt("home_other_home_mode", 0) > 0);
        initHook(ShowAllHideApp.INSTANCE, true); // 桌面快捷方式管理

        // 小米/红米平板相关
        boolean mMoreSetting = mPrefsMap.getBoolean("home_other_mi_pad_enable_more_setting");
        initHook(SetGestureNeedFingerNum.INSTANCE, mPrefsMap.getBoolean("mipad_input_need_finger_num"));
        initHook(EnableMoreSetting.INSTANCE, mMoreSetting);
        initHook(EnableHideGestureLine.INSTANCE, mMoreSetting);
    }
}
