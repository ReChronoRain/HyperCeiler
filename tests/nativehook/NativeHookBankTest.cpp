/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Host tests for the shared inline slot state machine
 * (nativehook/hook_bank.h): install refusal on a null continuation, patch
 * loss and re-arm, foreign-edit protection, and the health gate. The host is
 * a plain array - the state machine must never touch memory itself.
 */
#include "../../app/src/main/cpp/nativehook/hook_bank.h"

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
constexpr size_t kWords = 4;
using Slot = nhk::InlineSlot<kWords>;
using Words = nhk::SlotWords<kWords>;

struct FakeProcess {
    std::array<Words, 3> live{};
    void *continuation[3] = {nullptr, nullptr, nullptr};
    int install_calls = 0;
    int install_results[3] = {0, 0, 0}; // per-slot result of the last backend call
    std::vector<std::string> guards;

    Words original_for(int slot) const {
        Words words{};
        for (size_t i = 0; i < kWords; ++i) words[i] = 0x1000 + slot * 16 + i;
        return words;
    }
    Words patch_for(int slot) const {
        Words words{};
        for (size_t i = 0; i < kWords; ++i) words[i] = 0x9000 + slot * 16 + i;
        return words;
    }
};

FakeProcess g_process;

bool read_slot(const Slot &slot, Words &words) {
    const auto index = static_cast<size_t>((slot.address & 0xff) / 16);
    words = g_process.live[index];
    return true;
}

bool write_words(uintptr_t, const Words &words) {
    // The fake process applies the write to the slot whose original it matches.
    for (size_t i = 0; i < 3; ++i) {
        if (g_process.live[i] == words) return true;
        if (words == g_process.patch_for(static_cast<int>(i))
            || words == g_process.original_for(static_cast<int>(i))) {
            g_process.live[i] = words;
            return true;
        }
    }
    return true;
}

int hook_unhook_calls = 0;
int hook_uninstall_result = 0;

int hook_uninstall(void *) {
    ++hook_unhook_calls;
    return hook_uninstall_result;
}

int hook_install(void *target, void *, void **original) {
    const auto slot = static_cast<size_t>(reinterpret_cast<uintptr_t>(target) & 0xff) / 16;
    ++g_process.install_calls;
    if (g_process.install_results[slot] != 0) return g_process.install_results[slot];
    // The backend applies its trampoline and hands back the continuation.
    g_process.live[slot] = g_process.patch_for(static_cast<int>(slot));
    *original = g_process.continuation[slot] != nullptr ? g_process.continuation[slot]
                                                       : nullptr;
    return 0;
}

void on_guard(const nhk::HookEvent &event) {
    g_process.guards.emplace_back(event.reason);
}
void on_info(const nhk::HookEvent &event) {
    (void)event;
}

nhk::InlineHookHost<kWords> make_host() {
    nhk::InlineHookHost<kWords> host;
    host.read_slot = read_slot;
    host.write_words = write_words;
    host.hook_install = hook_install;
    host.hook_uninstall = hook_uninstall;
    host.on_guard = on_guard;
    host.on_info = on_info;
    return host;
}
} // namespace

