/* SPDX-License-Identifier: AGPL-3.0-or-later */
.text
.global dock_test_trampoline
.type dock_test_trampoline, %function
dock_test_trampoline:
    ret
.global dock_test_invoke
.type dock_test_invoke, %function
// AAPCS test wrapper: x0=entry, x1=tagged fixture, x2=register output.
// Its private 512-byte scratch allocation emulates the Dart stack.
dock_test_invoke:
    stp x29, x30, [sp, #-16]!
    stp x19, x20, [sp, #-16]!
    stp x22, x28, [sp, #-16]!
    sub sp, sp, #512
    mov x19, x2
    mov x20, x0
    mov x2, x1
    add x15, sp, #256
    str x15, [x19, #136]
    adrp x16, dock_test_null_object
    ldr x22, [x16, :lo12:dock_test_null_object]
    adrp x16, dock_test_heap_base
    ldr x28, [x16, :lo12:dock_test_heap_base]
    mov x0, #100
    mov x1, #101
    mov x3, #103
    mov x4, #104
    mov x5, #105
    mov x6, #106
    mov x7, #107
    mov x8, #108
    mov x9, #109
    mov x10, #110
    mov x11, #111
    mov x12, #112
    mov x13, #113
    mov x14, #114
    mov x16, #0xa0000000
    msr nzcv, x16
    blr x20
    stp x0, x1, [x19, #0]
    stp x2, x3, [x19, #16]
    stp x4, x5, [x19, #32]
    stp x6, x7, [x19, #48]
    stp x8, x9, [x19, #64]
    stp x10, x11, [x19, #80]
    stp x12, x13, [x19, #96]
    stp x14, x15, [x19, #112]
    mrs x16, nzcv
    str x16, [x19, #128]
    add sp, sp, #512
    ldp x22, x28, [sp], #16
    ldp x19, x20, [sp], #16
    ldp x29, x30, [sp], #16
    ret
.size dock_test_invoke, .-dock_test_invoke

.global dock_test_invoke_unlock
.type dock_test_invoke_unlock, %function
// AAPCS test wrapper: x0=entry, x1=tagged _UnlockWidgetState, d0=projected scale,
// x2=register output. d0 and every non-scratch Dart register must survive the hook.
dock_test_invoke_unlock:
    stp x29, x30, [sp, #-16]!
    stp x19, x20, [sp, #-16]!
    stp x22, x28, [sp, #-16]!
    sub sp, sp, #512
    mov x19, x2
    mov x20, x0
    add x15, sp, #256
    str x15, [x19, #136]
    adrp x16, dock_test_null_object
    ldr x22, [x16, :lo12:dock_test_null_object]
    adrp x16, dock_test_heap_base
    ldr x28, [x16, :lo12:dock_test_heap_base]
    mov x0, #100
    mov x2, #102
    mov x3, #103
    mov x4, #104
    mov x5, #105
    mov x6, #106
    mov x7, #107
    mov x8, #108
    mov x9, #109
    mov x10, #110
    mov x11, #111
    mov x12, #112
    mov x13, #113
    mov x14, #114
    mov x16, #0xa0000000
    msr nzcv, x16
    blr x20
    stp x0, x1, [x19, #0]
    stp x2, x3, [x19, #16]
    stp x4, x5, [x19, #32]
    stp x6, x7, [x19, #48]
    stp x8, x9, [x19, #64]
    stp x10, x11, [x19, #80]
    stp x12, x13, [x19, #96]
    stp x14, x15, [x19, #112]
    mrs x16, nzcv
    str x16, [x19, #128]
    fmov x16, d0
    str x16, [x19, #144]
    add sp, sp, #512
    ldp x22, x28, [sp], #16
    ldp x19, x20, [sp], #16
    ldp x29, x30, [sp], #16
    ret
.size dock_test_invoke_unlock, .-dock_test_invoke_unlock
.section .note.GNU-stack,"",%progbits
