/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Host tests for the page lifetime policy (nativehook/page_guard.h):
 * protected-page registration and the MADV_DONTNEED splitting around them.
 * A fake real madvise records the ranges it received, so the test can prove
 * that protected pages are carved out and everything else passes through.
 */
#include "../../app/src/main/cpp/nativehook/page_guard.h"

#include <cstdio>
#include <cstring>
#include <vector>

static int failures = 0;

static void check(bool condition, const char *message) {
    if (!condition) {
        std::printf("FAIL: %s\n", message);
        ++failures;
    }
}

namespace {
struct DiscardRecord {
    uintptr_t begin;
    size_t length;
    int advice = 4;
};
std::vector<DiscardRecord> g_discards;
int g_fake_result = 0;

int fake_madvise(void *address, size_t length, int advice) {
    if (g_fake_result != 0) return g_fake_result; // Failure injection.
    g_discards.push_back({reinterpret_cast<uintptr_t>(address), length, advice});
    return 0;
}

void reset() {
    g_discards.clear();
    g_fake_result = 0;
}
} // namespace

int main() {
    const uint64_t page = nhk::host_page_size();
    const uintptr_t base = 0x500000000ULL; // Aligned synthetic region.

    // --- Non-DONTNEED advice passes through untouched. ---
    reset();
    check(nhk::guarded_madvise(reinterpret_cast<void *>(base), page, 8 /* MADV_FREE */,
              fake_madvise) == 0,
        "non-DONTNEED passes through");
    check(g_discards.size() == 1 && g_discards[0].length == page,
        "non-DONTNEED reached the real madvise");

    // --- Without protection, DONTNEED passes through as one call. ---
    reset();
    check(nhk::guarded_madvise(reinterpret_cast<void *>(base), page * 4, 4,
              fake_madvise) == 0,
        "unprotected DONTNEED passes through");
    check(g_discards.size() == 1, "unprotected discard is one range");

    // --- Protect one page: a range touching it is split around it. ---
    nhk::add_protected_page(base + page * 2);
    reset();
    check(nhk::guarded_madvise(reinterpret_cast<void *>(base), page * 4, 4,
              fake_madvise) == 0,
        "guarded discard succeeds");
    check(g_discards.size() == 3, "three pages survive, the protected one is carved out");
    if (g_discards.size() == 3) {
        check(g_discards[0].begin == base && g_discards[0].length == page,
            "page 0 discarded");
        check(g_discards[1].begin == base + page && g_discards[1].length == page,
            "page 1 discarded");
        check(g_discards[2].begin == base + page * 3 && g_discards[2].length == page,
            "page 3 discarded; page 2 kept");
    }

    // --- Registration is idempotent. ---
    nhk::add_protected_page(base + page * 2);
    reset();
    nhk::guarded_madvise(reinterpret_cast<void *>(base), page * 4, 4, fake_madvise);
    check(g_discards.size() == 3, "idempotent registration keeps the same split");

    // --- A real failure inside a forwarded chunk propagates. ---
    reset();
    g_fake_result = -12;
    check(nhk::guarded_madvise(reinterpret_cast<void *>(base), page * 4, 4,
              fake_madvise) == -12,
        "real madvise failure propagates");

    // --- Unaligned ranges are handed to the kernel unchanged (its own EINVAL). ---
    {
        reset();
        const uintptr_t misaligned = base + 1;
        check(nhk::guarded_madvise(reinterpret_cast<void *>(misaligned), page, 4,
                  fake_madvise) == 0,
            "unaligned DONTNEED is not reshaped");
        check(g_discards.size() == 1 && g_discards[0].begin == misaligned,
            "unaligned request reached the real madvise verbatim");
    }

    // --- A full page table reports failure instead of dropping silently. ---
    {
        // Fill the remaining slots (the table is process-wide).
        uintptr_t filler = 0x700000000ULL;
        size_t accepted = 0;
        for (size_t i = 0; i < nhk::kMaxProtectedPages + 8; ++i) {
            if (nhk::add_protected_page(filler + i * page)) ++accepted;
        }
        check(accepted >= 1, "page table accepts registrations");
        // Once full, registration must report false.
        bool saw_refusal = false;
        for (size_t i = 0; i < nhk::kMaxProtectedPages + 8; ++i) {
            if (!nhk::add_protected_page(0x800000000ULL + i * page)) {
                saw_refusal = true;
                break;
            }
        }
        check(saw_refusal, "full page table reports refusal");
    }

    if (failures == 0) std::printf("NativeHookPageGuardTest passed\n");
    return failures == 0 ? 0 : 1;
}
