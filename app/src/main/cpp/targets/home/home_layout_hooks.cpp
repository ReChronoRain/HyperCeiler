/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_layout_config.h"
#include "home_layout_knobs.h"
#include "home_layout_elf_targets.h"
#include "nativehook/hook_bank.h"
#include "nativehook/memory_io.h"
#include "nativehook/native_image.h"

#include <android/log.h>
#include <pthread.h>
#include <sys/stat.h>
#include <sys/system_properties.h>

// Launcher tweaks (targets/home/tweaks). The planner locates every site from the image's own
// symbol table, so these are values only - no addresses cross this boundary.
#include "targets/home/tweaks/blob.h"
namespace hometweaks {
void PushTweaksConfig(const Config &config);
bool HomeTweaksFindSymbol(const char *name, uint32_t *outVa, uint32_t *outSize);
bool HomeTweaksTargetImage(char *path, size_t cap);
}

/*
 * Panel-interactive gate for the maintenance worker, owned by the dock motion
 * transport (dock_native_motion.cpp) because that is the only component here that
 * already talks to system_server - and system_server is where PowerManagerService
 * lives, while every native carrier of the panel state on this ROM is root-only.
 *
 * Declared here rather than pulled from a header because the transport is compiled
 * in only with the dock feature: with it off there is nothing to ask, and the gate
 * degrades to "always interactive" (see layout_panel_refresh_and_check) so this
 * file still builds and the worker keeps its unconditional cadence.
 */
#if defined(HYPERCEILER_DOCK_NATIVE_MOTION)
bool refresh_dock_screen_state();
bool dock_motion_screen_active();
#endif

#include <algorithm>
#include <array>
#include <atomic>
#include <cerrno>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <cstdlib>
#include <fstream>
#include <optional>
#include <span>
#include <string>
#include <string_view>
#include <time.h>
#include <unistd.h>
#include <vector>

/*
 * The launcher's geometry values live in shared configuration objects, and Dart AOT inlines a getter
 * as small as `ldur d0, [x0, #imm]` at almost every call site - so the standalone accessor function is
 * called rarely or never, and hooking it changes nothing. Measured on device: with all eight geometry
 * accessors hooked, `GridController.currentConfig` (a static getter too large to inline) counted 601
 * calls while every geometry accessor counted zero.
 *
 * What every consumer - inlined or not - does read is the object `currentConfig` returns. So the
 * module installs two capture trampolines, takes the pointers to those shared objects, decodes each
 * knob's field path out of the launcher's own accessor code, and rewrites the fields in place. Nothing
 * is hardcoded: the symbol names come from the launcher image's own symbol table, and the field
 * offsets are read from the instructions that actually use them, so a launcher OTA moves everything
 * and this keeps working.
 */
extern "C" {
/*
 * One control block per knob, for the hook strategy: the trampoline calls the original layout
 * aggregator and adds this delta to its double result. `hits` is bumped by the trampoline, which is
 * how "this aggregator never runs" is told apart from "it runs and the delta does nothing".
 */
#define HC_DECLARE_KNOB(name, column, label, dflt, lo, hi) \
    uint8_t hc_layout_dart_##name##_enabled = 0; \
    uint64_t hc_layout_dart_##name##_delta = 0; \
    uint64_t hc_layout_dart_##name##_hits = 0; \
    uint64_t hc_layout_dart_##name##_last = 0; \
    uint64_t hc_layout_dart_##name##_caller = 0; \
    void *hc_layout_dart_##name##_original = nullptr; \
    void hc_layout_dart_##name##_entry();
HC_LAYOUT_KNOBS(HC_DECLARE_KNOB)
#undef HC_DECLARE_KNOB
/* Objects captured by the trampolines, and the heap base a compressed pointer is relative to. */
uint64_t hc_layout_config_object = 0;
uint64_t hc_layout_dock_object = 0;
uint64_t hc_layout_heap_base = 0;
uint64_t hc_layout_config_capture_hits = 0;
uint64_t hc_layout_dock_capture_hits = 0;
void *hc_layout_config_capture_original = nullptr;
void *hc_layout_dock_capture_original = nullptr;
void hc_layout_config_capture_entry();
void hc_layout_dock_capture_entry();
void hc_layout_passthrough_entry();
void *hc_layout_passthrough_original = nullptr;
/* Probe readings, written by the passthrough stub itself. */
uint64_t hc_layout_probe_caller = 0;
uint64_t hc_layout_probe_value = 0;
uint32_t hc_layout_probe_hits = 0;
/* Called by both trampolines before they return, so the caller reads already-adjusted fields.
   `caller` is the Dart return address, recorded so the calibration can see which functions run. */
void hc_layout_apply_now(uint64_t config, uintptr_t caller);
}

namespace {
constexpr char kTag[] = "HyperCeiler.HomeLayout";
constexpr size_t kPatchWords = 4;
constexpr uint64_t kMaxLauncherImageBytes = 32U << 20;
constexpr int kMaxWorkerAttempts = 8;
/*
 * Worker cadences. The pass interval is what bounds how long a kernel-restored
 * patch can stay missing; the other two only bound how often an expensive question
 * is asked, so they are wall-clock throttles rather than iteration counts - the
 * iteration itself changes length once the panel is dozing.
 */
constexpr uint64_t kPassIntervalMs = 500;
constexpr uint64_t kPanelQueryIntervalMs = 1000;
constexpr uint64_t kDozingIterationMs = 10000;
constexpr uint64_t kGenerationCheckMs = 30000;
/* Slots: 0/1 the Rust grid handlers, 2/3 the two Dart object captures, then one per hook knob. */
constexpr size_t kConfigCaptureSlot = 2;
constexpr size_t kDockCaptureSlot = 3;
constexpr size_t kKnobHookSlotBase = 4;
constexpr size_t kSlotCount = kKnobHookSlotBase + HC_LAYOUT_KNOB_COUNT;
using Slot = nhk::InlineSlot<kPatchWords>;
using Words = nhk::SlotWords<kPatchWords>;
using HookFunction = int (*)(void *, void *, void **);
using UnhookFunction = int (*)(void *);

std::atomic_bool g_started{false};
/*
 * Field writes into the launcher's live objects are off by default: a wrong path there is a launcher
 * crash, not a no-op. Hook-based knobs (adjust a function's return value) do not need this gate.
 */
std::atomic_bool g_field_writes_enabled{false};
std::atomic_int g_attempts{0};
std::atomic_bool g_ready{false};
std::atomic_bool g_dart_ready{false};
std::atomic_int g_cell_x{0};
std::atomic_int g_cell_y{0};
HookFunction g_hook_function = nullptr;
UnhookFunction g_unhook_function = nullptr;
void *g_original_x = nullptr;
void *g_original_y = nullptr;
std::array<Slot, kSlotCount> g_slots{};
std::string g_container_path;
uint64_t g_view_begin = 0;
uint64_t g_view_end = 0;
std::string g_dart_container_path;
uint64_t g_dart_view_begin = 0;
uint64_t g_dart_view_end = 0;

int cell_x_replacement() {
    if (g_ready.load(std::memory_order_acquire)) return g_cell_x.load(std::memory_order_relaxed);
    const auto original = reinterpret_cast<int (*)()>(g_original_x);
    return original != nullptr ? original() : g_cell_x.load(std::memory_order_relaxed);
}

int cell_y_replacement() {
    if (g_ready.load(std::memory_order_acquire)) return g_cell_y.load(std::memory_order_relaxed);
    const auto original = reinterpret_cast<int (*)()>(g_original_y);
    return original != nullptr ? original() : g_cell_y.load(std::memory_order_relaxed);
}

/*
 * One row per geometry knob. `symbol` names the Dart accessor whose value the user is moving; it may
 * be replaced at run time by the calibration properties. The field path below it is derived from that
 * accessor's own instructions at run time.
 */
struct KnobRuntime {
    std::string symbol;
    int default_dp = 0;
    int min_dp = 0;
    int max_dp = 0;
    uint32_t getter = 0;
    /*
     * The live build's size for `getter`'s symbol, straight out of the same lookup that produced the
     * address. It is reported because a stale analysis symbol table is otherwise invisible: the
     * launcher image changes under the module (same versionName, new bytes) and every offset, size
     * and xref taken from the older copy silently describes a function that is no longer there.
     * With this on the log line, "is the thing I hooked the thing I analysed" is answerable from
     * logcat alone.
     */
    uint32_t getter_size = 0;
    /*
     * The field path is published atomically because the background worker derives it while the Dart
     * thread reads it inside the trampoline. A torn read here would mean a write to an unrelated
     * address, so the packed value is the only thing the trampoline trusts:
     *   bits 0..7 object (1 config, 2 dock), 8..15 off0 + 1 (0 means direct), 16..31 off1.
     */
    std::atomic<uint32_t> path{0};
    std::atomic<int> delta_px{0};
    /*
     * Trampoline-thread only, never touched by the worker.
     *
     * `pristine` is the launcher's own value, remembered from a moment when this knob was not
     * touching the field, and every write is `pristine + delta` - an absolute value, never a
     * read-modify-write.
     *
     * That is not a style choice. The desktop rebuilds its configuration by copying the current one,
     * so a read-modify-write adds the delta again to a value that already carried it: with the
     * delta applied on every one of the thousands of `currentConfig` calls, a 32 px request grew
     * without bound and pushed the whole workspace off screen on the device. Deriving every write
     * from a value captured while the knob was off cannot compound, whatever the launcher copies.
     */
    double pristine = 0;
    bool pristine_valid = false;

