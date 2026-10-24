/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_layout_config.h"
#include "nativehook/hook_bank.h"
#include <algorithm>
#include <atomic>
#include <cassert>
#include <iostream>
#include <map>
#include <optional>

constexpr size_t kPatchWords = 4;
using Slot = nhk::InlineSlot<kPatchWords>;
using Words = nhk::SlotWords<kPatchWords>;
#include "worker-types.inc"

std::array<Slot, kSlotCount> g_slots{};
std::array<void *, kSlotCount> originals{};
std::map<uintptr_t, Words> memory;
std::optional<DartLibrary> g_dart;
std::string g_dart_container_path;
uint64_t g_dart_view_begin = 0, g_dart_view_end = 0;
std::atomic_int g_cell_x{0}, g_cell_y{0};
std::atomic_bool g_ready{false}, g_dart_ready{false};
bool g_animation_hook_armed = false, g_magic_hook_armed = false;
void *g_original_x = nullptr, *g_original_y = nullptr;
void *hc_layout_animation_ratio_original = nullptr, *hc_layout_magic_original = nullptr;
void cell_x_replacement() {}
void cell_y_replacement() {}
void hc_layout_animation_ratio_entry() {}
void hc_layout_magic_entry() {}
void replacement() {}

bool mapped = false, symbols_ready = true, provider_ready = true;
bool icons_enabled = false;
unsigned discoveries = 0, installations = 0;
home_layout::Config next_config, published;

namespace home_layout {
bool query_config(Config &out) {
    if (!provider_ready) return false;
    out = next_config;
    return true;
}
}
bool ensure_dart_library() {
    ++discoveries;
    if (!mapped) return false; // Image discovery/validation is an external boundary here.
    g_dart.emplace();
    g_dart->path = "/fixture/base.apk";
    g_dart->view_begin = 0x20000;
    g_dart->view_end = 0x30000;
    g_dart_container_path = g_dart->path;
    g_dart_view_begin = g_dart->view_begin;
    g_dart_view_end = g_dart->view_end;
    return true;
}
void sync_title_config(const home_layout::Config &config) { published = config; }
void publish_animation_rate(const home_layout::TweaksConfig &) {}
void push_tweaks(const home_layout::Config &) {}
bool bind_animation_consumer(Located &) { return false; }
bool bind_magic_consumer(Located &) { return false; }
bool sync_hotseat_capacity(bool) { return true; }
bool sync_widget_move(bool) { return true; }
bool sync_folder_auto_close(bool) { return true; }
bool sync_dock_titles(bool) { return true; }

void prepare(size_t index) {
    if (g_slots[index].address) return;
    uintptr_t address = 0x100000 + index * 0x100;
    Words words{0xa9bf79fd, 0x910003fd, 0xd503201f, 0xd503201f};
    g_slots[index] = {address, reinterpret_cast<void *>(replacement), &originals[index], {}, words};
    memory[address] = words;
}
bool bind_icon_render() {
    if (!g_dart || !symbols_ready) return false;
    prepare(kIconRenderSlotBase);
    prepare(kIconRenderSlotBase + 1);
    return true;
}
bool bind_desktop_icon_render() {
    if (!g_dart || !symbols_ready) return false;
    for (unsigned i = 2; i < 6; ++i) prepare(kIconRenderSlotBase + i);
    return true;
}
bool bind_title_color() { return false; }
bool bind_title_custom() { return false; }
bool bind_drawer_title() { return false; }
bool bind_desktop_title() { return false; }
bool bind_progress_icon_render() { return false; }
bool bind_folder_icon_scale() { return false; }
bool bind_folder_animation_scale() { return false; }
bool bind_folder_app_return_scale() { return false; }
bool bind_widget_scale() { return false; }
bool bind_title_hide() { return false; }
size_t arm_hooks(std::vector<size_t> &order) {
    size_t added = 0;
    for (size_t i = 0; i < g_slots.size(); ++i)
        if (g_slots[i].address && std::find(order.begin(), order.end(), i) == order.end()) {
            order.push_back(i);
            ++added;
        }
    return added;
}
size_t publish_hooks() {
    icons_enabled = true;
    for (unsigned i = 0; i < 6; ++i)
        icons_enabled &= nhk::slot_ready(g_slots[kIconRenderSlotBase + i]);
    return icons_enabled;
}
[[maybe_unused]] constexpr int ANDROID_LOG_INFO = 4;
[[maybe_unused]] constexpr const char *kTag = "test";
template<class... Args> int __android_log_print(int, const char *, const char *, Args...) { return 0; }

