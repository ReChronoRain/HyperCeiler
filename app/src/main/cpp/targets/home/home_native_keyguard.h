/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
struct HomeKeyguardObservation {bool known=false,locked=false;uint64_t observed_ns=0,unlock_ns=0,callbacks=0,received=0;int32_t last_uid=-1,last_status=0;uint32_t last_code=0;};
// Control worker only; listener callbacks publish scalars and signal eventfd.
void hc_native_keyguard_refresh(void* retained_context,bool wanted,void(*wake)());
bool hc_native_keyguard_due();
HomeKeyguardObservation hc_native_keyguard_observation();
