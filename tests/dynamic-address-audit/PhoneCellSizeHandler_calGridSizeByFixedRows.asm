01522d38 +0000 stp     x29, x30, [x15, #-0x10]!
01522d3c +0004 mov     x29, x15
01522d40 +0008 sub     x15, x15, #0x30
01522d44 +000c stur    x1, [x29, #-0x10]
01522d48 +0010 stur    x2, [x29, #-0x18]
01522d4c +0014 ldur    w0, [x2, #7]
01522d50 +0018 add     x0, x0, x28, lsl #32
01522d54 +001c stur    x0, [x29, #-8]
01522d58 +0020 ldur    w3, [x0, #0x3b]
01522d5c +0024 add     x3, x3, x28, lsl #32
01522d60 +0028 cmp     w3, w22
01522d64 +002c b.eq    #0x1522f88
01522d68 +0030 sbfx    x4, x3, #1, #0x1f
01522d6c +0034 tbz     w3, #0, #0x1522d74
01522d70 +0038 ldur    x4, [x3, #7]
01522d74 +003c stur    x4, [x1, #0x7b]
01522d78 +0040 ldur    d0, [x2, #0xb]
01522d7c +0044 ldur    d1, [x1, #0x6b]
01522d80 +0048 fsub    d2, d0, d1
01522d84 +004c ldur    x3, [x2, #0x1b]
01522d88 +0050 scvtf   d0, x3
01522d8c +0054 fdiv    d1, d2, d0
01522d90 +0058 add     x16, x22, #0x20
01522d94 +005c str     x16, [x15]
01522d98 +0060 mov     v0.16b, v1.16b
01522d9c +0064 add     x4, x27, #0x27, lsl #12
01522da0 +0068 ldr     x4, [x4, #0x790]
01522da4 +006c bl      #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01522da8 +0070 ldur    x1, [x29, #-0x10]
01522dac +0074 stur    d0, [x1, #0xb]
01522db0 +0078 ldur    x2, [x29, #-8]
01522db4 +007c ldur    w0, [x2, #0x37]
01522db8 +0080 add     x0, x0, x28, lsl #32
01522dbc +0084 stur    w0, [x1, #0x73]
01522dc0 +0088 ldurb   w16, [x1, #-1]
01522dc4 +008c ldurb   w17, [x0, #-1]
01522dc8 +0090 and     x16, x17, x16, lsr #2
01522dcc +0094 tst     x16, x28, lsr #32
01522dd0 +0098 b.eq    #0x1522dd8
01522dd4 +009c bl      #0x1b56978 => (None, None)
01522dd8 +00a0 ldur    w0, [x2, #0x33]
01522ddc +00a4 add     x0, x0, x28, lsl #32
01522de0 +00a8 stur    w0, [x1, #0x77]
01522de4 +00ac ldurb   w16, [x1, #-1]
01522de8 +00b0 ldurb   w17, [x0, #-1]
01522dec +00b4 and     x16, x17, x16, lsr #2
01522df0 +00b8 tst     x16, x28, lsr #32
01522df4 +00bc b.eq    #0x1522dfc
01522df8 +00c0 bl      #0x1b56978 => (None, None)
01522dfc +00c4 fmov    d0, #16.00000000
01522e00 +00c8 stur    d0, [x1, #0x2b]
01522e04 +00cc ldur    x0, [x29, #-0x18]
01522e08 +00d0 ldur    d1, [x0, #0x13]
01522e0c +00d4 fsub    d2, d1, d0
01522e10 +00d8 stur    d2, [x29, #-0x20]
01522e14 +00dc ldur    x0, [x1, #0x7b]
01522e18 +00e0 add     x3, x0, #1
01522e1c +00e4 scvtf   d0, x3
01522e20 +00e8 fdiv    d1, d2, d0
01522e24 +00ec add     x16, x22, #0x20
01522e28 +00f0 str     x16, [x15]
01522e2c +00f4 mov     v0.16b, v1.16b
01522e30 +00f8 add     x4, x27, #0x27, lsl #12
01522e34 +00fc ldr     x4, [x4, #0x790]
01522e38 +0100 bl      #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01522e3c +0104 ldur    x0, [x29, #-0x10]
01522e40 +0108 ldur    d1, [x0, #0xb]
01522e44 +010c fmin    v2.2d, v1.2d, v0.2d
01522e48 +0110 stur    d2, [x0, #0x23]
01522e4c +0114 stur    d2, [x0, #0x1b]
01522e50 +0118 stur    d2, [x0, #0x13]
01522e54 +011c stur    d2, [x0, #0xb]
01522e58 +0120 ldur    x1, [x0, #0x7b]
01522e5c +0124 scvtf   d0, x1
01522e60 +0128 fmul    d1, d2, d0
01522e64 +012c ldur    d0, [x29, #-0x20]
01522e68 +0130 fsub    d3, d0, d1
01522e6c +0134 fsub    d0, d3, d2
01522e70 +0138 ldur    x0, [x29, #-8]
01522e74 +013c stur    d0, [x29, #-0x20]
01522e78 +0140 ldur    w1, [x0, #0x27]
01522e7c +0144 add     x1, x1, x28, lsl #32
01522e80 +0148 add     x16, x27, #0x14, lsl #12
01522e84 +014c ldr     x16, [x16, #0x80]
01522e88 +0150 cmp     w1, w16
01522e8c +0154 b.eq    #0x1522f40
01522e90 +0158 ldur    w1, [x0, #0x2b]
01522e94 +015c add     x1, x1, x28, lsl #32
01522e98 +0160 add     x16, x27, #0x14, lsl #12
01522e9c +0164 ldr     x16, [x16, #0x78]
01522ea0 +0168 cmp     w1, w16
01522ea4 +016c b.ne    #0x1522f40
01522ea8 +0170 ldr     x0, [x26, #0x78]
01522eac +0174 ldr     x0, [x0, #0x2700]
01522eb0 +0178 ldr     x16, [x27, #0x40]
01522eb4 +017c cmp     w0, w16
01522eb8 +0180 b.ne    #0x1522ec8
01522ebc +0184 add     x2, x27, #0x10, lsl #12
01522ec0 +0188 ldr     x2, [x2, #0x830]
01522ec4 +018c bl      #0x1b563d0 => (None, None)
01522ec8 +0190 mov     x3, x0
01522ecc +0194 ldur    x0, [x29, #-8]
01522ed0 +0198 stur    x3, [x29, #-0x10]
01522ed4 +019c ldur    w1, [x0, #0x2f]
01522ed8 +01a0 add     x1, x1, x28, lsl #32
01522edc +01a4 add     x16, x27, #0x14, lsl #12
01522ee0 +01a8 ldr     x16, [x16, #0x70]
01522ee4 +01ac cmp     w1, w16
01522ee8 +01b0 add     x16, x22, #0x20
01522eec +01b4 add     x17, x22, #0x30
01522ef0 +01b8 csel    x2, x16, x17, eq
01522ef4 +01bc mov     x1, x3
01522ef8 +01c0 bl      #0x95ed70 => ('SystemPropertyProvider.getNavigationBarHeight', 9825648)
01522efc +01c4 mov     v1.16b, v0.16b
01522f00 +01c8 fmov    d0, #12.00000000
01522f04 +01cc fsub    d2, d1, d0
01522f08 +01d0 ldur    x0, [x29, #-0x10]
01522f0c +01d4 ldur    d0, [x0, #0xf]
01522f10 +01d8 fsub    d1, d2, d0
01522f14 +01dc ldp     x0, x1, [x26, #0x60]
01522f18 +01e0 add     x0, x0, #0x10
01522f1c +01e4 cmp     x1, x0
01522f20 +01e8 b.ls    #0x1522f8c
01522f24 +01ec str     x0, [x26, #0x60]
01522f28 +01f0 sub     x0, x0, #0xf
01522f2c +01f4 mov     x1, #0xe15c
01522f30 +01f8 movk    x1, #3, lsl #16
01522f34 +01fc stur    x1, [x0, #-1]
01522f38 +0200 stur    d1, [x0, #7]
01522f3c +0204 b       #0x1522f44
01522f40 +0208 mov     x0, #0
01522f44 +020c ldur    d0, [x29, #-0x20]
01522f48 +0210 ldp     x1, x2, [x26, #0x60]
01522f4c +0214 add     x1, x1, #0x10
01522f50 +0218 cmp     x2, x1
01522f54 +021c b.ls    #0x1522f9c
01522f58 +0220 str     x1, [x26, #0x60]
01522f5c +0224 sub     x1, x1, #0xf
01522f60 +0228 mov     x2, #0xe15c
01522f64 +022c movk    x2, #3, lsl #16
01522f68 +0230 stur    x2, [x1, #-1]
01522f6c +0234 stur    d0, [x1, #7]
01522f70 +0238 stp     x0, x1, [x15]
01522f74 +023c bl      #0x18a3fa4 => ('double.+', 25837476)
01522f78 +0240 ldur    d0, [x0, #7]
01522f7c +0244 mov     x15, x29
01522f80 +0248 ldp     x29, x30, [x15], #0x10
01522f84 +024c ret     
01522f88 +0250 bl      #0x1b58a18 => (None, None)
01522f8c +0254 str     q1, [x15, #-0x10]!
01522f90 +0258 bl      #0x1b581e0 => (None, None)
01522f94 +025c ldr     q1, [x15], #0x10
01522f98 +0260 b       #0x1522f38
01522f9c +0264 str     q0, [x15, #-0x10]!
01522fa0 +0268 str     x0, [x15, #-8]!
01522fa4 +026c bl      #0x1b581e0 => (None, None)
01522fa8 +0270 mov     x1, x0
01522fac +0274 ldr     x0, [x15], #8
01522fb0 +0278 ldr     q0, [x15], #0x10
01522fb4 +027c b       #0x1522f6c