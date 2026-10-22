// Explicit frame/allocator/outer-continuation adapters, not a Dart VM.
.text
.global preview_allocate
.type preview_allocate,%function
preview_allocate:
 ldr x0,[x26,#0x60]
 add x16,x0,#64
 str x16,[x26,#0x60]
 mov x16,#0x766
 lsl x16,x16,#12
 str x16,[x0]
 add x0,x0,#1
 ret
.size preview_allocate,.-preview_allocate
.global preview_cal_bridge
.type preview_cal_bridge,%function
preview_cal_bridge:
 str x2,[x26,#0xb0]
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 sub x15,x15,#0xf0
 stur x1,[x29,#-0xa8]
 ldr x4,[x26,#0x98]
 stur w3,[x4,#0x17]
 ldr x16,[x26,#0xa0]
 br x16
.size preview_cal_bridge,.-preview_cal_bridge
.global preview_cal_return
.type preview_cal_return,%function
preview_cal_return:
 mov x0,x22
 mov x15,x29
 ldp x29,x30,[x15],#16
 ret
.size preview_cal_return,.-preview_cal_return
.global preview_load_record
.type preview_load_record,%function
preview_load_record:
 ldr x16,[x15]
 str x16,[x26,#0xb8]
 str x2,[x26,#0xc0]
 mov x0,x22
 ret
.size preview_load_record,.-preview_load_record
.section .note.GNU-stack,"",%progbits
// Experimental phone-class-only callsite target. Original BL already owns LR;
// tail-branch enters the original fixed function without a new Dart return PC.
.text
.global preview_column_begin,preview_column_end
.type preview_column_begin,%function
preview_column_begin:
 mov x2,#3
 stur x2,[x0,#7]
 b preview_column_begin
preview_column_end:
.size preview_column_begin,.-preview_column_begin
// Disabled-path callee observation: numerical OEM variable algorithm is not
// emulated. Record its original x2 input and publish a chosen row through Rx.
.global preview_variable_record
.type preview_variable_record,%function
preview_variable_record:
 str x2,[x26,#0xc8]
 ldur w16,[x1,#0x43]
 add x16,x16,x28,lsl #32
 mov w17,#14
 stur w17,[x16,#0x13]
 mov x0,x22
 ret
.size preview_variable_record,.-preview_variable_record
