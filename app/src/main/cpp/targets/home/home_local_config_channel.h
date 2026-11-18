/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstddef>
#include <cstdint>
#include <memory>
namespace home_local_config {
struct HapticWavePack;
using Publish = bool (*)(const unsigned char*,size_t,uint32_t,std::shared_ptr<const HapticWavePack>&);
bool requested();
void poll(Publish publish);
// Authenticated transport presence is independent of the short execution lease.
// Used only by background bootstrap, never to authorize feature execution.
bool peer_connected();
uint64_t publication_generation();
bool publication_current(uint64_t generation);
void set_cards_ready(bool ready);
// Called only by the haptic action worker. No UI-isolate I/O or WMS fallback.
bool perform_haptic(uint32_t profile,int operation,int64_t timestamp,int count);
}
