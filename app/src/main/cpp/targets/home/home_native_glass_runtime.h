/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
namespace home_dock {struct Config;}
// Explicit staging backends, production remains WMS. Only the SDK worker owns these
// resources. The readiness/deadline query is atomic and performs no SDK work.
void hc_native_glass_hold_refresh(void* context,const home_dock::Config* appearance,void(*wake)(),bool visible_probe=false,bool native_backend=false);
bool hc_native_glass_hold_due();
bool hc_native_glass_release_pending();
