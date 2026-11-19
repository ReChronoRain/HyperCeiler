/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_layout_elf_targets.h"
#include <cmath>

namespace home_back_arrow {
inline constexpr std::array<const char *, 14> names = {"RustBitmap_create",
                                                       "RustBitmap_copyPixelsFromBuffer",
                                                       "RustBitmap_destruct",
                                                       "RustCanvas_draw_bitmap_rect",
                                                       "RustPaint_init",
                                                       "RustPaint_set_alpha",
                                                       "RustPaint_drop",
                                                       "RustPaint_set_filter_bitmap",
                                                       "RustCanvas_draw_path",
                                                       "NativeWindow_dec_strong",
                                                       "Configuration_get_density_dpi",
                                                       "Configuration_get_screen_width_dp",
                                                       "Configuration_get_screen_height_dp",
                                                       "Configuration_get_smallest_screen_width_dp"};
struct Targets {
    uint64_t path = 0, bitmap = 0, positions = 0, vsync = 0, destroy = 0, arc_drop = 0;
    uint32_t height = 0, right = 0, rect = 0, arc_offset = 0;
    std::array<uint64_t, names.size()> imports{};
};
inline std::optional<Targets> resolve(std::span<const std::byte> bytes) {
    using namespace home_layout::elf_targets;
    auto image = parse(bytes);
    if (!image)
        return {};
    uint64_t size = 0;
    for (const auto &s : image->segments)
        if (s.type == 1) {
            if (s.vaddr > 128 * 1024 * 1024 || s.memsz > 128 * 1024 * 1024 - s.vaddr)
                return {};
            size = std::max(size, s.vaddr + s.memsz);
        }
    std::vector<std::byte> snapshot(size);
    for (const auto &s : image->segments)
        if (s.type == 1) {
            if (s.filesz > s.memsz || !nhk::in_image(bytes.size(), s.offset, s.filesz))
                return {};
            std::memcpy(snapshot.data() + s.vaddr, bytes.data() + s.offset, s.filesz);
        }
    auto dynamic = nhk::elf::ElfImage::make(0, snapshot);
    if (!dynamic)
        return {};
    std::array<uint64_t, names.size()> slots{};
    Targets result;
    for (size_t i = 0; i < names.size(); i++) {
        auto slot = dynamic->unique_slot(names[i]);
        if (!slot)
            return {};
        slots[i] = slot->rva;
    }
    for (const auto &s : image->segments)
        if (s.type == 1 && (s.flags & 1))
            for (uint64_t pc = s.vaddr; pc + 16 <= s.vaddr + s.filesz; pc += 4) {
                std::array<uint32_t, 4> words{};
                for (unsigned i = 0; i < 4; i++)
                    words[i] = *image->word(pc + 4 * i);
                auto got = nhk::arm64::decode_plt_got(words, 0, pc);
                if (!got)
                    continue;
                for (size_t i = 0; i < slots.size(); i++)
                    if (*got == slots[i]) {
                        if (result.imports[i])
                            return {};
                        result.imports[i] = pc;
                    }
            }
    for (auto address : result.imports)
        if (!address)
            return {};
    auto eh = image->unwind_header;
    if (eh.size() < 12 || eh[0] != std::byte{1} || eh[3] != std::byte{0x3b})
        return {};
    uint32_t count = nhk::load_le32(eh.data() + 8);
    if (count < 2 || count > (eh.size() - 12) / 8)
        return {};
    uint32_t path_height = 0, bitmap_height = 0;
    for (uint32_t i = 0; i + 1 < count; i++) {
        uint64_t begin =
            uint64_t(int64_t(image->unwind_vaddr) + int32_t(nhk::load_le32(eh.data() + 12 + i * 8)));
        uint64_t end =
            uint64_t(int64_t(image->unwind_vaddr) + int32_t(nhk::load_le32(eh.data() + 20 + i * 8)));
        if (end <= begin || end - begin > 768 || !image->file_offset(begin, end - begin))
            continue;
        auto contains = [&](uint32_t wanted) {
            for (uint64_t pc = begin; pc < end; pc += 4)
                if (image->word(pc) == wanted)
                    return true;
            return false;
        };
        auto calls = [&](uint64_t address) {
            unsigned n = 0;
            for (uint64_t pc = begin; pc < end; pc += 4) {
                auto w = image->word(pc);
                if (!w)
                    continue;
                auto target = nhk::arm64::branch_target(pc, *w, true);
                n += target && *target == address;
            }
            return n;
        };
        if (contains(0x2d410460) && contains(0x2d412aab) && contains(0xb94012a1) && contains(0xaa0203f4) &&
            contains(0xaa0403f5) && calls(result.imports[8]) == 1 && calls(result.imports[5]) == 1) {
            if (result.path)
                return {};
            result.path = begin;
            for (uint64_t pc = begin; pc < end; pc += 4) {
                auto w = *image->word(pc);
                if ((w & 0xffc003ff) == 0xbd4002c1)
                    path_height = ((w >> 10) & 4095) * 4;
            }
        }
        if (contains(0x2d410482) && contains(0x2d412c6a) && contains(0xb9401081) && contains(0xaa0203f4) &&
            calls(result.imports[3]) == 1 && calls(result.imports[5]) == 1) {
            if (result.bitmap)
                return {};
            result.bitmap = begin;
            for (uint64_t pc = begin; pc < end; pc += 4) {
                auto w = *image->word(pc);
                if ((w & 0xffc003ff) == 0xbd400020)
                    bitmap_height = ((w >> 10) & 4095) * 4;
            }
        }
        if (contains(0xbd400042) && contains(0xbd400c43) && contains(0x1e220808) && contains(0x1e230829)) {
            uint32_t side = 0, rect = 0;
            for (uint64_t pc = begin; pc < end; pc += 4) {
                auto w = *image->word(pc);
                if ((w & 0xffc003ff) == 0x39400028)
                    side = (w >> 10) & 4095;
                auto callee = nhk::arm64::branch_target(pc, w, true);
                if (!callee)
                    continue;
                auto bounds = image->bounds(*callee);
                if (!bounds || bounds->begin != *callee || bounds->end - *callee > 160 ||
                    !image->file_offset(*callee, bounds->end - *callee))
                    continue;
                for (uint64_t a = *callee; a < bounds->end; a += 4) {
                    auto op = *image->word(a);
                    if ((op & 0xffc003ff) == 0x91000029 && image->word(a + 4) == 0x3dc00120)
                        rect = (op >> 10) & 4095;
                }
            }
            if (side && rect) {
                if (result.right)
                    return {};
                result.right = side;
                result.rect = rect;
                result.positions = begin;
            }
        }
    }
    if (!result.path || !result.bitmap || !path_height || path_height != bitmap_height ||
        result.right <= path_height || result.right > path_height + 16 || !result.rect ||
        result.rect + 16 > path_height)
        return {};
    result.height = path_height;
    for (uint32_t i = 0; i + 1 < count; i++) {
        uint64_t begin =
            uint64_t(int64_t(image->unwind_vaddr) + int32_t(nhk::load_le32(eh.data() + 12 + i * 8)));
        uint64_t end =
            uint64_t(int64_t(image->unwind_vaddr) + int32_t(nhk::load_le32(eh.data() + 20 + i * 8)));
        if (end <= begin || end - begin > 16384 || !image->file_offset(begin, end - begin))
            continue;
        uint32_t first = *image->word(begin);
        bool drop = (first & 0xffc003ff) == 0x39400008 && ((first >> 10) & 4095) == result.right &&
                    image->word(begin + 4) == 0x7100091f && image->word(begin + 12) == 0xd65f03c0 &&
                    end - begin < 768;
        bool frame = first == 0x6db63bef && end - begin > 1024;
        if (!drop && !frame)
            continue;
        unsigned paint = 0, window = 0, positions = 0;
        bool receiver = false, hidden = false;
        for (uint64_t pc = begin; pc < end; pc += 4) {
            uint32_t w = *image->word(pc);
            if (pc - begin < 80) {
                receiver |= w == 0xaa0003f3;
                hidden |= (w & 0xffc003ff) == 0x39400008 && ((w >> 10) & 4095) == result.height + 4;
            }
            auto target = nhk::arm64::branch_target(pc, w, true);
            if (!target)
                continue;
            paint += *target == result.imports[6];
            window += *target == result.imports[9];
            positions += *target == result.positions;
        }
        if (drop && paint == 3 && window == 1) {
            if (result.destroy)
                return {};
            result.destroy = begin;
        }
        if (frame && receiver && hidden && positions == 2) {
            if (result.vsync)
                return {};
            result.vsync = begin;
        }
    }
    if (!result.vsync || !result.destroy)
        return {};
    for (uint32_t i = 0; i + 1 < count; i++) {
        uint64_t begin =
            uint64_t(int64_t(image->unwind_vaddr) + int32_t(nhk::load_le32(eh.data() + 12 + i * 8)));
        uint64_t end =
            uint64_t(int64_t(image->unwind_vaddr) + int32_t(nhk::load_le32(eh.data() + 20 + i * 8)));
        if (end <= begin || end - begin > 128 || !image->file_offset(begin, end - begin) ||
            image->word(begin + 12) != 0xaa0003f3)
            continue;
        bool weak = false;
        uint32_t offset = 0;
        for (uint64_t pc = begin; pc < end; pc += 4) {
            uint32_t w = *image->word(pc);
            weak |= w == 0x91002268;
            auto to = nhk::arm64::branch_target(pc, w, true);
            if (to && *to == result.destroy && pc >= begin + 4) {
                uint32_t add = *image->word(pc - 4);
                if ((add & 0xffc003ff) == 0x91000000)
                    offset = (add >> 10) & 4095;
            }
        }
        if (weak && offset >= 24 && offset <= 64 && (offset % 8) == 0) {
            if (result.arc_drop)
                return {};
            result.arc_drop = begin;
            result.arc_offset = offset;
        }
    }
    return result.arc_drop ? std::optional<Targets>(result) : std::nullopt;
}
} // namespace home_back_arrow

extern void *hc_back_path_draw_original;
extern void *hc_back_bitmap_draw_original;
extern void *hc_back_view_vsync_original;
extern void *hc_back_view_destroy_original;
extern void *hc_back_density_original;
extern "C" int hc_back_density(void *);
extern "C" void hc_back_path_draw(void *, void *, void *, const float *, const float *);
extern "C" void hc_back_bitmap_draw(void *, void *, void *, const float *, const float *);
extern "C" void hc_back_view_vsync(void *);
extern "C" void hc_back_view_destroy(void *);
void hc_back_arrow_bind(uintptr_t, const home_back_arrow::Targets &);
void hc_back_arrow_refresh();
