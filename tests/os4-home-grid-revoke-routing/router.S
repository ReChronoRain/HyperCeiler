// Experimental immutable entry router. No native/Dart call or extra frame.
// x16/x17 scratch contract is explicit; all parameter/vector/flags/LR intact
// on passthrough. Exact actual caller literals and original target addresses.
.text
.balign 8
.global grid_router_begin,grid_router_end,grid_router_gate,grid_router_targets,grid_router_callers,grid_router_captured,grid_router_payload_store
.type grid_router_begin,%function
grid_router_begin:
 ldr x17,grid_router_callers
 eor x17,x30,x17
 cbz x17,1f
 ldr x17,grid_router_callers+8
 eor x17,x30,x17
 cbz x17,1f
 ldr x17,grid_router_callers+16
 eor x17,x30,x17
 cbz x17,1f
 ldr x17,grid_router_callers+24
 eor x17,x30,x17
 cbz x17,1f
 ldr x17,grid_router_callers+32
 eor x17,x30,x17
 cbz x17,2f
 ldr x17,grid_router_callers+40
 eor x17,x30,x17
 cbz x17,2f
 b 5f
1:
 adr x17,grid_router_gate
 ldar x16,[x17]
grid_router_captured:
 nop // test-only cut can clear gate after this invocation captured it
 tbz x16,#0,4f
 b 3f
2:
 adr x17,grid_router_gate
 ldar x16,[x17]
 tbz x16,#0,5f
3:
 ubfx x2,x16,#8,#8
 ubfx x3,x16,#16,#8
 ldr x17,grid_router_callers+8
 eor x17,x30,x17
 cbnz x17,5f
grid_router_payload_store:
 stur x2,[x0,#7]
5:
 ldr x16,grid_router_targets
 br x16
4:
 ldr x16,grid_router_targets+8
 br x16
.balign 8
grid_router_gate: .quad 0
grid_router_targets: .quad 0,0
grid_router_callers: .quad 0,0,0,0,0,0
grid_router_end:
.size grid_router_begin,.-grid_router_begin
.global routing_dispatch,routing_return,routing_variable_record
.type routing_dispatch,%function
routing_dispatch:
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 sub x15,x15,#0xf0
 ldr x16,[x26,#0xd0]
 stur x16,[x29,#-0xa8]
 ldr x16,[x26,#0xd8]
 stur x16,[x29,#-0xc8]
 ldr x17,[x26,#0xe8]
 cbnz x17,1f
 mov x0,x2
 mov x2,x1
 mov x1,x3
 b 2f
1:
 mov x0,x16
 mov x5,#0 // actual payload layout field of this fixture
 mov x3,#4 // actual original reason decoder x3, not a typed row
2:
 ldr x16,[x26,#0xe0]
 br x16
.size routing_dispatch,.-routing_dispatch
.type routing_return,%function
routing_return:
 mov x15,x29
 ldp x29,x30,[x15],#16
 ret
.size routing_return,.-routing_return
.type routing_variable_record,%function
routing_variable_record:
 // Original variable prologue has executed through0xa4b478, recording fields.
 ldur x0,[x29,#-0x10]
 str x0,[x26,#0xf0]
 ldur x0,[x29,#-0x18]
 str x0,[x26,#0xf8]
 ldur x0,[x29,#-0x20]
 str x0,[x26,#0x100]
 ldr x0,[x26,#0x108]
 add x0,x0,#1
 str x0,[x26,#0x108]
 mov x0,x22
 mov x15,x29
 ldp x29,x30,[x15],#16
 ret
.size routing_variable_record,.-routing_variable_record
.section .note.GNU-stack,"",%progbits
.text
.global routing_natural_dispatch
.type routing_natural_dispatch,%function
routing_natural_dispatch:
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 sub x15,x15,#0xf0
 ldr x17,[x26,#0xe8]
 cbnz x17,1f
 mov x6,#0
 ldr x17,[x26,#0xe0]
 br x17
1:
 mov x16,x2
 mov x3,x5
 ldr x17,[x26,#0xe0]
 br x17
.size routing_natural_dispatch,.-routing_natural_dispatch
.section .note.GNU-stack,"",%progbits
