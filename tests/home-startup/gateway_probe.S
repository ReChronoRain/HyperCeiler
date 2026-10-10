/* SPDX-License-Identifier: AGPL-3.0-or-later */
/* Controlled destinations for the REAL production authorization gateways. */
.text
.p2align 4
.macro destination name, marker
.globl \name
.type \name,%function
\name:
    stp x9,x10,[sp,#-16]!
    adrp x9,gateway_marker
    add x9,x9,:lo12:gateway_marker
    mov x10,#\marker
    str x10,[x9]
    ldp x9,x10,[sp],#16
    b capture_gateway_registers
.endm
destination hc_layout_dart_HotseatMargin_entry, 0
destination hc_layout_dart_FolderRowSpacing_entry, 1
destination hc_layout_workspace_entry, 2
destination hc_layout_workspace_occupied_entry, 3
destination hc_layout_hotseat_horizontal_entry, 4
destination hc_layout_capsule_entry, 5
destination hc_layout_indicator_policy_entry, 6
destination hc_layout_indicator_slide_only_entry, 7
destination hc_layout_indicator_edit_result_entry, 8
destination hc_layout_folder_0_entry, 9
destination hc_layout_folder_1_entry, 10
destination hc_layout_folder_2_entry, 11
destination hc_layout_folder_3_entry, 12
destination hc_layout_folder_4_entry, 13
destination hc_layout_magic_entry, 14
.irp n,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14
destination original_\n, (100+\n)
.endr

capture_gateway_registers:
    adrp x16,gateway_registers
    add x16,x16,:lo12:gateway_registers
    stp x0,x1,[x16,#0]
    stp x2,x3,[x16,#16]
    stp x4,x5,[x16,#32]
    stp x6,x7,[x16,#48]
    stp x8,x9,[x16,#64]
    stp x10,x11,[x16,#80]
    stp x12,x13,[x16,#96]
    stp x14,x15,[x16,#112]
    stp x17,x18,[x16,#136]
    stp x19,x20,[x16,#152]
    stp x21,x22,[x16,#168]
    stp x23,x24,[x16,#184]
    stp x25,x26,[x16,#200]
    stp x27,x28,[x16,#216]
    stp x29,x30,[x16,#232]
    mrs x9,nzcv
    str x9,[x16,#248]
    mov x9,sp
    str x9,[x16,#256]
.irp n,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
    str q\n,[x16,#(272+16*\n)]
.endr
    // Re-establish the two registers used by the observation code before ret.
    ldr x9,[x16,#72]
    ret

.globl gateway_probe
.type gateway_probe,%function
gateway_probe:
    sub sp,sp,#192
    stp x19,x20,[sp,#0]
    stp x21,x22,[sp,#16]
    stp x23,x24,[sp,#32]
    stp x25,x26,[sp,#48]
    stp x27,x28,[sp,#64]
    stp x29,x30,[sp,#80]
    stp d8,d9,[sp,#96]
    stp d10,d11,[sp,#112]
    stp d12,d13,[sp,#128]
    stp d14,d15,[sp,#144]
    stp x18,xzr,[sp,#160]
    stp x0,x1,[sp,#176]
    adrp x16,gateway_expected_sp
    add x16,x16,:lo12:gateway_expected_sp
    mov x17,sp
    str x17,[x16]
.irp n,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,17,18,19,20,21,22,23,24,25,26,27,28,29
    mov x\n,#(0x100+\n)
.endr
.irp n,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
    movi v\n\().16b,#(32+\n)
.endr
    ldr x16,[sp,#184]
    msr nzcv,x16
    ldr x16,[sp,#176]
    blr x16
.globl gateway_expected_lr
gateway_expected_lr:
    ldp x19,x20,[sp,#0]
    ldp x21,x22,[sp,#16]
    ldp x23,x24,[sp,#32]
    ldp x25,x26,[sp,#48]
    ldp x27,x28,[sp,#64]
    ldp x29,x30,[sp,#80]
    ldp d8,d9,[sp,#96]
    ldp d10,d11,[sp,#112]
    ldp d12,d13,[sp,#128]
    ldp d14,d15,[sp,#144]
    ldp x18,xzr,[sp,#160]
    add sp,sp,#192
    ret
.section .note.GNU-stack,"",%progbits