nhk::InlineHookHost<kPatchWords> host{
    .read_slot = [](const Slot &slot, Words &out) {
        auto found = memory.find(slot.address);
        if (found == memory.end()) return false;
        out = found->second;
        return true;
    },
    .write_words = [](uintptr_t address, const Words &words) { memory[address] = words; return true; },
    .hook_install = [](void *target, void *, void **original) {
        ++installations;
        auto address = reinterpret_cast<uintptr_t>(target);
        memory.at(address)[0] = 0x14000001;
        *original = reinterpret_cast<void *>(address + 0x100000);
        return 0;
    }
};

void refresh(std::optional<Located> &located, home_layout::Config &config, std::vector<size_t> &order) {
    const int iterations = 10;
    bool any_knob = false;
    auto sync_requested = [] { return false; };
#include "worker-refresh.inc"
    (void)any_knob;
    assert(nhk::ensure_slots_live(g_slots, host, order));
    publish_hooks(); // The shipping worker publishes after its verified health/repair pass.
}

void reset(int height, int width, int scale, bool early_rust) {
    g_slots = {};
    originals = {};
    memory.clear();
    g_dart.reset();
    g_dart_container_path.clear();
    mapped = false;
    symbols_ready = provider_ready = true;
    icons_enabled = false;
    discoveries = installations = 0;
    g_ready.store(false);
    g_dart_ready.store(false);
    g_animation_hook_armed = g_magic_hook_armed = false;
    next_config = {};
    next_config.back_gesture = {height, width};
    next_config.tweaks.icon_scale_enabled = true;
    next_config.tweaks.icon_scale_code = scale;
    published = next_config;
    if (early_rust) {
        prepare(kBackGestureSlot);
        prepare(kBackWindowSlot);
        const std::array<size_t, 2> back{kBackGestureSlot, kBackWindowSlot};
        assert(nhk::ensure_slots_live(g_slots, host, back));
        g_dart_ready.store(true); // Native readiness does not prove a loaded Dart image.
    }
}

int main() {
    unsigned cases = 0;
    for (int height : {10, 25, 60, 80, 100}) for (int width : {100, 200, 400})
        for (int scale : {0x66, 0x70, 0x78}) for (bool early_rust : {false, true}) {
            reset(height, width, scale, early_rust);
            auto config = next_config;
            std::optional<Located> located;
            if (early_rust) {
                located.emplace();
                located->container_path = "/fixture/base.apk";
                located->view_begin = 0x10000;
                located->view_end = 0x18000;
            }
            std::vector<size_t> order;
            arm_hooks(order);
            refresh(located, config, order);
            assert(!icons_enabled && !g_dart);
            mapped = true;
            provider_ready = false; // A temporary Binder failure must not stop image discovery.
            refresh(located, config, order);
            if (!icons_enabled || !g_dart) {
                std::cerr << "FAIL: late Dart image was never adopted after native back-region startup\n";
                return 1;
            }
            assert(located && located->dart_container_path == "/fixture/base.apk");
            assert(located->dart_view_begin == 0x20000 && located->dart_view_end == 0x30000);
            if (early_rust) {
                assert(located->view_begin == 0x10000 && located->view_end == 0x18000);
                assert(nhk::slot_ready(g_slots[kBackGestureSlot]) && nhk::slot_ready(g_slots[kBackWindowSlot]));
            }
            const auto count = installations;
            const auto scans = discoveries;
            provider_ready = true;
            for (int changed : {100, 10, 60, height}) {
                next_config.back_gesture.height = changed;
                refresh(located, config, order);
                assert(icons_enabled && published.tweaks.icon_scale_enabled);
                assert(published.tweaks.icon_scale_code == scale && published.back_gesture.height == changed);
            }
            assert(installations == count && discoveries == scans);
            ++cases;
        }
    reset(25, 200, 0x66, true);
    auto config = next_config;
    std::optional<Located> located;
    std::vector<size_t> order;
    mapped = true;
    symbols_ready = false;
    refresh(located, config, order);
    assert(g_dart && !icons_enabled);
    symbols_ready = true;
    refresh(located, config, order);
    assert(icons_enabled && discoveries == 1);
    std::cout << "late Dart / native back-region coexistence: " << cases
              << " cases PASS; unavailable Binder, delayed symbols, repeated height changes, "
                 "preserved native slots and no duplicate installs PASS\n";
}
