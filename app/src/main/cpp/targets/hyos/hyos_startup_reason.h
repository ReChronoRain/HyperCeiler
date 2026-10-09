/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>

namespace hyos_startup_reason {
// Stable diagnostics shared by the guarded observer and pure-memory controller.
inline constexpr uint32_t kUnknownLinker = 1;
inline constexpr uint32_t kLoaderBusyOrUnreadable = 2;
inline constexpr uint32_t kFlutterGuardRejected = 3;
inline constexpr uint32_t kUnboundHandleOrThread = 4;
inline constexpr uint32_t kUnexpectedFirstSnapshot = 5;
inline constexpr uint32_t kDartGuardRejected = 6;
inline constexpr uint32_t kStartupReentered = 7;
} // namespace hyos_startup_reason
