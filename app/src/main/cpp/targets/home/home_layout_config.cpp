/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_layout_config.h"
#include "home_widget_move.h"

#include <android/binder_ibinder.h>
#include <android/log.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <sys/system_properties.h>

#include <charconv>
#include <system_error>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <dlfcn.h>
#include <optional>
#include <mutex>
#include <utility>
#include <string_view>
#include <vector>

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
 *   debug.hyperceiler.layout.tweaks   = <17 ints, protocol order; legacy 16 keeps capacity locked>
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
    int values[17] = {};
    size_t cursor = 0;
    size_t count = 0;
    while (count < 17 && cursor <= text.size()) {
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
    if (count != 16 && count != 17) return;
    config.tweaks = TweaksConfig{
        values[1] != 0, values[0],
        values[4] != 0, values[2], values[3],
        values[7] != 0, values[5], values[6],
        values[9] != 0, values[8],
        values[10] != 0, values[11] != 0,
        values[12] != 0, values[13], values[14] != 0, values[15], values[16] == 1};
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

/*
 * Last-known-good configuration cache.
 *
 * The Binder endpoint lives in system_server's Java world, and when LSPosed fails to dispatch the
 * module there (early-boot APK parse failure, no retry) the endpoint is gone for the whole
 * system_server lifetime — while the native patches that consume this config keep working. A
 * cache of the last successfully received config lets the consumer-side features (tweaks, knobs)
 * keep running with the user's last-known values instead of going dark with the endpoint.
 * Dock-glass/WMS-side hooks cannot be saved this way (they live in the dead process), but they
 * are the minority.
 */
constexpr char kCacheMagic[4] = {'H', 'C', 'L', 'C'};
constexpr uint32_t kCacheVersion = 12;
constexpr const char *kCachePath = "/data/user/0/com.miui.home/files/layout_config_cache.bin";
constexpr const char *kCacheTmpPath = "/data/user/0/com.miui.home/files/layout_config_cache.bin.tmp";

void serialize_config(const Config &config, std::vector<uint8_t> &out) {
    out.clear();
    auto put_u32 = [&out](uint32_t v) {
        out.push_back(static_cast<uint8_t>(v));
        out.push_back(static_cast<uint8_t>(v >> 8));
        out.push_back(static_cast<uint8_t>(v >> 16));
        out.push_back(static_cast<uint8_t>(v >> 24));
    };
    out.insert(out.end(), kCacheMagic, kCacheMagic + 4);
    put_u32(kCacheVersion);
    put_u32(config.grid_enabled ? 1 : 0);
    put_u32(static_cast<uint32_t>(config.cell_x));
    put_u32(static_cast<uint32_t>(config.cell_y));
    for (const auto &knob : config.knobs) {
        put_u32(knob.enabled ? 1 : 0);
        put_u32(static_cast<uint32_t>(knob.delta_dp));
    }
    put_u32(config.tweaks.folder_enabled ? 1 : 0);
    put_u32(static_cast<uint32_t>(config.tweaks.folder_cols));
    put_u32(config.tweaks.pad_enabled ? 1 : 0);
    put_u32(static_cast<uint32_t>(config.tweaks.pad_major));
    put_u32(static_cast<uint32_t>(config.tweaks.pad_minor));
    put_u32(config.tweaks.fold_enabled ? 1 : 0);
    put_u32(static_cast<uint32_t>(config.tweaks.fold_major));
    put_u32(static_cast<uint32_t>(config.tweaks.fold_minor));
    put_u32(config.tweaks.icon_scale_enabled ? 1 : 0);
    put_u32(static_cast<uint32_t>(config.tweaks.icon_scale_code));
    put_u32(config.tweaks.recents_hide_clear ? 1 : 0);
    put_u32(config.tweaks.recents_no_clear ? 1 : 0);
    put_u32(config.tweaks.animation_open_enabled ? 1 : 0);
    put_u32(static_cast<uint32_t>(config.tweaks.animation_open_rate_percent));
    put_u32(config.tweaks.animation_recents_enabled ? 1 : 0);
    put_u32(static_cast<uint32_t>(config.tweaks.animation_recents_rate_percent));
    put_u32(config.tweaks.hotseat_unlimited ? 1 : 0);
    put_u32(static_cast<uint32_t>(config.title_desktop_sp));
    put_u32(static_cast<uint32_t>(config.title_drawer_sp));
    put_u32(static_cast<uint32_t>(config.title_color));
    put_u32(config.title_hide_new_install ? 1U : 0U);
    put_u32(static_cast<uint32_t>(config.title_custom_labels.size()));
    for (const auto &row : config.title_custom_labels) {
        for (const auto *text : {&row.package, &row.label}) {
            put_u32(static_cast<uint32_t>(text->size()));
            for (char16_t unit : *text) put_u32(unit);
        }
    }
    put_u32(config.widget_allow_move ? 1U : 0U);
    put_u32(kFolderLayoutMagic);
    const auto &f = config.folder;
    for (int value : {f.title_center, f.full_width, f.padding_enabled, f.phone_padding,
            f.landscape_padding, f.portrait_padding, f.tablet}) put_u32(value);
    put_u32(kBackGestureMagic);
    put_u32(config.back_gesture.height); put_u32(config.back_gesture.width);
    put_u32(kFolderAutoCloseMagic); put_u32(config.folder_auto_close ? 1U : 0U);
}

bool parse_config(const std::vector<uint8_t> &data, Config &config) {
    if (data.size() < 4 + 4 + 4 + 4 + 4 + 8 * 8 + 16 * 4) return false;
    if (std::memcmp(data.data(), kCacheMagic, 4) != 0) return false;
    uint32_t version = 0;
    std::memcpy(&version, data.data() + 4, 4);
    if (version != 4 && version != 5 && version != 6 && version != 7 && version != 8
        && version != 9 && version != 10 && version != 11 && version != kCacheVersion)
        return false;
    size_t at = 8;
    auto get_u32 = [&]() -> std::optional<uint32_t> {
        if (at + 4 > data.size()) return std::nullopt;
        uint32_t v = 0;
        std::memcpy(&v, data.data() + at, 4);
        at += 4;
        return v;
    };
    const auto grid = get_u32();
    const auto cx = get_u32();
    const auto cy = get_u32();
    if (!grid || !cx || !cy || *cx < 3 || *cx > 9 || *cy < 4 || *cy > 13) return false;
    config.grid_enabled = *grid != 0;
    config.cell_x = static_cast<int>(*cx);
    config.cell_y = static_cast<int>(*cy);
    for (auto &knob : config.knobs) {
        const auto enabled = get_u32();
        const auto delta = get_u32();
        if (!enabled || !delta) return false;
        knob = KnobConfig{*enabled != 0, static_cast<int>(*delta)};
    }
    const auto fe = get_u32();        const auto fc = get_u32();
    const auto pe = get_u32();        const auto pm = get_u32();
    const auto pn = get_u32();        const auto fle = get_u32();
    const auto flm = get_u32();       const auto fln = get_u32();
    const auto ie = get_u32();        const auto ic = get_u32();
    const auto rh = get_u32();        const auto rn = get_u32();
    const auto oe = get_u32();        const auto op = get_u32();
    const auto re = get_u32();        const auto rr = get_u32();
    const auto hu = version == 4 ? std::optional<uint32_t>{0} : get_u32();
    if (!fe || !fc || !pe || !pm || !pn || !fle || !flm || !fln || !ie || !ic || !rh || !rn
        || !oe || !op || !re || !rr || !hu || *hu > 1 || *oe > 1 || *re > 1
        || *op < 30 || *op > 200 || *rr < 30 || *rr > 200)
        return false;
    config.tweaks = TweaksConfig{*fe != 0, static_cast<int>(*fc), *pe != 0,
        static_cast<int>(*pm), static_cast<int>(*pn), *fle != 0,
        static_cast<int>(*flm), static_cast<int>(*fln), *ie != 0,
        static_cast<int>(*ic), *rh != 0, *rn != 0,
        *oe != 0, static_cast<int>(*op), *re != 0, static_cast<int>(*rr), *hu != 0};
    if (version >= 6) {
        const auto desktop = get_u32();
        const auto drawer = get_u32();
        const auto color = get_u32();
        if (!desktop || !drawer || !color || *desktop > 20 || *drawer > 20) return false;
        config.title_desktop_sp = static_cast<int>(*desktop);
        config.title_drawer_sp = static_cast<int>(*drawer);
        config.title_color = static_cast<int32_t>(*color);
    }
    if (version >= 7) {
        const auto hide = get_u32();
        if (!hide || *hide > 1) return false;
        config.title_hide_new_install = *hide != 0;
    }
    if (version >= 8) {
        const auto read = [&](int32_t &value) {
            const auto next = get_u32();
            if (!next) return false;
            value = static_cast<int32_t>(*next); return true;
        };
        if (!home_title::read_custom_titles(read, config.title_custom_labels))
            return false;
    }
    config.widget_allow_move = false;
    if (version >= 9) {
        const auto move = get_u32();
        if (!move || *move > 1) return false;
        config.widget_allow_move = *move != 0;
    }
    config.folder = {};
    if (version >= 10 && !read_folder_layout([&](int32_t &word) {
            auto next = get_u32(); if (!next) return false;
            word = static_cast<int32_t>(*next); return true;
        }, config.folder, false)) return false;
    config.back_gesture = {};
    if (version >= 11 && !read_back_gesture([&](int32_t &word) {
        auto next = get_u32(); if (!next) return false; word = static_cast<int32_t>(*next); return true;
    }, config.back_gesture, false)) return false;
    config.folder_auto_close = false;
    if (version >= 12 && !read_folder_auto_close([&](int32_t &word) {
        auto next = get_u32(); if (!next) return false; word = static_cast<int32_t>(*next); return true;
    }, config.folder_auto_close, false)) return false;
    if (version >= 8 && at != data.size()) return false;
    return true;
}

bool write_config_cache(const Config &config) {
    std::vector<uint8_t> data;
    serialize_config(config, data);
    FILE *tmp = std::fopen(kCacheTmpPath, "wb");
    if (tmp == nullptr) return false;
    const auto written = std::fwrite(data.data(), 1, data.size(), tmp);
    const bool closed = std::fclose(tmp) == 0;
    if (written != data.size() || !closed || std::rename(kCacheTmpPath, kCachePath) != 0) {
        std::remove(kCacheTmpPath);
        return false;
    }
    return true;
}

bool read_config_cache(Config &config) {
    FILE *file = std::fopen(kCachePath, "rb");
    if (file == nullptr) return false;
    std::vector<uint8_t> data;
    uint8_t buffer[512];
    size_t read = 0;
    while ((read = std::fread(buffer, 1, sizeof(buffer), file)) > 0) {
        if (data.size() + read > 160 * 1024) { std::fclose(file); return false; }
        data.insert(data.end(), buffer, buffer + read);
    }
    const bool complete = !std::ferror(file);
    std::fclose(file);
    return complete && parse_config(data, config);
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
    // The deltas arrive in dp, the unit the launcher's own accessors answer in, so this bound is
    // a sanity limit on the requested span rather than a page-range re-check.
    constexpr int32_t kMaxDeltaDp = 2400;
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
        int32_t delta_dp = 0;
        valid = AParcel_readInt32(output, &enabled) == STATUS_OK
            && AParcel_readInt32(output, &delta_dp) == STATUS_OK
            && (enabled == 0 || enabled == 1) && std::abs(delta_dp) <= kMaxDeltaDp;
        if (!valid) break;
        candidate.knobs[static_cast<size_t>(index)] = KnobConfig{enabled != 0, delta_dp};
    }
    /*
     * The protocol appends the code-patch features as a fixed run of values. A fixed order is used
     * rather than a key/value list so every field has a range that can be checked here: a value
     * outside its range is refused outright instead of being applied to the launcher.
     */
    int32_t tweaks[17] = {};
    for (int i = 0; i < 16; ++i) {
        if (!valid || AParcel_readInt32(output, &tweaks[i]) != STATUS_OK) {
            valid = false;
            break;
        }
    }
    if (valid && AParcel_readInt32(output, &tweaks[16]) != STATUS_OK) tweaks[16] = 0;
    int32_t title[4] = {12, 12, -1, 0};
    if (valid && AParcel_readInt32(output, &title[0]) == STATUS_OK) {
        valid = AParcel_readInt32(output, &title[1]) == STATUS_OK
            && AParcel_readInt32(output, &title[2]) == STATUS_OK;
    } else {
        title[0] = 12; // AParcel does not guarantee an untouched out-param on old endpoints.
    }
    if (valid && AParcel_readInt32(output, &title[3]) != STATUS_OK) title[3] = 0;
    int32_t custom_magic = 0;
    if (valid && AParcel_readInt32(output, &custom_magic) == STATUS_OK) {
        const auto read = [&](int32_t &value) {
            return AParcel_readInt32(output, &value) == STATUS_OK;
        };
        valid = custom_magic == home_title::kCustomMagic
            && home_title::read_custom_titles(read, candidate.title_custom_labels);
    } // Older endpoints have no custom block; their authoritative default is an empty list.
    if (valid) {
        valid = read_widget_move_extension([&](int32_t &word) {
            return AParcel_readInt32(output, &word) == STATUS_OK;
        }, candidate.widget_allow_move);
    } // Older endpoints default this new feature to off, never consume another field as a flag.
    if (valid) valid = read_folder_layout([&](int32_t &word) {
        return AParcel_readInt32(output, &word) == STATUS_OK;
    }, candidate.folder);
    if (valid) valid = read_back_gesture([&](int32_t &word) {
        return AParcel_readInt32(output, &word) == STATUS_OK;
    }, candidate.back_gesture);
    if (valid) valid = read_folder_auto_close([&](int32_t &word) {
        return AParcel_readInt32(output, &word) == STATUS_OK;
    }, candidate.folder_auto_close);
    const auto flag = [](int32_t value) { return value == 0 || value == 1; };
    const auto within = [](int32_t value, int32_t lo, int32_t hi) {
        return value >= lo && value <= hi;
    };
    const bool tweaks_ok = flag(tweaks[1]) && within(tweaks[0], 1, 16)
        && flag(tweaks[4]) && within(tweaks[2], 2, 16) && within(tweaks[3], 2, 16)
        && flag(tweaks[7]) && within(tweaks[5], 2, 16) && within(tweaks[6], 2, 16)
        && flag(tweaks[9]) && within(tweaks[8], 0, 0xFF)
        && flag(tweaks[10]) && flag(tweaks[11])
        && flag(tweaks[12]) && within(tweaks[13], 30, 200)
        && flag(tweaks[14]) && within(tweaks[15], 30, 200) && flag(tweaks[16]);
    if (output != nullptr) AParcel_delete(output);
    if (!valid || !tweaks_ok || !within(title[0], 0, 20)
        || !within(title[1], 0, 20) || !flag(title[3])) {
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
        tweaks[10] != 0, tweaks[11] != 0,
        tweaks[12] != 0, tweaks[13], tweaks[14] != 0, tweaks[15], tweaks[16] != 0};
    candidate.title_desktop_sp = title[0];
    candidate.title_drawer_sp = title[1];
    candidate.title_color = title[2];
    candidate.title_hide_new_install = title[3] != 0;
    result = candidate;
    return true;
}

