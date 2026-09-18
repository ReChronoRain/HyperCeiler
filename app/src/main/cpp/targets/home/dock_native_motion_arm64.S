/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Dart compressed-pointer ARM64 ABI, not AAPCS: x15 stack, x16/x17 scratch.
// Launcher addresses, class IDs, payload fields, tagged header/CID layout and
// the canonical false object are decoded from each live libapp.so generation.
// A generation owns an immutable symbol bank and trampoline for process life.
// No heap writes, retained pointers, C calls, FP changes or runtime safepoints.
// Notification uses only a raw nonblocking write(2) to our permanent eventfd.
#include "dock_native_layout.h"
.text
.p2align 4
.macro resolved reg, symbol
    adrp x\reg, \symbol
    ldr w\reg, [x\reg, :lo12:\symbol]
.endm
.macro resolved64 reg, symbol
    adrp x\reg, \symbol
    ldr x\reg, [x\reg, :lo12:\symbol]
.endm
.macro object_cid object, out, tmp, bank
    adrp x\tmp, dock_tagged_header_offset_\bank
    ldrsw x\tmp, [x\tmp, :lo12:dock_tagged_header_offset_\bank]
    ldr w\out, [x\object, x\tmp]
    resolved \tmp, dock_class_id_shift_\bank
    lsrv w\out, w\out, w\tmp
    resolved \tmp, dock_class_id_mask_\bank
    and w\out, w\out, w\tmp
.endm
.macro increment_counter symbol
    adrp x9, \symbol
    add x9, x9, :lo12:\symbol
1:
    ldaxr x10, [x9]
    add x10, x10, #1
    stlxr w11, x10, [x9]
    cbnz w11, 1b
.endm
.macro decrement_counter symbol
    adrp x9, \symbol
    add x9, x9, :lo12:\symbol
1:
    ldaxr x10, [x9]
    sub x10, x10, #1
    stlxr w11, x10, [x9]
    cbnz w11, 1b
