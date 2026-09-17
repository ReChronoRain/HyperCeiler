/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_layout_config.h"

#include <android/binder_ibinder.h>
#include <android/log.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <sys/system_properties.h>

#include <charconv>
#include <system_error>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <dlfcn.h>
#include <string_view>

namespace home_layout {
namespace {
constexpr transaction_code_t kLayoutTransaction = 0x0048434C;
constexpr int32_t kLayoutAck = 0x48434C34;
constexpr char kDescriptor[] = "android.view.IWindowManager";

/*
 * Debug-only override of the preference snapshot, for calibrating the layout knobs from adb without
 * walking the settings page (which restarts the desktop on every change). All four properties are
 * plain values in the same units the Binder snapshot carries, and only exist while
 * `debug.hyperceiler.layout.override` is 1:
 *
 *   debug.hyperceiler.layout.override = 1
 *   debug.hyperceiler.layout.grid     = <enabled>,<cellX>,<cellY>
 *   debug.hyperceiler.layout.knobs    = <enabled>:<deltaPx>,...   (HC_LAYOUT_KNOBS order)
 *   debug.hyperceiler.layout.tweaks   = <12 ints, protocol v3 order>
 *
 * A malformed or missing property is ignored field by field, never fatal. This is a build-time
 * constant-free diagnostics path: it cannot change behaviour unless the property is set, so a
 * release build with no properties behaves exactly as the settings page says.
 */
bool debug_override_enabled() {
    char value[PROP_VALUE_MAX] = {};
    return __system_property_get("debug.hyperceiler.layout.override", value) > 0
        && value[0] == '1';
}

std::string_view property_text(const char *name) {
    static char value[PROP_VALUE_MAX] = {};
    if (__system_property_get(name, value) <= 0) return {};
    return value;
}

/* One `enabled:deltaPx` entry per geometry knob, in HC_LAYOUT_KNOBS order. */
void apply_knob_override(Config &config) {
    const std::string_view text = property_text("debug.hyperceiler.layout.knobs");
    if (text.empty()) return;
    size_t cursor = 0;
    for (size_t index = 0; index < config.knobs.size(); ++index) {
        const size_t comma = text.find(',', cursor);
        const std::string_view entry = text.substr(cursor,
            comma == std::string_view::npos ? std::string_view::npos : comma - cursor);
        cursor = comma == std::string_view::npos ? text.size() : comma + 1;
        const size_t colon = entry.find(':');
        if (colon == std::string_view::npos) continue;
        int enabled = 0;
        int delta = 0;
        const auto e = std::from_chars(entry.data(), entry.data() + colon, enabled);
        const auto d = std::from_chars(entry.data() + colon + 1, entry.data() + entry.size(), delta);
        if (e.ec != std::errc() || d.ec != std::errc()) continue;
        if (enabled != 0 && enabled != 1) continue;
        if (delta < -1000 || delta > 1000) continue;
        config.knobs[index] = KnobConfig{enabled != 0, delta};
    }
}

void apply_tweaks_override(Config &config) {
    const std::string_view text = property_text("debug.hyperceiler.layout.tweaks");
    if (text.empty()) return;
    int values[12] = {};
    size_t cursor = 0;
    size_t count = 0;
    while (count < 12 && cursor <= text.size()) {
        const size_t comma = text.find(',', cursor);
        const std::string_view entry = text.substr(cursor,
            comma == std::string_view::npos ? std::string_view::npos : comma - cursor);
        if (entry.empty()) break;
        const auto result = std::from_chars(entry.data(), entry.data() + entry.size(), values[count]);
        if (result.ec != std::errc()) return;
        ++count;
        if (comma == std::string_view::npos) break;
        cursor = comma + 1;
    }
    if (count != 12) return;
    config.tweaks = TweaksConfig{
        values[1] != 0, values[0],
        values[4] != 0, values[2], values[3],
        values[7] != 0, values[5], values[6],
        values[9] != 0, values[8],
        values[10] != 0, values[11] != 0};
}

void apply_debug_override(Config &config) {
    char grid[PROP_VALUE_MAX] = {};
    if (__system_property_get("debug.hyperceiler.layout.grid", grid) > 0) {
        int values[3] = {-1, -1, -1};
        size_t cursor = 0;
        size_t count = 0;
        const std::string_view text(grid);
        while (count < 3 && cursor <= text.size()) {
            const size_t comma = text.find(',', cursor);
            const std::string_view entry = text.substr(cursor,
                comma == std::string_view::npos ? std::string_view::npos : comma - cursor);
            if (entry.empty()) break;
            const auto result = std::from_chars(entry.data(), entry.data() + entry.size(),
                values[count]);
            if (result.ec != std::errc()) break;
            ++count;
            if (comma == std::string_view::npos) break;
            cursor = comma + 1;
        }
        if (count == 3 && (values[0] == 0 || values[0] == 1) && values[1] >= 3 && values[1] <= 9
            && values[2] >= 4 && values[2] <= 13) {
            config.grid_enabled = values[0] != 0;
            config.cell_x = values[1];
            config.cell_y = values[2];
        }
    }
    apply_knob_override(config);
    apply_tweaks_override(config);
}

void *on_create(void *) { return nullptr; }
void on_destroy(void *) {}
binder_status_t on_transact(AIBinder *, transaction_code_t, const AParcel *, AParcel *) {
    return STATUS_UNKNOWN_TRANSACTION;
}

const AIBinder_Class *window_class() {
    static AIBinder_Class *clazz = AIBinder_Class_define(kDescriptor,
        on_create, on_destroy, on_transact);
    return clazz;
}
} // namespace

/* The Binder lane: same synchronous IWindowManager transport as DockNativeMotionEndpoint, with an
   independent transaction code and response. No private launcher file or Java injection is needed. */
static bool query_binder(Config &result) {
    using GetService = AIBinder *(*)(const char *);
    const auto get_service = reinterpret_cast<GetService>(
        dlsym(RTLD_DEFAULT, "AServiceManager_getService"));
    if (get_service == nullptr) return false;
    AIBinder *window = get_service("window");
    if (window == nullptr) return false;
    const AIBinder_Class *clazz = AIBinder_getClass(window);
    bool associated = false;
    if (clazz != nullptr) {
        const char *descriptor = AIBinder_Class_getDescriptor(clazz);
        associated = descriptor != nullptr && std::strcmp(descriptor, kDescriptor) == 0;
    } else {
        clazz = window_class();
        associated = clazz != nullptr && AIBinder_associateClass(window, clazz);
    }
    if (!associated) {
        AIBinder_decStrong(window);
        return false;
    }
    AParcel *input = nullptr;
    if (AIBinder_prepareTransaction(window, &input) != STATUS_OK || input == nullptr) {
        AIBinder_decStrong(window);
        return false;
    }
    AParcel *output = nullptr;
    const binder_status_t status = AIBinder_transact(window, kLayoutTransaction,
        &input, &output, 0);
    AIBinder_decStrong(window);
    if (status != STATUS_OK || output == nullptr) {
        __android_log_print(ANDROID_LOG_WARN, "HyperCeiler.HomeLayout",
            "layout config transact failed status=%d output=%p", static_cast<int>(status),
            static_cast<void *>(output));
        return false;
    }
    int32_t ack = 0;
    int32_t grid_enabled = 0;
    int32_t x = 0;
    int32_t y = 0;
    int32_t count = 0;
    Config candidate;
    // The deltas arrive in pixels already scaled by the display density, so the
    // native bound is a sanity limit on the pixel span, not a dp range.
    constexpr int32_t kMaxDeltaPx = 2400;
    bool valid = status == STATUS_OK && output != nullptr
        && AParcel_readInt32(output, &ack) == STATUS_OK
        && AParcel_readInt32(output, &grid_enabled) == STATUS_OK
        && AParcel_readInt32(output, &x) == STATUS_OK
        && AParcel_readInt32(output, &y) == STATUS_OK
        && AParcel_readInt32(output, &count) == STATUS_OK
        && ack == kLayoutAck
        && (grid_enabled == 0 || grid_enabled == 1)
        && x >= 3 && x <= 9 && y >= 4 && y <= 13
        && count >= 0 && count <= HC_LAYOUT_KNOB_COUNT;
    for (int32_t index = 0; valid && index < count; ++index) {
        int32_t enabled = 0;
        int32_t delta_px = 0;
        valid = AParcel_readInt32(output, &enabled) == STATUS_OK
            && AParcel_readInt32(output, &delta_px) == STATUS_OK
            && (enabled == 0 || enabled == 1) && std::abs(delta_px) <= kMaxDeltaPx;
        if (!valid) break;
        candidate.knobs[static_cast<size_t>(index)] = KnobConfig{enabled != 0, delta_px};
    }
    /*
     * Protocol v3 appends the code-patch features as a fixed run of values. A fixed order is used
     * rather than a key/value list so every field has a range that can be checked here: a value
     * outside its range is refused outright instead of being applied to the launcher.
     */
    int32_t tweaks[12] = {};
    for (int32_t &value : tweaks) {
        if (!valid || AParcel_readInt32(output, &value) != STATUS_OK) {
            valid = false;
            break;
        }
    }
    const auto flag = [](int32_t value) { return value == 0 || value == 1; };
    const auto within = [](int32_t value, int32_t lo, int32_t hi) {
        return value >= lo && value <= hi;
    };
    const bool tweaks_ok = flag(tweaks[1]) && within(tweaks[0], 1, 16)
        && flag(tweaks[4]) && within(tweaks[2], 2, 16) && within(tweaks[3], 2, 16)
        && flag(tweaks[7]) && within(tweaks[5], 2, 16) && within(tweaks[6], 2, 16)
        && flag(tweaks[9]) && within(tweaks[8], 0, 0xFF)
        && flag(tweaks[10]) && flag(tweaks[11]);
    if (output != nullptr) AParcel_delete(output);
    if (!valid || !tweaks_ok) {
        /*
         * One line for the whole failure, because "config unavailable" on the launcher side used to be
         * the only symptom and it cannot tell a dead transact from a rejected snapshot.
         */
        __android_log_print(ANDROID_LOG_WARN, "HyperCeiler.HomeLayout",
            "layout config rejected status=%d ack=%#x grid=%d cell=%dx%d count=%d tweaks_ok=%d "
            "valid=%d", static_cast<int>(status), ack, grid_enabled, x, y, count,
            tweaks_ok ? 1 : 0, valid ? 1 : 0);
        return false;
    }
    candidate.grid_enabled = grid_enabled != 0;
    candidate.cell_x = x;
    candidate.cell_y = y;
    candidate.tweaks = TweaksConfig{
        tweaks[1] != 0, tweaks[0],
        tweaks[4] != 0, tweaks[2], tweaks[3],
        tweaks[7] != 0, tweaks[5], tweaks[6],
        tweaks[9] != 0, tweaks[8],
        tweaks[10] != 0, tweaks[11] != 0};
    result = candidate;
    return true;
}

bool query_config(Config &result) {
    Config candidate;
    const bool from_binder = query_binder(candidate);
    /*
     * The debug properties, when present, are layered on top of the snapshot so a single knob can be
     * calibrated without changing the settings page. They also make the query succeed when the
     * endpoint is not up yet, which is what lets a calibration run start before system_server has
     * bound the module.
     */
    if (debug_override_enabled()) {
        apply_debug_override(candidate);
        result = candidate;
        return true;
    }
    if (!from_binder) return false;
    result = candidate;
    return true;
}

} // namespace home_layout
