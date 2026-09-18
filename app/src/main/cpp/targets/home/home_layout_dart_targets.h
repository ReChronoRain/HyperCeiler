/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Dart AOT structural resolver for the HyperOS 4 launcher's hotseat geometry.
 *
 * Resolution follows the same discipline as dock_native_resolver.h: targets are
 * identified by SHAPE and RELATIONSHIP inside the shipped image, never by symbol
 * name, never by a stored displacement, and any ambiguity refuses the whole
 * family instead of picking a candidate. The Dart symbol table that libapp.so
 * happens to ship is deliberately NOT consulted here - it is only an independent
 * second path for tests and diagnostics (two independent paths agreeing on the
 * same address is what makes a resolution trustworthy).
 *
 * ARM64 generics (branch/load decoding) live in nativehook/arm64_decode.h so this
 * file only carries what is genuinely Dart:
 *
 *   prologue   stp x29,x30,[x15,#-0x10]!   x15 is the Dart stack, not AAPCS.
 *              mov x29,x15
 *   body       the first bl is the configuration accessor: every geometry getter
 *              of one controller calls the SAME accessor and then reads one
 *              scalar out of the returned object. That shared callee is the
 *              relationship that groups getters into a family - no address is
 *              stored, the grouping is discovered per image.
 *   read       the scalar is either a direct field of the returned object
 *              (ldur dN,[x0,#imm]) or a field of a nested object reached through
 *              the compressed-pointer convention (ldur wN,[x0,#imm] followed by
 *              add xN,xM,x28,lsl #32 and a second load).
 *   epilogue   mov x15,x29 ; ldp x29,x30,[x15],#0x10 ; ret
 *
 * Roles inside a family come from two further structural facts:
 *
 *   pair       exactly one function in the image calls exactly two members of the
 *              same family and combines them with plain additions - the
 *              launcher's vertical extent total (height + marginBottom + consts).
 *              An aggregation that also divides or subtracts (the title-relative
 *              calculation) is rejected, which is what separates the two.
 *   role       inside that pair the margin member materialises zero in its own
 *              body (the non-negative clamp / feature-gated zero idiom,
 *              `eor vN.16b,vN.16b,vN.16b`), while the height member is a plain
 *              field read with no zero materialisation.
 *
 * Every step is fail closed: a family whose pair aggregation is not unique, or a
 * pair the zero idiom does not separate, is reported and skipped - never guessed.
 */
#pragma once

#include "nativehook/elf_image.h"
#include "nativehook/nhk_base.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <optional>
#include <span>
#include <string>
#include <string_view>
#include <vector>

namespace home_layout::dart_targets {

/* Instruction encodings this resolver recognises. All are ARM64-generic shapes. */
constexpr uint32_t kStpX29X30X15Pre = 0xA9BF79FDu;   // stp x29,x30,[x15,#-0x10]!
constexpr uint32_t kMovX29X15 = 0xAA0F03FDu;         // mov x29,x15
constexpr uint32_t kMovX15X29 = 0xAA1D03EFu;         // mov x15,x29
constexpr uint32_t kLdpX29X30X15Post = 0xA8C179FDu;  // ldp x29,x30,[x15],#0x10
constexpr uint32_t kReturn = 0xD65F03C0u;            // ret

/* Longest body a scalar getter may have before the shape is considered unrelated. */
constexpr size_t kMaxGetterInsns = 48;
/* How far back a call site searches for the prologue that owns it. */
constexpr size_t kMaxPrologueBackScan = 4096;
/*
 * Aggregations (the extent totals that sum several geometry getters) are much
 * longer than the getters themselves, so their body scan gets its own window.
 */
constexpr size_t kMaxAggregationInsns = 4096;
/*
 * How many logged items a function must own before it is accepted as the layout aggregator. The
 * real one owns most of them; this floor keeps a stray match on the same logging idiom from
 * becoming the pool-capture site, whose failure mode would be a silently unreadable label pool.
 */
constexpr size_t kMinAggregatorItems = 8;

struct Getter {
    uint32_t va = 0;      // entry address (image VA)
    uint32_t length = 0;  // body length in instructions, prologue and epilogue included
    uint32_t anchor = 0;  // resolved configuration accessor (see families())
    uint32_t field = 0;   // immediate of the scalar load (diagnostics only)
    bool nested = false;  // scalar reached through a compressed-pointer hop
    bool zeroes = false;  // body materialises a vector zero: the non-negative clamp idiom
    /*
     * Every callee of the body. Family membership is decided by which of these is
     * shared with other getters, not by call order: a margin getter consults a
     * feature guard before the accessor, so "first bl" would file it elsewhere.
     */
    std::vector<uint32_t> callees;
};

/*
 * Logged geometry items.
 *
 * The layout aggregator logs every value it computes with a label, and each
 * logged value is obtained either by calling a geometry getter (devirtualised,
 * class-checked) or by reading the field inline. That pair of facts is a
 * relationship we can use: the item's label slot tells us WHICH knob it is, and
 * its value source tells us WHERE the knob lives.
 *
 * The label is loaded from the object pool, whose address (PP, x27) is only
 * known at runtime. The trampoline that captures PP and the heap base (x28) at
 * the aggregator's entry closes that gap, after which the string at
 * PP + pool_offset can be read and matched by content - the same content-based
 * addressing the Rust side uses for its needles.
 */
struct LogItem {
    uint32_t site = 0;        // address of the label load (diagnostics)
    uint32_t pool_offset = 0; // label slot, relative to the object pool
    uint32_t target = 0;      // geometry getter the item calls, 0 when inline
    uint32_t field = 0;       // inline read's immediate when no call was made
    bool called = false;
};

/* A knob: its label (content) and where its value comes from. */
struct Knob {
    const char *label = nullptr;
    uint32_t getter = 0;
    bool resolved() const { return getter != 0; }
};

struct Family {
    uint32_t anchor = 0;
    std::vector<Getter> members;
    uint32_t aggregation = 0; // function summing two members, 0 when not unique
    uint32_t height = 0;
    uint32_t margin = 0;
    const char *rejected = nullptr; // non-null: why this family was refused
    bool resolved() const { return height != 0 && margin != 0; }
};

struct View {
    uint32_t va = 0;      // the forwarding getter
    uint32_t source = 0;  // the scalar getter it delegates to
};

struct Resolution {
    std::vector<Getter> getters;
    std::vector<View> views;
    std::vector<Family> families;
    /* The aggregator's logged items: (label slot, value source) for the labelled knobs. */
    std::vector<LogItem> items;
    size_t resolved_families = 0;
    /*
     * The two knob targets. `margin`/`height` are the layout-side scalar getters
     * (the values the geometry aggregation consumes); `margin_view`/`height_view`
     * are the delegating views that expose the same values to other subsystems.
     * All four are filled only when the relations were unambiguous.
     */
    uint32_t margin = 0;
    uint32_t margin_view = 0;
    uint32_t height = 0;
    uint32_t height_view = 0;
    /* Layout-side view of the height (the margin's family, same field). Zero when
       several members share that field immediate, i.e. the offset is a coincidence. */
    uint32_t height_alt = 0;
    /*
     * The geometry aggregator: the function that calls the most members of the
     * widest family. Its entry is where the object pool (PP) and the heap base
     * (x28) can be captured, which is what the labelled knobs need.
     */
    uint32_t aggregator = 0;
    const char *rejected = nullptr;
    bool ok() const { return margin != 0 && height != 0; }
};

namespace detail {

inline bool is_bl(uint32_t word) { return (word >> 26) == 0x25u; }

inline std::optional<uint32_t> bl_target(uint32_t site, uint32_t word) {
    if (!is_bl(word)) return {};
    int32_t offset = static_cast<int32_t>(word & 0x03FFFFFFu);
    if ((offset & 0x02000000) != 0) offset -= 0x04000000;
    return static_cast<uint32_t>(static_cast<int64_t>(site) + (static_cast<int64_t>(offset) << 2));
}

/* SIMD&FP 64-bit unaligned load: the scalar read every geometry getter ends with. */
inline bool is_ldur_double(uint32_t word) { return (word & 0xFFC00C00u) == 0xFC400000u; }
/*
 * Frame-relative reloads are not object fields. x29 is the frame pointer and x15
 * the Dart stack, so a double load through either is a spill being read back -
 * counting it would hide a getter whose real read is nested.
 */
inline bool is_frame_relative(uint32_t word) {
    const uint32_t base = (word >> 5) & 0x1Fu;
    return base == 29u || base == 15u;
}
/* 32-bit unaligned load: the compressed-pointer field of a nested object. */
inline bool is_ldur_word(uint32_t word) { return (word & 0xFFE00C00u) == 0xB8400000u; }
/*
 * add xN,xM,x28,lsl #32: the Dart compressed-pointer decompression step. The
 * 0xFFE0FC00 mask covers the register fields, so x28 as the addend has to be
 * checked on the raw word - the mask cannot express it.
 */
inline bool is_heap_add(uint32_t word) {
    return (word & 0xFFE0FC00u) == 0x8B008000u && ((word >> 16) & 0x1Fu) == 28u;
}
/*
 * Zero materialisation: `eor vN.16b,vN.16b,vN.16b`, either Q width. The launcher
 * writes this whenever a geometry value is clamped to non-negative or gated to
 * zero, which is what separates the hotseat margin from its sibling height.
 */
inline bool is_zero_materialise(uint32_t word) { return (word & 0xBF20FC00u) == 0x2E201C00u; }
inline bool is_ldp_x29_x30(uint32_t word) { return (word & 0xFFFFFC00u) == 0xA8C17800u; }
/*
 * Static singleton accessor. The launcher's controllers expose their
 * configuration object through a lazily initialised static field: the body reads
 * the thread's field table (x26) and compares the slot against the canonical
 * false object held in w22 before initialising it. Ubiquitous helpers such as
 * int.toDouble also read a field, so both marks are required - that comparison
 * is what separates a controller accessor from a conversion helper, and it is
 * what stops the family relation from collapsing into a helper's callers.
 */
inline bool is_thread_static_load(uint32_t word) {
    return (word & 0xFFC00000u) == 0xF9400000u && ((word >> 5) & 0x1Fu) == 26u;
}
inline bool is_canonical_false_compare(uint32_t word) {
    return (word & 0xFFFFFC1Fu) == 0x6B16001Fu; // cmp wN, w22
}
/*
 * Double-precision arithmetic, verified against this image's own instructions:
 * FADD masks to 0x1E602800 and FDIV to 0x1E601800. Division is what marks an
 * aggregation as "not a plain sum", which is how the title-relative calculation
 * (divide, then subtract) is kept away from the vertical extent total.
 */
inline bool is_fadd(uint32_t word) { return (word & 0xFFE0FC00u) == 0x1E602800u; }
inline bool is_fdiv(uint32_t word) { return (word & 0xFFE0FC00u) == 0x1E601800u; }

/*
 * Scalar getter shape over one candidate entry: prologue, body with exactly one
 * relationship anchor and at least one scalar read, then the standard epilogue.
 */
inline std::optional<Getter> shape_at(std::span<const uint32_t> code, size_t first, uint32_t base) {
    const size_t count = code.size();
    if (first + 4 >= count) return {};
    if (code[first] != kStpX29X30X15Pre || code[first + 1] != kMovX29X15) return {};

    const size_t limit = std::min(count, first + kMaxGetterInsns);
    size_t end = 0;
    for (size_t at = first + 2; at + 2 < limit; ++at) {
        if (code[at] == kMovX15X29 && is_ldp_x29_x30(code[at + 1]) && code[at + 2] == kReturn) {
            end = at + 3;
            break;
        }
    }
    if (end == 0) return {};

    Getter getter;
    getter.va = base + static_cast<uint32_t>(first * 4);
    getter.length = static_cast<uint32_t>(end - first);
    size_t scalar_at = 0;
    size_t scalar_count = 0;
    for (size_t at = first + 2; at < end; ++at) {
        const uint32_t word = code[at];
        if (const auto target = bl_target(base + static_cast<uint32_t>(at * 4), word)) {
            if (std::find(getter.callees.begin(), getter.callees.end(), *target)
                == getter.callees.end()) {
                getter.callees.push_back(*target);
            }
            continue;
        }
        if (is_ldur_double(word) && !is_frame_relative(word)) {
            ++scalar_count;
            scalar_at = at;
            getter.field = (word >> 12) & 0x1FFu;
        }
        if (is_zero_materialise(word)) getter.zeroes = true;
    }
    if (getter.callees.empty() || scalar_count != 1) return {};
    /*
     * The nested form loads a compressed field first and decompresses it before
     * the scalar load; requiring that order keeps an unrelated spill from being
     * read as a nested reach.
     */
    for (size_t at = first + 2; at < scalar_at; ++at) {
        if (!is_ldur_word(code[at])) continue;
        for (size_t hop = at + 1; hop < scalar_at; ++hop) {
            if (is_heap_add(code[hop])) {
                getter.nested = true;
                break;
            }
        }
        break;
    }
    return getter;
}

/* Walk back to the prologue that owns `index`, so call sites can be grouped. */
inline uint32_t enclosing_function(std::span<const uint32_t> code, size_t index, uint32_t base) {
    const size_t floor = index > kMaxPrologueBackScan ? index - kMaxPrologueBackScan : 0;
    for (size_t at = index; at > floor; --at) {
        if (code[at] == kStpX29X30X15Pre) return base + static_cast<uint32_t>(at * 4);
    }
    return 0;
}

/* Body extent of the function starting at `first`, or 0 when it does not end. */
inline size_t function_end(std::span<const uint32_t> code, size_t first,
    size_t max_insns = kMaxGetterInsns) {
    const size_t limit = std::min(code.size(), first + max_insns);
    for (size_t at = first + 2; at + 2 < limit; ++at) {
        if (code[at] == kMovX15X29 && is_ldp_x29_x30(code[at + 1]) && code[at + 2] == kReturn) {
            return at + 3;
        }
    }
    return 0;
}

/* True when the function at `first` is a lazily initialised static accessor. */
inline bool is_static_accessor(std::span<const uint32_t> code, size_t first) {
    if (first + 4 >= code.size() || code[first] != kStpX29X30X15Pre) return false;
    const size_t end = function_end(code, first);
    if (end == 0) return false;
    bool thread_load = false;
    bool false_compare = false;
    for (size_t at = first + 2; at < end; ++at) {
        if (is_thread_static_load(code[at])) thread_load = true;
        if (is_canonical_false_compare(code[at])) false_compare = true;
    }
    return thread_load && false_compare;
}

} // namespace detail

/* Discover every scalar getter in the image. Addresses come from the image itself. */
inline std::vector<Getter> getters(std::span<const uint32_t> code, uint32_t base) {
    std::vector<Getter> found;
    for (size_t first = 0; first + 4 < code.size(); ++first) {
        if (code[first] != kStpX29X30X15Pre) continue;
        if (const auto getter = detail::shape_at(code, first, base)) found.push_back(*getter);
    }
    return found;
}

/*
 * Delegating views.
 *
 * Several subsystems read the same geometry value through a thin wrapper: the
 * wrapper returns zero when its own feature is inactive and otherwise forwards
 * to the value's real getter. Structurally the wrapper is a getter frame with no
 * object-field read of its own, the non-negative zero idiom, and exactly one
 * callee that is itself a scalar getter - that callee is the source of truth.
 * This is what lets the resolver pick the single value both the layout hub and
 * the window-extent total end up using, without naming either of them.
 */
inline std::vector<View> views(std::span<const uint32_t> code, uint32_t base,
    const std::vector<Getter> &getters) {
    std::vector<View> found;
    for (size_t first = 0; first + 4 < code.size(); ++first) {
        if (code[first] != kStpX29X30X15Pre) continue;
        const size_t limit = std::min(code.size(), first + kMaxGetterInsns);
        size_t end = 0;
        for (size_t at = first + 2; at + 2 < limit; ++at) {
            if (code[at] == kMovX15X29 && detail::is_ldp_x29_x30(code[at + 1])
                && code[at + 2] == kReturn) {
                end = at + 3;
                break;
            }
        }
        if (end == 0) continue;

        bool zeroes = false;
        bool reads_field = false;
        std::vector<uint32_t> getter_callees;
        for (size_t at = first + 2; at < end; ++at) {
            const uint32_t word = code[at];
            if (detail::is_zero_materialise(word)) zeroes = true;
            if (detail::is_ldur_double(word) && !detail::is_frame_relative(word)) {
                reads_field = true;
            }
            const auto target = detail::bl_target(base + static_cast<uint32_t>(at * 4), word);
            if (!target) continue;
            const bool is_getter = std::any_of(getters.begin(), getters.end(),
                [&](const Getter &g) { return g.va == *target; });
            if (is_getter && std::find(getter_callees.begin(), getter_callees.end(), *target)
                    == getter_callees.end()) {
                getter_callees.push_back(*target);
            }
        }
        if (reads_field || !zeroes || getter_callees.size() != 1) continue;
        found.push_back(View{base + static_cast<uint32_t>(first * 4), getter_callees.front()});
    }
    return found;
}

/*
 * Owner table: the entry address of the function that contains each instruction.
 *
 * One forward pass replaces a backward prologue scan per query, which matters
 * because the height rule asks "who calls this" for every branch in a 19 MB
 * instruction stream. Every Dart AOT function starts with the prologue this
 * resolver already recognises, so the last prologue seen owns what follows.
 */
inline std::vector<uint32_t> owner_table(std::span<const uint32_t> code, uint32_t base) {
    std::vector<uint32_t> owners(code.size(), 0);
    uint32_t current = 0;
    for (size_t at = 0; at < code.size(); ++at) {
        if (code[at] == kStpX29X30X15Pre) current = base + static_cast<uint32_t>(at * 4);
        owners[at] = current;
    }
    return owners;
}

/*
 * Group getters into families and apply the pair/role rules.
 *
 * A family is accepted only when exactly one function calls exactly two of its
 * members and combines them with plain additions, and the zero idiom belongs to
 * exactly one of that pair. Everything else is refused with a reason.
 */
inline std::vector<Family> families(std::vector<Getter> &all,
    std::span<const uint32_t> code, uint32_t base) {
    /*
     * The accessor of a family is the callee that the most getters have in
     * common; each getter joins the family of the shared callee it consults most
     * often. Call order is deliberately ignored, because a getter such as the
     * hotseat margin checks a feature guard before it reaches the accessor.
     */
    struct Count {
        uint32_t target = 0;
        size_t count = 0;
    };
    std::vector<Count> counts;
    for (const Getter &getter : all) {
        for (const uint32_t callee : getter.callees) {
            auto it = std::find_if(counts.begin(), counts.end(),
                [&](const Count &c) { return c.target == callee; });
            if (it == counts.end()) counts.push_back(Count{callee, 1});
            else ++it->count;
        }
    }
    for (Getter &getter : all) {
        const Count *best = nullptr;
        for (const uint32_t callee : getter.callees) {
            const auto it = std::find_if(counts.begin(), counts.end(),
                [&](const Count &c) { return c.target == callee; });
            if (it == counts.end() || it->count < 2) continue;
            /* Only a static accessor can head a geometry family; a helper that
               happens to be called everywhere must not collect one. */
            if (!detail::is_static_accessor(code, (callee - base) / 4)) continue;
            if (best == nullptr || it->count > best->count
                || (it->count == best->count && it->target < best->target)) {
                best = &*it;
            }
        }
        getter.anchor = best != nullptr ? best->target : 0;
    }

    std::vector<uint32_t> anchors;
    for (const Getter &getter : all) {
        if (getter.anchor == 0) continue;
        if (std::find(anchors.begin(), anchors.end(), getter.anchor) == anchors.end()) {
            anchors.push_back(getter.anchor);
        }
    }

    std::vector<Family> result;
    for (const uint32_t anchor : anchors) {
        Family family;
        family.anchor = anchor;
        for (const Getter &getter : all) {
            if (getter.anchor == anchor) family.members.push_back(getter);
        }
        if (family.members.size() < 2) {
            family.rejected = "fewer than two members";
            result.push_back(std::move(family));
            continue;
        }

        /* Every call site of this family's members, with its enclosing function. */
        struct CallSite {
            uint32_t owner = 0;
            uint32_t member = 0;
        };
        std::vector<CallSite> sites;
        for (size_t at = 0; at + 1 < code.size(); ++at) {
            const auto target = detail::bl_target(base + static_cast<uint32_t>(at * 4), code[at]);
            if (!target) continue;
            const bool is_member = std::any_of(family.members.begin(), family.members.end(),
                [&](const Getter &g) { return g.va == *target; });
            if (!is_member) continue;
            const uint32_t owner = detail::enclosing_function(code, at, base);
            if (owner != 0) sites.push_back(CallSite{owner, *target});
        }

        /* Distinct members called per owning function, and that function's shape. */
        std::vector<uint32_t> owners;
        std::vector<uint32_t> aggregations;
        for (const CallSite &site : sites) {
            if (std::find(owners.begin(), owners.end(), site.owner) != owners.end()) continue;
            owners.push_back(site.owner);

            std::vector<uint32_t> called;
            for (const CallSite &other : sites) {
                if (other.owner != site.owner) continue;
                if (std::find(called.begin(), called.end(), other.member) == called.end()) {
                    called.push_back(other.member);
                }
            }
            if (called.size() != 2) continue;

            const size_t first = static_cast<size_t>(site.owner - base) / 4;
            const size_t last = std::min(code.size(), first + kMaxAggregationInsns);
            bool adds = false;
            bool mixes = false;
            for (size_t at = first; at < last; ++at) {
                if (detail::is_fadd(code[at])) adds = true;
                if (detail::is_fdiv(code[at])) mixes = true;
            }
            if (adds && !mixes) aggregations.push_back(site.owner);
        }

        if (aggregations.size() != 1) {
            family.rejected = aggregations.empty() ? "no summation of exactly two members"
                                                   : "several summations of two members";
            result.push_back(std::move(family));
            continue;
        }
        family.aggregation = aggregations.front();

        /* Role inside the pair: the zero-clamping member is the margin. */
        std::vector<Getter> pair;
        for (const CallSite &site : sites) {
            if (site.owner != family.aggregation) continue;
            for (const Getter &getter : family.members) {
                if (getter.va != site.member) continue;
                if (std::none_of(pair.begin(), pair.end(),
                        [&](const Getter &g) { return g.va == getter.va; })) {
                    pair.push_back(getter);
                }
            }
        }
        if (pair.size() != 2) {
            family.rejected = "aggregation does not own exactly two members";
            result.push_back(std::move(family));
            continue;
        }
        if (pair[0].zeroes == pair[1].zeroes) {
            family.rejected = "zero idiom does not separate the pair";
            result.push_back(std::move(family));
            continue;
        }
        family.margin = pair[0].zeroes ? pair[0].va : pair[1].va;
        family.height = pair[0].zeroes ? pair[1].va : pair[0].va;
        result.push_back(std::move(family));
    }
    return result;
}


namespace detail {

/* add xN, x27, #imm, lsl #12 followed by ldr xN, [xN, #imm] */
inline bool is_pool_load(std::span<const uint32_t> code, size_t at, uint32_t *offset) {
    if (at + 1 >= code.size()) return false;
    const uint32_t add = code[at];
    const uint32_t load = code[at + 1];
    if (((add >> 31) & 1) != 1 || ((add >> 23) & 0x3Fu) != 0b100010u
        || ((add >> 22) & 1) != 1 || ((add >> 5) & 0x1Fu) != 27u) {
        return false;
    }
    if ((load >> 31) != 1 || ((load >> 24) & 0x3Fu) != 0x39u
        || ((load >> 5) & 0x1Fu) != (add & 0x1Fu)) {
        return false;
    }
    *offset = ((add >> 10) & 0xFFFu) << 12 | (((load >> 10) & 0xFFFu) * 8u);
    return true;
}

/* cmp xN, #0x73c: the configured-class check that guards a devirtualised access. */
inline bool is_class_check(uint32_t word) {
    return (word & 0xFFFFFC1Fu) == (0xF1000000u | (0x73Cu << 10) | 31u);
}

/* The class every logged geometry value belongs to, as checked before the access. */
constexpr uint32_t kGeometryClassId = 0x73Cu;

} // namespace detail

/*
 * Collect the aggregator's logged items: (label slot, value source).
 *
 * Only items that lead to a geometry access are kept, so an item whose value is
 * produced some other way is skipped rather than guessed at.
 */
inline std::vector<LogItem> log_items(std::span<const uint32_t> code, uint32_t base,
    const std::vector<Getter> &getters) {
    std::vector<LogItem> items;
    for (size_t at = 0; at + 2 < code.size(); ++at) {
        uint32_t pool_offset = 0;
        if (!detail::is_pool_load(code, at, &pool_offset)) continue;
        LogItem item;
        item.site = base + static_cast<uint32_t>(at * 4);
        item.pool_offset = pool_offset;
        const size_t limit = std::min(code.size(), at + 32);
        size_t checked = 0;
        for (size_t probe = at + 2; probe < limit; ++probe) {
            if (!detail::is_class_check(code[probe])) continue;
            checked = probe;
            break;
        }
        if (checked == 0) continue;
        /* Value source: a getter call first, then an inline scalar read. */
        for (size_t probe = checked; probe < std::min(code.size(), checked + 14); ++probe) {
            const auto target = detail::bl_target(base + static_cast<uint32_t>(probe * 4),
                code[probe]);
            if (!target) continue;
            const bool is_getter = std::any_of(getters.begin(), getters.end(),
                [&](const Getter &g) { return g.va == *target; });
            if (is_getter) {
                item.target = *target;
                item.called = true;
                break;
            }
        }
        if (!item.called) {
            for (size_t probe = checked; probe < std::min(code.size(), checked + 14); ++probe) {
                const uint32_t word = code[probe];
                if (detail::is_ldur_double(word) && !detail::is_frame_relative(word)) {
                    item.field = (word >> 12) & 0x1FFu;
                    break;
                }
                if (detail::is_ldur_word(word)) {
                    item.field = (word >> 12) & 0x1FFu;
                    break;
                }
            }
            if (item.field == 0) continue;
        }
        items.push_back(item);
    }
    return items;
}

/*
 * The family getter whose scalar read is `field`. Used when the aggregator read the
 * value inline, which happens whenever the getter was small enough to inline at the
 * call site; the standalone getter still exists and is what gets hooked. The lookup
 * is global on purpose: the field immediate is the only thing the inline site and
 * the getter have in common, so a build whose offsets are not unique across
 * configuration classes must refuse rather than pick the likeliest.
 */
inline uint32_t field_getter(const std::vector<Getter> &getters, uint32_t field) {
    uint32_t found = 0;
    for (const Getter &getter : getters) {
        if (getter.field != field) continue;
        if (found != 0 && found != getter.va) return 0; // ambiguous
        found = getter.va;
    }
    return found;
}

/* How many getters read `field`, so a refusal can say whether it was ambiguity. */
inline size_t field_candidates(const std::vector<Getter> &getters, uint32_t field) {
    size_t count = 0;
    for (const Getter &getter : getters) {
        if (getter.field == field) ++count;
    }
    return count;
}

/* What one label matched, in enough detail to explain a refusal on the device. */
struct LabelMatch {
    bool found = false;   // a slot in the pool holds this label
    bool called = false;  // the item calls a getter instead of reading inline
    uint32_t target = 0;  // the getter to hook; 0 when the source is not unique
    uint32_t field = 0;   // inline read's immediate
    size_t candidates = 0; // getters reading that field
    size_t header = 0;    // where in the object the text started (diagnostics)
};

/*
 * Report whether the pool slot at `pool_offset` holds exactly `label` as its string content.
 *
 * `header` receives the offset at which the text was found. That offset is a property of the
 * build's string layout, so it is discovered rather than assumed: the label is matched at each
 * plausible offset and the text itself is the discriminator. A slot holding some other string
 * cannot match a twenty-character label at a shifted offset by accident.
 */
using PoolLabelProbe = bool (*)(void *context, uint32_t pool_offset, std::string_view label,
    size_t *header);

/*
 * Match a knob label's content against the item whose pool slot holds it.
 *
 * The label is compared from its first character, which is what makes this content addressing
 * rather than a stored offset: a different build either matches the same text or not at all.
 */
inline LabelMatch match_label(const std::vector<LogItem> &items, const char *label,
    PoolLabelProbe probe, void *context, const std::vector<Getter> &getters) {
    LabelMatch match;
    if (label == nullptr || probe == nullptr) return match;
    const std::string_view text(label);
    for (const LogItem &item : items) {
        size_t header = 0;
        if (!probe(context, item.pool_offset, text, &header)) continue;
        if (!match.found) {
            match.found = true;
            match.called = item.called;
            match.field = item.field;
            match.header = header;
            match.candidates = item.called ? 1 : field_candidates(getters, item.field);
            match.target = item.called ? item.target : field_getter(getters, item.field);
            continue;
        }
        /* A second slot with the same text is only acceptable if it names the same source. */
        if (match.target != 0 && item.called && item.target != 0 && item.target != match.target) {
            match.target = 0;
            match.candidates = 0;
            return match;
        }
    }
    return match;
}

/*
 * Full resolution over one libapp.so image.
 *
 * `segments` must come from nhk::elf::parse_program_segments over the same bytes;
 * the largest executable segment is the AOT instruction stream. Nothing outside
 * that image is used, so an OTA that reorders or re-lays-out the code either
 * resolves to a different (still verified) set or refuses outright.
 */
inline std::optional<Resolution> resolve(std::span<const std::byte> bytes,
    const std::vector<nhk::elf::ProgramSegment> &segments) {
    const nhk::elf::ProgramSegment *text = nullptr;
    for (const auto &segment : segments) {
        if (segment.type != nhk::elf::kProgramTypeLoad) continue;
        if ((segment.flags & nhk::elf::kFlagExecute) == 0) continue;
        if (!nhk::in_image(bytes.size(), segment.offset, segment.filesz)) continue;
        if (text == nullptr || segment.filesz > text->filesz) text = &segment;
    }
    if (text == nullptr || text->filesz < 16) return {};

    const auto *raw = reinterpret_cast<const uint32_t *>(bytes.data() + text->offset);
    const std::span<const uint32_t> code(raw, static_cast<size_t>(text->filesz) / 4);
    const auto base = static_cast<uint32_t>(text->vaddr);

    Resolution resolution;
    resolution.getters = getters(code, base);
    if (resolution.getters.empty()) return {};
    const std::vector<uint32_t> owners = owner_table(code, base);
    resolution.views = views(code, base, resolution.getters);
    resolution.items = log_items(code, base, resolution.getters);
    resolution.families = families(resolution.getters, code, base);
    for (const Family &family : resolution.families) {
        if (family.resolved()) ++resolution.resolved_families;
    }

    /*
     * The margin: among the views, the one whose source sits in the largest
     * geometry family wins. The controller that exposes the full scalar surface
     * (the layout hub reads most of it) is far wider than the families around
     * unrelated forwarded values - a bottom-sheet height, a pixel-ratio helper -
     * so the widest family among the candidates identifies the hotseat value
     * without naming it. A tie refuses the resolution.
     */
    struct Candidate {
        uint32_t source = 0;
        uint32_t view = 0;
        size_t family_size = 0;
    };
    std::vector<Candidate> candidates;
    for (const View &view : resolution.views) {
        for (const Family &family : resolution.families) {
            const bool member = std::any_of(family.members.begin(), family.members.end(),
                [&](const Getter &g) { return g.va == view.source; });
            if (member) candidates.push_back(Candidate{view.source, view.va, family.members.size()});
        }
    }
    if (candidates.empty()) {
        resolution.rejected = "no view forwards into a geometry family";
        return resolution;
    }
    size_t widest = 0;
    for (const Candidate &candidate : candidates) widest = std::max(widest, candidate.family_size);
    std::vector<Candidate> best;
    for (const Candidate &candidate : candidates) {
        if (candidate.family_size != widest) continue;
        if (std::none_of(best.begin(), best.end(),
                [&](const Candidate &c) { return c.source == candidate.source; })) {
            best.push_back(candidate);
        }
    }
    if (best.size() != 1) {
        resolution.rejected = "several geometry members of the widest family are forwarded to";
        return resolution;
    }
    resolution.margin = best.front().source;
    resolution.margin_view = best.front().view;

    /*
     * The height: the function that SUMS the margin view with exactly one other
     * scalar getter names that getter as the height view. Callers that divide or
     * subtract (the title-relative calculation) are not extent totals and are
     * skipped, which is what leaves a single partner. The layout-side height is
     * then the getter of the margin's own family reading the same field.
     */
    std::vector<uint32_t> height_views;
    std::vector<uint32_t> seen_owners;
    for (size_t at = 0; at + 1 < code.size(); ++at) {
        const auto target = detail::bl_target(base + static_cast<uint32_t>(at * 4), code[at]);
        if (!target || *target != resolution.margin_view) continue;
        const uint32_t owner = owners[at];
        if (owner == 0 || std::find(seen_owners.begin(), seen_owners.end(), owner)
                != seen_owners.end()) {
            continue;
        }
        seen_owners.push_back(owner);

        const size_t first = static_cast<size_t>(owner - base) / 4;
        const size_t last = detail::function_end(code, first, kMaxAggregationInsns);
        if (last == 0) continue;
        bool adds = false;
        bool mixes = false;
        for (size_t body = first; body < last; ++body) {
            if (detail::is_fadd(code[body])) adds = true;
            if (detail::is_fdiv(code[body])) mixes = true;
        }
        if (!adds || mixes) continue;

        std::vector<uint32_t> partners;
        for (size_t other = 0; other + 1 < code.size(); ++other) {
            const auto called = detail::bl_target(base + static_cast<uint32_t>(other * 4),
                code[other]);
            if (!called || owners[other] != owner || *called == resolution.margin_view) continue;
            const bool is_getter = std::any_of(resolution.getters.begin(), resolution.getters.end(),
                [&](const Getter &g) { return g.va == *called; });
            if (is_getter && std::find(partners.begin(), partners.end(), *called) == partners.end()) {
                partners.push_back(*called);
            }
        }
        if (partners.size() == 1
            && std::find(height_views.begin(), height_views.end(), partners.front())
                == height_views.end()) {
            height_views.push_back(partners.front());
        }
    }
    if (height_views.size() != 1) {
        resolution.rejected = height_views.empty() ? "no summation partners the margin view"
                                                   : "several summation partners";
        return resolution;
    }
    resolution.height_view = height_views.front();
    /*
     * The height knob hooks the extent partner itself: it is the getter the
     * window-extent total adds, so its callers all take the overridden value.
     * The layout-side getter of the margin's family that reads the same field is
     * a second view of the value; it is reported when that lookup is unique and
     * left at zero when several members share the field immediate, because a
     * coincidence of offsets is not evidence.
     */
    resolution.height = resolution.height_view;
    const auto margin_it = std::find_if(resolution.getters.begin(), resolution.getters.end(),
        [&](const Getter &g) { return g.va == resolution.margin; });
    const auto height_it = std::find_if(resolution.getters.begin(), resolution.getters.end(),
        [&](const Getter &g) { return g.va == resolution.height_view; });
    if (margin_it != resolution.getters.end() && height_it != resolution.getters.end()) {
        uint32_t candidate = 0;
        size_t matches = 0;
        for (const Getter &getter : resolution.getters) {
            if (getter.anchor != margin_it->anchor || getter.va == resolution.margin) continue;
            if (getter.field != height_it->field) continue;
            if (matches == 0) candidate = getter.va;
            ++matches;
        }
        resolution.height_alt = matches == 1 ? candidate : 0;
    }

    /*
     * The aggregator is the function that OWNS the logged items: the one that loads each label out
     * of the object pool and then reads the matching value. Counting item sites per owning function
     * reuses the very relationship the items were found with, so a build that moves the layout
     * calculation elsewhere still names that function - no address, no ordering and no name is
     * assumed. Too few items to be a layout calculation, or a tie between two owners, refuses: the
     * pool capture only earns its keep if it lands on the real aggregator.
     */
    {
        std::vector<uint32_t> item_owners;
        std::vector<size_t> counts;
        for (const LogItem &item : resolution.items) {
            const size_t at = static_cast<size_t>(item.site - base) / 4;
            if (at >= owners.size()) continue;
            const uint32_t owner = owners[at];
            if (owner == 0) continue;
            const auto slot = std::find(item_owners.begin(), item_owners.end(), owner);
            if (slot == item_owners.end()) {
                item_owners.push_back(owner);
                counts.push_back(1);
            } else {
                ++counts[static_cast<size_t>(slot - item_owners.begin())];
            }
        }
        size_t best = 0;
        size_t ties = 0;
        for (size_t index = 0; index < counts.size(); ++index) {
            if (counts[index] > best) {
                best = counts[index];
                ties = 1;
                resolution.aggregator = item_owners[index];
            } else if (counts[index] == best) {
                ++ties;
            }
        }
        if (best < kMinAggregatorItems || ties != 1) resolution.aggregator = 0;
    }
    return resolution;
}

} // namespace home_layout::dart_targets
