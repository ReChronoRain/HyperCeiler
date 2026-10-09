/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Synthetic data image populated from the checked-in plan. No OEM code executes.
// Backend and authorization callbacks are fixtures, not Android/Hyos injection.
#include "targets/home/home_startup.h"
#include "targets/home/home_startup_plan.generated.h"
#include <algorithm>
#include <array>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

using namespace home_startup;
extern "C" uint64_t hc_layout_startup_epoch_gate;
namespace {
std::vector<unsigned char> image;
uintptr_t base;
void *originals[15]{};
std::array<unsigned char, 16> saved{};
unsigned hooks = 0, unhooks = 0;
bool published = false, saw_open_gate = false;
std::string scenario;
bool gate() { return (__atomic_load_n(&hc_layout_startup_epoch_gate, __ATOMIC_ACQUIRE) & 1) != 0; }
bool bounded(uintptr_t address, size_t size) {
    return address >= base && address - base <= image.size()
        && size <= image.size() - (address - base);
}
void continuation() {}
int hook(void *address, void *, void **original) {
    ++hooks;
    std::memcpy(saved.data(), address, saved.size());
    *original = reinterpret_cast<void *>(&continuation);
    const uint32_t patch[] = {0x58000050, 0xd61f0200, 0x12345678, 0x87654321};
    std::memcpy(address, patch, sizeof(patch));
    return scenario == "failed-install" ? -1 : 0;
}
int unhook(void *address) {
    ++unhooks;
    std::memcpy(address, saved.data(), saved.size());
    return 0;
}
bool authorized(void *) {
    if (scenario == "before-activation" && published) return false;
    if (scenario == "after-activation" && gate()) {
        // Deterministically reach the interval in which a gateway can have
        // entered, but execute() has not yet passed its final authorization.
        saw_open_gate = true;
        return false;
    }
    return true;
}
void synthetic_image() {
    size_t size = sizeof(plan::kIdentity);
    for (const auto &s : plan::kSegments) size = std::max(size, size_t(s.va) + s.memory_size);
    image.resize(size);
    base = reinterpret_cast<uintptr_t>(image.data());
    std::memcpy(image.data(), plan::kIdentity, sizeof(plan::kIdentity));
    for (const auto &g : plan::kGuards)
        std::memcpy(image.data() + g.va, plan::kGuardBytes + g.offset, g.size);
    for (const auto &w : plan::kWords) std::memcpy(image.data() + w.va, &w.original, 4);
    for (const auto &w : plan::kCapacity) std::memcpy(image.data() + w.va, &w.original, 4);
}
}
namespace home_startup::detail {
HookBinding binding(unsigned index) noexcept {
    return index < 15 ? HookBinding{reinterpret_cast<void *>(&continuation), &originals[index]}
        : HookBinding{};
}
void initialize_consumers(uintptr_t, uintptr_t, uintptr_t, uintptr_t, uintptr_t, const uint32_t *) noexcept {}
void publish_consumers(const home_layout::Config &, uint64_t) noexcept { published = true; }
void clear_consumers() noexcept {}
}
bool home_startup_test_read(uintptr_t address, void *out, size_t size) noexcept {
    if (!bounded(address, size)) return false;
    std::memcpy(out, reinterpret_cast<void *>(address), size);
    return true;
}
bool home_startup_test_write(uintptr_t address, const void *data, size_t size) noexcept {
    if (!bounded(address, size)) return false;
    std::memcpy(reinterpret_cast<void *>(address), data, size);
    return true;
}
int home_startup_test_protect(uintptr_t, size_t, int) noexcept { return 0; }
#define CHECK(condition, message) do { if (!(condition)) { std::fprintf(stderr, "FAIL: %s\n", message); return 1; } } while (false)
int main(int argc, char **argv) {
    CHECK(argc == 2, "scenario argument required");
    scenario = argv[1];
    synthetic_image();
    CHECK(prepare_for_child(), "owner preparation");
    home_layout::Config config;
    config.knobs[0].enabled = true;
    config.knobs[0].delta_dp = 12;
    if (scenario == "declined-grids") {
        config.knobs[0] = {};
        config.grid_enabled = true;
        config.cell_x = 6; config.cell_y = 7;
        config.tweaks.pad_enabled = config.tweaks.fold_enabled = true;
    }
    const Request request{base, hook, unhook, &config, authorized, nullptr};
    const auto result = execute(request);
    if (scenario == "declined-grids") {
        CHECK(result.declined == (PhoneGridDart | PhoneGridRust | PadGrid | FoldGrid), "grids remain declined");
        CHECK(result.applied == 0 && hooks == 0, "no grid installation");
    } else if (scenario == "normal-revoke") {
        CHECK(result.outcome == Outcome::Applied && gate(), "normal activation");
        request_revoke();
        CHECK(service_revoke().error == Error::Revoked, "normal revocation");
        CHECK(!gate() && unhooks == 0 && originals[0], "normal revocation retains continuation");
    } else {
        CHECK(result.outcome == Outcome::Declined && !gate(), "failure closes gateway");
        CHECK(hooks == 1, "one dynamic hook installed");
        if (scenario == "after-activation") {
            CHECK(result.error == Error::Revoked && saw_open_gate, "post-activation interval reached");
            CHECK(unhooks == 0 && originals[0], "post-activation must retain continuation");
            CHECK(execute(request).outcome == Outcome::Missed, "failed activation is not retried");
            CHECK(service_revoke().error == Error::Revoked && unhooks == 0 && originals[0], "maintenance retains continuation");
        } else {
            CHECK(scenario == "before-activation" || scenario == "failed-install", "known failure scenario");
            CHECK(unhooks == 1 && !originals[0], "pre-activation rollback unchanged");
            CHECK(std::memcmp(image.data() + plan::kHooks[0].va, saved.data(), saved.size()) == 0, "pre-activation original bytes restored");
        }
    }
    std::printf("PASS %s (synthetic image; no Android execution)\n", scenario.c_str());
    return 0;
}
