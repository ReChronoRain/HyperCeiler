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
 * Readiness, backend ownership and pending cleanup are separate states:
 * synchronous readback is required before publishing a new patch. Unknown
 * bytes remain quarantined instead of being adopted by a later health check.
 * Key invariants:
 *  - A replacement trampoline ends in `br x16`, where x16 is the continuation
 *    the hook library hands back through an out-parameter. A null
 *    continuation can jump to address 0. Missing continuations refuse readiness
 *    and trigger checked cleanup. A failing host write can leave physical bytes
 *    live; the pending record is not a guarantee that execution has stopped.
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
#include "code_write_lock.h"
#include "code_patch_write.h"
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
    // Backend ownership is not the same as verified/executable readiness.
    bool backend_owned = false;
    bool install_pending = false;
    bool write_pending = false;
    bool cleanup_pending = false;
    bool continuation_pending = false;
    CodePatchJournal<kPatchWords> write_journal{};
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
    /** Stateful production path; expected words and a persistent per-slot RX/byte journal. */
    bool (*write_slot)(InlineSlot<kPatchWords>&,const SlotWords<kPatchWords>&,
        const SlotWords<kPatchWords>&) = nullptr;
    /** Settle writer-owned obligations BEFORE RX-only stable reads. Paired with write_slot. */
    bool (*settle_write)(InlineSlot<kPatchWords>&) = nullptr;
};

enum class SlotOperation { install, restore, disarm, rearm };

/** Continuation of a slot is present and non-null. */
template<size_t kPatchWords>
inline bool continuation_exists(const InlineSlot<kPatchWords> &slot) {
    return slot.original != nullptr && *slot.original != nullptr;
}

/** A backend callback or failed writer still has state that must not be discarded. */
template<size_t N>
inline bool slot_has_pending(const InlineSlot<N> &slot) {
    return slot.install_pending || slot.write_pending || slot.cleanup_pending
        || slot.continuation_pending || slot.write_journal.pending();
}

template<size_t N>
inline bool slot_ready(const InlineSlot<N> &slot) {
    return slot.registered && slot.patch_known && continuation_exists(slot)
        && !slot_has_pending(slot);
}

// Validate the WHOLE order before invoking any reader/writer/backend. Bounding only
// the index does not bound the number of entries: repeated indices used to overflow
// the health pass's fixed arrays. Overlapping patch ranges are not independent slots.
template<size_t T, size_t N>
inline bool valid_slot_order(const std::array<InlineSlot<N>, T> &slots,
    std::span<const size_t> order) {
    static_assert(N > 0 && N <= SIZE_MAX / sizeof(uint32_t));
    if (order.size() > T) return false;
    constexpr size_t bytes = N * sizeof(uint32_t);
    std::array<bool, T> seen{};
    for (size_t pos = 0; pos < order.size(); ++pos) {
        const auto index = order[pos];
        if (index >= T || seen[index]) return false;
        seen[index] = true;
        const uintptr_t address = slots[index].address;
        if ((address & 3u) || address > UINTPTR_MAX - bytes) return false;
        for (size_t earlier = 0; earlier < pos; ++earlier) {
            const auto other = slots[order[earlier]].address;
            if (address < other + bytes && other < address + bytes) return false;
        }
    }
    return true;
}

/** Legacy hosts still supply their own transaction guarantees; production Dock/layout
 * use the paired stateful callbacks, never the raw bool-only writer. */
template<size_t N> bool slot_writer_available(const InlineHookHost<N>& host) {
    if (host.write_slot || host.settle_write) return host.write_slot && host.settle_write;
    return host.write_words!=nullptr;
}
template<size_t N> bool settle_slot_write(InlineSlot<N>& slot,const InlineHookHost<N>& host) {
    if (!slot_writer_available(host)) return false;
    if (!slot.write_journal.pending()) return true;
    return host.settle_write && host.settle_write(slot) && !slot.write_journal.pending();
}
template<size_t N> bool write_slot_words(InlineSlot<N>& slot,const SlotWords<N>& expected,
    const SlotWords<N>& wanted,const InlineHookHost<N>& host) {
    if (!settle_slot_write(slot,host)) return false;
    if (host.write_slot) return host.write_slot(slot,expected,wanted) && !slot.write_journal.pending();
    return host.write_words && host.write_words(slot.address,wanted);
}

