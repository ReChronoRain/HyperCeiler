/* SPDX-License-Identifier: AGPL-3.0-or-later */
/* Copyable exact-ROM Weather normal-return gateways.
 * Each template is284bytes of code plus padding and one injected pointer.
 * Every code reference is internal and PC-relative: no external relocations,
 * PLT, heap, C/Dart calls, retained receiver or FP/vector writes.
 * x16/x17 are Dart scratch; all other touched state is restored before the
 * original four-instruction epilogue. Valid pinned Thread/frame/receiver are
 * preconditions. Reading a live limit is not a Dart safepoint reservation.
 * Lease format: [absolute CLOCK_BOOTTIME nanoseconds:62 | raw mode:2].
 * No epoch identity or instantaneous revocation is implied by two samples.
 * clock_gettime errors fall back; a seccomp TRAP/KILL cannot be caught here.
 * The installer must separately authorize exact image, caller, OS syscall
 * policy, RX publication/lifetime and both original patch sites. This file
 * neither installs itself nor supplies an application-level safety proof.
 */
.text
.p2align 4
.macro card_epilogue name, receiver_offset
.section .text.\name,"ax",%progbits
.p2align 4
.global \name
.hidden \name
.type \name,%function
\name:
 // Exact vendor Thread fields: live stack_limit +0x48, never saved +0x678.
 // x16 holds the incoming flags until admission is decided; no stack access.
 mrs x16,nzcv
 ldr x17,[x26,#0x48]
 cbz x17,.Lno_stack_\name
 cmn x17,#4
 b.hs .Lno_stack_\name // explicit unsigned [-4,-1] interrupt sentinel range
 cmp x15,x17
 b.ls .Lno_stack_\name // no subtraction underflow
 sub x17,x15,x17
 cmp x17,#96
 b.ls .Lno_stack_\name // equality is conservatively rejected
 msr nzcv,x16
 sub x15,x15,#96
 stp x0,x1,[x15,#0]
 stp x2,x8,[x15,#16]
 stp x9,x10,[x15,#32]
 stp x11,x12,[x15,#48]
 mrs x16,nzcv
 str x16,[x15,#64]
 ldr x17,\name\()_lease_literal
 nop // Preserve the reviewed284-byte instruction layout.
 ldar x12,[x17]
 and x16,x12,#3
 cmp x16,#2
 b.lo .Ldone_\name
 mov x0,#7
 add x1,x15,#80
 mov x8,#113 // Linux AArch64 clock_gettime; policy admission is external.
 svc #0
 cbnz x0,.Ldone_\name
 ldp x9,x10,[x15,#80]
 tbnz x9,#63,.Ldone_\name
 tbnz x10,#63,.Ldone_\name
 movz x11,#0xca00
 movk x11,#0x3b9a,lsl #16
 cmp x10,x11
 b.hs .Ldone_\name
 umulh x17,x9,x11
 cbnz x17,.Ldone_\name
 mul x9,x9,x11
 adds x9,x9,x10
 b.cs .Ldone_\name
 cbz x9,.Ldone_\name
 lsr x10,x12,#2
 cmp x9,x10
 b.hs .Ldone_\name
 ldr x17,\name\()_lease_literal
 nop // Preserve the reviewed284-byte instruction layout.
 ldar x17,[x17]
 cmp x17,x12
 b.ne .Ldone_\name
 ldur x16,[x29,#\receiver_offset]
 tbz x16,#0,.Ldone_\name
 ldur x9,[x16,#-1]
 ubfx x9,x9,#12,#20
 cmp x9,#254
 b.ne .Ldone_\name
 and x17,x12,#3
 stur x17,[x16,#0x8b]
.Ldone_\name:
 ldr x16,[x15,#64]
 msr nzcv,x16
 ldp x0,x1,[x15,#0]
 ldp x2,x8,[x15,#16]
 ldp x9,x10,[x15,#32]
 ldp x11,x12,[x15,#48]
 add x15,x15,#96
 b .Lreturn_\name
.Lno_stack_\name:
 msr nzcv,x16
.Lreturn_\name:
 mov x0,x22
 mov x15,x29
 ldp x29,x30,[x15],#16
 ret
.global \name\()_code_end
.hidden \name\()_code_end
\name\()_code_end:
.size \name,.-\name
.if (\name\()_code_end-\name) != 284
.error "Weather card gateway instruction extent changed"
.endif
.space 4,0 //284bytes -> aligned288byte literal; template size296 is8-aligned.
.global \name\()_lease_literal
.hidden \name\()_lease_literal
.type \name\()_lease_literal,%object
\name\()_lease_literal:
.quad 0 // Installer injects aligned process-lifetime lease address before RX.
.size \name\()_lease_literal,8
.global \name\()_end
.hidden \name\()_end
\name\()_end:
.if (\name\()_lease_literal-\name) != 288
.error "Weather card lease literal offset changed"
.endif
.if (\name\()_end-\name) != 296
.error "Weather card template extent changed"
.endif
.endm
card_epilogue hc_weather_card_recompute_template, -16
card_epilogue hc_weather_card_constructor_template, -8
.section .note.GNU-stack,"",%progbits
