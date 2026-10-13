// SPDX-License-Identifier: Apache-2.0
#include "symtab.h"
#include "htcache.h"

#include "a64.h"
#include "scanner.h"

#include <elf.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <chrono>
#include <array>
#include <vector>
#include <algorithm>
#include <limits>

extern "C" {
#include "xz/xz.h"
}

namespace hometweaks {
namespace {

const TargetFunction kTargets[] = {
        {"AssistantDragDataHelper.putWidgetIntoBundle", "AssistantDragDataHelper.putWidgetIntoBundle", "OS4 Gadget 传输"},
        {"GadgetInfoModel.cloneModel", "GadgetInfoModel.cloneModel", "OS4 Gadget 模型校验"},
        {"BundleImpl.putInt", "BundleImpl.putInt", "OS4 Gadget 原序列化调用"},
        {"AssistantDragToPAHandler.canDragToPA", "AssistantDragToPAHandler.canDragToPA", "OS4 安卓小部件负一屏原逻辑"},
        {"AssistantDragToPAHandler._isSpanSupportedByPa", "AssistantDragToPAHandler._isSpanSupportedByPa", "OS4 小部件尺寸校验"},
        {"AssistantDragToPAHandler._ensureDragSessionId", "AssistantDragToPAHandler._ensureDragSessionId", "OS4 小部件交接校验"},
        // Only registered names survive the mini-ELF scan. These are validation/render targets,
        // not UI switches; omitting one makes original-body title injection silently do nothing.
        {"ShortcutIconWidget._buildTextWidget", "ShortcutIconWidget._buildTextWidget", "OS4 桌面标题原逻辑"},
        {"GridConfig.getTitleTextSize", "GridConfig.getTitleTextSize", "OS4 抽屉标题字号"},
        {"AppIcon.build", "AppIcon.build", "OS4 抽屉标题构建"},
        {"ShortcutIconWidget.getTextColor", "ShortcutIconWidget.getTextColor", "OS4 桌面标题颜色"},
        {"Color.withAlpha", "Color.withAlpha", "OS4 颜色字段和分配器校验"},
        {"ShortcutInfoModel.getPackageName", "ShortcutInfoModel.getPackageName", "OS4 自定义标题数据路径校验"},
        {"ShortcutInfoModel.getComponentName", "ShortcutInfoModel.getComponentName", "OS4 自定义标题数据路径校验"},
        {"PinShortcutInfoModel.getComponentName", "PinShortcutInfoModel.getComponentName", "OS4 自定义标题数据路径校验"},
        {"ShortcutInfoModel.copyShortcutModel", "ShortcutInfoModel.copyShortcutModel", "OS4 自定义标题数据路径校验"},
        {"PinShortcutInfoModel.copyShortcutModel", "PinShortcutInfoModel.copyShortcutModel", "OS4 自定义标题数据路径校验"},
        {"PinShortcutInfoModel.makePinAppComponentName", "PinShortcutInfoModel.makePinAppComponentName", "OS4 自定义标题数据路径校验"},
        {"ShortcutIconWidgetConfig.customShortcutIconConfig", "ShortcutIconWidgetConfig.customShortcutIconConfig", "OS4 抽屉文字组件配置"},
        {"allocateTwoByteString", "allocateTwoByteString", "OS4 自定义标题数据路径校验"},
        {"ShortcutIconWidget.getPrefixAssetName", "ShortcutIconWidget.getPrefixAssetName", "OS4 新安装标题标记"},
        {"FolderInfoModel.hasNewInstalledApp", "FolderInfoModel.hasNewInstalledApp", "OS4 文件夹新安装标记"},
        {"FolderIconGetxController.updateNewInstallNotification", "FolderIconGetxController.updateNewInstallNotification", "OS4 新安装标记调用校验"},
        {"ShortcutIconWidget._addNewInstallLight", "ShortcutIconWidget._addNewInstallLight", "OS4 新安装光效"},
        {"FolderGridViewGetxController.getFirstVisibleItemIndex", "FolderGridViewGetxController.getFirstVisibleItemIndex", "OS4 关闭预览原始索引取值校验"},
        {"FolderClingGetxController.calcPositionForCellX", "FolderClingGetxController.calcPositionForCellX", "OS4 关闭预览原始坐标计算校验"},
        {"Inst|find", "Inst|find", "OS4 文件夹原始 singleton 取值校验"},
        {"WidgetPositionUtil.transformPointX", "WidgetPositionUtil.transformPointX", "OS4 文件夹屏幕坐标变换校验"},
        {"SwipeController.isRtl", "SwipeController.isRtl", "OS4 文件夹方向分支校验"},
        {"Offset.translate", "Offset.translate", "OS4 原始 Offset 分配器消费校验"},
        {"WidgetPositionUtil.getCellPosition", "WidgetPositionUtil.getCellPosition", "OS4 文件夹动画坐标原逻辑"},
        {"FolderIconGetxController.calOriginPreviewIconLoc", "FolderIconGetxController.calOriginPreviewIconLoc", "OS4 文件夹动画尺寸原逻辑"},
        {"CellLayoutGetxController.isItemPosEmpty", "CellLayoutGetxController.isItemPosEmpty", "OS4 grid count bounds receiver"},
        {"CellLayoutGetxController.calculateCenterGlobalPosition", "CellLayoutGetxController.calculateCenterGlobalPosition", "OS4 拖放回位中心原逻辑"},
        // OS4 original-body splice/branch guards. Registration does not hook getters.
        {"HotSeatLayoutDelegate.cellLayout", "HotSeatLayoutDelegate.cellLayout", "OS4 原逻辑注入校验"},
        {"LauncherIndicatorState._buildScreenIndicator", "LauncherIndicatorState._buildScreenIndicator", "OS4 原逻辑注入校验"},
        {"LauncherIndicatorState.isInEditing", "LauncherIndicatorState.isInEditing", "OS4 原逻辑注入校验"},
        {"LauncherIndicatorState._showIndicator", "LauncherIndicatorState._showIndicator", "OS4 原逻辑注入校验"},
        {"LauncherIndicatorState._animateIndicator", "LauncherIndicatorState._animateIndicator", "OS4 原逻辑注入校验"},
        {"LauncherIndicatorState._getCurrentIndicatorType", "LauncherIndicatorState._getCurrentIndicatorType", "OS4 idle indicator type producer"},
        {"LauncherIndicatorState._refreshIndicator", "LauncherIndicatorState._refreshIndicator", "OS4 原逻辑注入校验"},
        {"HotseatLayerGetxController._isSeatsFull", "HotseatLayerGetxController._isSeatsFull", "OS4 原逻辑注入校验"},
        {"HotseatDragHandler._isSeatsFull", "HotseatDragHandler._isSeatsFull", "OS4 原逻辑注入校验"},
        {"HotseatLayerGetxController.setHotSeatItems", "HotseatLayerGetxController.setHotSeatItems", "OS4 原逻辑注入校验"},
        {"HotseatLayerGetxController.resetAnimationAndUpdateDatabase", "HotseatLayerGetxController.resetAnimationAndUpdateDatabase", "OS4 原逻辑注入校验"},
        {"HotseatLayerGetxController._updateDragItemCount", "HotseatLayerGetxController._updateDragItemCount", "OS4 原逻辑注入校验"},
        {"HotseatLayerGetxController._pushDragItem", "HotseatLayerGetxController._pushDragItem", "OS4 原逻辑注入校验"},
        {"DeviceConfig.hotSeatMaxCount", "DeviceConfig.hotSeatMaxCount", "OS4 原逻辑注入校验"},

        {"_insertClearButtonOverlay", "RecentsPageState._insertClearButtonOverlay",
         "功能9 插入清理按钮"},
        {"_buildFoldDockClearContainer", "RecentsPageState._buildFoldDockClearContainer",
         "功能9 折叠屏清理容器"},
        {"_syncVisibility", "_ClearAnimationWidgetState._syncVisibility",
         "功能18 可见性同步"},
        {"initState", "_ClearAnimationWidgetState.initState", "功能18 初始动画"},
        {"GridController.init", "GridController.init", "功能16 网格列数字段写入"},
        {"FolderGridViewGetxController._calibrateItemCellHeight",
         "FolderGridViewGetxController._calibrateItemCellHeight",
         "功能16 格子高度（正方形修正的落点）"},
        {"FolderGridViewGetxController.folderCellWidth",
         "FolderGridViewGetxController.folderCellWidth",
         "功能16 自适应的格子宽度"},
        {"PadRowColHandler.handle", "PadRowColHandler.handle", "功能4 平板行列 handler"},
        {"PhoneRowColHandler.handle", "PhoneRowColHandler.handle", "功能19 手机列数 handler"},
        {"FoldCalculateHandler.handle", "FoldCalculateHandler.handle",
         "功能20 折叠屏行列 handler"},
        {"PhoneCellSizeHandler.calGridSizeByVariable",
         "PhoneCellSizeHandler.calGridSizeByVariable", "功能19 行数（管线拷贝）"},
        {"PhoneDeviceRules._calCellCountYWithGridHeightAndLayoutType",
         "PhoneDeviceRules._calCellCountYWithGridHeightAndLayoutType",
         "功能19 行数（DeviceRules 拷贝）"},
        {"IconConfigHandler.calIconSize", "IconConfigHandler.calIconSize",
         "功能21 手机/通用图标尺寸"},
        {"Q18CalculateHandler._calInnerPortrait", "Q18CalculateHandler._calInnerPortrait",
         "功能21 折叠屏内屏竖屏"},
        {"Q18CalculateHandler._calInnerLandscape", "Q18CalculateHandler._calInnerLandscape",
         "功能21 折叠屏内屏横屏"},
        {"PadCellSizeHandler.calculateIconSize", "PadCellSizeHandler.calculateIconSize",
         "功能21 平板图标尺寸"},
        /*
         * Layout geometry accessors. These are not tweaks features: they are the Dart accessors the
         * layout module hooks to move the workspace/hotseat/indicator/search-bar geometry. They live
         * in the same table because that table is the image's symbol index, and resolving them here is
         * what keeps the hooks address-free: the name comes from the launcher's own build, and the
         * address is read out of the image that is actually loaded. A launcher OTA that moves every
         * function changes none of these names.
         */
        {"GridController.hotSeatsMarginBottom", "GridController.hotSeatsMarginBottom",
         "布局 底栏底部边距"},
        {"HotSeatsConstants2.hotSeatsHeight", "HotSeatsConstants2.hotSeatsHeight", "布局 底栏高度"},
        {"GridSizeCalRules.stableWorkspaceCellPaddingTop",
         "GridSizeCalRules.stableWorkspaceCellPaddingTop", "布局 工作区顶部边距"},
        {"GridController.workspaceCellPaddingBottom", "GridController.workspaceCellPaddingBottom",
         "布局 工作区底部边距"},
        {"GridController.workspaceCellPaddingSide", "GridController.workspaceCellPaddingSide",
         "布局 工作区水平边距"},
        {"GridCellDelegate.performLayout", "GridCellDelegate.performLayout", "布局 网格实际布局代码注入"},
        {"GridOccupiedCellDelegate.performLayout", "GridOccupiedCellDelegate.performLayout", "布局 图标实际布局代码注入"},
        /*
         * The OS4 indicator's own bottom offset. Read out of launcher 7722: it is the only getter
         * between the indicator update paths (`startIndictorAnimation`, `onOrientationChanged`) and
         * the GridController margin chain, so a delta here reaches the indicator alone. The upstream
         * `GridController.workspaceIndicatorMarginBottom` stays listed for the calibration probe —
         * it has exactly one caller, which sits on this same path. The earlier
         * `WidgetLocationCalculator.calIndicatorCenterPosition` entry was dropped: it only feeds the
         * dot indicator at creation time, which OS4 no longer shows.
         */
        {"GridController.workspaceIndicatorMarginBottom",
         "GridController.workspaceIndicatorMarginBottom", "布局 指示器底部边距(上游)"},
        {"_CapsuleIndicatorState.build", "_CapsuleIndicatorState.build", "布局 胶囊组件构建探针"},
        {"Container.build", "Container.build", "布局 胶囊外边距字段布局校验"},
        // Slot 7 reads Container.padding from the function that actually consumes it,
        // rather than trusting Container.build's own read set - `Container.build`
        // delegates padding at the end, so the field is only visible downstream.
        {"Container._paddingIncludingDecoration", "Container._paddingIncludingDecoration",
         "布局 Container.padding 字段消费侧校验"},
        {"LauncherIndicatorState.build", "LauncherIndicatorState.build",
         "布局 胶囊第三动画包装调用点"},
        {"Padding.createRenderObject", "Padding.createRenderObject",
         "布局 胶囊整体位移包装校验"},
        {"LauncherIndicatorState._wrapWithAnimation",
         "LauncherIndicatorState._wrapWithAnimation", "布局 指示器组件包装探针"},
        {"WorkspaceGetxController.indicatorOffsetBottomPortrait",
         "WorkspaceGetxController.indicatorOffsetBottomPortrait", "布局 指示器底部边距"},
        {"GridController.searchBarMarginBottom", "GridController.searchBarMarginBottom",
         "布局 搜索框底部边距"},
        {"GridController.searchBarWidthPx", "GridController.searchBarWidthPx", "布局 搜索框宽度"},
        /*
         * The app-open duration ratio has two Dart boundaries. `getAnimDurationRatio` is the shared
         * entry every consumer funnels through (flight, Folme spring, blur, wallpaper), so replacing
         * its result keeps the whole app-open move in step; `FlightCohort.animDurationRatio` only
         * feeds the icon flight and is kept as the fallback because it is the boundary already
         * proven to bind on launcher 7719. The hook tries them in this order.
         */
        {"getAnimDurationRatio", "getAnimDurationRatio", "动画 时长倍率统一入口"},
        {"FlightCohort.animDurationRatio", "FlightCohort.animDurationRatio",
         "动画 应用打开时长倍率"},
        /*
         * The gear-derived speed factor of the launcher's own animation-rate setting
         * (`miui_home_animation_rate`). Measured on device: app open/close transitions respond to
         * the official gear (~1.27x between the slowest and fastest) but do not respond to
         * `getAnimDurationRatio`, because `TaskFlightAnimationDriver._calculateResponse` takes both
         * inputs and the flight/spring families weight this one. `Utilities
         * .getDefaultGestureAnimMagicSpeed` is also read by `SpringParamsProvider
         * .generateSpringBundlesByAnimType`, `_TaskWidgetState._enterSpringDescription` and the
         * shortcut-menu parameter update, so it is the shared lever for open/close and recents feel.
         */
        {"Utilities.getDefaultGestureAnimMagicSpeed", "Utilities.getDefaultGestureAnimMagicSpeed",
         "动画 手势动画速率因子"},
        /*
         * Candidate accessors for calibration. The debug probe can point a knob at any of these by
         * name, so the layout geometry can be re-calibrated on a new launcher build without a code
         * change; only the eight above ship as wired knobs. Resolving a name costs one comparison in
         * the single symbol scan, and a name that is absent simply resolves to nothing.
         */
        {"HotSeatsConstants2.hotSeatsMarginBottom", "HotSeatsConstants2.hotSeatsMarginBottom",
         "候选 dock 边距视图"},
        {"GridSizeCalRules._calVariableHeight", "GridSizeCalRules._calVariableHeight", "解锁布局工作区自动间距"},
        {"GridSizeCalRules.calVarCellHeight", "GridSizeCalRules.calVarCellHeight", "工作区最终高度上限校验"},
        {"PhoneCellSizeHandler.calVariableValues", "PhoneCellSizeHandler.calVariableValues", "解锁布局工作区自动间距"},
        {"GridConfig.calGridSize", "GridConfig.calGridSize", "候选 布局总算"},
        {"GridController.statusBarHeight", "GridController.statusBarHeight", "候选 状态栏高度"},
        {"GridController.navigateBarHeight", "GridController.navigateBarHeight", "候选 导航栏高度"},
        {"GridController.deviceScreenWidth", "GridController.deviceScreenWidth",
         "候选 设备屏宽（含边距）"},
        {"GridController.gridHeight", "GridController.gridHeight", "候选 网格高度"},
        {"GridController.titleMarginTop", "GridController.titleMarginTop", "候选 标题上边距"},
        {"GridController.dockCellHeight", "GridController.dockCellHeight", "候选 dock 格高"},
        {"GridController.dockCellWidth", "GridController.dockCellWidth", "候选 dock 格宽"},
        {"GridController.deviceHeight", "GridController.deviceHeight", "候选 设备高"},
        {"GridController.searchBarWidthDeltaPx", "GridController.searchBarWidthDeltaPx",
         "候选 搜索框宽增量"},
        {"GridController.iconTopPadding", "GridController.iconTopPadding", "候选 图标上内边距"},
        {"GridController.titleHeight", "GridController.titleHeight", "候选 标题高度"},
        {"AppIconRemoteClick.handleRemoteClick", "AppIconRemoteClick.handleRemoteClick", "文件夹启动应用自动关闭"},
        {"FolderClingGetxController.handleFolderClingClose", "FolderClingGetxController.handleFolderClingClose", "文件夹原始关闭链路"},
        {"Inst|isRegistered", "Inst|isRegistered", "文件夹控制器注册检查"},
        {"FolderGridView._buildScrollableGrid", "FolderGridView._buildScrollableGrid", "文件夹关闭图标坐标一致性"},
        {"SliverGridDelegateWithFixedCrossAxisCount.getLayout", "SliverGridDelegateWithFixedCrossAxisCount.getLayout", "文件夹关闭图标坐标一致性"},
        {"FolderAnimController._setCloseGridItemAnim", "FolderAnimController._setCloseGridItemAnim", "文件夹关闭图标坐标一致性"},
        {"FolderAnimController._refreshCachedGridParams", "FolderAnimController._refreshCachedGridParams", "文件夹关闭图标坐标一致性"},
        {"FolderClingGetxController.gridContainerWidth", "FolderClingGetxController.gridContainerWidth", "文件夹关闭图标坐标一致性"},
        {"FolderAnimController._calcRealIconPos", "FolderAnimController._calcRealIconPos", "文件夹图标正空隙居中边界"},
        {"FolderAnimController._setGridEndLoc", "FolderAnimController._setGridEndLoc", "文件夹关闭图标坐标一致性"},
        {"FolderAnimController._calcFolderPreviewLoc", "FolderAnimController._calcFolderPreviewLoc", "文件夹关闭预览落点一致性"},
        {"FolderAnimController._initGridViewItemAnimParams", "FolderAnimController._initGridViewItemAnimParams", "文件夹动画锚点负空隙截零"},
        {"ShortcutIconDropMixin.folderItemPadding", "ShortcutIconDropMixin.folderItemPadding", "文件夹原始图标内边距"},
        {"GridController.folderCellHeight", "GridController.folderCellHeight", "候选 文件夹格高"},
        {"FolderGridViewGetxController.folderCellHeight",
         "FolderGridViewGetxController.folderCellHeight", "文件夹行间距"},
        {"GridController.folderCellWidth", "GridController.folderCellWidth", "候选 文件夹格宽"},
        {"GridController.screenMarginBottom", "GridController.screenMarginBottom",
         "候选 屏幕下边距"},
        {"GridController.calWidgetHoriPadding", "GridController.calWidgetHoriPadding",
         "候选 小部件水平内边距"},
        {"GridController.miuiWidgetPaddingTop", "GridController.miuiWidgetPaddingTop",
         "候选 小部件上内边距"},
        {"GridController.folderPreviewWidth", "GridController.folderPreviewWidth",
         "候选 文件夹预览宽"},
        {"GridController.realScreenWidth", "GridController.realScreenWidth", "候选 真实屏宽"},
        {"GridController.folderPreviewHeight", "GridController.folderPreviewHeight",
         "候选 文件夹预览高"},
        {"GridController.folderPreviewItemPadding", "GridController.folderPreviewItemPadding",
         "候选 文件夹预览内边距"},
        {"GridController.iconHoriPadding", "GridController.iconHoriPadding", "候选 图标水平内边距"},
        {"DeviceConfig.workspacePaddingTop", "DeviceConfig.workspacePaddingTop",
         "候选 工作区上边距（计算期）"},
        {"DeviceConfig.workspaceCellPaddingSide", "DeviceConfig.workspaceCellPaddingSide",
         "候选 工作区水平边距（计算期）"},
        {"DeviceConfig.workspaceCellPaddingBottom", "DeviceConfig.workspaceCellPaddingBottom",
         "候选 工作区底边距（计算期）"},
        {"GridConfig.workspaceCellSide", "GridConfig.workspaceCellSide", "候选 工作区水平边距"},
        {"GridConfig.workspacePaddingTop", "GridConfig.workspacePaddingTop",
         "候选 工作区上边距"},
        {"GridConfig.workspaceCellPaddingBottom", "GridConfig.workspaceCellPaddingBottom",
         "候选 工作区底边距"},
        {"GridSizeCalRules.stableIndicatorHeight", "GridSizeCalRules.stableIndicatorHeight",
         "候选 指示器高度"},
        {"GridSizeCalRules.screenMarginTop", "GridSizeCalRules.screenMarginTop",
         "候选 屏幕上边距"},
        /*
         * Two names added for read-only probing after the 6309 xref pass (see
         * LAYOUT_HOOK_EVIDENCE.md §5.2). Neither is a shipped target: they exist here so the
         * calibration channel can resolve them, because a name that is absent from this table can
         * not be probed at all - the lookup is an exact match against these needles.
         *
         * `GridConfig.getScreenMarginTop` is the strongest candidate for the workspace's top
         * whitespace: on the current build its only layout-path caller is `GridConfig.calGridSize`
         * (twice), the entry of the size calculation, while the previously assumed candidates are
         * consumed by the search bar and the indicator instead.
         * `PhoneDeviceRules.getScreenTopRation` is the ratio the variable-height grid path reads;
         * whether the phone uses that path at all is exactly what a hit count will tell us.
         */
        {"GridConfig.getScreenMarginTop", "GridConfig.getScreenMarginTop",
         "候选 屏幕上边距（网格尺寸路径）"},
        {"PhoneDeviceRules.getScreenTopRation", "PhoneDeviceRules.getScreenTopRation",
         "候选 屏幕顶部比例（可变高度路径）"},
        {"GridController.currentConfig", "GridController.currentConfig", "候选 当前配置访问器"},
        {"HotSeatsConstants2._dockGridConfig", "HotSeatsConstants2._dockGridConfig",
         "候选 dock 配置访问器"},
        {"GridController.iconConfig", "GridController.iconConfig", "候选 图标配置访问器"},
        {"GridController.getOrientationType", "GridController.getOrientationType",
         "候选 方向类型"},
        /*
         * Folder padding accessors, added for the read-only probe of the folder-interior margins
         * (LAYOUT_HOOK_EVIDENCE.md §17). The settings page has no folder-margin knobs yet; before a
         * knob can be wired, the probe has to show which of these the open-folder layout actually
         * calls, how often, and with which caller. Standard Dart prologues (verified offline against
         * the 6309 image), so the hook path can bind them; `newPadding*` are shared-entry thunks and
         * are deliberately absent - bind_dart_target refuses their prologue.
         */
        {"FolderGridViewGetxController.calGridWidth", "FolderGridViewGetxController.calGridWidth", "文件夹原逻辑布局"},
        {"FolderClingWidget.getFolderClingWidth", "FolderClingWidget.getFolderClingWidth", "文件夹原逻辑布局"},
        {"BigFolderCommonStrategy.calItemSize", "BigFolderCommonStrategy.calItemSize", "大文件夹工作区尺寸原逻辑"},
        {"BigFolderCommonStrategy._calItemSizeByWidth", "BigFolderCommonStrategy._calItemSizeByWidth", "大文件夹预览尺寸原逻辑"},
        {"BigFolderCommonStrategy._calItemSizeByHeight", "BigFolderCommonStrategy._calItemSizeByHeight", "大文件夹预览尺寸原逻辑"},
        {"FolderLayoutStrategy.calItemSize", "FolderLayoutStrategy.calItemSize", "大文件夹工作区尺寸原逻辑"},
        {"FolderIconGetxController.folderIconSize", "FolderIconGetxController.folderIconSize", "大文件夹工作区尺寸原逻辑"},
        {"FolderIconGetxController.checkOrUpdateStrategy", "FolderIconGetxController.checkOrUpdateStrategy", "大文件夹工作区尺寸原逻辑"},
        {"FolderClingGetxController._calcFolderPaddingTop", "FolderClingGetxController._calcFolderPaddingTop", "文件夹原逻辑布局"},
        {"FolderHeaderWidget.build", "FolderHeaderWidget.build", "文件夹标题容器原逻辑"},
        {"AndroidAttributeUtils.convertGravity", "AndroidAttributeUtils.convertGravity", "文件夹 Stack Alignment 语义"},
        {"Stack.updateRenderObject", "Stack.updateRenderObject", "文件夹标题对齐字段校验"},
        {"AndroidAttributeUtils.convertTextAlignment", "AndroidAttributeUtils.convertTextAlignment", "文件夹标题居中独立语义校验"},
        {"_encodeParagraphStyle", "_encodeParagraphStyle", "文件夹标题枚举 index 非 Smi 校验"},
        {"CrossAxisAlignment._getChildCrossAxisOffset", "CrossAxisAlignment._getChildCrossAxisOffset", "文件夹标题列居中校验"},
        {"Flex.updateRenderObject", "Flex.updateRenderObject", "文件夹标题列字段校验"},
        {"RxObjectMixin.value", "RxObjectMixin.value", "文件夹配置接收对象校验"},
        {"FolderHeaderWidget._buildText", "FolderHeaderWidget._buildText", "文件夹原逻辑布局"},
        {"FolderHeaderWidget._buildEditor", "FolderHeaderWidget._buildEditor", "文件夹原逻辑布局"},
        {"_FlutterTextViewState._resolveEffectiveTextAlign", "_FlutterTextViewState._resolveEffectiveTextAlign", "文件夹原逻辑布局"},
        {"FolderGridViewGetxController.folderGridPaddingTop",
         "FolderGridViewGetxController.folderGridPaddingTop", "候选 文件夹网格上边距"},
        {"FolderGridViewGetxController.folderGridPaddingBottom",
         "FolderGridViewGetxController.folderGridPaddingBottom", "候选 文件夹网格下边距"},
        {"FolderGridViewGetxController.folderGridPaddingLeft",
         "FolderGridViewGetxController.folderGridPaddingLeft", "候选 文件夹网格左边距"},
        {"FolderGridViewGetxController.folderGridOuterHorizontalPadding",
         "FolderGridViewGetxController.folderGridOuterHorizontalPadding", "候选 文件格外层水平边距"},
        {"FolderClingGetxController.getCommonFolderHeaderPaddingTop",
         "FolderClingGetxController.getCommonFolderHeaderPaddingTop", "候选 文件夹头部上边距"},
        {"FolderClingGetxController.getCommonFolderHeaderPaddingBottom",
         "FolderClingGetxController.getCommonFolderHeaderPaddingBottom", "候选 文件夹头部下边距"},
        /*
         * Layout aggregators: too large for Dart to inline, so hooking one of these is actually
         * reached. They are the calibration targets for the geometry knobs whose small accessors are
         * inlined away.
         */
        {"HotseatLayerGetxController.calHotseatOffsetY",
         "HotseatLayerGetxController.calHotseatOffsetY", "候选 底栏 Y 偏移"},
        {"HotseatLayerGetxController.calHotseatCenterPosition",
         "HotseatLayerGetxController.calHotseatCenterPosition", "候选 底栏中心位置"},
        {"HotseatLayerGetxController._getTotalHotSeatsMarginBottom",
         "HotseatLayerGetxController._getTotalHotSeatsMarginBottom", "候选 底栏总边距"},
        {"HotseatLayerGetxController.calculatePositionY",
         "HotseatLayerGetxController.calculatePositionY", "候选 底栏位置 Y"},
        {"HotseatLayerGetxController.calculatePositionX",
         "HotseatLayerGetxController.calculatePositionX", "候选 底栏位置 X"},
        {"HotseatLayerGetxController.getCenterLocation",
         "HotseatLayerGetxController.getCenterLocation", "候选 底栏中心点"},
        {"GlobalHotseatWindowManager.updateInsets", "GlobalHotseatWindowManager.updateInsets",
         "候选 底栏窗口 insets"},
        {"WorkspaceGetxController.indicatorOffsetBottomPortrait",
         "WorkspaceGetxController.indicatorOffsetBottomPortrait", "候选 指示器底部偏移（竖屏）"},
        {"GlobalHotseatWindowManager._relayout", "GlobalHotseatWindowManager._relayout",
         "候选 dock 窗口重排"},
        {"GlobalHotseatWindowManager._buildDockLayout", "GlobalHotseatWindowManager._buildDockLayout",
         "候选 dock 布局构建"},
        {"GlobalHotseatWindowManager.dockWindowHeight",
         "GlobalHotseatWindowManager.dockWindowHeight", "候选 dock 窗口高度"},
        {"HotseatLayoutCalculator.calculate", "HotseatLayoutCalculator.calculate",
         "候选 底栏布局计算"},
        {"_DockLightContentState._buildContent", "_DockLightContentState._buildContent",
         "候选 dock 亮色内容构建"},
        {"_RenderDockExcludedBackground._getDockScreenRectNow",
         "_RenderDockExcludedBackground._getDockScreenRectNow", "候选 dock 屏幕矩形"},
        {"HotseatLayerGetxController.backgroundLayout",
         "HotseatLayerGetxController.backgroundLayout", "候选 底栏背景布局"},
        /* Dock window chain, from the static call graph: build -> updateDockHierarchy -> FRB -> Rust. */
        {"GlobalHotseatWindowManager.updateDockHierarchy",
         "GlobalHotseatWindowManager.updateDockHierarchy", "候选 dock 层级更新（Dart）"},
        {"RustLibApiImpl.crateApiWindowUpdateDockHierarchy",
         "RustLibApiImpl.crateApiWindowUpdateDockHierarchy", "候选 dock FRB 出口"},
        {"_HotseatLayerPadState.build", "_HotseatLayerPadState.build", "候选 dock 层级构建者"},
        {"DockAnimRunner._applyLayer", "DockAnimRunner._applyLayer", "候选 dock 动画层"},
};
constexpr size_t kTargetCount = sizeof(kTargets) / sizeof(kTargets[0]);

static_assert(kTargetCount <= kMaxTargetSlots, "kTargets 超出 SymbolIndex 的槽位数（kMaxTargetSlots）");

constexpr uint32_t kXzDictDesired = 8u << 20;
constexpr uint32_t kXzDictMax = 64u << 20;
constexpr size_t kXzOutCap = 16u << 20;
constexpr uint32_t kMaxSections = 256;
constexpr uint32_t kMaxSymbols = 400000;
constexpr uint32_t kMaxFunctionBytes = 0x4000;

bool NameEquals(const char* a, const char* b) { return strcmp(a, b) == 0; }

const char* StrAt(const char* table, size_t tableSize, uint32_t offset) {
    if (table == nullptr || offset >= tableSize) return nullptr;
    const char* s = table + offset;
    const size_t max = tableSize - offset;
    return (memchr(s, '\0', max) != nullptr) ? s : nullptr;
}

bool VaInExecSegment(const Image& image, uint32_t va, size_t bytes = 4) {
    if (!bytes || (va & 3u) || (bytes & 3u) || !image.base || (image.base & 3u)
        || image.segmentCount == 0 || image.segmentCount > std::size(image.segments)
        || uint64_t(bytes) > uint64_t(UINT32_MAX) + 1u - va
        || uintptr_t(va) > UINTPTR_MAX - image.base) return false;
    const uintptr_t address = image.base + uintptr_t(va);
    for (size_t i = 0; i < image.segmentCount; ++i) {
        const Segment& s = image.segments[i];
        if ((s.flags & 7u) != 5u || s.begin < image.base || s.begin >= s.end
            || (s.begin & 3u) || (s.end & 3u)) continue;
        if (address >= s.begin && address < s.end && bytes <= s.end - address) return true;
    }
    return false;
}

bool LooksLikeDartFunction(const Image& image, uint32_t va, const char *name, uint32_t size) {
    if (!name || va < 8 || (va & 3) || size < 8 || size > kMaxFunctionBytes
        || (size & 3) || !VaInExecSegment(image, va, size)) return false;
    const bool text_align = NameEquals(name, "AndroidAttributeUtils.convertTextAlignment");
    const bool grid_width = NameEquals(name, "FolderClingGetxController.gridContainerWidth");
    // Copy just the dispatch prefix, never dereference an execution mapping directly.
    // A failed/partial read cannot classify even when it wrote matching initial words.
    // Tiny normal framed bodies retain their old admission regardless of exact name.
    const size_t required = grid_width ? 7 : text_align ? 3 : 2;
    const size_t count = std::min<size_t>(size / sizeof(uint32_t), required);
    std::array<uint32_t, 7> p{};
    if (!CodeView(image).ReadWords(va, p.data(), count)) return false;
    if (p[0] == 0xA9BF79FDu && p[1] == 0xAA0F03FDu) return true;
    // Exact named read-only semantic roots, not anonymous leaf patch targets.
    if (text_align) return size >= 12 && p[0] == 0xaa0103e3u && p[1] == 0xf1000c7fu
        && (p[2] & 0xff00001fu) == 0x5400000cu;
    if (grid_width) return size >= 28 && (p[0] & 0xffe00fff) == 0xb8400020 && p[1] == 0x8b1c8000
        && (p[5] & 0xffe00fff) == 0xfc400000 && p[6] == 0xd65f03c0;
    if (NameEquals(name, "ShortcutInfoModel.getComponentName") && size == 0x48)
        return p[0] == 0xd28020f1u && p[1] == 0xb8716822u;
    if (NameEquals(name, "allocateTwoByteString") && size == 0xec)
        return p[0] == 0xf94001e2u && p[1] == 0x93407c42u;
    return false;
}

void SetStatus(char* buf, size_t cap, const char* fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(buf, cap, fmt, ap);
    va_end(ap);
}

bool BytesInBuffer(uint64_t offset, uint64_t bytes, size_t total) {
    return offset <= total && bytes <= total - offset;
}

uint64_t DictSizeFromProps(uint8_t d) {
    if (d > 40) return 0;
    return static_cast<uint64_t>(2 | (d & 1u)) << (d / 2 + 11);
}

bool PropsForDictSize(uint64_t want, uint8_t* out) {
    for (uint8_t d = 0; d <= 40; ++d) {
        if (DictSizeFromProps(d) == want) {
            *out = d;
            return true;
        }
    }
    return false;
}

bool PatchBlockDictSize(std::vector<uint8_t>* blob, uint8_t newProps) {
    constexpr size_t kStreamHeader = 12;
    if (blob->size() < kStreamHeader + 12) return false;
    uint8_t* h = blob->data() + kStreamHeader;
    const size_t headerSize = (static_cast<size_t>(h[0]) + 1) * 4;
    if (headerSize < 12 || kStreamHeader + headerSize > blob->size()) return false;
    if (h[1] != 0) return false;
    if (h[2] != 0x21 || h[3] != 1) return false;
    if (DictSizeFromProps(h[4]) == 0) return false;
    h[4] = newProps;
    const uint32_t crc = xz_crc32(h, headerSize - 4, 0);
    for (size_t i = 0; i < 4; ++i) {
        h[headerSize - 4 + i] = static_cast<uint8_t>((crc >> (8 * i)) & 0xFFu);
    }
    return true;
}

uint64_t SymbolNowMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();
}
enum xz_ret RunDecoder(const std::vector<uint8_t>& packed, uint32_t dictMax,
                       std::vector<uint8_t>* out, uint64_t deadline = 0) {
    struct xz_dec* dec = xz_dec_init(XZ_PREALLOC, dictMax);
    if (dec == nullptr) return XZ_MEM_ERROR;
    struct xz_buf buf{}; buf.in = packed.data(); buf.in_size = packed.size();
    buf.out = out->data();
    enum xz_ret ret = XZ_OK;
    for (unsigned round = 0; round < 1024; ++round) {
        if (deadline && SymbolNowMs() >= deadline) break;
        buf.out_size = deadline ? std::min(out->size(), buf.out_pos + size_t(65536)) : out->size();
        const size_t input = buf.in_pos, output = buf.out_pos;
        ret = xz_dec_run(dec, &buf);
        if (ret == XZ_UNSUPPORTED_CHECK) continue;
        if (ret != XZ_OK || (input == buf.in_pos && output == buf.out_pos)
            || buf.out_pos == out->size()) break;
    }
    xz_dec_end(dec);
    if (ret == XZ_STREAM_END && out->size() != buf.out_pos) out->resize(buf.out_pos);
    return ret;
}

}

