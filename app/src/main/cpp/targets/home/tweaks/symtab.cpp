// SPDX-License-Identifier: Apache-2.0
#include "symtab.h"

#include "a64.h"

#include <elf.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <vector>

extern "C" {
#include "xz/xz.h"
}

namespace hometweaks {
namespace {

const TargetFunction kTargets[] = {
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
        {"GridController.workspaceIndicatorMarginBottom",
         "GridController.workspaceIndicatorMarginBottom", "布局 指示器底部边距"},
        {"GridController.searchBarMarginBottom", "GridController.searchBarMarginBottom",
         "布局 搜索框底部边距"},
        {"GridController.searchBarWidthPx", "GridController.searchBarWidthPx", "布局 搜索框宽度"},
        /*
         * Candidate accessors for calibration. The debug probe can point a knob at any of these by
         * name, so the layout geometry can be re-calibrated on a new launcher build without a code
         * change; only the eight above ship as wired knobs. Resolving a name costs one comparison in
         * the single symbol scan, and a name that is absent simply resolves to nothing.
         */
        {"HotSeatsConstants2.hotSeatsMarginBottom", "HotSeatsConstants2.hotSeatsMarginBottom",
         "候选 dock 边距视图"},
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
        {"GridController.folderCellHeight", "GridController.folderCellHeight", "候选 文件夹格高"},
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
        {"GridController.currentConfig", "GridController.currentConfig", "候选 当前配置访问器"},
        {"HotSeatsConstants2._dockGridConfig", "HotSeatsConstants2._dockGridConfig",
         "候选 dock 配置访问器"},
        {"GridController.iconConfig", "GridController.iconConfig", "候选 图标配置访问器"},
        {"GridController.getOrientationType", "GridController.getOrientationType",
         "候选 方向类型"},
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

bool VaInExecSegment(const Image& image, uint32_t va) {
    for (size_t i = 0; i < image.segmentCount; ++i) {
        const Segment& s = image.segments[i];
        if ((s.flags & 0x1u) == 0) continue;
        const uint64_t begin = s.begin - image.base;
        const uint64_t end = s.end - image.base;
        if (static_cast<uint64_t>(va) >= begin && static_cast<uint64_t>(va) + 4 <= end) {
            return true;
        }
    }
    return false;
}

bool LooksLikeDartFunction(const Image& image, uint32_t va) {
    if (!VaInExecSegment(image, va)) return false;
    if (va < 8) return false;
    const uint32_t* p = reinterpret_cast<const uint32_t*>(
            image.base + static_cast<uintptr_t>(va));
    return p[0] == 0xA9BF79FDu  &&
           p[1] == 0xAA0F03FDu ;
}

void SetStatus(char* buf, size_t cap, const char* fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(buf, cap, fmt, ap);
    va_end(ap);
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

enum xz_ret RunDecoder(const std::vector<uint8_t>& packed, uint32_t dictMax,
                       std::vector<uint8_t>* out) {
    struct xz_dec* dec = xz_dec_init(XZ_PREALLOC, dictMax);
    if (dec == nullptr) return XZ_MEM_ERROR;
    struct xz_buf buf {};
    buf.in = packed.data();
    buf.in_size = packed.size();
    buf.out = out->data();
    buf.out_size = out->size();
    enum xz_ret ret = XZ_OK;
    for (int round = 0; round < 4; ++round) {
        ret = xz_dec_run(dec, &buf);
        if (ret != XZ_UNSUPPORTED_CHECK) break;
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
    loaded_ = false;
    foundCount_ = 0;
    status_[0] = '\0';
    memset(has_, 0, sizeof(has_));
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

bool SymbolIndex::EnsureLoaded(const Image& image) {
    if (attempted_) return loaded_;
    attempted_ = true;

    Elf64_Ehdr eh{};
    if (!ReadImageFile(image, 0, &eh, sizeof(eh))) {
        SetStatus(status_, sizeof(status_), "读不到目标文件（%s）", image.path);
        return false;
    }
    if (memcmp(eh.e_ident, ELFMAG, SELFMAG) != 0 || eh.e_ident[EI_CLASS] != ELFCLASS64 ||
        eh.e_shentsize != sizeof(Elf64_Shdr) || eh.e_shnum == 0 ||
        eh.e_shnum > kMaxSections || eh.e_shstrndx >= eh.e_shnum) {
        SetStatus(status_, sizeof(status_), "ELF 节头表不合法（shnum=%u）", eh.e_shnum);
        return false;
    }

    std::vector<uint8_t> shdrs(static_cast<size_t>(eh.e_shnum) * sizeof(Elf64_Shdr));
    if (!ReadImageFile(image, eh.e_shoff, shdrs.data(), shdrs.size())) {
        SetStatus(status_, sizeof(status_), "读不到节头表（off=%#llx）",
                  static_cast<unsigned long long>(eh.e_shoff));
        return false;
    }
    const Elf64_Shdr* sh = reinterpret_cast<const Elf64_Shdr*>(shdrs.data());

    const Elf64_Shdr& shstr = sh[eh.e_shstrndx];
    std::vector<char> shstrBuf(static_cast<size_t>(shstr.sh_size) + 1, '\0');
    if (shstr.sh_size == 0 || shstr.sh_size > (1u << 20) ||
        !ReadImageFile(image, shstr.sh_offset, shstrBuf.data(), shstr.sh_size)) {
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
    enum xz_ret ret = RunDecoder(packed, patched ? kXzDictDesired : kXzDictMax, &debug);
    if (ret != XZ_STREAM_END && patched) {
        LOGW("改了块头字典尺寸后解压失败（ret=%d），改回原样重试", static_cast<int>(ret));
        packed.resize(static_cast<size_t>(debugSec->sh_size));
        if (ReadImageFile(image, debugSec->sh_offset, packed.data(), packed.size())) {
            debug.resize(kXzOutCap);
            ret = RunDecoder(packed, kXzDictMax, &debug);
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
    const Elf64_Ehdr* deh = reinterpret_cast<const Elf64_Ehdr*>(debug.data());
    if (memcmp(deh->e_ident, ELFMAG, SELFMAG) != 0 || deh->e_shentsize != sizeof(Elf64_Shdr) ||
        deh->e_shnum == 0 || deh->e_shoff + static_cast<uint64_t>(deh->e_shnum) * sizeof(Elf64_Shdr) > debugSize) {
        SetStatus(status_, sizeof(status_), "调试数据不是预期的 ELF");
        return false;
    }
    const Elf64_Shdr* dsh = reinterpret_cast<const Elf64_Shdr*>(debug.data() + deh->e_shoff);

    const Elf64_Shdr* symtab = nullptr;
    const Elf64_Shdr* strtab = nullptr;
    for (uint32_t i = 0; i < deh->e_shnum; ++i) {
        if (dsh[i].sh_type != SHT_SYMTAB) continue;
        if (dsh[i].sh_entsize != sizeof(Elf64_Sym)) continue;
        if (dsh[i].sh_link >= deh->e_shnum) continue;
        if (dsh[i].sh_offset + dsh[i].sh_size > debugSize) continue;
        symtab = &dsh[i];
        strtab = &dsh[dsh[i].sh_link];
        break;
    }
    if (symtab == nullptr || strtab == nullptr || strtab->sh_offset + strtab->sh_size > debugSize) {
        SetStatus(status_, sizeof(status_), "调试 ELF 里没有可用的符号表");
        return false;
    }

    const uint32_t count = static_cast<uint32_t>(symtab->sh_size / sizeof(Elf64_Sym));
    if (count > kMaxSymbols) {
        SetStatus(status_, sizeof(status_), "符号表异常大（%u 条）", count);
        return false;
    }
    const Elf64_Sym* syms = reinterpret_cast<const Elf64_Sym*>(debug.data() + symtab->sh_offset);
    const char* strtabData = reinterpret_cast<const char*>(debug.data() + strtab->sh_offset);
    const size_t strtabSize = static_cast<size_t>(strtab->sh_size);

    std::vector<uint32_t> textVas;
    textVas.reserve(count / 4 + 8);

    for (uint32_t i = 0; i < count; ++i) {
        const Elf64_Sym& sym = syms[i];
        if (sym.st_shndx != SHN_UNDEF && sym.st_value != 0 &&
            ELF64_ST_TYPE(sym.st_info) == STT_FUNC) {
            textVas.push_back(static_cast<uint32_t>(sym.st_value));
        }
        if (sym.st_name == 0 || sym.st_value == 0) continue;
        const char* name = StrAt(strtabData, strtabSize, sym.st_name);
        if (name == nullptr) continue;
        for (size_t t = 0; t < kTargetCount; ++t) {
            if (!NameEquals(name, kTargets[t].fullName)) continue;
            const uint32_t size = static_cast<uint32_t>(sym.st_size);
            if (has_[t] && size_[t] >= size) break;
            va_[t] = static_cast<uint32_t>(sym.st_value);
            size_[t] = size;
            has_[t] = true;
            break;
        }
    }

    for (size_t t = 0; t < kTargetCount; ++t) {
        if (!has_[t]) continue;
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
        if (size_[t] == 0 || !LooksLikeDartFunction(image, va_[t])) {
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
    loaded_ = foundCount_ > 0;
    if (loaded_) {
        SetStatus(status_, sizeof(status_), "符号表解析成功：%d/%zu 个目标函数（%u 条符号）",
                  foundCount_, kTargetCount, count);
    } else {
        SetStatus(status_, sizeof(status_), "符号表里没找到任何目标函数（%u 条符号）", count);
    }
    return loaded_;
}

}
