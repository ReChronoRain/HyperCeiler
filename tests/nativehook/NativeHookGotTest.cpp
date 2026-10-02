/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Host tests for the GOT backend (nativehook/got_hook_backend.h): RELRO-safe
 * pointer replacement, all-or-nothing rollback, uniqueness gate and the
 * health model (healthy / rearmable / foreign).
 *
 * Compiled with -DNHK_NO_PROC_MAPS on macOS: read_protection reports a
 * writable page, so the tests exercise atomic replacement, rollback and
 * health accounting; the mprotect toggle itself is device-verified.
 */
#include "../../app/src/main/cpp/nativehook/got_hook_backend.h"

#include "../nativehook/elf_fixture.h"

#include <cstdio>
#include <cstring>
#include <map>
#include <sys/mman.h>
#include <vector>

static int failures = 0;

static void check(bool condition, const char *message) {
    if (!condition) {
        std::printf("FAIL: %s\n", message);
        ++failures;
    }
}

namespace {
// Two zeroed pages standing in for the image's RW data (GOT slots).
void *g_pages = nullptr;
constexpr size_t kPageBytes = 0x4000;

bool read_live(uintptr_t slot, const void *&value) {
    return std::memcpy(&value, reinterpret_cast<void *>(slot), sizeof(value)) != nullptr;
}

// Injection for the residue test: the first write stores and reports a failed
// protection restore (the store landed), and every later write fails outright -
// which is exactly the sequence that leaves a slot holding the replacement while
// the rollback reports itself unclean.
int g_write_calls = 0;
nhk::PointerWriteResult injected_write(uintptr_t, void *, int) {
    ++g_write_calls;
    nhk::PointerWriteResult result;
    if (g_write_calls == 1) {
        result.stored = true;             // The failed slot did store.
        result.protection_restored = false;
        return result;
    }
    return result; // Rollback writes fail too.
}

const void *live_value(uintptr_t slot) {
    const void *value = nullptr;
    read_live(slot, value);
    return value;
}
} // namespace

