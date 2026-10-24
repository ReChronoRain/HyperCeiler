00a47c50 +0000 stp     x29, x30, [x15, #-0x10]!
00a47c54 +0004 mov     x29, x15
00a47c58 +0008 sub     x15, x15, #8
00a47c5c +000c ldur    w0, [x1, #0x1f]
00a47c60 +0010 add     x0, x0, x28, lsl #32
00a47c64 +0014 stur    x0, [x29, #-8]
00a47c68 +0018 ldur    x1, [x0, #-1]
00a47c6c +001c ubfx    x1, x1, #0xc, #0x14
00a47c70 +0020 cmp     x1, #0x785
00a47c74 +0024 b.ne    #0xa47c88
00a47c78 +0028 ldur    d0, [x0, #0x4f]
00a47c7c +002c ldr     x4, [x27, #0x418]
00a47c80 +0030 bl      #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
00a47c84 +0034 b       #0xa47ca8
00a47c88 +0038 mov     x1, x0
00a47c8c +003c add     x2, x27, #0x27, lsl #12
00a47c90 +0040 ldr     x2, [x2, #0x3a0]
00a47c94 +0044 bl      #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
00a47c98 +0048 ldur    x0, [x29, #-8]
00a47c9c +004c ldur    d0, [x0, #0x4f]
00a47ca0 +0050 ldr     x4, [x27, #0x418]
00a47ca4 +0054 bl      #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
00a47ca8 +0058 mov     x15, x29
00a47cac +005c ldp     x29, x30, [x15], #0x10
00a47cb0 +0060 ret     