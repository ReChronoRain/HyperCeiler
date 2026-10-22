/* SPDX-License-Identifier: AGPL-3.0-or-later */
/* Hyos-only authorization gateways. No Dart/native call or new frame.
 * x16 is the same scratch register consumed by the existing continuations.
 * NZCV, LR, x9-x11, x15 and every argument/result register are untouched. */
.text
.p2align 4
.hidden hc_layout_startup_epoch_gate
.macro startup_gateway index, target, original, offset=0
/* These destinations are part of this DSO, not interposable C entry points.
 * A PLT veneer may clobber x17 before the Dart splice saves its registers. */
.hidden \target
.globl hc_home_startup_\index
.hidden hc_home_startup_\index
.type hc_home_startup_\index, %function
hc_home_startup_\index:
    adrp x16, hc_layout_startup_epoch_gate
    add x16, x16, :lo12:hc_layout_startup_epoch_gate
    ldar x16, [x16]
    tbz x16, #0, 1f
    b \target
1:
    adrp x16, \original
    add x16, x16, :lo12:\original
    ldr x16, [x16, #\offset]
    br x16
.size hc_home_startup_\index, .-hc_home_startup_\index
.endm
startup_gateway 0, hc_layout_dart_HotseatMargin_entry, hc_layout_dart_HotseatMargin_original
startup_gateway 1, hc_layout_dart_FolderRowSpacing_entry, hc_layout_dart_FolderRowSpacing_original
startup_gateway 2, hc_layout_workspace_entry, hc_layout_dart_WorkspaceTop_original
startup_gateway 3, hc_layout_workspace_occupied_entry, hc_layout_dart_WorkspaceBottom_original
startup_gateway 4, hc_layout_hotseat_horizontal_entry, hc_layout_dart_WorkspaceSide_original
startup_gateway 5, hc_layout_capsule_entry, hc_layout_dart_IndicatorMargin_original
startup_gateway 6, hc_layout_indicator_policy_entry, hc_layout_dart_SearchBarMargin_original
startup_gateway 7, hc_layout_indicator_slide_only_entry, hc_layout_dart_SearchBarWidth_original
startup_gateway 8, hc_layout_indicator_edit_result_entry, hc_layout_dart_IndicatorDot_original
startup_gateway 9, hc_layout_folder_0_entry, hc_layout_folder_original, 0
startup_gateway 10, hc_layout_folder_1_entry, hc_layout_folder_original, 8
startup_gateway 11, hc_layout_folder_2_entry, hc_layout_folder_original, 16
startup_gateway 12, hc_layout_folder_3_entry, hc_layout_folder_original, 24
startup_gateway 13, hc_layout_folder_4_entry, hc_layout_folder_original, 32
startup_gateway 14, hc_layout_magic_entry, hc_layout_magic_original
.section .note.GNU-stack,"",%progbits
