/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * NativeHookRuntime GOT backend: caller-specific PLT/GOT hooks.
 *
 * Ported from MiuiBackGestureHook's lsposed_hook_backend.cpp
 * (WritePointer / CollectRelocationSlots / PltHookRaw and the RELRO handling;
 * Apache-2.0, see THIRD_PARTY_NOTICES.md in this directory) and decoupled
 * from its launcher targets: the backend works over an [elf::ElfImage] of
 * whatever library the caller hands it and never names a module itself.
 *
 * Semantics carried over:
 *  - A symbol's slots are JUMP_SLOT plus GLOB_DAT relocations. Multiple slots
 *    per symbol are acceptable only while every slot still holds the same
 *    original pointer; one deviating slot fails the whole installation.
 *  - RELRO pages are temporarily made writable per page, the pointer is
 *    stored atomically, and the original protection is restored afterwards.
 *  - Installation is all-or-nothing: a failure part-way rolls the already
 *    written slots back to the original pointer. No partial writes survive.
 *  - Health model: all slots == replacement is healthy; all slots == the
 *    known original is re-armable; anything else is a foreign edit and is
 *    reported, never overwritten.
 */
#pragma once

#include "elf_image.h"
#include "nhk_base.h"
#include "page_guard.h"

#include <atomic>
#include <cstdint>
#include <fstream>
#include <optional>
#include <span>
#include <string>
#include <string_view>
#include <vector>

#include <sys/mman.h>
#include <unistd.h>

namespace nhk {

/**
 * A page protection captured *before* a transaction starts.
 *
 * Re-reading the protection during rollback is wrong: if the first attempt made
 * the page writable and then failed to restore it, a re-read sees RW and
 * "restores" RW - leaving RELRO permanently writable while reporting success.
 * The transaction therefore carries the original value to every write.
 */
struct ProtectedPage {
    uintptr_t page = 0;
    int protection = 0;
};

/** The captured protection of the page holding `slot`, or nothing. */
inline std::optional<int> protection_for(std::span<const ProtectedPage> pages,
    uintptr_t slot) {
    const uintptr_t page = static_cast<uintptr_t>(page_down(slot));
    for (const auto &entry : pages) {
        if (entry.page == page) return entry.protection;
    }
    return {};
}

/**
 * Which image generation a hook belongs to.
 *
 * Without this a maintenance pass cannot tell "the library was remapped, the old
 * address is simply gone" from "someone else took the slot": both look like an
 * unreadable-or-foreign slot. Identity is taken from the target's own mappings,
 * never from some other library's inventory.
 */
struct ImageIdentity {
    uint32_t device_major = 0;
    uint32_t device_minor = 0;
    uint64_t inode = 0;
    uint64_t load_base = 0;

    bool operator==(const ImageIdentity &) const = default;
};

/** One installed GOT hook and its observable health. */
template<size_t kMaxSlots = 8>
struct GotHook {
    static constexpr size_t kSlotCapacity = kMaxSlots;

    std::string symbol;
    uintptr_t slots[kMaxSlots]{};
    size_t slot_count = 0;
    void *replacement = nullptr;
    void *expected = nullptr; // The original pointer all slots agreed on.
    // Protections captured before the first write, so a rollback or a later
    // restore returns the pages to their original bits rather than to whatever
    // a failed attempt happened to leave behind.
    ProtectedPage pages[kMaxSlots]{};
    size_t page_count = 0;
    ImageIdentity identity{};
    // A failed request that could not be rolled back cleanly still produces a
    // record: the slots may hold the replacement, and losing that knowledge is
    // what made such a state unrecoverable. `partial` records are never treated
    // as healthy and never overwritten.
    bool partial = false;
    size_t attempted = 0;   // Slots the request tried to write.
    size_t stored = 0;      // Slots whose pointer value actually changed.
    bool rollback_was_clean = true;

    /**
     * Whether this hook's image generation still exists in `current`.
     *
     * The test is the mapping's implied load bias (`begin - file_offset`), not
     * "some mapping begins at the load base": the first mapping of a library is
     * not guaranteed to be the one at offset 0, and a remap elsewhere in the
     * order would then look like a generation change.
     */
    template<typename Ranges>
    bool identity_present(const Ranges &current) const {
        if (identity.inode == 0) return true; // No identity recorded: assume present.
        for (const auto &range : current) {
            if (range.inode != identity.inode) continue;
            if (static_cast<uint64_t>(range.begin) < range.file_offset) continue;
            if (static_cast<uint64_t>(range.begin) - range.file_offset
                == identity.load_base) {
                return true;
            }
        }
        return false;
    }

