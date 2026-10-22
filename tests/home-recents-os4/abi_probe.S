/* Controlled CABI caller/capture around the actual copied production gateway. */
.text
.p2align 4
.global recents_capture
.type recents_capture,%function
recents_capture:
 stp x16,x17,[sp,#-16]!
 adrp x16,recents_observed
 add x16,x16,:lo12:recents_observed
.irp n,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,18,19,20,21,22,23,24,25,26,27,28,29,30
 str x\n,[x16,#(8*\n)]
.endr
 ldp x9,x10,[sp]
 stp x9,x10,[x16,#128]
 mrs x9,nzcv
 str x9,[x16,#248]
 add x9,sp,#16
 str x9,[x16,#256]
 mrs x9,fpcr
 str x9,[x16,#264]
 mrs x9,fpsr
 str x9,[x16,#272]
.irp n,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
 str q\n,[x16,#(288+16*\n)]
.endr
 ldr w9,[x29,#0x1c]
 str w9,[x16,#800]
 add sp,sp,#16
 ret
.size recents_capture,.-recents_capture
.global recents_probe
.type recents_probe,%function
// x0=actual copied gateway, x1=NZCV, x2=FPCR, x3=FPSR.
recents_probe:
 sub sp,sp,#352
 stp x19,x20,[sp,#0]
 stp x21,x22,[sp,#16]
 stp x23,x24,[sp,#32]
 stp x25,x26,[sp,#48]
 stp x27,x28,[sp,#64]
 stp x29,x30,[sp,#80]
 stp q8,q9,[sp,#96]
 stp q10,q11,[sp,#128]
 stp q12,q13,[sp,#160]
 stp q14,q15,[sp,#192]
 str x18,[sp,#224]
 mrs x16,fpcr
 str x16,[sp,#232]
 mrs x16,fpsr
 str x16,[sp,#240]
 mrs x16,nzcv
 str x16,[sp,#248]
 stp x0,x1,[sp,#256]
 stp x2,x3,[sp,#272]
 adrp x16,recents_expected_sp
 add x16,x16,:lo12:recents_expected_sp
 mov x17,sp
 str x17,[x16]
.irp n,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,17,18,19,20,21,22,23,24,25,26,27,28
 mov x\n,#(0x100+\n)
.endr
 add x29,sp,#304
.irp n,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
 movi v\n\().16b,#(32+\n)
.endr
 ldr x16,[sp,#272]
 msr fpcr,x16
 ldr x16,[sp,#280]
 msr fpsr,x16
 ldr x16,[sp,#264]
 msr nzcv,x16
 ldr x16,[sp,#256]
 blr x16
.global recents_expected_lr
recents_expected_lr:
 ldp x19,x20,[sp,#0]
 ldp x21,x22,[sp,#16]
 ldp x23,x24,[sp,#32]
 ldp x25,x26,[sp,#48]
 ldp x27,x28,[sp,#64]
 ldp x29,x30,[sp,#80]
 ldp q8,q9,[sp,#96]
 ldp q10,q11,[sp,#128]
 ldp q12,q13,[sp,#160]
 ldp q14,q15,[sp,#192]
 ldr x18,[sp,#224]
 ldr x16,[sp,#232]
 msr fpcr,x16
 ldr x16,[sp,#240]
 msr fpsr,x16
 ldr x16,[sp,#248]
 msr nzcv,x16
 add sp,sp,#352
 ret
.size recents_probe,.-recents_probe
.section .note.GNU-stack,"",%progbits
