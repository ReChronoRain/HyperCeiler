/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Desktop Dart AOT semantic resolver.
 *
 * Everything ARM64-generic (register fields, branches, load/store shapes,
 * PC-relative reconstruction, materialized constants, bitfield extraction)
 * moved to nativehook/arm64_decode.h so Rust/C resolvers reuse it; this file
 * keeps only what is genuinely Dart: the prologue shape, the compressed
 * pointer convention (x28 heap base, LSL #32), the allocation-tag ABI and the
 * scale/animate/set semantic relation. Resolution is fail closed by
 * construction: any ambiguity returns nothing at all.
 */
#pragma once
#include "nativehook/arm64_decode.h"
#include "nativehook/resolver.h"

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>
#include <optional>
#include <span>
#include <vector>

namespace dock_motion {

// The desktop inline patch is four ARM64 words (the backend's trampoline shape).
inline constexpr size_t kPatchTargetWords = 4;

using nhk::arm64::CodeRange;
using nhk::arm64::Match;
using nhk::arm64::rd;
using nhk::arm64::rn;
using nhk::arm64::rm;
using nhk::arm64::is_bl;
using nhk::arm64::is_b;
using nhk::arm64::is_ret;
using nhk::arm64::is_ldur_w;
using nhk::arm64::is_ldur_x;
using nhk::arm64::is_ldur_d;
using nhk::arm64::is_stur_w;
using nhk::arm64::is_stur_x;
using nhk::arm64::is_stur_d;
using nhk::arm64::memory_offset;
using nhk::arm64::is_add_imm_x;
using nhk::arm64::add_imm;
using nhk::arm64::branch_target;
using nhk::arm64::call_target;
using nhk::arm64::at;
using nhk::arm64::materialized_u32;
using nhk::arm64::ubfx;
using nhk::arm64::lsl_amount;
using nhk::arm64::decode_conditional_branch_target;
using nhk::arm64::kMaxFunctionWords;

struct Layout {
    uint32_t params_class_id;
    uint32_t double_class_id;
    int32_t tagged_header_offset;
    uint32_t class_id_shift;
    uint32_t class_id_mask;
    uint32_t alpha_offset;
    uint32_t scale_offset;
    uint32_t surface_offset;
    uint32_t recents_offset;
    uint32_t double_value_offset;
    uint32_t false_from_null;
};
struct UnlockLayout {
    uint32_t state_widget_offset;
    uint32_t widget_cell_offset;
    uint32_t cell_container_offset;
    std::array<int64_t, 5> hotseat_containers;
};
struct UnlockResolution {
    uintptr_t scale;
    /**
     * Return address (LR) of the exact call that feeds the projected scale into the setter.
     *
     * <p>The setter itself is a generic double setter shared by unrelated widgets, so the older
     * receiver-chain filter could not reliably separate the per-frame icon projection from other
     * calls. The call site inside the animation routine can: exactly one call is the projection.
     */
    uintptr_t call_return;
    UnlockLayout layout;
};
struct Resolution {
    uintptr_t scale;
    uintptr_t animate;
    uintptr_t set;
    Layout layout;
    std::optional<UnlockResolution> unlock;
};

inline bool same_unlock_layout(const UnlockLayout &left, const UnlockLayout &right) {
    return left.state_widget_offset == right.state_widget_offset
        && left.widget_cell_offset == right.widget_cell_offset
        && left.cell_container_offset == right.cell_container_offset
        && left.hotseat_containers == right.hotseat_containers;
}
inline bool same_unlock(const std::optional<UnlockResolution> &left,
    const std::optional<UnlockResolution> &right) {
    if (left.has_value() != right.has_value()) return false;
    return !left || (left->scale == right->scale
        && left->call_return == right->call_return
        && same_unlock_layout(left->layout, right->layout));
}

inline bool same_layout(const Layout &left, const Layout &right) {
    return left.params_class_id == right.params_class_id
        && left.double_class_id == right.double_class_id
        && left.tagged_header_offset == right.tagged_header_offset
        && left.class_id_shift == right.class_id_shift
        && left.class_id_mask == right.class_id_mask
        && left.alpha_offset == right.alpha_offset
        && left.scale_offset == right.scale_offset
        && left.surface_offset == right.surface_offset
        && left.recents_offset == right.recents_offset
        && left.double_value_offset == right.double_value_offset
        && left.false_from_null == right.false_from_null;
}
inline bool same_resolution(const Resolution &left, const Resolution &right) {
    return left.scale == right.scale && left.animate == right.animate
        && left.set == right.set && same_layout(left.layout, right.layout)
        && same_unlock(left.unlock, right.unlock);
}

inline bool is_dart_prologue(std::span<const uint32_t> words) {
    return words.size() >= 2 && words[0] == 0xa9bf79fd && words[1] == 0xaa0f03fd;
}

/**
 * Dart compressed-pointer convention: a tagged object is
 * `(heap_base << 32) | raw`, and decompression adds the heap base from x28
 * shifted left by 32. Recognizing the exact shape is what lets the resolver
 * tell field loads of the receiver apart from unrelated spills.
 */
inline bool is_compressed_pointer_add(uint32_t word) {
    return (word & 0xff200000) == 0x8b000000 && rn(word) == rd(word)
        && ((word >> 22) & 3) == 0 // ADD (shifted register), LSL only.
        && rm(word) == 28 && ((word >> 10) & 0x3f) == 32;
}

inline std::vector<Match> functions(std::span<const CodeRange> ranges) {
    std::vector<Match> result;
    for (const auto &range : ranges) {
        for (size_t start = 0; start + 2 <= range.words.size(); ++start) {
            if (!is_dart_prologue(range.words.subspan(start))) continue;
            const size_t available = std::min(kMaxFunctionWords, range.words.size() - start);
            size_t length = 0;
            for (size_t i = 2; i < available; ++i) {
                if (is_ret(range.words[start + i])) {
                    length = i + 1;
                    break;
                }
            }
            if (length != 0) {
                result.push_back({range.address + start * sizeof(uint32_t),
                    range.words.subspan(start, length)});
            }
        }
    }
    return result;
}

struct TagAbi {
    int32_t header_offset;
    uint32_t class_shift;
    uint32_t class_bits;
    uint32_t size_shift;
    uint32_t size_bits;
    uint32_t size_scale;
};
struct Allocation {
    uint32_t tag;
    TagAbi abi;
};

inline bool tag_fields_overlap(uint32_t left_shift, uint32_t left_bits,
    uint32_t right_shift, uint32_t right_bits) {
    const uint32_t left_end = left_shift + left_bits;
    const uint32_t right_end = right_shift + right_bits;
    return left_shift < right_end && right_shift < left_end;
}
inline bool valid_tag_abi(const TagAbi &abi) {
    if (abi.header_offset >= 0 || abi.class_bits == 0 || abi.class_bits > 31
        || abi.size_bits == 0 || abi.size_bits > 16 || abi.size_scale > 8) return false;
    // Allocation tags are materialized and consumed through W registers. A
    // 64-bit UBFX shape would otherwise make the uint32_t extraction truncate
    // data or invoke an undefined shift.
    if (abi.class_shift >= 32 || abi.class_bits > 32 - abi.class_shift
        || abi.size_shift >= 32 || abi.size_bits > 32 - abi.size_shift) return false;
    return !tag_fields_overlap(abi.class_shift, abi.class_bits,
        abi.size_shift, abi.size_bits);
}

/**
 * Recognize an allocation stub behind a call.
 *
 * The stub materializes the pre-filled object header (the tag) and jumps to the
 * allocator; on 6309-style builds it also reads the object header back and
 * extracts the class id right there (ldur x?, [x0, #-1]; ubfx …). 7654-style
 * builds devirtualize that guard away at exactly the allocation site this
 * resolver needs - the stub is just movz/movk/b - while the tag and the
 * allocator are unchanged. The class field's position is a property of the
 * snapshot's object header, shared by every stub in the same image, so a stub
 * without its own guard borrows the header ABI from the image's other stubs
 * (the caller supplies it after a first full pass; `borrowed == nullptr`
 * preserves the old strict behavior). resolve() still verifies the derived ids
 * against live behavior downstream, so a wrong borrow cannot silently hook.
 */
inline std::optional<Allocation> allocation(std::span<const CodeRange> ranges,
    uintptr_t address, const TagAbi *borrowed = nullptr) {
    const auto stub = at(ranges, address, 12);
    const auto tag = materialized_u32(stub, 2);
    if (!tag || stub.size() < 8 || !is_b(stub[2])) return {};
    std::optional<int32_t> header;
    std::optional<std::pair<uint32_t, uint32_t>> class_bits;
    for (size_t i = 3; i + 1 < stub.size(); ++i) {
        if (!is_ldur_x(stub[i]) || rn(stub[i]) != 0) continue;
        const auto bits = ubfx(stub[i + 1], rd(stub[i]), rd(stub[i]));
        if (bits) {
            header = memory_offset(stub[i]);
            class_bits = bits;
            break;
        }
    }
    const auto allocator_address = branch_target(address + 2 * sizeof(uint32_t), stub[2], false);
    if (!header || !class_bits) {
        /* No own guard: only the borrowed header ABI makes the tag decodable. */
        if (!borrowed || !allocator_address) return {};
        header = borrowed->header_offset;
        class_bits = std::make_pair(borrowed->class_shift, borrowed->class_bits);
    }
    const auto allocator = at(ranges, *allocator_address, 2);
    if (allocator.size() != 2) return {};
    const auto size_bits = ubfx(allocator[0], 2, rd(allocator[0]));
    const auto scale = size_bits ? lsl_amount(allocator[1], rd(allocator[0])) : std::nullopt;
    if (!size_bits || !scale) return {};
    const TagAbi abi{*header, class_bits->first, class_bits->second,
        size_bits->first, size_bits->second, *scale};
    if (!valid_tag_abi(abi)) return {};
    return Allocation{*tag, abi};
}
inline uint32_t field_mask(uint32_t bits) {
    return bits >= 32 ? UINT32_MAX : (uint32_t{1} << bits) - 1;
}
inline uint32_t tag_field(uint32_t tag, uint32_t shift, uint32_t bits) {
    if (bits == 0 || shift >= 32 || bits > 32 - shift) return 0;
    return (tag >> shift) & field_mask(bits);
}
inline uint32_t object_size(uint32_t tag, const TagAbi &abi) {
    if (!valid_tag_abi(abi)) return 0;
    return tag_field(tag, abi.size_shift, abi.size_bits) << abi.size_scale;
}
inline uint32_t class_id(uint32_t tag, const TagAbi &abi) {
    if (!valid_tag_abi(abi)) return 0;
    return tag_field(tag, abi.class_shift, abi.class_bits);
}

struct ScalarField {
    uint32_t begin;
    uint32_t end;
};
inline std::optional<ScalarField> scalar_field(int offset, int32_t header_offset,
    uint32_t size, unsigned width, unsigned alignment) {
    // Layout publishes field offsets as uint32_t and the assembly consumes
    // them as unsigned register offsets, so a valid physical location reached
    // through a negative encoded offset is not representable by this ABI.
    if (offset < 0 || header_offset >= 0 || width == 0 || alignment == 0
        || (alignment & (alignment - 1)) != 0) return {};
    // LDUR/STUR offsets are relative to the tagged pointer. The allocation base
    // is header_offset bytes before it: encoded +7 with header -1 is byte 8.
    const int64_t allocation_offset = static_cast<int64_t>(offset) - header_offset;
    if (allocation_offset < static_cast<int64_t>(sizeof(uint64_t))
        || allocation_offset > size || allocation_offset % alignment != 0) return {};
    const auto begin = static_cast<uint32_t>(allocation_offset);
    if (width > size - begin) return {};
    return ScalarField{begin, begin + width};
}
inline bool scalar_fields_overlap(const ScalarField &left, const ScalarField &right) {
    return left.begin < right.end && right.begin < left.end;
}
template<size_t N>
inline bool scalar_fields_disjoint(const std::array<ScalarField, N> &fields) {
    for (size_t left = 0; left < fields.size(); ++left) {
        for (size_t right = left + 1; right < fields.size(); ++right) {
            if (scalar_fields_overlap(fields[left], fields[right])) return false;
        }
    }
    return true;
}

struct Factory {
    Match body;
    Allocation allocation;
    int alpha;
    int scale;
    int surface;
    int recents;
    uint32_t false_from_null;
};

inline std::vector<Factory> factories(std::span<const CodeRange> ranges) {
    /*
     * Two passes because a stub can ship without its own class guard (7654
     * devirtualized the header read away at the site this resolver needs): pass
     * one collects the header ABI from the stubs that still carry it, pass two
     * re-runs the shape match letting guard-less stubs borrow that ABI. A
     * borrowed id is still verified by resolve()'s class checks against live
     * behavior, so a bad borrow refuses instead of hooking.
     */
    const TagAbi *borrowed = nullptr;
    TagAbi borrowed_storage{};
    std::vector<Factory> result;
    for (int pass = 0; pass < 2; ++pass) {
        result.clear();
        for (const auto &range : ranges) {
            for (size_t start = 0; start < range.words.size(); ++start) {
                if (!is_bl(range.words[start])) continue;
                const uintptr_t address = range.address + start * sizeof(uint32_t);
                const auto target = branch_target(address, range.words[start], true);
                const auto created = target
                    ? allocation(ranges, *target, borrowed)
                    : std::nullopt;
                if (!created) continue;
                const size_t available = std::min<size_t>(40, range.words.size() - start);
                size_t length = 0;
                for (size_t i = 1; i < available; ++i) {
                    if (is_ret(range.words[start + i])) {
                        length = i + 1;
                        break;
                    }
                }
                if (length == 0) continue;
                const auto body = range.words.subspan(start, length);
                std::optional<int> alpha;
                std::optional<int> scale;
                std::optional<int> surface;
                std::optional<int> recents;
                std::optional<uint32_t> false_from_null;
                for (size_t i = 1; i < body.size(); ++i) {
                    const uint32_t word = body[i];
                    if (is_stur_x(word) && rd(word) == 31 && rn(word) == 0) {
                        alpha = memory_offset(word);
                    }
                    if (i + 1 < body.size() && is_stur_d(word) && is_stur_d(body[i + 1])
                        && rn(word) == 0 && rn(body[i + 1]) == 0 && rd(word) == rd(body[i + 1])
                        && memory_offset(body[i + 1]) == memory_offset(word) + 8) {
                        scale = memory_offset(word);
                    }
                    if (i + 1 < body.size() && is_ldur_x(word) && rn(word) == 29
                        && memory_offset(word) < 0 && is_stur_w(body[i + 1])
                        && rd(body[i + 1]) == rd(word) && rn(body[i + 1]) == 0) {
                        surface = memory_offset(body[i + 1]);
                    }
                    if (i + 1 < body.size() && is_add_imm_x(word) && rn(word) == 22
                        && is_stur_w(body[i + 1]) && rd(body[i + 1]) == rd(word)
                        && rn(body[i + 1]) == 0) {
                        recents = memory_offset(body[i + 1]);
                        false_from_null = add_imm(word);
                    }
                }
                if (!alpha || !scale || !surface || !recents || !false_from_null) continue;
                const uint32_t size = object_size(created->tag, created->abi);
                const auto alpha_field = scalar_field(*alpha, created->abi.header_offset,
                    size, 8, 8);
                const auto scale_field = scalar_field(*scale, created->abi.header_offset,
                    size, 16, 8);
                const auto surface_field = scalar_field(*surface, created->abi.header_offset,
                    size, 4, 4);
                const auto recents_field = scalar_field(*recents, created->abi.header_offset,
                    size, 4, 4);
                if (!alpha_field || !scale_field || !surface_field || !recents_field
                    || !scalar_fields_disjoint(std::array{
                        *alpha_field, *scale_field, *surface_field, *recents_field})) continue;
                result.push_back({{address, body}, *created, *alpha, *scale, *surface,
                    *recents, *false_from_null});
            }
        }
        if (!result.empty() && borrowed == nullptr) {
            /* Any accepted factory's header ABI describes the same snapshot
             * object header; pass two rescans everything letting the
             * guard-less stubs borrow it. */
            borrowed_storage = result.front().allocation.abi;
            borrowed = &borrowed_storage;
            continue;
        }
        break;
    }
    return result;
}

struct ScaleCallback {
    Match body;
    uintptr_t setter;
    std::array<int, 2> receiver_fields;
};
inline std::vector<ScaleCallback> scale_callbacks(const std::vector<Match> &all_functions) {
    std::vector<ScaleCallback> result;
    for (const auto &function : all_functions) {
        if (function.words.size() < 12 || function.words.size() > 40) continue;
        std::vector<uintptr_t> targets;
        std::vector<int> fields;
        for (size_t i = 0; i < function.words.size(); ++i) {
            if (const auto target = call_target(function, i)) targets.push_back(*target);
            if (i + 1 < function.words.size() && is_ldur_w(function.words[i])
                && is_compressed_pointer_add(function.words[i + 1])
                && rd(function.words[i]) == rd(function.words[i + 1])) {
                fields.push_back(memory_offset(function.words[i]));
            }
        }
        if (targets.size() != 2 || targets[0] != targets[1] || fields.size() != 2
            || fields[0] == fields[1]) continue;
        result.push_back({function, targets[0], {fields[0], fields[1]}});
    }
    return result;
}

struct BoxUse {
    uint32_t tag;
    int header_offset;
    int value_offset;
    int source_offset;
    int receiver_offset;
};
inline std::optional<BoxUse> box_before_call(const Match &function, size_t call) {
    if (call < 3 || call >= function.words.size() || !is_bl(function.words[call])) return {};
    const uint32_t value_store = function.words[call - 1];
    const uint32_t header_store = function.words[call - 2];
    if (!is_stur_d(value_store) || !is_stur_x(header_store)
        || rn(value_store) != rn(header_store)) return {};
    const uint32_t tag_reg = rd(header_store);
    const size_t begin = call > 20 ? call - 20 : 0;
    std::optional<uint32_t> tag;
    for (size_t i = begin; i + 1 < call - 2; ++i) {
        if (const auto value = materialized_u32(function.words.subspan(i), tag_reg)) tag = value;
    }
    if (!tag) return {};
    std::optional<int> source;
    std::optional<int> receiver;
    for (size_t i = begin; i < call - 2; ++i) {
        if (is_ldur_d(function.words[i]) && rd(function.words[i]) == rd(value_store)) {
            source = memory_offset(function.words[i]);
        }
        if (!receiver && i + 1 < call && is_ldur_w(function.words[i])
            && is_compressed_pointer_add(function.words[i + 1])
            && rd(function.words[i]) == rd(function.words[i + 1])) {
            receiver = memory_offset(function.words[i]);
        }
    }
    if (!source || !receiver) return {};
    return BoxUse{*tag, memory_offset(header_store), memory_offset(value_store),
        *source, *receiver};
}

struct SetResolution {
    Match function;
    Allocation boxed;
    uint32_t boxed_value_offset;
};
inline std::vector<BoxUse> box_uses(const Match &function, uintptr_t setter) {
    std::vector<BoxUse> result;
    for (size_t i = 0; i < function.words.size(); ++i) {
        const auto target = call_target(function, i);
        if (!target || *target != setter) continue;
        if (const auto use = box_before_call(function, i)) result.push_back(*use);
    }
    return result;
}
inline std::optional<SetResolution> set_relation(const Match &function,
    const std::vector<BoxUse> &uses, const Factory &factory, const ScaleCallback &scale) {
    if (uses.size() < 3) return {};
    const auto same_box = [&](const BoxUse &use) {
        return use.tag == uses[0].tag && use.header_offset == uses[0].header_offset
            && use.value_offset == uses[0].value_offset;
    };
    if (!std::all_of(uses.begin(), uses.end(), same_box)) return {};
    const auto has_source = [&](int offset) {
        return std::any_of(uses.begin(), uses.end(),
            [&](const BoxUse &use) { return use.source_offset == offset; });
    };
    const auto has_receiver = [&](int offset) {
        return std::any_of(uses.begin(), uses.end(),
            [&](const BoxUse &use) { return use.receiver_offset == offset; });
    };
    if (!has_source(factory.alpha) || !has_source(factory.scale)
        || !has_source(factory.scale + 8)
        || !has_receiver(scale.receiver_fields[0])
        || !has_receiver(scale.receiver_fields[1])) return {};
    const auto &abi = factory.allocation.abi;
    const uint32_t size = object_size(uses[0].tag, abi);
    if (uses[0].header_offset != abi.header_offset
        || !scalar_field(uses[0].value_offset, abi.header_offset, size, 8, 8)) return {};
    return SetResolution{function, {uses[0].tag, abi},
        static_cast<uint32_t>(uses[0].value_offset)};
}

inline std::vector<uintptr_t> calls(const Match &function, size_t limit = SIZE_MAX) {
    std::vector<uintptr_t> result;
    for (size_t i = 0; i < function.words.size() && result.size() < limit; ++i) {
        if (const auto target = call_target(function, i)) result.push_back(*target);
    }
    return result;
}
inline uint32_t normalize_relocatable(uint32_t word) {
    if (is_bl(word) || is_b(word)) return word & 0xfc000000;
    if ((word & 0xff000010) == 0x54000000) return word & ~0x00ffffe0;
    if ((word & 0x7e000000) == 0x34000000) return word & ~0x00ffffe0;
    if ((word & 0x1f000000) == 0x11000000) return word & ~0x003ffc00;
    if ((word & 0x3b000000) == 0x39000000) return word & ~0x003ffc00;
    if ((word & 0x3b200c00) == 0x38000000) return word & ~0x001ff000;
    if ((word & 0x1f800000) == 0x12800000) return word & ~0x001fffe0;
    return word;
}
inline bool shared_dynamic_prelude(const Match &left, const Match &right) {
    size_t left_end = 0;
    size_t right_end = 0;
    unsigned left_calls = 0;
    unsigned right_calls = 0;
    while (left_end < left.words.size() && left_calls < 4) {
        if (is_bl(left.words[left_end])) ++left_calls;
        ++left_end;
    }
    while (right_end < right.words.size() && right_calls < 4) {
        if (is_bl(right.words[right_end])) ++right_calls;
        ++right_end;
    }
    if (left_calls != 4 || right_calls != 4 || left_end != right_end) return false;
    for (size_t i = 0; i < left_end; ++i) {
        if (normalize_relocatable(left.words[i]) != normalize_relocatable(right.words[i])) return false;
    }
    return true;
}
inline std::span<const uint32_t> function_cluster(std::span<const CodeRange> ranges,
    uintptr_t address) {
    for (const auto &range : ranges) {
        if (!range.contains(address)) continue;
        const size_t start = (address - range.address) / sizeof(uint32_t);
        size_t length = std::min<size_t>(512, range.words.size() - start);
        for (size_t i = 2; i + 1 < length; ++i) {
            if (is_dart_prologue(range.words.subspan(start + i))) {
                length = i;
                break;
            }
        }
        return range.words.subspan(start, length);
    }
    return {};
}
inline bool cluster_uses_layout(std::span<const uint32_t> cluster,
    const Factory &factory, const SetResolution &set) {
    bool alpha = false;
    bool scale = false;
    bool scale_y = false;
    unsigned boxed_tags = 0;
    for (size_t i = 0; i < cluster.size(); ++i) {
        if (is_ldur_d(cluster[i])) {
            const int offset = memory_offset(cluster[i]);
            alpha |= offset == factory.alpha;
            scale |= offset == factory.scale;
            scale_y |= offset == factory.scale + 8;
        }
        if (i + 1 < cluster.size()) {
            const auto tag = materialized_u32(cluster.subspan(i), rd(cluster[i]));
            if (tag && *tag == set.boxed.tag) ++boxed_tags;
        }
    }
    return alpha && scale && scale_y && boxed_tags >= 3;
}
inline std::optional<Match> animate_function(std::span<const CodeRange> ranges,
    const std::vector<Match> &all_functions, const Factory &factory,
    const SetResolution &set) {
    const Match &set_function = set.function;
    const auto set_calls = calls(set_function, 4);
    if (set_calls.size() != 4) return {};
    std::optional<Match> result;
    for (const auto &function : all_functions) {
        if (function.address == set_function.address) continue;
        if (calls(function, 4) != set_calls
            || !shared_dynamic_prelude(function, set_function)
            || !cluster_uses_layout(function_cluster(ranges, function.address), factory, set)) continue;
        if (result) return {};
        result = function;
    }
    return result;
}

// ---------------------------------------------------------------------------
// Unlock Hotseat projection
//
// The launcher does not expose its unlock transform through Java. Dart computes each
// _UnlockWidgetState's visual scale with conversionValueFrom3DTo2D and passes the result in d0 to
// a tiny ValueNotifier setter. The routines below recover that setter and the state -> widget ->
// CellLocationInfo.container chain by relationships, not addresses or fixed payload offsets.
// ---------------------------------------------------------------------------

inline bool is_mov_x(uint32_t word, uint32_t destination, uint32_t source) {
    // MOV Xd, Xm is the ORR Xd, XZR, Xm alias with no shift.
    return (word & 0xffe0ffe0) == 0xaa0003e0
        && rd(word) == destination && rm(word) == source;
}

inline std::optional<uint32_t> cmn_x_immediate(uint32_t word, uint32_t source) {
    // CMN Xn, #imm is ADDS XZR, Xn, #imm. Reject the 32-bit and register forms.
    if ((word & 0xffc003ff) != 0xb100001f || rn(word) != source) return {};
    uint32_t value = (word >> 10) & 0xfff;
    if (((word >> 22) & 1) != 0) value <<= 12;
    return value;
}

struct RawDoubleSetter {
    Match body;
    uintptr_t notifier_setter;
    uint32_t boxed_tag;
    int boxed_header_offset;
    int boxed_value_offset;
    int receiver_field_offset;
};

inline std::vector<RawDoubleSetter> raw_double_setters(
    const std::vector<Match> &all_functions) {
    std::vector<RawDoubleSetter> result;
    for (const auto &function : all_functions) {
        if (function.words.size() < 16 || function.words.size() > 40) continue;
        std::vector<uintptr_t> targets;
        for (size_t index = 0; index < function.words.size(); ++index) {
            if (const auto target = call_target(function, index)) targets.push_back(*target);
        }
        if (targets.size() != 1) continue;

        std::optional<int> receiver_field;
        std::optional<uint32_t> receiver_object;
        for (size_t index = 0; index + 1 < function.words.size(); ++index) {
            const uint32_t load = function.words[index];
            if (!is_ldur_w(load) || rn(load) != 1
                || !is_compressed_pointer_add(function.words[index + 1])
                || rd(load) != rd(function.words[index + 1])) continue;
            if (receiver_field) { receiver_field.reset(); break; }
            receiver_field = memory_offset(load);
            receiver_object = rd(load);
        }
        if (!receiver_field || !receiver_object || *receiver_field < 0) continue;

        std::optional<RawDoubleSetter> candidate;
        for (size_t value_index = 1; value_index < function.words.size(); ++value_index) {
            const uint32_t value_store = function.words[value_index];
            if (!is_stur_d(value_store) || rd(value_store) != 0
                || memory_offset(value_store) < 0 || rn(value_store) == 29) continue;
            const uint32_t object = rn(value_store);
            const size_t begin = value_index > 12 ? value_index - 12 : 0;
            for (size_t header_index = begin; header_index < value_index; ++header_index) {
                const uint32_t header_store = function.words[header_index];
                if (!is_stur_x(header_store) || rn(header_store) != object) continue;
                const uint32_t tag_register = rd(header_store);
                std::optional<uint32_t> tag;
                for (size_t materialize = begin; materialize + 1 < header_index; ++materialize) {
                    if (const auto value = materialized_u32(
                            function.words.subspan(materialize), tag_register)) tag = value;
                }
                if (!tag) continue;
                bool passes_receiver = false;
                for (size_t move = value_index + 1; move < function.words.size(); ++move) {
                    if (is_bl(function.words[move])) break;
                    passes_receiver |= is_mov_x(function.words[move], 1, *receiver_object);
                }
                if (!passes_receiver) continue;
                RawDoubleSetter found{function, targets[0], *tag,
                    memory_offset(header_store), memory_offset(value_store), *receiver_field};
                if (candidate) { candidate.reset(); break; }
                candidate = found;
            }
            if (candidate) break;
        }
        if (candidate) result.push_back(*candidate);
    }
    return result;
}

struct HotseatCenter {
    Match body;
    int container_offset;
    std::array<int64_t, 5> containers;
};

inline std::vector<HotseatCenter> hotseat_centers(
    const std::vector<Match> &all_functions) {
    std::vector<HotseatCenter> result;
    for (const auto &function : all_functions) {
        for (size_t index = 0; index + 10 < function.words.size(); ++index) {
            const uint32_t load = function.words[index];
            if (!is_ldur_x(load) || rn(load) != 1 || memory_offset(load) < 0) continue;
            const uint32_t value_register = rd(load);
            std::array<int64_t, 5> containers{};
            std::optional<uintptr_t> equal_target;
            bool valid = true;
            for (size_t item = 0; item < containers.size(); ++item) {
                const size_t compare_index = index + 1 + item * 2;
                const size_t branch_index = compare_index + 1;
                const auto immediate = cmn_x_immediate(
                    function.words[compare_index], value_register);
                const uint32_t branch = function.words[branch_index];
                const auto target = decode_conditional_branch_target(
                    function.address + branch_index * sizeof(uint32_t), branch);
                const uint32_t condition = branch & 0xf;
                if (!immediate || *immediate == 0 || !target
                    || (item < containers.size() - 1 ? condition != 0 : condition != 1)) {
                    valid = false;
                    break;
                }
                if (item < containers.size() - 1) {
                    if (!equal_target) equal_target = target;
                    else if (*equal_target != *target) { valid = false; break; }
                } else if (equal_target && *target == *equal_target) {
                    valid = false;
                    break;
                }
                containers[item] = -static_cast<int64_t>(*immediate);
            }
            if (!valid || !equal_target) continue;
            auto sorted = containers;
            std::ranges::sort(sorted);
            if (std::adjacent_find(sorted.begin(), sorted.end()) != sorted.end()) continue;
            result.push_back({function, memory_offset(load), containers});
        }
    }
    return result;
}

struct CellResolver {
    Match body;
    HotseatCenter center;
    int state_widget_offset;
    int widget_cell_offset;
};

inline std::vector<CellResolver> cell_resolvers(const std::vector<Match> &all_functions,
    const std::vector<HotseatCenter> &centers) {
    std::vector<CellResolver> result;
    for (const auto &center : centers) {
        for (const auto &function : all_functions) {
            for (size_t call = 0; call < function.words.size(); ++call) {
                const auto target = call_target(function, call);
                if (!target || *target != center.body.address) continue;
                std::optional<CellResolver> candidate;
                for (size_t first = 0; first + 3 < call; ++first) {
                    const uint32_t state_load = function.words[first];
                    if (!is_ldur_w(state_load) || rn(state_load) != 0
                        || !is_compressed_pointer_add(function.words[first + 1])
                        || rd(state_load) != rd(function.words[first + 1])) continue;
                    const uint32_t widget_register = rd(state_load);
                    for (size_t second = first + 2; second + 1 < call; ++second) {
                        const uint32_t cell_load = function.words[second];
                        if (!is_ldur_w(cell_load) || rn(cell_load) != widget_register
                            || !is_compressed_pointer_add(function.words[second + 1])
                            || rd(cell_load) != rd(function.words[second + 1])) continue;
                        const uint32_t cell_register = rd(cell_load);
                        bool passed_to_center = false;
                        for (size_t move = second + 2; move < call; ++move) {
                            passed_to_center |= is_mov_x(function.words[move], 1, cell_register);
                        }
                        if (!passed_to_center) continue;
                        CellResolver found{function, center, memory_offset(state_load),
                            memory_offset(cell_load)};
                        if (candidate) { candidate.reset(); break; }
                        candidate = found;
                    }
                }
                if (candidate) result.push_back(*candidate);
            }
        }
    }
    return result;
}

inline const Match *function_at(const std::vector<Match> &functions, uintptr_t address) {
    const auto found = std::ranges::find_if(functions,
        [&](const Match &function) { return function.address == address; });
    return found == functions.end() ? nullptr : &*found;
}

inline bool call_preceded_by_zero_d0(const Match &function, size_t call) {
    const size_t begin = call > 3 ? call - 3 : 0;
    for (size_t index = begin; index < call; ++index) {
        // EOR V0.16B, V0.16B, V0.16B: Dart's canonical raw-double zero.
        if (function.words[index] == 0x6e201c00) return true;
    }
    return false;
}

inline bool plausible_pointer_field(int offset, int32_t header, unsigned alignment) {
    if (offset < 0 || header >= 0 || alignment == 0) return false;
    const int64_t physical = static_cast<int64_t>(offset) - header;
    return physical >= static_cast<int64_t>(sizeof(uint64_t))
        && physical <= 4096 && physical % alignment == 0;
}

inline std::optional<UnlockResolution> resolve_unlock(
    const std::vector<Match> &all_functions, const Layout &dart_layout) {
    auto setters = raw_double_setters(all_functions);
    setters.erase(std::remove_if(setters.begin(), setters.end(), [&](const auto &setter) {
        const uint32_t cid = (setter.boxed_tag >> dart_layout.class_id_shift)
            & dart_layout.class_id_mask;
        return cid != dart_layout.double_class_id
            || setter.boxed_header_offset != dart_layout.tagged_header_offset
            || setter.boxed_value_offset != static_cast<int>(dart_layout.double_value_offset);
    }), setters.end());
    const auto centers = hotseat_centers(all_functions);
    const auto resolvers = cell_resolvers(all_functions, centers);
    std::optional<UnlockResolution> result;
    for (const auto &cell : resolvers) {
        if (!plausible_pointer_field(cell.state_widget_offset,
                dart_layout.tagged_header_offset, 4)
            || !plausible_pointer_field(cell.widget_cell_offset,
                dart_layout.tagged_header_offset, 4)
            || !plausible_pointer_field(cell.center.container_offset,
                dart_layout.tagged_header_offset, 8)) continue;
        for (const auto &owner : all_functions) {
            const auto owner_calls = calls(owner);
            if (std::ranges::find(owner_calls, cell.body.address) == owner_calls.end()) continue;
            for (const uintptr_t nested_address : owner_calls) {
                const Match *nested = function_at(all_functions, nested_address);
                if (nested == nullptr) continue;
                struct SetterCall { size_t index; const RawDoubleSetter *setter; };
                std::vector<SetterCall> setter_calls;
                for (size_t index = 0; index < nested->words.size(); ++index) {
                    const auto target = call_target(*nested, index);
                    if (!target) continue;
                    for (const auto &setter : setters) {
                        if (*target == setter.body.address) setter_calls.push_back({index, &setter});
                    }
                }
                for (size_t first = 0; first < setter_calls.size(); ++first) {
                    if (!call_preceded_by_zero_d0(*nested, setter_calls[first].index)) continue;
                    for (size_t second = first + 1; second < setter_calls.size(); ++second) {
                        if (setter_calls[first].setter->body.address
                            == setter_calls[second].setter->body.address) continue;
                        UnlockResolution candidate{setter_calls[second].setter->body.address,
                            // LR observed by the setter: the instruction after its BL.
                            nested->address
                                + (setter_calls[second].index + 1) * sizeof(uint32_t),
                            {static_cast<uint32_t>(cell.state_widget_offset),
                             static_cast<uint32_t>(cell.widget_cell_offset),
                             static_cast<uint32_t>(cell.center.container_offset),
                             cell.center.containers}};
                        if (result) {
                            if (result->scale == candidate.scale
                                && result->call_return == candidate.call_return
                                && same_unlock_layout(result->layout, candidate.layout)) continue;
                            return {};
                        }
                        result = candidate;
                    }
                }
            }
        }
    }
    return result;
}

/**
 * Resolve the dock's scale/animate/set triple.
 *
 * `candidate_count` (optional) reports the fail-closed gate's view: 0 = no
 * candidate, 1 = exactly one (proceed), >= 2 = several distinct resolutions
 * existed and the run was refused rather than picking one. This keeps the
 * 0/1/many contract visible to the runtime instead of hiding it inside a
 * bare empty optional.
 */
inline std::optional<Resolution> resolve(std::span<const CodeRange> ranges,
    size_t *candidate_count = nullptr) {
    if (candidate_count != nullptr) *candidate_count = 0;
    for (size_t left = 0; left < ranges.size(); ++left) {
        const uintptr_t left_bytes = ranges[left].words.size_bytes();
        if (left_bytes > UINTPTR_MAX - ranges[left].address) return {};
        const uintptr_t left_end = ranges[left].address + left_bytes;
        for (size_t right = left + 1; right < ranges.size(); ++right) {
            const uintptr_t right_bytes = ranges[right].words.size_bytes();
            if (right_bytes > UINTPTR_MAX - ranges[right].address) return {};
            const uintptr_t right_end = ranges[right].address + right_bytes;
            if (ranges[left].address < right_end && ranges[right].address < left_end) return {};
        }
    }
    const auto all_functions = functions(ranges);
    const auto all_factories = factories(ranges);
    const auto all_scales = scale_callbacks(all_functions);
    std::optional<Resolution> result;
    for (const auto &scale : all_scales) {
        for (const auto &function : all_functions) {
            const auto uses = box_uses(function, scale.setter);
            if (uses.size() < 3) continue;
            for (const auto &factory : all_factories) {
                const auto set = set_relation(function, uses, factory, scale);
                if (!set) continue;
                const auto animate = animate_function(ranges, all_functions, factory, *set);
                if (!animate) continue;
                const auto &abi = factory.allocation.abi;
                const uint32_t params_id = class_id(factory.allocation.tag, abi);
                const uint32_t double_id = class_id(set->boxed.tag, abi);
                if (params_id == 0 || double_id == 0 || params_id == double_id) continue;
                Resolution candidate{scale.body.address, animate->address, set->function.address,
                    {params_id, double_id, abi.header_offset, abi.class_shift,
                        field_mask(abi.class_bits), static_cast<uint32_t>(factory.alpha),
                        static_cast<uint32_t>(factory.scale), static_cast<uint32_t>(factory.surface),
                        static_cast<uint32_t>(factory.recents), set->boxed_value_offset,
                        factory.false_from_null}, std::nullopt};
                if (result) {
                    if (same_resolution(*result, candidate)) continue;
                    // Two distinct resolutions: refuse, and tell the runtime how many
                    // collided so the refusal is distinguishable from "not found".
                    if (candidate_count != nullptr) *candidate_count = 2;
                    return {};
                }
                result = candidate;
            }
        }
    }
    if (result) result->unlock = resolve_unlock(all_functions, result->layout);
    if (candidate_count != nullptr && result) *candidate_count = 1;
    return result;
}

/**
 * Contract-shaped output for the NativeHookRuntime: the same resolution plus
 * the three recents hook points plus the optional unlock projection point as
 * `nhk::ResolvedTarget`s (original words
 * attached) and the resolver evidence. Addresses are absolute runtime
 * addresses of the snapshotted generation; `rva` is filled by the caller,
 * which knows the generation's load bias.
 */
struct DartHookTargets {
    Resolution resolution;
    std::array<nhk::ResolvedTarget, 3> targets;
    std::optional<nhk::ResolvedTarget> unlock_target;
    nhk::ResolverEvidence evidence;
};

inline std::optional<DartHookTargets> resolve_hook_targets(
    std::span<const CodeRange> ranges, uint64_t load_bias) {
    size_t candidates = 0;
    const auto resolution = resolve(ranges, &candidates);
    DartHookTargets result;
    result.evidence.candidate_count = candidates;
    result.evidence.stage = candidates == 1 ? "dart-semantic-unique"
        : candidates == 0 ? "dart-semantic-not-found" : "dart-semantic-ambiguous";
    if (!resolution) return {};
    const uintptr_t addresses[3] = {resolution->scale, resolution->animate,
        resolution->set};
    for (size_t i = 0; i < 3; ++i) {
        auto &target = result.targets[i];
        target.kind = nhk::TargetKind::kInline;
        target.rva = addresses[i] >= load_bias ? addresses[i] - load_bias : 0;
        const auto words = at(ranges, addresses[i], kPatchTargetWords);
        target.original_words.assign(words.begin(), words.end());
        // The snapshot must be able to supply the prologue the runtime will
        // verify against live memory; a short read means the resolution cannot
        // be handed over safely.
        if (target.original_words.size() != kPatchTargetWords) return {};
        result.evidence.call_sites.push_back(target.rva);
    }
    if (resolution->unlock) {
        nhk::ResolvedTarget target;
        target.kind = nhk::TargetKind::kInline;
        target.rva = resolution->unlock->scale >= load_bias
            ? resolution->unlock->scale - load_bias : 0;
        const auto words = at(ranges, resolution->unlock->scale, kPatchTargetWords);
        target.original_words.assign(words.begin(), words.end());
        if (target.original_words.size() != kPatchTargetWords) return {};
        result.evidence.call_sites.push_back(target.rva);
        result.unlock_target = std::move(target);
    }
    result.resolution = *resolution;
    return result;
}
} // namespace dock_motion