.endm
.macro save_state
    stp x9, x10, [x15, #-16]!
    mrs x9, nzcv
    stp x9, x11, [x15, #-16]!
.endm
.macro restore_state
    ldp x9, x11, [x15], #16
    msr nzcv, x9
    ldp x9, x10, [x15], #16
.endm
.macro notify
    adrp x17, dock_motion_subscribed
    add x17, x17, :lo12:dock_motion_subscribed
    ldar w16, [x17]
    cbz w16, 99f
    stp x0, x1, [x15, #-16]!
    stp x2, x8, [x15, #-16]!
    adrp x17, dock_motion_event
    ldr w0, [x17, :lo12:dock_motion_event]
    adrp x1, dock_motion_one
    add x1, x1, :lo12:dock_motion_one
    mov x2, #8
    mov x8, #64
    svc #0
    ldp x2, x8, [x15], #16
    ldp x0, x1, [x15], #16
99:
.endm

.macro scale_entry bank
.global dock_motion_scale_entry_\bank
.hidden dock_motion_scale_entry_\bank
.type dock_motion_scale_entry_\bank, %function
dock_motion_scale_entry_\bank:
    save_state
    increment_counter dock_motion_active_callbacks
    increment_counter dock_motion_entry_hits
    tbz x2, #0, .Lscale_done_\bank
    object_cid 2, 16, 9, \bank
    resolved 9, dock_double_class_id_\bank
    cmp w16, w9
    b.ne .Lscale_done_\bank
    resolved 16, dock_double_value_offset_\bank
    ldr x10, [x2, x16]
    adrp x17, dock_motion_value
    add x17, x17, :lo12:dock_motion_value
.Lscale_retry_\bank:
    ldaxr x11, [x17]
    and x10, x10, #-4
    and x16, x11, #3
    orr x10, x10, x16
    cmp x10, x11
    b.eq .Lscale_done_\bank
    stlxr w16, x10, [x17]
    cbnz w16, .Lscale_retry_\bank
    increment_counter dock_motion_publish_hits
    notify
.Lscale_done_\bank:
    clrex
    adrp x16, dock_motion_scale_original_\bank
    ldr x16, [x16, :lo12:dock_motion_scale_original_\bank]
    decrement_counter dock_motion_active_callbacks
    restore_state
    br x16
.size dock_motion_scale_entry_\bank, .-dock_motion_scale_entry_\bank
.endm

// Exact Hotseat unlock projection. Unlike scale_entry, Dart passes the scalar raw in d0 and
// the _UnlockWidgetState receiver in x1. Follow only the resolver-published object-field chain;
// never retain a Dart heap pointer across this callback or introduce a runtime safepoint.
.macro unlock_scale_entry bank
.global dock_motion_unlock_scale_entry_\bank
.hidden dock_motion_unlock_scale_entry_\bank
.type dock_motion_unlock_scale_entry_\bank, %function
dock_motion_unlock_scale_entry_\bank:
    save_state
    increment_counter dock_motion_active_callbacks
    increment_counter dock_auto_aim_entry_hits
    // The hooked setter is a generic double setter, so the receiver-chain heuristic alone can
    // miss the real per-frame icon projection. The exact projection call site is unique: accept
    // it by return address (LR) first, and keep the chain as the fallback for other call sites.
    resolved64 16, dock_unlock_call_return_\bank
    cmp x30, x16
    b.eq .Lunlock_publish_\bank
    tbz x1, #0, .Lunlock_bad_receiver_\bank
    resolved 16, dock_unlock_state_widget_offset_\bank
    ldr w16, [x1, x16]
    tbz w16, #0, .Lunlock_bad_widget_\bank
    add x16, x16, x28, lsl #32
    cmp x16, x22
    b.eq .Lunlock_bad_widget_\bank
    resolved 17, dock_unlock_widget_cell_offset_\bank
    ldr w17, [x16, x17]
    tbz w17, #0, .Lunlock_bad_cell_\bank
    add x17, x17, x28, lsl #32
    cmp x17, x22
    b.eq .Lunlock_bad_cell_\bank
    resolved 16, dock_unlock_cell_container_offset_\bank
    ldr x10, [x17, x16]
    resolved64 9, dock_unlock_hotseat_container_0_\bank
    cmp x10, x9
    b.eq .Lunlock_publish_\bank
    resolved64 9, dock_unlock_hotseat_container_1_\bank
    cmp x10, x9
    b.eq .Lunlock_publish_\bank
    resolved64 9, dock_unlock_hotseat_container_2_\bank
    cmp x10, x9
    b.eq .Lunlock_publish_\bank
    resolved64 9, dock_unlock_hotseat_container_3_\bank
    cmp x10, x9
    b.eq .Lunlock_publish_\bank
    resolved64 9, dock_unlock_hotseat_container_4_\bank
    cmp x10, x9
    b.ne .Lunlock_bad_container_\bank
    b .Lunlock_publish_\bank
// Stage counters: a rejected projection must say which hop of the decoded state ->
// widget -> cell -> container chain failed, instead of silently never publishing.
.Lunlock_bad_receiver_\bank:
    increment_counter dock_auto_aim_receiver_bad
    b .Lunlock_done_\bank
.Lunlock_bad_widget_\bank:
    increment_counter dock_auto_aim_widget_bad
    b .Lunlock_done_\bank
.Lunlock_bad_cell_\bank:
    increment_counter dock_auto_aim_cell_bad
    b .Lunlock_done_\bank
.Lunlock_bad_container_\bank:
    increment_counter dock_auto_aim_container_miss
    b .Lunlock_done_\bank
.Lunlock_publish_\bank:
    fmov x10, d0
    and x10, x10, #-4
    orr x10, x10, #3
    adrp x17, dock_auto_aim_value
    add x17, x17, :lo12:dock_auto_aim_value
.Lunlock_retry_\bank:
    ldaxr x11, [x17]
    cmp x10, x11
    b.eq .Lunlock_done_\bank
    stlxr w16, x10, [x17]
    cbnz w16, .Lunlock_retry_\bank
    increment_counter dock_auto_aim_publish_hits
    notify
.Lunlock_done_\bank:
    clrex
    adrp x16, dock_motion_unlock_scale_original_\bank
    ldr x16, [x16, :lo12:dock_motion_unlock_scale_original_\bank]
    decrement_counter dock_motion_active_callbacks
    restore_state
    br x16
.size dock_motion_unlock_scale_entry_\bank, .-dock_motion_unlock_scale_entry_\bank
.endm

.macro scene_entry name, original, immediate, bank
.global \name
.hidden \name
.type \name, %function
\name:
    save_state
    increment_counter dock_motion_active_callbacks
    increment_counter dock_motion_entry_hits
    tbz x2, #0, 98f
    object_cid 2, 16, 9, \bank
    resolved 9, dock_params_class_id_\bank
    cmp w16, w9
    b.ne 98f
    mov x11, #0x3ff0000000000000
    resolved 16, dock_alpha_offset_\bank
    ldr x16, [x2, x16]
    cmp x16, x11
    b.ne 98f
    resolved 16, dock_surface_offset_\bank
    ldr w16, [x2, x16]
    tbz x16, #0, 98f
    add x16, x16, x28, lsl #32
    object_cid 16, 16, 9, \bank
    resolved 9, dock_false_from_null_\bank
    add x17, x22, x9
    object_cid 17, 17, 9, \bank
    cmp w16, w17
    b.ne 98f
    resolved 16, dock_recents_offset_\bank
    ldr w16, [x2, x16]
    tbz x16, #0, 98f
    add x16, x16, x28, lsl #32
    object_cid 16, 16, 9, \bank
    resolved 9, dock_false_from_null_\bank
    add x17, x22, x9
    object_cid 17, 17, 9, \bank
    cmp w16, w17
    b.ne 98f
    resolved 16, dock_scale_offset_\bank
    ldr x16, [x2, x16]
    mov x10, #0
    cmp x16, x11
    b.eq 80f
    b.hi 90f
    // 0.94 is a semantic scene threshold, not an address or object offset.
    mov x17, #0xae14
    movk x17, #0xe147, lsl #16
    movk x17, #0x147a, lsl #32
    movk x17, #0x3fee, lsl #48
    cmp x16, x17
    b.lo 90f
    mov x10, #1
    b 90f
80:
    mov x10, #2
90:
    adrp x17, dock_motion_value
    add x17, x17, :lo12:dock_motion_value
.if !\immediate
92:
    ldaxr x11, [x17]
    and x11, x11, #-4
    and x10, x10, #3
    orr x10, x10, x11
    stlxr w16, x10, [x17]
    cbnz w16, 92b
.else
    and x16, x16, #-4
    orr x10, x10, x16
    stlr x10, [x17]
.endif
    increment_counter dock_motion_publish_hits
    notify
98:
    adrp x16, \original
    ldr x16, [x16, :lo12:\original]
    decrement_counter dock_motion_active_callbacks
    restore_state
    br x16
.size \name, .-\name
.endm

.macro motion_bank bank
scale_entry \bank
scene_entry dock_motion_anim_entry_\bank, dock_motion_anim_original_\bank, 0, \bank
scene_entry dock_motion_set_entry_\bank, dock_motion_set_original_\bank, 1, \bank
unlock_scale_entry \bank
.endm

motion_bank 0
motion_bank 1
motion_bank 2
motion_bank 3
motion_bank 4
motion_bank 5
motion_bank 6
motion_bank 7
motion_bank 8
motion_bank 9
motion_bank 10
motion_bank 11
motion_bank 12
motion_bank 13
motion_bank 14
motion_bank 15

.section .note.GNU-stack,"",%progbits