    enum class State { healthy, rearmable, foreign, empty };

    template<typename ReadPointer>
    State health(ReadPointer read_pointer) const {
        if (slot_count == 0) return State::empty;
        bool replaced = true;
        bool original = true;
        for (size_t i = 0; i < slot_count; ++i) {
            const void *current = nullptr;
            if (!read_pointer(slots[i], current)) return State::foreign;
            if (current != replacement) replaced = false;
            if (current != expected) original = false;
        }
        if (replaced) return State::healthy;
        if (original) return State::rearmable;
        return State::foreign;
    }

    /** The protection captured for `slot`, if this hook captured one. */
    std::optional<int> captured_protection(uintptr_t slot) const {
        return protection_for(
            std::span<const ProtectedPage>(pages, page_count), slot);
    }
};

/**
 * Protection bits of the page containing `page`, parsed directly from
 * `/proc/self/maps`.
 *
 * This deliberately does not reuse the file-inventory parser: that one keeps
 * only file-backed ranges (anonymous mappings have no path) and records just
 * the executable/writable bits. A slot table can perfectly well live in an
 * anonymous mapping, and the restore step needs the real permission triple, so
 * a lost PROT_READ must never be invented.
 *
 * Hosts without procfs (the macOS test environment) compile with
 * NHK_NO_PROC_MAPS and report a read-write page: the tests then still exercise
 * atomic replacement and rollback, while the permission toggle itself is
 * device-verified.
 */
#ifndef NHK_NO_PROC_MAPS
inline int read_protection(uintptr_t page) {
    std::ifstream maps("/proc/self/maps");
    if (!maps) return 0;
    std::string line;
    while (std::getline(maps, line)) {
        unsigned long long begin = 0;
        unsigned long long end = 0;
        unsigned long long offset = 0;
        unsigned long long inode = 0;
        unsigned device_major = 0;
        unsigned device_minor = 0;
        char permissions[5]{};
        if (std::sscanf(line.c_str(), "%llx-%llx %4s %llx %x:%x %llu",
                &begin, &end, permissions, &offset, &device_major, &device_minor,
                &inode) != 7) {
            continue;
        }
        if (end <= begin || page < begin || page >= end) continue;
        int protection = 0;
        if (permissions[0] == 'r') protection |= PROT_READ;
        if (permissions[1] == 'w') protection |= PROT_WRITE;
        if (permissions[2] == 'x') protection |= PROT_EXEC;
        return protection;
    }
    return 0; // Unknown page: the caller must refuse to write.
}
#else
inline int read_protection(uintptr_t) {
    return PROT_READ | PROT_WRITE;
}
#endif

/** Result of one pointer write, with the protection outcome kept separate. */
struct PointerWriteResult {
    bool stored = false;         // The pointer value was written.
    bool protection_restored = false; // The page is back to its original bits.
    bool ok() const { return stored && protection_restored; }
};

/**
 * Store `value` into the pointer at `slot`, using the protection captured for
 * its page. Returns whether the store happened and, independently, whether the
 * original protection was restored - a caller must be able to tell "the pointer
 * moved but the page is still writable" from "nothing happened".
 */
inline PointerWriteResult write_pointer_with_protection(uintptr_t slot, void *value,
    int original_protection) {
    PointerWriteResult result;
    const uint64_t page_size = host_page_size();
    const uintptr_t page = static_cast<uintptr_t>(page_down(slot));
    if (mprotect(reinterpret_cast<void *>(page), page_size,
            original_protection | PROT_WRITE) != 0) {
        return result;
    }
    __atomic_store_n(reinterpret_cast<void **>(slot), value, __ATOMIC_RELEASE);
    result.stored = true;
    result.protection_restored =
        mprotect(reinterpret_cast<void *>(page), page_size, original_protection) == 0;
    return result;
}

/**
 * Capture the protection of every page a set of slots lives on, once per page.
 * Returns nothing when any page's protection cannot be determined: without the
 * original bits the transaction could not be undone faithfully.
 */
inline std::optional<std::vector<ProtectedPage>> capture_protections(
    std::span<const uintptr_t> slots) {
    std::vector<ProtectedPage> captured;
    for (const uintptr_t slot : slots) {
        const uintptr_t page = static_cast<uintptr_t>(page_down(slot));
        const bool known = std::ranges::any_of(captured,
            [&](const ProtectedPage &entry) { return entry.page == page; });
        if (known) continue;
        const int protection = read_protection(page);
        if (protection == 0) return {}; // Unknown page: refuse to write it.
        if (captured.size() >= GotHook<>::kSlotCapacity) return {};
        captured.push_back({page, protection});
    }
    return captured;
}

/**
 * Whether every page a hook captured protection for still holds the bits it had
 * before the first write.
 *
 * A slot that reads back as `expected` is not proof that the transaction is
 * undone: the write path stores the value and *only then* restores the page, so
 * a page can be left writable while its slot already holds the original pointer.
 * Releasing the record at that point would let the next install capture RW as
 * "the original" and leave the RELRO page permanently writable.
 *
 * `read` is parameterized so the host tests can drive the comparison with a
 * synthetic protection source; the default reads the real page.
 */
template<size_t kMaxSlots, typename ReadProtection>
inline bool protections_restored(const GotHook<kMaxSlots> &hook,
    ReadProtection read) {
    for (size_t i = 0; i < hook.page_count; ++i) {
        const int current = read(hook.pages[i].page);
        // An unreadable page is not "restored", and neither is a different
        // triple: both cases must keep the residue alive.
        if (current == 0 || current != hook.pages[i].protection) return false;
    }
    return true;
}

template<size_t kMaxSlots>
inline bool protections_restored(const GotHook<kMaxSlots> &hook) {
    return protections_restored(
        hook, [](uintptr_t page) { return read_protection(page); });
}

/**
 * Put every page a hook captured back to its original bits.
 *
 * Used to drain a residue whose slots already read back as the original: the
 * pointer is home, only the permission is wrong, and re-applying the captured
 * value is the faithful repair - it never samples the current bits, which are
 * exactly the ones that cannot be trusted.
 *
 * `apply` is parameterized for the same reason as [protections_restored]; the
 * default is the real mprotect. Returns whether every page was accepted, but
 * callers must re-check with [protections_restored] rather than trust the
 * return value alone.
 */
template<size_t kMaxSlots, typename ApplyProtection>
inline bool reapply_protections(const GotHook<kMaxSlots> &hook,
    ApplyProtection apply) {
    bool applied = true;
    for (size_t i = 0; i < hook.page_count; ++i) {
        if (!apply(hook.pages[i])) applied = false;
    }
    return applied;
}

template<size_t kMaxSlots>
inline bool reapply_protections(const GotHook<kMaxSlots> &hook) {
    return reapply_protections(hook, [](const ProtectedPage &page) {
        return mprotect(reinterpret_cast<void *>(page.page), host_page_size(),
                   page.protection) == 0;
    });
}

/**
 * Test seam for the pointer write path.
 *
 * A double-write failure (the store landing, the protection restore failing, and
 * then the rollback write failing) cannot be provoked on a real page: mprotect
 * will not fail on demand. Tests install an override here to exercise the
 * residue-recording path. Production code never sets it.
 */
inline PointerWriteResult (*&pointer_write_override())(uintptr_t, void *, int) {
    static PointerWriteResult (*override_fn)(uintptr_t, void *, int) = nullptr;
    return override_fn;
}

/** One write through the (optionally overridden) pointer write path. */
inline PointerWriteResult perform_pointer_write(uintptr_t slot, void *value,
    int protection) {
    const auto override_fn = pointer_write_override();
    if (override_fn != nullptr) return override_fn(slot, value, protection);
    return write_pointer_with_protection(slot, value, protection);
}

/**
 * Atomic store of `value` into `slot`, toggling write permission only for the
 * duration of the store (protection read back afterwards).
 *
 * Prefer [write_pointer_with_protection] inside a multi-slot transaction.
 */
inline bool write_pointer(uintptr_t slot, void *value) {
    const int protection = read_protection(static_cast<uintptr_t>(page_down(slot)));
    if (protection == 0) return false;
    return write_pointer_with_protection(slot, value, protection).ok();
}

/**
 * Write `replacement` into every slot, or restore `expected` in all of them.
 *
 * The failing index is rolled back too: `write` can store the pointer and then
 * fail to restore the page protection, so a slot that reports failure may well
 * hold the new value. `rollback_clean` reports whether the rollback writes
 * themselves succeeded, so a caller never presents a refusal as if the table
 * were untouched when it is not.
 *
 * Split out from [install_got_hooks] so the rollback contract is testable
 * without needing mprotect to fail on a real page.
 */
template<typename WritePointer>
inline bool write_all_or_rollback(std::span<const uintptr_t> addresses,
    void *replacement, void *expected, WritePointer write,
    bool *rollback_clean = nullptr) {
    size_t attempted = 0;
    bool wrote_all = true;
    for (size_t i = 0; i < addresses.size(); ++i) {
        attempted = i + 1;
        if (!write(addresses[i], replacement)) {
            wrote_all = false;
            break;
        }
    }
    if (wrote_all) return true;
    for (size_t i = 0; i < attempted; ++i) {
        if (!write(addresses[i], expected) && rollback_clean != nullptr) {
            *rollback_clean = false;
        }
    }
    return false;
}

/**
 * Install GOT hooks for `requests` over `image`, all-or-nothing per request.
 *
 * `read_memory` must read a slot fault-reportingly: it returns false when the
 * page cannot be read safely (concurrent refill, foreign unmapping), never
 * dereferences blindly.
 *
 * A failed request rolls its own written slots back. The rollback covers the
 * slot whose write reported failure too: write_pointer can store the pointer
 * and then fail to restore the page protection, so a "failed" slot may well
 * hold the replacement. `rollback_clean` (optional) reports whether every
 * rollback write itself succeeded - the caller must not present a refusal as
 * if the table were untouched when it is not.
 *
 * Previously successful requests stay installed; the caller decides whether
 * that is acceptable (the desktop runtime treats a partial install as
 * unhealthy and re-runs the pass, which converges).
 *
 * `protect_pages` calls the page-lifetime policy for each slot page and must
 * succeed, otherwise the symbol is refused: a slot that a concurrent
 * MADV_DONTNEED can discard would silently lose the hook.
 */
struct GotHookRequest {
    std::string_view symbol;
    void *replacement = nullptr;
};

template<typename ReadMemory>
inline bool install_got_hooks(const elf::ElfImage &image,
    std::span<const GotHookRequest> requests,
    std::vector<GotHook<>> &installed, ReadMemory read_memory,
    bool guard_pages = true, bool *rollback_clean = nullptr,
    ImageIdentity identity = {}, std::vector<GotHook<>> *residual = nullptr) {
    if (rollback_clean != nullptr) *rollback_clean = true;
    for (const auto &request : requests) {
        const auto slots = image.collect_slots(request.symbol);
        if (slots.empty()) return false;
        if (slots.size() > GotHook<>::kSlotCapacity) return false;

        // Resolve every slot to a runtime address and agree on the original.
        void *expected = nullptr;
        std::vector<uintptr_t> addresses;
        for (const auto &slot : slots) {
            const uintptr_t address = image.runtime(slot.rva);
            if (address == 0) return false;
            const void *current = nullptr;
            if (!read_memory(address, current) || current == nullptr) return false;
            if (expected == nullptr) {
                expected = const_cast<void *>(current);
            } else if (current != expected) {
                // The PLT and a data relocation disagree: refuse the symbol.
                return false;
            }
            addresses.push_back(address);
        }
        if (expected == nullptr || expected == request.replacement) return false;

        GotHook<> hook;
        hook.symbol = request.symbol;
        hook.replacement = request.replacement;
        hook.expected = expected;
        hook.slot_count = addresses.size();
        for (size_t i = 0; i < addresses.size(); ++i) {
            hook.slots[i] = addresses[i];
            if (guard_pages && !add_protected_page(addresses[i])) {
                // Page table exhausted: the slot would stay unprotected.
                return false;
            }
        }
        // Capture the original protections before touching anything: the
        // rollback must restore the bits the pages had at the start, not the
        // bits a failed attempt left behind.
        const auto pages = capture_protections(
            std::span<const uintptr_t>(addresses.data(), addresses.size()));
        if (!pages || pages->size() > GotHook<>::kSlotCapacity) return false;
        hook.page_count = pages->size();
        for (size_t i = 0; i < pages->size(); ++i) hook.pages[i] = (*pages)[i];

        // All-or-nothing write with rollback (see write_all_or_rollback). The
        // writer counts how many slots actually stored the replacement so an
        // unclean rollback can be recorded precisely.
        size_t stored = 0;
        const bool wrote = write_all_or_rollback(
            std::span<const uintptr_t>(addresses.data(), addresses.size()),
            request.replacement, expected,
            [&](uintptr_t slot, void *value) {
                const auto protection = protection_for(*pages, slot);
                if (!protection) return false;
                const auto result = perform_pointer_write(slot, value, *protection);
                // Count only writes of the replacement: those are the slots that
                // may still hold it if the rollback turns out incomplete.
                if (result.stored && value == request.replacement) ++stored;
                return result.ok();
            },
            rollback_clean);
        if (!wrote) {
            // Keep the record when the slots may still hold the replacement: an
            // unrecorded residue is unrecoverable by construction.
            if (rollback_clean != nullptr && !*rollback_clean) {
                hook.partial = true;
                hook.identity = identity;
                hook.attempted = addresses.size();
                hook.stored = stored;
                hook.rollback_was_clean = false;
                if (residual != nullptr) residual->push_back(std::move(hook));
            }
            return false;
        }
        hook.identity = identity;
        hook.attempted = addresses.size();
        hook.stored = stored;
        installed.push_back(std::move(hook));
    }
    return true;
}

/**
 * Undo every installed GOT hook: write each slot's agreed original pointer
 * back. `write` is the same primitive used to install, so the mprotect toggle
 * and atomic store semantics are identical. Returns false if any slot could not
 * be restored; restoration attempts continue for the remaining slots so a
 * single failure does not strand the rest of the table.
 */
template<typename ReadPointer, typename WritePointer>
inline bool restore_got_hooks(std::span<const GotHook<>> installed,
    ReadPointer read_pointer, WritePointer write) {
    bool all_restored = true;
    for (const auto &hook : installed) {
        for (size_t i = 0; i < hook.slot_count; ++i) {
            // Ownership: only a slot that still holds our replacement (or is
            // already back to the original) may be touched. A third party's
            // pointer is not ours to restore.
            const void *current = nullptr;
            if (!read_pointer(hook.slots[i], current)) {
                all_restored = false;
                continue;
            }
            if (current != hook.replacement && current != hook.expected) {
                all_restored = false;
                continue;
            }
            const auto protection = hook.captured_protection(hook.slots[i]);
            const bool restored = protection
                ? write_pointer_with_protection(hook.slots[i], hook.expected,
                      *protection).ok()
                : write(hook.slots[i], hook.expected);
            if (!restored) all_restored = false;
        }
    }
    return all_restored;
}

/**
 * Install the MADV_DONTNEED guard: hook the `madvise` import of the library
 * named by the caller through the GOT backend. The caller decides *which*
 * library's madvise matters (the source project defaulted to the HyperOS
 * Flutter runtime; that decision is not this layer's business).
 *
 * `publish_original` receives the original `madvise` pointer **before** the
 * GOT slot is replaced: the replacement must never be reachable while its
 * forwarding target is still unset, or a concurrent call would be swallowed.
 *
 * Returns false when the import is absent, ambiguous, or cannot be safely
 * published - the caller may still run, it simply lacks the extra page
 * protection.
 */
template<typename ReadMemory, typename PublishOriginal>
inline bool install_madvise_guard(const elf::ElfImage &image, ReadMemory read_memory,
    void *replacement, PublishOriginal publish_original,
    std::vector<GotHook<>> &installed, bool *rollback_clean = nullptr,
    ImageIdentity identity = {}, std::vector<GotHook<>> *residual = nullptr) {
    const auto slots = image.collect_slots("madvise");
    if (slots.empty()) return false;
    void *expected = nullptr;
    for (const auto &slot : slots) {
        const void *current = nullptr;
        if (!read_memory(image.runtime(slot.rva), current) || current == nullptr) return false;
        if (expected == nullptr) expected = const_cast<void *>(current);
        else if (current != expected) return false;
    }
    if (expected == nullptr || expected == replacement) return false;
    // Publish the forwarding target first: only then may the GOT slot point at
    // the replacement.
    publish_original(expected);
    std::vector<GotHookRequest> requests{{"madvise", replacement}};
    return install_got_hooks(image, requests, installed, read_memory, true,
        rollback_clean, identity, residual);
}

} // namespace nhk
