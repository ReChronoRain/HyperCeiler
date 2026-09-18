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
