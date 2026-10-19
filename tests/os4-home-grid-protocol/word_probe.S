#include "producer_probe.S"
.text
.global secondary_dispatch
.type secondary_dispatch,%function
secondary_dispatch:
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 sub x15,x15,#0x100
 ldr x16,[x26,#0x88]
 stur x16,[x29,#-0xa8]
 ldr x16,[x26,#0x90]
 stur x16,[x29,#-0xc8]
 ldr x16,[x26,#0xa0]
 cbnz x16,1f
 mov x0,x2
 mov x2,x1
 mov x1,x3
1:
 ldr x16,[x26,#0x98]
 br x16
.size secondary_dispatch,.-secondary_dispatch
.global secondary_return
.type secondary_return,%function
secondary_return:
 mov x15,x29
 ldp x29,x30,[x15],#16
 ret
.size secondary_return,.-secondary_return
.global geometry_foreign
.type geometry_foreign,%function
geometry_foreign:
 adrp x16,geometry_foreign_calls
 add x16,x16,:lo12:geometry_foreign_calls
 ldr x17,[x16]
 add x17,x17,#1
 str x17,[x16]
 mov x0,x22
 ret
.size geometry_foreign,.-geometry_foreign
.section .note.GNU-stack,"",%progbits