int main() {
    auto &process = g_process;
    process.live.fill({});
    nhk::InlineHookHost<kWords> host = make_host();

    nhk::InlineSlot<kWords> slot;
    slot.address = 0x00; // FakeProcess keys live words by (address & 0xff) / 16.
    slot.replacement = reinterpret_cast<void *>(0xfeed);
    slot.original = process.continuation;
    slot.source = nhk::CodeSource{1, 2, 3, 0};
    slot.original_words = process.original_for(0);

    // --- Refusal: backend succeeds but leaves no continuation. ---
    process.live[0] = slot.original_words;
    process.install_results[0] = 0; // "success"
    check(!nhk::install_slot(slot, host), "null continuation refuses install");
    check(!slot.registered, "slot stays disarmed after refusal");
    check(process.live[0] == slot.original_words, "prologue restored after refusal");
    check(!process.guards.empty(), "refusal reported as a guard event");

    // --- Install with a continuation. ---
    process.guards.clear();
    process.live[0] = slot.original_words;
    process.continuation[0] = reinterpret_cast<void *>(0xc0ffee);
    check(nhk::install_slot(slot, host), "install with continuation succeeds");
    check(slot.registered, "slot armed");
    check(slot.patch_known && slot.patch_words == process.patch_for(0),
        "live patch words adopted");

    // --- Healthy: patch still live. ---
    process.live[0] = slot.patch_words;
    auto read_all = [&](const std::array<uintptr_t, 1> &,
                        const std::array<nhk::CodeSource, 1> &,
                        std::array<Words, 1> &out) {
        out[0] = process.live[0];
        return true;
    };
    std::array<Slot, 1> healthy_slots{slot};
    check(nhk::slots_healthy(healthy_slots, read_all), "healthy when patch words live");
    check(healthy_slots[0].patch_known, "health pass keeps patch words known");

    // --- Patch lost (file page refilled): re-arm restores it. ---
    process.live[0] = slot.original_words;
    const size_t before = process.install_calls;
    std::array<Slot, 1> slots{slot};
    const std::vector<size_t> order{0};
    check(nhk::ensure_slots_live(slots, host, order), "re-arm succeeds after patch loss");
    check(process.install_calls == static_cast<int>(before) + 1, "re-arm went through the backend");
    check(process.live[0] == process.patch_for(0), "patch words live again");
    slot = slots[0];

    // --- Foreign edit: neither patch nor original. Must be left untouched. ---
    const Words foreign = {0xdead, 0xbeef, 0xdead, 0xbeef};
    process.live[0] = foreign;
    process.guards.clear();
    std::array<Slot, 1> slots2{slot};
    check(!nhk::ensure_slots_live(slots2, host, order), "foreign edit fails the pass");
    check(process.live[0] == foreign, "foreign words not overwritten");
    check(!process.guards.empty(), "foreign edit reported as a guard event");

    // --- Uninstall: backend removal + prologue restored, idempotent. ---
    {
        process.live[0] = slot.patch_words;
        slot.registered = true;
        slot.patch_known = true;
        const int before_unhook = hook_unhook_calls;
        check(nhk::uninstall_slot(slot, host), "uninstall succeeds");
        check(hook_unhook_calls == before_unhook + 1, "backend unhook invoked");
        check(!slot.registered, "slot disarmed");
        check(process.live[0] == slot.original_words, "prologue restored on uninstall");
        check(nhk::uninstall_slot(slot, host), "second uninstall is a no-op");
        check(hook_unhook_calls == before_unhook + 1, "no redundant backend call");
    }

    // --- Uninstall refuses a slot that is no longer ours. ---
    {
        process.live[0] = Words{0xaa, 0xbb, 0xcc, 0xdd}; // Third-party words.
        slot.registered = true;
        slot.patch_known = true;
        const int before_unhook = hook_unhook_calls;
        check(!nhk::uninstall_slot(slot, host), "foreign slot is not uninstalled");
        check(hook_unhook_calls == before_unhook, "no backend call for a foreign slot");
        check(process.live[0] == Words{0xaa, 0xbb, 0xcc, 0xdd},
            "foreign words untouched by uninstall");
        check(slot.registered, "slot stays registered after a refused uninstall");
        process.live[0] = slot.patch_words;
    }

    // --- Uninstall without backend support refuses rather than leaving a
    // trampoline reachable behind an unpatched prologue. ---
    {
        nhk::InlineHookHost<kWords> no_unhook = host;
        no_unhook.hook_uninstall = nullptr;
        slot.registered = true;
        slot.patch_known = true;
        process.continuation[0] = reinterpret_cast<void *>(0xc0ffee);
        check(!nhk::uninstall_slot(slot, no_unhook),
            "uninstall refused without backend removal");
        check(slot.registered, "slot stays registered after refusal");
    }

    // --- restore_patch_words with a null continuation must refuse. ---
    Slot dead = slot;
    dead.patch_words = process.patch_for(0);
    dead.patch_known = true;
    dead.registered = false;
    void *null_continuation = nullptr;
    dead.original = &null_continuation;
    check(!nhk::restore_patch_words(dead, host), "re-arm without continuation refused");
    check(!process.guards.empty(), "null re-arm reported as a guard event");

    if (failures == 0) std::printf("NativeHookBankTest passed\n");
    return failures == 0 ? 0 : 1;
}
