/* SPDX-License-Identifier: AGPL-3.0-or-later */
/* Host-only callsites: one synthetic Weather load bias gives both exact
 * production return-PC offsets. This is not an AArch64 ABI test. */
.text
.global joint_material_call
.type joint_material_call,@function
joint_material_call:
    sub $8, %rsp
    call *%rcx
.global joint_material_return
joint_material_return:
    add $8, %rsp
    ret
.size joint_material_call, .-joint_material_call
.org joint_material_return + (0x5fc76c - 0x4402e4) - 6, 0x90
.global joint_level_call
.type joint_level_call,@function
joint_level_call:
    sub $8, %rsp
    call *%rdx
.global joint_level_return
joint_level_return:
    add $8, %rsp
    ret
.size joint_level_call, .-joint_level_call
.section .note.GNU-stack,"",@progbits
