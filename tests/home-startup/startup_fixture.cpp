/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "startup_fixture.h"
#include "targets/home/home_startup_plan.generated.h"
#include "nativehook/elf_image.h"
#include <atomic>
#include <cstring>
#include <fstream>
#include <iterator>
#include <string>
#include <vector>
extern "C" uint64_t hc_layout_startup_epoch_gate;
namespace startup_fixture {
namespace {
std::vector<std::byte> image;
uintptr_t image_base = 0;
void *originals[15]{};
unsigned hooks = 0;
std::atomic<uint64_t> publish_mask{0};
void (*hook_event)(unsigned) = nullptr;
void (*publish_event)() = nullptr;
}
bool load(const char *path) {
    std::ifstream file(path, std::ios::binary); if (!file) return false;
    std::string raw(std::istreambuf_iterator<char>{file}, {});
    std::span<const std::byte> bytes(reinterpret_cast<const std::byte *>(raw.data()), raw.size());
    const auto segments = nhk::elf::parse_program_segments(bytes); if (!segments) return false;
    uint64_t extent = 0;
    for (const auto &s : *segments) if (s.type == 1) {
        if (s.vaddr + s.memsz > (1u << 28) || !nhk::in_image(bytes.size(), s.offset, s.filesz)) return false;
        extent = std::max(extent, s.vaddr + s.memsz);
    }
    image.assign(extent, std::byte{}); image_base = reinterpret_cast<uintptr_t>(image.data());
    for (const auto &s : *segments) if (s.type == 1)
        std::memcpy(image.data() + s.vaddr, bytes.data() + s.offset, s.filesz);
    return true;
}
uintptr_t base() noexcept { return image_base; }
bool contains(uintptr_t a) noexcept { return image_base && a >= image_base && a - image_base < image.size(); }
int hook(void *target, void *, void **original) {
    const auto address = reinterpret_cast<uintptr_t>(target);
    if (!contains(address) || address - image_base + 16 > image.size() || !original) return -1;
    ++hooks;
    if (hook_event) hook_event(hooks);
    *original = reinterpret_cast<void *>(uintptr_t(0xab000000 + hooks * 16));
    const uint32_t words[] = {0x58000050, 0xd61f0200, hooks, 0xf001cafe};
    std::memcpy(target, words, sizeof(words)); return 0;
}
int unhook(void *target) {
    const auto address = reinterpret_cast<uintptr_t>(target);
    if (!contains(address)) return -1;
    const uint32_t va = address - image_base;
    for (const auto &g : home_startup::plan::kGuards) if (g.va <= va && va + 16 <= g.va + g.size) {
        std::memcpy(target, home_startup::plan::kGuardBytes + g.offset + va - g.va, 16); return 0;
    }
    return -1;
}
bool all_words_original() noexcept {
    for (const auto &w : home_startup::plan::kWords) {
        uint32_t v = 0; std::memcpy(&v, image.data() + w.va, 4); if (v != w.original) return false;
    }
    for (const auto &w : home_startup::plan::kCapacity) {
        uint32_t v = 0; std::memcpy(&v, image.data() + w.va, 4); if (v != w.original) return false;
    }
    for (const auto &h : home_startup::plan::kHooks) for (const auto &g : home_startup::plan::kGuards)
        if (g.va <= h.va && h.va + 16 <= g.va + g.size
            && std::memcmp(image.data() + h.va, home_startup::plan::kGuardBytes + g.offset + h.va - g.va, 16)) return false;
    return true;
}
bool gate_enabled() noexcept { return __atomic_load_n(&hc_layout_startup_epoch_gate, __ATOMIC_ACQUIRE) & 1; }
unsigned hook_count() noexcept { return hooks; }
uint64_t published_mask() noexcept { return publish_mask.load(); }
void set_hook_event(void (*event)(unsigned)) noexcept { hook_event = event; }
void set_publish_event(void (*event)()) noexcept { publish_event = event; }
}
bool home_startup_test_read(uintptr_t at, void *out, size_t size) noexcept {
    if (!startup_fixture::contains(at) || size > startup_fixture::image.size() - (at - startup_fixture::base())) return false;
    std::memcpy(out, reinterpret_cast<void *>(at), size); return true;
}
bool home_startup_test_write(uintptr_t at, const void *from, size_t size) noexcept {
    if (!startup_fixture::contains(at) || size > startup_fixture::image.size() - (at - startup_fixture::base())) return false;
    std::memcpy(reinterpret_cast<void *>(at), from, size); return true;
}
int home_startup_test_protect(uintptr_t, size_t, int) noexcept { return 0; }
namespace home_startup::detail {
HookBinding binding(unsigned i) noexcept {
    return i < 15 ? HookBinding{reinterpret_cast<void *>(uintptr_t(0x1000 + i * 16)), &startup_fixture::originals[i]}
        : HookBinding{};
}
void initialize_consumers(uintptr_t, uintptr_t, uintptr_t, uintptr_t, uintptr_t, const uint32_t *) noexcept {}
void publish_consumers(const home_layout::Config &, uint64_t applied) noexcept {
    startup_fixture::publish_mask.store(applied);
    if (startup_fixture::publish_event) startup_fixture::publish_event();
}
void clear_consumers() noexcept { startup_fixture::publish_mask.store(0); }
}