/** Restore/verify the prologue BEFORE releasing its continuation. Keep every failed
 * step pending. This does not provide an in-flight trampoline drain or a mapping lease.
 * The host/backend must still supply those lifetimes and transactional code writes. */
template<size_t N>
inline bool recover_slot_cleanup(InlineSlot<N> &slot, const InlineHookHost<N> &host) {
    if (!slot.cleanup_pending || !host.read_slot || !slot_writer_available(host)) return false;
    CodeWriteGuard code_write_lock;
    if (!code_write_lock) return false;
    if (!settle_slot_write(slot,host)) return false;
    SlotWords<N> observed{};
    if (!host.read_slot(slot, observed)) return false;
    if (observed != slot.original_words) {
        if (!slot.patch_known || observed != slot.patch_words) return false;
        if (!write_slot_words(slot,observed,slot.original_words,host)) {
            slot.write_pending = true;
            return false;
        }
    } else if (slot.write_pending) {
        // A false writer result may mean bytes changed but permissions did not.
        // A word-only read cannot discharge that obligation.
        if (!write_slot_words(slot,observed,slot.original_words,host)) return false;
    }
    if (!host.read_slot(slot, observed) || observed != slot.original_words) return false;
    slot.write_pending = false;
    if (slot.backend_owned) {
        if (!host.hook_uninstall
            || host.hook_uninstall(reinterpret_cast<void *>(slot.address)) != 0) return false;
        slot.backend_owned = false;
        if (slot.original) *slot.original = nullptr;
    }
    // The backend itself may touch code while removing its record. Never publish
    // cleanup success on its return code alone, and never unhook twice on a retry.
    if (!host.read_slot(slot, observed) || observed != slot.original_words) return false;
    slot.registered = false;
    slot.patch_known = false;
    slot.install_pending = false;
    slot.continuation_pending = false;
    slot.cleanup_pending = false;
    return true;
}

/** Install only the bound preimage; do not call a changed entry "loader relocation".
 * Unknown post-install bytes are quarantined, not adopted later by a health read. */
template<size_t N>
inline bool install_slot(InlineSlot<N> &slot, const InlineHookHost<N> &host) {
    static_assert(N > 0);
    if (!host.read_slot || !slot_writer_available(host) || (slot.address & 3u)
        || slot.address > UINTPTR_MAX - N * sizeof(uint32_t)) return false;
    CodeWriteGuard code_write_lock;
    if (!code_write_lock) return false;
    if (!settle_slot_write(slot,host)) return false;
    if (slot.cleanup_pending) { (void)recover_slot_cleanup(slot, host); return false; }
    if (slot.install_pending || slot.write_pending || slot.continuation_pending) return false;
    if (slot.registered) {
        SlotWords<N> live{};
        return slot_ready(slot) && host.read_slot(slot, live) && live == slot.patch_words;
    }
    if (!host.hook_install || !slot.original || !slot.replacement) return false;
    SlotWords<N> before{};
    if (!host.read_slot(slot, before) || before != slot.original_words) {
        if (host.on_guard) host.on_guard({"motion hook refused; bound preimage changed", 0});
        return false;
    }
    if (host.protect_range && !host.protect_range(slot.address, N * sizeof(uint32_t))) return false;
    // The policy callback can fail/change the entry. Recheck before backend work.
    if (!host.read_slot(slot, before) || before != slot.original_words) return false;
    void *previous = *slot.original;
    const int rc = host.hook_install(reinterpret_cast<void *>(slot.address),
        slot.replacement, slot.original);
    SlotWords<N> after{};
    const bool read = host.read_slot(slot, after);
    if (rc != 0) {
        const bool output_changed = *slot.original != previous;
        if (!*slot.original) *slot.original = previous;
        // Failed callbacks may have touched words or out-parameters. Preserve an
        // uncertain record; do not blindly reinstall or overwrite a foreign edit.
        if (output_changed) slot.continuation_pending = true;
        if (!read || after != before || slot.continuation_pending) slot.install_pending = true;
        return false;
    }
    slot.backend_owned = true;
    if (!read) {
        slot.install_pending = true;
        if (host.on_guard) host.on_guard({"motion hook pending; installed bytes unverified", 0});
        return false;
    }
    if (after == before) {
        slot.cleanup_pending = true;
        (void)recover_slot_cleanup(slot, host);
        return false;
    }
    if (slot.patch_known && after != slot.patch_words) {
        slot.install_pending = true;
        return false;
    }
    // Capture only the synchronous successful backend's readback. A later
    // unrelated health read is NOT evidence of which patch the backend installed.
    slot.patch_words = after;
    slot.patch_known = true;
    if (!continuation_exists(slot)) {
        slot.cleanup_pending = true;
        const bool restored = recover_slot_cleanup(slot, host);
        if (host.on_guard) host.on_guard({restored
            ? "motion hook refused; continuation missing; cleanup verified"
            : "motion hook pending; continuation missing; cleanup incomplete", 0});
        return false;
    }
    slot.registered = true;
    return true;
}

