// Experimental linked PIC entry router. No C/Dart calls and no private Dart PC.
// x16/x17 are branch scratch. All other entry arguments and LR are retained.
// Targets and the nonzero X<<8|Y key are immutable while reachable.
// Feature key may toggle 0 <-> initialKey independently of the shared epoch.
.text
.p2align 2
.global primary_router
.type primary_router,%function
primary_router:
 adrp x17,primary_none_return
 ldr x17,[x17,:lo12:primary_none_return]
 eor x17,x30,x17
 cbz x17,.Lknown
 adrp x17,primary_fixed_return
 ldr x17,[x17,:lo12:primary_fixed_return]
 eor x17,x30,x17
 cbnz x17,.Loriginal
.Lknown:
 adrp x17,primary_epoch_address
 ldr x17,[x17,:lo12:primary_epoch_address]
 ldar x16,[x17]
 tbz x16,#0,.Ldisabled
 adrp x17,primary_config_key
 add x17,x17,:lo12:primary_config_key
 ldar x16,[x17]
 // Reject high16, columns outside3..9, rows outside4..13; preserve NZCV.
 lsr x17,x16,#16
 cbnz x17,.Ldisabled
 ubfx x17,x16,#8,#8
 sub x17,x17,#3
 lsr x17,x17,#3
 cbnz x17,.Ldisabled
 ubfx x17,x16,#8,#8
 sub x17,x17,#10
 cbz x17,.Ldisabled
 and x16,x16,#0xff
 lsr x17,x16,#4
 cbnz x17,.Ldisabled
 mov x17,#0x3ff0
 lsr x17,x17,x16
 tbz x17,#0,.Ldisabled
 // Exact original 11 instructions 15228e8..1522910. There is no call,
 // PC-relative literal, or branch in this prefix. Config remains nullable.
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 sub x15,x15,#0x30
 stur x1,[x29,#-0x10]
 stur x2,[x29,#-0x18]
 ldur w0,[x2,#7]
 add x0,x0,x28,lsl #32
 stur x0,[x29,#-8]
 ldur w3,[x0,#0x3b]
 add x3,x3,x28,lsl #32
 cmp w3,w22
 mov x4,x16
 adrp x17,primary_resume
 ldr x17,[x17,:lo12:primary_resume]
 br x17
.Ldisabled:
 adrp x17,primary_none_return
 ldr x17,[x17,:lo12:primary_none_return]
 eor x17,x30,x17
 cbnz x17,.Loriginal
 adrp x17,primary_variable
 ldr x17,[x17,:lo12:primary_variable]
 br x17
.Loriginal:
 adrp x17,primary_original
 ldr x17,[x17,:lo12:primary_original]
 br x17
.size primary_router,.-primary_router

// Test publication API. It is never called while an admitted frame is active.
// x0 points to {noneLR,fixedLR,original,resume,variable,sharedEpoch*,configKey}.
.global primary_test_configure
.type primary_test_configure,%function
primary_test_configure:
 adrp x16,primary_none_return
 add x16,x16,:lo12:primary_none_return
 ldp x1,x2,[x0]
 stp x1,x2,[x16]
 ldp x1,x2,[x0,#16]
 stp x1,x2,[x16,#16]
 ldp x1,x2,[x0,#32]
 stp x1,x2,[x16,#32]
 ldr x1,[x0,#48]
 str x1,[x16,#48]
 ret
.size primary_test_configure,.-primary_test_configure
.global primary_test_feature_address
.type primary_test_feature_address,%function
primary_test_feature_address:
 adrp x0,primary_config_key
 add x0,x0,:lo12:primary_config_key
 ret
.size primary_test_feature_address,.-primary_test_feature_address

.bss
.balign 8
.hidden primary_none_return,primary_fixed_return,primary_original,primary_resume
.hidden primary_variable,primary_epoch_address,primary_config_key
.type primary_none_return,%object
primary_none_return: .zero 8
.type primary_fixed_return,%object
primary_fixed_return: .zero 8
.type primary_original,%object
primary_original: .zero 8
.type primary_resume,%object
primary_resume: .zero 8
.type primary_variable,%object
primary_variable: .zero 8
.type primary_epoch_address,%object
primary_epoch_address: .zero 8
.type primary_config_key,%object
primary_config_key: .zero 8
.section .note.GNU-stack,"",%progbits
