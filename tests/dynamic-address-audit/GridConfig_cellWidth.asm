0118aac4 +0000 stp     x29, x30, [x15, #-0x10]!
0118aac8 +0004 mov     x29, x15
0118aacc +0008 sub     x15, x15, #8
0118aad0 +000c ldur    w0, [x1, #0x1f]
0118aad4 +0010 add     x0, x0, x28, lsl #32
0118aad8 +0014 stur    x0, [x29, #-8]
0118aadc +0018 ldur    x1, [x0, #-1]
0118aae0 +001c ubfx    x1, x1, #0xc, #0x14
0118aae4 +0020 cmp     x1, #0x785
0118aae8 +0024 b.ne    #0x118aafc
0118aaec +0028 ldur    d0, [x0, #0x47]
0118aaf0 +002c ldr     x4, [x27, #0x418]
0118aaf4 +0030 bl      #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
0118aaf8 +0034 b       #0x118ab1c
0118aafc +0038 mov     x1, x0
0118ab00 +003c add     x2, x27, #0x27, lsl #12
0118ab04 +0040 ldr     x2, [x2, #0x328]
0118ab08 +0044 bl      #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
0118ab0c +0048 ldur    x0, [x29, #-8]
0118ab10 +004c ldur    d0, [x0, #0x47]
0118ab14 +0050 ldr     x4, [x27, #0x418]
0118ab18 +0054 bl      #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
0118ab1c +0058 mov     x15, x29
0118ab20 +005c ldp     x29, x30, [x15], #0x10
0118ab24 +0060 ret     