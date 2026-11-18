/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
namespace home_dock {struct Config;}
// Always-hidden material/parent test on the existing background SDK worker.
void hc_native_glass_probe(void* retained_context,const home_dock::Config& appearance);

void hc_native_capsule_probe(void* retained_context,const home_dock::Config& appearance);
