/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * NativeHookRuntime base primitives.
 *
 * Shared, dependency-free utilities used by every layer of the runtime:
 * overflow-checked arithmetic, bounded range checks, little-endian loads and
 * page helpers. Everything here is inline and host-testable; none of it talks
 * to a process, a file system or a hook backend.
 *
 * The overflow/range discipline follows the resolver style of
 * MiuiBackGestureHook (Apache-2.0, see THIRD_PARTY_NOTICES.md in this
 * directory): an address arithmetic that could wrap is a hard failure, never
 * a wrapped value.
 */
#pragma once

#include <cstddef>
#include <cstdint>
#include <span>
#include <string_view>
// sysconf/_SC_PAGESIZE live in different headers per platform.
#include <unistd.h>
#if !defined(_SC_PAGESIZE)
#include <limits.h>
#define NHK_HOST_PAGE_SIZE 4096
#endif

namespace nhk {

/** True when left + right would wrap; the caller must fail closed instead. */
inline bool add_overflows(uint64_t left, uint64_t right) {
    return left > UINT64_MAX - right;
}

/** Byte range [offset, offset+size) inside a whole-image span. */
inline bool in_image(size_t image_size, uint64_t offset, uint64_t size) {
    if (size == 0) return false;
    if (add_overflows(offset, size)) return false;
    return offset <= image_size && size <= image_size - offset;
}

inline uint16_t load_le16(const std::byte *bytes) {
    return static_cast<uint16_t>(std::to_integer<uint16_t>(bytes[0]))
        | static_cast<uint16_t>(std::to_integer<uint16_t>(bytes[1]) << 8);
}

inline uint32_t load_le32(const std::byte *bytes) {
    return static_cast<uint32_t>(load_le16(bytes))
        | (static_cast<uint32_t>(load_le16(bytes + 2)) << 16);
}

inline uint64_t load_le64(const std::byte *bytes) {
    return static_cast<uint64_t>(load_le32(bytes))
        | (static_cast<uint64_t>(load_le32(bytes + 4)) << 32);
}

/** Bounded, NUL-aware string comparison straight out of a mapped strtab. */
inline bool bounded_string_equals(const char *mapped, uint64_t mapped_bytes,
    std::string_view expected) {
    if (mapped == nullptr || expected.empty() || expected.size() >= mapped_bytes) return false;
    return std::string_view(mapped, strnlen(mapped, mapped_bytes)) == expected;
}

inline uint64_t host_page_size() {
#if defined(NHK_HOST_PAGE_SIZE)
    return NHK_HOST_PAGE_SIZE;
#else
    const long page = sysconf(_SC_PAGESIZE);
    return page > 0 ? static_cast<uint64_t>(page) : 4096;
#endif
}

inline uint64_t page_down(uint64_t value) {
    const uint64_t page = host_page_size();
    return value - value % page;
}

inline uint64_t page_up(uint64_t value) {
    const uint64_t page = host_page_size();
    const uint64_t remainder = value % page;
    return remainder == 0 ? value : value + (page - remainder);
}

} // namespace nhk
