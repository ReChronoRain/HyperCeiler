016f77c8 +0000 stp      x29, x30, [x15, #-0x10]!
016f77cc +0004 mov      x29, x15
016f77d0 +0008 sub      x15, x15, #0x70
016f77d4 +000c mov      x0, x1
016f77d8 +0010 stur     x1, [x29, #-0x10]
016f77dc +0014 stur     x2, [x29, #-0x18]
016f77e0 +0018 ldur     w3, [x0, #0x1b]
016f77e4 +001c add      x3, x3, x28, lsl #32
016f77e8 +0020 stur     x3, [x29, #-8]
016f77ec +0024 ldur     d0, [x3, #0x2b]
016f77f0 +0028 stur     d0, [x29, #-0x50]
016f77f4 +002c ldur     d1, [x3, #0x33]
016f77f8 +0030 stur     d1, [x29, #-0x48]
016f77fc +0034 ldur     w1, [x3, #0x6b]
016f7800 +0038 add      x1, x1, x28, lsl #32
016f7804 +003c ldur     d2, [x1, #0x17]
016f7808 +0040 ldur     w4, [x0, #0x1f]
016f780c +0044 add      x4, x4, x28, lsl #32
016f7810 +0048 ldur     w5, [x4, #0x27]
016f7814 +004c add      x5, x5, x28, lsl #32
016f7818 +0050 tbnz     w5, #4, #0x16f7830
016f781c +0054 fmov     d3, #2.00000000
016f7820 +0058 fsub     d4, d1, d2
016f7824 +005c fdiv     d5, d4, d3
016f7828 +0060 mov      v4.16b, v5.16b
016f782c +0064 b        #0x16f7838
016f7830 +0068 fmov     d3, #2.00000000
016f7834 +006c ldur     d4, [x1, #0x1f]
016f7838 +0070 fdiv     d5, d2, d3
016f783c +0074 fadd     d2, d4, d5
016f7840 +0078 fdiv     d4, d1, d3
016f7844 +007c fsub     d3, d2, d4
016f7848 +0080 stur     d3, [x29, #-0x40]
016f784c +0084 ldur     w1, [x0, #0xf]
016f7850 +0088 add      x1, x1, x28, lsl #32
016f7854 +008c bl       #0x1772e58 => ('RxList.value', 24587864)
016f7858 +0090 ldur     x1, [x0, #-1]
016f785c +0094 ubfx     x1, x1, #0xc, #0x14
016f7860 +0098 mov      x16, x0
016f7864 +009c mov      x0, x1
016f7868 +00a0 mov      x1, x16
016f786c +00a4 mov      x17, #0x7b83
016f7870 +00a8 movk     x17, #1, lsl #16
016f7874 +00ac add      x30, x0, x17
016f7878 +00b0 ldr      x30, [x21, x30, lsl #3]
016f787c +00b4 blr      x30
016f7880 +00b8 mov      x2, x0
016f7884 +00bc ldur     x0, [x29, #-8]
016f7888 +00c0 stur     x2, [x29, #-0x28]
016f788c +00c4 ldur     w3, [x0, #0x3b]
016f7890 +00c8 add      x3, x3, x28, lsl #32
016f7894 +00cc stur     x3, [x29, #-0x20]
016f7898 +00d0 ldur     d0, [x3, #7]
016f789c +00d4 stur     d0, [x29, #-0x58]
016f78a0 +00d8 ldur     x4, [x29, #-0x10]
016f78a4 +00dc ldur     d1, [x29, #-0x50]
016f78a8 +00e0 ldur     d2, [x29, #-0x48]
016f78ac +00e4 ldur     d3, [x29, #-0x40]
016f78b0 +00e8 ldur     x0, [x2, #-1]
016f78b4 +00ec ubfx     x0, x0, #0xc, #0x14
016f78b8 +00f0 mov      x1, x2
016f78bc +00f4 add      x30, x0, #0x7d6
016f78c0 +00f8 ldr      x30, [x21, x30, lsl #3]
016f78c4 +00fc blr      x30
016f78c8 +0100 tbnz     w0, #4, #0x16f7c2c
016f78cc +0104 ldur     x2, [x29, #-0x28]
016f78d0 +0108 ldur     x0, [x2, #-1]
016f78d4 +010c ubfx     x0, x0, #0xc, #0x14
016f78d8 +0110 mov      x1, x2
016f78dc +0114 add      x30, x0, #0xcd7
016f78e0 +0118 ldr      x30, [x21, x30, lsl #3]
016f78e4 +011c blr      x30
016f78e8 +0120 stur     x0, [x29, #-8]
016f78ec +0124 ldur     w1, [x0, #0x1f]
016f78f0 +0128 add      x1, x1, x28, lsl #32
016f78f4 +012c cmp      w1, w22
016f78f8 +0130 b.ne     #0x16f7938
016f78fc +0134 ldur     x1, [x0, #0x17]
016f7900 +0138 ldur     x2, [x0, #7]
016f7904 +013c ldur     x3, [x0, #0xf]
016f7908 +0140 bl       #0xb043e8 => ('CellGridCell.generateKey', 11551720)
016f790c +0144 mov      x1, x0
016f7910 +0148 ldur     x3, [x29, #-8]
016f7914 +014c stur     w0, [x3, #0x1f]
016f7918 +0150 ldurb    w16, [x3, #-1]
016f791c +0154 ldurb    w17, [x0, #-1]
016f7920 +0158 and      x16, x17, x16, lsr #2
016f7924 +015c tst      x16, x28, lsr #32
016f7928 +0160 b.eq     #0x16f7930
016f792c +0164 bl       #0x1b569b8 => (None, None)
016f7930 +0168 mov      x4, x1
016f7934 +016c b        #0x16f7940
016f7938 +0170 mov      x3, x0
016f793c +0174 mov      x4, x1
016f7940 +0178 ldur     x0, [x29, #-0x10]
016f7944 +017c stur     x4, [x29, #-0x38]
016f7948 +0180 ldur     w5, [x0, #0xb]
016f794c +0184 add      x5, x5, x28, lsl #32
016f7950 +0188 stur     x5, [x29, #-0x30]
016f7954 +018c cmp      w5, w22
016f7958 +0190 b.eq     #0x16f7d44
016f795c +0194 mov      x1, x5
016f7960 +0198 mov      x2, x4
016f7964 +019c bl       #0x189304c => ('_LinkedHashMapMixin._getValueOrData', 25768012)
016f7968 +01a0 mov      x1, x0
016f796c +01a4 ldur     x0, [x29, #-0x30]
016f7970 +01a8 ldur     w2, [x0, #0xf]
016f7974 +01ac add      x2, x2, x28, lsl #32
016f7978 +01b0 cmp      w2, w1
016f797c +01b4 b.eq     #0x16f7c1c
016f7980 +01b8 cmp      w1, w22
016f7984 +01bc b.eq     #0x16f7c1c
016f7988 +01c0 ldur     x1, [x29, #-0x10]
016f798c +01c4 ldur     d1, [x29, #-0x50]
016f7990 +01c8 ldur     d2, [x29, #-0x48]
016f7994 +01cc ldur     d3, [x29, #-0x40]
016f7998 +01d0 ldur     x0, [x29, #-8]
016f799c +01d4 ldur     d0, [x29, #-0x58]
016f79a0 +01d8 ldur     x2, [x0, #7]
016f79a4 +01dc scvtf    d4, x2
016f79a8 +01e0 fmul     d5, d4, d1
016f79ac +01e4 fadd     d4, d0, d5
016f79b0 +01e8 stur     d4, [x29, #-0x68]
016f79b4 +01ec ldur     x2, [x0, #0xf]
016f79b8 +01f0 scvtf    d5, x2
016f79bc +01f4 fmul     d6, d5, d2
016f79c0 +01f8 fadd     d5, d6, d3
016f79c4 +01fc stur     d5, [x29, #-0x60]
016f79c8 +0200 ldr      x0, [x26, #0x78]
016f79cc +0204 ldr      x0, [x0, #0x2490]
016f79d0 +0208 ldr      x16, [x27, #0x40]
016f79d4 +020c cmp      w0, w16
016f79d8 +0210 b.ne     #0x16f79e4
016f79dc +0214 ldr      x2, [x27, #0x60]
016f79e0 +0218 bl       #0x1b563d0 => (None, None)
016f79e4 +021c mov      x1, x22
016f79e8 +0220 mov      x2, #8
016f79ec +0224 stur     x0, [x29, #-8]
016f79f0 +0228 bl       #0x1b58288 => (None, None)
016f79f4 +022c add      x16, x27, #0xa8, lsl #12
016f79f8 +0230 ldr      x16, [x16, #0x4a8]
016f79fc +0234 stur     w16, [x0, #0xf]
016f7a00 +0238 ldur     d0, [x29, #-0x68]
016f7a04 +023c ldp      x1, x2, [x26, #0x60]
016f7a08 +0240 add      x1, x1, #0x10
016f7a0c +0244 cmp      x2, x1
016f7a10 +0248 b.ls     #0x16f7d48
016f7a14 +024c str      x1, [x26, #0x60]
016f7a18 +0250 sub      x1, x1, #0xf
016f7a1c +0254 mov      x2, #0xe15c
016f7a20 +0258 movk     x2, #3, lsl #16
016f7a24 +025c stur     x2, [x1, #-1]
016f7a28 +0260 stur     d0, [x1, #7]
016f7a2c +0264 stur     w1, [x0, #0x13]
016f7a30 +0268 ldr      x16, [x27, #0x33f0]
016f7a34 +026c stur     w16, [x0, #0x17]
016f7a38 +0270 ldur     d1, [x29, #-0x60]
016f7a3c +0274 ldp      x1, x2, [x26, #0x60]
016f7a40 +0278 add      x1, x1, #0x10
016f7a44 +027c cmp      x2, x1
016f7a48 +0280 b.ls     #0x16f7d64
016f7a4c +0284 str      x1, [x26, #0x60]
016f7a50 +0288 sub      x1, x1, #0xf
016f7a54 +028c mov      x2, #0xe15c
016f7a58 +0290 movk     x2, #3, lsl #16
016f7a5c +0294 stur     x2, [x1, #-1]
016f7a60 +0298 stur     d1, [x1, #7]
016f7a64 +029c stur     w1, [x0, #0x1b]
016f7a68 +02a0 str      x0, [x15]
016f7a6c +02a4 bl       #0x868b8c => ('_StringBase._interpolate', 8817548)
016f7a70 +02a8 ldur     x1, [x29, #-8]
016f7a74 +02ac mov      x5, x0
016f7a78 +02b0 ldr      x2, [x27, #0x78]
016f7a7c +02b4 mov      x3, x22
016f7a80 +02b8 mov      x6, x22
016f7a84 +02bc mov      x7, x22
016f7a88 +02c0 bl       #0x86af9c => ('_LoggerImpl._log', 8826780)
016f7a8c +02c4 bl       #0x191ccf8 => (None, None)
016f7a90 +02c8 ldur     d0, [x29, #-0x50]
016f7a94 +02cc stur     x0, [x29, #-0x30]
016f7a98 +02d0 stur     d0, [x0, #7]
016f7a9c +02d4 stur     d0, [x0, #0xf]
016f7aa0 +02d8 ldur     d1, [x29, #-0x48]
016f7aa4 +02dc stur     d1, [x0, #0x17]
016f7aa8 +02e0 stur     d1, [x0, #0x1f]
016f7aac +02e4 ldur     x3, [x29, #-0x10]
016f7ab0 +02e8 ldur     w4, [x3, #0xb]
016f7ab4 +02ec add      x4, x4, x28, lsl #32
016f7ab8 +02f0 stur     x4, [x29, #-8]
016f7abc +02f4 cmp      w4, w22
016f7ac0 +02f8 b.eq     #0x16f7d80
016f7ac4 +02fc mov      x1, x4
016f7ac8 +0300 ldur     x2, [x29, #-0x38]
016f7acc +0304 bl       #0x189304c => ('_LinkedHashMapMixin._getValueOrData', 25768012)
016f7ad0 +0308 mov      x1, x0
016f7ad4 +030c ldur     x0, [x29, #-8]
016f7ad8 +0310 ldur     w2, [x0, #0xf]
016f7adc +0314 add      x2, x2, x28, lsl #32
016f7ae0 +0318 cmp      w2, w1
016f7ae4 +031c b.ne     #0x16f7af0
016f7ae8 +0320 mov      x4, x22
016f7aec +0324 b        #0x16f7af4
016f7af0 +0328 mov      x4, x1
016f7af4 +032c ldur     x3, [x29, #-0x10]
016f7af8 +0330 ldur     d0, [x29, #-0x68]
016f7afc +0334 ldur     d1, [x29, #-0x60]
016f7b00 +0338 stur     x4, [x29, #-8]
016f7b04 +033c cmp      w4, w22
016f7b08 +0340 b.eq     #0x16f7d84
016f7b0c +0344 ldur     x0, [x4, #-1]
016f7b10 +0348 ubfx     x0, x0, #0xc, #0x14
016f7b14 +034c add      x16, x22, #0x20
016f7b18 +0350 str      x16, [x15]
016f7b1c +0354 mov      x1, x4
016f7b20 +0358 ldur     x2, [x29, #-0x30]
016f7b24 +035c add      x4, x27, #0x47, lsl #12
016f7b28 +0360 ldr      x4, [x4, #0x658]
016f7b2c +0364 mov      x17, #0x2aa2
016f7b30 +0368 movk     x17, #1, lsl #16
016f7b34 +036c add      x30, x0, x17
016f7b38 +0370 ldr      x30, [x21, x30, lsl #3]
016f7b3c +0374 blr      x30
016f7b40 +0378 ldur     x1, [x29, #-8]
016f7b44 +037c bl       #0x8a6bb4 => ('RenderBox.size', 9071540)
016f7b48 +0380 bl       #0x18b600c => (None, None)
016f7b4c +0384 ldur     d0, [x29, #-0x68]
016f7b50 +0388 stur     x0, [x29, #-0x30]
016f7b54 +038c stur     d0, [x0, #7]
016f7b58 +0390 ldur     d0, [x29, #-0x60]
016f7b5c +0394 stur     d0, [x0, #0xf]
016f7b60 +0398 ldur     x3, [x29, #-0x10]
016f7b64 +039c ldur     w4, [x3, #0xb]
016f7b68 +03a0 add      x4, x4, x28, lsl #32
016f7b6c +03a4 stur     x4, [x29, #-8]
016f7b70 +03a8 cmp      w4, w22
016f7b74 +03ac b.eq     #0x16f7d88
016f7b78 +03b0 mov      x1, x4
016f7b7c +03b4 ldur     x2, [x29, #-0x38]
016f7b80 +03b8 bl       #0x189304c => ('_LinkedHashMapMixin._getValueOrData', 25768012)
016f7b84 +03bc mov      x1, x0
016f7b88 +03c0 ldur     x0, [x29, #-8]
016f7b8c +03c4 ldur     w2, [x0, #0xf]
016f7b90 +03c8 add      x2, x2, x28, lsl #32
016f7b94 +03cc cmp      w2, w1
016f7b98 +03d0 b.ne     #0x16f7ba4
016f7b9c +03d4 mov      x0, x22
016f7ba0 +03d8 b        #0x16f7ba8
016f7ba4 +03dc mov      x0, x1
016f7ba8 +03e0 cmp      w0, w22
016f7bac +03e4 b.eq     #0x16f7d8c
016f7bb0 +03e8 ldur     w3, [x0, #7]
016f7bb4 +03ec add      x3, x3, x28, lsl #32
016f7bb8 +03f0 stur     x3, [x29, #-8]
016f7bbc +03f4 cmp      w3, w22
016f7bc0 +03f8 b.eq     #0x16f7d90
016f7bc4 +03fc mov      x0, x3
016f7bc8 +0400 mov      x2, x22
016f7bcc +0404 mov      x1, x22
016f7bd0 +0408 ldur     x4, [x0, #-1]
016f7bd4 +040c ubfx     x4, x4, #0xc, #0x14
016f7bd8 +0410 mov      x17, #0x158b
016f7bdc +0414 cmp      x4, x17
016f7be0 +0418 b.eq     #0x16f7bf8
016f7be4 +041c add      x8, x27, #0x77, lsl #12
016f7be8 +0420 ldr      x8, [x8, #0x240]
016f7bec +0424 add      x3, x27, #0xa8, lsl #12
016f7bf0 +0428 ldr      x3, [x3, #0x4b0]
016f7bf4 +042c bl       #0x1b56180 => (None, None)
016f7bf8 +0430 ldur     x0, [x29, #-0x30]
016f7bfc +0434 ldur     x1, [x29, #-8]
016f7c00 +0438 stur     w0, [x1, #7]
016f7c04 +043c ldurb    w16, [x1, #-1]
016f7c08 +0440 ldurb    w17, [x0, #-1]
016f7c0c +0444 and      x16, x17, x16, lsr #2
016f7c10 +0448 tst      x16, x28, lsr #32
016f7c14 +044c b.eq     #0x16f7c1c
016f7c18 +0450 bl       #0x1b56978 => (None, None)
016f7c1c +0454 ldur     x3, [x29, #-0x20]
016f7c20 +0458 ldur     d0, [x29, #-0x58]
016f7c24 +045c ldur     x2, [x29, #-0x28]
016f7c28 +0460 b        #0x16f78a0
016f7c2c +0464 ldur     x0, [x29, #-0x10]
016f7c30 +0468 ldur     w1, [x0, #0x17]
016f7c34 +046c add      x1, x1, x28, lsl #32
016f7c38 +0470 tbnz     w1, #4, #0x16f7d34
016f7c3c +0474 ldur     x4, [x29, #-0x18]
016f7c40 +0478 ldur     x3, [x29, #-0x20]
016f7c44 +047c ldur     w5, [x0, #0x13]
016f7c48 +0480 add      x5, x5, x28, lsl #32
016f7c4c +0484 stur     x5, [x29, #-8]
016f7c50 +0488 mov      x1, x22
016f7c54 +048c mov      x2, #4
016f7c58 +0490 bl       #0x1b58288 => (None, None)
016f7c5c +0494 mov      x2, x0
016f7c60 +0498 add      x16, x27, #0x4b, lsl #12
016f7c64 +049c ldr      x16, [x16, #0x640]
016f7c68 +04a0 stur     w16, [x2, #0xf]
016f7c6c +04a4 ldur     x0, [x29, #-8]
016f7c70 +04a8 ldur     x3, [x0, #0x27]
016f7c74 +04ac sbfiz    x0, x3, #1, #0x1f
016f7c78 +04b0 cmp      x3, x0, asr #1
016f7c7c +04b4 b.eq     #0x16f7c88
016f7c80 +04b8 bl       #0x1b58510 => (None, None)
016f7c84 +04bc stur     x3, [x0, #7]
016f7c88 +04c0 stur     w0, [x2, #0x13]
016f7c8c +04c4 str      x2, [x15]
016f7c90 +04c8 bl       #0x868b8c => ('_StringBase._interpolate', 8817548)
016f7c94 +04cc mov      x1, x0
016f7c98 +04d0 ldur     x0, [x29, #-0x20]
016f7c9c +04d4 stur     x1, [x29, #-8]
016f7ca0 +04d8 ldur     d0, [x0, #7]
016f7ca4 +04dc stur     d0, [x29, #-0x40]
016f7ca8 +04e0 ldr      x0, [x26, #0x78]
016f7cac +04e4 ldr      x0, [x0, #0x2490]
016f7cb0 +04e8 ldr      x16, [x27, #0x40]
016f7cb4 +04ec cmp      w0, w16
016f7cb8 +04f0 b.ne     #0x16f7cc4
016f7cbc +04f4 ldr      x2, [x27, #0x60]
016f7cc0 +04f8 bl       #0x1b563d0 => (None, None)
016f7cc4 +04fc mov      x1, x0
016f7cc8 +0500 add      x2, x27, #0xa8, lsl #12
016f7ccc +0504 ldr      x2, [x2, #0x4c0]
016f7cd0 +0508 ldr      x4, [x27, #0x70]
016f7cd4 +050c bl       #0x89c194 => ('_LoggerImpl.d', 9027988)
016f7cd8 +0510 ldur     x0, [x29, #-0x18]
016f7cdc +0514 ldur     d0, [x0, #7]
016f7ce0 +0518 stur     d0, [x29, #-0x48]
016f7ce4 +051c bl       #0x191ccf8 => (None, None)
016f7ce8 +0520 ldur     d0, [x29, #-0x48]
016f7cec +0524 stur     d0, [x0, #7]
016f7cf0 +0528 stur     d0, [x0, #0xf]
016f7cf4 +052c ldur     x1, [x29, #-0x18]
016f7cf8 +0530 ldur     d0, [x1, #0xf]
016f7cfc +0534 stur     d0, [x0, #0x17]
016f7d00 +0538 stur     d0, [x0, #0x1f]
016f7d04 +053c ldur     x1, [x29, #-0x10]
016f7d08 +0540 ldur     x2, [x29, #-8]
016f7d0c +0544 mov      x3, x0
016f7d10 +0548 bl       #0x16f5794 => ('MultiChildLayoutDelegate.layoutChild', 24074132)
016f7d14 +054c bl       #0x18b600c => (None, None)
016f7d18 +0550 ldur     d0, [x29, #-0x40]
016f7d1c +0554 stur     d0, [x0, #7]
016f7d20 +0558 stur     xzr, [x0, #0xf]
016f7d24 +055c ldur     x1, [x29, #-0x10]
016f7d28 +0560 ldur     x2, [x29, #-8]
016f7d2c +0564 mov      x3, x0
016f7d30 +0568 bl       #0x16f56b0 => ('MultiChildLayoutDelegate.positionChild', 24073904)
016f7d34 +056c mov      x0, x22
016f7d38 +0570 mov      x15, x29
016f7d3c +0574 ldp      x29, x30, [x15], #0x10
016f7d40 +0578 ret      
016f7d44 +057c bl       #0x1b58a18 => (None, None)
016f7d48 +0580 str      q0, [x15, #-0x10]!
016f7d4c +0584 str      x0, [x15, #-8]!
016f7d50 +0588 bl       #0x1b581e0 => (None, None)
016f7d54 +058c mov      x1, x0
016f7d58 +0590 ldr      x0, [x15], #8
016f7d5c +0594 ldr      q0, [x15], #0x10
016f7d60 +0598 b        #0x16f7a28
016f7d64 +059c stp      q0, q1, [x15, #-0x20]!
016f7d68 +05a0 str      x0, [x15, #-8]!
016f7d6c +05a4 bl       #0x1b581e0 => (None, None)
016f7d70 +05a8 mov      x1, x0
016f7d74 +05ac ldr      x0, [x15], #8
016f7d78 +05b0 ldp      q0, q1, [x15], #0x20
016f7d7c +05b4 b        #0x16f7a60
016f7d80 +05b8 bl       #0x1b58a64 => (None, None)
016f7d84 +05bc bl       #0x1b58a64 => (None, None)
016f7d88 +05c0 bl       #0x1b58a18 => (None, None)
016f7d8c +05c4 bl       #0x1b58a18 => (None, None)
016f7d90 +05c8 bl       #0x1b58a18 => (None, None)