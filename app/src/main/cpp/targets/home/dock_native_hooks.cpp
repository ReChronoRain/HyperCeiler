/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "dock_native_layout.h"
#include "dock_native_resolver.h"
#include "dock_native_runtime.h"
#include "nativehook/got_hook_backend.h"
#include "nativehook/hook_bank.h"
#include "nativehook/inline_hook_backend.h"

#include <android/log.h>
#include <algorithm>
#include <array>
#include <atomic>
#include <cerrno>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <exception>
#include <fstream>
#include <fcntl.h>
#include <limits>
#include <mutex>
#include <optional>
#include <pthread.h>
#include <span>
#include <string>
#include <sys/mman.h>
#include <sys/uio.h>
#include <time.h>
#include <unistd.h>
#include <utility>
#include <vector>

bool prepare_dock_motion();
void activate_dock_motion();
void deactivate_dock_motion();
void run_dock_motion();

extern "C" {
#define DEFINE_DOCK_MOTION_BANK(bank) \
    uint32_t dock_params_class_id_##bank = 0; \
    uint32_t dock_double_class_id_##bank = 0; \
    int32_t dock_tagged_header_offset_##bank = 0; \
    uint32_t dock_class_id_shift_##bank = 0; \
    uint32_t dock_class_id_mask_##bank = 0; \
    uint32_t dock_alpha_offset_##bank = 0; \
    uint32_t dock_scale_offset_##bank = 0; \
    uint32_t dock_surface_offset_##bank = 0; \
    uint32_t dock_recents_offset_##bank = 0; \
    uint32_t dock_double_value_offset_##bank = 0; \
    uint32_t dock_false_from_null_##bank = 0; \
    uint32_t dock_unlock_state_widget_offset_##bank = 0; \
    uint32_t dock_unlock_widget_cell_offset_##bank = 0; \
    uint32_t dock_unlock_cell_container_offset_##bank = 0; \
    uintptr_t dock_unlock_call_return_##bank = 0; \
    int64_t dock_unlock_hotseat_container_0_##bank = 0; \
    int64_t dock_unlock_hotseat_container_1_##bank = 0; \
    int64_t dock_unlock_hotseat_container_2_##bank = 0; \
    int64_t dock_unlock_hotseat_container_3_##bank = 0; \
    int64_t dock_unlock_hotseat_container_4_##bank = 0; \
    void *dock_motion_scale_original_##bank = nullptr; \
    void *dock_motion_anim_original_##bank = nullptr; \
    void *dock_motion_set_original_##bank = nullptr; \
    void *dock_motion_unlock_scale_original_##bank = nullptr;
DOCK_MOTION_BANKS(DEFINE_DOCK_MOTION_BANK)
#undef DEFINE_DOCK_MOTION_BANK

// Published by the replacement trampolines in dock_native_motion.cpp. The health
// worker reads them so a silent hook loss is visible in the log instead of looking
// exactly like an idle desktop.
extern std::atomic<uint64_t> dock_motion_entry_hits;
extern std::atomic<uint64_t> dock_motion_publish_hits;
extern std::atomic<uint64_t> dock_auto_aim_entry_hits;
extern std::atomic<uint64_t> dock_auto_aim_publish_hits;
extern std::atomic<uint64_t> dock_auto_aim_receiver_bad;
extern std::atomic<uint64_t> dock_auto_aim_widget_bad;
extern std::atomic<uint64_t> dock_auto_aim_cell_bad;
extern std::atomic<uint64_t> dock_auto_aim_container_miss;
extern std::atomic<uint64_t> dock_motion_active_callbacks;
extern std::atomic<uint32_t> dock_motion_subscribed;
}

