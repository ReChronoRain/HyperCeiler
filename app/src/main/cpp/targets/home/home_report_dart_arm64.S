/* SPDX-License-Identifier: AGPL-3.0-or-later */
/* The guarded 7719 predicate uses Dart's x15 stack and returns a canonical bool in x0. */
.text
.p2align 4
.hidden hc_home_report_original
.hidden hc_home_report_enabled
.globl hc_home_report_entry
.type hc_home_report_entry, %function
hc_home_report_entry:
    /* x16 is Dart's temporary register. Do not introduce a non-Dart return PC/frame. */
    adrp x16, hc_home_report_enabled
    add x16, x16, :lo12:hc_home_report_enabled
    ldar w16, [x16]
    cbz w16, 1f
2:
    /* Canonical Dart false, not AAPCS integer zero or an allocated bool object. */
    add x0, x22, #0x30
    ret
1:
    adrp x16, hc_home_report_original
    add x16, x16, :lo12:hc_home_report_original
    ldar x16, [x16]
    /* Defensive fallback only: install rolls back a hook that supplies no original. */
    cbz x16, 2b
    br x16
.size hc_home_report_entry, .-hc_home_report_entry
.section .note.GNU-stack,"",%progbits