size_t TargetFunctionCount() { return kTargetCount; }
const TargetFunction& TargetFunctionAt(size_t index) { return kTargets[index]; }

SymbolIndex& SymbolIndex::Instance() {
    static SymbolIndex index;
    return index;
}

void SymbolIndex::ResetForTest() {
    cachePersistTried_ = false;
    loaded_ = false;
    foundCount_ = 0;
    status_[0] = '\0';
    memset(has_, 0, sizeof(has_));
    memset(span_, 0, sizeof(span_));
    attempted_ = false;
}

bool SymbolIndex::Has(const char* needle) const {
    if (!loaded_ || needle == nullptr) return false;
    for (size_t i = 0; i < kTargetCount; ++i) {
        if (NameEquals(kTargets[i].needle, needle)) return has_[i];
    }
    return false;
}

bool SymbolIndex::Find(const char* needle, uint32_t* va, uint32_t* size) const {
    if (!loaded_ || needle == nullptr) return false;
    for (size_t i = 0; i < kTargetCount; ++i) {
        if (!NameEquals(kTargets[i].needle, needle)) continue;
        if (!has_[i]) return false;
        if (va != nullptr) *va = va_[i];
        if (size != nullptr) *size = size_[i];
        return true;
    }
    return false;
}

// A whitelist neighbour is not a function boundary: retain bounds from all STT_FUNC entries.
bool SymbolIndex::SpanFrom(uint32_t va, uint32_t* span) const {
    if (!loaded_ || va == 0 || span == nullptr) return false;
    for (size_t i = 0; i < kTargetCount; ++i) {
        if (has_[i] && va_[i] == va && span_[i] >= 4) {
            *span = span_[i];
            return true;
        }
    }
    return false;
}

