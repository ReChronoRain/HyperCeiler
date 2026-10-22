/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include <zygisk_next_api.h>

// Invoked outside the specialization callback and outside the original dlopen call.
// Implementations must tolerate retries and nested dependency loads. No JNI is available.
void hyperceiler_hyos_before_home_load(const ZygiskNextAPI *api);
void hyperceiler_hyos_after_home_load(const ZygiskNextAPI *api, const char *name,
                                     void *handle);
