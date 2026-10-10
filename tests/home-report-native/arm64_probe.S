/* SPDX-License-Identifier: AGPL-3.0-or-later */
.text
.p2align 4
.globl call_dart_report_fixture
.type call_dart_report_fixture, %function
call_dart_report_fixture:
    stp x19, x20, [sp, #-96]!
    stp x21, x22, [sp, #16]
    stp x23, x24, [sp, #32]
    stp x25, x26, [sp, #48]
    stp x27, x28, [sp, #64]
    stp x29, x30, [sp, #80]
    mov x19, x0
    ldr x16, [x19, #0]
    ldr x1, [x19, #8]
    ldr x2, [x19, #16]
    ldr x22, [x19, #24]
    ldr x28, [x19, #32]
    ldr x27, [x19, #40]
    ldr x21, [x19, #48]
    ldr x15, [x19, #56]
    mov x29, x15
    mov x9, sp
    str x9, [x19, #128]
    adr x9, 1f
    str x9, [x19, #136]
    mov x9, #0xa0000000
    msr nzcv, x9
    mov x9, #0x9999
    mov x10, #0xaaaa
    mov x11, #0xbbbb
    blr x16
1:
    str x0, [x19, #64]
    str x9, [x19, #72]
    str x10, [x19, #80]
    str x11, [x19, #88]
    mrs x9, nzcv
    str x9, [x19, #96]
    str x15, [x19, #104]
    mov x9, sp
    str x9, [x19, #112]
    str x30, [x19, #120]
    str x29, [x19, #144]
    ldp x21, x22, [sp, #16]
    ldp x23, x24, [sp, #32]
    ldp x25, x26, [sp, #48]
    ldp x27, x28, [sp, #64]
    ldp x29, x30, [sp, #80]
    ldp x19, x20, [sp], #96
    ret
.size call_dart_report_fixture, .-call_dart_report_fixture

/* The actual predicate's dispatch-table equality call is supplied by the fixture. */
.globl report_equality_fixture
.type report_equality_fixture, %function
report_equality_fixture:
    adrp x16, report_fixture_calls
    add x16, x16, :lo12:report_fixture_calls
    ldr w17, [x16]
    add w17, w17, #1
    str w17, [x16]
    adrp x16, report_fixture_equal
    ldr w16, [x16, :lo12:report_fixture_equal]
    cbz w16, 1f
    add x0, x22, #0x20
    ret
1:
    add x0, x22, #0x30
    ret
.size report_equality_fixture, .-report_equality_fixture
.section .note.GNU-stack,"",%progbits