    /*
     * Hook strategy. `hook_mode` is set when the calibration property points the knob at a layout
     * aggregator instead of at the field its accessor reads; the aggregators are large enough that
     * Dart cannot inline them, so a hook there is actually reached.
     */
    bool hook_mode = false;
    uintptr_t hook_address = 0;
    nhk::CodeSource hook_source{};
    Words hook_words{};
    void *hook_entry = nullptr;
    void **hook_original = nullptr;
    uint64_t *hook_delta = nullptr;
    uint64_t *hook_hits = nullptr;
    uint64_t *hook_last = nullptr;    // the launcher's own return value, raw double bits
    uint64_t *hook_caller = nullptr;  // the Dart caller's return address, for the call chain
    uint8_t *hook_enabled = nullptr;
    bool hook_armed = false;
};

/*
 * Knobs whose value is produced by a named accessor are moved by hooking that accessor and adding the
 * delta to the double it returns. This is the safe lever: it never writes into a live object, so a
 * wrong target produces "no effect" instead of a corrupted heap. A null entry means "no hook target
 * known yet" - that knob stays inert rather than guessing.
 *
 * Order matches HC_LAYOUT_KNOBS.
 */
constexpr const char *kKnobHookSymbols[HC_LAYOUT_KNOB_COUNT] = {
    /*
     * Every entry is null on purpose, and this is the shipped state.
     *
     * Hooking `GridSizeCalRules.stableWorkspaceCellPaddingTop` for the workspace top margin was tried
     * on device and the launcher died with a native tombstone. Until that is understood the geometry
     * knobs stay inert: an unconvincing feature is acceptable, an unstable launcher is not. A
     * calibration run can still name a target through `debug.hyperceiler.layout.hook0..7`, which is
     * gated behind the debug channel.
     */
    /*
     * WorkspaceTop is deliberately **inert**: hooking it is safe (0 crashes) but the semantics are
     * wrong. Measured on the current build with a read-only probe:
     *   - `GridController.titleMarginTop` (833 hits, caller `ShortcutIconWidget._buildTextWidget`):
     *     a +20 delta translated the whole icon grid by -30 px. The user clarified the semantics:
     *     this is the *icon ↔ its title* spacing, which scales the icon cell and shifts the grid -
     *     it is NOT the workspace's own top padding, so it must not answer a "top margin" slider.
     *   - `GridConfig.workspacePaddingTop`: 0 hits - the workspace reads the config object's fields
     *     directly (disassembly of `GridConfig.calGridSize`: `ldur w4,[x3,#0x1f]` → decompress →
     *     class id 0x73c → `ldur w1,[x4,#0x3f]`, which is again an object reference, not a number).
     *     So the real top padding lives in a nested padding object, and the numeric leaf has not been
     *     reached yet. Writing it means touching the Dart heap - the exact thing the first crash was
     *     made of - so the object chain has to be dumped and verified at runtime first.
     * Until that dump exists, this knob stays inert rather than shipping a mislabeled lever.
     */
    /* HotseatMargin   */ nullptr,
    /* HotseatHeight   */ nullptr,
    /* WorkspaceTop    */ nullptr,
    /* WorkspaceBottom */ nullptr,
    /* WorkspaceSide   */ nullptr,
    /* IndicatorMargin */ nullptr,
    /* SearchBarMargin */ "GridController.searchBarMarginBottom",
    /* SearchBarWidth  */ nullptr,
};

/*
 * Multiplier applied to a knob's delta when it is published to the trampoline.
 *
 * A "top margin" slider should push content *down* as it grows. `titleMarginTop` does the opposite
 * and by a factor: the measured response on the current build is Δy ≈ -1.5 × delta, because the grid
 * is anchored at the bottom, so growing the icon cell lifts the block. Feeding the raw delta through
 * would make the slider move the workspace the wrong way, faster than the user asked for.
 *
 * The number is a measurement, not a guess: -20 → +29 px, +5 → -7 px, +20 → -30 px, each verified
 * twice with a zero-delta control run in between (control: Δy = 0). A launcher OTA that changes the
 * layout will change this constant; the probe (`debug.hyperceiler.layout.hook*`) is how to re-measure
 * it, and the failure mode is "the slider moves things the wrong way", which is visible, not fatal.
 */
constexpr double kKnobDeltaGain[HC_LAYOUT_KNOB_COUNT] = {
    1.0,             // HotseatMargin   (inert)
    1.0,             // HotseatHeight   (inert)
    -1.0 / 1.5,      // WorkspaceTop   : titleMarginTop, inverted with a 1.5x gain
    1.0,             // WorkspaceBottom (inert)
    1.0,             // WorkspaceSide   (inert)
    1.0,             // IndicatorMargin (inert)
    1.0,             // SearchBarMargin : startup-only, uncalibrated
    1.0,             // SearchBarWidth  (inert)
};

std::array<KnobRuntime, HC_LAYOUT_KNOB_COUNT> g_knobs = {{
#define HC_KNOB_ROW(name, column, symbol, dflt, lo, hi) {symbol, dflt, lo, hi},
    HC_LAYOUT_KNOBS(HC_KNOB_ROW)
#undef HC_KNOB_ROW
}};

/* Bind the per-knob trampoline pointers from the same list, so the two can never drift apart. */
bool g_hook_globals_inited = false;

void init_knob_hooks() {
    // Not a function-local static: the flag would be inherited across the fork, and a desktop
    // forked after the spawner ran this once would keep null hook pointers forever.
    if (g_hook_globals_inited) return;
    g_hook_globals_inited = true;
    size_t index = 0;
#define HC_SET_HOOK(name, column, symbol, dflt, lo, hi)                                          \
    g_knobs[index].hook_entry = reinterpret_cast<void *>(hc_layout_dart_##name##_entry);         \
    g_knobs[index].hook_original = &hc_layout_dart_##name##_original;                            \
    g_knobs[index].hook_delta = &hc_layout_dart_##name##_delta;                                  \
    g_knobs[index].hook_hits = &hc_layout_dart_##name##_hits;                                    \
    g_knobs[index].hook_last = &hc_layout_dart_##name##_last;                                    \
    g_knobs[index].hook_caller = &hc_layout_dart_##name##_caller;                                \
    g_knobs[index].hook_enabled = &hc_layout_dart_##name##_enabled;                              \
    ++index;
    HC_LAYOUT_KNOBS(HC_SET_HOOK)
#undef HC_SET_HOOK
}

std::optional<std::vector<nhk::ExecutableMapping>> current_mappings() {
    std::ifstream maps("/proc/self/maps");
    if (!maps) return {};
    const auto all = nhk::parse_file_mappings(maps);
    std::vector<nhk::ExecutableMapping> owned;
    const auto collect = [&](const std::string &path, uint64_t begin, uint64_t end) {
        if (path.empty() || end <= begin) return;
        const std::vector<nhk::ImageContainer> container{{path, begin, end}};
        const auto found = nhk::owned_image_mappings(all, container);
        owned.insert(owned.end(), found.begin(), found.end());
    };
    collect(g_container_path, g_view_begin, g_view_end);
    collect(g_dart_container_path, g_dart_view_begin, g_dart_view_end);
    if (owned.empty()) return std::nullopt;
    return owned;
}

bool stable_read(const Slot &slot, Words &words) {
    const std::array<uintptr_t, 1> address{slot.address};
    const std::array<nhk::CodeSource, 1> expected{slot.source};
    const auto before = current_mappings();
    if (!before || nhk::mapping_state(*before, address, expected, sizeof(words))
            != nhk::MappingState::same) return false;
    if (!nhk::safe_read(slot.address, std::as_writable_bytes(std::span(&words, 1)))) return false;
    const auto after = current_mappings();
    return after && nhk::mapping_state(*after, address, expected, sizeof(words))
        == nhk::MappingState::same;
}

/*
 * Steady-state read of the armed slots' live words: one read per slot, nothing else.
 *
 * `stable_read` above rebuilds the whole /proc/self/maps inventory twice so it can prove the image
 * generation is still mapped at those offsets. That proof is what the health pass used to pay for on
 * every cadence - `ensure_slots_live` calls the validated read once per armed slot, and this pass runs
 * twice a second, which on this launcher (thousands of mappings) came to twenty-four full inventory
 * parses per pass. It is the same shape as the dock's 22%-of-a-core incident at four times the
 * frequency, and it was the entire cost of the layout feature's steady state.
 *
 * A slot whose live words still equal the recorded patch is a working patch. A torn read here cannot
 * be trusted, but it also cannot hurt: it fails the comparison and drops into the validated repair
 * below, which re-reads through `stable_read` and is the only path that writes code. See
 * `ordered_slots_healthy` in nativehook/hook_bank.h for why the split is safe.
 */
bool read_live_words(std::span<const uintptr_t> addresses, std::span<const nhk::CodeSource>,
    std::span<Words> observed) {
    for (size_t index = 0; index < addresses.size(); ++index) {
        if (!nhk::safe_read(addresses[index], std::as_writable_bytes(observed.subspan(index, 1)))) {
            return false;
        }
    }
    return true;
}

bool bank_live(const std::vector<size_t> &order) {
    return nhk::ordered_slots_healthy(g_slots, order, read_live_words);
}

/*
 * The generation proof `stable_read` performs, over one inventory shared by every armed slot.
 *
 * The steady state deliberately reads words only, and the words cannot see a generation change on
 * their own: what they prove is "the patch is still there", not "the address still belongs to the
 * image we bound it against". One inventory per interval, with a lookup per slot, is the periodic
 * restatement of that second half - and unlike the validated read it does not multiply the inventory
 * parse by the number of slots.
 */
bool bank_generation_holds(const std::vector<size_t> &order,
    const std::vector<nhk::ExecutableMapping> &inventory) {
    for (const size_t index : order) {
        const Slot &slot = g_slots[index];
        const std::array<uintptr_t, 1> address{slot.address};
        const std::array<nhk::CodeSource, 1> expected{slot.source};
        if (nhk::mapping_state(inventory, address, expected, sizeof(Words))
            != nhk::MappingState::same) {
            return false;
        }
    }
    return true;
}

std::optional<uint64_t> process_age_ms() {
    std::ifstream stat("/proc/self/stat");
    if (!stat) return {};
    std::string line;
    std::getline(stat, line);
    const size_t close = line.rfind(')');
    if (close == std::string::npos) return {};
    unsigned long long start_ticks = 0;
    if (std::sscanf(line.c_str() + close + 1,
            "%*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %llu",
            &start_ticks) != 1) return {};
    const long hz = sysconf(_SC_CLK_TCK);
    if (start_ticks == 0 || hz <= 0) return {};
    std::ifstream uptime("/proc/uptime");
    if (!uptime) return {};
    double seconds = 0;
    if (!(uptime >> seconds)) return {};
    const double start_seconds = static_cast<double>(start_ticks) / static_cast<double>(hz);
    if (seconds < start_seconds) return {};
    return static_cast<uint64_t>((seconds - start_seconds) * 1000.0);
}

uint64_t process_age_or_zero() {
    const auto age = process_age_ms();
    return age ? *age : 0;
}

nhk::InlineHookHost<kPatchWords> &slot_host() {
    static nhk::InlineHookHost<kPatchWords> host = [] {
        nhk::InlineHookHost<kPatchWords> value;
        value.read_slot = [](const nhk::InlineSlot<kPatchWords> &slot, nhk::SlotWords<kPatchWords> &out) {
            const bool ok = stable_read(slot, out);
            if (!ok) {
                __android_log_print(ANDROID_LOG_ERROR, kTag,
                    "layout read_slot failed address=%p", reinterpret_cast<void *>(slot.address));
            }
            return ok;
        };
        value.write_words = [](uintptr_t address, const Words &words) {
            return nhk::write_code_bytes(address, std::as_bytes(std::span(&words, 1)));
        };
        value.hook_install = [](void *target, void *replacement, void **original) {
            const int rc = g_hook_function != nullptr
                ? g_hook_function(target, replacement, original)
                : -1;
            if (rc != 0) {
                __android_log_print(ANDROID_LOG_ERROR, kTag,
                    "layout hook_install failed target=%p rc=%d", target, rc);
            }
            return rc;
        };
        value.hook_uninstall = [](void *target) {
            return g_unhook_function != nullptr ? g_unhook_function(target) : -1;
        };
        value.on_guard = [](const nhk::HookEvent &event) {
            __android_log_print(ANDROID_LOG_ERROR, kTag, "layout %s slot=%zu", event.reason,
                event.slot);
        };
        value.on_info = [](const nhk::HookEvent &event) {
            __android_log_print(ANDROID_LOG_INFO, kTag, "layout %s slot=%zu", event.reason,
                event.slot);
        };
        return value;
    }();
    return host;
}

/* One stored library of the launcher APK, bound to its runtime view. */
struct Library {
    std::string path;
    uint64_t view_begin = 0;
    uint64_t view_end = 0;
    uint64_t load_base = 0;
    std::vector<std::byte> bytes;
    std::vector<nhk::elf::ProgramSegment> segments;
    std::vector<nhk::ExecutableMapping> owned;

    std::optional<uintptr_t> at(uint64_t va) const {
        if (load_base > UINTPTR_MAX - va) return std::nullopt;
        return static_cast<uintptr_t>(load_base + va);
    }

    std::optional<nhk::CodeSource> source(uintptr_t address) const {
        return nhk::source_at(owned, address, sizeof(Words));
    }

    std::optional<uint64_t> file_offset(uint64_t va, size_t count) const {
        for (const auto &segment : segments) {
            if (segment.type != nhk::elf::kProgramTypeLoad || va < segment.vaddr) continue;
            const uint64_t delta = va - segment.vaddr;
            if (delta > segment.filesz || count > segment.filesz - delta) continue;
            if (!nhk::in_image(bytes.size(), segment.offset + delta, count)) return std::nullopt;
            return segment.offset + delta;
        }
        return std::nullopt;
    }
};

template <typename Mappings>
std::optional<Library> open_library(const std::string &apk_path, std::string_view entry,
    const Mappings &all) {
    const auto stored = nhk::zip_stored_entry(apk_path, entry);
    if (!stored || stored->second == 0 || stored->second > kMaxLauncherImageBytes
        || nhk::add_overflows(stored->first, stored->second)) return {};
    std::ifstream apk(apk_path, std::ios::binary);
    if (!apk) return {};
    Library library;
    library.path = apk_path;
    library.view_begin = stored->first;
    library.view_end = stored->first + stored->second;
    library.bytes.resize(static_cast<size_t>(stored->second));
    apk.seekg(static_cast<std::streamoff>(stored->first));
    apk.read(reinterpret_cast<char *>(library.bytes.data()),
        static_cast<std::streamsize>(library.bytes.size()));
    if (static_cast<size_t>(apk.gcount()) != library.bytes.size()) return {};
    const auto segments = nhk::elf::parse_program_segments(library.bytes);
    if (!segments) return {};
    library.segments = *segments;
    const auto view = nhk::image_view_from_segments_in_window(all, apk_path, library.segments,
        library.view_begin, library.view_end);
    if (!view) return {};
    const std::vector<nhk::ImageContainer> container{{
        apk_path, library.view_begin, library.view_end}};
    library.owned = nhk::owned_image_mappings(all, container);
    if (library.owned.empty()) return {};
    library.load_base = view->load_base;
    return library;
}

struct Located {
    uintptr_t x = 0;
    uintptr_t y = 0;
    nhk::CodeSource x_source{};
    nhk::CodeSource y_source{};
    Words x_words{};
    Words y_words{};
    std::string container_path;
    uint64_t view_begin = 0;
    uint64_t view_end = 0;
    std::string dart_container_path;
    uint64_t dart_view_begin = 0;
    uint64_t dart_view_end = 0;
};

/*
 * The Dart snapshot stays useful after locate(): the object fields are rewritten long after the
 * container is found, so only the mapping metadata is retained and the few instruction words are
 * re-read from the APK on demand.
 */
struct DartLibrary {
    std::string path;
    uint64_t view_begin = 0;
    uint64_t view_end = 0;
    uint64_t load_base = 0;
    std::vector<nhk::elf::ProgramSegment> segments;
    std::vector<nhk::ExecutableMapping> owned;
};

std::optional<DartLibrary> g_dart;

std::optional<std::string> launcher_apk(const std::vector<nhk::FileMapping> &all) {
    std::optional<std::string> path;
    for (const auto &mapping : all) {
        const std::string_view candidate = nhk::strip_deleted(mapping.path);
        if (candidate.find("/com.miui.home-") == std::string_view::npos
            || !candidate.ends_with("/base.apk")) continue;
        if (path && *path != candidate) return {};
        path = std::string(candidate);
    }
    if (path) return path;
    /*
     * The /data upgrade pattern above is not the only shape a launcher APK has: the 7654 desktop
     * ships from /product/priv-app/MiuiHome, which no directory guess should have to know. The
     * tweaks module has already proven which image is actually loaded (dl_iterate_phdr), so hand
     * its answer over instead of a second, wrong guess. Without this fallback every geometry site
     * and every hook knob lost its image anchor on 7654 — "layout targets unavailable" plus a
     * silent knobs=0/8.
     */
    char proven[512]{};
    if (hometweaks::HomeTweaksTargetImage(proven, sizeof(proven))) {
        return std::string(proven);
    }
    return path;
}

bool ensure_dart_library() {
    if (g_dart) return true;
    std::ifstream maps("/proc/self/maps");
    if (!maps) return false;
    const auto all = nhk::parse_file_mappings(maps);
    const auto path = launcher_apk(all);
    if (!path) return false;
    /*
     * Cheap gate before the expensive open: this is polled while the launcher starts, and
     * open_library() reads the whole 28 MB entry out of the APK before it can discover the window is
     * not mapped yet. An embedded library's first segment is mapped at the entry's own file offset.
     */
    const auto stored = nhk::zip_stored_entry(*path, "libapp.so");
    if (!stored) return false;
    /*
     * Cheap gate before the expensive open: this is polled while the launcher starts, and
     * open_library() reads the whole 28 MB entry out of the APK before it can discover the window is
     * not mapped yet.
     *
     * The old check required a mapping whose file offset equals the entry's own start, which almost
     * never holds: an ELF segment with a non-zero p_offset maps at entry_start + p_offset, and ART
     * also splits mappings at page boundaries. Accept any mapping of this APK whose offset falls
     * inside the entry's span - the same condition open_library's window check applies later.
     */
    bool entry_mapped = false;
    for (const auto &mapping : all) {
        if (nhk::strip_deleted(mapping.path) != *path) continue;
        if (mapping.file_offset < stored->first
            || mapping.file_offset >= stored->first + stored->second) {
            continue;
        }
        entry_mapped = true;
        break;
    }
    if (!entry_mapped) {
        // Polled, so report once: a silent false here used to look exactly like "the image is not
        // loaded yet", and cost a whole debugging session (the probe never bound).
        static bool reported = false;
        if (!reported) {
            reported = true;
            __android_log_print(ANDROID_LOG_WARN, kTag,
                "layout dart entry mapped check failed: entry=%#llx size=%llu maps=%zu",
                static_cast<unsigned long long>(stored->first),
                static_cast<unsigned long long>(stored->second), all.size());
        }
        return false;
    }
    const auto dart = open_library(*path, "libapp.so", all);
    if (!dart) return false;
    DartLibrary retained;
    retained.path = dart->path;
    retained.view_begin = dart->view_begin;
    retained.view_end = dart->view_end;
    retained.load_base = dart->load_base;
    retained.segments = dart->segments;
    retained.owned = dart->owned;
    g_dart = std::move(retained);
    g_dart_container_path = g_dart->path;
    g_dart_view_begin = g_dart->view_begin;
    g_dart_view_end = g_dart->view_end;
    /*
     * Identity of the Dart image this process is running, reported so that static analysis material
     * can be checked against it instead of assumed. The launcher image changes under the module -
     * same versionName, new bytes - and yesterday's symbol table then describes functions that are
     * no longer there: an address can land inside a *different* function while still looking like a
     * plausible entry. Entry offset, entry size and the APK's own mtime are enough to tell the two
     * apart from logcat alone.
     */
    {
        struct stat apk {};
        const bool have_apk = stat(g_dart->path.c_str(), &apk) == 0;
        __android_log_print(ANDROID_LOG_INFO, kTag,
            "layout dart container base=%p entry=%#llx size=%#llx apk_size=%llu apk_mtime=%lld",
            reinterpret_cast<void *>(static_cast<uintptr_t>(g_dart->load_base)),
            static_cast<unsigned long long>(g_dart->view_begin),
            static_cast<unsigned long long>(g_dart->view_end - g_dart->view_begin),
            have_apk ? static_cast<unsigned long long>(apk.st_size) : 0ULL,
            have_apk ? static_cast<long long>(apk.st_mtime) : 0LL);
    }
    return true;
}

std::optional<uint64_t> dart_file_offset(uint64_t va, size_t count) {
    if (!g_dart) return {};
    for (const auto &segment : g_dart->segments) {
        if (segment.type != nhk::elf::kProgramTypeLoad || va < segment.vaddr) continue;
        const uint64_t delta = va - segment.vaddr;
        if (delta > segment.filesz || count > segment.filesz - delta) continue;
        return segment.offset + delta;
    }
    return {};
}

/* Instruction words of a Dart function, read from the APK entry the image is mapped from. */
bool dart_words(uint32_t va, size_t count, std::vector<uint32_t> &out) {
    if (!g_dart || va == 0 || count == 0) return false;
    for (size_t attempt : {count, size_t{32}, size_t{24}, size_t{16}}) {
        if (attempt > count) continue;
        const auto file = dart_file_offset(va, attempt * 4);
        if (!file || g_dart->view_begin > UINT64_MAX - *file) continue;
        std::ifstream apk(g_dart->path, std::ios::binary);
        if (!apk) return false;
        out.assign(attempt, 0);
        apk.seekg(static_cast<std::streamoff>(g_dart->view_begin + *file));
        apk.read(reinterpret_cast<char *>(out.data()),
            static_cast<std::streamsize>(out.size() * 4));
        if (apk.gcount() == static_cast<std::streamsize>(out.size() * 4)) return true;
    }
    return false;
}

/*
 * Bind a Dart image VA to its runtime address, code source and instruction words. This is only used
 * for the two capture trampolines: the geometry fields no longer need a hook.
 */
bool bind_dart_target(uint32_t va, uintptr_t &address, nhk::CodeSource &source, Words &words) {
    if (!g_dart || va == 0 || g_dart->load_base > UINTPTR_MAX - va) return false;
    const auto file = dart_file_offset(va, sizeof(Words));
    if (!file || g_dart->view_begin > UINT64_MAX - *file) return false;
    address = static_cast<uintptr_t>(g_dart->load_base + va);
    const auto origin = nhk::source_at(g_dart->owned, address, sizeof(Words));
    if (!origin || origin->file_offset != *file) return false;
    std::ifstream apk(g_dart->path, std::ios::binary);
    if (!apk) return false;
    std::array<std::byte, sizeof(Words)> bytes{};
    apk.seekg(static_cast<std::streamoff>(g_dart->view_begin + *file));
    apk.read(reinterpret_cast<char *>(bytes.data()), static_cast<std::streamsize>(bytes.size()));
    if (apk.gcount() != static_cast<std::streamsize>(bytes.size())) return false;
    source = *origin;
    std::memcpy(words.data(), bytes.data(), sizeof(Words));
    return true;
}

bool bind_target(const Library &library, uint64_t va, uintptr_t &address,
    nhk::CodeSource &source, Words &words) {
    const auto at = library.at(va);
    const auto file = library.file_offset(va, sizeof(Words));
    if (!at || !file) return false;
    const auto origin = library.source(*at);
    if (!origin || origin->file_offset != *file) return false;
    address = *at;
    source = *origin;
    std::memcpy(words.data(), library.bytes.data() + *file, sizeof(Words));
    return true;
}

std::optional<Located> locate() {
    std::ifstream maps("/proc/self/maps");
    if (!maps) return {};
    const auto all = nhk::parse_file_mappings(maps);
    const auto path = launcher_apk(all);
    if (!path) return {};

    Located located;
    if (const auto rust = open_library(*path, "libapp_launcher.so", all)) {
        const auto image = home_layout::elf_targets::parse(rust->bytes);
        const auto targets = home_layout::elf_targets::resolve(rust->bytes);
        if (image && targets
            && bind_target(*rust, targets->cell_count_x, located.x, located.x_source,
                located.x_words)
            && bind_target(*rust, targets->cell_count_y, located.y, located.y_source,
                located.y_words)) {
            located.container_path = rust->path;
            located.view_begin = rust->view_begin;
            located.view_end = rust->view_end;
            __android_log_print(ANDROID_LOG_INFO, kTag,
                "layout rust targets X=%p Y=%p", reinterpret_cast<void *>(located.x),
                reinterpret_cast<void *>(located.y));
        }
    }
    if (ensure_dart_library()) {
        located.dart_container_path = g_dart->path;
        located.dart_view_begin = g_dart->view_begin;
        located.dart_view_end = g_dart->view_end;
    }
    if (located.x == 0 && located.dart_container_path.empty()) return {};
    return located;
}

void delay_ms(long milliseconds) {
    timespec delay{milliseconds / 1000, (milliseconds % 1000) * 1000000};
    while (nanosleep(&delay, &delay) != 0 && errno == EINTR) {}
}

/*
 * Wall clock for the worker's own throttles. CLOCK_MONOTONIC through the vDSO costs tens of
 * nanoseconds, which is what makes "ask at most once a second" cheaper than the question it guards.
 * Zero means the clock call failed: the callers read that as "not due yet" rather than as an
 * interval of zero, so a broken clock degrades to a slower check instead of a busy loop.
 */
uint64_t monotonic_ms() {
    timespec now{};
    if (clock_gettime(CLOCK_MONOTONIC, &now) != 0) return 0;
    return static_cast<uint64_t>(now.tv_sec) * 1000ULL
        + static_cast<uint64_t>(now.tv_nsec) / 1000000ULL;
}

/*
 * Throttle predicate for a wall-clock interval, with zero as the "never asked yet" sentinel.
 */
bool interval_due(uint64_t last, uint64_t now, uint64_t interval) {
    if (last == 0) return true;
    if (now == 0) return false;
    return now - last >= interval;
}

/*
 * Ask the panel state and answer whether the desktop can be drawn.
 *
 * Fail-open in both directions: built without the dock transport there is nobody to ask, and with it
 * a failed query keeps the transport's previous answer (which starts at "interactive"). A missing
 * endpoint therefore costs the saving, never the hooks.
 */
bool layout_panel_refresh_and_check() {
#if defined(HYPERCEILER_DOCK_NATIVE_MOTION)
    (void) refresh_dock_screen_state();
    return dock_motion_screen_active();
#else
    return true;
#endif
}

void *attempt_finished() {
    g_started.store(false, std::memory_order_release);
    return nullptr;
}

void push_tweaks(const home_layout::Config &values) {
    hometweaks::Config tweaks;
    tweaks.flags = 1u; // master on
    if (values.grid_enabled) tweaks.enabled.push_back(hometweaks::kFeaturePhoneGrid);
    if (values.tweaks.folder_enabled) tweaks.enabled.push_back(hometweaks::kFeatureFolderCols);
    if (values.tweaks.pad_enabled) tweaks.enabled.push_back(hometweaks::kFeaturePadGrid);
    if (values.tweaks.fold_enabled) tweaks.enabled.push_back(hometweaks::kFeatureFoldGrid);
    if (values.tweaks.icon_scale_enabled) tweaks.enabled.push_back(hometweaks::kFeatureIconSize);
    if (values.tweaks.recents_no_clear) tweaks.enabled.push_back(hometweaks::kFeatureNoClear);
    if (values.tweaks.recents_hide_clear) tweaks.enabled.push_back(hometweaks::kFeatureHideClear);
    tweaks.phoneCols = values.cell_x;
    tweaks.phoneRows = values.cell_y;
    tweaks.folderCols = values.tweaks.folder_cols;
    tweaks.padMajor = values.tweaks.pad_major;
    tweaks.padMinor = values.tweaks.pad_minor;
    tweaks.foldMajor = values.tweaks.fold_major;
    tweaks.foldMinor = values.tweaks.fold_minor;
    tweaks.iconScaleCode = values.tweaks.icon_scale_code;
    __android_log_print(ANDROID_LOG_INFO, kTag,
        "layout tweaks grid=%d/%dx%d folder=%d/%d pad=%d/%d fold=%d/%d icon=%d noClear=%d "
        "hideClear=%d", values.grid_enabled ? 1 : 0, values.cell_x, values.cell_y,
        values.tweaks.folder_enabled ? 1 : 0, values.tweaks.folder_cols,
        values.tweaks.pad_major, values.tweaks.pad_minor, values.tweaks.fold_major,
        values.tweaks.fold_minor, values.tweaks.icon_scale_code,
        values.tweaks.recents_no_clear ? 1 : 0, values.tweaks.recents_hide_clear ? 1 : 0);
    hometweaks::PushTweaksConfig(tweaks);
}

// ---------------------------------------------------------------------------
// Field path derivation.
// ---------------------------------------------------------------------------

constexpr uint32_t kDartPrologue = 0xA9BF79FDu;

bool is_ldur_double(uint32_t word) {
    return (word & 0xFFE00C00u) == 0xFC400000u;
}

bool is_ldur_word(uint32_t word) {
    return (word & 0xFFE00C00u) == 0xB8400000u;
}

bool is_add_heap(uint32_t word, uint32_t reg) {
    // add xR, xR, x28, lsl #32
    return word == (0x8B000000u | (28u << 16) | (reg << 5) | reg | (32u << 10));
}

int imm9(uint32_t word) {
    return static_cast<int>((word >> 12) & 0x1FFu);
}

uint32_t rn(uint32_t word) {
    return (word >> 5) & 0x1Fu;
}

uint32_t rt(uint32_t word) {
    return word & 0x1Fu;
}

bool bl_target(uint32_t word, uint32_t pc, uint32_t *target) {
    if ((word & 0xFC000000u) != 0x94000000u) return false;
    int64_t imm = static_cast<int32_t>(word & 0x03FFFFFFu);
    if ((imm & 0x02000000) != 0) imm -= 0x04000000;
    *target = static_cast<uint32_t>(static_cast<int64_t>(pc) + (imm << 2));
    return true;
}

struct FieldPath {
    uint8_t object = 0; // 1 config, 2 dock
    int off0 = -1;      // nested pointer offset, -1 for a direct field
    int off1 = -2;      // field offset, -2 unresolved
};

/* Pack a resolved path into the single word the trampoline reads. */
uint32_t pack_path(uint8_t object, int off0, int off1) {
    const uint32_t nested = static_cast<uint32_t>(off0 + 1) & 0xFFu;
    return static_cast<uint32_t>(object) | (nested << 8) | (static_cast<uint32_t>(off1) << 16);
}

/*
 * Find the field an accessor reads out of a captured object.
 *
 * The body is `bl <capture target>` followed by either `ldur d0, [x0, #off]` or
 * `ldur wN, [x0, #nested]` / `add xN, xN, x28, lsl #32` / `ldur d0, [xN, #off]`. The last such site
 * wins, because an accessor may call another accessor first (workspaceIndicatorMarginBottom calls
 * hotSeatsMarginBottom and then reads the config itself).
 */
bool decode_path(std::span<const uint32_t> code, uint32_t base, uint32_t config_va,
    uint32_t dock_va, FieldPath &path) {
    uint8_t object = 0;
    FieldPath best;
    for (size_t at = 0; at + 4 < code.size(); ++at) {
        uint32_t target = 0;
        if (!bl_target(code[at], base + static_cast<uint32_t>(at * 4), &target)) continue;
        if (target == config_va) object = 1;
        else if (dock_va != 0 && target == dock_va) object = 2;
        else continue;
        const size_t limit = std::min(code.size(), at + 6);
        for (size_t probe = at + 1; probe < limit; ++probe) {
            const uint32_t word = code[probe];
            if (is_ldur_double(word) && rn(word) == 0) {
                best = FieldPath{object, -1, imm9(word)};
                break;
            }
            if (is_ldur_word(word) && rn(word) == 0) {
                const uint32_t reg = rt(word);
                for (size_t nested = probe + 1; nested < std::min(code.size(), probe + 4); ++nested) {
                    if (is_add_heap(code[nested], reg)
                        && nested + 1 < code.size() && is_ldur_double(code[nested + 1])
                        && rn(code[nested + 1]) == reg) {
                        best = FieldPath{object, imm9(word), imm9(code[nested + 1])};
                        break;
                    }
                    if (is_ldur_double(code[nested]) && rn(code[nested]) == reg) {
                        best = FieldPath{object, imm9(word), imm9(code[nested])};
                        break;
                    }
                }
                break;
            }
        }
    }
    if (best.off1 == -2) return false;
    path = best;
    return true;
}

uint32_t g_config_capture_va = 0;
uint32_t g_dock_capture_va = 0;
uintptr_t g_config_capture_address = 0;
uintptr_t g_dock_capture_address = 0;
nhk::CodeSource g_config_capture_source{};
nhk::CodeSource g_dock_capture_source{};
Words g_config_capture_words{};
Words g_dock_capture_words{};
bool g_captures_armed = false;
bool g_probe_primed = false;
int g_device_object_offset = -1;

size_t bind_knobs() {
    if (!ensure_dart_library()) return 0;

    // The shipped hook targets, unless the calibration channel retargeted the knob already.
    for (size_t index = 0; index < g_knobs.size(); ++index) {
        if (kKnobHookSymbols[index] == nullptr || g_knobs[index].hook_mode) continue;
        g_knobs[index].hook_mode = true;
        g_knobs[index].symbol = kKnobHookSymbols[index];
    }

    // Hook-mode knobs: resolve the layout aggregator and bind its entry for the geometry trampoline.
    for (KnobRuntime &knob : g_knobs) {
        if (!knob.hook_mode || knob.hook_address != 0) continue;
        uint32_t va = 0;
        uint32_t size = 0;
        if (!hometweaks::HomeTweaksFindSymbol(knob.symbol.c_str(), &va, &size) || size < 16) {
            continue;
        }
        if (!bind_dart_target(va, knob.hook_address, knob.hook_source, knob.hook_words)) continue;
        if (knob.hook_words[0] != kDartPrologue) {
            knob.hook_address = 0;
            continue;
        }
        knob.getter = va;
        knob.getter_size = size;
    }
    if (!g_field_writes_enabled.load(std::memory_order_relaxed)) {
        size_t hooked = 0;
        for (const KnobRuntime &knob : g_knobs) {
            if (knob.hook_address != 0) ++hooked;
        }
        return hooked;
    }

    if (g_config_capture_va == 0) {
        uint32_t size = 0;
        hometweaks::HomeTweaksFindSymbol("GridController.currentConfig", &g_config_capture_va,
            &size);
    }
    if (g_dock_capture_va == 0) {
        uint32_t size = 0;
        hometweaks::HomeTweaksFindSymbol("HotSeatsConstants2._dockGridConfig", &g_dock_capture_va,
            &size);
    }
    if (g_config_capture_va == 0 || g_dock_capture_va == 0) return 0;

    // Pass one: accessors that reach a captured singleton, decoded into a field path.
    for (KnobRuntime &knob : g_knobs) {
        if (knob.hook_mode || knob.path.load(std::memory_order_relaxed) != 0) continue;
        uint32_t va = 0;
        uint32_t size = 0;
        if (!hometweaks::HomeTweaksFindSymbol(knob.symbol.c_str(), &va, &size) || size < 16) {
            continue;
        }
        std::vector<uint32_t> code;
        // Bound the scan by the symbol's own size: reading past the end would pick a neighbouring
        // function's field read, which is how searchBarWidthPx first decoded as searchBarWidthDeltaPx.
        const size_t words = std::clamp<size_t>(size / 4, 8, 48);
        if (!dart_words(va, words, code) || code.empty() || code[0] != kDartPrologue) continue;
        FieldPath path;
        if (!decode_path(code, va, g_config_capture_va, g_dock_capture_va, path)) continue;
        knob.getter = va;
        knob.path.store(pack_path(path.object, path.off0, path.off1), std::memory_order_release);
        if (path.off0 > 0 && g_device_object_offset < 0) g_device_object_offset = path.off0;
    }
    /*
     * There is deliberately no second pass.
     *
     * A fallback used to take the nested-object offset from a *different* accessor and combine it with
     * a receiver-relative field read, which produced a path nothing had proved: the offset belonged to
     * one object and the field to whatever the caller happened to pass. Writing an eight-byte double
     * through such a path corrupted the Dart heap and the launcher died inside libhyper_os_flutter.so.
     * A knob is now bound only when its own accessor decodes the whole path - a call to a captured
     * singleton followed by the field read - and is otherwise simply left unbound.
     */
    size_t bound = 0;
    for (const KnobRuntime &knob : g_knobs) {
        const bool ready = knob.hook_mode ? knob.hook_address != 0
                                          : knob.path.load(std::memory_order_relaxed) != 0;
        if (ready) ++bound;
    }
    return bound;
}

size_t arm_captures() {
    if (g_captures_armed) return 0;
    if (g_config_capture_va == 0 || g_dock_capture_va == 0) return 0;
    if (!bind_dart_target(g_config_capture_va, g_config_capture_address, g_config_capture_source,
            g_config_capture_words)) return 0;
    if (!bind_dart_target(g_dock_capture_va, g_dock_capture_address, g_dock_capture_source,
            g_dock_capture_words)) return 0;
    g_slots[kConfigCaptureSlot] = {g_config_capture_address,
        reinterpret_cast<void *>(hc_layout_config_capture_entry),
        &hc_layout_config_capture_original, g_config_capture_source, g_config_capture_words};
    g_slots[kDockCaptureSlot] = {g_dock_capture_address,
        reinterpret_cast<void *>(hc_layout_dock_capture_entry),
        &hc_layout_dock_capture_original, g_dock_capture_source, g_dock_capture_words};
    g_captures_armed = true;
    return 2;
}

/*
 * Install the hook trampolines for every knob calibrated onto a layout aggregator. A knob whose
 * aggregator is not resolved yet is simply skipped and retried by the health loop.
 */
size_t arm_hooks(std::vector<size_t> &order) {
    size_t added = 0;
    for (size_t index = 0; index < g_knobs.size(); ++index) {
        KnobRuntime &knob = g_knobs[index];
        if (!knob.hook_mode || knob.hook_address == 0 || knob.hook_armed) continue;
        const size_t slot = kKnobHookSlotBase + index;
        g_slots[slot] = {knob.hook_address, knob.hook_entry, knob.hook_original, knob.hook_source,
            knob.hook_words};
        order.push_back(slot);
        knob.hook_armed = true;
        ++added;
    }
    return added;
}

/*
 * Publish the hook delta only after its slot is installed: a knob whose hook was refused keeps a zero
 * delta and a cleared enable flag, so the launcher behaves exactly as unpatched.
 */
size_t publish_hooks() {
    size_t live = 0;
    for (size_t index = 0; index < g_knobs.size(); ++index) {
        KnobRuntime &knob = g_knobs[index];
        if (!knob.hook_mode || knob.hook_enabled == nullptr || knob.hook_delta == nullptr) continue;
        const int delta = knob.delta_px.load(std::memory_order_relaxed);
        if (delta == 0 || !knob.hook_armed) {
            *knob.hook_enabled = 0;
            continue;
        }
        // The slider is in the user's units; the accessor is not always, so the gain is what makes a
        // step read the way the label says (see kKnobDeltaGain).
        const double value = static_cast<double>(delta) * kKnobDeltaGain[index];
        uint64_t bits = 0;
        std::memcpy(&bits, &value, sizeof(bits));
        *knob.hook_delta = bits;
        *knob.hook_enabled = 1;
        ++live;
    }
    return live;
}

/*
 * Write every requested delta into the captured object.
 *
 * The launcher owns the field and can rewrite it at any time (rotation, density change, a settings
 * change that rebuilds the config), so the write is self-healing: the launcher's own value is
 * remembered as `base`, and a field that no longer carries `base + applied_delta` is treated as a
 * fresh launcher value. Switching a knob off stops writing, which leaves the launcher's own value in
 * place.
 */
size_t apply_knob_fields() {
    const uintptr_t config = static_cast<uintptr_t>(hc_layout_config_object);
    const uintptr_t dock = static_cast<uintptr_t>(hc_layout_dock_object);
    const uint64_t heap = hc_layout_heap_base;
    size_t applied = 0;
    for (KnobRuntime &knob : g_knobs) {
        const uint32_t packed = knob.path.load(std::memory_order_acquire);
        if (packed == 0) continue;
        const int delta = knob.delta_px.load(std::memory_order_relaxed);
        const uint8_t object = packed & 0xFFu;
        const int off0 = static_cast<int>((packed >> 8) & 0xFFu) - 1;
        const int off1 = static_cast<int>((packed >> 16) & 0xFFFFu);
        uintptr_t base_object = object == 1 ? config : dock;
        if (base_object == 0) continue;
        uintptr_t target = base_object;
        if (off0 >= 0) {
            if (heap == 0) continue;
            uint32_t compressed = 0;
            std::memcpy(&compressed, reinterpret_cast<const void *>(target + off0), 4);
            if (compressed == 0) continue;
            target = static_cast<uintptr_t>(compressed) + (heap << 32);
        }
        auto *field = reinterpret_cast<double *>(target + off1);
        const double current = *field;
        if (delta == 0) {
            // Switched off: remember the launcher's own value and leave the field alone.
            knob.pristine = current;
            knob.pristine_valid = true;
            continue;
        }
        if (!knob.pristine_valid) {
            knob.pristine = current;
            knob.pristine_valid = true;
        }
        const double wanted = knob.pristine + static_cast<double>(delta);
        /*
         * Backstop. Every legitimate request is bounded by the settings page's dp range times the
         * display density, so a value outside this window means the field is not what it was assumed
         * to be - refuse it instead of letting one bad write take the launcher's layout out.
         */
        if (!(wanted > -4000.0 && wanted < 4000.0)) continue;
        if (current != wanted) *field = wanted;
        ++applied;
    }
    return applied;
}

/*
 * Caller histogram: which Dart functions ask for the config while the desktop lays out. Calibration
 * only - it turns "the geometry is computed somewhere in here" into a concrete list of functions to
 * hook, without guessing. Fixed-size and lock-free; a collision just replaces a sample.
 */
constexpr size_t kCallerSlots = 256;
uint64_t g_caller_key[kCallerSlots] = {};
uint32_t g_caller_count[kCallerSlots] = {};

void hc_layout_record_caller(uintptr_t caller) {
    if (caller == 0) return;
    const size_t slot = (caller >> 2) & (kCallerSlots - 1);
    if (g_caller_key[slot] != caller) {
        g_caller_key[slot] = caller;
        g_caller_count[slot] = 0;
    }
    ++g_caller_count[slot];
}

/*
 * The trampoline entry point: runs on the Dart thread, inside the launcher's own call, so it must do
 * nothing but a handful of field writes. There is no allocation, no lock and no logging here.
 *
 * Only the trampoline calls this. The background worker deliberately does not: two writers racing on
 * the same self-healing bookkeeping would corrupt `base`, and the trampoline already covers every
 * moment the launcher can read a field.
 */
extern "C" void hc_layout_apply_now(uint64_t config, uintptr_t caller) {
    hc_layout_record_caller(caller);
    (void) config;
    apply_knob_fields();
}

void *worker(void *) {
    /*
     * Named for field triage. The audit that produced this worker's power fix could not attribute
     * this loop to anything: it saw five unnamed threads in the launcher and no way to tell which
     * one was ours, which is the difference between "the loop does not run" and "the loop runs but
     * says nothing". One line costs less than that investigation did.
     */
    (void) pthread_setname_np(pthread_self(), "hc-home-layout");
    home_layout::Config config;
    bool queried = false;
    for (int attempt = 0; attempt < 20; ++attempt) {
        if (home_layout::query_config(config)) { queried = true; break; }
        delay_ms(100);
    }
    if (!queried) {
        __android_log_print(ANDROID_LOG_WARN, kTag,
            "layout config unavailable; leaving launcher unmodified");
        return attempt_finished();
    }
    auto sync_requested = [&]() {
        bool any = false;
        for (size_t index = 0; index < HC_LAYOUT_KNOB_COUNT; ++index) {
            const bool enabled = config.knobs[index].enabled;
            g_knobs[index].delta_px.store(enabled ? config.knobs[index].delta_px : 0,
                std::memory_order_relaxed);
            any = any || enabled;
        }
        return any;
    };
    bool any_knob = sync_requested();
    /*
     * Stage-one probe: hook the workspace top padding accessor and change nothing.
     *
     * It answers the questions a value change cannot: is the hook itself stable, how often is the
     * function called, what does the launcher really return, and which Dart function is asking. Only
     * after that is a value change worth trying - and only a tiny one.
     */
    char probe_top[PROP_VALUE_MAX] = {};
    const bool top_probe =
        __system_property_get("debug.hyperceiler.layout.probe_top", probe_top) > 0
        && probe_top[0] == '1';
    if (top_probe) {
        g_knobs[2].hook_mode = true;
        g_knobs[2].symbol = "GridSizeCalRules.stableWorkspaceCellPaddingTop";
        g_knobs[2].delta_px.store(0, std::memory_order_relaxed);
        /* Minimal stub: proves whether the hook itself can run here at all. */
        g_knobs[2].hook_entry = reinterpret_cast<void *>(hc_layout_passthrough_entry);
        g_knobs[2].hook_original = &hc_layout_passthrough_original;
    }

    /*
     * Whether any code-patch feature (the tweaks half of the panel) asked for something. The gate
     * below used to check only the grid and the knobs, so a desktop with only, say, the icon scale
     * enabled pushed no config at all: every tweaks feature silently read as "not enabled" no
     * matter what the page said - which is exactly how "folder columns / icon scale do nothing"
     * presented on a live device. The tweaks pipeline consumes this same Config, so its ask
     * belongs in this gate, not after it.
     */
    const bool any_tweak = config.tweaks.folder_enabled || config.tweaks.pad_enabled
        || config.tweaks.fold_enabled || config.tweaks.icon_scale_enabled
        || config.tweaks.recents_hide_clear || config.tweaks.recents_no_clear;
    if (!config.grid_enabled && !any_knob && !top_probe && !any_tweak) {
        __android_log_print(ANDROID_LOG_INFO, kTag, "layout preferences disabled; no hooks installed");
        return attempt_finished();
    }
    g_cell_x.store(config.cell_x, std::memory_order_relaxed);
    g_cell_y.store(config.cell_y, std::memory_order_relaxed);
    push_tweaks(config);

    std::optional<Located> located;
    for (int attempt = 0; attempt < 60; ++attempt) {
        located = locate();
        if (located) break;
        delay_ms(100);
    }
    if (!located) {
        /*
         * The geometry sites are only the first two slots (the grid cell replacements). A launcher
         * build whose xref sites moved costs the grid knobs, not the whole worker: the hook-mode
         * knobs below are symbol-resolved against the same image and can still bind, which is
         * exactly what a probe run on a moved launcher needs. The 6309→7654-260904 rollback hit
         * this: tweaks patches landed (they are symbol-named) while the worker quit here and the
         * probe hooks never armed.
         */
        __android_log_print(ANDROID_LOG_WARN, kTag,
            "layout targets unavailable; continuing with hook knobs only");
    } else {
        g_container_path = located->container_path;
        g_view_begin = located->view_begin;
        g_view_end = located->view_end;
        g_dart_container_path = located->dart_container_path;
        g_dart_view_begin = located->dart_view_begin;
        g_dart_view_end = located->dart_view_end;
    }

    /*
     * Calibration channel, read once per process and gated on `debug.hyperceiler.layout.override=1`.
     *
     * The gate is not cosmetic: `debug.hyperceiler.layout.symbols` / `hook0..7` / `raw0..7` retarget a
     * knob, and while they were read unconditionally a leftover property from an earlier calibration
     * run silently overrode the shipped table - which is exactly how "the settings page does nothing"
     * was produced once already. With the gate, an unset or stale property cannot influence production
     * behaviour at all.
     */
    char debug_gate[PROP_VALUE_MAX] = {};
    const bool debug_channel =
        __system_property_get("debug.hyperceiler.layout.override", debug_gate) > 0
        && debug_gate[0] == '1';
    if (debug_channel) {
        __android_log_print(ANDROID_LOG_WARN, kTag,
            "layout calibration channel is ON; preferences are overridden by debug properties");
        char probe[PROP_VALUE_MAX] = {};
        if (__system_property_get("debug.hyperceiler.layout.probe", probe) > 0
            && probe[0] == '1') {
            for (size_t index = 0; index < g_knobs.size(); ++index) {
                char name[PROP_VALUE_MAX] = {};
                const std::string key = "debug.hyperceiler.layout.probe" + std::to_string(index);
                if (__system_property_get(key.c_str(), name) <= 0) continue;
                if (name[0] != '\0') g_knobs[index].symbol = name;
            }
        } else {
            char value[PROP_VALUE_MAX] = {};
            if (__system_property_get("debug.hyperceiler.layout.symbols", value) > 0) {
                const std::string_view text(value);
                size_t cursor = 0;
                for (size_t index = 0; index < g_knobs.size(); ++index) {
                    const size_t comma = text.find(',', cursor);
                    const std::string_view entry = text.substr(cursor,
                        comma == std::string_view::npos ? std::string_view::npos : comma - cursor);
                    if (!entry.empty()) g_knobs[index].symbol = std::string(entry);
                    if (comma == std::string_view::npos) break;
                    cursor = comma + 1;
                }
            }
        }
        /*
         * `debug.hyperceiler.layout.hook0..hook7` switch a single knob to the hook strategy and name
         * the layout aggregator it should hook. This is the calibration path for the values whose
         * accessor Dart inlines away.
         */
        for (size_t index = 0; index < g_knobs.size(); ++index) {
            char name[PROP_VALUE_MAX] = {};
            const std::string key = "debug.hyperceiler.layout.hook" + std::to_string(index);
            if (__system_property_get(key.c_str(), name) <= 0 || name[0] == '\0') continue;
            // "-" is the off switch: a system property cannot be unset from adb, so a knob has to be
            // able to leave hook mode again without a reboot.
            if (name[0] == '-' || std::strcmp(name, "off") == 0) {
                /*
                 * The off switch has to actually leave hook mode: skipping the assignment kept a
                 * knob hooked under its previous target forever, and a later arm attempt would
                 * keep installing a site the calibration had already abandoned.
                 */
                if (g_knobs[index].hook_mode) {
                    g_knobs[index].hook_mode = false;
                    g_knobs[index].hook_address = 0;
                    g_knobs[index].hook_armed = false;
                    g_knobs[index].symbol.clear();
                }
                continue;
            }
            g_knobs[index].hook_mode = true;
            g_knobs[index].symbol = name;
        }
        /*
         * There is deliberately no "write this offset" calibration property here any more. Such a
         * channel was used once to sweep the configuration object for a field, and because it wrote an
         * eight-byte double at offsets that are not all doubles it corrupted the Dart heap: the
         * launcher then died inside libhyper_os_flutter.so on a DartWorker thread. A field path may
         * only ever come from decoding a `ldur d0, [..]` in the launcher's own accessor, which is what
         * bind_knobs does; a free-form offset has no place in a release build.
         */
    }
    /*
     * Unconditional. These pointers used to be filled only inside the calibration branch, so with the
     * channel closed every hook knob had a null `hook_enabled` and `publish_hooks` wrote through it -
     * a null-pointer write on the worker thread, which took the whole launcher process down. Every
     * crash that was blamed on the hook itself was this line.
     */
    init_knob_hooks();

    const auto knob_ready = [](const KnobRuntime &knob) {
        return knob.hook_mode ? knob.hook_address != 0
                              : knob.path.load(std::memory_order_relaxed) != 0;
    };
    const auto all_bound = [&]() {
        return std::all_of(g_knobs.begin(), g_knobs.end(), knob_ready);
    };
    /*
     * Resolve before the desktop computes its layout. The snapshot is mapped shortly before Dart runs
     * its one-time layout, so the wait is short and tight; anything resolved after that layout is only
     * seen once something forces the desktop to lay out again.
     */
    for (int attempt = 0; attempt < 200 && !all_bound(); ++attempt) {
        if (bind_knobs() == g_knobs.size() || all_bound()) break;
        delay_ms(25);
    }
    bind_knobs();

    std::vector<size_t> order;
    if (config.grid_enabled && located && located->x != 0 && located->y != 0) {
        g_slots[0] = {located->x, reinterpret_cast<void *>(cell_x_replacement), &g_original_x,
            located->x_source, located->x_words};
        g_slots[1] = {located->y, reinterpret_cast<void *>(cell_y_replacement), &g_original_y,
            located->y_source, located->y_words};
        order.push_back(0);
        order.push_back(1);
    }
    /*
     * The field-rewriting path is opt-in while it is still being validated, and off by default.
     *
     * It writes into the launcher's own live objects, so a wrong path is a launcher crash rather than
     * a no-op; a build that could do that without being asked is not shippable. `debug.hyperceiler.
     * layout.knobs_enable=1` is the explicit request.
     */
    char knobs_gate[PROP_VALUE_MAX] = {};
    const bool field_writes =
        __system_property_get("debug.hyperceiler.layout.knobs_enable", knobs_gate) > 0
        && knobs_gate[0] == '1';
    g_field_writes_enabled.store(field_writes, std::memory_order_relaxed);
    if ((any_knob || top_probe) && field_writes) {
        if (arm_captures() != 0) {
            order.push_back(kConfigCaptureSlot);
            order.push_back(kDockCaptureSlot);
        }
    }
    arm_hooks(order);
    publish_hooks();
    const bool grid_live =
        config.grid_enabled && located && located->x != 0 && located->y != 0;
    g_ready.store(grid_live, std::memory_order_release);
    g_dart_ready.store(g_captures_armed || !order.empty(), std::memory_order_release);
    {
        size_t bound = 0;
        for (const KnobRuntime &knob : g_knobs) {
            if (knob_ready(knob)) ++bound;
        }
        __android_log_print(ANDROID_LOG_INFO, kTag,
            "layout hooks live grid=%d cell=%dx%d knobs=%zu/%zu captures=%d age=%llums",
            config.grid_enabled ? 1 : 0, config.cell_x, config.cell_y, bound, g_knobs.size(),
            g_captures_armed ? 1 : 0, static_cast<unsigned long long>(process_age_or_zero()));
        for (const KnobRuntime &knob : g_knobs) {
            const uint32_t packed = knob.path.load(std::memory_order_relaxed);
            __android_log_print(ANDROID_LOG_INFO, kTag,
                "layout knob %s mode=%s va=%#x sym_size=%u w0=%08x addr=%p object=%u off0=%d "
                "off1=%d delta=%d",
                knob.symbol.c_str(), knob.hook_mode ? "hook" : "field", knob.getter,
                knob.getter_size, knob.hook_words[0],
                reinterpret_cast<void *>(knob.hook_address), packed & 0xFFu,
                static_cast<int>((packed >> 8) & 0xFFu) - 1,
                static_cast<int>((packed >> 16) & 0xFFFFu),
                knob.delta_px.load(std::memory_order_relaxed));
        }
    }

    int iterations = 0;
    /*
     * Worker-local throttle state. Both intervals are wall-clock rather than iteration counts because
     * the iteration itself changes length once the panel is dozing.
     */
    uint64_t last_panel_query = 0;
    uint64_t last_generation_check = 0;
    for (;;) {
        const uint64_t now = monotonic_ms();
        /*
         * Panel gate, the same one the dock maintenance worker uses. While the panel dozes the
         * desktop is not drawn, so not one patched call site can run and there is nothing to
         * maintain - yet this loop kept asking system_server for its configuration over a synchronous
         * Binder transaction and re-verifying every slot, twice a second, for the whole night. The
         * dock measured that exact pattern as essentially the entire cost of the launcher process
         * while the screen was off.
         *
         * Only a *fresh* answer skips a pass, so a stale "dozing" cannot stop maintenance: the loop
         * is asleep for kDozingIterationMs, which is also what makes the query itself one call per
         * that interval while dozing.
         */
        if (interval_due(last_panel_query, now, kPanelQueryIntervalMs)) {
            last_panel_query = now;
            if (!layout_panel_refresh_and_check()) {
                /*
                 * A shorter doze pause than the dock's 30 s. The dock maintains a hook chain that
                 * only matters while a gesture is running, while this one owns on-screen geometry: a
                 * patch the kernel refilled overnight has to be back before the desktop is laid out
                 * again, and while dozing a pass is a handful of word reads, so the tighter bound
                 * costs nothing.
                 */
                delay_ms(kDozingIterationMs);
                continue;
            }
        }
        delay_ms(kPassIntervalMs);
        ++iterations;
        /*
         * Probe readout: the passthrough stub records every call into these globals, and this is
         * where they get printed. The caller is reported as an image-relative address so it can be
         * named straight from the symbol table, without keeping a load base anywhere else.
         */
        {
            static uint32_t probe_reported = 0;
            const uint32_t probe_hits = hc_layout_probe_hits;
            if (probe_hits != probe_reported) {
                probe_reported = probe_hits;
                double probe_value = 0;
                std::memcpy(&probe_value, &hc_layout_probe_value, sizeof(probe_value));
                const uint64_t caller = hc_layout_probe_caller;
                const uint64_t in_image =
                    g_dart && caller >= g_dart->load_base ? caller - g_dart->load_base : 0;
                __android_log_print(ANDROID_LOG_INFO, kTag,
                    "layout probe hits=%u value=%f caller=0x%llx (image 0x%llx)", probe_hits,
                    probe_value, static_cast<unsigned long long>(caller),
                    static_cast<unsigned long long>(in_image));
            }
        }
        if (iterations % 10 == 0) {
            home_layout::Config latest;
            if (home_layout::query_config(latest)) {
                if (latest.grid_enabled && located && located->x != 0 && located->y != 0
                    && std::find(order.begin(), order.end(), size_t{0}) == order.end()) {
                    g_slots[0] = {located->x, reinterpret_cast<void *>(cell_x_replacement),
                        &g_original_x, located->x_source, located->x_words};
                    g_slots[1] = {located->y, reinterpret_cast<void *>(cell_y_replacement),
                        &g_original_y, located->y_source, located->y_words};
                    order.push_back(0);
                    order.push_back(1);
                    g_ready.store(true, std::memory_order_release);
                }
                const bool cell_changed = latest.cell_x != config.cell_x
                    || latest.cell_y != config.cell_y || latest.grid_enabled != config.grid_enabled;
                const bool knobs_changed = latest.knobs != config.knobs;
                const bool tweaks_changed = latest.tweaks != config.tweaks;
                config = latest;
                if (cell_changed) {
                    g_cell_x.store(config.cell_x, std::memory_order_relaxed);
                    g_cell_y.store(config.cell_y, std::memory_order_relaxed);
                    g_ready.store(config.grid_enabled && located && located->x != 0
                        && located->y != 0, std::memory_order_release);
                }
                if (tweaks_changed) push_tweaks(config);
                (void) knobs_changed;
            }
            any_knob = sync_requested();
            if (arm_hooks(order) != 0 || publish_hooks() != 0) {
                g_dart_ready.store(true, std::memory_order_release);
            }
        }
        if (!all_bound()) bind_knobs();
        if (any_knob && !g_captures_armed) {
            if (arm_captures() != 0) {
                order.push_back(kConfigCaptureSlot);
                order.push_back(kDockCaptureSlot);
                g_dart_ready.store(true, std::memory_order_release);
            }
        }
        if (arm_hooks(order) != 0) publish_hooks();
        if (iterations % 20 == 0) {
            size_t bound = 0;
            int deltas = 0;
            for (const KnobRuntime &knob : g_knobs) {
                if (knob_ready(knob)) ++bound;
                if (knob.delta_px.load(std::memory_order_relaxed) != 0) ++deltas;
            }
            __android_log_print(ANDROID_LOG_INFO, kTag,
                "layout captures config=%#llx dock=%#llx heap=%#llx hits=%llu/%llu bound=%zu "
                "deltas=%d",
                static_cast<unsigned long long>(hc_layout_config_object),
                static_cast<unsigned long long>(hc_layout_dock_object),
                static_cast<unsigned long long>(hc_layout_heap_base),
                static_cast<unsigned long long>(hc_layout_config_capture_hits),
                static_cast<unsigned long long>(hc_layout_dock_capture_hits), bound, deltas);
            /*
             * Per-knob probe readout. `hits` alone can only say "the hook runs"; the verdict a
             * read-only probe has to support is "this function is the control point for that value",
             * which needs the launcher's own return value and the caller as well. `last` is the raw
             * double the patched call returned, `caller` the Dart return address (reported as an
             * image VA so the symbol table can name it), `d` the delta currently requested for the
             * knob (zero until a preference enables it) - so a passthrough probe is distinguishable
             * from an active one at a glance.
             */
            std::string hits;
            for (const KnobRuntime &knob : g_knobs) {
                if (!knob.hook_mode || knob.hook_hits == nullptr) continue;
                double last = 0;
                uint64_t caller = 0;
                if (knob.hook_last != nullptr) {
                    std::memcpy(&last, knob.hook_last, sizeof(last));
                }
                if (knob.hook_caller != nullptr) caller = *knob.hook_caller;
                const uint64_t in_image =
                    g_dart && caller >= g_dart->load_base ? caller - g_dart->load_base : 0;
                char entry[256] = {};
                snprintf(entry, sizeof(entry), " %s=%llu last=%.4f caller=%#llx d=%d",
                    knob.symbol.c_str(), static_cast<unsigned long long>(*knob.hook_hits), last,
                    static_cast<unsigned long long>(in_image),
                    knob.delta_px.load(std::memory_order_relaxed));
                hits += entry;
            }
            if (!hits.empty()) {
                __android_log_print(ANDROID_LOG_INFO, kTag, "layout hook hits%s", hits.c_str());
            }
            // Caller histogram, as image VAs so the offline symbol table can name them.
            const uint64_t load_base = g_dart ? g_dart->load_base : 0;
            std::vector<std::pair<uint32_t, uint32_t>> callers;
            callers.reserve(kCallerSlots);
            for (size_t slot = 0; slot < kCallerSlots; ++slot) {
                if (g_caller_count[slot] == 0) continue;
                const uintptr_t caller = static_cast<uintptr_t>(g_caller_key[slot]);
                if (caller < load_base) continue;
                callers.emplace_back(static_cast<uint32_t>(caller - load_base), g_caller_count[slot]);
            }
            std::sort(callers.begin(), callers.end(),
                [](const auto &a, const auto &b) { return a.second > b.second; });
            for (size_t i = 0; i < callers.size() && i < 12; ++i) {
                __android_log_print(ANDROID_LOG_INFO, kTag, "layout caller va=%#x count=%u",
                    callers[i].first, callers[i].second);
            }
        }
        /*
         * Health pass. The steady state is one word read per armed slot - no mapping inventory - so
         * the cadence above is no longer what this loop costs, it only bounds how long a lost patch
         * can stay lost.
         */
        bool live = bank_live(order);
        /*
         * Periodic generation proof, at the backstop cadence rather than per pass: one inventory for
         * every armed slot instead of one per slot. A read failure here is treated like any other
         * unproven state and falls into the repair below, exactly as the per-slot validated read did.
         */
        if (live && interval_due(last_generation_check, now, kGenerationCheckMs)) {
            last_generation_check = now;
            const auto inventory = current_mappings();
            live = inventory.has_value() && bank_generation_holds(order, *inventory);
        }
        if (!live) {
            /*
             * The validated repair - the only place in this loop that builds a mapping inventory.
             * It used to be the steady-state check as well, which is what made a healthy desktop pay
             * for twenty-four inventory parses per pass; now it runs when a patch was actually lost,
             * or when the bank has not been installed yet.
             */
            if (nhk::ensure_slots_live(g_slots, slot_host(), order) && bank_live(order)) {
                live = true;
                __android_log_print(ANDROID_LOG_INFO, kTag, "layout hook bank re-armed");
            }
        }
        if (!live) {
            g_ready.store(false, std::memory_order_release);
            g_dart_ready.store(false, std::memory_order_release);
            g_captures_armed = false;
            __android_log_print(ANDROID_LOG_ERROR, kTag,
                "layout hook bank unhealthy; stopped overriding");
            return attempt_finished();
        }
    }
}
} // namespace

/*
 * Install the stage-one probe synchronously, from the loader callback for libapp.so.
 *
 * The probe's target is called exactly once, during start-up. The ordinary path resolves the symbol
 * table (an xz decode plus a 74k-symbol scan, tens of milliseconds) on a background thread while the
 * launcher keeps running, so the patch could land while that single call was executing - a torn
 * instruction stream, which is one of the two ways the launcher died. Doing the resolution here, in
 * the dlopen callback, is before the Dart runtime runs at all, so the function is patched before it
 * can ever be called.
 */
/*
 * Adopt the home-layout state for this process, resetting anything inherited across a fork.
 *
 * Every flag below is process-global and is inherited by a forked child, while the thread that
 * produced it is not. HYOS forks the desktop out of a process that shares this module's code and its
 * command line, so a desktop can start life already holding the parent's "worker is running" or
 * "attempts exhausted" verdict - and then refuse to create a worker for the rest of its life.
 *
 * Adoption is pid-stamped, so it is a no-op everywhere except the first call in a process: the
 * explicit call at each start site and the one inside start_home_layout_hooks cannot double-reset a
 * live desktop, and a start site that forgets to prepare cannot wedge the desktop either. Returns
 * true when this call was the one that adopted (and therefore reset) the state.
 *
 * The reset body is the one that used to live in home_layout_prepare_for_launcher_child: the spawner
 * shares this code and burns worker attempts and arm flags on its own behalf, which was observed live
 * as a launcher with no layout logs at all.
 */
std::atomic<pid_t> g_state_owner{0};

bool adopt_layout_state() {
    const pid_t self = getpid();
    if (g_state_owner.load(std::memory_order_acquire) == self) return false;
    g_state_owner.store(self, std::memory_order_release);

    g_started.store(false, std::memory_order_release);
    g_attempts.store(0, std::memory_order_release);
    g_ready.store(false, std::memory_order_release);
    g_dart_ready.store(false, std::memory_order_release);
    g_field_writes_enabled.store(false, std::memory_order_release);
    g_cell_x.store(0, std::memory_order_relaxed);
    g_cell_y.store(0, std::memory_order_relaxed);
    g_captures_armed = false;
    g_probe_primed = false;
    g_hook_globals_inited = false;
    init_knob_hooks();
    g_dart.reset();
    g_config_capture_va = 0;
    g_dock_capture_va = 0;
    g_config_capture_address = 0;
    g_dock_capture_address = 0;
    hc_layout_config_object = 0;
    hc_layout_config_capture_hits = 0;
    g_dart_container_path.clear();
    g_dart_view_begin = 0;
    g_dart_view_end = 0;
    g_container_path.clear();
    g_view_begin = 0;
    g_view_end = 0;
    for (Slot &slot : g_slots) slot = Slot{};
    for (KnobRuntime &knob : g_knobs) {
        knob.hook_mode = false;
        knob.hook_address = 0;
        knob.hook_armed = false;
        knob.symbol.clear();
        knob.path.store(0, std::memory_order_relaxed);
        knob.getter = 0;
        knob.getter_size = 0;
        knob.pristine_valid = false;
        knob.delta_px.store(0, std::memory_order_relaxed);
    }
    return true;
}

void home_layout_prepare_for_launcher_child() {
    (void) adopt_layout_state();
}

/*
 * Bind and arm the geometry hooks inside the same callback window as the probe. The desktop computes
 * its geometry once, right after this library loads; a hook installed after that point is never
 * called again, which is exactly how a "successfully armed" knob ends up invisible on screen.
 */
void prime_home_layout_knobs(HookFunction hook, UnhookFunction unhook) {
    if (hook == nullptr) return;
    g_hook_function = hook;
    g_unhook_function = unhook;
    if (!ensure_dart_library()) return;
    // Shipped hook targets, the same pass the worker runs later.
    for (size_t index = 0; index < g_knobs.size(); ++index) {
        if (kKnobHookSymbols[index] == nullptr || g_knobs[index].hook_mode) continue;
        g_knobs[index].hook_mode = true;
        g_knobs[index].symbol = kKnobHookSymbols[index];
    }
    (void)bind_knobs();
    /*
     * The symbol table is parsed on first use and the monitor thread may have started that parse a
     * moment earlier; until it finishes, every lookup misses. A bounded wait here is what makes the
     * callback actually beat the first layout instead of leaving it to the slower worker.
     */
    for (int attempt = 0; attempt < 25; ++attempt) {
        bool pending = false;
        for (const KnobRuntime &knob : g_knobs) {
            if (knob.hook_mode && knob.hook_address == 0) pending = true;
        }
        if (!pending) break;
        delay_ms(20);
        (void)bind_knobs();
    }
    /*
     * Pull the deltas here as well: the very first layout should already run on the user's values
     * instead of waiting for the slower worker path to publish them.
     */
    home_layout::Config config;
    if (home_layout::query_config(config)) {
        for (size_t index = 0; index < HC_LAYOUT_KNOB_COUNT; ++index) {
            g_knobs[index].delta_px.store(config.knobs[index].delta_px,
                std::memory_order_relaxed);
        }
    }
    static std::vector<size_t> order; // callback-owned, the same pattern as the probe
    const size_t added = arm_hooks(order);
    if (added == 0 && order.empty()) return;
    /*
     * One slot per ensure call, each with its own verdict. A batched ensure returns a single false
     * for the whole order, which hides which address the bank refused - the difference between a
     * page that cannot be written and a prologue that cannot be replayed is exactly the diagnosis.
     */
    for (const size_t index : order) {
        Slot &slot = g_slots[index];
        Words live{};
        const bool read_ok = stable_read(slot, live);
        const bool same = read_ok && live == slot.original_words;
        __android_log_print(ANDROID_LOG_INFO, kTag,
            "layout slot %zu addr=%p read=%d same=%d live0=%08x orig0=%08x", index,
            reinterpret_cast<void *>(slot.address), read_ok, same, read_ok ? live[0] : 0,
            slot.original_words[0]);
        std::vector<size_t> single{index};
        const bool one = nhk::ensure_slots_live(g_slots, slot_host(), single);
        __android_log_print(ANDROID_LOG_INFO, kTag, "layout slot %zu live=%d", index, one ? 1 : 0);
        if (!one) {
            for (KnobRuntime &knob : g_knobs) {
                if (knob.hook_mode && knob.hook_armed &&
                    kKnobHookSlotBase + (&knob - g_knobs.data()) == index) {
                    knob.hook_armed = false;
                }
            }
        }
    }
    publish_hooks();
}

void prime_home_layout_probe(HookFunction hook, UnhookFunction unhook) {
    /*
     * Idempotent: the loader reports libapp.so more than once. Overwriting an already registered slot
     * reset its bookkeeping and made the bank install a second hook on the same address, which killed
     * the launcher - the hook itself had installed cleanly on the first call (live=1).
     */
    if (g_knobs[2].hook_armed || g_probe_primed) return;
    char probe[PROP_VALUE_MAX] = {};
    if (__system_property_get("debug.hyperceiler.layout.probe_top", probe) <= 0
        || probe[0] != '1') return;
    if (hook == nullptr) return;
    g_hook_function = hook;
    g_unhook_function = unhook;
    if (!ensure_dart_library()) return;
    g_knobs[2].hook_mode = true;
    g_knobs[2].symbol = "GridSizeCalRules.stableWorkspaceCellPaddingTop";
    g_knobs[2].hook_entry = reinterpret_cast<void *>(hc_layout_passthrough_entry);
    g_knobs[2].hook_original = &hc_layout_passthrough_original;
    uint32_t va = 0;
    uint32_t size = 0;
    if (!hometweaks::HomeTweaksFindSymbol(g_knobs[2].symbol.c_str(), &va, &size) || size < 16) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "layout probe: symbol not found");
        return;
    }
    if (!bind_dart_target(va, g_knobs[2].hook_address, g_knobs[2].hook_source,
            g_knobs[2].hook_words)) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "layout probe: bind failed va=%#x", va);
        return;
    }
    /*
     * Fill the same two diagnostic fields bind_knobs fills, so this slot's log line reports the
     * address it actually hooked instead of a zero. The worker's per-knob line is the one place a
     * reader checks "what did we hook, in which build" - a zero there is worse than useless.
     */
    g_knobs[2].getter = va;
    g_knobs[2].getter_size = size;
    static std::vector<size_t> order;
    const size_t slot = kKnobHookSlotBase + 2;
    g_slots[slot] = {g_knobs[2].hook_address, g_knobs[2].hook_entry, g_knobs[2].hook_original,
        g_knobs[2].hook_source, g_knobs[2].hook_words};
    order.push_back(slot);
    g_knobs[2].hook_armed = true;
    g_probe_primed = true;
    const bool live = nhk::ensure_slots_live(g_slots, slot_host(), order);
    __android_log_print(ANDROID_LOG_WARN, kTag,
        "layout probe primed va=%#x addr=%p live=%d", va,
        reinterpret_cast<void *>(g_knobs[2].hook_address), live ? 1 : 0);
}

