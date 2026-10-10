/* Fixture-only entry adapters for unchanged OS4 ARM64 blocks. */
.text
.p2align 4
.global run_card_tail
.type run_card_tail,%function
// x0=entry, x1=receiver(tagged), x2=pool, x3=thread, x4=Dart stack top.
run_card_tail:
 stp x29,x30,[sp,#-16]!
 stp x19,x20,[sp,#-16]!
 stp x21,x22,[sp,#-16]!
 stp x23,x24,[sp,#-16]!
 stp x25,x26,[sp,#-16]!
 stp x27,x28,[sp,#-16]!
 mov x16,x0
 mov x27,x2
 mov x26,x3
 mov x28,xzr
 mov x22,#1
 mov x15,x4
 adr x30,1f
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 sub x15,x15,#0x80
 stur x1,[x29,#-0x10]
 stur xzr,[x29,#-8]
 mov x4,x1
 br x16
1:
 ldp x27,x28,[sp],#16
 ldp x25,x26,[sp],#16
 ldp x23,x24,[sp],#16
 ldp x21,x22,[sp],#16
 ldp x19,x20,[sp],#16
 ldp x29,x30,[sp],#16
 ret
.size run_card_tail,.-run_card_tail
.global run_card_predicate
.type run_card_predicate,%function
// x0=entry block, x1=receiver(tagged), x2=Dart stack top, x3=null.
run_card_predicate:
 stp x29,x30,[sp,#-16]!
 stp x22,x23,[sp,#-16]!
 mov x16,x0
 mov x0,x1
 mov x22,x3
 mov x15,x2
 adr x30,2f
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 br x16
2:
 ldp x22,x23,[sp],#16
 ldp x29,x30,[sp],#16
 ret
.size run_card_predicate,.-run_card_predicate

.global run_card_constructor
.type run_card_constructor,%function
// x0=full original constructor, x1=tagged receiver, x2=pool, x3=thread, x4=stack top.
run_card_constructor:
 stp x29,x30,[sp,#-16]!
 stp x19,x20,[sp,#-16]!
 stp x21,x22,[sp,#-16]!
 stp x23,x24,[sp,#-16]!
 stp x25,x26,[sp,#-16]!
 stp x27,x28,[sp,#-16]!
 mov x16,x0
 mov x27,x2
 mov x26,x3
 mov x28,xzr
 mov x22,#1
 mov x15,x4
 blr x16
 ldp x27,x28,[sp],#16
 ldp x25,x26,[sp],#16
 ldp x23,x24,[sp],#16
 ldp x21,x22,[sp],#16
 ldp x19,x20,[sp],#16
 ldp x29,x30,[sp],#16
 ret
.size run_card_constructor,.-run_card_constructor
.section .note.GNU-stack,"",%progbits
.text
.global run_card_gateway_probe
.type run_card_gateway_probe,%function
// Candidate-only ABI probe: x0=gateway, x1=object, x2=stack top, x3=output, x4=Thread.
run_card_gateway_probe:
 stp x29,x30,[sp,#-16]!
 stp x19,x20,[sp,#-16]!
 stp x21,x22,[sp,#-16]!
 stp x23,x24,[sp,#-16]!
 stp x25,x26,[sp,#-16]!
 stp x27,x28,[sp,#-16]!
 mov x19,x3
 mov x26,x4
 mov x16,x0
 mov x15,x2
 mov x29,#0x2929
 adr x30,3f
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 sub x15,x15,#0x80
 stur x1,[x29,#-16]
 stur x1,[x29,#-8]
 mov x22,#1
 mov x0,#0x0101
 mov x1,#0x1111
 mov x2,#0x2222
 mov x3,#0x3333
 mov x4,#0x4444
 mov x5,#0x5555
 mov x6,#0x6666
 mov x7,#0x7777
 mov x8,#0x8888
 mov x9,#0x9999
 mov x10,#0xaaaa
 mov x11,#0xbbbb
 mov x12,#0xcccc
 mov x13,#0xdddd
 mov x14,#0xeeee
 mov x17,#0xa0000000
 msr nzcv,x17
 br x16
3:
 stp x0,x1,[x19,#0]
 stp x2,x3,[x19,#16]
 stp x4,x5,[x19,#32]
 stp x6,x7,[x19,#48]
 stp x8,x9,[x19,#64]
 stp x10,x11,[x19,#80]
 stp x12,x13,[x19,#96]
 stp x14,x15,[x19,#112]
 str x29,[x19,#128]
 mrs x17,nzcv
 str x17,[x19,#136]
 ldp x27,x28,[sp],#16
 ldp x25,x26,[sp],#16
 ldp x23,x24,[sp],#16
 ldp x21,x22,[sp],#16
 ldp x19,x20,[sp],#16
 ldp x29,x30,[sp],#16
 ret
.size run_card_gateway_probe,.-run_card_gateway_probe
