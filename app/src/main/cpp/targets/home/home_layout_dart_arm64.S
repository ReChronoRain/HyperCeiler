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
    cmp x1, #0x7f2
    b.ne 1f
    ldur x9, [x29, #-0x18]
    mov x17, #0x11b
    ldr w10, [x9, x17]
    cmp w10, #24
    b.ne 1f
    ldur x10, [x9, #0x1f]
    cmp x10, #1
    b.ne 1f
    ldur x10, [x9, #0x27]
    cmp x10, #1
    b.ne 1f
    dart_restore
    ldur x0, [x29, #-0x18]
    ldur x1, [x29, #-0x10]
    add x2, x27, #0x36, lsl #12
    ldr x2, [x2, #0xff8] // Original widget_type pool entry.
    mov x3, #0x1802
    movk x3, #0x3086, lsl #16 // Smi(0x18430c01), not protocol type 5.
    adrp x16, hc_gadget_put_int
    ldr x16, [x16, :lo12:hc_gadget_put_int]
    br x16 // Execute the original BL, not blr from module code.
1:
    dart_restore
    cmp x1, #0x7ed
    b.ne 2f
    ldur x0, [x29, #-0x18]
    ldur x1, [x29, #-0x10]
    adrp x16, hc_gadget_select_continue
    ldr x16, [x16, :lo12:hc_gadget_select_continue]
    br x16
2:
    adrp x16, hc_gadget_common
    ldr x16, [x16, :lo12:hc_gadget_common]
    br x16

.globl hc_gadget_return_entry
.p2align 4
hc_gadget_return_entry:
    // putInt may GC: reload the original rooted model, never cache a heap pointer.
    dart_save
    ldur x9, [x29, #-0x18]
    ldur x10, [x9, #-1]
    ubfx x10, x10, #12, #20
    cmp x10, #0x7f2
    b.ne 3f
    dart_restore
    adrp x16, hc_gadget_common
    ldr x16, [x16, :lo12:hc_gadget_common]
    br x16
3:
    dart_restore
    // Unchanged MaMl serialization, including its original Gadget id load.
    ldur x0, [x29, #-0x18]
    mov x17, #0x143
    ldr w3, [x0, x17]
    add x3, x3, x28, lsl #32
    adrp x16, hc_gadget_return_continue
    ldr x16, [x16, :lo12:hc_gadget_return_continue]
    br x16

/* Only the bridged Gadget12 bypasses unsupported 1x1; all other size results remain original.
 * Original span BL has already returned: reload model through rooted handler +0x2b. */
.globl hc_gadget_span_entry
.p2align 4
hc_gadget_span_entry:
    dart_save
    adrp x9, hc_gadget_bridge_enabled
    add x9, x9, :lo12:hc_gadget_bridge_enabled
    ldar w9, [x9]
    cbz w9, 1f
    ldur x9, [x29, #-0x18]
    ldur w9, [x9, #0x2b]
    add x9, x9, x28, lsl #32
    ldur x10, [x9, #-1]
    ubfx x10, x10, #12, #20
    cmp x10, #0x7f2
    b.ne 1f
    mov x17, #0x11b
    ldr w10, [x9, x17]
    cmp w10, #24
    b.ne 1f
    ldur x10, [x9, #0x1f]
    cmp x10, #1
    b.ne 1f
    ldur x10, [x9, #0x27]
    cmp x10, #1
    b.ne 1f
    dart_restore
    b 2f
1:
    dart_restore
    tbnz w0, #4, 3f
2:
    ldur x0, [x29, #-8]
    ldur x2, [x29, #-0x18]
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
    stur x1, [x29, #-0x28]
    ldr x0, [x26, #0x78]
    ldr x0, [x0, #0x2710]
    cmp w0, w22
    adrp x16, hc_title_color_continue
    add x16, x16, :lo12:hc_title_color_continue
    ldr x16, [x16]
    br x16

/* Original caption is replaced only in the drawing local. Root the private UTF16 string
 * at the original stack-map slot before resuming the untouched allocation BL. */
.globl hc_title_custom_entry
.p2align 4
hc_title_custom_entry:
    ldur x2, [x29, #-0x18]
    ldur w3, [x2, #0x47]
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
    mov x17, #0x153
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
    stur x3, [x29, #-0x38]
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
    ldur x0, [x29, #-8]
    stur d0, [x29, #-0x40]
    ldur w1, [x0, #0x13]
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
    ldur d0, [x0, #0x43]
    dart_save
    // Replay the original custom-config branch, not a global "drawer currently visible" flag.
    ldur x16, [x29, #-8]
    ldur w17, [x16, #0x17]
    add x17, x17, x28, lsl #32
    ldur w17, [x17, #0xb]
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
    ldur x0, [x29, #-8]
    stur d0, [x29, #-0x58]
    ldur w1, [x0, #0x17]
    adrp x16, hc_desktop_title_continue
    add x16, x16, :lo12:hc_desktop_title_continue
    ldr x16, [x16]
    br x16

/* Resize the matching text line box too. Otherwise a larger font is clipped to the old
 * twelve-sp box. Replay the no-call load window, preserving every register and NZCV. */
.globl hc_desktop_title_height_entry
.p2align 4
hc_desktop_title_height_entry:
    ldur w1, [x0, #0x6f]
    add x1, x1, x28, lsl #32
    ldur d0, [x1, #0x27]
    dart_save
    ldur x16, [x29, #-8]
    ldur w17, [x16, #0x17]
    add x17, x17, x28, lsl #32
    ldur w17, [x17, #0xb]
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
    stur d0, [x29, #-0x60]
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
    ldur x0, [x29, #-0x10]
    stur d0, [x29, #-0x48]
    ldur w1, [x0, #0x5f]
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
    ldr x0, [x27, #0xf8]
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
    fadd d0, d0, d1
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
    ldr x0, [x27, #0x6250]
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
