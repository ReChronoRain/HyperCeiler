// Private-image experiment, leaf code and local immutable caller/mutable gate.
// Pure leaf: no call/frame/SP/LR/NZCV modifications or stack slot type changes.
.text
.balign 8
.global grid_quantize_begin,grid_quantize_end,grid_leaf_expected,grid_leaf_gate,grid_leaf_branch
.type grid_quantize_begin,%function
grid_quantize_begin:
 ldur x16,[x29,#8]
 ldr x17,grid_leaf_expected
 eor x16,x16,x17
 cbnz x16,1f
 ldr x16,grid_leaf_gate
 tbz x16,#0,1f
 ldur x1,[x29,#-8]
 ldur d3,[x1,#0x47]
 fmul d3,d3,d1
 frintz d3,d3
 fdiv d3,d3,d1
 stur d3,[x1,#0x47]
 stur d3,[x1,#0x4f]
 fmov d0,d3
1:
 // Original 16B at a4ab78. FP-18 has always been a double in this frame.
 ldur x0,[x29,#-0x10]
 scvtf d1,x0
 fmul d2,d0,d1
 stur d2,[x29,#-0x18]
grid_leaf_branch:
 b .
.size grid_quantize_begin,.-grid_quantize_begin
.balign 8
grid_leaf_expected:
 .quad 0
grid_leaf_gate:
 .quad 0
grid_quantize_end:
.global singlepoint_condition
.type singlepoint_condition,%function
singlepoint_condition:
 ldr x0,[x26,#0xb0]
 ret
.size singlepoint_condition,.-singlepoint_condition
.section .note.GNU-stack,"",%progbits
