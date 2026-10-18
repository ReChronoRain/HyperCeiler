/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <cstring>
#include <string>
#include <string_view>
#include <vector>
#include "home_dynamic_layout.h"

namespace home_title {
struct CustomTitle {
    std::u16string package, label;
    bool operator==(const CustomTitle &) const = default;
};
constexpr int32_t kCustomMagic = 0x48435431;
constexpr size_t kMaxCustomUnits = 32768;

/*
 * Unicode surrogate range, from the standard rather than from the launcher: a UTF-16 code unit
 * in [D800, DBFF] starts a pair and one in [DC00, DFFF] continues one. Nothing here depends on
 * which build is loaded, which is why these stay as written.
 */
constexpr unsigned kSurrogateHighFirst = 0xd800;
constexpr unsigned kSurrogateHighLast = 0xdbff;
constexpr unsigned kSurrogateLowFirst = 0xdc00;
constexpr unsigned kSurrogateLowLast = 0xdfff;

/*
 * `thread` is a `Dart_Thread*` -- the VM's own C++ thread object, not a Dart heap object -- and
 * these are the bump-allocator cursors inside it. They are part of the VM's C ABI rather than
 * of any launcher build: the C API this module allocates through (`Dart_NewString` and friends)
 * reads the same two words on every Dart version, so they are not something an image scan could
 * or should relocate. A VM that moved them would fail the `top`/`end` sanity checks below, and
 * the function would return the original string instead of writing into an unknown region.
 */
constexpr unsigned kThreadTopOffset = 0x60;
constexpr unsigned kThreadEndOffset = 0x68;

inline bool valid_label(std::u16string_view text) {
    for (size_t i = 0; i < text.size(); ++i) {
        const unsigned unit = text[i];
        if (!unit) return false;
        if (unit >= kSurrogateHighFirst && unit <= kSurrogateHighLast) {
            if (++i >= text.size() || text[i] < kSurrogateLowFirst
                || text[i] > kSurrogateLowLast) {
                return false;
            }
        } else if (unit >= kSurrogateLowFirst && unit <= kSurrogateLowLast) {
            return false;
        }
    }
    return true;
}
// The same bounded reader serves Binder and last-good file snapshots. Never publish a partial list.
template<class Read> bool read_custom_titles(Read read, std::vector<CustomTitle> &result) {
    int32_t count = 0;
    if (!read(count) || count < 0 || count > 1024) return false;
    size_t units = 0;
    std::vector<CustomTitle> next;
    for (int index = 0; index < count; ++index) {
        CustomTitle row;
        for (auto *text : {&row.package, &row.label}) {
            int32_t length = 0;
            const int bound = text == &row.package ? 255 : 512;
            if (!read(length) || length <= 0 || length > bound
                || units + static_cast<unsigned>(length) > kMaxCustomUnits) return false;
            units += length;
            for (int at = 0; at < length; ++at) {
                int32_t unit = 0;
                if (!read(unit) || unit <= 0 || unit > 65535) return false;
                text->push_back(static_cast<char16_t>(unit));
            }
        }
        bool dot = false;
        for (char16_t ch : row.package) {
            if (ch == '.') dot = true;
            else if (!((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')
                       || (ch >= '0' && ch <= '9') || ch == '_')) return false;
        }
        if (!dot || !valid_label(row.label)
            || (!next.empty() && next.back().package >= row.package)) return false;
        next.push_back(std::move(row));
    }
    result = std::move(next);
    return true;
}

inline unsigned dart_cid(uint64_t object) {
    if (!(object & 1U)) return 0;
    uint64_t header = 0;
    std::memcpy(&header, reinterpret_cast<const void *>(object - 1), 8);
    return (header >> 12) & 0xfffffU;
}
inline uint64_t dart_field(uint64_t object, unsigned offset, uint64_t heap) {
    uint32_t value = 0;
    std::memcpy(&value, reinterpret_cast<const void *>(object + offset), 4);
    return (heap << 32) + value;
}
inline bool string_equals(uint64_t text, std::u16string_view expected) {
    const unsigned cid = dart_cid(text);
    if (cid != 94 && cid != 95) return false;
    uint32_t smi = 0;
    std::memcpy(&smi, reinterpret_cast<const void *>(text + 7), 4);
    if ((smi & 1U) || (smi >> 1) != expected.size()) return false;
    for (size_t index = 0; index < expected.size(); ++index) {
        uint16_t value = 0;
        std::memcpy(&value, reinterpret_cast<const void *>(text + 15 + index * (cid == 94 ? 1 : 2)),
            cid == 94 ? 1 : 2);
        if (value != expected[index]) return false;
    }
    return true;
}

// Replay the current getter dataflow. Dispatch identity, not a historic CID range,
// determines which owning getter the actual model uses. No Dart call or heap write.
inline uint64_t model_package(uint64_t model, uint64_t heap, uint64_t dispatch, uint64_t null_object,
    uint64_t component_method, uint64_t pin_method, const home_dynamic::ModelLayout &layout) {
    if (!layout.usable() || !dispatch || !(model & 1U)) return 0;
    const unsigned cid = dart_cid(model);
    if (!cid) return 0;
    uint64_t method = 0;
    const int64_t offset = (static_cast<int64_t>(cid) - layout.dispatch) * 8;
    std::memcpy(&method, reinterpret_cast<const void *>(dispatch + offset), 8);
    if (method == pin_method) return dart_field(model, layout.pin_name, heap);
    if (method != component_method) return 0;
    uint64_t package = 0;
    const auto flag = dart_field(model, layout.flag, heap);
    if ((flag & layout.flag_mask) == 0) {
        const auto intent = dart_field(model, layout.intent, heap);
        if (static_cast<uint32_t>(intent) != static_cast<uint32_t>(null_object) && (intent & 1U)) {
            const auto component = dart_field(intent, layout.component, heap);
            if (static_cast<uint32_t>(component) != static_cast<uint32_t>(null_object) && (component & 1U))
                package = dart_field(component, layout.component_package, heap);
        }
    }
    if (!package || static_cast<uint32_t>(package) == static_cast<uint32_t>(null_object))
        package = dart_field(model, layout.package, heap);
    return package;
}

// Exact allocateTwoByteString fast path. Caller roots this pointer in the original Dart frame
// before the next original allocation BL. No retained Dart pointer, GC or runtime call is involved.
inline uint64_t clone_label(uint64_t original, uint64_t thread, std::u16string_view label) {
    if (!thread || label.empty() || label.size() > 512) return original;
    uint64_t top = 0, end = 0;
    std::memcpy(&top, reinterpret_cast<const void *>(thread + kThreadTopOffset), 8);
    std::memcpy(&end, reinterpret_cast<const void *>(thread + kThreadEndOffset), 8);
    const uint64_t size = (label.size() * 2 + 31) & ~uint64_t{15};
    if (!top || (top & 7) || end <= top || end - top <= size) return original;
    const uint64_t next = top + size;
    const uint64_t header = 0x5f05cU | (size <= 240 ? size << 4 : 0);
    const uint32_t smi_length = static_cast<uint32_t>(label.size() * 2);
    std::memset(reinterpret_cast<void *>(top), 0, size);
    std::memcpy(reinterpret_cast<void *>(top), &header, 8);
    std::memcpy(reinterpret_cast<void *>(top + 8), &smi_length, 4);
    std::memcpy(reinterpret_cast<void *>(top + 16), label.data(), label.size() * 2);
    std::memcpy(reinterpret_cast<void *>(thread + 0x60), &next, 8);
    return top + 1;
}
inline uint64_t custom_label(uint64_t original, uint64_t model, uint64_t heap, uint64_t thread,
    uint64_t dispatch, uint64_t null_object, uint64_t component_method, uint64_t pin_method,
    const std::vector<CustomTitle> &names, const home_dynamic::ModelLayout &layout) {
    if (names.empty()) return original;
    const auto package = model_package(model, heap, dispatch, null_object, component_method, pin_method, layout);
    // Compare the compressed Dart string in place: no UTF conversion, temporary allocation,
    // full app-list scan or retained heap pointer on the rendering path.
    const auto cid = dart_cid(package);
    if (cid != 94 && cid != 95) return original;
    uint32_t smi = 0;
    std::memcpy(&smi, reinterpret_cast<const void *>(package + 7), 4);
    if ((smi & 1U) || (smi >> 1) > 255) return original;
    const size_t length = smi >> 1;
    const auto compare = [&](std::u16string_view text) {
        for (size_t index = 0; index < length && index < text.size(); ++index) {
            uint16_t unit = 0;
            std::memcpy(&unit, reinterpret_cast<const void *>(package + 15 + index * (cid == 94 ? 1 : 2)),
                        cid == 94 ? 1 : 2);
            if (unit != text[index]) return unit < text[index] ? -1 : 1;
        }
        return length == text.size() ? 0 : length < text.size() ? -1 : 1;
    };
    size_t lo = 0, hi = names.size();
    while (lo < hi) {
        const size_t mid = lo + (hi - lo) / 2;
        const int order = compare(names[mid].package);
        if (!order) return clone_label(original, thread, names[mid].label);
        if (order < 0) hi = mid; else lo = mid + 1;
    }
    return original;
}
}
