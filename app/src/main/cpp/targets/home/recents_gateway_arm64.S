/* SPDX-License-Identifier: AGPL-3.0-or-later */
/* Exact native Rust before-publication gateway; copied as a complete template.
 * Requires runtime HWCAP_ATOMICS admission (CASAL/LSE), original native stack,
 * and installer/page ownership contracts. No C call, heap, Binder or Dart GC.
 * All touched integer registers/NZCV are restored; vector state is untouched
 * except the selected low32 of v0. FPCR/FPSR and x18..x30 are never modified.
 * Normal stack temporarily grows128 bytes; asynchronous unwinding of copied
 * code is a separate lifecycle obligation, not supplied by this template.
 */
.arch armv8-a
.arch_extension lse
.section .text.hc_home_recents_template,"ax",%progbits
.p2align 4
.global hc_home_recents_template
.hidden hc_home_recents_template
.type hc_home_recents_template,%function
hc_home_recents_template:
 sub sp,sp,#128
 stp x0,x1,[sp,#0]
 stp x8,x9,[sp,#16]
 stp x10,x11,[sp,#32]
 stp x12,x13,[sp,#48]
 stp x14,x15,[sp,#64]
 stp x16,x17,[sp,#80]
 mrs x16,nzcv
 str x16,[sp,#96]
 ldr x17,hc_home_recents_lease_literal
 ldar x12,[x17]
 cbnz x12,.Llease
 mov x13,#1
 casal x12,x13,[x17]
 cbz x12,.Ldone // Early original-only update wins pending->permanently denied.
.Llease:
 and x13,x12,#255
 cmp x13,#30
 b.lo .Ldone
 cmp x13,#200
 b.hi .Ldone
 mov x0,#7 // CLOCK_BOOTTIME
 add x1,sp,#104
 mov x8,#113 // Linux AArch64 clock_gettime; exact policy admission is external.
 svc #0
 cbnz x0,.Ldeny
 ldp x9,x10,[sp,#104]
 tbnz x9,#63,.Ldeny
 tbnz x10,#63,.Ldeny
 movz x11,#0xca00
 movk x11,#0x3b9a,lsl #16 //1000000000
 cmp x10,x11
 b.hs .Ldeny
 mov x11,#1000
 umulh x14,x9,x11
 cbnz x14,.Ldeny
 mul x9,x9,x11
 movz x11,#0x4240
 movk x11,#0xf,lsl #16 //1000000
 udiv x10,x10,x11
 adds x9,x9,x10
 b.cs .Ldeny
 cbz x9,.Ldeny
 lsr x10,x12,#8
 cmp x9,x10
 b.hs .Ldeny
 // A same-selection renewal is allowed; cancellation/changed selection never
 // qualifies. The publisher only renews the initial epoch and percentage.
 ldr x17,hc_home_recents_lease_literal
 ldar x17,[x17]
 and x14,x17,#255
 cmp x14,x13
 b.ne .Ldone
 lsr x17,x17,#8
 cmp x9,x17
 b.hs .Ldone
 sub x13,x13,#30
 adr x16,.Lratios
 ldr w14,[x16,x13,lsl #2]
 ins v0.s[0],w14
 b .Ldone
.Ldeny:
 ldr x17,hc_home_recents_lease_literal
 mov x13,#1
 casal x12,x13,[x17] // Never overwrite a concurrent newer valid publication.
.Ldone:
 ldr x16,[sp,#96]
 msr nzcv,x16
 ldp x0,x1,[sp,#0]
 ldp x8,x9,[sp,#16]
 ldp x10,x11,[sp,#32]
 ldp x12,x13,[sp,#48]
 ldp x14,x15,[sp,#64]
 ldp x16,x17,[sp,#80]
 add sp,sp,#128
 str s0,[x29,#0x1c] // Exact displaced instruction at Home VA7a58a8.
.global hc_home_recents_return_branch
.hidden hc_home_recents_return_branch
hc_home_recents_return_branch:
 b hc_home_recents_return_branch // Installer replaces before executable publish.
.global hc_home_recents_code_end
.hidden hc_home_recents_code_end
hc_home_recents_code_end:
.size hc_home_recents_template,.-hc_home_recents_template
.p2align 3
.global hc_home_recents_lease_literal
.hidden hc_home_recents_lease_literal
hc_home_recents_lease_literal:
.quad 0 // Aligned permanent atomic lease address, filled before RX publication.
.p2align 2
.Lratios:
 .float 0.30,0.31,0.32,0.33,0.34,0.35,0.36,0.37,0.38,0.39
 .float 0.40,0.41,0.42,0.43,0.44,0.45,0.46,0.47,0.48,0.49
 .float 0.50,0.51,0.52,0.53,0.54,0.55,0.56,0.57,0.58,0.59
 .float 0.60,0.61,0.62,0.63,0.64,0.65,0.66,0.67,0.68,0.69
 .float 0.70,0.71,0.72,0.73,0.74,0.75,0.76,0.77,0.78,0.79
 .float 0.80,0.81,0.82,0.83,0.84,0.85,0.86,0.87,0.88,0.89
 .float 0.90,0.91,0.92,0.93,0.94,0.95,0.96,0.97,0.98,0.99
 .float 1.00,1.01,1.02,1.03,1.04,1.05,1.06,1.07,1.08,1.09
 .float 1.10,1.11,1.12,1.13,1.14,1.15,1.16,1.17,1.18,1.19
 .float 1.20,1.21,1.22,1.23,1.24,1.25,1.26,1.27,1.28,1.29
 .float 1.30,1.31,1.32,1.33,1.34,1.35,1.36,1.37,1.38,1.39
 .float 1.40,1.41,1.42,1.43,1.44,1.45,1.46,1.47,1.48,1.49
 .float 1.50,1.51,1.52,1.53,1.54,1.55,1.56,1.57,1.58,1.59
 .float 1.60,1.61,1.62,1.63,1.64,1.65,1.66,1.67,1.68,1.69
 .float 1.70,1.71,1.72,1.73,1.74,1.75,1.76,1.77,1.78,1.79
 .float 1.80,1.81,1.82,1.83,1.84,1.85,1.86,1.87,1.88,1.89
 .float 1.90,1.91,1.92,1.93,1.94,1.95,1.96,1.97,1.98,1.99
 .float 2.00
.global hc_home_recents_template_end
.hidden hc_home_recents_template_end
hc_home_recents_template_end:
.section .note.GNU-stack,"",%progbits
