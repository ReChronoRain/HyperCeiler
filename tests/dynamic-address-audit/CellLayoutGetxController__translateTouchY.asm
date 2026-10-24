00b1c88c +0000 stp     x29, x30, [x15, #-0x10]!
00b1c890 +0004 mov     x29, x15
00b1c894 +0008 sub     x15, x15, #0x40
00b1c898 +000c stur    x1, [x29, #-0x10]
00b1c89c +0010 stur    x2, [x29, #-0x18]
00b1c8a0 +0014 stur    d1, [x29, #-0x30]
00b1c8a4 +0018 stur    d2, [x29, #-0x38]
00b1c8a8 +001c ldp     x0, x3, [x26, #0x60]
00b1c8ac +0020 add     x0, x0, #0x10
00b1c8b0 +0024 cmp     x3, x0
00b1c8b4 +0028 b.ls    #0xb1cfcc
00b1c8b8 +002c str     x0, [x26, #0x60]
00b1c8bc +0030 sub     x0, x0, #0xf
00b1c8c0 +0034 mov     x3, #0xe15c
00b1c8c4 +0038 movk    x3, #3, lsl #16
00b1c8c8 +003c stur    x3, [x0, #-1]
00b1c8cc +0040 stur    d0, [x0, #7]
00b1c8d0 +0044 stur    x0, [x29, #-8]
00b1c8d4 +0048 mov     x1, #0xc
00b1c8d8 +004c bl      #0x1b571f8 => (None, None)
00b1c8dc +0050 mov     x1, x0
00b1c8e0 +0054 ldur    x0, [x29, #-8]
00b1c8e4 +0058 stur    x1, [x29, #-0x20]
00b1c8e8 +005c stur    w0, [x1, #0xf]
00b1c8ec +0060 ldur    d0, [x29, #-0x30]
00b1c8f0 +0064 ldp     x0, x2, [x26, #0x60]
00b1c8f4 +0068 add     x0, x0, #0x10
00b1c8f8 +006c cmp     x2, x0
00b1c8fc +0070 b.ls    #0xb1cfec
00b1c900 +0074 str     x0, [x26, #0x60]
00b1c904 +0078 sub     x0, x0, #0xf
00b1c908 +007c mov     x2, #0xe15c
00b1c90c +0080 movk    x2, #3, lsl #16
00b1c910 +0084 stur    x2, [x0, #-1]
00b1c914 +0088 stur    d0, [x0, #7]
00b1c918 +008c stur    w0, [x1, #0x13]
00b1c91c +0090 ldur    d0, [x29, #-0x38]
00b1c920 +0094 ldp     x0, x2, [x26, #0x60]
00b1c924 +0098 add     x0, x0, #0x10
00b1c928 +009c cmp     x2, x0
00b1c92c +00a0 b.ls    #0xb1d004
00b1c930 +00a4 str     x0, [x26, #0x60]
00b1c934 +00a8 sub     x0, x0, #0xf
00b1c938 +00ac mov     x2, #0xe15c
00b1c93c +00b0 movk    x2, #3, lsl #16
00b1c940 +00b4 stur    x2, [x0, #-1]
00b1c944 +00b8 stur    d0, [x0, #7]
00b1c948 +00bc stur    w0, [x1, #0x17]
00b1c94c +00c0 ldr     x0, [x27, #0x18f0]
00b1c950 +00c4 stur    w0, [x1, #0x1b]
00b1c954 +00c8 ldr     x0, [x26, #0x78]
00b1c958 +00cc ldr     x0, [x0, #0x2710]
00b1c95c +00d0 cmp     w0, w22
00b1c960 +00d4 b.ne    #0xb1c99c
00b1c964 +00d8 ldr     x0, [x26, #0x78]
00b1c968 +00dc ldr     x0, [x0, #0x1cd0]
00b1c96c +00e0 ldr     x16, [x27, #0x40]
00b1c970 +00e4 cmp     w0, w16
00b1c974 +00e8 b.ne    #0xb1c980
00b1c978 +00ec ldr     x2, [x27, #0x7800]
00b1c97c +00f0 bl      #0x1b563d0 => (None, None)
00b1c980 +00f4 add     x16, x27, #8, lsl #12
00b1c984 +00f8 ldr     x16, [x16, #0xfc0]
00b1c988 +00fc str     x16, [x15]
00b1c98c +0100 ldr     x4, [x27, #0x50]
00b1c990 +0104 bl      #0xb1f9a8 => ('Inst|find', 11663784)
00b1c994 +0108 mov     x1, x0
00b1c998 +010c b       #0xb1c9a0
00b1c99c +0110 mov     x1, x0
00b1c9a0 +0114 bl      #0xb1a278 => ('GridController.currentConfig', 11641464)
00b1c9a4 +0118 ldur    d0, [x0, #0x33]
00b1c9a8 +011c stur    d0, [x29, #-0x30]
00b1c9ac +0120 ldr     x0, [x26, #0x78]
00b1c9b0 +0124 ldr     x0, [x0, #0x2710]
00b1c9b4 +0128 cmp     w0, w22
00b1c9b8 +012c b.ne    #0xb1c9f4
00b1c9bc +0130 ldr     x0, [x26, #0x78]
00b1c9c0 +0134 ldr     x0, [x0, #0x1cd0]
00b1c9c4 +0138 ldr     x16, [x27, #0x40]
00b1c9c8 +013c cmp     w0, w16
00b1c9cc +0140 b.ne    #0xb1c9d8
00b1c9d0 +0144 ldr     x2, [x27, #0x7800]
00b1c9d4 +0148 bl      #0x1b563d0 => (None, None)
00b1c9d8 +014c add     x16, x27, #8, lsl #12
00b1c9dc +0150 ldr     x16, [x16, #0xfc0]
00b1c9e0 +0154 str     x16, [x15]
00b1c9e4 +0158 ldr     x4, [x27, #0x50]
00b1c9e8 +015c bl      #0xb1f9a8 => ('Inst|find', 11663784)
00b1c9ec +0160 mov     x1, x0
00b1c9f0 +0164 b       #0xb1c9f8
00b1c9f4 +0168 mov     x1, x0
00b1c9f8 +016c ldur    x2, [x29, #-0x20]
00b1c9fc +0170 ldur    d0, [x29, #-0x30]
00b1ca00 +0174 bl      #0xb1a278 => ('GridController.currentConfig', 11641464)
00b1ca04 +0178 ldur    x1, [x0, #0x23]
00b1ca08 +017c scvtf   d0, x1
00b1ca0c +0180 ldur    d1, [x29, #-0x30]
00b1ca10 +0184 fmul    d2, d1, d0
00b1ca14 +0188 ldp     x0, x1, [x26, #0x60]
00b1ca18 +018c add     x0, x0, #0x10
00b1ca1c +0190 cmp     x1, x0
00b1ca20 +0194 b.ls    #0xb1d01c
00b1ca24 +0198 str     x0, [x26, #0x60]
00b1ca28 +019c sub     x0, x0, #0xf
00b1ca2c +01a0 mov     x1, #0xe15c
00b1ca30 +01a4 movk    x1, #3, lsl #16
00b1ca34 +01a8 stur    x1, [x0, #-1]
00b1ca38 +01ac stur    d2, [x0, #7]
00b1ca3c +01b0 ldur    x2, [x29, #-0x20]
00b1ca40 +01b4 stur    w0, [x2, #0x1f]
00b1ca44 +01b8 ldurb   w16, [x2, #-1]
00b1ca48 +01bc ldurb   w17, [x0, #-1]
00b1ca4c +01c0 and     x16, x17, x16, lsr #2
00b1ca50 +01c4 tst     x16, x28, lsr #32
00b1ca54 +01c8 b.eq    #0xb1ca5c
00b1ca58 +01cc bl      #0x1b56998 => (None, None)
00b1ca5c +01d0 ldr     x0, [x26, #0x78]
00b1ca60 +01d4 ldr     x0, [x0, #0x26f0]
00b1ca64 +01d8 ldr     x16, [x27, #0x40]
00b1ca68 +01dc cmp     w0, w16
00b1ca6c +01e0 b.ne    #0xb1ca78
00b1ca70 +01e4 ldr     x2, [x27, #0x7818]
00b1ca74 +01e8 bl      #0x1b563d0 => (None, None)
00b1ca78 +01ec mov     x1, x0
00b1ca7c +01f0 bl      #0x8a5438 => ('FoldDeviceManager.isFoldDualPageMode', 9065528)
00b1ca80 +01f4 tbnz    w0, #4, #0xb1cb88
00b1ca84 +01f8 ldr     x0, [x26, #0x78]
00b1ca88 +01fc ldr     x0, [x0, #0x2710]
00b1ca8c +0200 cmp     w0, w22
00b1ca90 +0204 b.ne    #0xb1cac4
00b1ca94 +0208 ldr     x0, [x26, #0x78]
00b1ca98 +020c ldr     x0, [x0, #0x1cd0]
00b1ca9c +0210 ldr     x16, [x27, #0x40]
00b1caa0 +0214 cmp     w0, w16
00b1caa4 +0218 b.ne    #0xb1cab0
00b1caa8 +021c ldr     x2, [x27, #0x7800]
00b1caac +0220 bl      #0x1b563d0 => (None, None)
00b1cab0 +0224 add     x16, x27, #8, lsl #12
00b1cab4 +0228 ldr     x16, [x16, #0xfc0]
00b1cab8 +022c str     x16, [x15]
00b1cabc +0230 ldr     x4, [x27, #0x50]
00b1cac0 +0234 bl      #0xb1f9a8 => ('Inst|find', 11663784)
00b1cac4 +0238 ldur    x2, [x29, #-0x20]
00b1cac8 +023c ldur    w1, [x0, #0xf3]
00b1cacc +0240 add     x1, x1, x28, lsl #32
00b1cad0 +0244 bl      #0x17d148c => ('RxObjectMixin.value', 24974476)
00b1cad4 +0248 ldur    x2, [x29, #-0x20]
00b1cad8 +024c ldur    w1, [x2, #0x1b]
00b1cadc +0250 add     x1, x1, x28, lsl #32
00b1cae0 +0254 ldur    d0, [x0, #7]
00b1cae4 +0258 ldur    d1, [x1, #7]
00b1cae8 +025c fadd    d2, d1, d0
00b1caec +0260 ldp     x0, x1, [x26, #0x60]
00b1caf0 +0264 add     x0, x0, #0x10
00b1caf4 +0268 cmp     x1, x0
00b1caf8 +026c b.ls    #0xb1d02c
00b1cafc +0270 str     x0, [x26, #0x60]
00b1cb00 +0274 sub     x0, x0, #0xf
00b1cb04 +0278 mov     x1, #0xe15c
00b1cb08 +027c movk    x1, #3, lsl #16
00b1cb0c +0280 stur    x1, [x0, #-1]
00b1cb10 +0284 stur    d2, [x0, #7]
00b1cb14 +0288 stur    w0, [x2, #0x1b]
00b1cb18 +028c ldurb   w16, [x2, #-1]
00b1cb1c +0290 ldurb   w17, [x0, #-1]
00b1cb20 +0294 and     x16, x17, x16, lsr #2
00b1cb24 +0298 tst     x16, x28, lsr #32
00b1cb28 +029c b.eq    #0xb1cb30
00b1cb2c +02a0 bl      #0x1b56998 => (None, None)
00b1cb30 +02a4 ldur    w0, [x2, #0x1f]
00b1cb34 +02a8 add     x0, x0, x28, lsl #32
00b1cb38 +02ac ldur    d1, [x0, #7]
00b1cb3c +02b0 fadd    d2, d1, d0
00b1cb40 +02b4 ldp     x0, x1, [x26, #0x60]
00b1cb44 +02b8 add     x0, x0, #0x10
00b1cb48 +02bc cmp     x1, x0
00b1cb4c +02c0 b.ls    #0xb1d044
00b1cb50 +02c4 str     x0, [x26, #0x60]
00b1cb54 +02c8 sub     x0, x0, #0xf
00b1cb58 +02cc mov     x1, #0xe15c
00b1cb5c +02d0 movk    x1, #3, lsl #16
00b1cb60 +02d4 stur    x1, [x0, #-1]
00b1cb64 +02d8 stur    d2, [x0, #7]
00b1cb68 +02dc stur    w0, [x2, #0x1f]
00b1cb6c +02e0 ldurb   w16, [x2, #-1]
00b1cb70 +02e4 ldurb   w17, [x0, #-1]
00b1cb74 +02e8 and     x16, x17, x16, lsr #2
00b1cb78 +02ec tst     x16, x28, lsr #32
00b1cb7c +02f0 b.eq    #0xb1cb84
00b1cb80 +02f4 bl      #0x1b56998 => (None, None)
00b1cb84 +02f8 b       #0xb1cb90
00b1cb88 +02fc ldur    x2, [x29, #-0x20]
00b1cb8c +0300 eor     v0.16b, v0.16b, v0.16b
00b1cb90 +0304 stur    d0, [x29, #-0x30]
00b1cb94 +0308 ldr     x0, [x26, #0x78]
00b1cb98 +030c ldr     x0, [x0, #0x2490]
00b1cb9c +0310 ldr     x16, [x27, #0x40]
00b1cba0 +0314 cmp     w0, w16
00b1cba4 +0318 b.ne    #0xb1cbb0
00b1cba8 +031c ldr     x2, [x27, #0x60]
00b1cbac +0320 bl      #0x1b563d0 => (None, None)
00b1cbb0 +0324 ldur    x2, [x29, #-0x20]
00b1cbb4 +0328 add     x1, x27, #0x61, lsl #12
00b1cbb8 +032c ldr     x1, [x1, #0x678]
00b1cbbc +0330 stur    x0, [x29, #-8]
00b1cbc0 +0334 bl      #0x1b575bc => (None, None)
00b1cbc4 +0338 ldur    x1, [x29, #-8]
00b1cbc8 +033c mov     x2, x0
00b1cbcc +0340 ldr     x4, [x27, #0x70]
00b1cbd0 +0344 bl      #0x89c194 => ('_LoggerImpl.d', 9027988)
00b1cbd4 +0348 ldur    x2, [x29, #-0x20]
00b1cbd8 +034c ldur    w0, [x2, #0x1b]
00b1cbdc +0350 add     x0, x0, x28, lsl #32
00b1cbe0 +0354 ldur    d0, [x0, #7]
00b1cbe4 +0358 ldur    d3, [x29, #-0x30]
00b1cbe8 +035c fsub    d1, d0, d3
00b1cbec +0360 ldur    w0, [x2, #0xf]
00b1cbf0 +0364 add     x0, x0, x28, lsl #32
00b1cbf4 +0368 ldur    w1, [x2, #0x13]
00b1cbf8 +036c add     x1, x1, x28, lsl #32
00b1cbfc +0370 ldur    d0, [x0, #7]
00b1cc00 +0374 ldur    d2, [x1, #7]
00b1cc04 +0378 ldur    x1, [x29, #-0x10]
00b1cc08 +037c mov     v31.16b, v0.16b
00b1cc0c +0380 mov     v0.16b, v1.16b
00b1cc10 +0384 mov     v1.16b, v31.16b
00b1cc14 +0388 bl      #0xb1d64c => ('CellLayoutGetxController._getScaledY', 11654732)
00b1cc18 +038c ldur    d3, [x29, #-0x30]
00b1cc1c +0390 fadd    d1, d0, d3
00b1cc20 +0394 ldp     x0, x1, [x26, #0x60]
00b1cc24 +0398 add     x0, x0, #0x10
00b1cc28 +039c cmp     x1, x0
00b1cc2c +03a0 b.ls    #0xb1d05c
00b1cc30 +03a4 str     x0, [x26, #0x60]
00b1cc34 +03a8 sub     x0, x0, #0xf
00b1cc38 +03ac mov     x1, #0xe15c
00b1cc3c +03b0 movk    x1, #3, lsl #16
00b1cc40 +03b4 stur    x1, [x0, #-1]
00b1cc44 +03b8 stur    d1, [x0, #7]
00b1cc48 +03bc ldur    x2, [x29, #-0x20]
00b1cc4c +03c0 stur    w0, [x2, #0x23]
00b1cc50 +03c4 ldurb   w16, [x2, #-1]
00b1cc54 +03c8 ldurb   w17, [x0, #-1]
00b1cc58 +03cc and     x16, x17, x16, lsr #2
00b1cc5c +03d0 tst     x16, x28, lsr #32
00b1cc60 +03d4 b.eq    #0xb1cc68
00b1cc64 +03d8 bl      #0x1b56998 => (None, None)
00b1cc68 +03dc ldur    w0, [x2, #0x1f]
00b1cc6c +03e0 add     x0, x0, x28, lsl #32
00b1cc70 +03e4 ldur    d0, [x0, #7]
00b1cc74 +03e8 fsub    d1, d0, d3
00b1cc78 +03ec ldur    w0, [x2, #0xf]
00b1cc7c +03f0 add     x0, x0, x28, lsl #32
00b1cc80 +03f4 ldur    w1, [x2, #0x13]
00b1cc84 +03f8 add     x1, x1, x28, lsl #32
00b1cc88 +03fc ldur    d0, [x0, #7]
00b1cc8c +0400 ldur    d2, [x1, #7]
00b1cc90 +0404 ldur    x1, [x29, #-0x10]
00b1cc94 +0408 mov     v31.16b, v0.16b
00b1cc98 +040c mov     v0.16b, v1.16b
00b1cc9c +0410 mov     v1.16b, v31.16b
00b1cca0 +0414 bl      #0xb1d64c => ('CellLayoutGetxController._getScaledY', 11654732)
00b1cca4 +0418 mov     v1.16b, v0.16b
00b1cca8 +041c ldur    d0, [x29, #-0x30]
00b1ccac +0420 fadd    d2, d1, d0
00b1ccb0 +0424 ldp     x0, x1, [x26, #0x60]
00b1ccb4 +0428 add     x0, x0, #0x10
00b1ccb8 +042c cmp     x1, x0
00b1ccbc +0430 b.ls    #0xb1d06c
00b1ccc0 +0434 str     x0, [x26, #0x60]
00b1ccc4 +0438 sub     x0, x0, #0xf
00b1ccc8 +043c mov     x1, #0xe15c
00b1cccc +0440 movk    x1, #3, lsl #16
00b1ccd0 +0444 stur    x1, [x0, #-1]
00b1ccd4 +0448 stur    d2, [x0, #7]
00b1ccd8 +044c ldur    x3, [x29, #-0x20]
00b1ccdc +0450 stur    w0, [x3, #0x27]
00b1cce0 +0454 ldurb   w16, [x3, #-1]
00b1cce4 +0458 ldurb   w17, [x0, #-1]
00b1cce8 +045c and     x16, x17, x16, lsr #2
00b1ccec +0460 tst     x16, x28, lsr #32
00b1ccf0 +0464 b.eq    #0xb1ccf8
00b1ccf4 +0468 bl      #0x1b569b8 => (None, None)
00b1ccf8 +046c mov     x2, x3
00b1ccfc +0470 add     x1, x27, #0x61, lsl #12
00b1cd00 +0474 ldr     x1, [x1, #0x680]
00b1cd04 +0478 bl      #0x1b575bc => (None, None)
00b1cd08 +047c ldur    x1, [x29, #-8]
00b1cd0c +0480 mov     x2, x0
00b1cd10 +0484 ldr     x4, [x27, #0x70]
00b1cd14 +0488 bl      #0x89c194 => ('_LoggerImpl.d', 9027988)
00b1cd18 +048c ldur    x1, [x29, #-0x10]
00b1cd1c +0490 ldur    x2, [x29, #-0x18]
00b1cd20 +0494 bl      #0xb1d124 => ('CellLayoutGetxController._getCenterPosition', 11653412)
00b1cd24 +0498 mov     x2, x0
00b1cd28 +049c ldur    w0, [x2, #0xb]
00b1cd2c +04a0 sbfx    x1, x0, #1, #0x1f
00b1cd30 +04a4 mov     x0, x1
00b1cd34 +04a8 mov     x1, #1
00b1cd38 +04ac cmp     x1, x0
00b1cd3c +04b0 b.hs    #0xb1d07c
00b1cd40 +04b4 ldur    w0, [x2, #0xf]
00b1cd44 +04b8 add     x0, x0, x28, lsl #32
00b1cd48 +04bc ldur    w1, [x0, #0x13]
00b1cd4c +04c0 add     x1, x1, x28, lsl #32
00b1cd50 +04c4 ldur    x2, [x29, #-0x20]
00b1cd54 +04c8 ldur    w0, [x2, #0x17]
00b1cd58 +04cc add     x0, x0, x28, lsl #32
00b1cd5c +04d0 ldur    d0, [x1, #7]
00b1cd60 +04d4 ldur    d1, [x0, #7]
00b1cd64 +04d8 fsub    d2, d0, d1
00b1cd68 +04dc ldur    w0, [x2, #0x23]
00b1cd6c +04e0 add     x0, x0, x28, lsl #32
00b1cd70 +04e4 ldur    d0, [x0, #7]
00b1cd74 +04e8 fcmp    d0, d2
00b1cd78 +04ec b.lt    #0xb1cd94
00b1cd7c +04f0 ldur    w0, [x2, #0x1b]
00b1cd80 +04f4 add     x0, x0, x28, lsl #32
00b1cd84 +04f8 ldur    d0, [x0, #7]
00b1cd88 +04fc mov     x15, x29
00b1cd8c +0500 ldp     x29, x30, [x15], #0x10
00b1cd90 +0504 ret     
00b1cd94 +0508 ldur    x3, [x29, #-0x18]
00b1cd98 +050c ldur    w0, [x2, #0x1f]
00b1cd9c +0510 add     x0, x0, x28, lsl #32
00b1cda0 +0514 ldur    w1, [x2, #0x1b]
00b1cda4 +0518 add     x1, x1, x28, lsl #32
00b1cda8 +051c ldur    d1, [x0, #7]
00b1cdac +0520 ldur    d3, [x1, #7]
00b1cdb0 +0524 fsub    d4, d1, d3
00b1cdb4 +0528 ldur    w0, [x2, #0x27]
00b1cdb8 +052c add     x0, x0, x28, lsl #32
00b1cdbc +0530 ldur    d1, [x0, #7]
00b1cdc0 +0534 fsub    d5, d1, d0
00b1cdc4 +0538 fdiv    d1, d4, d5
00b1cdc8 +053c ldp     x0, x1, [x26, #0x60]
00b1cdcc +0540 add     x0, x0, #0x10
00b1cdd0 +0544 cmp     x1, x0
00b1cdd4 +0548 b.ls    #0xb1d080
00b1cdd8 +054c str     x0, [x26, #0x60]
00b1cddc +0550 sub     x0, x0, #0xf
00b1cde0 +0554 mov     x1, #0xe15c
00b1cde4 +0558 movk    x1, #3, lsl #16
00b1cde8 +055c stur    x1, [x0, #-1]
00b1cdec +0560 stur    d1, [x0, #7]
00b1cdf0 +0564 stur    w0, [x2, #0x2b]
00b1cdf4 +0568 ldurb   w16, [x2, #-1]
00b1cdf8 +056c ldurb   w17, [x0, #-1]
00b1cdfc +0570 and     x16, x17, x16, lsr #2
00b1ce00 +0574 tst     x16, x28, lsr #32
00b1ce04 +0578 b.eq    #0xb1ce0c
00b1ce08 +057c bl      #0x1b56998 => (None, None)
00b1ce0c +0580 fmul    d4, d1, d0
00b1ce10 +0584 fsub    d0, d3, d4
00b1ce14 +0588 ldp     x0, x1, [x26, #0x60]
00b1ce18 +058c add     x0, x0, #0x10
00b1ce1c +0590 cmp     x1, x0
00b1ce20 +0594 b.ls    #0xb1d0a0
00b1ce24 +0598 str     x0, [x26, #0x60]
00b1ce28 +059c sub     x0, x0, #0xf
00b1ce2c +05a0 mov     x1, #0xe15c
00b1ce30 +05a4 movk    x1, #3, lsl #16
00b1ce34 +05a8 stur    x1, [x0, #-1]
00b1ce38 +05ac stur    d0, [x0, #7]
00b1ce3c +05b0 stur    w0, [x2, #0x2f]
00b1ce40 +05b4 ldurb   w16, [x2, #-1]
00b1ce44 +05b8 ldurb   w17, [x0, #-1]
00b1ce48 +05bc and     x16, x17, x16, lsr #2
00b1ce4c +05c0 tst     x16, x28, lsr #32
00b1ce50 +05c4 b.eq    #0xb1ce58
00b1ce54 +05c8 bl      #0x1b56998 => (None, None)
00b1ce58 +05cc ldp     x0, x1, [x26, #0x60]
00b1ce5c +05d0 add     x0, x0, #0x10
00b1ce60 +05d4 cmp     x1, x0
00b1ce64 +05d8 b.ls    #0xb1d0c0
00b1ce68 +05dc str     x0, [x26, #0x60]
00b1ce6c +05e0 sub     x0, x0, #0xf
00b1ce70 +05e4 mov     x1, #0xe15c
00b1ce74 +05e8 movk    x1, #3, lsl #16
00b1ce78 +05ec stur    x1, [x0, #-1]
00b1ce7c +05f0 stur    d2, [x0, #7]
00b1ce80 +05f4 stur    w0, [x2, #0x33]
00b1ce84 +05f8 ldurb   w16, [x2, #-1]
00b1ce88 +05fc ldurb   w17, [x0, #-1]
00b1ce8c +0600 and     x16, x17, x16, lsr #2
00b1ce90 +0604 tst     x16, x28, lsr #32
00b1ce94 +0608 b.eq    #0xb1ce9c
00b1ce98 +060c bl      #0x1b56998 => (None, None)
00b1ce9c +0610 fmul    d3, d1, d2
00b1cea0 +0614 fadd    d1, d3, d0
00b1cea4 +0618 stur    d1, [x29, #-0x30]
00b1cea8 +061c ldp     x0, x1, [x26, #0x60]
00b1ceac +0620 add     x0, x0, #0x10
00b1ceb0 +0624 cmp     x1, x0
00b1ceb4 +0628 b.ls    #0xb1d0e0
00b1ceb8 +062c str     x0, [x26, #0x60]
00b1cebc +0630 sub     x0, x0, #0xf
00b1cec0 +0634 mov     x1, #0xe15c
00b1cec4 +0638 movk    x1, #3, lsl #16
00b1cec8 +063c stur    x1, [x0, #-1]
00b1cecc +0640 stur    d1, [x0, #7]
00b1ced0 +0644 stur    w0, [x2, #0x37]
00b1ced4 +0648 ldurb   w16, [x2, #-1]
00b1ced8 +064c ldurb   w17, [x0, #-1]
00b1cedc +0650 and     x16, x17, x16, lsr #2
00b1cee0 +0654 tst     x16, x28, lsr #32
00b1cee4 +0658 b.eq    #0xb1ceec
00b1cee8 +065c bl      #0x1b56998 => (None, None)
00b1ceec +0660 mov     x1, x3
00b1cef0 +0664 bl      #0xb1c354 => ('DragObject.touchLocalPosition', 11649876)
00b1cef4 +0668 ldur    x2, [x29, #-0x20]
00b1cef8 +066c ldur    w1, [x2, #0xf]
00b1cefc +0670 add     x1, x1, x28, lsl #32
00b1cf00 +0674 ldur    d0, [x1, #7]
00b1cf04 +0678 mov     x1, x0
00b1cf08 +067c bl      #0xab6740 => ('Offset./', 11233088)
00b1cf0c +0680 mov     x2, x0
00b1cf10 +0684 ldur    x0, [x29, #-0x18]
00b1cf14 +0688 stur    x2, [x29, #-0x28]
00b1cf18 +068c ldur    w1, [x0, #0x17]
00b1cf1c +0690 add     x1, x1, x28, lsl #32
00b1cf20 +0694 ldur    x0, [x29, #-0x20]
00b1cf24 +0698 ldur    w3, [x0, #0xf]
00b1cf28 +069c add     x3, x3, x28, lsl #32
00b1cf2c +06a0 ldur    d0, [x3, #7]
00b1cf30 +06a4 bl      #0x95dafc => ('Size./', 9820924)
00b1cf34 +06a8 ldur    x1, [x29, #-0x10]
00b1cf38 +06ac ldur    d0, [x29, #-0x30]
00b1cf3c +06b0 ldur    x2, [x29, #-0x28]
00b1cf40 +06b4 mov     x3, x0
00b1cf44 +06b8 bl      #0xb1d108 => ('CellLayoutGetxController._getYFromCenter', 11653384)
00b1cf48 +06bc ldp     x0, x1, [x26, #0x60]
00b1cf4c +06c0 add     x0, x0, #0x10
00b1cf50 +06c4 cmp     x1, x0
00b1cf54 +06c8 b.ls    #0xb1d0f8
00b1cf58 +06cc str     x0, [x26, #0x60]
00b1cf5c +06d0 sub     x0, x0, #0xf
00b1cf60 +06d4 mov     x1, #0xe15c
00b1cf64 +06d8 movk    x1, #3, lsl #16
00b1cf68 +06dc stur    x1, [x0, #-1]
00b1cf6c +06e0 stur    d0, [x0, #7]
00b1cf70 +06e4 ldur    x3, [x29, #-0x20]
00b1cf74 +06e8 stur    w0, [x3, #0x3b]
00b1cf78 +06ec ldurb   w16, [x3, #-1]
00b1cf7c +06f0 ldurb   w17, [x0, #-1]
00b1cf80 +06f4 and     x16, x17, x16, lsr #2
00b1cf84 +06f8 tst     x16, x28, lsr #32
00b1cf88 +06fc b.eq    #0xb1cf90
00b1cf8c +0700 bl      #0x1b569b8 => (None, None)
00b1cf90 +0704 mov     x2, x3
00b1cf94 +0708 add     x1, x27, #0x61, lsl #12
00b1cf98 +070c ldr     x1, [x1, #0x688]
00b1cf9c +0710 bl      #0x1b575bc => (None, None)
00b1cfa0 +0714 ldur    x1, [x29, #-8]
00b1cfa4 +0718 mov     x2, x0
00b1cfa8 +071c ldr     x4, [x27, #0x70]
00b1cfac +0720 bl      #0x89c194 => ('_LoggerImpl.d', 9027988)
00b1cfb0 +0724 ldur    x0, [x29, #-0x20]
00b1cfb4 +0728 ldur    w1, [x0, #0x3b]
00b1cfb8 +072c add     x1, x1, x28, lsl #32
00b1cfbc +0730 ldur    d0, [x1, #7]
00b1cfc0 +0734 mov     x15, x29
00b1cfc4 +0738 ldp     x29, x30, [x15], #0x10
00b1cfc8 +073c ret     
00b1cfcc +0740 stp     q1, q2, [x15, #-0x20]!
00b1cfd0 +0744 str     q0, [x15, #-0x10]!
00b1cfd4 +0748 stp     x1, x2, [x15, #-0x10]!
00b1cfd8 +074c bl      #0x1b581e0 => (None, None)
00b1cfdc +0750 ldp     x1, x2, [x15], #0x10
00b1cfe0 +0754 ldr     q0, [x15], #0x10
00b1cfe4 +0758 ldp     q1, q2, [x15], #0x20
00b1cfe8 +075c b       #0xb1c8cc
00b1cfec +0760 str     q0, [x15, #-0x10]!
00b1cff0 +0764 str     x1, [x15, #-8]!
00b1cff4 +0768 bl      #0x1b581e0 => (None, None)
00b1cff8 +076c ldr     x1, [x15], #8
00b1cffc +0770 ldr     q0, [x15], #0x10
00b1d000 +0774 b       #0xb1c914
00b1d004 +0778 str     q0, [x15, #-0x10]!
00b1d008 +077c str     x1, [x15, #-8]!
00b1d00c +0780 bl      #0x1b581e0 => (None, None)
00b1d010 +0784 ldr     x1, [x15], #8
00b1d014 +0788 ldr     q0, [x15], #0x10
00b1d018 +078c b       #0xb1c944
00b1d01c +0790 str     q2, [x15, #-0x10]!
00b1d020 +0794 bl      #0x1b581e0 => (None, None)
00b1d024 +0798 ldr     q2, [x15], #0x10
00b1d028 +079c b       #0xb1ca38
00b1d02c +07a0 stp     q0, q2, [x15, #-0x20]!
00b1d030 +07a4 str     x2, [x15, #-8]!
00b1d034 +07a8 bl      #0x1b581e0 => (None, None)
00b1d038 +07ac ldr     x2, [x15], #8
00b1d03c +07b0 ldp     q0, q2, [x15], #0x20
00b1d040 +07b4 b       #0xb1cb10
00b1d044 +07b8 stp     q0, q2, [x15, #-0x20]!
00b1d048 +07bc str     x2, [x15, #-8]!
00b1d04c +07c0 bl      #0x1b581e0 => (None, None)
00b1d050 +07c4 ldr     x2, [x15], #8
00b1d054 +07c8 ldp     q0, q2, [x15], #0x20
00b1d058 +07cc b       #0xb1cb64
00b1d05c +07d0 stp     q1, q3, [x15, #-0x20]!
00b1d060 +07d4 bl      #0x1b581e0 => (None, None)
00b1d064 +07d8 ldp     q1, q3, [x15], #0x20
00b1d068 +07dc b       #0xb1cc44
00b1d06c +07e0 str     q2, [x15, #-0x10]!
00b1d070 +07e4 bl      #0x1b581e0 => (None, None)
00b1d074 +07e8 ldr     q2, [x15], #0x10
00b1d078 +07ec b       #0xb1ccd4
00b1d07c +07f0 bl      #0x1b588b8 => (None, None)
00b1d080 +07f4 stp     q2, q3, [x15, #-0x20]!
00b1d084 +07f8 stp     q0, q1, [x15, #-0x20]!
00b1d088 +07fc stp     x2, x3, [x15, #-0x10]!
00b1d08c +0800 bl      #0x1b581e0 => (None, None)
00b1d090 +0804 ldp     x2, x3, [x15], #0x10
00b1d094 +0808 ldp     q0, q1, [x15], #0x20
00b1d098 +080c ldp     q2, q3, [x15], #0x20
00b1d09c +0810 b       #0xb1cdec
00b1d0a0 +0814 stp     q1, q2, [x15, #-0x20]!
00b1d0a4 +0818 str     q0, [x15, #-0x10]!
00b1d0a8 +081c stp     x2, x3, [x15, #-0x10]!
00b1d0ac +0820 bl      #0x1b581e0 => (None, None)
00b1d0b0 +0824 ldp     x2, x3, [x15], #0x10
00b1d0b4 +0828 ldr     q0, [x15], #0x10
00b1d0b8 +082c ldp     q1, q2, [x15], #0x20
00b1d0bc +0830 b       #0xb1ce38
00b1d0c0 +0834 stp     q1, q2, [x15, #-0x20]!
00b1d0c4 +0838 str     q0, [x15, #-0x10]!
00b1d0c8 +083c stp     x2, x3, [x15, #-0x10]!
00b1d0cc +0840 bl      #0x1b581e0 => (None, None)
00b1d0d0 +0844 ldp     x2, x3, [x15], #0x10
00b1d0d4 +0848 ldr     q0, [x15], #0x10
00b1d0d8 +084c ldp     q1, q2, [x15], #0x20
00b1d0dc +0850 b       #0xb1ce7c
00b1d0e0 +0854 str     q1, [x15, #-0x10]!
00b1d0e4 +0858 stp     x2, x3, [x15, #-0x10]!
00b1d0e8 +085c bl      #0x1b581e0 => (None, None)
00b1d0ec +0860 ldp     x2, x3, [x15], #0x10
00b1d0f0 +0864 ldr     q1, [x15], #0x10
00b1d0f4 +0868 b       #0xb1cecc
00b1d0f8 +086c str     q0, [x15, #-0x10]!
00b1d0fc +0870 bl      #0x1b581e0 => (None, None)
00b1d100 +0874 ldr     q0, [x15], #0x10
00b1d104 +0878 b       #0xb1cf6c