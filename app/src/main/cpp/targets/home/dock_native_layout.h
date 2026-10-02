/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
// Every independently mapped Dart runtime gets an immutable bank. Assembly
// reads each value from a relocated module symbol, so no launcher address,
// class ID, heap-field offset or Dart tag layout is encoded in this header or
// in dock_native_motion_arm64.S. A bank is never reused in the same process;
// callbacks which entered an older generation can therefore finish safely.
// When all 16 banks are consumed the install path fails closed and refuses
// new generations: recycling would republish a bank's layout symbols while
// callbacks of the previous generation may still be in flight.
#define DOCK_MOTION_BANKS(X) \
    X(0) X(1) X(2) X(3) X(4) X(5) X(6) X(7) \
    X(8) X(9) X(10) X(11) X(12) X(13) X(14) X(15)

#ifndef __ASSEMBLER__
#include <cstdint>
extern "C" {
#define DECLARE_DOCK_MOTION_BANK(bank) \
    extern uint32_t dock_params_class_id_##bank; \
    extern uint32_t dock_double_class_id_##bank; \
    extern int32_t dock_tagged_header_offset_##bank; \
    extern uint32_t dock_class_id_shift_##bank; \
    extern uint32_t dock_class_id_mask_##bank; \
    extern uint32_t dock_alpha_offset_##bank; \
    extern uint32_t dock_scale_offset_##bank; \
    extern uint32_t dock_surface_offset_##bank; \
    extern uint32_t dock_recents_offset_##bank; \
    extern uint32_t dock_double_value_offset_##bank; \
    extern uint32_t dock_false_from_null_##bank; \
    extern uint32_t dock_unlock_state_widget_offset_##bank; \
    extern uint32_t dock_unlock_widget_cell_offset_##bank; \
    extern uint32_t dock_unlock_cell_container_offset_##bank; \
    extern int64_t dock_unlock_hotseat_container_0_##bank; \
    extern int64_t dock_unlock_hotseat_container_1_##bank; \
    extern int64_t dock_unlock_hotseat_container_2_##bank; \
    extern int64_t dock_unlock_hotseat_container_3_##bank; \
    extern int64_t dock_unlock_hotseat_container_4_##bank; \
    extern void *dock_motion_scale_original_##bank; \
    extern void *dock_motion_anim_original_##bank; \
    extern void *dock_motion_set_original_##bank; \
    extern void *dock_motion_unlock_scale_original_##bank; \
    void dock_motion_scale_entry_##bank(); \
    void dock_motion_anim_entry_##bank(); \
    void dock_motion_set_entry_##bank(); \
    void dock_motion_unlock_scale_entry_##bank();
DOCK_MOTION_BANKS(DECLARE_DOCK_MOTION_BANK)
#undef DECLARE_DOCK_MOTION_BANK
}
#endif