/*
 * One line per distinct start verdict per process.
 *
 * Every gate below used to return without a word, which made "this desktop has no layout worker"
 * indistinguishable from "the worker runs and says nothing" - the question POWER_AUDIT_20260917 §5.1
 * could not answer, and the one a whole device session was spent on. Each verdict is reported once,
 * because the property hook that calls this fires on every property read and would otherwise turn
 * the instrumentation into a log storm.
 */
uint32_t start_verdict_bit(const char *verdict) {
    if (verdict == nullptr) return 1u << 0;
    if (std::strcmp(verdict, "hook-null") == 0) return 1u << 1;
    if (std::strcmp(verdict, "ready") == 0) return 1u << 2;
    if (std::strcmp(verdict, "dart-ready") == 0) return 1u << 3;
    if (std::strcmp(verdict, "attempts-exhausted") == 0) return 1u << 4;
    return 1u << 5;
}

void report_start_verdict(const char *site, const char *verdict, bool adopted, int attempts) {
    static std::atomic<uint32_t> reported{0};
    const uint32_t bit = start_verdict_bit(verdict);
    if ((reported.fetch_or(bit, std::memory_order_acq_rel) & bit) != 0) return;
    __android_log_print(ANDROID_LOG_INFO, kTag,
        "layout start site=%s pid=%d adopted=%d verdict=%s attempts=%d ready=%d dart_ready=%d "
        "started=%d",
        site != nullptr ? site : "?", static_cast<int>(getpid()), adopted ? 1 : 0,
        verdict != nullptr ? verdict : "starting", attempts,
        g_ready.load(std::memory_order_relaxed) ? 1 : 0,
        g_dart_ready.load(std::memory_order_relaxed) ? 1 : 0,
        g_started.load(std::memory_order_relaxed) ? 1 : 0);
}

