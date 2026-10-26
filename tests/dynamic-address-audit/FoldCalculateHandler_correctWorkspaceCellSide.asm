0151fa58 +0000 stp     x29, x30, [x15, #-0x10]!
0151fa5c +0004 mov     x29, x15
0151fa60 +0008 sub     x15, x15, #0x18
0151fa64 +000c fmov    d0, #2.00000000
0151fa68 +0010 mov     x0, x2
0151fa6c +0014 stur    x2, [x29, #-8]
0151fa70 +0018 ldur    d1, [x0, #0x2b]
0151fa74 +001c ldur    x1, [x0, #0x1b]
0151fa78 +0020 scvtf   d2, x1
0151fa7c +0024 fmul    d3, d1, d2
0151fa80 +0028 ldur    d1, [x0, #0xb]
0151fa84 +002c fsub    d2, d1, d3
0151fa88 +0030 fdiv    d1, d2, d0
0151fa8c +0034 ldur    w1, [x0, #0x3b]
0151fa90 +0038 add     x1, x1, x28, lsl #32
0151fa94 +003c ldur    w2, [x0, #0x3f]
0151fa98 +0040 add     x2, x2, x28, lsl #32
0151fa9c +0044 ldur    d0, [x2, #7]
0151faa0 +0048 fadd    d2, d0, d1
0151faa4 +004c ldur    d0, [x2, #0x17]
0151faa8 +0050 fsub    d3, d0, d1
0151faac +0054 ldp     x2, x3, [x26, #0x60]
0151fab0 +0058 add     x2, x2, #0x10
0151fab4 +005c cmp     x3, x2
0151fab8 +0060 b.ls    #0x151fb2c
0151fabc +0064 str     x2, [x26, #0x60]
0151fac0 +0068 sub     x2, x2, #0xf
0151fac4 +006c mov     x3, #0xe15c
0151fac8 +0070 movk    x3, #3, lsl #16
0151facc +0074 stur    x3, [x2, #-1]
0151fad0 +0078 stur    d2, [x2, #7]
0151fad4 +007c ldp     x3, x4, [x26, #0x60]
0151fad8 +0080 add     x3, x3, #0x10
0151fadc +0084 cmp     x4, x3
0151fae0 +0088 b.ls    #0x151fb48
0151fae4 +008c str     x3, [x26, #0x60]
0151fae8 +0090 sub     x3, x3, #0xf
0151faec +0094 mov     x4, #0xe15c
0151faf0 +0098 movk    x4, #3, lsl #16
0151faf4 +009c stur    x4, [x3, #-1]
0151faf8 +00a0 stur    d3, [x3, #7]
0151fafc +00a4 stp     x3, x2, [x15]
0151fb00 +00a8 add     x4, x27, #0x33, lsl #12
0151fb04 +00ac ldr     x4, [x4, #0xf58]
0151fb08 +00b0 bl      #0x151eaf0 => ('Insets.copyWith', 22145776)
0151fb0c +00b4 str     x0, [x15]
0151fb10 +00b8 ldur    x1, [x29, #-8]
0151fb14 +00bc add     x4, x27, #0x33, lsl #12
0151fb18 +00c0 ldr     x4, [x4, #0xf60]
0151fb1c +00c4 bl      #0x151e178 => ('ComputeContext.copyWith', 22143352)
0151fb20 +00c8 mov     x15, x29
0151fb24 +00cc ldp     x29, x30, [x15], #0x10
0151fb28 +00d0 ret     
0151fb2c +00d4 stp     q2, q3, [x15, #-0x20]!
0151fb30 +00d8 stp     x0, x1, [x15, #-0x10]!
0151fb34 +00dc bl      #0x1b581e0 => (None, None)
0151fb38 +00e0 mov     x2, x0
0151fb3c +00e4 ldp     x0, x1, [x15], #0x10
0151fb40 +00e8 ldp     q2, q3, [x15], #0x20
0151fb44 +00ec b       #0x151fad0
0151fb48 +00f0 str     q3, [x15, #-0x10]!
0151fb4c +00f4 stp     x1, x2, [x15, #-0x10]!
0151fb50 +00f8 str     x0, [x15, #-8]!
0151fb54 +00fc bl      #0x1b581e0 => (None, None)
0151fb58 +0100 mov     x3, x0
0151fb5c +0104 ldr     x0, [x15], #8
0151fb60 +0108 ldp     x1, x2, [x15], #0x10
0151fb64 +010c ldr     q3, [x15], #0x10
0151fb68 +0110 b       #0x151faf8