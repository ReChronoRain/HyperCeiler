// Private-image experiment; pure leaf, no stack/frame/LR/NZCV changes.
.text
.balign 4
.global grid_quantize_begin
.global grid_quantize_end
.type grid_quantize_begin,%function
grid_quantize_begin:
 ldur x3,[x29,#-8]
 ldur d3,[x3,#0x47]
 fmul d3,d3,d1
 frintz d3,d3
 fdiv d3,d3,d1
 stur d3,[x3,#0x47]
 stur d3,[x3,#0x4f]
 fsub d2,d0,d3
 ldur x16,[x29,#-0x10]
 scvtf d1,x16
 fmul d2,d2,d1
 fmov d1,#2.0
 fdiv d2,d2,d1
 ldur d1,[x3,#0x1f]
 fadd d2,d2,d1
 stur d2,[x3,#0x1f]
 fmov d0,d3
 // Full four words overwritten at a49c6c, in original order.
 ldur x3,[x29,#-8]
 stur d0,[x3,#0x8f]
 stur d0,[x3,#0x87]
 ldur w2,[x3,#0x43]
 b . // runner fills a49c7c continuation
.size grid_quantize_begin,.-grid_quantize_begin
grid_quantize_end:
.global singlepoint_condition
.type singlepoint_condition,%function
singlepoint_condition:
 ldr x0,[x26,#0xb0]
 ret
.size singlepoint_condition,.-singlepoint_condition
.section .note.GNU-stack,"",%progbits
