/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Trampolines that capture the launcher's shared geometry objects.
 *
 * Why capture instead of hooking each accessor: Dart AOT inlines a getter as small as
 * `ldur d0, [x0, #imm]` at almost every call site, so the standalone accessor function is called
 * rarely or never. Hooking it therefore changes nothing. What every consumer - inlined or not - does
 * read is the shared configuration object, so the layout module captures the pointer to those
 * objects once and rewrites the fields in place (see home_layout_hooks.cpp).
 *
 * The register rules are the dock stubs': the Dart ABI keeps its stack in x15 (not AAPCS sp),
 * reserves x16/x17 as scratch, and expects a stub to preserve x9-x11 plus the flags across the hook.
 * x28 (the compressed-pointer heap base) is captured too, because the nested objects are reached
 * through a compressed pointer field.
 *
 * The trampolines are plain tail calls around the original accessor: nothing is adjusted here, so the
 * launcher's own value is returned untouched and the C side owns every decision.
 */
#include "targets/home/home_layout_knobs.h"

.text
.p2align 4

.macro dart_save
    stp x9, x10, [x15, #-16]!
    mrs x9, nzcv
    stp x9, x11, [x15, #-16]!
.endm

.macro dart_restore
    ldp x9, x11, [x15], #16
    msr nzcv, x9
    ldp x9, x10, [x15], #16
.endm

.macro dynamic_offset symbol
    adrp x16, \symbol
    ldr x17, [x16, :lo12:\symbol]
.endm
.macro dynamic_load reg, base, symbol
    dynamic_offset \symbol
    ldr \reg, [\base, x17]
.endm
.macro dynamic_store reg, base, symbol
    dynamic_offset \symbol
    str \reg, [\base, x17]
.endm

/* No relocated Dart BL and no heap allocation. The original putInt BL and return PC
 * retain their stack map. Both new routes rejoin the original serializer frame. */
.globl hc_gadget_select_entry
.p2align 4
hc_gadget_select_entry:
    dart_save
    adrp x9, hc_gadget_bridge_enabled
    add x9, x9, :lo12:hc_gadget_bridge_enabled
    ldar w9, [x9]
    cbz w9, 1f
    dynamic_offset hc_gadget_cid
    cmp x1, x17
    b.ne 1f
    dynamic_load x9, x29, hc_gadget_model_slot
    dynamic_load w10, x9, hc_gadget_model_field
    cmp w10, #24 // Gadget12 semantic ID, compressed Smi; not a CID/address.
    b.ne 1f
    dynamic_load x10, x9, hc_gadget_width_field
    cmp x10, #1
    b.ne 1f
    dynamic_load x10, x9, hc_gadget_height_field
    cmp x10, #1
    b.ne 1f
    dart_restore
    dynamic_load x0, x29, hc_gadget_model_slot
    dynamic_load x1, x29, hc_gadget_bundle_slot
    dynamic_load x2, x27, hc_gadget_widget_pool
    mov x3, #0x1802
    movk x3, #0x3086, lsl #16
    adrp x16, hc_gadget_put_int
    ldr x16, [x16, :lo12:hc_gadget_put_int]
    br x16
1:
    dart_restore
    dynamic_offset hc_gadget_select_cid
    cmp x1, x17
    b.ne 2f
    dynamic_load x0, x29, hc_gadget_model_slot
    dynamic_load x1, x29, hc_gadget_bundle_slot
    adrp x16, hc_gadget_select_continue
    ldr x16, [x16, :lo12:hc_gadget_select_continue]
    br x16
2:
    adrp x16, hc_gadget_original_reject
    ldr x16, [x16, :lo12:hc_gadget_original_reject]
    br x16

.globl hc_gadget_return_entry
.p2align 4
hc_gadget_return_entry:
    dart_save
    dynamic_load x9, x29, hc_gadget_model_slot
    ldur x10, [x9, #-1]
    ubfx x10, x10, #12, #20
    dynamic_offset hc_gadget_cid
    cmp x10, x17
    b.ne 3f
    dart_restore
    adrp x16, hc_gadget_common
    ldr x16, [x16, :lo12:hc_gadget_common]
    br x16
3:
    dart_restore
    dynamic_load x0, x29, hc_gadget_model_slot
    dynamic_load w3, x0, hc_gadget_marker_field
    add x3, x3, x28, lsl #32
    adrp x16, hc_gadget_return_continue
    ldr x16, [x16, :lo12:hc_gadget_return_continue]
    br x16

.globl hc_gadget_span_entry
.p2align 4
hc_gadget_span_entry:
    dart_save
    adrp x9, hc_gadget_bridge_enabled
    add x9, x9, :lo12:hc_gadget_bridge_enabled
    ldar w9, [x9]
    cbz w9, 1f
    dynamic_load x9, x29, hc_gadget_handler_slot
    dynamic_load w9, x9, hc_gadget_handler_field
    add x9, x9, x28, lsl #32
    ldur x10, [x9, #-1]
    ubfx x10, x10, #12, #20
    dynamic_offset hc_gadget_cid
    cmp x10, x17
    b.ne 1f
    dynamic_load w10, x9, hc_gadget_model_field
    cmp w10, #24
    b.ne 1f
    dynamic_load x10, x9, hc_gadget_width_field
    cmp x10, #1
    b.ne 1f
    dynamic_load x10, x9, hc_gadget_height_field
    cmp x10, #1
    b.ne 1f
    dart_restore
    b 2f
1:
    dart_restore
    tbnz w0, #4, 3f
2:
    dynamic_load x0, x29, hc_gadget_receiver_slot
    dynamic_load x2, x29, hc_gadget_handler_slot
    mov x1, x0
    adrp x16, hc_gadget_span_continue
    ldr x16, [x16, :lo12:hc_gadget_span_continue]
    br x16
3:
    adrp x16, hc_gadget_span_reject
    ldr x16, [x16, :lo12:hc_gadget_span_reject]
    br x16

/*
 * capture_trampoline entry, original, object, hits
 *
 * Calls the original accessor, publishes its result (x0) and the heap base (x28), and then applies
 * the requested field deltas *before returning*. Doing it here rather than from a background thread
 * is the whole point: the caller reads the fields immediately after the call, and the desktop caches
 * everything it computed, so a write that lands after the layout has run changes nothing at all.
 *
 * x0 must survive the C helper call, and x15 (the Dart stack) is callee-saved for the Dart caller but
 * scratch for AAPCS, so both are parked on the Dart stack around the call. SP is the thread's native
 * stack and is valid for the C helper's own frame.
 */
.macro capture_trampoline entry, original, object, hits
.globl \entry
.p2align 4
\entry:
    dart_save
    adrp x9, \hits
    add x9, x9, :lo12:\hits
    ldr x10, [x9]
    add x10, x10, #1
    str x10, [x9]
    stp x30, xzr, [x15, #-16]!
    adrp x9, \original
    add x9, x9, :lo12:\original
    ldr x9, [x9]
    blr x9
    ldp x30, xzr, [x15], #16
    adrp x9, \object
    add x9, x9, :lo12:\object
    stlr x0, [x9]
    adrp x9, hc_layout_heap_base
    add x9, x9, :lo12:hc_layout_heap_base
    stlr x28, [x9]
    stp x0, x30, [sp, #-16]!
    stp x15, xzr, [sp, #-16]!
    mov x1, x30
    bl hc_layout_apply_now
    ldp x15, xzr, [sp], #16
    ldp x0, x30, [sp], #16
    dart_restore
    ret
.endm

capture_trampoline hc_layout_config_capture_entry, \
    hc_layout_config_capture_original, hc_layout_config_object, hc_layout_config_capture_hits
capture_trampoline hc_layout_dock_capture_entry, \
    hc_layout_dock_capture_original, hc_layout_dock_object, hc_layout_dock_capture_hits

/* Keep the launcher's selection intact. Clone only through the verified nursery fast path;
 * a full nursery returns the original color, never an untracked Dart GC slow-path call.
 * Only the private clone is recolored, not a shared pool constant. */
.globl hc_title_color_entry
.p2align 4
hc_title_color_entry:
    dart_save
    adrp x9, hc_title_color_enabled
    add x9, x9, :lo12:hc_title_color_enabled
    ldar w9, [x9]
    cbz w9, 1f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, x1
    mov x1, x26
    bl hc_title_color_clone
    str x0, [sp, #8]
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
1:
    dart_restore
    dynamic_store x1, x29, hc_title_color_frame
    dynamic_load x0, x26, hc_title_color_thread
    dynamic_load x0, x0, hc_title_color_static
    cmp w0, w22
    adrp x16, hc_title_color_continue
    add x16, x16, :lo12:hc_title_color_continue
    ldr x16, [x16]
    br x16

.globl hc_folder_title_color_entry
.p2align 4
hc_folder_title_color_entry:
    dart_save
    adrp x9, hc_title_color_enabled
    add x9, x9, :lo12:hc_title_color_enabled
    ldar w9, [x9]
    cbz w9, 1f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, x1
    mov x1, x26
    bl hc_title_color_clone
    str x0, [sp, #8]
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
1:
    dart_restore
    dynamic_store x1, x29, hc_folder_title_color_frame
    dynamic_load x0, x26, hc_folder_title_color_thread
    dynamic_load x0, x0, hc_folder_title_color_static
    cmp w0, w22
    adrp x16, hc_folder_title_color_continue
    add x16, x16, :lo12:hc_folder_title_color_continue
    ldr x16, [x16]
    br x16

/* Scale only the desktop folder name's fresh font-size box. Preserve q1 and
 * original style allocation call/slow-path branch/GC stack map. */
.globl hc_folder_title_scale_entry
.p2align 4
hc_folder_title_scale_entry:
    dynamic_load d0, x29, hc_folder_title_font_frame
    str q1, [x15, #-16]!
    adrp x16, hc_folder_title_scale_bits
    add x16, x16, :lo12:hc_folder_title_scale_bits
    ldar x17, [x16]
    fmov d1, x17
    fmul d0, d0, d1
    ldr q1, [x15], #16
    dynamic_offset hc_folder_title_nursery
    add x16, x26, x17
    ldp x1, x2, [x16]
    dynamic_offset hc_folder_title_allocation
    add x1, x1, x17
    cmp x2, x1
    adrp x16, hc_folder_title_scale_continue
    ldr x16, [x16, :lo12:hc_folder_title_scale_continue]
    br x16

.globl hc_folder_title_scale4_entry
.p2align 4
hc_folder_title_scale4_entry:
    dynamic_load d0, x29, hc_folder_title_font_frame
    str q1, [x15, #-16]!
    adrp x16, hc_folder_title_scale_bits
    add x16, x16, :lo12:hc_folder_title_scale_bits
    ldar x17, [x16]
    fmov d1, x17
    fmul d0, d0, d1
    ldr q1, [x15], #16
    dynamic_offset hc_folder_title_nursery
    add x16, x26, x17
    ldp x1, x4, [x16]
    dynamic_offset hc_folder_title_allocation
    add x1, x1, x17
    cmp x4, x1
    adrp x16, hc_folder_title_scale_continue
    ldr x16, [x16, :lo12:hc_folder_title_scale_continue]
    br x16

/* Original caption is replaced only in the drawing local. Root the private UTF16 string
 * at the original stack-map slot before resuming the untouched allocation BL. */
.globl hc_title_custom_entry
.p2align 4
hc_title_custom_entry:
    dynamic_load x2, x29, hc_title_custom_widget_frame
    dynamic_load w3, x2, hc_title_custom_label_field
    add x3, x3, x28, lsl #32
    dart_save
    adrp x9, hc_title_custom_enabled
    add x9, x9, :lo12:hc_title_custom_enabled
    ldar w9, [x9]
    cbz w9, 12f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, x3
    dynamic_offset hc_title_custom_model_field
    ldr w1, [x2, x17]
    add x1, x1, x28, lsl #32
    mov x2, x28
    mov x3, x26
    mov x4, x21
    mov x5, x22
    mov w6, #0
    bl hc_title_custom_label
    str x0, [sp, #24]
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
12:
    dart_restore
    dynamic_store x3, x29, hc_title_custom_label_frame
    adrp x16, hc_title_custom_continue
    add x16, x16, :lo12:hc_title_custom_continue
    ldr x16, [x16]
    br x16

/* AppIcon.build keeps its original getter call. Replace its following no-call window only;
 * every other caller keeps the launcher value, and GC never sees a foreign Dart return PC. */
.globl hc_drawer_title_entry
.p2align 4
hc_drawer_title_entry:
    dart_save
    adrp x9, hc_drawer_title_override
    add x9, x9, :lo12:hc_drawer_title_override
    ldar w9, [x9]
    cbz w9, 2f
    adrp x10, hc_drawer_title_baseline
    add x10, x10, :lo12:hc_drawer_title_baseline
    str d0, [x10]
    adrp x9, hc_drawer_title_sp
    add x9, x9, :lo12:hc_drawer_title_sp
    ldar w9, [x9]
    scvtf d0, w9
2:
    dart_restore
    dynamic_load x0, x29, hc_drawer_font_widget
    dynamic_store d0, x29, hc_drawer_font_frame
    dynamic_load w1, x0, hc_drawer_font_field
    add x1, x1, x28, lsl #32
    adrp x16, hc_drawer_title_continue
    add x16, x16, :lo12:hc_drawer_title_continue
    ldr x16, [x16]
    br x16

/* Exact four displaced instructions from _buildTextWidget+0xb4. Apply the desktop size
 * before it is cached in the builder's local; default 12 replays the launcher value. */
.globl hc_desktop_title_entry
.p2align 4
hc_desktop_title_entry:
    dynamic_load d0, x0, hc_desktop_font_field
    dart_save
    // Replay the original custom-config branch, not a global "drawer currently visible" flag.
    dynamic_load x9, x29, hc_desktop_widget_frame
    dynamic_load w17, x9, hc_desktop_config_field
    add x17, x17, x28, lsl #32
    mov x9, x17
    dynamic_load w17, x9, hc_desktop_config_flag
    tbz w17, #4, 13f
    adrp x9, hc_desktop_title_sp
    add x9, x9, :lo12:hc_desktop_title_sp
    adrp x10, hc_desktop_title_baseline
    add x10, x10, :lo12:hc_desktop_title_baseline
    b 14f
13:
    adrp x9, hc_drawer_title_sp
    add x9, x9, :lo12:hc_drawer_title_sp
    adrp x10, hc_shared_drawer_title_baseline
    add x10, x10, :lo12:hc_shared_drawer_title_baseline
14:
    ldar w9, [x9]
    cmp w9, #12
    b.eq 3f
    str d0, [x10]
    scvtf d0, w9
3:
    dart_restore
    dynamic_load x0, x29, hc_desktop_widget_frame
    dynamic_store d0, x29, hc_desktop_font_frame
    dynamic_load w1, x0, hc_desktop_config_field
    adrp x16, hc_desktop_title_continue
    add x16, x16, :lo12:hc_desktop_title_continue
    ldr x16, [x16]
    br x16

/* Resize the matching text line box too. Otherwise a larger font is clipped to the old
 * twelve-sp box. Replay the no-call load window, preserving every register and NZCV. */
.globl hc_desktop_title_height_entry
.p2align 4
hc_desktop_title_height_entry:
    dynamic_load w1, x0, hc_desktop_height_text_field
    add x1, x1, x28, lsl #32
    dynamic_load d0, x1, hc_desktop_height_field
    dart_save
    dynamic_load x9, x29, hc_desktop_widget_frame
    dynamic_load w17, x9, hc_desktop_config_field
    add x17, x17, x28, lsl #32
    mov x9, x17
    dynamic_load w17, x9, hc_desktop_config_flag
    tbz w17, #4, 15f
    adrp x9, hc_desktop_title_sp
    add x9, x9, :lo12:hc_desktop_title_sp
    adrp x10, hc_desktop_title_baseline
    add x10, x10, :lo12:hc_desktop_title_baseline
    b 16f
15:
    adrp x9, hc_drawer_title_sp
    add x9, x9, :lo12:hc_drawer_title_sp
    adrp x10, hc_shared_drawer_title_baseline
    add x10, x10, :lo12:hc_shared_drawer_title_baseline
16:
    ldar w9, [x9]
    cmp w9, #12
    b.eq 7f
    stp d1, d2, [sp, #-16]!
    ldr d2, [x10]
    fcmp d2, #0.0
    b.le 8f
    scvtf d1, w9
    fdiv d1, d1, d2
    fmul d0, d0, d1
8:
    ldp d1, d2, [sp], #16
7:
    dart_restore
    dynamic_store d0, x29, hc_desktop_height_frame
    adrp x16, hc_desktop_title_height_continue
    add x16, x16, :lo12:hc_desktop_title_height_continue
    ldr x16, [x16]
    br x16

/* Drawer line box consumes a fixed titleHeight, not the supplied font-size argument. */
.globl hc_drawer_title_height_entry
.p2align 4
hc_drawer_title_height_entry:
    dart_save
    adrp x9, hc_drawer_title_override
    add x9, x9, :lo12:hc_drawer_title_override
    ldar w9, [x9]
    cbz w9, 9f
    stp d1, d2, [sp, #-16]!
    adrp x10, hc_drawer_title_baseline
    add x10, x10, :lo12:hc_drawer_title_baseline
    ldr d2, [x10]
    fcmp d2, #0.0
    b.le 10f
    adrp x9, hc_drawer_title_sp
    add x9, x9, :lo12:hc_drawer_title_sp
    ldar w9, [x9]
    scvtf d1, w9
    fdiv d1, d1, d2
    fmul d0, d0, d1
10:
    ldp d1, d2, [sp], #16
9:
    dart_restore
    dynamic_load x0, x29, hc_drawer_height_widget
    dynamic_store d0, x29, hc_drawer_height_frame
    dynamic_load w1, x0, hc_drawer_height_field
    add x1, x1, x28, lsl #32
    dart_save
    adrp x9, hc_title_custom_enabled
    add x9, x9, :lo12:hc_title_custom_enabled
    ldar w9, [x9]
    cbz w9, 12f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x17, x0
    mov x0, x1
    mov x1, x17
    mov x2, x28
    mov x3, x26
    mov x4, x21
    mov x5, x22
    mov w6, #1
    bl hc_title_custom_label
    str x0, [sp, #8]
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
12:
    dart_restore
    adrp x16, hc_drawer_title_height_continue
    add x16, x16, :lo12:hc_drawer_title_height_continue
    ldr x16, [x16]
    br x16

/* Preserve XSpace/other title prefixes. Only the exact original new-install asset is hidden. */
.globl hc_title_prefix_entry
.p2align 4
hc_title_prefix_entry:
    dart_save
    adrp x9, hc_title_hide_new_install
    add x9, x9, :lo12:hc_title_hide_new_install
    ldar w9, [x9]
    cbz w9, 1f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, x1
    bl hc_title_is_new_asset
    cbz w0, 11f
    dynamic_load x0, x27, hc_title_empty_pool
    str x0, [sp, #8]
11:
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
1:
    dart_restore
    mov x0, x1
    mov x15, x29
    ldp x29, x30, [x15], #16
    ret

/* Scope the folder answer to the drawing controller, not model/database processing. */
.globl hc_title_folder_new_entry
.p2align 4
hc_title_folder_new_entry:
    dart_save
    adrp x9, hc_title_hide_new_install
    add x9, x9, :lo12:hc_title_hide_new_install
    ldar w9, [x9]
    cbz w9, 5f
    adrp x9, hc_title_folder_new_caller
    add x9, x9, :lo12:hc_title_folder_new_caller
    ldr x9, [x9]
    cmp x30, x9
    b.ne 5f
    dart_restore
    add x0, x22, #0x30 // Dart false
    ret
5:
    dart_restore
    adrp x16, hc_title_folder_new_original
    add x16, x16, :lo12:hc_title_folder_new_original
    ldr x16, [x16]
    br x16

.globl hc_title_light_entry
.p2align 4
hc_title_light_entry:
    adrp x16, hc_title_hide_new_install
    add x16, x16, :lo12:hc_title_hide_new_install
    ldar w16, [x16]
    cbz w16, 6f
    mov x0, x22 // Original void-return method's Dart null.
    ret
6:
    adrp x16, hc_title_light_original
    add x16, x16, :lo12:hc_title_light_original
    ldr x16, [x16]
    br x16

/*
 * geometry_trampoline entry, original, delta, enabled, hits
 *
 * The second strategy, used for the functions that are too large for Dart to inline (the layout
 * aggregators such as calHotseatOffsetY): call the original, then add the user's delta to the double
 * it returned. These are the functions that actually decide where something is drawn, so a hook on
 * one of them survives the inlining that makes the small accessors useless.
 */
.macro geometry_trampoline entry, original, delta, enabled, hits, last, caller
.globl \entry
.p2align 4
\entry:
    dart_save
    /*
     * x30 still holds the Dart caller's return address, and x9 is usable only now: dart_save has
     * parked the caller's x9/x10/x11 on the Dart stack. Capturing the caller before dart_save
     * clobbered x9, which the Dart ABI requires a callee to preserve, and the launcher restarted in a
     * loop - the first thing the pass-through probe caught, before any value was ever changed.
     */
    adrp x9, \caller
    add x9, x9, :lo12:\caller
    str x30, [x9]
    adrp x9, \hits
    add x9, x9, :lo12:\hits
    ldr x10, [x9]
    add x10, x10, #1
    str x10, [x9]
    stp x30, xzr, [x15, #-16]!
    adrp x9, \original
    add x9, x9, :lo12:\original
    ldr x9, [x9]
    blr x9
    ldp x30, xzr, [x15], #16
    /* The value the launcher itself produced, before any delta. */
    adrp x9, \last
    add x9, x9, :lo12:\last
    str d0, [x9]
    adrp x9, \enabled
    add x9, x9, :lo12:\enabled
    ldar w10, [x9]
    cbz w10, 1f
    adrp x9, \delta
    add x9, x9, :lo12:\delta
    ldr d1, [x9]
    /* Bottom margin is an absolute dp preference, not a stock-relative inset.
     * Still execute the validated original getter for its original semantics
     * and preserve it verbatim when disabled. No heap fields are overwritten. */
    .ifc \entry,hc_layout_dart_HotseatMargin_entry
    fmov d0, d1
    .else
    fadd d0, d0, d1
    .endif
1:
    dart_restore
    ret
.endm

/*
 * The knob list is expanded by the C preprocessor, not the assembler: the labels contain spaces and
 * the names need pasting, both of which cpp handles while GAS macro arguments do not.
 */
#define HC_ASM_KNOB(name, column, label, dflt, lo, hi) \
    geometry_trampoline hc_layout_dart_##name##_entry, \
        hc_layout_dart_##name##_original, hc_layout_dart_##name##_delta, \
        hc_layout_dart_##name##_enabled, hc_layout_dart_##name##_hits, \
        hc_layout_dart_##name##_last, hc_layout_dart_##name##_caller;
HC_LAYOUT_KNOBS(HC_ASM_KNOB)
#undef HC_ASM_KNOB

/* Interior original RenderBox layout splices. Preserve all live Dart registers
 * and NZCV; helpers write verified scalar frame slots (indicator also reuses its
 * freshly allocated original EdgeInsets). They never reserve Dart heap space.
 * Original loops retain constraints, hit testing and ParentData.offset ownership. */
.macro workspace_layout_splice name, slot, occupied
.globl \name
.p2align 4
\name:
    dart_save
    adrp x9, hc_layout_dart_\slot\()_hits
    add x9, x9, :lo12:hc_layout_dart_\slot\()_hits
    ldr x10, [x9]
    add x10, x10, #1
    str x10, [x9]
    adrp x9, hc_layout_dart_\slot\()_enabled
    add x9, x9, :lo12:hc_layout_dart_\slot\()_enabled
    ldarb w10, [x9] // Acquire the release-published scalar/gate pair.
    cbz w10, 1f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, x29
    mov x1, x28
    .if \occupied == 3
    ldr x2, [sp, #0] /* original x0: freshly allocated EdgeInsets */
    mov x3, x22
    bl hc_layout_indicator_position
    .else
    mov x2, #\occupied
    bl hc_layout_workspace_layout
    .endif
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
1:
    dart_restore
    adrp x16, hc_layout_dart_\slot\()_original
    add x16, x16, :lo12:hc_layout_dart_\slot\()_original
    ldr x16, [x16]
    br x16
.endm
workspace_layout_splice hc_layout_workspace_entry, WorkspaceTop, 0
workspace_layout_splice hc_layout_workspace_occupied_entry, WorkspaceBottom, 1
workspace_layout_splice hc_layout_hotseat_horizontal_entry, WorkspaceSide, 2

/* Original coordinate/size arithmetic, not a getter-return interceptor.
 * No Dart call is relocated: all five windows contain only loads/arithmetic.
 * Native SP stays 16-byte aligned; Dart x15 retains its 8-byte ABI alignment.
 * The helper replays the verified outputs, then resumes at patch+16. */
.macro folder_geometry_splice name, kind
.globl \name
.p2align 4
\name:
    dart_save
    adrp x9, hc_layout_folder_enabled
    add x9, x9, :lo12:hc_layout_folder_enabled
    ldar w10, [x9]
    cbz w10, 2f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, x29
    mov x1, x28
    mov x2, sp
    mov x3, #\kind
    bl hc_layout_folder_body
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
    dart_restore
    adrp x16, hc_layout_folder_resume
    add x16, x16, :lo12:hc_layout_folder_resume
    ldr x16, [x16, #(\kind * 8)]
    br x16
2:
    // Partial registration must use untouched original arithmetic, including
    // the screen-index local; never run a half-installed scalar carry path.
    dart_restore
    adrp x16, hc_layout_folder_original
    add x16, x16, :lo12:hc_layout_folder_original
    ldr x16, [x16, #(\kind * 8)]
    br x16
.endm
folder_geometry_splice hc_layout_folder_0_entry, 0
folder_geometry_splice hc_layout_folder_1_entry, 1
folder_geometry_splice hc_layout_folder_2_entry, 2
folder_geometry_splice hc_layout_folder_3_entry, 3
folder_geometry_splice hc_layout_folder_4_entry, 4

/* The drop-back center's X and Y are calculated in separate no-call windows
 * of the launcher's original function. Retain its Dart frame and replay only
 * those four verified instructions per site through native scalar outputs. */
.macro drop_geometry_splice name, kind
.globl \name
.p2align 4
\name:
    dart_save
    adrp x9, hc_layout_drop_enabled
    add x9, x9, :lo12:hc_layout_drop_enabled
    ldar w10, [x9]
    cbz w10, 2f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, x29
    mov x1, x28
    mov x2, sp
    mov x3, #\kind
    bl hc_layout_drop_body
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
    dart_restore
    adrp x16, hc_layout_drop_resume
    add x16, x16, :lo12:hc_layout_drop_resume
    ldr x16, [x16, #(\kind * 8)]
    br x16
2:
    dart_restore
    adrp x16, hc_layout_drop_original
    add x16, x16, :lo12:hc_layout_drop_original
    ldr x16, [x16, #(\kind * 8)]
    br x16
.endm
drop_geometry_splice hc_layout_drop_0_entry, 0
drop_geometry_splice hc_layout_drop_1_entry, 1

/* Both branches use the same original outer Insets. Entry runs after its bottom
 * store; the helper changes only that object, never the animation/height frame local. */
workspace_layout_splice hc_layout_capsule_entry, IndicatorMargin, 3

/* The original screen-indicator builder owns its Dart frame and every Dart call.
 * For edit-only mode, route its existing isInEditing call through both page-count
 * paths, then choose its original empty-widget or original dot-builder branch.
 * No foreign call frame, cached editing state, timer, or getter-return override. */
.globl hc_layout_indicator_policy_entry
.p2align 4
hc_layout_indicator_policy_entry:
    dart_save
    adrp x9, hc_layout_indicator_mode
    add x9, x9, :lo12:hc_layout_indicator_mode
    ldar w10, [x9]
    cmp w10, #2
    b.ne 1f
    dart_restore
    adrp x16, hc_layout_indicator_edit_call
    add x16, x16, :lo12:hc_layout_indicator_edit_call
    ldr x16, [x16]
    br x16
1:
    dart_restore
    adrp x16, hc_layout_dart_SearchBarMargin_original
    add x16, x16, :lo12:hc_layout_dart_SearchBarMargin_original
    ldr x16, [x16]
    br x16

.globl hc_layout_indicator_edit_result_entry
.p2align 4
hc_layout_indicator_edit_result_entry:
    dart_save
    adrp x9, hc_layout_indicator_mode
    add x9, x9, :lo12:hc_layout_indicator_mode
    ldar w10, [x9]
    cmp w10, #2
    b.ne 1f
    adrp x9, hc_layout_dart_IndicatorDot_hits
    add x9, x9, :lo12:hc_layout_dart_IndicatorDot_hits
    ldr x10, [x9]
    add x10, x10, #1
    str x10, [x9]
    /* Dart true = null+0x20 (bit 4 clear), false = null+0x30 (bit 4 set). */
    tbnz w0, #4, 2f
    dart_restore
    adrp x16, hc_layout_indicator_build_dots
    add x16, x16, :lo12:hc_layout_indicator_build_dots
    ldr x16, [x16]
    br x16
2:
    dart_restore
    /* +0x408 lies inside the 16-byte patched window. Replay its verified pool load
     * here, then enter the untouched epilogue at +0x40c, never the patch literal. */
    dynamic_load x0, x27, hc_indicator_empty_pool
    adrp x16, hc_layout_indicator_build_empty
    add x16, x16, :lo12:hc_layout_indicator_build_empty
    ldr x16, [x16]
    br x16
1:
    dart_restore
    adrp x16, hc_layout_dart_IndicatorDot_original
    add x16, x16, :lo12:hc_layout_dart_IndicatorDot_original
    ldr x16, [x16]
    br x16

/* Slide-only policy at the original selected-type decision. A normal idle
 * refresh requesting dots uses hidden state 3; the original hide/animate switch
 * and timeout closure already leave all visible flags false for that state.
 * Swipe, editing, capsule and all-apps requests retain their original type. */
.globl hc_layout_indicator_slide_only_entry
.p2align 4
hc_layout_indicator_slide_only_entry:
    dart_save
    adrp x9, hc_layout_indicator_mode
    add x9, x9, :lo12:hc_layout_indicator_mode
    ldar w10, [x9]
    cmp w10, #2
    b.ne 3f
    /* The capsule's swipe path also requests normal dots (type 0). Preserve
     * editing/capsule/drawer types, and let original type 3 clear visible flags. */
    cbnz x3, 1f
    mov x3, #3
    b 1f
3:
    cmp w10, #1
    b.ne 1f
    cbnz x3, 1f
    ldr x10, [x29, #8]
    adrp x9, hc_layout_indicator_idle_caller
    add x9, x9, :lo12:hc_layout_indicator_idle_caller
    ldr x9, [x9]
    cmp x10, x9
    b.ne 1f
    mov x3, #3
1:
    dart_restore
    adrp x16, hc_layout_dart_SearchBarWidth_original
    add x16, x16, :lo12:hc_layout_dart_SearchBarWidth_original
    ldr x16, [x16]
    br x16

/* Read-only slot-2 calibration: was the capsule's new Container.margin consumed by Flutter? */
.globl hc_layout_container_probe_entry
.p2align 4
hc_layout_container_probe_entry:
    dart_save
    adrp x9, hc_layout_dart_WorkspaceTop_hits
    add x9, x9, :lo12:hc_layout_dart_WorkspaceTop_hits
    ldr x10, [x9]
    add x10, x10, #1
    str x10, [x9]
    stp x1, x2, [x15, #-16]!
    stp x30, xzr, [x15, #-16]!
    mov x0, x1
    mov x1, x28
    mov x2, x22
    stp x15, xzr, [sp, #-16]!
    bl hc_layout_probe_container
    ldp x15, xzr, [sp], #16
    ldp x30, xzr, [x15], #16
    ldp x1, x2, [x15], #16
    stp x30, xzr, [x15, #-16]!
    adrp x9, hc_layout_dart_WorkspaceTop_original
    add x9, x9, :lo12:hc_layout_dart_WorkspaceTop_original
    ldr x9, [x9]
    blr x9
    ldp x30, xzr, [x15], #16
    dart_restore
    ret

/*
 * Dart consumer of the Rust-owned ratio: the shared `getAnimDurationRatio` entry when it binds,
 * else the flight-only `FlightCohort.animDurationRatio` (see bind_animation_consumer). The
 * original Dart function still runs, including its object and state checks; the enabled path
 * replaces only the returned double. A Dart boundary is used because the LSPosed backend on
 * launcher 7719 refuses the Rust owner's SIMD-save prologue, while it already handles these
 * ordinary Dart prologues for the working geometry hooks above.
 */
.globl hc_layout_animation_ratio_entry
.p2align 4
hc_layout_animation_ratio_entry:
    dart_save
    adrp x9, hc_layout_animation_ratio_hits
    add x9, x9, :lo12:hc_layout_animation_ratio_hits
    ldr x10, [x9]
    add x10, x10, #1
    str x10, [x9]
    stp x30, xzr, [x15, #-16]!
    adrp x9, hc_layout_animation_ratio_original
    add x9, x9, :lo12:hc_layout_animation_ratio_original
    ldr x9, [x9]
    blr x9
    ldp x30, xzr, [x15], #16
    adrp x9, hc_layout_animation_ratio_original_bits
    add x9, x9, :lo12:hc_layout_animation_ratio_original_bits
    str d0, [x9]
    adrp x9, hc_layout_animation_ratio_enabled
    add x9, x9, :lo12:hc_layout_animation_ratio_enabled
    ldar w10, [x9]
    cbz w10, 1f
    adrp x9, hc_layout_animation_ratio_bits
    add x9, x9, :lo12:hc_layout_animation_ratio_bits
    ldr d0, [x9]
1:
    dart_restore
    ret

/*
 * The gear-derived duration factor (`Utilities.getDefaultGestureAnimMagicSpeed`). Measured on
 * device: the app open/close window springs scale 1:1 with this value (gear 0/1/2 => 1.0/0.86/0.75
 * and the close band moved 0.609s => 0.463s), while `getAnimDurationRatio` does not reach the
 * window transition at all. Same replacement semantics as the ratio stub above - both are
 * "1.0 = nominal duration" scales - but a fully independent enable gate, duration value, original
 * slot and hit counter: open/close and recents are separate product controls.
 */
.globl hc_layout_magic_entry
.p2align 4
hc_layout_magic_entry:
    dart_save
    adrp x9, hc_layout_magic_hits
    add x9, x9, :lo12:hc_layout_magic_hits
    ldr x10, [x9]
    add x10, x10, #1
    str x10, [x9]
    stp x30, xzr, [x15, #-16]!
    adrp x9, hc_layout_magic_original
    add x9, x9, :lo12:hc_layout_magic_original
    ldr x9, [x9]
    blr x9
    ldp x30, xzr, [x15], #16
    adrp x9, hc_layout_magic_original_bits
    add x9, x9, :lo12:hc_layout_magic_original_bits
    str d0, [x9]
    adrp x9, hc_layout_magic_enabled
    add x9, x9, :lo12:hc_layout_magic_enabled
    ldar w10, [x9]
    cbz w10, 1f
    adrp x9, hc_layout_magic_bits
    add x9, x9, :lo12:hc_layout_magic_bits
    ldr d0, [x9]
1:
    dart_restore
    ret

/*
 * Minimal pass-through stub, used to separate "my instrumentation is wrong" from "the inline hook
 * itself cannot be trusted here". It saves exactly the registers the Dart ABI requires a callee to
 * preserve, calls the original, and returns whatever it returned - nothing else, no counter, no
 * scratch store, no value inspection.
 */
.globl hc_layout_passthrough_entry
.p2align 4
hc_layout_passthrough_entry:
    dart_save
    stp x30, xzr, [x15, #-16]!
    /*
     * Record the three readings the calibration protocol asks for: how often the function ran, its
     * raw return value, and who called it (x30 on entry is the return address; subtracting the image
     * load base on the C side turns it into a symbol-table address). x9/x10 are Dart scratch and the
     * pair was already saved above, so the measurement itself changes nothing the launcher sees.
     */
    adrp x9, hc_layout_probe_caller
    add x9, x9, :lo12:hc_layout_probe_caller
    str x30, [x9]
    adrp x9, hc_layout_probe_hits
    add x9, x9, :lo12:hc_layout_probe_hits
    ldr w10, [x9]
    add w10, w10, #1
    str w10, [x9]
    adrp x9, hc_layout_passthrough_original
    add x9, x9, :lo12:hc_layout_passthrough_original
    ldr x9, [x9]
    blr x9
    adrp x9, hc_layout_probe_value
    add x9, x9, :lo12:hc_layout_probe_value
    str d0, [x9]
    ldp x30, xzr, [x15], #16
    dart_restore
    ret


/* Folder-layout original-code splices. Native helper never calls Dart, allocates,
 * or holds a heap pointer. Each original GC return PC stays in libapp.so.
 * All V registers and Dart scratch registers survive AAPCS; native SP remains16. */
.macro folder_layout_splice kind
.globl hc_folder_layout_\kind\()_entry
.p2align 4
hc_folder_layout_\kind\()_entry:
    dart_save
    adrp x9, hc_folder_layout_ready
    add x9, x9, :lo12:hc_folder_layout_ready
    ldar w9, [x9]
    cbz w9, 1f
    adrp x9, hc_folder_layout_requested
    add x9, x9, :lo12:hc_folder_layout_requested
    ldar x9, [x9]
.if (\kind == 4) || (\kind == 5) || (\kind == 7)
    tbz x9, #0, 1f
.else
    tbz x9, #1, 1f
.endif
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, sp
    mov x1, x27
    mov w2, #\kind
    lsl x3, x28, #32
    bl hc_folder_layout_body
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
    dart_restore

.if (\kind == 1) || (\kind == 6)
    // Helper replays the dynamically decoded original double load before adjusting it.
    mov x15, x29
    ldp x29, x30, [x15], #16
    ret
.elseif (\kind == 2) || (\kind == 3)
    // Original freshly allocated Double store, common to fast and GC slow paths.
    stur d0, [x0, #7]
    mov x15, x29
    ldp x29, x30, [x15], #16
    ret
.else
    b 2f
.endif
1:
    dart_restore
2:
    adrp x16, hc_folder_layout_original
    add x16, x16, :lo12:hc_folder_layout_original
    ldr x16, [x16, #(\kind * 8)]
    br x16
.endm
folder_layout_splice 0
folder_layout_splice 1
folder_layout_splice 2
folder_layout_splice 3
folder_layout_splice 4
folder_layout_splice 5
folder_layout_splice 6

folder_layout_splice 7

/* Final workspace row spacing: both fixed and variable paths, no Dart BL relocation. */
.macro grid_autofit_splice kind
.globl hc_grid_autofit_\kind\()_entry
.p2align 4
hc_grid_autofit_\kind\()_entry:
    dart_save
    adrp x9, hc_grid_autofit_ready
    add x9, x9, :lo12:hc_grid_autofit_ready
    ldar w9, [x9]
    cbz w9, 1f
    adrp x9, hc_grid_autofit_requested
    add x9, x9, :lo12:hc_grid_autofit_requested
    ldar w9, [x9]
    tbz x9, #0, 1f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, sp
    mov x1, x29
    mov w2, #\kind
    bl hc_grid_autofit_body
    str w0, [sp, #144]
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    ldr w16, [sp, #144]
    add sp, sp, #704
    dart_restore

    cbz w16, 2f
    adrp x16, hc_grid_autofit_resume
    add x16, x16, :lo12:hc_grid_autofit_resume
    ldr x16, [x16, #(\kind * 8)]
    br x16
1:
    dart_restore
2:
    adrp x16, hc_grid_autofit_original
    add x16, x16, :lo12:hc_grid_autofit_original
    ldr x16, [x16, #(\kind * 8)]
    br x16
.endm
grid_autofit_splice 0
grid_autofit_splice 1

// Folder preview coordinates consume the actually rendered SliverGrid, not unclamped metrics.
.macro folder_preview_splice kind
.globl hc_folder_preview_\kind\()_entry
.p2align 4
hc_folder_preview_\kind\()_entry:
    dart_save
    adrp x9, hc_folder_preview_enabled
    add x9, x9, :lo12:hc_folder_preview_enabled
    ldar w9, [x9]
    cbz w9, 1f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, sp
    mov x1, x29
    mov w2, #\kind
    lsl x3, x28, #32
    bl hc_folder_preview_body
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
    dart_restore

    b 2f
1:
    dart_restore
2:
    adrp x16, hc_folder_preview_original
    add x16, x16, :lo12:hc_folder_preview_original
    ldr x16, [x16, #(\kind * 8)]
    br x16
.endm
folder_preview_splice 0
folder_preview_splice 1
folder_preview_splice 2

/*
 * The DESTINATION preview inside FolderAnimController._calcFolderPreviewLoc is spliced twice:
 *
 *  kind 3 sits on the four words that end with the absolute index in x2, and only records it.
 *  kind 4 sits on the write-back of the finished Offset and rewrites its two doubles.
 *
 * Both windows replay the original four words verbatim and then jump one word past the window,
 * so the launcher's own arithmetic, GC call and write barrier are untouched. Neither window
 * contains a branch or a Dart call, which is what makes a plain 16-byte splice safe here.
 */
.macro folder_preview_register_entry name, kind
.globl \name
.p2align 4
\name:
    dart_save
    adrp x9, hc_folder_preview_enabled
    add x9, x9, :lo12:hc_folder_preview_enabled
    ldar w9, [x9]
    cbz w9, 1f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, sp
    mov x1, x29
    mov w2, #\kind
    lsl x3, x28, #32
    bl hc_folder_preview_body
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
    dart_restore
    b 2f
1:
    dart_restore
2:
    /*
     * The backend continuation replays the four displaced words and returns into the launcher, so
     * kind 3's x2 read and kind 4's Offset rewrite happen before that replay runs. Kind 4 stores the
     * compressed pointer unchanged; kind 3 has already stashed its index.
     */
    adrp x16, hc_folder_preview_original
    add x16, x16, :lo12:hc_folder_preview_original
    ldr x16, [x16, #(\kind * 8)]
    br x16
.endm
folder_preview_register_entry hc_folder_preview_3_entry, 3
folder_preview_register_entry hc_folder_preview_4_entry, 4
folder_preview_register_entry hc_folder_preview_5_entry, 5
folder_preview_register_entry hc_folder_preview_6_entry, 6

// Read-only destination observer: records _calcRealIconPos' incoming receiver/item and the
// cached grid insets it adds, without touching any of them. Never rewrites a field.
.macro folder_probe_splice kind
.globl hc_folder_probe_\kind\()_entry
.p2align 4
hc_folder_probe_\kind\()_entry:
    dart_save
    adrp x9, hc_folder_probe_enabled
    add x9, x9, :lo12:hc_folder_probe_enabled
    ldar w9, [x9]
    cbz w9, 1f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, sp
    mov x1, x29
    mov w2, #\kind
    lsl x3, x28, #32
    bl hc_folder_probe_body
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
    dart_restore

    b 2f
1:
    dart_restore
2:
    adrp x16, hc_folder_probe_original
    add x16, x16, :lo12:hc_folder_probe_original
    ldr x16, [x16, #(\kind * 8)]
    br x16
.endm
folder_probe_splice 0

/* Large-folder original size producer and workspace-size consumers. No relocated calls. */
.macro big_folder_size_splice name, kind
.globl \name
.p2align 4
\name:
    dart_save
    adrp x9, hc_big_folder_size_enabled
    add x9, x9, :lo12:hc_big_folder_size_enabled
    ldar w10, [x9]
    cbz w10, 2f
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
    mov x0, x29
    mov x1, x28
    mov x2, sp
    mov x3, #\kind
    mov x4, x26
    mov x5, x22
    bl hc_big_folder_size_body
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
    dart_restore
    adrp x16, hc_big_folder_size_resume
    add x16, x16, :lo12:hc_big_folder_size_resume
    ldr x16, [x16, #(\kind * 8)]
    br x16
2:
    // Partial registration must use untouched original arithmetic, including
    // the screen-index local; never run a half-installed scalar carry path.
    dart_restore
    adrp x16, hc_big_folder_size_original
    add x16, x16, :lo12:hc_big_folder_size_original
    ldr x16, [x16, #(\kind * 8)]
    br x16
.endm

big_folder_size_splice hc_big_folder_size_0_entry, 0
big_folder_size_splice hc_big_folder_size_1_entry, 1
big_folder_size_splice hc_big_folder_size_2_entry, 2
big_folder_size_splice hc_big_folder_size_3_entry, 3
big_folder_size_splice hc_big_folder_size_4_entry, 4

big_folder_size_splice hc_big_folder_size_5_entry, 5
big_folder_size_splice hc_big_folder_size_6_entry, 6

/* Default Dock background policy. Original Dart frame and result remain untouched.
 * Two fields come from the current image's verified getters, never fixed offsets.
 * Publish one coherent scalar (revision << 1 | editing); no Dart/C calls or GC roots. */
.globl hc_dock_edit_entry
.p2align 4
hc_dock_edit_entry:
    dart_save
    adrp x9, hc_dock_edit_controller_field
    ldr w9, [x9, :lo12:hc_dock_edit_controller_field]
    ldr w10, [x1, x9]
    add x10, x10, x28, lsl #32
    adrp x9, hc_dock_edit_value_field
    ldr w9, [x9, :lo12:hc_dock_edit_value_field]
    ldr w10, [x10, x9]
    add w11, w22, #0x20
    cmp w10, w11
    b.eq 1f
    add w11, w22, #0x30
    cmp w10, w11
    b.ne 4f
    mov x10, #0
    b 2f
1:
    mov x10, #1
2:
    adrp x9, dock_edit_packed
    add x9, x9, :lo12:dock_edit_packed
    ldar x11, [x9]
    cbz x11, 3f
    eor x16, x11, x10
    tbz x16, #0, 4f
3:
    add x11, x11, #2
    and x11, x11, #-2
    orr x11, x11, x10
    stlr x11, [x9]
    stp x0, x1, [x15, #-16]!
    stp x2, x8, [x15, #-16]!
    adrp x17, dock_motion_event
    ldr w0, [x17, :lo12:dock_motion_event]
    tbnz w0, #31, 5f
    adrp x1, dock_motion_one
    add x1, x1, :lo12:dock_motion_one
    mov x2, #8
    mov x8, #64
    svc #0
5:
    ldp x2, x8, [x15], #16
    ldp x0, x1, [x15], #16
4:
    dart_restore
    adrp x16, hc_dock_edit_original
    ldr x16, [x16, :lo12:hc_dock_edit_original]
    br x16

.macro workspace_native_save
    sub sp, sp, #704
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x18, [sp, #112]
    stp x15, x30, [sp, #128]
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    stp q8, q9, [sp, #288]
    stp q10, q11, [sp, #320]
    stp q12, q13, [sp, #352]
    stp q14, q15, [sp, #384]
    stp q16, q17, [sp, #416]
    stp q18, q19, [sp, #448]
    stp q20, q21, [sp, #480]
    stp q22, q23, [sp, #512]
    stp q24, q25, [sp, #544]
    stp q26, q27, [sp, #576]
    stp q28, q29, [sp, #608]
    stp q30, q31, [sp, #640]
.endm
.macro workspace_native_restore
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldp q8, q9, [sp, #288]
    ldp q10, q11, [sp, #320]
    ldp q12, q13, [sp, #352]
    ldp q14, q15, [sp, #384]
    ldp q16, q17, [sp, #416]
    ldp q18, q19, [sp, #448]
    ldp q20, q21, [sp, #480]
    ldp q22, q23, [sp, #512]
    ldp q24, q25, [sp, #544]
    ldp q26, q27, [sp, #576]
    ldp q28, q29, [sp, #608]
    ldp q30, q31, [sp, #640]
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x18, [sp, #112]
    ldp x15, x30, [sp, #128]
    add sp, sp, #704
.endm

/* Workspace original double-tap policy. Only the stock preference predicate
 * changes; all mode/overlay/edit/scroll guards continue in the original body. */
.globl hc_workspace_double_policy_entry
.p2align 4
hc_workspace_double_policy_entry:
    dart_save
    adrp x9, hc_workspace_swipe_enabled
    add x9, x9, :lo12:hc_workspace_swipe_enabled
    ldar w9, [x9]
    cbz w9, 9f
    workspace_native_save
    mov x0, x29
    mov x1, x28
    mov x2, x21
    bl hc_workspace_scope_authorize
    workspace_native_restore
9:
    adrp x9, hc_workspace_double_enabled
    add x9, x9, :lo12:hc_workspace_double_enabled
    ldar w9, [x9]
    adrp x10, hc_workspace_double_selected
    add x10, x10, :lo12:hc_workspace_double_selected
    stlr w9, [x10]
    cbz w9, 1f
    dart_restore
    add x0, x22, #0x20
    adrp x16, hc_workspace_double_policy_resume
    ldr x16, [x16, :lo12:hc_workspace_double_policy_resume]
    br x16
1:
    dart_restore
    dynamic_load x0, x29, hc_workspace_double_frame
    dynamic_load w1, x0, hc_workspace_double_field
    add x1, x1, x28, lsl #32
    // Replay the original Rx call without moving its GC return PC or adding
    // a Dart frame: LR equals the original BL+4, exactly as the stock call.
    adrp x30, hc_workspace_double_policy_resume
    ldr x30, [x30, :lo12:hc_workspace_double_policy_resume]
    adrp x16, hc_workspace_double_rx
    ldr x16, [x16, :lo12:hc_workspace_double_rx]
    br x16

/* The four displaced instructions contain no Dart call. Stock replays them
 * then reaches the original BLR/GC return PC. Custom skips that ONE closure
 * call and rejoins its original timer cancellation/reset, without Dart allocation. */
.globl hc_workspace_double_callback_entry
.p2align 4
hc_workspace_double_callback_entry:
    dart_save
    // Latch the policy decision of this original invocation. Disabling between
    // its preference guard and callback must NOT resurrect the stock lock action.
    adrp x9, hc_workspace_double_selected
    add x9, x9, :lo12:hc_workspace_double_selected
    ldar w9, [x9]
    cbz w9, 1f
    workspace_native_save
    bl hc_workspace_double_emit
    workspace_native_restore
    dart_restore
    adrp x16, hc_workspace_double_resume
    ldr x16, [x16, :lo12:hc_workspace_double_resume]
    br x16
1:
    dart_restore
    adrp x16, hc_workspace_double_callback_original
    ldr x16, [x16, :lo12:hc_workspace_double_callback_original]
    br x16

/* Filter the original status-bar interception RESULT, after its own pointer-map
 * bookkeeping and guards. No original Dart call/GC PC moves; all epilogues are
 * decoded and validated as part of the complete source bank. */
.macro workspace_status_result name, kind
.globl \name
.p2align 4
\name:
    dart_save
    workspace_native_save
    mov x0, x29
    mov x1, x28
    mov x2, x21
    mov x3, sp
    mov x4, #\kind
    mov x5, x22
    bl hc_workspace_status_result
    str x0, [sp, #0]
    workspace_native_restore
    dart_restore
    mov x15, x29
    ldp x29, x30, [x15], #16
    ret
.endm
workspace_status_result hc_workspace_status_true_entry, 0
workspace_status_result hc_workspace_status_register_entry, 1
workspace_status_result hc_workspace_status_field_entry, 2

/* Scalar-only banks: preserve SIMD scratch, GPRs and flags; no Dart call/GC PC relocation. */
.macro icon_render_bank name, reg, original, hits, enabled=hc_icon_render_enabled, scale=hc_icon_scale_bits, applied=, applied_factor=
.globl \name
.p2align 4
\name:
    dart_save
    adrp x9, \enabled
    add x9, x9, :lo12:\enabled
    ldar w9, [x9]
    cbz w9, 1f
    str q31, [x15, #-16]!
    adrp x9, \scale
    add x9, x9, :lo12:\scale
    ldar x10, [x9]
    fmov d31, x10
    fmul \reg, \reg, d31
    ldr q31, [x15], #16
.ifnb \applied
    mov x11, #0x3ff0000000000000
    cmp x10, x11
    b.eq 2f
    adrp x9, \applied
    ldr x11, [x9, :lo12:\applied]
    add x11, x11, #1
    str x11, [x9, :lo12:\applied]
.ifnb \applied_factor
    adrp x9, \applied_factor
    add x9, x9, :lo12:\applied_factor
    stlr x10, [x9]
.endif
2:
.endif
    adrp x9, \hits
    ldr x10, [x9, :lo12:\hits]
    add x10, x10, #1
    str x10, [x9, :lo12:\hits]
1:
    dart_restore
    adrp x16, \original
    ldr x16, [x16, :lo12:\original]
    br x16
.endm
icon_render_bank hc_icon_width_entry, d1, hc_icon_width_original, hc_icon_width_hits
icon_render_bank hc_icon_height_entry, d0, hc_icon_height_original, hc_icon_height_hits

icon_render_bank hc_desktop_icon_width_entry, d1, hc_desktop_icon_width_original, hc_desktop_icon_width_hits
icon_render_bank hc_desktop_icon_height_entry, d1, hc_desktop_icon_height_original, hc_desktop_icon_height_hits

icon_render_bank hc_dock_icon_width_entry, d0, hc_dock_icon_width_original, hc_dock_icon_width_hits
icon_render_bank hc_dock_icon_height_entry, d0, hc_dock_icon_height_original, hc_dock_icon_height_hits

icon_render_bank hc_progress_icon_width_entry, d1, hc_progress_icon_width_original, hc_progress_icon_width_hits, hc_progress_icon_enabled
icon_render_bank hc_progress_icon_height_entry, d1, hc_progress_icon_height_original, hc_progress_icon_height_hits, hc_progress_icon_enabled
icon_render_bank hc_progress_dock_width_entry, d0, hc_progress_dock_width_original, hc_progress_dock_width_hits, hc_progress_icon_enabled
icon_render_bank hc_progress_dock_height_entry, d0, hc_progress_dock_height_original, hc_progress_dock_height_hits, hc_progress_icon_enabled

/* Strategies have just overwritten these unboxed lengths. Scale background,
 * previews and gaps before the original padding calculator centers the result.
 * Caller guards select the independently validated small/large geometry tables.
 * No Dart call or allocation moves, and no movable pointer survives the replay. */
.globl hc_folder_icon_scale_entry
.p2align 4
hc_folder_icon_scale_entry:
    dart_save
    adrp x9, hc_folder_icon_scale_enabled
    add x9, x9, :lo12:hc_folder_icon_scale_enabled
    ldar w9, [x9]
    cbz w9, 1f
    adrp x9, hc_folder_icon_scale_caller
    ldr x9, [x9, :lo12:hc_folder_icon_scale_caller]
    cmp x30, x9
    b.ne 2f
    adrp x9, hc_folder_icon_scale_fields
    add x9, x9, :lo12:hc_folder_icon_scale_fields
    mov x11, #4
    adrp x16, hc_small_folder_scale_bits
    add x16, x16, :lo12:hc_small_folder_scale_bits
    b 3f
2:
    adrp x9, hc_folder_icon_scale_big_caller
    ldr x9, [x9, :lo12:hc_folder_icon_scale_big_caller]
    cmp x30, x9
    b.ne 1f
    adrp x9, hc_folder_icon_scale_big_fields
    add x9, x9, :lo12:hc_folder_icon_scale_big_fields
    mov x11, #13
    adrp x16, hc_big_folder_scale_bits
    add x16, x16, :lo12:hc_big_folder_scale_bits
3:
    stp q30, q31, [x15, #-32]!
    ldar x10, [x16]
    fmov d31, x10
4:
    ldr x10, [x9], #8
    ldr d30, [x1, x10]
    fmul d30, d30, d31
    str d30, [x1, x10]
    subs x11, x11, #1
    b.ne 4b
    ldp q30, q31, [x15], #32
1:
    dart_restore
    adrp x16, hc_folder_icon_scale_original
    ldr x16, [x16, :lo12:hc_folder_icon_scale_original]
    br x16

/* Closing previews consume the same ordinary-icon render scale as AppIcon. */
icon_render_bank hc_folder_anim_scale0_entry, d0, hc_folder_anim_scale0_original, hc_folder_anim_scale_hits, hc_folder_anim_scale_enabled
icon_render_bank hc_folder_anim_scale1_entry, d0, hc_folder_anim_scale1_original, hc_folder_anim_scale_hits, hc_folder_anim_scale_enabled
icon_render_bank hc_folder_anim_scale2_entry, d0, hc_folder_anim_scale2_original, hc_folder_anim_scale_hits, hc_folder_anim_scale_enabled
icon_render_bank hc_folder_anim_scale3_entry, d0, hc_folder_anim_scale3_original, hc_folder_anim_scale_hits, hc_folder_anim_scale_enabled
icon_render_bank hc_folder_anim_scale4_entry, d0, hc_folder_anim_scale4_original, hc_folder_anim_scale_hits, hc_folder_anim_scale_enabled
/* App-to-folder stable landing dimensions use the same ordinary-icon factor. */
icon_render_bank hc_folder_return_width_entry, d0, hc_folder_return_width_original, hc_folder_return_scale_hits, hc_folder_return_scale_enabled
icon_render_bank hc_folder_return_height_entry, d0, hc_folder_return_height_original, hc_folder_return_scale_hits, hc_folder_return_scale_enabled

/* Verified two-capture Observer callers, after the original AllocateClosure
 * returned. This covers both VM allocation paths without moving a GC PC or
 * patching the allocator. Replay the original publication first. Only the real
 * closure entrypoint AND its validated caller may wrap this fresh Observer. */
.globl hc_widget_scale_entry
.p2align 4
hc_widget_scale_entry:
    dynamic_load x0, x29, hc_widget_parent_frame
    dynamic_store w1, x0, hc_widget_child_field
    dart_save
    adrp x9, hc_widget_scale_enabled
    add x9, x9, :lo12:hc_widget_scale_enabled
    ldar w9, [x9]
    cbz w9, 2f
    dynamic_load x9, x1, hc_widget_closure_entry_field
    adrp x10, hc_widget_dispatch_entry
    ldr x10, [x10, :lo12:hc_widget_dispatch_entry]
    cmp x9, x10
    b.ne 2f
    workspace_native_save
    mov x0, x1
    mov x1, x29
    mov x2, x30
    mov x3, x26
    mov x4, x27
    mov x5, x22
    bl hc_widget_scale_build
    cbz x1, 1f
    str x0, [sp, #0]
1:
    workspace_native_restore
2:
    dart_restore
    mov x15, x29
    ldp x29, x30, [x15], #16
    ret

/* Phone map/default branches converge here with raw row=-1; other-device bypass
 * carries row=0. Only the scalar Smi argument changes. Original copyWith BL,
 * GC return PC, row argument and receiver load remain in the original frame. */
.globl hc_dart_columns_entry
.p2align 4
hc_dart_columns_entry:
    dart_save
    adrp x9, hc_dart_columns_ready
    add x9, x9, :lo12:hc_dart_columns_ready
    ldar w9, [x9]
    cbz w9, 1f
    adrp x9, hc_native_columns_request
    add x9, x9, :lo12:hc_native_columns_request
    ldar w9, [x9]
    tbz w9, #0, 1f
    cmn x1, #1
    b.ne 1f
    lsr w9, w9, #8
    sub w10, w9, #1
    cmp w10, #7
    b.hi 1f
    adrp x10, hc_grid_transient_seen
    add x10, x10, :lo12:hc_grid_transient_seen
    mov w11, #1
    stlr w11, [x10]
    lsl w2, w9, #1
    lsl x3, x1, #1
    // Forward an explicit row request to the same original copyWith. Otherwise
    // changing columns selects the launcher's auto-row calculation (e.g. 5x9).
    adrp x9, hc_grid_autofit_requested
    add x9, x9, :lo12:hc_grid_autofit_requested
    ldar w9, [x9]
    tbz w9, #0, 2f
    lsr w9, w9, #8
    sub w10, w9, #2
    cmp w10, #14
    b.hi 2f
    lsl w3, w9, #1
2:
    dart_restore
    stp x3, x2, [x15]
    dynamic_load x1, x29, hc_dart_columns_frame
    adrp x16, hc_dart_columns_continue
    ldr x16, [x16, :lo12:hc_dart_columns_continue]
    br x16
1:
    dart_restore
    adrp x16, hc_dart_columns_original
    ldr x16, [x16, :lo12:hc_dart_columns_original]
    br x16

/* A displayed transient grid must never leak back into native defaults, even
 * between disabling the preference and the next model rebuild. Keep ownership
 * sticky for this Home process; a clean Home start restores native persistence. */
.macro transient_grid original
    dart_save
    adrp x9, hc_native_columns_request
    add x9, x9, :lo12:hc_native_columns_request
    ldar w9, [x9]
    tbz w9, #0, 1f
    adrp x10, hc_grid_transient_seen
    add x10, x10, :lo12:hc_grid_transient_seen
    mov w11, #1
    stlr w11, [x10]
    b 2f
1:
    adrp x9, hc_grid_transient_seen
    add x9, x9, :lo12:hc_grid_transient_seen
    ldar w9, [x9]
    cbnz w9, 2f
    dart_restore
    adrp x16, \original
    ldr x16, [x16, :lo12:\original]
    br x16
2:
    dart_restore
.endm

/* The validated original synchronous method returns null on every path. */
.globl hc_grid_persist_entry
.p2align 4
hc_grid_persist_entry:
    transient_grid hc_grid_persist_original
    mov x0, x22
    ret

/* Preserve the live layout-type scalar update; omit only logging/closure
 * allocation and the default-preference side effect. No object pointer retained. */
.globl hc_layout_save_entry
.p2align 4
hc_layout_save_entry:
    transient_grid hc_layout_save_original
    dart_save
    adrp x9, hc_layout_save_field
    ldr x9, [x9, :lo12:hc_layout_save_field]
    str x2, [x1, x9]
    dart_restore
    mov x0, x22
    ret

/* Original four-word builder initialization. Only the rendered frame local is
 * changed for Dock; the Folme controller and its animation remain untouched. */
.macro dock_title_model index
.globl hc_dock_title_model_\index\()_entry
.p2align 4
hc_dock_title_model_\index\()_entry:
    mrs x16, nzcv
    str x16, [sp, #-16]!
    adrp x16, hc_dock_title_model_hits
    add x16, x16, :lo12:hc_dock_title_model_hits
    ldr x17, [x16, #(\index * 8)]
    add x17, x17, #1
    str x17, [x16, #(\index * 8)]
    adrp x16, hc_dock_title_model_fields
    add x16, x16, :lo12:hc_dock_title_model_fields
    add x16, x16, #(\index * 32)
    ldrsw x17, [x16, #4]
    str x1, [x29, x17]
    ldrsw x17, [x16, #12]
    str w0, [x1, x17]
    ldrsw x17, [x16, #8]
    ldr x0, [x29, x17]
    ldrsw x17, [x16, #16]
    str w0, [x1, x17]
    adrp x16, hc_dock_title_model_enabled
    add x16, x16, :lo12:hc_dock_title_model_enabled
    ldar w16, [x16]
    cbz w16, 3f
    adrp x16, hc_dock_title_model_fields
    add x16, x16, :lo12:hc_dock_title_model_fields
    add x16, x16, #(\index * 32)
    ldrsw x17, [x16]
    ldr x17, [x29, x17]
    ldr w16, [x16, #24]
    ldr w17, [x17, x16]
    add x17, x17, x28, lsl #32
    adrp x16, hc_dock_title_model_fields
    add x16, x16, :lo12:hc_dock_title_model_fields
    add x16, x16, #(\index * 32)
    ldr w16, [x16, #28]
    ldr x17, [x17, x16]
    cmn x17, #0x65
    b.eq 1f
    cmn x17, #0x66
    b.eq 1f
    cmn x17, #0x67
    b.eq 1f
    cmn x17, #0x69
    b.eq 1f
    cmn x17, #0x6a
    b.ne 3f
1:
    adrp x16, hc_dock_title_model_hotseat_hits
    add x16, x16, :lo12:hc_dock_title_model_hotseat_hits
    ldr x17, [x16, #(\index * 8)]
    add x17, x17, #1
    str x17, [x16, #(\index * 8)]
    adrp x16, hc_dock_title_model_fields
    add x16, x16, :lo12:hc_dock_title_model_fields
    add x16, x16, #(\index * 32)
    ldrsw x17, [x16, #4]
    str x0, [x29, x17]
3:
    ldr x16, [sp], #16
    msr nzcv, x16
    adrp x16, hc_dock_title_model_continue
    add x16, x16, :lo12:hc_dock_title_model_continue
    ldr x16, [x16, #(\index * 8)]
    br x16
.endm
dock_title_model 0
dock_title_model 1
