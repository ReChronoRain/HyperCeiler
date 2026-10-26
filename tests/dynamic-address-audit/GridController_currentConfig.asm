00b1a278 +0000 stp     x29, x30, [x15, #-0x10]!
00b1a27c +0004 mov     x29, x15
00b1a280 +0008 sub     x15, x15, #8
00b1a284 +000c ldr     x0, [x26, #0x78]
00b1a288 +0010 ldr     x0, [x0, #0x2710]
00b1a28c +0014 cmp     w0, w22
00b1a290 +0018 b.ne    #0xb1a2c4
00b1a294 +001c ldr     x0, [x26, #0x78]
00b1a298 +0020 ldr     x0, [x0, #0x1cd0]
00b1a29c +0024 ldr     x16, [x27, #0x40]
00b1a2a0 +0028 cmp     w0, w16
00b1a2a4 +002c b.ne    #0xb1a2b0
00b1a2a8 +0030 ldr     x2, [x27, #0x7800]
00b1a2ac +0034 bl      #0x1b563d0 => (None, None)
00b1a2b0 +0038 add     x16, x27, #8, lsl #12
00b1a2b4 +003c ldr     x16, [x16, #0xfc0]
00b1a2b8 +0040 str     x16, [x15]
00b1a2bc +0044 ldr     x4, [x27, #0x50]
00b1a2c0 +0048 bl      #0xb1f9a8 => ('Inst|find', 11663784)
00b1a2c4 +004c ldur    w1, [x0, #0x2b]
00b1a2c8 +0050 add     x1, x1, x28, lsl #32
00b1a2cc +0054 bl      #0x17d148c => ('RxObjectMixin.value', 24974476)
00b1a2d0 +0058 mov     x15, x29
00b1a2d4 +005c ldp     x29, x30, [x15], #0x10
00b1a2d8 +0060 ret     