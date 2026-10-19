// Experiment only. Six non-calling instructions; no frame/SP/LR/NZCV change.
.text
.balign 4
.global grid_quantize_begin
.global grid_quantize_end
.type grid_quantize_begin,%function
grid_quantize_begin:
 ldur d1,[x29,#-0x18]
 fmul d3,d3,d1
 frintz d3,d3
 fdiv d3,d3,d1
 ldur x3,[x29,#-0x8]
 b . // runner patches this one immediate B to original a49c48
.size grid_quantize_begin,.-grid_quantize_begin
grid_quantize_end:
 stur d1,[x29,#-0x18]
 ldur d2,[x29,#-0x30]
 fsub d2,d2,d0
 frintn d3,d3 // only for a negative control, never copied by positive
.section .note.GNU-stack,"",%progbits
