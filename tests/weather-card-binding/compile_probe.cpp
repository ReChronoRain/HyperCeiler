/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/weather/weather_card_binding.h"
extern bool card_probe_read(uintptr_t, std::span<std::byte>);
bool card_probe_resolve(uintptr_t dart, uintptr_t flutter, weather_card::Binding &out) {
    return weather_card::resolve(dart, flutter, card_probe_read, out);
}
bool card_probe_verify(const weather_card::Binding &binding, const weather_card::BranchWords &words, unsigned mask) {
    return weather_card::verify_state(binding, card_probe_read, words, mask);
}
bool card_probe_owned(const weather_card::Binding &binding, const weather_card::BranchWords &words) {
    return weather_card::verify_owned(binding, card_probe_read, words);
}
