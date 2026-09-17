/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * NativeHookRuntime hook bank: generic slot lifecycle for inline hooks.
 *
 * Abstracted from the desktop Dart hook's slot machinery (this project's
 * dock_native_hooks.cpp: install_slot / restore_patch_words /
 * ensure_slots_live / bank_healthy), so every future target - Rust app, C/C++
 * library, another Dart AOT image - drives the same state machine instead of
 * growing a second one.
 *
 * The invariants carried over unchanged (they are what kept the desktop
 * channel crash-free):
 *  - A replacement trampoline ends in `br x16`, where x16 is the continuation
 *    the hook library hands back through an out-parameter. A null
 *    continuation is an unconditional jump to address 0, so a slot is never
 *    left armed without one; every refusal/disarm bumps the guard counter.
 *  - A slot whose live words equal the known patch is healthy; live words
 *    equal to the original mean the file-backed page was refilled - safe to
 *    re-arm while generation and continuation still hold; anything else is a
 *    foreign edit and must be left alone, never overwritten.
 *  - Every read that can observe a concurrently-refilled page goes through
 *    the host's stable read (double inventory validation); this layer never
 *    touches memory directly. The one exception is the steady-state health
 *    check, where the host may pass its cheap live-word read instead: a torn
 *    read there can only cost a repair attempt, and the repair itself is the
 *    validated path (`ensure_slots_live`).
 *
 * The host (feature) supplies memory access, the hook entry points and the
 * logging sink; this layer owns only the state machine.
 */
#pragma once

#include "native_image.h"
#include "nhk_base.h"
#include "resolver.h"

#include <array>
#include <cstdint>
#include <span>
#include <vector>

