/* SPDX-License-Identifier: AGPL-3.0-or-later */
/* Resolve the Rust launcher layout functions from its own ELF bytes, never OTA offsets. */
#pragma once

#include "home_layout_unwind.h"
#include "nativehook/arm64_decode.h"
#include "nativehook/elf_image.h"
#include "nativehook/nhk_base.h"

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <optional>
#include <span>
#include <string_view>
#include <vector>

namespace home_layout::elf_targets {

struct Targets {
    uint64_t grid_config_initializer = 0;
    uint64_t grid_height = 0;
    uint64_t cell_count_x = 0;
    uint64_t cell_count_y = 0;
    // Optional: the launcher's single nav-bar-height resolver. Absent (0) when this
    // build no longer carries its needle; the hotseat-margin hook is skipped then,
    // while the grid hooks above stay usable.
    uint64_t resolve_dimen_px = 0;
};

struct Image {
    std::span<const std::byte> bytes;
    std::vector<nhk::elf::ProgramSegment> segments;
    std::span<const std::byte> unwind_header;
    uint64_t unwind_vaddr = 0;

    [[nodiscard]] std::optional<uint64_t> file_offset(uint64_t vaddr, size_t count) const {
        for (const auto &segment : segments) {
            if (segment.type != nhk::elf::kProgramTypeLoad || vaddr < segment.vaddr) continue;
            const uint64_t delta = vaddr - segment.vaddr;
            if (delta > segment.filesz || count > segment.filesz - delta) continue;
            if (!nhk::in_image(bytes.size(), segment.offset + delta, count)) return {};
            return segment.offset + delta;
        }
        return {};
    }

    [[nodiscard]] std::optional<uint32_t> word(uint64_t vaddr) const {
        const auto offset = file_offset(vaddr, 4);
        if (!offset) return {};
        return nhk::load_le32(bytes.data() + *offset);
    }

