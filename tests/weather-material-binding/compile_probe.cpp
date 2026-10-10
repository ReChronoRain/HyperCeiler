/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/weather/weather_material_binding.h"
extern bool read_target(uintptr_t, std::span<std::byte>);
extern "C" bool material_resolve(uintptr_t base, weather_material_binding::Binding *out) {
    return weather_material_binding::resolve(base, read_target, *out);
}
extern "C" bool material_dart(uintptr_t base) {
    return weather_material_binding::verify_dart(base, read_target);
}
extern "C" bool material_cold(const weather_material_binding::Binding *binding) {
    return weather_material_binding::cold_state(*binding, read_target);
}