/** Re-arm only a known patch over its exact original, or verify our existing patch.
 * A false writer result remains pending even when its bytes appear to have landed. */
template<size_t N>
inline bool restore_patch_words(InlineSlot<N> &slot, const InlineHookHost<N> &host) {
    if (!slot.patch_known || slot.cleanup_pending || slot.install_pending || slot.continuation_pending
        || !continuation_exists(slot)
        || !host.read_slot || !slot_writer_available(host)) return false;
    CodeWriteGuard code_write_lock;
    if (!code_write_lock) return false;
    if (!settle_slot_write(slot,host)) return false;
    SlotWords<N> observed{};
    if (!host.read_slot(slot, observed)
        || (observed != slot.original_words && observed != slot.patch_words)) return false;
    if (host.protect_range && !host.protect_range(slot.address, N * sizeof(uint32_t))) return false;
    if (!host.read_slot(slot, observed)
        || (observed != slot.original_words && observed != slot.patch_words)) return false;
    if (observed != slot.patch_words || slot.write_pending) {
        slot.registered = false;
        slot.write_pending = true;
        if (!write_slot_words(slot,observed,slot.patch_words,host)) return false;
        if (!host.read_slot(slot, observed) || observed != slot.patch_words) return false;
    }
    slot.write_pending = false;
    slot.install_pending = false;
    slot.registered = true;
    return true;
}

/** Retry only already-known obligations. An unverified successful install can
 * retire after the validated entry is original again; unknown bytes are not guessed. */
template<size_t N>
inline bool settle_slot_pending(InlineSlot<N> &slot, const InlineHookHost<N> &host) {
    if (!slot_has_pending(slot)) return true;
    CodeWriteGuard code_write_lock;
    if (!code_write_lock) return false;
    if (!settle_slot_write(slot,host)) return false;
    if (slot.cleanup_pending) return recover_slot_cleanup(slot, host);
    if (slot.continuation_pending) return false;
    if (slot.write_pending) return restore_patch_words(slot, host);
    if (slot.install_pending) {
        SlotWords<N> observed{};
        if (!slot.backend_owned || !host.read_slot || !host.read_slot(slot, observed)
            || observed != slot.original_words) return false;
        slot.registered = false;
        slot.cleanup_pending = true;
        return recover_slot_cleanup(slot, host);
    }
    return true;
}

/** Bring the selected bank live; unknown/foreign words are never late-adopted. */
template<size_t T, size_t N>
inline bool ensure_slots_live(std::array<InlineSlot<N>, T> &slots,
    const InlineHookHost<N> &host, std::span<const size_t> order) {
    if (!valid_slot_order(slots, order)) return false;
    if (order.empty()) return true;
    if (!host.read_slot || !slot_writer_available(host)) return false;
    CodeWriteGuard code_write_lock;
    if (!code_write_lock) return false;
    for (const auto index : order) {
        auto &slot = slots[index];
        if (!settle_slot_write(slot,host)) return false;
        if (slot.cleanup_pending) { (void)recover_slot_cleanup(slot, host); return false; }
        if (slot.registered && !continuation_exists(slot)) {
            slot.backend_owned = true;
            slot.registered = false;
            slot.cleanup_pending = true;
            const bool restored = recover_slot_cleanup(slot, host);
            if (host.on_guard) host.on_guard({restored
                ? "motion hook disarmed; cleanup verified"
                : "motion hook pending; cleanup incomplete", index});
            return false;
        }
        if (slot.registered) {
            SlotWords<N> observed{};
            if (!host.read_slot(slot, observed)) return false;
            if (slot_ready(slot) && observed == slot.patch_words) continue;
            if (observed != slot.original_words) {
                if (host.on_guard) host.on_guard({"motion hook foreign or unknown words; leaving untouched", index});
                return false;
            }
            slot.registered = false;
        }
        // A refilled file-backed page does not remove the backend's record.
        // Reinstalling an already-known slot can return duplicate-hook failure
        // and clear/change the continuation out-parameter, poisoning a valid
        // record before restore gets a chance to run. Preserve the established
        // continuation and restore ONLY the verified patch over its original.
        // A failed restore stays pending; it must never fall through to a new
        // backend installation with uncertain ownership.
        const bool live = slot.patch_known
            ? restore_patch_words(slot, host)
            : install_slot(slot, host);
        if (!live) {
            if (host.on_info) host.on_info({"motion hook not verified; channel stays down", index});
            return false;
        }
    }
    return true;
}