    [[nodiscard]] std::optional<unwind::FunctionBounds> bounds(uint64_t site) const {
        return unwind::lookup(unwind_header, unwind_vaddr, site);
    }
};

inline std::optional<Image> parse(std::span<const std::byte> bytes) {
    const auto segments = nhk::elf::parse_program_segments(bytes);
    if (!segments) return {};
    Image image{bytes, *segments, {}, 0};
    size_t headers = 0;
    for (const auto &segment : image.segments) {
        if (segment.type != 0x6474e550u) continue;
        if (!nhk::in_image(bytes.size(), segment.offset, segment.filesz)) return {};
        image.unwind_header = bytes.subspan(segment.offset, segment.filesz);
        image.unwind_vaddr = segment.vaddr;
        ++headers;
    }
    return headers == 1 ? std::optional<Image>(std::move(image)) : std::nullopt;
}

/* Match one complete log label, and refuse duplicate literals even in the same image. */
inline std::optional<uint64_t> unique_literal(const Image &image, std::string_view needle) {
    std::optional<uint64_t> found;
    for (const auto &segment : image.segments) {
        if (segment.type != nhk::elf::kProgramTypeLoad || segment.filesz < needle.size()
            || (segment.flags & nhk::elf::kFlagExecute) != 0
            || !nhk::in_image(image.bytes.size(), segment.offset, segment.filesz)) continue;
        const auto *begin = reinterpret_cast<const char *>(image.bytes.data() + segment.offset);
        const auto *end = begin + segment.filesz;
        const auto *at = begin;
        while ((at = std::search(at, end, needle.begin(), needle.end())) != end) {
            const uint64_t vaddr = segment.vaddr + static_cast<uint64_t>(at - begin);
            if (found) return {};
            found = vaddr;
            ++at;
        }
    }
    return found;
}

/* The launcher logger addresses the control byte immediately before its UTF-8 text. */
inline bool addresses_literal(uint64_t target, uint64_t literal) {
    return target <= literal && literal - target <= 4;
}

inline std::vector<uint64_t> reference_sites(const Image &image, uint64_t literal) {
    std::vector<uint64_t> sites;
    for (const auto &segment : image.segments) {
        if (segment.type != nhk::elf::kProgramTypeLoad
            || (segment.flags & nhk::elf::kFlagExecute) == 0
            || !nhk::in_image(image.bytes.size(), segment.offset, segment.filesz)) continue;
        for (uint64_t at = 0; at + 8 <= segment.filesz; at += 4) {
            const std::array<uint32_t, 2> pair{
                nhk::load_le32(image.bytes.data() + segment.offset + at),
                nhk::load_le32(image.bytes.data() + segment.offset + at + 4)};
            const auto target = nhk::arm64::decode_address_pair(pair, 0, segment.vaddr + at);
            if (target && addresses_literal(*target, literal)) sites.push_back(segment.vaddr + at + 4);
        }
    }
    return sites;
}

inline std::optional<unwind::FunctionBounds> unique_function(
    const Image &image, uint64_t literal) {
    std::optional<unwind::FunctionBounds> selected;
    for (const uint64_t site : reference_sites(image, literal)) {
        const auto bounds = image.bounds(site);
        if (!bounds) return {};
        if (selected && selected->begin != bounds->begin) return {};
        selected = bounds;
    }
    return selected;
}

struct GetterShape {
    uint64_t lock = 0;
    uint64_t value = 0;
};

/* Two adjacent OnceLock getters share one lock and load neighbouring W0 values. */
inline std::optional<GetterShape> once_getter(const Image &image, uint64_t entry) {
    const auto first = image.word(entry);
    const auto second = image.word(entry + 4);
    const auto third = image.word(entry + 8);
    if (!first || !second || !third
        || (*first & 0xffc003ffu) != 0xd10003ffu  // SUB SP, SP, #imm
        || (*second & 0xffc07fffu) != 0xa9007bfdu // STP x29, x30, [SP,#imm]
        || (*third & 0xffc003ffu) != 0x910003fdu) return {};
    const auto lock_adrp = image.word(entry + 12);
    const auto lock_add = image.word(entry + 16);
    if (!lock_adrp || !lock_add) return {};
    const std::array<uint32_t, 2> pair{*lock_adrp, *lock_add};
    const auto lock = nhk::arm64::decode_address_pair(pair, 0, entry + 12);
    if (!lock) return {};
    std::optional<uint64_t> value;
    for (size_t index = 5; index < 32; ++index) {
        const auto instruction = image.word(entry + index * 4);
        if (!instruction || (*instruction & 0xffc003ffu) != 0xb9400120u) continue;
        const auto previous = image.word(entry + (index - 1) * 4);
        if (!previous || nhk::arm64::rd(*previous) != 9) continue;
        const auto page = nhk::arm64::decode_adrp(entry + (index - 1) * 4, *previous);
        if (!page) continue;
        const uint64_t at = *page + (((*instruction >> 10) & 0xfffu) * 4);
        if (value) return {};
        value = at;
    }
    return value ? std::optional<GetterShape>(GetterShape{*lock, *value}) : std::nullopt;
}

inline std::optional<Targets> resolve(std::span<const std::byte> bytes) {
    const auto image = parse(bytes);
    if (!image) return {};
    const auto grid_literal = unique_literal(*image, " cal_grid_size: x:");
    const auto height_literal = unique_literal(*image,
        " get_grid_height_with_search_and_indicator: square_hot_seat_cell_height:");
    if (!grid_literal || !height_literal) return {};
    const auto grid = unique_function(*image, *grid_literal);
    const auto height = unique_function(*image, *height_literal);
    if (!grid || !height || grid->begin == height->begin) return {};

    // The initializer calls the workspace-height rule and, on either side of that call,
    // the same cell-count-X fallback. The fallback's next unwind entry is Y. This
    // relationship is validated structurally; it is not a saved displacement.
    std::optional<uint64_t> height_call;
    std::vector<std::pair<uint64_t, uint64_t>> getter_calls;
    for (uint64_t site = grid->begin; site + 4 <= grid->end; site += 4) {
        const auto instruction = image->word(site);
        if (!instruction) return {};
        const auto target = nhk::arm64::branch_target(site, *instruction, true);
        if (!target) continue;
        if (*target == height->begin) {
            if (height_call) return {};
            height_call = site;
        }
        if (once_getter(*image, *target)) getter_calls.emplace_back(site, *target);
    }
    if (!height_call) return {};
    std::optional<uint64_t> x;
    for (const auto &[before_site, target] : getter_calls) {
        if (before_site >= *height_call || *height_call - before_site > 512) continue;
        const bool after = std::any_of(getter_calls.begin(), getter_calls.end(),
            [&](const auto &call) { return call.second == target
                && call.first > *height_call && call.first - *height_call <= 512; });
        if (!after) continue;
        if (x && *x != target) return {};
        x = target;
    }
    if (!x) return {};
    const auto x_bounds = image->bounds(*x + 4);
    const auto x_shape = once_getter(*image, *x);
    if (!x_bounds || !x_shape || x_bounds->begin != *x) return {};
    const uint64_t y = x_bounds->end;
    const auto y_shape = once_getter(*image, y);
    if (!y_shape || x_shape->lock != y_shape->lock
        || x_shape->value + sizeof(uint32_t) != y_shape->value) return {};

    // resolve_dimen_px is the launcher's only framework-dimen resolver that hardcodes
    // "navigation_bar_height" (21 bytes) inside its body, so hooking its return value
    // is scoped to that one resource by construction. Its log needle is unique: the
    // shorter " resolve_dimen_px: " is also a prefix of the other log label, while
    // this one (length-prefixed by \xc0\x1f in place, hence no trailing separator)
    // matches exactly one site.
    Targets targets{grid->begin, height->begin, *x, y, 0};
    const auto dimen_literal = unique_literal(*image, " resolve_dimen_px: resource_id:");
    if (dimen_literal) {
        const auto dimen = unique_function(*image, *dimen_literal);
        if (dimen && dimen->begin != grid->begin && dimen->begin != height->begin
            && dimen->begin != *x && dimen->begin != y) {
            targets.resolve_dimen_px = dimen->begin;
        }
    }
    return targets;
}

} // namespace home_layout::elf_targets