namespace nhk {

/** Cap on simultaneously armed banks, mirroring the desktop's layout banks. */
inline constexpr size_t kMaxBanks = 16;

/**
 * One inline hook point. `original` is the caller-owned out-parameter the
 * hook backend fills with the continuation pointer; `source` pins the file
 * identity the slot was installed against, so a validated read can detect a
 * generation remap before any byte is trusted.
 */
template<size_t kPatchWords>
struct InlineSlot {
    uintptr_t address = 0;
    void *replacement = nullptr;
    void **original = nullptr;
    CodeSource source;
    std::array<uint32_t, kPatchWords> original_words{};
    std::array<uint32_t, kPatchWords> patch_words{};
    bool registered = false;
    bool patch_known = false;
};

template<size_t kPatchWords>
using SlotWords = std::array<uint32_t, kPatchWords>;

/**
 * Feature-supplied services. All memory access must be validated by the
 * feature (stable reads against the mapping inventory); this layer never
 * dereferences anything itself.
 */
struct HookEvent {
    const char *reason;
    size_t slot; // Slot index within the bank, for the feature's log format.
};

template<size_t kPatchWords>
struct InlineHookHost {
    /** Validated read of one slot's live words (double-inventory checked). */
    bool (*read_slot)(const InlineSlot<kPatchWords> &, SlotWords<kPatchWords> &) = nullptr;
    /** Write code words back (mprotect + memcpy + clear cache handled inside). */
    bool (*write_words)(uintptr_t, const SlotWords<kPatchWords> &) = nullptr;
    /** LSPosed-style backend: returns 0 on success, fills `original`. */
    int (*hook_install)(void *target, void *replacement, void **original) = nullptr;
    /** Optional backend removal; null means the feature never unhooks. */
    int (*hook_uninstall)(void *target) = nullptr;
    /**
     * Optional page-lifetime hook: called with the byte range that is about to
     * be patched, before the backend writes anything. A feature that hooks a
     * runtime which discards its own code pages registers the range here so a
     * concurrent discard cannot take the trampoline with it. Returning false
     * refuses the installation: an unprotected slot is worse than no slot.
     */
    bool (*protect_range)(uintptr_t address, size_t bytes) = nullptr;
    /** A guard-worthy event: log it and count it so it survives log rotation. */
    void (*on_guard)(const HookEvent &) = nullptr;
    /** Low-rate diagnostics sink (already deduplicated by the caller). */
    void (*on_info)(const HookEvent &) = nullptr;
};

enum class SlotOperation { install, restore, disarm, rearm };

/** Continuation of a slot is present and non-null. */
template<size_t kPatchWords>
inline bool continuation_exists(const InlineSlot<kPatchWords> &slot) {
    return slot.original != nullptr && *slot.original != nullptr;
}

/**
 * Install a slot through the backend, refusing to leave a null continuation.
 *
 * On a backend "success" without a continuation, the untouched prologue is
 * written back (the Dart/native function keeps running) and the slot is
 * refused: a silent follow loss is strictly better than a crash.
 */
template<size_t kPatchWords>
inline bool install_slot(InlineSlot<kPatchWords> &slot,
    const InlineHookHost<kPatchWords> &host) {
    if (slot.registered) return true;
    if (host.hook_install == nullptr || slot.original == nullptr) {
        if (host.on_guard != nullptr) {
            host.on_guard({"motion hook install precondition failed", 0});
        }
        return false;
    }
    if (host.read_slot == nullptr || host.write_words == nullptr) {
        if (host.on_guard != nullptr) {
            host.on_guard({"motion hook install io missing", 0});
        }
        return false;
    }
    SlotWords<kPatchWords> before{};
    if (!host.read_slot(slot, before)) return false;
    if (before != slot.original_words) {
        if (slot.patch_known) return false;
        /*
         * First install, and the live words differ from the file. The Dart runtime relocates pool
         * references while mapping a snapshot, so the loaded code is the truth about what executes -
         * adopt it as the prologue the continuation replays. A foreign hook is refused instead: a
         * live branch-and-link pair at the entry is the shape our own backend and every other inline
         * hooker leaves behind, and replaying it inside the trampoline would branch to its target
         * twice. The prologue check the caller already ran guarantees the entry was a plain frame
         * setup in the file, so anything branchy here is someone else's patch, not loader state.
         */
        if ((before[0] & 0xFFE00000u) == 0x58000000u || before[0] == 0xD61F0200u) return false;
        slot.original_words = before;
    }
    // Register the patched range with the page-lifetime policy before the
    // backend touches it: a trampoline page that a concurrent MADV_DONTNEED can
    // discard is not a working hook. Failure here refuses the slot.
    if (host.protect_range != nullptr
        && !host.protect_range(slot.address, kPatchWords * sizeof(uint32_t))) {
        if (host.on_guard != nullptr) {
            host.on_guard({"motion hook refused; patch range cannot be protected", 0});
        }
        return false;
    }

    void *previous = *slot.original;
    if (host.hook_install(reinterpret_cast<void *>(slot.address), slot.replacement,
            slot.original) != 0) {
        if (*slot.original == nullptr) *slot.original = previous;
        return false;
    }
    if (*slot.original == nullptr) {
        if (host.on_guard != nullptr) {
            host.on_guard({"motion hook refused; continuation missing; prologue restored", 0});
        }
        host.write_words(slot.address, before);
        *slot.original = previous;
        return false;
    }
    slot.registered = true;
    SlotWords<kPatchWords> after{};
    if (host.read_slot(slot, after) && after != slot.original_words) {
        slot.patch_words = after;
        slot.patch_known = true;
    }
    return true;
}

/**
 * Re-write the exact patch words the backend installed, without involving it.
 *
 * Valid only while the backend still owns the address (the bank is never
 * unhooked for process life) and the continuation exists. This is the
 * fallback that matters most for file-backed code: the page came back from
 * the kernel and the backend refuses to re-arm an address it still owns.
 */
template<size_t kPatchWords>
inline bool restore_patch_words(InlineSlot<kPatchWords> &slot,
    const InlineHookHost<kPatchWords> &host) {
    if (!slot.patch_known) return false;
    if (!continuation_exists(slot)) {
        if (host.on_guard != nullptr) {
            host.on_guard({"motion hook re-arm skipped; continuation missing", 0});
        }
        return false;
    }
    // Re-registration is idempotent and covers a slot installed before the
    // policy existed.
    if (host.protect_range != nullptr
        && !host.protect_range(slot.address, kPatchWords * sizeof(uint32_t))) {
        return false;
    }
    if (!host.write_words(slot.address, slot.patch_words)) return false;
    slot.registered = true;
    return true;
}

/**
 * Bring every slot of the bank into the armed state.
 *
 * `order` is the feature's installation order (the desktop installs scene
 * hooks before the scale hook so subscribers wake in a valid state). A slot
 * whose words are neither ours nor the original is a foreign edit: left
 * alone, reported through `on_guard`, and reported as unhealthy by the caller
 * - never overwritten.
 */
template<size_t kTargets, size_t kPatchWords>
inline bool ensure_slots_live(std::array<InlineSlot<kPatchWords>, kTargets> &slots,
    const InlineHookHost<kPatchWords> &host,
    std::span<const size_t> order) {
    for (const size_t index : order) {
        if (index >= kTargets) return false;
        auto &slot = slots[index];
        if (slot.registered) {
            // A live patch whose continuation vanished would branch to address 0
            // on the next call. Disarm before anything else.
            if (!continuation_exists(slot)) {
                host.write_words(slot.address, slot.original_words);
                if (host.on_guard != nullptr) {
                    host.on_guard({"motion hook disarmed; continuation missing", index});
                }
                slot.registered = false;
                slot.patch_known = false;
                return false;
            }
            SlotWords<kPatchWords> observed{};
            if (host.read_slot == nullptr || !host.read_slot(slot, observed)) return false;
            if (slot.patch_known && observed == slot.patch_words) continue;
            if (observed == slot.original_words) {
                if (host.on_info != nullptr) {
                    host.on_info({"motion hook patch lost; re-arming", index});
                }
                slot.registered = false;
            } else if (!slot.patch_known) {
                // A hook that landed after the bank was reported partial: adopt
                // the live words instead of rewriting them for process life.
                slot.patch_words = observed;
                slot.patch_known = true;
                continue;
            } else {
                // Foreign edit between our patch and the original: do not touch.
                if (host.on_guard != nullptr) {
                    host.on_guard({"motion hook foreign words; leaving untouched", index});
                }
                return false;
            }
        }
        if (!install_slot(slot, host) && !restore_patch_words(slot, host)) {
            if (host.on_info != nullptr) {
                host.on_info({"motion hook re-arm failed; channel stays down", index});
            }
            return false;
        }
    }
    return true;
}

/**
 * Disarm a slot: ask the backend to drop its trampoline, then restore the
 * original prologue so the target runs unmodified again.
 *
 * Ownership is verified first: the live words must be ours (the known patch) or
 * already the untouched prologue. A third party's words mean the slot is no
 * longer ours to restore, and writing over them would destroy someone else's
 * hook - such a slot is refused instead.
 *
 * The write-back is not optional: a backend that only clears its own record
 * would leave the patched instructions in place, and the untouched prologue is
 * the only state every party agrees on. A slot that is already disarmed is a
 * successful no-op, which makes the call idempotent for cleanup paths.
 */
template<size_t kPatchWords>
inline bool uninstall_slot(InlineSlot<kPatchWords> &slot,
    const InlineHookHost<kPatchWords> &host) {
    if (!slot.registered) return true;
    if (host.write_words == nullptr || host.read_slot == nullptr) return false;
    SlotWords<kPatchWords> observed{};
    if (!host.read_slot(slot, observed)) return false;
    const bool holds_our_patch = slot.patch_known && observed == slot.patch_words;
    const bool already_original = observed == slot.original_words;
    if (!holds_our_patch && !already_original) {
        if (host.on_guard != nullptr) {
            host.on_guard({"motion hook uninstall refused; slot is not ours", 0});
        }
        return false;
    }
    if (host.hook_uninstall != nullptr) {
        if (host.hook_uninstall(reinterpret_cast<void *>(slot.address)) != 0) return false;
    } else if (continuation_exists(slot)) {
        // No backend removal available: the slot cannot be taken back safely
        // because its trampoline may still be reachable.
        if (host.on_guard != nullptr) {
            host.on_guard({"motion hook cannot be uninstalled; backend has no unhook", 0});
        }
        return false;
    }
    if (!host.write_words(slot.address, slot.original_words)) return false;
    slot.registered = false;
    slot.patch_known = false;
    if (slot.original != nullptr) *slot.original = nullptr;
    return true;
}

/**
 * Health verification: every slot registered, continuation present, live
 * words equal to the known patch (adopting an unknown-but-live patch once).
 * `read_all` is the feature's batched validated read; it exists so a healthy
 * steady state costs one inventory round trip, not one per slot.
 */
template<size_t kTargets, size_t kPatchWords, typename ReadAll>
inline bool slots_healthy(std::array<InlineSlot<kPatchWords>, kTargets> &slots,
    ReadAll read_all) {
    std::array<uintptr_t, kTargets> addresses{};
    std::array<CodeSource, kTargets> sources{};
    std::array<SlotWords<kPatchWords>, kTargets> observed{};
    for (size_t i = 0; i < kTargets; ++i) {
        addresses[i] = slots[i].address;
        sources[i] = slots[i].source;
        if (!slots[i].registered) return false;
        if (!continuation_exists(slots[i])) return false;
    }
    if (!read_all(addresses, sources, observed)) return false;
    for (size_t i = 0; i < kTargets; ++i) {
        auto &slot = slots[i];
        if (!slot.patch_known) {
            if (observed[i] == slot.original_words) return false;
            slot.patch_words = observed[i];
            slot.patch_known = true;
        } else if (observed[i] != slot.patch_words) {
            return false;
        }
    }
    return true;
}

/**
 * Health verification for the subset of a bank that is actually armed.
 *
 * `slots_healthy` above covers a bank whose every slot must be armed. A feature
 * that arms an optional subset needs the same verdict restricted to `order`,
 * because an unarmed slot is not a broken one: the launcher layout arms its
 * cell-count hooks, its geometry knobs and its two object captures
 * independently, and a knob with no resolved target is deliberately inert.
 *
 * `read_all` here is the host's *cheap* batched live-word read - no mapping
 * inventory. That split is the point: proving the image generation still holds
 * is what costs a full /proc/self/maps parse, and that proof belongs to the
 * repair path (`ensure_slots_live`, which reads through the host's validated
 * `read_slot`), not to a check that runs several times a second. A lost patch
 * fails here - the live words no longer match - and drops straight into that
 * repair. See the desktop layout worker for the incident this encodes.
 */
template<size_t kTargets, size_t kPatchWords, typename ReadAll>
inline bool ordered_slots_healthy(std::array<InlineSlot<kPatchWords>, kTargets> &slots,
    std::span<const size_t> order, ReadAll read_all) {
    std::array<uintptr_t, kTargets> addresses{};
    std::array<CodeSource, kTargets> sources{};
    std::array<SlotWords<kPatchWords>, kTargets> observed{};
    size_t armed = 0;
    for (const size_t index : order) {
        if (index >= kTargets) return false;
        auto &slot = slots[index];
        if (!slot.registered || !continuation_exists(slot)) return false;
        addresses[armed] = slot.address;
        sources[armed] = slot.source;
        ++armed;
    }
    if (armed == 0) return true;
    if (!read_all(std::span<const uintptr_t>(addresses.data(), armed),
            std::span<const CodeSource>(sources.data(), armed),
            std::span<SlotWords<kPatchWords>>(observed.data(), armed))) {
        return false;
    }
    size_t cursor = 0;
    for (const size_t index : order) {
        auto &slot = slots[index];
        const SlotWords<kPatchWords> &live = observed[cursor++];
        if (!slot.patch_known) {
            // A hook that landed after the bank was reported partial: adopt the
            // live words instead of rewriting them for process life.
            if (live == slot.original_words) return false;
            slot.patch_words = live;
            slot.patch_known = true;
        } else if (live != slot.patch_words) {
            return false;
        }
    }
    return true;
}

} // namespace nhk
