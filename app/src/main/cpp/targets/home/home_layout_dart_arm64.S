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
 * and NZCV; the helper writes only verified local frame slots, never heap fields.
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
    ldrb w10, [x9]
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
    mov x2, x26
    mov x3, x22
    bl hc_layout_indicator_pair
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

/* Both finished animation branches receive the same fresh outer Padding. */
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
