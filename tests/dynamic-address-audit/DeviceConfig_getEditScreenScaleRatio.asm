00a44c6c +0000 stp     x29, x30, [x15, #-0x10]!
00a44c70 +0004 mov     x29, x15
00a44c74 +0008 sub     x15, x15, #0x28
00a44c78 +000c stur    x1, [x29, #-8]
00a44c7c +0010 mov     x1, #4
00a44c80 +0014 bl      #0x1b571f8 => (None, None)
00a44c84 +0018 ldur    x1, [x29, #-8]
00a44c88 +001c stur    x0, [x29, #-0x10]
00a44c8c +0020 stur    w1, [x0, #0xf]
00a44c90 +0024 ldr     x2, [x26, #0x78]
00a44c94 +0028 ldr     x2, [x2, #0x2710]
00a44c98 +002c cmp     w2, w22
00a44c9c +0030 b.ne    #0xa44cd8
00a44ca0 +0034 ldr     x0, [x26, #0x78]
00a44ca4 +0038 ldr     x0, [x0, #0x1cd0]
00a44ca8 +003c ldr     x16, [x27, #0x40]
00a44cac +0040 cmp     w0, w16
00a44cb0 +0044 b.ne    #0xa44cbc
00a44cb4 +0048 ldr     x2, [x27, #0x7800]
00a44cb8 +004c bl      #0x1b563d0 => (None, None)
00a44cbc +0050 add     x16, x27, #8, lsl #12
00a44cc0 +0054 ldr     x16, [x16, #0xfc0]
00a44cc4 +0058 str     x16, [x15]
00a44cc8 +005c ldr     x4, [x27, #0x50]
00a44ccc +0060 bl      #0xb1f9a8 => ('Inst|find', 11663784)
00a44cd0 +0064 mov     x1, x0
00a44cd4 +0068 b       #0xa44cdc
00a44cd8 +006c mov     x1, x2
00a44cdc +0070 ldur    x2, [x29, #-0x10]
00a44ce0 +0074 bl      #0x8e77ac => ('GridController.deviceHeight', 9336748)
00a44ce4 +0078 ldp     x0, x1, [x26, #0x60]
00a44ce8 +007c add     x0, x0, #0x10
00a44cec +0080 cmp     x1, x0
00a44cf0 +0084 b.ls    #0xa44f1c
00a44cf4 +0088 str     x0, [x26, #0x60]
00a44cf8 +008c sub     x0, x0, #0xf
00a44cfc +0090 mov     x1, #0xe15c
00a44d00 +0094 movk    x1, #3, lsl #16
00a44d04 +0098 stur    x1, [x0, #-1]
00a44d08 +009c stur    d0, [x0, #7]
00a44d0c +00a0 ldur    x2, [x29, #-0x10]
00a44d10 +00a4 stur    w0, [x2, #0x13]
00a44d14 +00a8 ldurb   w16, [x2, #-1]
00a44d18 +00ac ldurb   w17, [x0, #-1]
00a44d1c +00b0 and     x16, x17, x16, lsr #2
00a44d20 +00b4 tst     x16, x28, lsr #32
00a44d24 +00b8 b.eq    #0xa44d2c
00a44d28 +00bc bl      #0x1b56998 => (None, None)
00a44d2c +00c0 ldur    x1, [x29, #-8]
00a44d30 +00c4 bl      #0x9fea90 => ('DeviceConfig.cellHeight', 10480272)
00a44d34 +00c8 stur    d0, [x29, #-0x18]
00a44d38 +00cc ldr     x0, [x26, #0x78]
00a44d3c +00d0 ldr     x0, [x0, #0x2710]
00a44d40 +00d4 cmp     w0, w22
00a44d44 +00d8 b.ne    #0xa44d80
00a44d48 +00dc ldr     x0, [x26, #0x78]
00a44d4c +00e0 ldr     x0, [x0, #0x1cd0]
00a44d50 +00e4 ldr     x16, [x27, #0x40]
00a44d54 +00e8 cmp     w0, w16
00a44d58 +00ec b.ne    #0xa44d64
00a44d5c +00f0 ldr     x2, [x27, #0x7800]
00a44d60 +00f4 bl      #0x1b563d0 => (None, None)
00a44d64 +00f8 add     x16, x27, #8, lsl #12
00a44d68 +00fc ldr     x16, [x16, #0xfc0]
00a44d6c +0100 str     x16, [x15]
00a44d70 +0104 ldr     x4, [x27, #0x50]
00a44d74 +0108 bl      #0xb1f9a8 => ('Inst|find', 11663784)
00a44d78 +010c mov     x1, x0
00a44d7c +0110 b       #0xa44d84
00a44d80 +0114 mov     x1, x0
00a44d84 +0118 ldur    x0, [x29, #-8]
00a44d88 +011c ldur    x2, [x29, #-0x10]
00a44d8c +0120 ldur    d0, [x29, #-0x18]
00a44d90 +0124 bl      #0xb1a278 => ('GridController.currentConfig', 11641464)
00a44d94 +0128 ldur    x1, [x0, #0x23]
00a44d98 +012c scvtf   d0, x1
00a44d9c +0130 ldur    d1, [x29, #-0x18]
00a44da0 +0134 fmul    d2, d1, d0
00a44da4 +0138 ldp     x0, x1, [x26, #0x60]
00a44da8 +013c add     x0, x0, #0x10
00a44dac +0140 cmp     x1, x0
00a44db0 +0144 b.ls    #0xa44f2c
00a44db4 +0148 str     x0, [x26, #0x60]
00a44db8 +014c sub     x0, x0, #0xf
00a44dbc +0150 mov     x1, #0xe15c
00a44dc0 +0154 movk    x1, #3, lsl #16
00a44dc4 +0158 stur    x1, [x0, #-1]
00a44dc8 +015c stur    d2, [x0, #7]
00a44dcc +0160 ldur    x2, [x29, #-0x10]
00a44dd0 +0164 stur    w0, [x2, #0x17]
00a44dd4 +0168 ldurb   w16, [x2, #-1]
00a44dd8 +016c ldurb   w17, [x0, #-1]
00a44ddc +0170 and     x16, x17, x16, lsr #2
00a44de0 +0174 tst     x16, x28, lsr #32
00a44de4 +0178 b.eq    #0xa44dec
00a44de8 +017c bl      #0x1b56998 => (None, None)
00a44dec +0180 ldur    w0, [x2, #0x13]
00a44df0 +0184 add     x0, x0, x28, lsl #32
00a44df4 +0188 ldur    x3, [x29, #-8]
00a44df8 +018c ldur    w1, [x3, #0x23]
00a44dfc +0190 add     x1, x1, x28, lsl #32
00a44e00 +0194 ldr     x16, [x27, #0x40]
00a44e04 +0198 cmp     w1, w16
00a44e08 +019c b.eq    #0xa44f3c
00a44e0c +01a0 ldur    d0, [x1, #0x1f]
00a44e10 +01a4 ldur    d1, [x3, #0x83]
00a44e14 +01a8 fadd    d2, d0, d1
00a44e18 +01ac ldur    d0, [x0, #7]
00a44e1c +01b0 fsub    d1, d0, d2
00a44e20 +01b4 mov     x1, x3
00a44e24 +01b8 stur    d1, [x29, #-0x18]
00a44e28 +01bc bl      #0x95ebb0 => ('DeviceConfig.getEditingEntryThumbnailOccupyHeight', 9825200)
00a44e2c +01c0 mov     v1.16b, v0.16b
00a44e30 +01c4 ldur    d0, [x29, #-0x18]
00a44e34 +01c8 fsub    d2, d0, d1
00a44e38 +01cc ldur    x1, [x29, #-8]
00a44e3c +01d0 stur    d2, [x29, #-0x20]
00a44e40 +01d4 bl      #0xa44f90 => ('DeviceConfig.workspaceIndicatorHeight', 10768272)
00a44e44 +01d8 mov     v1.16b, v0.16b
00a44e48 +01dc ldur    d0, [x29, #-0x20]
00a44e4c +01e0 fsub    d2, d0, d1
00a44e50 +01e4 ldur    x1, [x29, #-8]
00a44e54 +01e8 stur    d2, [x29, #-0x18]
00a44e58 +01ec bl      #0xa44f60 => ('DeviceConfig.getBoardVerPadding', 10768224)
00a44e5c +01f0 mov     v1.16b, v0.16b
00a44e60 +01f4 fmov    d0, #2.00000000
00a44e64 +01f8 fmul    d2, d1, d0
00a44e68 +01fc ldur    d0, [x29, #-0x18]
00a44e6c +0200 fsub    d1, d0, d2
00a44e70 +0204 ldur    x2, [x29, #-0x10]
00a44e74 +0208 ldur    w0, [x2, #0x17]
00a44e78 +020c add     x0, x0, x28, lsl #32
00a44e7c +0210 ldur    d0, [x0, #7]
00a44e80 +0214 fdiv    d2, d1, d0
00a44e84 +0218 stur    d2, [x29, #-0x18]
00a44e88 +021c ldp     x0, x1, [x26, #0x60]
00a44e8c +0220 add     x0, x0, #0x10
00a44e90 +0224 cmp     x1, x0
00a44e94 +0228 b.ls    #0xa44f48
00a44e98 +022c str     x0, [x26, #0x60]
00a44e9c +0230 sub     x0, x0, #0xf
00a44ea0 +0234 mov     x1, #0xe15c
00a44ea4 +0238 movk    x1, #3, lsl #16
00a44ea8 +023c stur    x1, [x0, #-1]
00a44eac +0240 stur    d2, [x0, #7]
00a44eb0 +0244 stur    w0, [x2, #0x1b]
00a44eb4 +0248 ldurb   w16, [x2, #-1]
00a44eb8 +024c ldurb   w17, [x0, #-1]
00a44ebc +0250 and     x16, x17, x16, lsr #2
00a44ec0 +0254 tst     x16, x28, lsr #32
00a44ec4 +0258 b.eq    #0xa44ecc
00a44ec8 +025c bl      #0x1b56998 => (None, None)
00a44ecc +0260 ldr     x0, [x26, #0x78]
00a44ed0 +0264 ldr     x0, [x0, #0x2490]
00a44ed4 +0268 ldr     x16, [x27, #0x40]
00a44ed8 +026c cmp     w0, w16
00a44edc +0270 b.ne    #0xa44ee8
00a44ee0 +0274 ldr     x2, [x27, #0x60]
00a44ee4 +0278 bl      #0x1b563d0 => (None, None)
00a44ee8 +027c ldur    x2, [x29, #-0x10]
00a44eec +0280 add     x1, x27, #0x2f, lsl #12
00a44ef0 +0284 ldr     x1, [x1, #0x300]
00a44ef4 +0288 stur    x0, [x29, #-8]
00a44ef8 +028c bl      #0x1b575bc => (None, None)
00a44efc +0290 ldur    x1, [x29, #-8]
00a44f00 +0294 mov     x2, x0
00a44f04 +0298 ldr     x4, [x27, #0x70]
00a44f08 +029c bl      #0x89c194 => ('_LoggerImpl.d', 9027988)
00a44f0c +02a0 ldur    d0, [x29, #-0x18]
00a44f10 +02a4 mov     x15, x29
00a44f14 +02a8 ldp     x29, x30, [x15], #0x10
00a44f18 +02ac ret     
00a44f1c +02b0 str     q0, [x15, #-0x10]!
00a44f20 +02b4 bl      #0x1b581e0 => (None, None)
00a44f24 +02b8 ldr     q0, [x15], #0x10
00a44f28 +02bc b       #0xa44d08
00a44f2c +02c0 str     q2, [x15, #-0x10]!
00a44f30 +02c4 bl      #0x1b581e0 => (None, None)
00a44f34 +02c8 ldr     q2, [x15], #0x10
00a44f38 +02cc b       #0xa44dc8
00a44f3c +02d0 add     x9, x27, #0xf, lsl #12
00a44f40 +02d4 ldr     x9, [x9, #0x7b0]
00a44f44 +02d8 bl      #0x1b58ca0 => (None, None)
00a44f48 +02dc str     q2, [x15, #-0x10]!
00a44f4c +02e0 str     x2, [x15, #-8]!
00a44f50 +02e4 bl      #0x1b581e0 => (None, None)
00a44f54 +02e8 ldr     x2, [x15], #8
00a44f58 +02ec ldr     q2, [x15], #0x10
00a44f5c +02f0 b       #0xa44eac