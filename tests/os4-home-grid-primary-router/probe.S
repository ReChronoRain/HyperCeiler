// Test-only external provider/async/throw boundaries. The router calls none.
.text
.global primary_provider
.type primary_provider,%function
primary_provider:
 // This adapter is entered from the complete original alignPixel frame.
 // Observe the original caller PC. Revoke only after fixedRows's first BL,
 // not during the enclosing handle's earlier padding alignment.
 ldr x16,[x29,#8]
 ldr x17,[x26,#0x90]
 ldr x0,[x17]
 eor x0,x0,x16
 cbnz x0,1f
 ldr x0,[x17,#8]
 cbz x0,1f
 ldr x16,[x17,#16]
 stlr x16,[x0]
 str xzr,[x17,#8]
 ldr x16,[x17,#24]
 add x16,x16,#1
 str x16,[x17,#24]
 // Full fixed frame retains the caller's original saved LR, handler,
 // context, and config pointers; no private router return PC is saved.
 ldr x16,[x29]
 ldr x0,[x16,#8]
 str x0,[x17,#32]
 ldur x0,[x16,#-0x10]
 str x0,[x17,#40]
 ldur x0,[x16,#-0x18]
 str x0,[x17,#48]
 ldur x0,[x16,#-8]
 str x0,[x17,#56]
1:
 ldr x0,[x26,#0x88]
 ret
.size primary_provider,.-primary_provider
.global primary_init_async
.type primary_init_async,%function
primary_init_async:
 ret
.size primary_init_async,.-primary_init_async
.global primary_return_async
.type primary_return_async,%function
primary_return_async:
 mov x15,x29
 ldp x29,x30,[x15],#16
 ret
.size primary_return_async,.-primary_return_async
.global primary_original_null_throw
.type primary_original_null_throw,%function
primary_original_null_throw:
 // Test observes original null-decode error branch, not Dart exception unwind.
 mov x0,#42
 mov x8,#93
 svc #0
 brk #0
.size primary_original_null_throw,.-primary_original_null_throw

// Optional boundary observers. They never call, alter saved LR or create a
// Dart frame. They capture every non-scratch GPR, full Q, SP and NZCV before
// and after the linked router, then branch to the real original target.
.macro capture offset,kind,target
 ldr x17,[x26,#0x98]
 add x17,x17,#\offset
 stp x0,x1,[x17]
 stp x2,x3,[x17,#16]
 stp x4,x5,[x17,#32]
 stp x6,x7,[x17,#48]
 stp x8,x9,[x17,#64]
 stp x10,x11,[x17,#80]
 stp x12,x13,[x17,#96]
 stp x14,x15,[x17,#112]
 stp x18,x19,[x17,#144]
 stp x20,x21,[x17,#160]
 stp x22,x23,[x17,#176]
 stp x24,x25,[x17,#192]
 stp x26,x27,[x17,#208]
 stp x28,x29,[x17,#224]
 str x30,[x17,#240]
 mov x16,sp
 str x16,[x17,#248]
 stp q0,q1,[x17,#256]
 stp q2,q3,[x17,#288]
 stp q4,q5,[x17,#320]
 stp q6,q7,[x17,#352]
 stp q8,q9,[x17,#384]
 stp q10,q11,[x17,#416]
 stp q12,q13,[x17,#448]
 stp q14,q15,[x17,#480]
 stp q16,q17,[x17,#512]
 stp q18,q19,[x17,#544]
 stp q20,q21,[x17,#576]
 stp q22,q23,[x17,#608]
 stp q24,q25,[x17,#640]
 stp q26,q27,[x17,#672]
 stp q28,q29,[x17,#704]
 stp q30,q31,[x17,#736]
 mrs x16,nzcv
 str x16,[x17,#768]
 mov x16,#\kind
 str x16,[x17,#776]
 ldr x17,[x26,#\target]
 br x17
.endm
.global primary_observe_entry,primary_observe_accept,primary_observe_original,primary_observe_variable
.type primary_observe_entry,%function
primary_observe_entry:
 capture 0,0,0xa0
.size primary_observe_entry,.-primary_observe_entry
.type primary_observe_accept,%function
primary_observe_accept:
 capture 0x400,1,0xa8
.size primary_observe_accept,.-primary_observe_accept
.type primary_observe_original,%function
primary_observe_original:
 capture 0x400,2,0xb0
.size primary_observe_original,.-primary_observe_original
.type primary_observe_variable,%function
primary_observe_variable:
 capture 0x400,3,0xb8
.size primary_observe_variable,.-primary_observe_variable
.section .note.GNU-stack,"",%progbits