int main() {
    using nhk::elf::ElfImage;

    g_pages = mmap(nullptr, kPageBytes, PROT_READ | PROT_WRITE,
        MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    check(g_pages != MAP_FAILED, "test pages mapped");
    if (g_pages == MAP_FAILED) return 1;
    std::memset(g_pages, 0, kPageBytes);
    const uintptr_t pages = reinterpret_cast<uintptr_t>(g_pages);
    // The fixture's slot RVAs start at 0xc00; mirror that layout in the pages.
    auto slot_at = [&](uint64_t rva) { return pages + (rva - 0xc00); };

    void *original_madvise = reinterpret_cast<void *>(0x1234);
    void *original_memcpy = reinterpret_cast<void *>(0x5678);
    void *fake_replacement = reinterpret_cast<void *>(0xdead);

    const auto fixture = elf_fixture::build(
        {"dock_target"},
        {{"madvise", nhk::elf::kRelocJumpSlot, 0xc00}, {"memcpy", nhk::elf::kRelocJumpSlot, 0xc08}, {"memcpy", nhk::elf::kRelocJumpSlot, 0xc10}});
    check(fixture.has_value(), "fixture builds");
    const auto image = ElfImage::make(pages - 0xc00 /* load bias consistent with slots */,
        std::span<const std::byte>(reinterpret_cast<const std::byte *>(fixture->data()),
            fixture->size()));
    check(image.has_value(), "image parses");
    if (!image) return 1;

    // Seed the live slots with their originals.
    *reinterpret_cast<void **>(slot_at(0xc00)) = original_madvise;
    *reinterpret_cast<void **>(slot_at(0xc08)) = original_memcpy;
    *reinterpret_cast<void **>(slot_at(0xc10)) = original_memcpy;

    // --- write_pointer: atomic replace + protection restore. ---
    check(nhk::write_pointer(slot_at(0xc00), fake_replacement), "write_pointer succeeds");
    check(live_value(slot_at(0xc00)) == fake_replacement, "slot holds the replacement");
    check(nhk::write_pointer(slot_at(0xc00), original_madvise), "write_pointer restores");
    check(live_value(slot_at(0xc00)) == original_madvise, "slot holds the original again");

    // --- install_got_hooks: happy path registers health. ---
    std::vector<nhk::GotHook<>> installed;
    const std::vector<nhk::GotHookRequest> requests = {
        {"madvise", fake_replacement},
        {"memcpy", fake_replacement},
    };
    check(nhk::install_got_hooks(*image, requests, installed,
              [](uintptr_t slot, const void *&value) { return read_live(slot, value); }),
        "both symbols installed");
    check(installed.size() == 2, "two hooks registered");
    if (installed.size() == 2) {
        check(installed[0].slot_count == 1, "madvise kept its single slot");
        check(installed[1].slot_count == 2, "memcpy kept both slots");
        check(installed[0].health([](uintptr_t slot, const void *&value) { return read_live(slot, value); })
                == nhk::GotHook<>::State::healthy,
            "healthy after install");
    }
    check(live_value(slot_at(0xc00)) == fake_replacement, "madvise slot replaced");
    check(live_value(slot_at(0xc08)) == fake_replacement, "memcpy slot 1 replaced");
    check(live_value(slot_at(0xc10)) == fake_replacement, "memcpy slot 2 replaced");

    // --- Restore originals by hand: the hook becomes re-armable. ---
    *reinterpret_cast<void **>(slot_at(0xc00)) = original_madvise;
    check(installed[0].health([](uintptr_t slot, const void *&value) { return read_live(slot, value); })
            == nhk::GotHook<>::State::rearmable,
        "original pointer means re-armable");

    // --- A foreign pointer must never be overwritten: health says foreign. ---
    *reinterpret_cast<void **>(slot_at(0xc00)) = reinterpret_cast<void *>(0xbad0);
    check(installed[0].health([](uintptr_t slot, const void *&value) { return read_live(slot, value); })
            == nhk::GotHook<>::State::foreign,
        "foreign pointer detected");

    // --- Rollback: a request whose slots disagree fails and writes nothing. ---
    *reinterpret_cast<void **>(slot_at(0xc08)) = original_memcpy;   // memcpy agrees...
    *reinterpret_cast<void **>(slot_at(0xc10)) = fake_replacement;  // ...but not with slot 2.
    const std::vector<nhk::GotHookRequest> disagreeing = {{"memcpy", fake_replacement}};
    std::vector<nhk::GotHook<>> refused;
    check(!nhk::install_got_hooks(*image, disagreeing, refused,
              [](uintptr_t slot, const void *&value) { return read_live(slot, value); }),
        "disagreeing slots fail closed");
    check(refused.empty(), "nothing registered for a refused symbol");

    // --- restore_got_hooks writes the agreed originals back. ---
    {
        // Re-install cleanly first (the earlier section left a foreign pointer).
        *reinterpret_cast<void **>(slot_at(0xc00)) = original_madvise;
        *reinterpret_cast<void **>(slot_at(0xc08)) = original_memcpy;
        *reinterpret_cast<void **>(slot_at(0xc10)) = original_memcpy;
        std::vector<nhk::GotHook<>> fresh;
        check(nhk::install_got_hooks(*image, requests, fresh,
                  [](uintptr_t slot, const void *&value) { return read_live(slot, value); }),
            "re-install for restore test");
        check(nhk::restore_got_hooks(fresh,
                  [](uintptr_t slot, const void *&value) { return read_live(slot, value); },
                  [](uintptr_t slot, void *value) { return nhk::write_pointer(slot, value); }),
            "restore_got_hooks succeeds");
        check(live_value(slot_at(0xc00)) == original_madvise, "madvise slot restored");
        check(live_value(slot_at(0xc08)) == original_memcpy, "memcpy slot 1 restored");
        check(live_value(slot_at(0xc10)) == original_memcpy, "memcpy slot 2 restored");
    }

    // --- Partial failure rolls back every slot that was attempted. ---
    //
    // mprotect cannot be made to fail on a host page, so the rollback contract
    // is exercised through the extracted helper with an injecting writer: the
    // second write fails, and the first (already replaced) slot must be put
    // back - including the failing index itself, which may have stored its
    // value before the protection restore failed.
    {
        uintptr_t slots[3] = {0x1111, 0x2222, 0x3333};
        void *const original = reinterpret_cast<void *>(0x3000);
        void *const replacement = reinterpret_cast<void *>(0x4000);
        std::vector<std::pair<uintptr_t, void *>> writes;
        int calls = 0;
        const bool ok = nhk::write_all_or_rollback(
            std::span<const uintptr_t>(slots, 3), replacement, original,
            [&](uintptr_t slot, void *value) {
                ++calls;
                if (calls == 2) return false; // Fail in the middle.
                writes.emplace_back(slot, value);
                return true;
            });
        check(!ok, "a failing write fails the whole request");
        check(writes.size() == 3, "the failed write and its rollback are both attempted");
        if (writes.size() == 3) {
            check(writes[0] == std::make_pair(slots[0], replacement),
                "first slot was written before the failure");
            check(writes[1] == std::make_pair(slots[0], original),
                "first slot rolled back to the original");
            check(writes[2] == std::make_pair(slots[1], original),
                "the failing slot is rolled back too");
        }

        // A rollback write that fails must be reported, not hidden.
        bool rollback_clean = true;
        calls = 0;
        const bool ok2 = nhk::write_all_or_rollback(
            std::span<const uintptr_t>(slots, 3), replacement, original,
            [&](uintptr_t, void *) {
                ++calls;
                return calls == 1; // First (the write) succeeds, rollback fails.
            },
            &rollback_clean);
        check(!ok2, "failed request reported");
        check(!rollback_clean, "an incomplete rollback is reported to the caller");
    }

    // --- An unclean rollback leaves a record, not silence. ---
    //
    // Before this, a request whose rollback failed returned false with an empty
    // `installed`, so nobody could tell that a slot might still hold the
    // replacement - the residue was unrecoverable by construction.
    {
        *reinterpret_cast<void **>(slot_at(0xc00)) = original_madvise;
        *reinterpret_cast<void **>(slot_at(0xc08)) = original_memcpy;
        *reinterpret_cast<void **>(slot_at(0xc10)) = original_memcpy;
        std::vector<nhk::GotHook<>> residues;
        std::vector<nhk::GotHook<>> none;
        bool clean = true;
        g_write_calls = 0;
        nhk::pointer_write_override() = injected_write;
        const std::vector<nhk::GotHookRequest> one = {{"memcpy", fake_replacement}};
        const bool ok = nhk::install_got_hooks(*image, one, none,
            [](uintptr_t slot, const void *&value) { return read_live(slot, value); },
            true, &clean, nhk::ImageIdentity{1, 2, 0x1234, pages - 0xc00}, &residues);
        nhk::pointer_write_override() = nullptr;
        check(!ok, "an unclean rollback fails the request");
        check(!clean, "the caller is told the rollback was not clean");
        check(residues.size() == 1,
            "the residue is recorded instead of being dropped");
        if (!residues.empty()) {
            check(residues[0].partial, "the record is marked partial");
            check(residues[0].stored >= 1, "the record knows a slot stored");
            check(!residues[0].rollback_was_clean, "the record knows the rollback failed");
            check(residues[0].identity.inode == 0x1234, "the record carries the image identity");
        }
        check(none.empty(), "no healthy record is produced");
        // Restore the live pages for the following sections.
        *reinterpret_cast<void **>(slot_at(0xc00)) = original_madvise;
        *reinterpret_cast<void **>(slot_at(0xc08)) = original_memcpy;
        *reinterpret_cast<void **>(slot_at(0xc10)) = original_memcpy;
    }

    // --- Image identity separates "remapped" from "taken over". ---
    {
        nhk::GotHook<> hook;
        hook.identity = nhk::ImageIdentity{1, 2, 0x4242, 0x7100000000ULL};
        // identity_present() is a template over any range with an inode and a
        // base address, so the test needs no mapping type from the image layer.
        struct Range {
            uintptr_t begin;
            uint64_t file_offset;
            uint64_t inode;
        };
        // identity derives the load bias from `begin - file_offset`, so a
        // remap is a change even when the inode is unchanged.
        const std::vector<Range> present{{0x7100000000ULL, 0, 0x4242}};
        const std::vector<Range> remapped{{0x7200000000ULL, 0, 0x4242}};
        const std::vector<Range> other_file{{0x7100000000ULL, 0, 0x9999}};
        check(hook.identity_present(present), "same generation is present");
        check(!hook.identity_present(remapped),
            "a mapping at a new load base is a different generation");
        check(!hook.identity_present(other_file), "another inode is not this image");
    }

    // --- A residue needs both halves back: pointers *and* page protections. ---
    //
    // The write path stores the value and only then restores the page, so "the
    // slot reads as the original" can coexist with "the page is still writable".
    // Releasing the record there would let the next install capture RW as "the
    // original" and leave the RELRO page permanently writable - so the residue
    // test must cover the protection, not only the pointer.
    {
        nhk::GotHook<> hook;
        hook.pages[0] = {0x1000, PROT_READ};
        hook.pages[1] = {0x2000, PROT_READ | PROT_EXEC};
        hook.page_count = 2;

        std::map<uintptr_t, int> live{{0x1000, PROT_READ},
            {0x2000, PROT_READ | PROT_EXEC}};
        auto read = [&](uintptr_t page) {
            const auto found = live.find(page);
            return found == live.end() ? 0 : found->second;
        };
        check(nhk::protections_restored(hook, read),
            "matching bits count as restored");

        // One page left writable: the residue is not undone.
        live[0x2000] = PROT_READ | PROT_WRITE;
        check(!nhk::protections_restored(hook, read),
            "a page left writable keeps the residue alive");

        // An unreadable page proves nothing either: it must not read as restored.
        live[0x2000] = PROT_READ | PROT_EXEC;
        live.erase(0x1000);
        check(!nhk::protections_restored(hook, read),
            "an unreadable page is not a restored page");

        // Re-applying must use the *captured* bits, never re-sample the live
        // ones: the live ones are exactly the ones that cannot be trusted.
        live[0x1000] = PROT_READ | PROT_WRITE;
        std::vector<nhk::ProtectedPage> applied;
        const bool ok = nhk::reapply_protections(hook,
            [&](const nhk::ProtectedPage &page) {
                applied.push_back(page);
                live[page.page] = page.protection;
                return true;
            });
        check(ok, "reapply reports success");
        check(applied.size() == 2, "every captured page is re-applied");
        if (applied.size() == 2) {
            check(applied[0].protection == PROT_READ
                    && applied[1].protection == (PROT_READ | PROT_EXEC),
                "reapply uses the captured bits, not the live ones");
        }
        check(nhk::protections_restored(hook, read),
            "the residue is gone once the captured bits are back");

        // An mprotect that fails must be reported, not swallowed.
        check(!nhk::reapply_protections(hook,
                  [](const nhk::ProtectedPage &) { return false; }),
            "a rejected page is reported");
    }

    // --- Missing symbol fails closed. ---
    const std::vector<nhk::GotHookRequest> absent = {{"not_an_import", fake_replacement}};
    std::vector<nhk::GotHook<>> empty_result;
    check(!nhk::install_got_hooks(*image, absent, empty_result,
              [](uintptr_t slot, const void *&value) { return read_live(slot, value); }),
        "absent symbol fails closed");

    if (failures == 0) std::printf("NativeHookGotTest passed\n");
    return failures == 0 ? 0 : 1;
}
