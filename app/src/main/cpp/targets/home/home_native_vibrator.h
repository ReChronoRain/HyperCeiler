/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <optional>
#include <span>

// Control worker resolves the installed SDK. The feedback worker alone owns
// Binder state, and only submits a freshly authorized platform waveform.
void hc_native_vibrator_refresh();
bool hc_native_vibrator_ready();
bool hc_native_vibrator_cache_selected();
bool hc_native_vibrator_published_selected();
std::optional<bool> hc_native_vibrator_policy_allows();
bool hc_native_vibrator_play(std::span<const uint8_t> arguments);