// Serialize the infrequent configuration requests, not the Dart build path.
// A persisted snapshot is only a cold-start bootstrap; it must never supersede
// a newer successful Binder result when storage writes fail or the endpoint dies.
struct ProcessConfigCache {
    std::mutex mutex;
    std::optional<Config> latest;
    std::vector<uint8_t> persisted;
};
static ProcessConfigCache g_process_config;

// LSPosed invokes the library-loaded callback while Android's linker lock may be
// held. Never wait for the request mutex or enter Binder/dlsym on that path:
// the worker can own this mutex while waiting for the same linker lock.
// A busy cache is not a disabled snapshot; the worker publishes after loading.
bool query_bootstrap_config(Config &result) {
    std::unique_lock<std::mutex> lock(g_process_config.mutex, std::try_to_lock);
    if (!lock.owns_lock()) return false;
    Config candidate;
    if (g_process_config.latest) {
        candidate = *g_process_config.latest;
    } else if (!read_config_cache(candidate)) {
        if (!debug_override_enabled()) return false;
    }
    // Keep calibration private, just as query_config does. Do not persist or
    // publish a disk bootstrap as a new authoritative Binder revision here.
    if (debug_override_enabled()) apply_debug_override(candidate);
    result = std::move(candidate);
    return true;
}

bool query_config(Config &result) {
    std::lock_guard<std::mutex> lock(g_process_config.mutex);
    Config candidate;
    const bool from_binder = query_binder(candidate);
    if (debug_override_enabled()) {
        // Calibration never changes the authoritative in-memory or disk snapshot.
        apply_debug_override(candidate);
        result = candidate;
        return true;
    }
    if (!from_binder) {
        if (g_process_config.latest) {
            result = *g_process_config.latest;
            return true;
        }
        if (!read_config_cache(candidate)) return false;
        g_process_config.latest = candidate;
        result = candidate;
        return true;
    }
    // Publish even when the subsequent disk write is rejected. Explicit disable,
    // a lower/higher margin and a complete settings reset remain authoritative.
    g_process_config.latest = candidate;
    result = candidate;
    std::vector<uint8_t> serialized;
    serialize_config(candidate, serialized);
    if (serialized != g_process_config.persisted && write_config_cache(candidate))
        g_process_config.persisted = std::move(serialized);
    return true;
}

} // namespace home_layout