/** Disarm without freeing a still-reachable continuation before a failed write.
 * Production currently keeps banks for process life; backend in-flight draining
 * remains its own contract, not something this word-state machine can prove. */
template<size_t N>
inline bool uninstall_slot(InlineSlot<N> &slot, const InlineHookHost<N> &host) {
    if (!slot.registered && !slot.backend_owned && !slot_has_pending(slot)) return true;
    CodeWriteGuard code_write_lock;
    if (!code_write_lock) return false;
    if (!settle_slot_write(slot,host)) return false;
    if (!host.read_slot || !slot_writer_available(host)
        || (slot.continuation_pending && !slot.backend_owned)) return false;
    SlotWords<N> observed{};
    if (!host.read_slot(slot, observed)
        || (observed != slot.original_words && (!slot.patch_known || observed != slot.patch_words))) {
        if (host.on_guard) host.on_guard({"motion hook uninstall refused; slot is not ours", 0});
        return false;
    }
    if (!host.hook_uninstall && (slot.backend_owned || continuation_exists(slot))) return false;
    // Support an already-recorded bank as well as a freshly installed one.
    slot.backend_owned = slot.backend_owned || slot.registered || continuation_exists(slot);
    slot.registered = false;
    slot.cleanup_pending = true;
    return recover_slot_cleanup(slot, host);
}

/**
 * Health verification: every slot registered, continuation present, live
 * words equal to the already verified patch. Health is observational only.
 * `read_all` is the feature's batched validated read; it exists so a healthy
 * steady state costs one inventory round trip, not one per slot.
 */
template<size_t kTargets, size_t kPatchWords, typename ReadAll>
inline bool slots_healthy(const std::array<InlineSlot<kPatchWords>, kTargets> &slots,
    ReadAll read_all) {
    std::array<uintptr_t, kTargets> addresses{};
    std::array<CodeSource, kTargets> sources{};
    std::array<SlotWords<kPatchWords>, kTargets> observed{};
    for (size_t i = 0; i < kTargets; ++i) {
        addresses[i] = slots[i].address;
        sources[i] = slots[i].source;
        if (!slot_ready(slots[i])) return false;
    }
    std::array<size_t, kTargets> order{};
    for (size_t i = 0; i < kTargets; ++i) order[i] = i;
    if (!valid_slot_order(slots, order)) return false;
    if (!read_all(addresses, sources, observed)) return false;
    for (size_t i = 0; i < kTargets; ++i) {
        auto &slot = slots[i];
        if (observed[i] != slot.patch_words) return false;
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
inline bool ordered_slots_healthy(const std::array<InlineSlot<kPatchWords>, kTargets> &slots,
    std::span<const size_t> order, ReadAll read_all) {
    if (!valid_slot_order(slots, order)) return false;
    std::array<uintptr_t, kTargets> addresses{};
    std::array<CodeSource, kTargets> sources{};
    std::array<SlotWords<kPatchWords>, kTargets> observed{};
    size_t armed = 0;
    for (const size_t index : order) {
        if (index >= kTargets) return false;
        auto &slot = slots[index];
        if (!slot_ready(slot)) return false;
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
        if (live != slot.patch_words) return false;
    }
    return true;
}

} // namespace nhk