/*
 * `site` names the signal that asked for the worker (native-init / property / setprogname / library /
 * configure), so the log above can say which of the five call sites got what verdict.
 */
void start_home_layout_hooks(const char *site, HookFunction hook, UnhookFunction unhook) {
    /*
     * Adopt (and therefore reset) inherited state before reading a single gate: a desktop that
     * inherited "already running" would otherwise never create its own worker, and every call site
     * relying on its own prepare call is exactly the discipline that failed here.
     */
    const bool adopted = adopt_layout_state();
    const int attempts = g_attempts.load(std::memory_order_acquire);
    const char *verdict = nullptr;
    if (hook == nullptr) verdict = "hook-null";
    else if (g_ready.load(std::memory_order_relaxed)) verdict = "ready";
    else if (g_dart_ready.load(std::memory_order_relaxed)) verdict = "dart-ready";
    else if (attempts >= kMaxWorkerAttempts) verdict = "attempts-exhausted";
    if (verdict == nullptr) {
        bool expected = false;
        if (!g_started.compare_exchange_strong(expected, true, std::memory_order_acq_rel)) {
            verdict = "already-started";
        }
    }
    if (verdict != nullptr) {
        report_start_verdict(site, verdict, adopted, attempts);
        return;
    }
    report_start_verdict(site, nullptr, adopted, attempts);
    g_attempts.fetch_add(1, std::memory_order_acq_rel);
    g_hook_function = hook;
    g_unhook_function = unhook;
    pthread_attr_t attributes{};
    if (pthread_attr_init(&attributes) != 0) {
        g_started.store(false, std::memory_order_release);
        return;
    }
    pthread_attr_setdetachstate(&attributes, PTHREAD_CREATE_DETACHED);
    pthread_t thread{};
    const int created = pthread_create(&thread, &attributes, worker, nullptr);
    pthread_attr_destroy(&attributes);
    if (created != 0) {
        g_started.store(false, std::memory_order_release);
        __android_log_print(ANDROID_LOG_ERROR, kTag, "layout worker unavailable=%d", created);
    }
}
