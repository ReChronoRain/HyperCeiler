// SPDX-License-Identifier: Apache-2.0
#pragma once

#include "ht_plan.h"
#include "image.h"

#include <stdint.h>

namespace hometweaks {

uint64_t ImageIdentity(const Image& image);

bool LoadSitesCache(uint64_t imageId, LocatedSites* out);
bool SaveSitesCache(uint64_t imageId, const LocatedSites& sites);

void DropSitesCache();

}