namespace {
uint64_t SymbolRegistryId() {
    uint64_t h = 1469598103934665603ull;
    for (const auto& target : kTargets) {
        for (const char* p = target.fullName; *p; ++p) { h ^= uint8_t(*p); h *= 1099511628211ull; }
        h ^= 0; h *= 1099511628211ull;
    }
    return h;
}
uint32_t SymbolRead32(const uint8_t* p) {
    return uint32_t(p[0]) | uint32_t(p[1]) << 8 | uint32_t(p[2]) << 16 | uint32_t(p[3]) << 24;
}
void SymbolPut32(std::vector<uint8_t>& out, uint32_t v) {
    for (unsigned k = 0; k < 4; ++k) out.push_back(uint8_t(v >> (k * 8)));
}
}
bool SymbolIndex::TryLoadCached(const Image& image, uint64_t imageId) {
    if (loaded_) return true;
    if (!CodeView(image).ExecutableRangesOk()) return false;
    if (!imageId) imageId = ImageIdentity(image);
    std::vector<uint8_t> payload;
    if (!LoadSymbolCache(imageId, &payload) || payload.size() != 12 + kTargetCount * 16
        || SymbolRead32(payload.data()) != uint32_t(SymbolRegistryId())
        || SymbolRead32(payload.data() + 4) != uint32_t(SymbolRegistryId() >> 32)
        || SymbolRead32(payload.data() + 8) != kTargetCount) return false;
    std::array<uint32_t, kMaxTargetSlots> va{}, size{}, span{};
    std::array<bool, kMaxTargetSlots> has{};
    int found = 0;
    for (size_t i = 0; i < kTargetCount; ++i) {
        const uint8_t* p = payload.data() + 12 + i * 16;
        const uint32_t flag = SymbolRead32(p);
        va[i] = SymbolRead32(p + 4); size[i] = SymbolRead32(p + 8); span[i] = SymbolRead32(p + 12);
        if (flag > 1 || (!flag && (va[i] || size[i] || span[i]))) return false;
        if (!flag) continue;
        if (!span[i] || span[i] > 0x100000 || (span[i] & 3)
            || !VaInExecSegment(image, va[i], span[i])
            || !LooksLikeDartFunction(image, va[i], kTargets[i].fullName, size[i])) return false;
        has[i] = true; ++found;
    }
    if (!found) return false;
    for (size_t i = 0; i < kTargetCount; ++i) {
        va_[i] = va[i]; size_[i] = size[i]; span_[i] = span[i]; has_[i] = has[i];
    }
    foundCount_ = found; loaded_ = true; attempted_ = true; cachePersistTried_ = true;
    SetStatus(status_, sizeof(status_), "原始镜像符号缓存命中：%d/%zu", found, kTargetCount);
    LOGI("%s；不解压符号表", status_); return true;
}
bool SymbolIndex::SaveCached(const Image& image) {
    if (!loaded_) return false;
    std::vector<uint8_t> payload;
    const uint64_t registry = SymbolRegistryId();
    SymbolPut32(payload, uint32_t(registry)); SymbolPut32(payload, uint32_t(registry >> 32));
    SymbolPut32(payload, uint32_t(kTargetCount));
    for (size_t i = 0; i < kTargetCount; ++i) {
        const bool valid = has_[i] && span_[i] && span_[i] <= 0x100000;
        SymbolPut32(payload, valid ? 1u : 0u);
        SymbolPut32(payload, valid ? va_[i] : 0u); SymbolPut32(payload, valid ? size_[i] : 0u);
        SymbolPut32(payload, valid ? span_[i] : 0u);
    }
    return SaveSymbolCache(ImageIdentity(image), payload);
}