namespace {
constexpr char kTag[] = "HyperCeiler.DockNative";
constexpr size_t kTargetCount = 3;
constexpr size_t kPatchBytes = 4 * sizeof(uint32_t);
constexpr unsigned kInventorySettlingScans = 3;
constexpr unsigned kScanCooldownChecks = 4;

// Defined after the maintenance loop; the loop calls it once per pass.
void maybe_install_madvise_guard();
// Defined with the page-lifetime policy; used by the slot host as protect_range.
bool protect_patch_range(uintptr_t address, size_t bytes);
// Bounded retries for the page-lifetime guard before it reports unavailable.
constexpr uint32_t kMadviseGuardMaxAttempts = 8;
using Hook = int (*)(void *, void *, void **);
using Unhook = int (*)(void *);
using PatchWords = std::array<uint32_t, kPatchBytes / sizeof(uint32_t)>;
using TargetAddresses = std::array<uintptr_t, kTargetCount>;
using TargetSources = std::array<dock_motion::CodeSource, kTargetCount>;

Hook hook_function = nullptr;
Unhook unhook_function = nullptr;
std::atomic_bool started{false};
std::atomic_bool worker_alive{false};
std::atomic_bool runtime_ready{false};
std::mutex hook_mutex;
std::vector<dock_motion::ExecutableMapping> last_inventory;
unsigned settling_scans = 0;
unsigned scan_cooldown = 0;
bool capacity_reported = false;

/**
 * Counts every time a slot was refused or disarmed because its continuation pointer was
 * missing, i.e. every crash that the tail-branch guard prevented. Surfaced in the pipeline
 * heartbeat so a guarded event stays visible after logcat has rotated the one-shot line away.
 */
std::atomic<uint64_t> dock_motion_guard_events{0};

struct ResolvedInstance {
    dock_motion::Resolution resolution;
    std::vector<dock_motion::ExecutableMapping> mappings;
    TargetSources sources;
    std::array<PatchWords, kTargetCount> original_words;
    // Contract-shaped resolver output (nativehook/resolver.h): the recents points and optional
    // unlock projection point plus their evidence. `targets[i].address`
    // is verified against `original_words` before the bank is built, so the
    // contract cannot drift from the runtime's view.
    std::array<nhk::ResolvedTarget, kTargetCount> targets;
    std::optional<dock_motion::CodeSource> unlock_source;
    std::optional<PatchWords> unlock_original_words;
    std::optional<nhk::ResolvedTarget> unlock_target;
    nhk::ResolverEvidence evidence;
};

// Slot lifecycle lives in the shared NativeHookRuntime (nativehook/hook_bank.h);
// the dock only names its three targets and publishes the per-bank layout.
using HookSlot = nhk::InlineSlot<kPatchBytes / sizeof(uint32_t)>;

struct HookBank {
    size_t index;
    dock_motion::Resolution resolution;
    std::vector<dock_motion::ExecutableMapping> mappings;
    std::array<HookSlot, kTargetCount> slots;
    std::array<HookSlot, 1> unlock_slots;
    bool unlock_available = false;
};

std::vector<HookBank> hook_banks;

struct BankSymbols {
    uint32_t *params_class_id;
    uint32_t *double_class_id;
    int32_t *tagged_header_offset;
    uint32_t *class_id_shift;
    uint32_t *class_id_mask;
    uint32_t *alpha_offset;
    uint32_t *scale_offset;
    uint32_t *surface_offset;
    uint32_t *recents_offset;
    uint32_t *double_value_offset;
    uint32_t *false_from_null;
    uint32_t *unlock_state_widget_offset;
    uint32_t *unlock_widget_cell_offset;
    uint32_t *unlock_cell_container_offset;
    uintptr_t *unlock_call_return;
    std::array<int64_t *, 5> unlock_hotseat_containers;
    std::array<void *, kTargetCount> replacements;
    std::array<void **, kTargetCount> originals;
    void *unlock_replacement;
    void **unlock_original;
};

#define DOCK_BANK_SYMBOLS(bank) BankSymbols{ \
    &dock_params_class_id_##bank, &dock_double_class_id_##bank, \
    &dock_tagged_header_offset_##bank, &dock_class_id_shift_##bank, \
    &dock_class_id_mask_##bank, &dock_alpha_offset_##bank, \
    &dock_scale_offset_##bank, &dock_surface_offset_##bank, \
    &dock_recents_offset_##bank, &dock_double_value_offset_##bank, \
    &dock_false_from_null_##bank, \
    &dock_unlock_state_widget_offset_##bank, &dock_unlock_widget_cell_offset_##bank, \
    &dock_unlock_cell_container_offset_##bank, \
    &dock_unlock_call_return_##bank, \
    {&dock_unlock_hotseat_container_0_##bank, &dock_unlock_hotseat_container_1_##bank, \
     &dock_unlock_hotseat_container_2_##bank, &dock_unlock_hotseat_container_3_##bank, \
     &dock_unlock_hotseat_container_4_##bank}, \
    {reinterpret_cast<void *>(dock_motion_scale_entry_##bank), \
     reinterpret_cast<void *>(dock_motion_anim_entry_##bank), \
     reinterpret_cast<void *>(dock_motion_set_entry_##bank)}, \
    {&dock_motion_scale_original_##bank, &dock_motion_anim_original_##bank, \
     &dock_motion_set_original_##bank}, \
    reinterpret_cast<void *>(dock_motion_unlock_scale_entry_##bank), \
    &dock_motion_unlock_scale_original_##bank}

const std::array<BankSymbols, 16> kBankSymbols{{
    DOCK_BANK_SYMBOLS(0), DOCK_BANK_SYMBOLS(1), DOCK_BANK_SYMBOLS(2),
    DOCK_BANK_SYMBOLS(3), DOCK_BANK_SYMBOLS(4), DOCK_BANK_SYMBOLS(5),
    DOCK_BANK_SYMBOLS(6), DOCK_BANK_SYMBOLS(7),
    DOCK_BANK_SYMBOLS(8), DOCK_BANK_SYMBOLS(9), DOCK_BANK_SYMBOLS(10),
    DOCK_BANK_SYMBOLS(11), DOCK_BANK_SYMBOLS(12), DOCK_BANK_SYMBOLS(13),
    DOCK_BANK_SYMBOLS(14), DOCK_BANK_SYMBOLS(15),
}};
#undef DOCK_BANK_SYMBOLS

TargetAddresses addresses(const dock_motion::Resolution &resolution) {
    return {resolution.scale, resolution.animate, resolution.set};
}

TargetAddresses addresses(const HookBank &bank) {
    TargetAddresses result{};
    for (size_t i = 0; i < result.size(); ++i) result[i] = bank.slots[i].address;
    return result;
}

TargetSources sources(const HookBank &bank) {
    TargetSources result{};
    for (size_t i = 0; i < result.size(); ++i) result[i] = bank.slots[i].source;
    return result;
}

bool safe_read(uintptr_t address, std::span<std::byte> destination) {
    if (destination.empty() || address > UINTPTR_MAX - destination.size()) return false;
    size_t completed = 0;
    while (completed < destination.size()) {
        iovec local{destination.data() + completed, destination.size() - completed};
        iovec remote{reinterpret_cast<void *>(address + completed), destination.size() - completed};
        const ssize_t count = process_vm_readv(getpid(), &local, 1, &remote, 1, 0);
        if (count < 0 && errno == EINTR) continue;
        if (count <= 0) {
            // Some Android process policies reject process_vm_readv even for self.
            // pread remains fault-reporting: never replace this with an unchecked
            // memcpy from a runtime mapping which may disappear concurrently.
            const int memory = open("/proc/self/mem", O_RDONLY | O_CLOEXEC);
            if (memory < 0) return false;
            bool success = true;
            while (completed < destination.size()) {
                const uintptr_t position = address + completed;
                if (position > static_cast<uintptr_t>(std::numeric_limits<off_t>::max())) {
                    success = false;
                    break;
                }
                const ssize_t copied = pread(memory, destination.data() + completed,
                    destination.size() - completed, static_cast<off_t>(position));
                if (copied < 0 && errno == EINTR) continue;
                if (copied <= 0) { success = false; break; }
                completed += static_cast<size_t>(copied);
            }
            close(memory);
            return success;
        }
        if (static_cast<size_t>(count) > destination.size() - completed) return false;
        completed += static_cast<size_t>(count);
    }
    return true;
}

// True when every entry of `subset` is still present, verbatim, in `set`. Image
// discovery wants exactly this conservative meaning: a VMA split or merge makes
// the subset test fail, which only forfeits a match - it never asserts that
// live code disappeared.
bool mapping_subset(const std::vector<dock_motion::ExecutableMapping> &subset,
    const std::vector<dock_motion::ExecutableMapping> &set) {
    return std::ranges::all_of(subset, [&](const auto &mapping) {
        return std::ranges::find(set, mapping) != set.end();
    });
}

// ---------------------------------------------------------------------------
// AOT image discovery
//
// HYOS does not extract its launcher libraries: `libapp.so` lives uncompressed
// and page aligned inside the application APK, so the extracted library path does
// not exist and `/proc/self/maps` reports the APK path for every library that
// shares it. A name test therefore finds nothing at all, and a naive "any
// executable mapping of the APK" test would splice several unrelated libraries
// into one inventory.
//
// Ownership is recovered in three bounded layers:
//   1. a bare `libapp.so` mapping (older packaging, or an extracted image);
//   2. the stored `lib/<abi>/libapp.so` entry of a ZIP container, resolved from
//      the central directory and rebased onto the same page granularity the
//      kernel used, so the executable ranges of one image can be attributed;
//   3. a page-aligned ELF header probe, kept as a fallback for a container that
//      is not a readable ZIP. That layer cannot tell sibling images apart, so it
//      is used only when layers 1 and 2 produced nothing; the resolver's
//      unique-match requirement still fails closed on a mixed inventory.
// ---------------------------------------------------------------------------

/** Discovery layers, recorded per file so each probe runs at most once. */
struct ContainerProbe {
    ContainerProbe(std::string path_value, uint64_t inode_value)
        : path(std::move(path_value)), inode(inode_value) {}

    std::string path;
    uint64_t inode = 0;
    bool zip_done = false;
    bool embedded_done = false;
    std::optional<dock_motion::LibappContainer> zip;
    std::optional<dock_motion::LibappContainer> embedded;
};

constexpr size_t kContainerProbesPerScan = 8;
constexpr size_t kContainerProbeLimit = 2048;
constexpr uint64_t kContainerMinBytes = 64U * 1024U;
constexpr size_t kElfProbeStride = 4096;
constexpr unsigned kElfProbeSlots = 4;
constexpr size_t kElfProbeBytes = 64 + 128 * 56;

std::vector<ContainerProbe> container_probes;

/** Deduplication slot for a container probe, keyed by path and inode. */
size_t probe_slot(const std::string &path, uint64_t inode) {
    for (size_t index = 0; index < container_probes.size(); ++index) {
        const auto &probe = container_probes[index];
        if (probe.path == path && probe.inode == inode) return index;
    }
    if (container_probes.size() >= kContainerProbeLimit) return container_probes.size();
    container_probes.push_back(ContainerProbe{path, inode});
    return container_probes.size() - 1;
}

/**
 * Fallback discovery for a container that is not a readable ZIP: find a page
 * aligned ELF header in a readable, non-writable mapping and bound the image from
 * its own segment table.
 */
std::optional<dock_motion::LibappContainer> probe_embedded_image(
    const dock_motion::FileMapping &mapping) {
    const uint64_t length = mapping.end - mapping.begin;
    for (unsigned slot = 0; slot < kElfProbeSlots; ++slot) {
        const uint64_t displacement = slot * kElfProbeStride;
        if (displacement >= length) break;
        const size_t wanted = static_cast<size_t>(
            std::min<uint64_t>(kElfProbeBytes, length - displacement));
        if (wanted < 64) break;
        std::vector<std::byte> header(wanted);
        if (!safe_read(mapping.begin + displacement, std::span(header))) continue;
        const auto image = dock_motion::parse_embedded_image(
            std::span(header), mapping.file_offset + displacement);
        if (!image) continue;
        return dock_motion::LibappContainer{
            mapping.path, image->view_begin, image->view_end};
    }
    return std::nullopt;
}

bool container_is_new(const std::vector<dock_motion::LibappContainer> &result,
    const dock_motion::LibappContainer &candidate) {
    return std::ranges::none_of(result, [&](const auto &known) {
        return known.path == candidate.path && known.view_begin == candidate.view_begin
            && known.view_end == candidate.view_end;
    });
}

void append_container(std::vector<dock_motion::LibappContainer> &result,
    const std::optional<dock_motion::LibappContainer> &candidate) {
    if (candidate && container_is_new(result, *candidate)) result.push_back(*candidate);
}

/** Candidates ordered by mapped bytes so a bounded budget still reaches the AOT image. */
std::vector<std::pair<std::string, uint64_t>> probe_candidates(
    const std::vector<dock_motion::FileMapping> &entries, bool embedded_layer) {
    struct Candidate {
        uint64_t inode = 0;
        uint64_t bytes = 0;
        std::string path;
    };
    std::vector<Candidate> candidates;
    for (const auto &entry : entries) {
        if (entry.inode == 0 || entry.writable) continue;
        const std::string_view path = dock_motion::strip_deleted(entry.path);
        if (path.empty() || path.front() != '/') continue;
        const std::string owned(path);
        const auto found = std::ranges::find_if(candidates, [&](const Candidate &candidate) {
            return candidate.inode == entry.inode || candidate.path == owned;
        });
        const uint64_t bytes = entry.end - entry.begin;
        if (found == candidates.end()) candidates.push_back({entry.inode, bytes, owned});
        else found->bytes += bytes;
    }
    std::ranges::sort(candidates, [](const Candidate &left, const Candidate &right) {
        return left.bytes > right.bytes;
    });
    std::vector<std::pair<std::string, uint64_t>> result;
    size_t budget = kContainerProbesPerScan;
    for (const auto &candidate : candidates) {
        if (candidate.bytes < kContainerMinBytes || budget == 0) break;
        const size_t slot = probe_slot(candidate.path, candidate.inode);
        if (slot >= container_probes.size()) break;
        const bool done = embedded_layer
            ? container_probes[slot].embedded_done : container_probes[slot].zip_done;
        if (done) continue;
        --budget;
        result.emplace_back(candidate.path, candidate.inode);
    }
    return result;
}

/** One-shot report per distinct signature, bounded so a log cannot grow forever. */
bool claim_report(const std::string &signature) {
    constexpr size_t kMaxReports = 96;
    static std::mutex mutex;
    static std::vector<std::string> reported;
    std::lock_guard lock(mutex);
    if (std::ranges::find(reported, signature) != reported.end()) return false;
    if (reported.size() >= kMaxReports) return false;
    reported.push_back(signature);
    return true;
}

/** Log the inventory once per distinct container set so a failure is diagnosable. */
void report_containers(const std::vector<dock_motion::LibappContainer> &containers,
    const char *source, size_t entries) {
    std::string signature = source;
    for (const auto &container : containers) {
        signature += '|' + container.path + ':' + std::to_string(container.view_begin)
            + ':' + std::to_string(container.view_end);
    }
    if (!claim_report(signature)) return;
    for (const auto &container : containers) {
        __android_log_print(ANDROID_LOG_INFO, kTag,
            "motion container source=%s entries=%zu path=%s view=0x%llx bytes=0x%llx",
            source, entries, container.path.c_str(),
            static_cast<unsigned long long>(container.view_begin),
            static_cast<unsigned long long>(container.view_end - container.view_begin));
    }
    if (containers.empty()) {
        __android_log_print(ANDROID_LOG_WARN, kTag,
            "motion container absent: no AOT image located across %zu file mappings",
            entries);
        return;
    }
    __android_log_print(ANDROID_LOG_INFO, kTag,
        "motion container ready source=%s count=%zu", source, containers.size());
}

/**
 * Log the container view once per distinct outcome, next to the file mappings that
 * actually back it and the executable ranges selected from them. A container that
 * is discovered but selects nothing looks identical to no container at all
 * from the outside, and that pair of facts is what tells the two apart.
 */
void report_image(const std::vector<dock_motion::LibappContainer> &containers,
    const std::vector<dock_motion::FileMapping> &entries,
    const std::vector<dock_motion::ExecutableMapping> &mappings) {
    std::string signature = "image";
    for (const auto &container : containers) {
        signature += '|' + container.path + ':' + std::to_string(container.view_begin)
            + ':' + std::to_string(container.view_end);
    }
    signature += '#';
    for (const auto &mapping : mappings) {
        signature += std::to_string(mapping.begin) + ':' + std::to_string(mapping.file_offset)
            + ',';
    }
    if (!claim_report(signature)) return;

    constexpr size_t kMaxContainers = 3;
    constexpr size_t kMaxEntriesPerContainer = 8;
    for (size_t index = 0; index < containers.size() && index < kMaxContainers; ++index) {
        const auto &container = containers[index];
        size_t shown = 0;
        for (const auto &entry : entries) {
            if (shown >= kMaxEntriesPerContainer) break;
            if (dock_motion::strip_deleted(entry.path) != container.path) continue;
            ++shown;
            __android_log_print(ANDROID_LOG_INFO, kTag,
                "motion image map path=%s off=0x%llx bytes=0x%llx exec=%d write=%d in_view=%d",
                container.path.c_str(),
                static_cast<unsigned long long>(entry.file_offset),
                static_cast<unsigned long long>(entry.end - entry.begin),
                entry.executable ? 1 : 0, entry.writable ? 1 : 0,
                (entry.file_offset >= container.view_begin
                    && entry.file_offset < container.view_end) ? 1 : 0);
        }
    }
    for (const auto &mapping : mappings) {
        __android_log_print(ANDROID_LOG_INFO, kTag,
            "motion image owner begin=0x%llx end=0x%llx offset=0x%llx inode=%llu",
            static_cast<unsigned long long>(mapping.begin),
            static_cast<unsigned long long>(mapping.end),
            static_cast<unsigned long long>(mapping.file_offset),
            static_cast<unsigned long long>(mapping.inode));
    }
}

/** Resolve the AOT image's container, recording each layer so it runs at most once. */
bool collect_containers(const std::vector<dock_motion::FileMapping> &entries,
    std::vector<dock_motion::LibappContainer> &containers, const char *&source) {
    // Layer 1: the AOT image is its own file.
    for (const auto &entry : entries) {
        const std::string_view path = dock_motion::strip_deleted(entry.path);
        if (!dock_motion::libapp_path(path)) continue;
        append_container(containers,
            dock_motion::LibappContainer{std::string(path), 0, UINT64_MAX});
    }
    if (!containers.empty()) {
        source = "bare";
        return true;
    }

    // Layer 2: the AOT image is a stored entry of an application container.
    for (const auto &probe : container_probes) {
        if (probe.zip_done) append_container(containers, probe.zip);
    }
    if (containers.empty()) {
        for (const auto &candidate : probe_candidates(entries, false)) {
            const size_t slot = probe_slot(candidate.first, candidate.second);
            if (slot >= container_probes.size()) break;
            auto &probe = container_probes[slot];
            probe.zip_done = true;
            const auto entry = dock_motion::zip_stored_libapp(candidate.first);
            if (!entry) continue;
            probe.zip = dock_motion::LibappContainer{candidate.first,
                page_down(entry->first), page_up(entry->first + entry->second)};
        }
        for (const auto &probe : container_probes) {
            if (probe.zip_done) append_container(containers, probe.zip);
        }
    }
    if (!containers.empty()) {
        source = "zip";
        return true;
    }

    // Layer 3: container readable but not a ZIP; probe embedded ELF headers.
    for (const auto &probe : container_probes) {
        if (probe.embedded_done) append_container(containers, probe.embedded);
    }
    if (containers.empty()) {
        for (const auto &candidate : probe_candidates(entries, true)) {
            const size_t slot = probe_slot(candidate.first, candidate.second);
            if (slot >= container_probes.size()) break;
            auto &probe = container_probes[slot];
            probe.embedded_done = true;
            for (const auto &entry : entries) {
                if (entry.inode != candidate.second || entry.writable) continue;
                if (dock_motion::strip_deleted(entry.path) != candidate.first) continue;
                auto found = probe_embedded_image(entry);
                if (!found) continue;
                found->path = candidate.first;
                probe.embedded = found;
                break;
            }
        }
        for (const auto &probe : container_probes) {
            if (probe.embedded_done) append_container(containers, probe.embedded);
        }
    }
    source = containers.empty() ? "none" : "embedded";
    return !containers.empty();
}

std::optional<std::vector<dock_motion::ExecutableMapping>> current_mappings() {
    std::ifstream maps("/proc/self/maps");
    if (!maps) return {};
    const auto entries = dock_motion::parse_file_mappings(maps);

    std::vector<dock_motion::LibappContainer> containers;
    const char *source = "none";
    collect_containers(entries, containers, source);
    report_containers(containers, source, entries.size());
    auto mappings = dock_motion::executable_libapp_mappings(entries, containers);
    report_image(containers, entries, mappings);
    return mappings;
}

/** A silent resolver looks like a working one; report the first failing stage. */
void report_resolution(const std::vector<dock_motion::ExecutableMapping> &mappings,
    const char *stage) {
    std::string signature = std::string("stage=") + stage;
    uint64_t bytes = 0;
    for (const auto &mapping : mappings) {
        signature += '|' + std::to_string(mapping.begin) + ':' + std::to_string(mapping.file_offset);
        bytes += mapping.end - mapping.begin;
    }
    if (!claim_report(signature)) return;
    __android_log_print(ANDROID_LOG_WARN, kTag,
        "motion resolve stalled stage=%s ranges=%zu bytes=0x%llx", stage, mappings.size(),
        static_cast<unsigned long long>(bytes));
}
bool stable_read(const TargetAddresses &locations, const TargetSources &expected,
    std::array<PatchWords, kTargetCount> &words) {
    const auto before = current_mappings();
    if (!before || dock_motion::mapping_state(*before, locations, expected, kPatchBytes)
            != dock_motion::MappingState::same) return false;
    for (size_t i = 0; i < locations.size(); ++i) {
        if (!safe_read(locations[i], std::as_writable_bytes(std::span(&words[i], 1)))) return false;
    }
    const auto after = current_mappings();
    return after && dock_motion::mapping_state(*after, locations, expected, kPatchBytes)
            == dock_motion::MappingState::same;
}

bool stable_read(uintptr_t address, const dock_motion::CodeSource &source,
    PatchWords &words) {
    const std::array<uintptr_t, 1> location{address};
    const std::array<dock_motion::CodeSource, 1> expected{source};
    const auto before = current_mappings();
    if (!before || dock_motion::mapping_state(*before, location, expected, kPatchBytes)
            != dock_motion::MappingState::same) return false;
    if (!safe_read(address, std::as_writable_bytes(std::span(&words, 1)))) return false;
    const auto after = current_mappings();
    return after && dock_motion::mapping_state(*after, location, expected, kPatchBytes)
            == dock_motion::MappingState::same;
}

struct OwnedRanges {
    std::vector<std::vector<uint32_t>> storage;
    std::vector<dock_motion::CodeRange> ranges;
};

std::optional<OwnedRanges> copy_generation_code(
    const std::vector<dock_motion::ExecutableMapping> &mappings) {
    if (mappings.empty()) return {};
    OwnedRanges owned;
    owned.storage.reserve(mappings.size());
    owned.ranges.reserve(mappings.size());
    for (const auto &mapping : mappings) {
        const size_t length = mapping.end - mapping.begin;
        owned.storage.emplace_back(length / sizeof(uint32_t));
        auto &copy = owned.storage.back();
        if (!safe_read(mapping.begin, std::as_writable_bytes(std::span(copy)))) return {};
        owned.ranges.push_back({mapping.begin, std::span<const uint32_t>(copy)});
    }
    const auto after = current_mappings();
    if (!after || !mapping_subset(mappings, *after)) return {};
    return owned;
}

bool distinct_targets(const TargetAddresses &locations) {
    for (size_t left = 0; left < locations.size(); ++left) {
        if (locations[left] == 0 || locations[left] % alignof(uint32_t) != 0) return false;
        for (size_t right = left + 1; right < locations.size(); ++right) {
            if (locations[left] == locations[right]) return false;
        }
    }
    return true;
}

std::optional<ResolvedInstance> resolve_generation(
    const std::vector<dock_motion::ExecutableMapping> &mappings) {
    try {
        auto owned = copy_generation_code(mappings);
        if (!owned) {
            report_resolution(mappings, "copy");
            return {};
        }
        // The generation's load bias turns the resolver's contract-shaped RVAs
        // back into runtime addresses (they must round-trip onto the same
        // mapping, which the source check below proves).
        uint64_t load_bias = 0;
        if (const auto key = dock_motion::generation_key(mappings.front())) {
            load_bias = key->load_bias;
        }
        const auto targets = dock_motion::resolve_hook_targets(owned->ranges, load_bias);
        if (!targets) {
            report_resolution(mappings, "resolve");
            return {};
        }
        const auto &resolution = targets->resolution;
        const auto locations = addresses(resolution);
        if (!distinct_targets(locations)) {
            report_resolution(mappings, "distinct");
            return {};
        }
        TargetSources target_sources{};
        if (!dock_motion::sources_for(mappings, locations, kPatchBytes, target_sources)) {
            report_resolution(mappings, "sources");
            return {};
        }
        std::array<PatchWords, kTargetCount> originals{};
        if (!stable_read(locations, target_sources, originals)) {
            report_resolution(mappings, "stable");
            return {};
        }
        for (size_t i = 0; i < locations.size(); ++i) {
            const auto copied = dock_motion::at(owned->ranges, locations[i], originals[i].size());
            if (copied.size() != originals[i].size()
                || !std::equal(copied.begin(), copied.end(), originals[i].begin())) {
                report_resolution(mappings, "words");
                return {};
            }
            // The contract's original words must describe the same prologue the
            // validated live read produced; anything else is a resolver bug.
            const auto &declared = targets->targets[i].original_words;
            if (declared.size() != originals[i].size()
                || !std::equal(declared.begin(), declared.end(), originals[i].begin())) {
                report_resolution(mappings, "contract");
                return {};
            }
        }
        std::optional<dock_motion::CodeSource> unlock_source;
        std::optional<PatchWords> unlock_original;
        if (resolution.unlock) {
            if (!targets->unlock_target
                || std::ranges::find(locations, resolution.unlock->scale) != locations.end()
                || resolution.unlock->scale == 0
                || resolution.unlock->scale % alignof(uint32_t) != 0) {
                report_resolution(mappings, "unlock-distinct");
                return {};
            }
            const std::array<uintptr_t, 1> unlock_location{resolution.unlock->scale};
            std::array<dock_motion::CodeSource, 1> unlock_sources{};
            if (!dock_motion::sources_for(
                    mappings, unlock_location, kPatchBytes, unlock_sources)) {
                report_resolution(mappings, "unlock-source");
                return {};
            }
            std::array<PatchWords, 1> unlock_words{};
            const auto before = current_mappings();
            if (!before || dock_motion::mapping_state(*before, unlock_location,
                    unlock_sources, kPatchBytes) != dock_motion::MappingState::same
                || !safe_read(unlock_location[0],
                    std::as_writable_bytes(std::span(&unlock_words[0], 1)))) {
                report_resolution(mappings, "unlock-stable");
                return {};
            }
            const auto after = current_mappings();
            if (!after || dock_motion::mapping_state(*after, unlock_location,
                    unlock_sources, kPatchBytes) != dock_motion::MappingState::same) {
                report_resolution(mappings, "unlock-stable");
                return {};
            }
            const auto copied = dock_motion::at(
                owned->ranges, unlock_location[0], unlock_words[0].size());
            if (copied.size() != unlock_words[0].size()
                || !std::equal(copied.begin(), copied.end(), unlock_words[0].begin())
                || targets->unlock_target->original_words.size() != unlock_words[0].size()
                || !std::equal(targets->unlock_target->original_words.begin(),
                    targets->unlock_target->original_words.end(), unlock_words[0].begin())) {
                report_resolution(mappings, "unlock-words");
                return {};
            }
            unlock_source = unlock_sources[0];
            unlock_original = unlock_words[0];
        }
        return ResolvedInstance{resolution, mappings, target_sources, originals,
            targets->targets, unlock_source, unlock_original, targets->unlock_target,
            targets->evidence};
    } catch (...) {
        report_resolution(mappings, "exception");
        return {};
    }
}

void publish_layout(size_t index, const dock_motion::Layout &layout) {
    const auto &symbols = kBankSymbols[index];
    *symbols.params_class_id = layout.params_class_id;
    *symbols.double_class_id = layout.double_class_id;
    *symbols.tagged_header_offset = layout.tagged_header_offset;
    *symbols.class_id_shift = layout.class_id_shift;
    *symbols.class_id_mask = layout.class_id_mask;
    *symbols.alpha_offset = layout.alpha_offset;
    *symbols.scale_offset = layout.scale_offset;
    *symbols.surface_offset = layout.surface_offset;
    *symbols.recents_offset = layout.recents_offset;
    *symbols.double_value_offset = layout.double_value_offset;
    *symbols.false_from_null = layout.false_from_null;
}

void publish_unlock_layout(size_t index, const dock_motion::UnlockResolution &unlock) {
    const auto &symbols = kBankSymbols[index];
    const auto &layout = unlock.layout;
    *symbols.unlock_state_widget_offset = layout.state_widget_offset;
    *symbols.unlock_widget_cell_offset = layout.widget_cell_offset;
    *symbols.unlock_cell_container_offset = layout.cell_container_offset;
    *symbols.unlock_call_return = unlock.call_return;
    for (size_t item = 0; item < layout.hotseat_containers.size(); ++item) {
        *symbols.unlock_hotseat_containers[item] = layout.hotseat_containers[item];
    }
}

bool same_instance(const HookBank &bank, const ResolvedInstance &instance) {
    if (!dock_motion::same_resolution(bank.resolution, instance.resolution)) return false;
    for (size_t i = 0; i < bank.slots.size(); ++i) {
        if (!(bank.slots[i].source == instance.sources[i])) return false;
    }
    if (bank.unlock_available) {
        if (!instance.unlock_source
            || !(bank.unlock_slots[0].source == *instance.unlock_source)) return false;
    }
    return true;
}

HookBank make_bank(size_t index, const ResolvedInstance &instance) {
    const auto &symbols = kBankSymbols[index];
    HookBank bank{index, instance.resolution, instance.mappings, {{
        {instance.resolution.scale, symbols.replacements[0], symbols.originals[0],
            instance.sources[0], instance.original_words[0]},
        {instance.resolution.animate, symbols.replacements[1], symbols.originals[1],
            instance.sources[1], instance.original_words[1]},
        {instance.resolution.set, symbols.replacements[2], symbols.originals[2],
            instance.sources[2], instance.original_words[2]},
    }}, {}, false};
    if (instance.resolution.unlock && instance.unlock_source && instance.unlock_original_words) {
        bank.unlock_slots[0] = {instance.resolution.unlock->scale,
            symbols.unlock_replacement, symbols.unlock_original, *instance.unlock_source,
            *instance.unlock_original_words};
        bank.unlock_available = true;
    }
    return bank;
}

/** Write raw bytes back into a code page, restoring its original protection afterwards. */
bool write_code_words(uintptr_t address, const PatchWords &words) {
    const uintptr_t page = address - address % host_page_size();
    const size_t length = static_cast<size_t>(address + kPatchBytes - page);
    void *base = reinterpret_cast<void *>(page);
    if (mprotect(base, length, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) return false;
    std::memcpy(reinterpret_cast<void *>(address), words.data(), kPatchBytes);
    __builtin___clear_cache(reinterpret_cast<char *>(address),
        reinterpret_cast<char *>(address + kPatchBytes));
    return mprotect(base, length, PROT_READ | PROT_EXEC) == 0;
}

// ---------------------------------------------------------------------------
// Slot lifecycle through the shared NativeHookRuntime.
//
// install_slot / restore_patch_words / ensure_slots_live / health moved into
// nativehook/hook_bank.h so Rust and C targets drive the same state machine.
// The dock contributes only the memory accessors, the logging format and the
// guard counter below; the invariants (continuation never null, foreign edits
// untouched, re-arm only while the generation holds) are the runtime's.
// ---------------------------------------------------------------------------

nhk::InlineHookHost<kPatchBytes / sizeof(uint32_t)> &slot_host() {
    static nhk::InlineHookHost<kPatchBytes / sizeof(uint32_t)> host = [] {
        nhk::InlineHookHost<kPatchBytes / sizeof(uint32_t)> value;
        value.read_slot = [](const HookSlot &slot, PatchWords &words) {
            return stable_read(slot.address, slot.source, words);
        };
        value.write_words = [](uintptr_t address, const PatchWords &words) {
            return write_code_words(address, words);
        };
        value.hook_install = [](void *target, void *replacement, void **original) {
            const auto &api = nhk::LsposedInlineBackend::instance().entries();
            if (api.hook_func != nullptr) return api.hook_func(target, replacement, original);
            return hook_function != nullptr ? hook_function(target, replacement, original) : -1;
        };
        value.hook_uninstall = [](void *target) {
            const auto &api = nhk::LsposedInlineBackend::instance().entries();
            if (api.unhook_func != nullptr) return api.unhook_func(target);
            return unhook_function != nullptr ? unhook_function(target) : -1;
        };
        value.protect_range = [](uintptr_t address, size_t bytes) {
            return protect_patch_range(address, bytes);
        };
        value.on_guard = [](const nhk::HookEvent &event) {
            dock_motion_guard_events.fetch_add(1, std::memory_order_relaxed);
            __android_log_print(ANDROID_LOG_ERROR, kTag, "%s", event.reason);
        };
        value.on_info = [](const nhk::HookEvent &event) {
            __android_log_print(ANDROID_LOG_INFO, kTag, "%s", event.reason);
        };
        return value;
    }();
    return host;
}

constexpr std::array<size_t, kTargetCount> kInstallOrder{1, 2, 0};
constexpr std::array<size_t, 1> kUnlockInstallOrder{0};

constexpr uint64_t kPipelineReportNs = 10000000000ULL;

bool monotonic_ns(uint64_t &value) {
    timespec now{};
    if (clock_gettime(CLOCK_MONOTONIC, &now) != 0) return false;
    value = static_cast<uint64_t>(now.tv_sec) * 1000000000ULL + now.tv_nsec;
    return true;
}

/**
 * Low-rate ground truth for the whole native chain: whether the Dart hooks still execute
 * (entry), whether they publish a packed sample (publish), whether a replacement is on the
 * stack right now (callbacks) and whether the health pass believes the bank is live. Both
 * a silent hook loss and a healthy idle desktop produce no other log line, so without this
 * the two are indistinguishable. Must be called with hook_mutex held.
 */
void report_pipeline(bool healthy) {
    static uint64_t next_ns = 0;
    static uint64_t entry = 0;
    static uint64_t publish = 0;
    static uint64_t callbacks = 0;
    static uint64_t guard = 0;
    static uint64_t auto_aim_entry = 0;
    static uint64_t auto_aim_publish = 0;
    static uint64_t aim_receiver_bad = 0;
    static uint64_t aim_widget_bad = 0;
    static uint64_t aim_cell_bad = 0;
    static uint64_t aim_container_miss = 0;
    uint64_t now = 0;
    if (!monotonic_ns(now)) return;
    if (next_ns == 0) next_ns = now + kPipelineReportNs;
    if (now < next_ns) return;
    next_ns = now + kPipelineReportNs;
    const uint64_t current_entry = dock_motion_entry_hits.load(std::memory_order_relaxed);
    const uint64_t current_publish = dock_motion_publish_hits.load(std::memory_order_relaxed);
    const uint64_t current_callbacks = dock_motion_active_callbacks.load(std::memory_order_relaxed);
    const uint64_t current_guard = dock_motion_guard_events.load(std::memory_order_relaxed);
    const uint64_t current_auto_aim_entry =
        dock_auto_aim_entry_hits.load(std::memory_order_relaxed);
    const uint64_t current_auto_aim_publish =
        dock_auto_aim_publish_hits.load(std::memory_order_relaxed);
    const uint64_t current_aim_receiver_bad =
        dock_auto_aim_receiver_bad.load(std::memory_order_relaxed);
    const uint64_t current_aim_widget_bad =
        dock_auto_aim_widget_bad.load(std::memory_order_relaxed);
    const uint64_t current_aim_cell_bad =
        dock_auto_aim_cell_bad.load(std::memory_order_relaxed);
    const uint64_t current_aim_container_miss =
        dock_auto_aim_container_miss.load(std::memory_order_relaxed);
    const uint32_t current_subscribed = dock_motion_subscribed.load(std::memory_order_relaxed);
    __android_log_print(ANDROID_LOG_INFO, kTag,
        "motion pipeline subscribed=%u healthy=%d banks=%zu entry=%llu(+%llu) publish=%llu(+%llu) aimEntry=%llu(+%llu) aimPublish=%llu(+%llu) aimReject rx=%llu(+%llu) widget=%llu(+%llu) cell=%llu(+%llu) container=%llu(+%llu) callbacks=%llu(+%llu) guard=%llu(+%llu)",
        current_subscribed, healthy ? 1 : 0, hook_banks.size(),
        static_cast<unsigned long long>(current_entry),
        static_cast<unsigned long long>(current_entry - entry),
        static_cast<unsigned long long>(current_publish),
        static_cast<unsigned long long>(current_publish - publish),
        static_cast<unsigned long long>(current_auto_aim_entry),
        static_cast<unsigned long long>(current_auto_aim_entry - auto_aim_entry),
        static_cast<unsigned long long>(current_auto_aim_publish),
        static_cast<unsigned long long>(current_auto_aim_publish - auto_aim_publish),
        static_cast<unsigned long long>(current_aim_receiver_bad),
        static_cast<unsigned long long>(current_aim_receiver_bad - aim_receiver_bad),
        static_cast<unsigned long long>(current_aim_widget_bad),
        static_cast<unsigned long long>(current_aim_widget_bad - aim_widget_bad),
        static_cast<unsigned long long>(current_aim_cell_bad),
        static_cast<unsigned long long>(current_aim_cell_bad - aim_cell_bad),
        static_cast<unsigned long long>(current_aim_container_miss),
        static_cast<unsigned long long>(current_aim_container_miss - aim_container_miss),
        static_cast<unsigned long long>(current_callbacks),
        static_cast<unsigned long long>(current_callbacks - callbacks),
        static_cast<unsigned long long>(current_guard),
        static_cast<unsigned long long>(current_guard - guard));
    entry = current_entry;
    publish = current_publish;
    auto_aim_entry = current_auto_aim_entry;
    auto_aim_publish = current_auto_aim_publish;
    aim_receiver_bad = current_aim_receiver_bad;
    aim_widget_bad = current_aim_widget_bad;
    aim_cell_bad = current_aim_cell_bad;
    aim_container_miss = current_aim_container_miss;
    callbacks = current_callbacks;
    guard = current_guard;
}

bool bank_healthy(HookBank &bank) {
    // Delegates to the shared runtime; the batched stable read keeps the healthy
    // steady state at one inventory round trip instead of one per slot.
    return nhk::slots_healthy<kTargetCount, kPatchBytes / sizeof(uint32_t)>(
        bank.slots,
        [](const std::array<uintptr_t, kTargetCount> &locations,
            const TargetSources &expected,
            std::array<PatchWords, kTargetCount> &observed) {
            return stable_read(locations, expected, observed);
        });
}

bool unlock_healthy(HookBank &bank) {
    if (!bank.unlock_available) return true;
    return nhk::slots_healthy<1, kPatchBytes / sizeof(uint32_t)>(
        bank.unlock_slots,
        [](const std::array<uintptr_t, 1> &locations,
            const std::array<dock_motion::CodeSource, 1> &expected,
            std::array<PatchWords, 1> &observed) {
            return stable_read(locations[0], expected[0], observed[0]);
        });
}

bool add_instance(const ResolvedInstance &instance) {
    if (std::ranges::any_of(hook_banks,
            [&](const auto &bank) { return same_instance(bank, instance); })) return false;
    size_t index = hook_banks.size();
    if (index >= kBankSymbols.size()) {
        // Fail closed. Every bank stays bound to the generation that installed it for
        // the whole process lifetime (the contract in dock_native_layout.h), so a
        // callback that entered an older generation can always finish safely.
        //
        // Recycling was considered and deliberately rejected. Judging bank liveness by
        // exact mapping-inventory identity is a bet: the kernel splits and merges VMAs
        // (an mprotect inside a segment splits the /proc/self/maps entry - including
        // splits caused by this module's own inline-patch protection flips), and a
        // split or merge makes a *live* generation fail the exact-match test. Recycling
        // that bank republishes its layout symbols under a new generation while the old
        // replacement is still executing and still reading them: a memory-safety
        // hazard, not a performance trade-off. The scenario needs 16 generations in one
        // process before it can arise, so refusing is the cheap side of the trade.
        if (!capacity_reported) {
            capacity_reported = true;
            __android_log_print(ANDROID_LOG_WARN, kTag,
                "motion runtime bank capacity reached; refusing new generations (fail closed)");
        }
        return false;
    }
    // Publish the layout before installing: the replacement reads these symbols.
    publish_layout(index, instance.resolution.layout);
    if (instance.resolution.unlock) {
        publish_unlock_layout(index, *instance.resolution.unlock);
    }
    HookBank replacement = make_bank(index, instance);
    hook_banks.push_back(std::move(replacement));
    auto &bank = hook_banks[index];
    const bool installed = ensure_slots_live(bank.slots, slot_host(), kInstallOrder)
        && bank_healthy(bank);
    const bool unlock_installed = !bank.unlock_available
        || (ensure_slots_live(bank.unlock_slots, slot_host(), kUnlockInstallOrder)
            && unlock_healthy(bank));
    __android_log_print(installed ? ANDROID_LOG_INFO : ANDROID_LOG_WARN, kTag,
        "motion runtime bank=%zu paramsCID=%u doubleCID=%u install=%s unlock=%s",
        index, instance.resolution.layout.params_class_id,
        instance.resolution.layout.double_class_id, installed ? "complete" : "partial",
        !bank.unlock_available ? "unresolved"
            : unlock_installed ? "live-projection" : "partial");
    return installed;
}

bool maintain_dock_motion_hooks_impl(bool force) {
    std::lock_guard lock(hook_mutex);
    const auto inventory = current_mappings();
    if (!inventory) {
        report_pipeline(false);
        deactivate_dock_motion();
        return false;
    }
    const bool inventory_changed = *inventory != last_inventory;
    if (inventory_changed) {
        last_inventory = *inventory;
        settling_scans = kInventorySettlingScans;
        scan_cooldown = 0;
    }

    // Page-lifetime policy first: the guard must already be installed before a
    // patch range is registered, otherwise a discard can land in the window
    // between registration and guard installation.
    maybe_install_madvise_guard();

    bool healthy = false;
    for (auto &bank : hook_banks) {
        const auto state = dock_motion::mapping_state(*inventory, addresses(bank),
            sources(bank), kPatchBytes);
        if (state != dock_motion::MappingState::same) continue;
        // bank_healthy() is the only per-tick cost while the generation is intact. The
        // repair probe (a per-slot stable read) runs only after that verification fails,
        // so a lost patch is healed without taxing the healthy steady state.
        bool recents_healthy = bank_healthy(bank);
        if (!recents_healthy
            && ensure_slots_live(bank.slots, slot_host(), kInstallOrder)) {
            recents_healthy = bank_healthy(bank);
        }
        if (recents_healthy) healthy = true;
        // AUTO_AIM is optional and never takes the established recents channel down. Repair its
        // exact-scale setter independently whenever this generation still owns the code mapping.
        if (bank.unlock_available && !unlock_healthy(bank)) {
            (void)ensure_slots_live(
                bank.unlock_slots, slot_host(), kUnlockInstallOrder);
        }
    }

    bool scan = force || inventory_changed || !healthy;
    if (!scan && settling_scans != 0) {
        if (scan_cooldown == 0) scan = true;
        else --scan_cooldown;
    }
    if (scan) {
        const auto generations = dock_motion::runtime_generations(*inventory);
        for (const auto &generation : generations) {
            const auto instance = resolve_generation(generation);
            if (instance && add_instance(*instance)) healthy = true;
        }
        if (generations.empty()) {
            // A silent resolver is indistinguishable from a working one on the
            // device, so report each distinct executable inventory once.
            static std::string reported_inventory;
            std::string signature;
            for (const auto &mapping : *inventory) {
                signature += std::to_string(mapping.inode) + ':'
                    + std::to_string(mapping.begin) + ':'
                    + std::to_string(mapping.file_offset) + ';';
            }
            if (signature != reported_inventory) {
                reported_inventory = signature;
                __android_log_print(ANDROID_LOG_WARN, kTag,
                    "motion resolver idle: executable=%zu generations=0, no hook bank",
                    inventory->size());
            }
        }
        if (settling_scans != 0) --settling_scans;
        scan_cooldown = healthy ? kScanCooldownChecks : kScanCooldownChecks * 2;
    }

    // A scan can install a bank after the first health pass.
    if (!healthy) {
        for (auto &bank : hook_banks) {
            if (bank_healthy(bank)) {
                healthy = true;
                break;
            }
        }
    }
    report_pipeline(healthy);
    if (healthy) activate_dock_motion();
    else deactivate_dock_motion();
    return healthy;
}

// ---------------------------------------------------------------------------
// Optional page-lifetime policy (nativehook/page_guard.h + got_hook_backend.h).
//
// The HyperOS Flutter runtime discards its own code pages with MADV_DONTNEED,
// and a discard that lands on one of our trampoline pages or GOT slots takes
// the hook with it. Hooking that one library's `madvise` import and splitting
// discard requests around registered pages closes the race. This decision
// belongs to the desktop feature - the runtime itself stays policy-free - and
// a failed install only costs the extra protection, never the channel.
// ---------------------------------------------------------------------------

// Guard state machine:
//   0 = pending      - not installed yet, retry on later passes
//   1 = installed    - replacement is live and its forward target is published
//   2 = unavailable  - gave up (budget/parse/import), no slot was left behind
//   3 = installing   - one pass is working on it
//   4 = residual     - a slot may still hold the replacement; the forward target
//                      is kept published and no further install is attempted
std::atomic<uint32_t> madvise_guard_state{0};
std::atomic<uint32_t> madvise_guard_attempts{0};
// Published before the GOT slot can reach the replacement; the hook reads it
// with acquire semantics. Never cleared while a replacement may still be
// reachable - clearing it would turn the runtime's own madvise into a failure.
std::atomic<void *> g_real_madvise{nullptr};
// Install record, so the guard can be re-verified against the live slots
// instead of being assumed healthy for process life.
std::vector<nhk::GotHook<>> g_madvise_hooks;

// Explicit budget: a snapshot larger than this is refused outright rather than
// silently truncated. An image whose tables sit above the first few megabytes
// (a real system runtime has PT_DYNAMIC at vaddr 0x1101638) needs the whole
// range, so a "prefix" constant cannot work.
constexpr uint64_t kGuardImageBudgetBytes = 64ULL * 1024ULL * 1024ULL;
// Large enough for the ELF header plus any legal program header table
// (kMaxProgramHeaders * 56 + 64 = 7232). A fixed 4 KiB window would refuse a
// well-formed image with more than 72 headers.
constexpr size_t kGuardHeaderBytes = 16 * 1024;

int hyperceiler_guarded_madvise(void *address, size_t length, int advice) {
    const auto real = reinterpret_cast<int (*)(void *, size_t, int)>(
        g_real_madvise.load(std::memory_order_acquire));
    if (real == nullptr) {
        // The replacement must never be reachable without its forward target;
        // report an error rather than acting as an unguarded pass-through.
        return -1;
    }
    return nhk::guarded_madvise(address, length, advice, real);
}

bool flutter_runtime_path(std::string_view path) {
    constexpr std::string_view kFlutterRuntimeLibrary = "libhyper_os_flutter.so";
    path = dock_motion::strip_deleted(path);
    return path == kFlutterRuntimeLibrary || path.ends_with("/libhyper_os_flutter.so");
}

/**
 * Which file the runtime image is mapped from, plus - when it is a stored entry
 * of a container - the page window that entry occupies inside it.
 *
 * `extractNativeLibs=false` is why the window exists: the kernel maps the
 * library straight out of the APK and reports the *container* path for every
 * library it backs, so `base.apk` is shared by the whole `lib/<abi>/` directory
 * and a path alone can no longer identify an image. A bare file keeps the
 * trivial window [0, UINT64_MAX) and its offsets already are image offsets.
 */
struct RuntimeImageTarget {
    std::string path;
    uint64_t view_begin = 0;
    uint64_t view_end = UINT64_MAX;
};

struct RuntimeContainerProbe {
    std::string path;
    uint64_t inode = 0;
    std::optional<std::pair<uint64_t, uint64_t>> entry;
};

constexpr size_t kRuntimeContainerProbeLimit = 8;
// Only ever touched with `hook_mutex` held: the guard install and maintenance
// passes are both driven from the single maintenance worker. Probing an APK
// reads its tail and central directory, so the answer is cached for the process
// lifetime instead of once per pass.
std::vector<RuntimeContainerProbe> runtime_container_probes;

/** Stored `libhyper_os_flutter.so` entry of a container, probed at most once. */
std::optional<std::pair<uint64_t, uint64_t>> runtime_container_entry(
    const std::string &path, uint64_t inode) {
    for (const auto &probe : runtime_container_probes) {
        if (probe.path == path && probe.inode == inode) return probe.entry;
    }
    if (runtime_container_probes.size() >= kRuntimeContainerProbeLimit) return std::nullopt;
    RuntimeContainerProbe probe;
    probe.path = path;
    probe.inode = inode;
    probe.entry = nhk::zip_stored_entry(path, "libhyper_os_flutter.so");
    const auto entry = probe.entry;
    runtime_container_probes.push_back(std::move(probe));
    return entry;
}

/**
 * Find the runtime image in one inventory.
 *
 * Layer 1 is the library mapped under its own name, which is also everything a
 * firmware with `extractNativeLibs=true` produces. Layer 2 is the container
 * mapping described by [RuntimeImageTarget]; it is tried second because a
 * transient preload copy of the same library can be mapped at the same time,
 * and the file that carries the library's own name is the better answer when
 * both exist. Returns nothing when neither is present.
 *
 * Measured on device (Android 17, `extractNativeLibs=false`): on a cold launcher
 * start the vendor's preload copy of `/system_ext/lib64/libhyper_os_flutter.so`
 * was mapped for a few milliseconds only - one pass saw its four segments, the
 * scan seven milliseconds later did not, and the engine's own mappings showed up
 * about four seconds in, backed by stored `base.apk` entries (window 0x710000).
 * Layer 1 alone would have installed nothing on that start; Layer 2 is what
 * makes the guard reachable at all. Layer 1 still leads, because when the copy
 * does persist it is a real engine image - and a record bound to a copy that
 * disappears afterwards is repaired by the generation check, which is exactly
 * what it exists for.
 */
std::optional<RuntimeImageTarget> identify_runtime_image(
    const std::vector<nhk::FileMapping> &all) {
    for (const auto &mapping : all) {
        if (!flutter_runtime_path(mapping.path)) continue;
        return RuntimeImageTarget{
            std::string(nhk::strip_deleted(mapping.path)), 0, UINT64_MAX};
    }
    for (const auto &mapping : all) {
        if (mapping.writable || mapping.path.empty() || mapping.path.front() != '/') continue;
        const std::string_view path = nhk::strip_deleted(mapping.path);
        if (!path.ends_with(".apk")) continue;
        const auto entry = runtime_container_entry(std::string(path), mapping.inode);
        if (!entry) continue;
        const uint64_t begin = page_down(entry->first);
        const uint64_t end = page_up(entry->first + entry->second);
        if (mapping.file_offset < begin || mapping.file_offset >= end) continue;
        return RuntimeImageTarget{std::string(path), begin, end};
    }
    return std::nullopt;
}

/**
 * One target image's own mapping inventory, or nothing when it could not be
 * determined at all.
 *
 * Offsets are rebased on the view start, so `begin - file_offset` is the load
 * bias for every mapping - the identity the GOT records are verified against
 * (`begin - file_offset == load_base`). An unrebased container offset would make
 * that comparison fail for every mapping but the first.
 *
 * The two failure modes must stay distinguishable, because the maintenance pass
 * draws opposite conclusions from them:
 *
 * - an **empty** list means the image genuinely has no mapping right now, so
 *   the recorded generation is gone and the record may be dropped;
 * - **nothing** means "unknown" - `/proc/self/maps` was unreadable or the scan
 *   hit its budget. Treating that as an unload would discard a record whose GOT
 *   slots may still hold the replacement, and the next install would then
 *   refuse the symbol (expected == replacement) with the healthy record already
 *   lost. Unrecoverable by construction, so it must never happen.
 */
std::optional<std::vector<nhk::ExecutableMapping>> runtime_mappings(
    const std::vector<nhk::FileMapping> &mappings,
    const RuntimeImageTarget &target) {
    std::vector<nhk::ExecutableMapping> result;
    for (const auto &mapping : mappings) {
        if (nhk::strip_deleted(mapping.path) != target.path) continue;
        if (mapping.file_offset < target.view_begin
            || mapping.file_offset >= target.view_end) continue;
        if (result.size() >= nhk::kMaxExecutableMappings) return {};
        result.push_back({mapping.begin, mapping.end,
            mapping.file_offset - target.view_begin, mapping.device_major,
            mapping.device_minor, mapping.inode});
    }
    std::ranges::sort(result, {}, &nhk::ExecutableMapping::begin);
    return result;
}

// The image the recorded GOT slots belong to, captured at install time. Kept so
// the maintenance pass verifies the record against the *same* identity it was
// created from - re-identifying would follow the rules to a different answer if
// a preload copy of the library appeared meanwhile. Cleared together with the
// hook record; never cleared while a record still exists.
std::optional<RuntimeImageTarget> g_madvise_target;

/**
 * Drop the guarded-slot record together with the generation it belongs to.
 *
 * The two are one fact split in half: a record whose identity is gone can no
 * longer validate its slots, and an identity with no record has nothing left to
 * validate. Releasing them apart is exactly how a stale record survives a remap
 * and then "proves" stability against the wrong image.
 */
void release_madvise_record() {
    g_madvise_hooks.clear();
    g_madvise_target.reset();
}

bool read_pointer_safely(uintptr_t slot, const void *&value) {
    return safe_read(slot,
        std::span<std::byte>(reinterpret_cast<std::byte *>(&value), sizeof(value)));
}

/**
 * Register the pages a patch range will touch with the page-lifetime policy.
 * Both the first and the last byte can fall on different pages; every page in
 * between must be registered, and a full table refuses the range.
 */
bool protect_patch_range(uintptr_t address, size_t bytes) {
    if (bytes == 0 || nhk::add_overflows(address, bytes)) return false;
    const uint64_t page = host_page_size();
    const uintptr_t first = static_cast<uintptr_t>(page_down(address));
    const uintptr_t last = static_cast<uintptr_t>(page_down(address + bytes - 1));
    for (uintptr_t current = first; current <= last; current += page) {
        if (!nhk::add_protected_page(current)) return false;
        if (current > UINTPTR_MAX - page) break; // Defensive: no wrap-around loop.
    }
    return true;
}

/**
 * Re-verify the guard against the live slots, per image generation.
 *
 * The distinction that matters: an unreadable slot can mean "someone else took
 * it" or "this generation is gone". Only the target's own mapping identity can
 * tell them apart, and only the second case may be followed by a fresh install
 * on the new generation. Log lines are deduplicated per outcome - a maintenance
 * pass runs four times a second and must not flood the buffer.
 */
void maintain_madvise_guard() {
    if (g_madvise_hooks.empty()) return;
    // 0 none, 1 remapped, 2 foreign, 3 rearm, 4 residual cleared,
    // 5 residual kept, 6 inventory unavailable.
    static uint32_t logged_outcome = 0;
    std::ifstream maps("/proc/self/maps");
    std::optional<std::vector<nhk::ExecutableMapping>> current;
    if (maps && g_madvise_target) {
        current = runtime_mappings(nhk::parse_file_mappings(maps), *g_madvise_target);
    }
    if (!current) {
        // "Unknown" is not "gone": an unreadable or over-budget scan proves
        // nothing about the library. Postpone the pass and keep the record - the
        // slots may still hold the replacement, and dropping the record would
        // make that state unrecoverable.
        if (logged_outcome != 6) {
            logged_outcome = 6;
            __android_log_print(ANDROID_LOG_INFO, kTag,
                "madvise guard inventory unavailable; keeping the record and retrying");
        }
        return;
    }
    bool healthy = false;
    bool rearmable = false;
    bool foreign = false;
    bool generation_gone = false;
    bool residual_present = false;
    for (const auto &hook : g_madvise_hooks) {
        if (!hook.identity_present(*current)) {
            generation_gone = true;
            continue;
        }
        if (hook.partial) {
            residual_present = true;
            continue;
        }
        switch (hook.health([](uintptr_t slot, const void *&value) {
                    return read_pointer_safely(slot, value);
                })) {
            case nhk::GotHook<>::State::healthy: healthy = true; break;
            case nhk::GotHook<>::State::rearmable: rearmable = true; break;
            case nhk::GotHook<>::State::foreign: foreign = true; break;
            case nhk::GotHook<>::State::empty: break;
        }
    }

    if (generation_gone) {
        // The library was remapped or unloaded: the recorded addresses belong to
        // a generation that no longer exists. Never write them, keep the forward
        // target published (a call already inside the replacement still returns
        // through it), and let the next install pass discover the new one.
        if (logged_outcome != 1) {
            logged_outcome = 1;
            __android_log_print(ANDROID_LOG_INFO, kTag,
                "madvise guard generation changed; rediscovering the runtime image");
        }
        release_madvise_record();
        madvise_guard_state.store(0U, std::memory_order_release);
        return;
    }
    if (residual_present) {
        // A residue may only be dropped once every recorded slot is back to the
        // original pointer *and* every page it captured protection for holds the
        // bits captured before the first write. The pointer test alone is not
        // enough: the write path stores the value and only then restores the
        // page, so a page can be left writable while its slot already reads as
        // the original. Releasing the record there would let the next install
        // capture RW as "the original" and leave the RELRO page permanently
        // writable.
        bool slots_restored = true;
        bool protections_ok = true;
        for (const auto &hook : g_madvise_hooks) {
            if (!hook.partial) continue;
            for (size_t i = 0; i < hook.slot_count; ++i) {
                const void *value = nullptr;
                if (!read_pointer_safely(hook.slots[i], value) || value != hook.expected) {
                    slots_restored = false;
                }
            }
            if (!nhk::protections_restored(hook)) protections_ok = false;
        }
        if (slots_restored && !protections_ok) {
            // The pointers are home; only the permission is wrong. Re-apply the
            // captured bits - never re-sample them, since the current ones are
            // exactly what cannot be trusted.
            for (const auto &hook : g_madvise_hooks) {
                if (hook.partial) (void)nhk::reapply_protections(hook);
            }
            protections_ok = true;
            for (const auto &hook : g_madvise_hooks) {
                if (hook.partial && !nhk::protections_restored(hook)) {
                    protections_ok = false;
                }
            }
        }
        if (slots_restored && protections_ok) {
            if (logged_outcome != 4) {
                logged_outcome = 4;
                __android_log_print(ANDROID_LOG_INFO, kTag,
                    "madvise guard residue cleared; slots and page protections are original");
            }
            release_madvise_record();
            madvise_guard_state.store(0U, std::memory_order_release);
        } else if (logged_outcome != 5) {
            // Keep the record: it is the only thing that still knows the true
            // original protection, so it must survive until it can be applied.
            logged_outcome = 5;
            __android_log_print(ANDROID_LOG_WARN, kTag,
                "madvise guard residue retained; slots_restored=%d protections_restored=%d",
                slots_restored ? 1 : 0, protections_ok ? 1 : 0);
        }
        return;
    }
    if (foreign) {
        // The generation is still mapped but the slot is not ours any more:
        // leave it alone, keep the forward target, and stop trying.
        if (logged_outcome != 2) {
            logged_outcome = 2;
            __android_log_print(ANDROID_LOG_WARN, kTag,
                "madvise guard slot taken over by a third party; leaving it alone");
        }
        madvise_guard_state.store(4U, std::memory_order_release);
        return;
    }
    if (rearmable && !healthy) {
        // Every slot is back to the original pointer (page refill, remap):
        // a fresh install pass is safe again.
        if (logged_outcome != 3) {
            logged_outcome = 3;
            __android_log_print(ANDROID_LOG_INFO, kTag,
                "madvise guard slots returned to original; re-arming");
        }
        release_madvise_record();
        madvise_guard_state.store(0U, std::memory_order_release);
    }
}

/**
 * Report why the guard is still pending, once per distinct reason.
 *
 * The pass runs four times a second, so a log per attempt would flood the
 * buffer - that is why the retry budget exists. But the budget alone made the
 * whole subsystem unobservable: "the runtime is not loaded yet" and "the path
 * never matches the inventory" both leave state 0 behind with no attempt
 * consumed and no line in the log, and the two call for opposite responses.
 * Each reason is therefore reported exactly once, keyed separately from the
 * rendered detail so a changing count (say, the inventory size) cannot turn one
 * state into a stream of lines.
 */
void report_guard_pending(const std::string &key, const std::string &detail) {
    if (!claim_report("guard|" + key)) return;
    __android_log_print(ANDROID_LOG_INFO, kTag,
        "madvise guard pending: %s", detail.c_str());
}

void maybe_install_madvise_guard() {
    // The generation check walks /proc/self/maps, which is not free: run it on
    // every fourth maintenance pass (~1 s) instead of four times a second.
    static std::atomic<uint32_t> maintain_tick{0};
    if (maintain_tick.fetch_add(1, std::memory_order_relaxed) % 4U == 0U) {
        maintain_madvise_guard();
    }
    const uint32_t state = madvise_guard_state.load(std::memory_order_acquire);
    if (state == 1U || state == 2U || state == 4U) return; // Settled (or residual).
    uint32_t expected = 0;
    if (!madvise_guard_state.compare_exchange_strong(expected, 3U,
            std::memory_order_acq_rel)) {
        return; // Another pass is installing right now.
    }
    // Two kinds of "not installed", and the budget only makes sense for one.
    //
    // `give_up` is for a *settled* answer: the library is here and the guard
    // cannot be built from it (its image is over budget, its ELF does not
    // parse). Retrying those forever would burn a pass every 250 ms for nothing.
    //
    // `stay_pending` is for "not yet": the image is mid-load, so its mappings
    // are still appearing one by one and a snapshot taken now is incomplete. The
    // APK-backed case made this concrete - the first pass found the stored entry
    // but the executable segment was not mapped yet, and a budget spent on "not
    // yet" ends in a permanent give-up seconds before the library is usable.
    // These cost no attempt; each reason still logs once.
    const auto give_up = [&](const char *reason) {
        report_guard_pending(reason, reason);
        const uint32_t attempt = madvise_guard_attempts.fetch_add(1) + 1;
        if (attempt >= kMadviseGuardMaxAttempts) {
            __android_log_print(ANDROID_LOG_WARN, kTag,
                "madvise guard unavailable after %u attempts: %s (fail-open)",
                attempt, reason);
            madvise_guard_state.store(2U, std::memory_order_release);
        } else {
            // Settled but failed this time; another pass may still succeed.
            madvise_guard_state.store(0U, std::memory_order_release);
        }
    };
    const auto stay_pending = [&](const char *reason) {
        report_guard_pending(reason, reason);
        madvise_guard_state.store(0U, std::memory_order_release);
    };

    std::ifstream maps("/proc/self/maps");
    if (!maps) {
        give_up("cannot read /proc/self/maps");
        return;
    }
    const auto all = nhk::parse_file_mappings(maps);
    // Which image do the recorded GOT slots belong to? With
    // `extractNativeLibs=false` the kernel reports the APK path for the runtime's
    // mappings, so matching on "libhyper_os_flutter.so" alone only ever finds the
    // vendor's transient preload copy - which the spawner dlcloses moments later.
    const auto target = identify_runtime_image(all);
    if (!target) {
        // Not loaded yet: stay pending and spend no attempt. The runtime may be
        // dlopen'd seconds after the launcher starts, and a budget consumed by
        // "not yet" would end in a permanent give-up before it ever appears.
        report_guard_pending("runtime-not-mapped",
            "runtime not mapped across " + std::to_string(all.size())
                + " file mapping(s)");
        madvise_guard_state.store(0U, std::memory_order_release);
        return;
    }
    // Stable-snapshot discipline, measured on the *target's own* mappings: the
    // Dart image's inventory says nothing about this library, so comparing it
    // would validate the wrong thing entirely. An absent inventory ("unreadable")
    // and an empty one ("no mapping left") are both unusable here, and the check
    // below refuses the pass instead of guessing which one it was.
    const auto flutter_before = runtime_mappings(all, *target);
    // Stage 1: the ELF header and program header table. They live in the first
    // mapping of the file, so read that one's head - not "the first executable
    // mapping", whose first byte is not the header at all.
    const nhk::FileMapping *header_map = nullptr;
    for (const auto &mapping : all) {
        if (nhk::strip_deleted(mapping.path) != target->path) continue;
        if (mapping.file_offset < target->view_begin
            || mapping.file_offset >= target->view_end) {
            continue;
        }
        if (header_map == nullptr || mapping.file_offset < header_map->file_offset) {
            header_map = &mapping;
        }
    }
    if (header_map == nullptr) {
        report_guard_pending("runtime-window-empty",
            "no mapping inside the runtime window of " + target->path);
        madvise_guard_state.store(0U, std::memory_order_release);
        return;
    }
    if (header_map->end - header_map->begin < kGuardHeaderBytes) {
        give_up("runtime header mapping shorter than a page");
        return;
    }
    std::array<std::byte, kGuardHeaderBytes> head{};
    if (!safe_read(header_map->begin, std::span<std::byte>(head))) {
        stay_pending("runtime header unreadable this pass");
        return;
    }
    const auto segments = nhk::elf::parse_program_segments(std::span<const std::byte>(head));
    if (!segments) {
        stay_pending("runtime program headers rejected");
        return;
    }
    // The window matters: an APK carries several libraries, so the target's
    // mappings can only be told apart from its neighbours by the byte range its
    // stored entry occupies.
    const auto view = nhk::image_view_from_segments_in_window(
        all, target->path, *segments, target->view_begin, target->view_end);
    if (!view) {
        stay_pending("runtime mappings do not match its program headers");
        return;
    }
    if (view->needed_vaddr == 0 || view->needed_vaddr > kGuardImageBudgetBytes) {
        __android_log_print(ANDROID_LOG_WARN, kTag,
            "madvise guard unavailable: image needs %llu bytes, budget is %llu",
            static_cast<unsigned long long>(view->needed_vaddr),
            static_cast<unsigned long long>(kGuardImageBudgetBytes));
        give_up("image exceeds the snapshot budget");
        return;
    }

    // Stage 2: snapshot the image's virtual address range, laid out by address
    // so the parser can index it by vaddr; gaps stay zero. Every range must
    // read completely - a half-read snapshot must not be parsed as if it were
    // the image.
    static std::vector<std::byte> snapshot;
    const size_t needed = static_cast<size_t>(view->needed_vaddr);
    if (snapshot.size() < needed) snapshot.resize(needed);
    std::fill(snapshot.begin(), snapshot.begin() + static_cast<ptrdiff_t>(needed),
        std::byte{0});
    for (const auto &range : view->ranges) {
        if (range.vaddr >= needed) continue;
        const uint64_t usable = std::min<uint64_t>(range.bytes, needed - range.vaddr);
        if (usable == 0) continue;
        if (!safe_read(range.begin,
                std::span<std::byte>(snapshot.data() + range.vaddr,
                    static_cast<size_t>(usable)))) {
            stay_pending("runtime segment unreadable this pass");
            return;
        }
    }
    const auto image = nhk::elf::ElfImage::make_with_segments(
        static_cast<uintptr_t>(view->load_base),
        std::span<const std::byte>(snapshot.data(), needed), *segments);
    if (!image) {
        give_up("runtime ELF rejected");
        return;
    }
    std::optional<std::vector<nhk::ExecutableMapping>> flutter_after;
    {
        std::ifstream fresh("/proc/self/maps");
        if (fresh) flutter_after = runtime_mappings(nhk::parse_file_mappings(fresh), *target);
    }
    if (!flutter_before || flutter_before->empty() || !flutter_after
        || flutter_after->empty()) {
        // "Unknown" is not "unchanged": the second scan could not be taken, so
        // stability cannot be proven either way. Refuse this pass instead of
        // installing against a snapshot whose counterpart is missing - a later
        // pass re-reads both sides and settles.
        stay_pending("runtime inventory unavailable during the snapshot");
        return;
    }
    if (*flutter_before != *flutter_after) {
        // The library was remapped while being read: the headers and the bytes
        // may come from different generations.
        stay_pending("runtime mappings changed during the snapshot");
        return;
    }
    nhk::ImageIdentity identity;
    identity.device_major = header_map->device_major;
    identity.device_minor = header_map->device_minor;
    identity.inode = header_map->inode;
    identity.load_base = view->load_base;

    std::vector<nhk::GotHook<>> installed;
    std::vector<nhk::GotHook<>> residual;
    bool rollback_clean = true;
    const bool installed_ok = nhk::install_madvise_guard(*image,
        [](uintptr_t slot, const void *&value) {
            return read_pointer_safely(slot, value);
        },
        reinterpret_cast<void *>(hyperceiler_guarded_madvise),
        // Published before the slot is replaced: once the GOT points at the
        // replacement, its forwarding target is already visible.
        [](void *original) {
            g_real_madvise.store(original, std::memory_order_release);
        },
        installed, &rollback_clean, identity, &residual);
    if (!installed_ok) {
        if (!rollback_clean && !residual.empty()) {
            // Slots may still hold the replacement. Keep the record (so the
            // state stays visible and repairable) and keep the forward target:
            // a clean rollback does NOT prove nothing is executing the
            // replacement - a thread may already have loaded its address or be
            // inside it, still to read the forward target. Clearing it would
            // turn that call into a failure.
            __android_log_print(ANDROID_LOG_ERROR, kTag,
                "madvise guard rollback incomplete; %zu slot record(s) kept and "
                "forward target retained", residual.size());
            g_madvise_hooks = std::move(residual);
            // Kept as evidence *and* as the identity later passes validate
            // against - a residue without its generation cannot be repaired.
            g_madvise_target = *target;
            madvise_guard_state.store(4U, std::memory_order_release);
            return;
        }
        // Nothing was left behind; the forward target is either untouched (never
        // published) or, if it was published, left as it is for the same
        // in-flight reason.
        give_up("madvise import missing or ambiguous");
        return;
    }
    g_madvise_hooks = std::move(installed);
    // Bind the record to the exact image generation it was read from: every
    // later maintenance pass validates the slots against this identity.
    g_madvise_target = *target;
    // Naming the source is what tells the two identification layers apart in the
    // field: window=0 is a bare, file-named copy, any other window is a stored
    // entry inside a container. Without it the line only says "it worked".
    __android_log_print(ANDROID_LOG_INFO, kTag,
        "madvise guard installed; %zu slot(s) protected from %s window=0x%llx",
        g_madvise_hooks.size(), target->path.c_str(),
        static_cast<unsigned long long>(target->view_begin));
    madvise_guard_state.store(1U, std::memory_order_release);
}

bool maintain_dock_motion_hooks(bool force) noexcept {
    try {
        const bool healthy = maintain_dock_motion_hooks_impl(force);
        runtime_ready.store(healthy, std::memory_order_release);
        return healthy;
    } catch (...) {
        runtime_ready.store(false, std::memory_order_release);
        deactivate_dock_motion();
        __android_log_print(ANDROID_LOG_WARN, kTag,
            "motion health check failed safely due to a native exception");
        return false;
    }
}

void pause_for(long nanoseconds) {
    timespec remaining{nanoseconds / 1000000000L, nanoseconds % 1000000000L};
    while (nanosleep(&remaining, &remaining) != 0 && errno == EINTR) {}
}

void *health_worker(void *) {
    for (;;) {
        const bool healthy = maintain_dock_motion_hooks(false);
        pause_for(healthy ? 250000000L : 2000000000L);
    }
}

void *motion_worker_impl() {
    if (!prepare_dock_motion()) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "motion event channel unavailable");
        return nullptr;
    }
    pthread_t health;
    if (pthread_create(&health, nullptr, health_worker, nullptr) == 0) pthread_detach(health);
    else {
        __android_log_print(ANDROID_LOG_WARN, kTag, "motion health worker unavailable");
        return nullptr;
    }
    __android_log_print(ANDROID_LOG_INFO, kTag,
        "dynamic motion v35 transport starting independently of runtime discovery");
    // run_dock_motion() is a permanent service loop. If it ever returns, the launcher
    // would silently lose real-time motion for the rest of its life, because the only
    // remaining re-arm paths fire on rare one-shot events. Restart the transport
    // instead of letting this worker end and clearing `started`.
    for (;;) {
        run_dock_motion();
        __android_log_print(ANDROID_LOG_WARN, kTag,
            "motion transport returned unexpectedly; restarting in 1s");
        pause_for(1000000000L);
    }
}

void *motion_worker(void *) {
    worker_alive.store(true, std::memory_order_release);
    try {
        void *result = motion_worker_impl();
        worker_alive.store(false, std::memory_order_release);
        started.store(false, std::memory_order_release);
        return result;
    } catch (...) {
        worker_alive.store(false, std::memory_order_release);
        runtime_ready.store(false, std::memory_order_release);
        started.store(false, std::memory_order_release);
        deactivate_dock_motion();
        __android_log_print(ANDROID_LOG_WARN, kTag,
            "motion worker stopped safely due to a native exception");
        return nullptr;
    }
}
} // namespace

bool revalidate_dock_motion_hooks() {
    return maintain_dock_motion_hooks(true);
}

uint32_t dock_native_motion_state() {
    uint32_t state = 0;
    if (started.load(std::memory_order_acquire)) state |= 1U;
    if (worker_alive.load(std::memory_order_acquire)) state |= 2U;
    if (runtime_ready.load(std::memory_order_acquire)) state |= 4U;
    return state;
}

void start_dock_native_motion(Hook hook, Unhook unhook) {
    if (hook == nullptr || started.exchange(true)) return;
    hook_function = hook;
    unhook_function = unhook;
    // Register with the shared backend boundary so the slot host installs
    // through the same entry points the runtime documents.
    nhk::LsposedInlineBackend::instance().install_entries({hook, unhook});
    pthread_t thread;
    if (pthread_create(&thread, nullptr, motion_worker, nullptr) == 0) pthread_detach(thread);
    else {
        started.store(false, std::memory_order_release);
        __android_log_print(ANDROID_LOG_WARN, kTag, "motion worker unavailable");
    }
}
