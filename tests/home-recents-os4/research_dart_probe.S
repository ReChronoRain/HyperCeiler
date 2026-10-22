.text
.align 4
.global run_dart
.type run_dart,%function
// CABI x0=target, x1=thread, x2=pool, x3=Dart stack, x4=receiver.
// Heap is explicitly below 4GiB. No GC path may execute in this fixture.
run_dart:
 stp x29,x30,[sp,#-16]!
 stp x19,x20,[sp,#-16]!
 stp x21,x22,[sp,#-16]!
 stp x23,x24,[sp,#-16]!
 stp x25,x26,[sp,#-16]!
 stp x27,x28,[sp,#-16]!
 mov x16,x0
 mov x26,x1
 mov x27,x2
 mov x15,x3
 mov x1,x4
 mov x28,xzr
 mov x22,#1
 movk x22,#1,lsl #16
 blr x16
 ldp x27,x28,[sp],#16
 ldp x25,x26,[sp],#16
 ldp x23,x24,[sp],#16
 ldp x21,x22,[sp],#16
 ldp x19,x20,[sp],#16
 ldp x29,x30,[sp],#16
 ret
.section .note.GNU-stack,"",%progbits
