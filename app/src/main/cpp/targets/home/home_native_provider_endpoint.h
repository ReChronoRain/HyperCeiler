/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <string>
struct AIBinder;
// Control-worker only. Returns one owned NDK strong reference, or null.
// Does not depend on the optional SurfaceControl/material SDK interfaces.
AIBinder* hc_native_extra_endpoint(void* retained_context,std::string& reason,bool& unsupported);
