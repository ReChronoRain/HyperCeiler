/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_dock_geometry.h"
#include "home_native_glass_client.h"
#include <cstdint>
struct ASurfaceControl;
// One consumer of the Dart eventfd. Staging fans out to a separate legacy IPC
// queue; a blocked/missing WMS endpoint never blocks the local rendering thread.
int hc_native_glass_motion_begin(int dart_event);
void hc_native_glass_motion_consumed();
void hc_native_glass_motion_target(ASurfaceControl* layer,const home_dock::Bounds& bounds,
    int32_t viewport_width,int32_t viewport_height,float density,int32_t style,home_native_glass::FrameEffects effects);
void hc_native_glass_motion_clear();
void hc_native_glass_motion_notify();
uint64_t hc_native_glass_motion_frames();
// Existing runtime-verified Dart edit producer, unknown is not editing.
bool hc_native_glass_editing();
