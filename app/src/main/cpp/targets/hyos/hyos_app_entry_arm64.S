/* SPDX-License-Identifier: AGPL-3.0-or-later */
.text
.p2align 4
.hidden hc_hyos_prepare_app_entry
.globl hc_hyos_app_entry
.hidden hc_hyos_app_entry
.type hc_hyos_app_entry, %function
hc_hyos_app_entry:
    .cfi_startproc
    bti c
    /* Rust: x0 = Runtime reference, x8 = opaque 24-byte sret destination.
     * Preserve volatile arguments, FP state and LR; tail-call the real entry. */
    sub sp, sp, #320
    .cfi_def_cfa_offset 320
    stp x0, x1, [sp, #0]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x9, [sp, #64]
    stp x10, x11, [sp, #80]
    stp x12, x13, [sp, #96]
    stp x14, x15, [sp, #112]
    stp x16, x17, [sp, #128]
    stp x18, x30, [sp, #144]
    .cfi_offset 30, -168
    stp q0, q1, [sp, #160]
    stp q2, q3, [sp, #192]
    stp q4, q5, [sp, #224]
    stp q6, q7, [sp, #256]
    mrs x16, nzcv
    str x16, [sp, #288]
    mrs x16, fpcr
    str x16, [sp, #296]
    mrs x16, fpsr
    str x16, [sp, #304]
    mov x0, x30
    bl hc_hyos_prepare_app_entry
    str x0, [sp, #312]
    ldp q0, q1, [sp, #160]
    ldp q2, q3, [sp, #192]
    ldp q4, q5, [sp, #224]
    ldp q6, q7, [sp, #256]
    ldr x16, [sp, #288]
    msr nzcv, x16
    ldr x16, [sp, #296]
    msr fpcr, x16
    ldr x16, [sp, #304]
    msr fpsr, x16
    ldp x0, x1, [sp, #0]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x9, [sp, #64]
    ldp x10, x11, [sp, #80]
    ldp x12, x13, [sp, #96]
    ldp x14, x15, [sp, #112]
    ldr x17, [sp, #136]
    ldp x18, x30, [sp, #144]
    ldr x16, [sp, #312]
    add sp, sp, #320
    .cfi_def_cfa_offset 0
    .cfi_restore 30
    br x16
    .cfi_endproc
.size hc_hyos_app_entry, .-hc_hyos_app_entry
.section .note.GNU-stack,"",%progbits