bool SymbolIndex::EnsureLoaded(const Image& image, uint64_t budgetMs) {
    if (attempted_) {
        if (loaded_ && !budgetMs && !cachePersistTried_) {
            cachePersistTried_ = true; (void)SaveCached(image);
        }
        return loaded_;
    }
    const uint64_t deadline = budgetMs ? SymbolNowMs() + std::min(budgetMs, uint64_t(300)) : 0;
    const auto expired = [&]() { return deadline && SymbolNowMs() >= deadline; };
    struct ResetBudgetAttempt {
        SymbolIndex* self; bool bounded;
        ~ResetBudgetAttempt() { if (bounded && !self->loaded()) self->ResetForTest(); }
    } reset{this, budgetMs != 0};
    if (TryLoadCached(image)) return true;
    attempted_ = true;
    if (!CodeView(image).ExecutableRangesOk()) {
        SetStatus(status_, sizeof(status_), "目标执行段范围/权限不合法");
        return false;
    }

    Elf64_Ehdr eh{};
    if (!ReadImageFile(image, 0, &eh, sizeof(eh))) {
        SetStatus(status_, sizeof(status_), "读不到目标文件（%s）", image.path);
        return false;
    }
    if (memcmp(eh.e_ident, ELFMAG, SELFMAG) != 0 || eh.e_ident[EI_CLASS] != ELFCLASS64 ||
        eh.e_ident[EI_DATA] != ELFDATA2LSB || eh.e_ident[EI_VERSION] != EV_CURRENT ||
        eh.e_machine != EM_AARCH64 || eh.e_version != EV_CURRENT ||
        eh.e_shentsize != sizeof(Elf64_Shdr) || eh.e_shnum == 0 ||
        eh.e_shnum > kMaxSections || eh.e_shstrndx >= eh.e_shnum) {
        SetStatus(status_, sizeof(status_), "ELF 节头表不合法（shnum=%u）", eh.e_shnum);
        return false;
    }

    std::vector<Elf64_Shdr> shdrs(eh.e_shnum);
    if (!ReadImageFile(image, eh.e_shoff, shdrs.data(), shdrs.size() * sizeof(Elf64_Shdr))) {
        SetStatus(status_, sizeof(status_), "读不到节头表（off=%#llx）",
                  static_cast<unsigned long long>(eh.e_shoff));
        return false;
    }
    const Elf64_Shdr* sh = shdrs.data();

    const Elf64_Shdr& shstr = sh[eh.e_shstrndx];
    if (shstr.sh_type != SHT_STRTAB || shstr.sh_size == 0 || shstr.sh_size > (1u << 20)) {
        SetStatus(status_, sizeof(status_), ".shstrtab 尺寸/类型不合法");
        return false;
    }
    std::vector<char> shstrBuf(static_cast<size_t>(shstr.sh_size), '\0');
    if (!ReadImageFile(image, shstr.sh_offset, shstrBuf.data(), shstrBuf.size())) {
        SetStatus(status_, sizeof(status_), "读不到 .shstrtab");
        return false;
    }

    const Elf64_Shdr* debugSec = nullptr;
    for (uint32_t i = 0; i < eh.e_shnum; ++i) {
        const char* name = StrAt(shstrBuf.data(), shstr.sh_size, sh[i].sh_name);
        if (name != nullptr && NameEquals(name, ".gnu_debugdata")) {
            debugSec = &sh[i];
            break;
        }
    }
    if (debugSec == nullptr || debugSec->sh_size == 0 ||
        debugSec->sh_size > (64u << 20)) {
        SetStatus(status_, sizeof(status_),
                  "目标 so 里没有 .gnu_debugdata（无法按符号名定位）");
        return false;
    }

    std::vector<uint8_t> packed(static_cast<size_t>(debugSec->sh_size));
    if (!ReadImageFile(image, debugSec->sh_offset, packed.data(), packed.size())) {
        SetStatus(status_, sizeof(status_), "读不到 .gnu_debugdata（%llu 字节）",
                  static_cast<unsigned long long>(debugSec->sh_size));
        return false;
    }

    xz_crc32_init();
#ifdef XZ_USE_CRC64
    xz_crc64_init();
#endif

    std::vector<uint8_t> debug(kXzOutCap);
    uint8_t smallProps = 0;
    const bool patched = PropsForDictSize(kXzDictDesired, &smallProps) &&
                         PatchBlockDictSize(&packed, smallProps);
    if (expired()) return false;
    enum xz_ret ret = RunDecoder(packed, patched ? kXzDictDesired : kXzDictMax, &debug, deadline);
    if (ret != XZ_STREAM_END && patched && !expired()) {
        LOGW("改了块头字典尺寸后解压失败（ret=%d），改回原样重试", static_cast<int>(ret));
        packed.resize(static_cast<size_t>(debugSec->sh_size));
        if (ReadImageFile(image, debugSec->sh_offset, packed.data(), packed.size())) {
            debug.resize(kXzOutCap);
            ret = RunDecoder(packed, kXzDictMax, &debug, deadline);
        }
    }
    if (ret != XZ_STREAM_END) {
        SetStatus(status_, sizeof(status_), "XZ 解压失败（ret=%d）", static_cast<int>(ret));
        return false;
    }
    const size_t debugSize = debug.size();
    packed.clear();
    packed.shrink_to_fit();

    if (debugSize < sizeof(Elf64_Ehdr)) {
        SetStatus(status_, sizeof(status_), "解压出来的调试数据太短");
        return false;
    }
    Elf64_Ehdr deh{};
    memcpy(&deh, debug.data(), sizeof(deh));
    if (memcmp(deh.e_ident, ELFMAG, SELFMAG) != 0 || deh.e_ident[EI_CLASS] != ELFCLASS64 ||
        deh.e_ident[EI_DATA] != ELFDATA2LSB || deh.e_ident[EI_VERSION] != EV_CURRENT ||
        deh.e_machine != EM_AARCH64 || deh.e_version != EV_CURRENT ||
        deh.e_shentsize != sizeof(Elf64_Shdr) || deh.e_shnum == 0 || deh.e_shnum > kMaxSections ||
        !BytesInBuffer(deh.e_shoff, uint64_t(deh.e_shnum) * sizeof(Elf64_Shdr), debugSize)) {
        SetStatus(status_, sizeof(status_), "调试数据不是预期的 ELF");
        return false;
    }
    std::vector<Elf64_Shdr> debugSections(deh.e_shnum);
    memcpy(debugSections.data(), debug.data() + deh.e_shoff, debugSections.size() * sizeof(Elf64_Shdr));
    const Elf64_Shdr* dsh = debugSections.data();

    const Elf64_Shdr* symtab = nullptr;
    const Elf64_Shdr* strtab = nullptr;
    for (uint32_t i = 0; i < deh.e_shnum; ++i) {
        if (dsh[i].sh_type != SHT_SYMTAB) continue;
        if (dsh[i].sh_entsize != sizeof(Elf64_Sym)) continue;
        if (dsh[i].sh_link >= deh.e_shnum || dsh[i].sh_size % sizeof(Elf64_Sym) != 0) continue;
        if (!BytesInBuffer(dsh[i].sh_offset, dsh[i].sh_size, debugSize)) continue;
        symtab = &dsh[i];
        strtab = &dsh[dsh[i].sh_link];
        break;
    }
    if (symtab == nullptr || strtab == nullptr || strtab->sh_type != SHT_STRTAB ||
        !BytesInBuffer(strtab->sh_offset, strtab->sh_size, debugSize)) {
        SetStatus(status_, sizeof(status_), "调试 ELF 里没有可用的符号表");
        return false;
    }

    const uint64_t wideCount = symtab->sh_size / sizeof(Elf64_Sym);
    if (wideCount > kMaxSymbols) {
        SetStatus(status_, sizeof(status_), "符号表异常大（%llu 条）",
                  static_cast<unsigned long long>(wideCount));
        return false;
    }
    const uint32_t count = static_cast<uint32_t>(wideCount);
    const char* strtabData = reinterpret_cast<const char*>(debug.data() + strtab->sh_offset);
    const size_t strtabSize = static_cast<size_t>(strtab->sh_size);

    std::vector<uint32_t> textVas;
    textVas.reserve(count / 4 + 8);
    bool ambiguous[kTargetCount]{};

    for (uint32_t i = 0; i < count; ++i) {
        if ((i & 255u) == 0 && expired()) return false;
        Elf64_Sym sym{};
        memcpy(&sym, debug.data() + symtab->sh_offset + uint64_t(i) * sizeof(sym), sizeof(sym));
        // Neither a data symbol nor a truncated 64-bit address is a function root.
        if (sym.st_shndx == SHN_UNDEF || sym.st_shndx >= deh.e_shnum ||
            !(dsh[sym.st_shndx].sh_flags & SHF_EXECINSTR) ||
            ELF64_ST_TYPE(sym.st_info) != STT_FUNC || sym.st_value == 0 ||
            sym.st_value > UINT32_MAX || (sym.st_value & 3) ||
            sym.st_size > UINT32_MAX || (sym.st_size & 3) ||
            !VaInExecSegment(image, static_cast<uint32_t>(sym.st_value)) ||
            (sym.st_size && !VaInExecSegment(image, static_cast<uint32_t>(sym.st_value), sym.st_size))) continue;
        textVas.push_back(static_cast<uint32_t>(sym.st_value));
        if (sym.st_name == 0) continue;
        const char* name = StrAt(strtabData, strtabSize, sym.st_name);
        if (name == nullptr) continue;
        for (size_t t = 0; t < kTargetCount; ++t) {
            if (!NameEquals(name, kTargets[t].fullName)) continue;
            const uint32_t size = static_cast<uint32_t>(sym.st_size);
            if (has_[t] && va_[t] != sym.st_value) ambiguous[t] = true;
            if (has_[t] && size_[t] >= size) break;
            va_[t] = static_cast<uint32_t>(sym.st_value);
            size_[t] = size;
            has_[t] = true;
            break;
        }
    }

    std::sort(textVas.begin(), textVas.end());
    textVas.erase(std::unique(textVas.begin(), textVas.end()), textVas.end());
    for (size_t t = 0; t < kTargetCount; ++t) {
        if (expired()) return false;
        if (!has_[t]) continue;
        if (ambiguous[t]) { has_[t] = false; va_[t] = size_[t] = span_[t] = 0; continue; }
        const auto next_function = std::upper_bound(textVas.begin(), textVas.end(), va_[t]);
        if (next_function != textVas.end()) {
            const uint32_t span = *next_function - va_[t];
            if (VaInExecSegment(image, va_[t], span)) span_[t] = span;
        }
        if (size_[t] == 0) {
            uint32_t next = 0;
            for (uint32_t candidate : textVas) {
                if (candidate > va_[t] && (next == 0 || candidate < next)) next = candidate;
            }
            size_[t] = (next > va_[t]) ? (next - va_[t]) : 0;
        }
        if (size_[t] == 0 || size_[t] > kMaxFunctionBytes) {
            size_[t] = (size_[t] == 0) ? 0 : kMaxFunctionBytes;
        }
        if (size_[t] == 0 || !LooksLikeDartFunction(image, va_[t], kTargets[t].fullName, size_[t])) {
            has_[t] = false;
            size_[t] = 0;
            va_[t] = 0;
        }
    }
    textVas.clear();
    textVas.shrink_to_fit();
    debug.clear();
    debug.shrink_to_fit();

    foundCount_ = 0;
    for (size_t t = 0; t < kTargetCount; ++t) {
        if (has_[t]) ++foundCount_;
    }
    if (expired()) return false;
    loaded_ = foundCount_ > 0;
    if (loaded_) {
        SetStatus(status_, sizeof(status_), "符号表解析成功：%d/%zu 个目标函数（%u 条符号）",
                  foundCount_, kTargetCount, count);
    } else {
        SetStatus(status_, sizeof(status_), "符号表里没找到任何目标函数（%u 条符号）", count);
    }
    if (loaded_ && !budgetMs) { cachePersistTried_ = true; (void)SaveCached(image); }
    return loaded_;
}

}
