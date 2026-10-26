00cc592c +0000 stp      x29, x30, [x15, #-0x10]!
00cc5930 +0004 mov      x29, x15
00cc5934 +0008 sub      x15, x15, #0x58
00cc5938 +000c mov      x3, x1
00cc593c +0010 stur     x1, [x29, #-0x10]
00cc5940 +0014 stur     x2, [x29, #-0x18]
00cc5944 +0018 ldur     w4, [x2, #0x1b]
00cc5948 +001c add      x4, x4, x28, lsl #32
00cc594c +0020 stur     x4, [x29, #-8]
00cc5950 +0024 ldur     x5, [x2, #0x73]
00cc5954 +0028 ldur     w0, [x4, #0xb]
00cc5958 +002c sbfx     x1, x0, #1, #0x1f
00cc595c +0030 mov      x0, x1
00cc5960 +0034 mov      x1, x5
00cc5964 +0038 cmp      x1, x0
00cc5968 +003c b.hs     #0xcc5e10
00cc596c +0040 ldur     w0, [x4, #0xf]
00cc5970 +0044 add      x0, x0, x28, lsl #32
00cc5974 +0048 add      x16, x0, x5, lsl #2
00cc5978 +004c ldur     w1, [x16, #0xf]
00cc597c +0050 add      x1, x1, x28, lsl #32
00cc5980 +0054 ldur     w0, [x1, #0x27]
00cc5984 +0058 add      x0, x0, x28, lsl #32
00cc5988 +005c ldr      x16, [x27, #0x40]
00cc598c +0060 cmp      w0, w16
00cc5990 +0064 b.ne     #0xcc599c
00cc5994 +0068 ldr      x2, [x27, #0x7ce8]
00cc5998 +006c bl       #0x1b5632c => (None, None)
00cc599c +0070 mov      x1, #0x3c
00cc59a0 +0074 tbz      w0, #0, #0xcc59ac
00cc59a4 +0078 ldur     x1, [x0, #-1]
00cc59a8 +007c ubfx     x1, x1, #0xc, #0x14
00cc59ac +0080 cmp      x1, #0x740
00cc59b0 +0084 b.eq     #0xcc59c4
00cc59b4 +0088 mov      x0, x22
00cc59b8 +008c mov      x15, x29
00cc59bc +0090 ldp      x29, x30, [x15], #0x10
00cc59c0 +0094 ret      
00cc59c4 +0098 ldur     x3, [x29, #-0x10]
00cc59c8 +009c ldur     x2, [x29, #-0x18]
00cc59cc +00a0 ldur     x4, [x29, #-8]
00cc59d0 +00a4 ldur     x5, [x2, #0x73]
00cc59d4 +00a8 ldur     w0, [x4, #0xb]
00cc59d8 +00ac sbfx     x1, x0, #1, #0x1f
00cc59dc +00b0 mov      x0, x1
00cc59e0 +00b4 mov      x1, x5
00cc59e4 +00b8 cmp      x1, x0
00cc59e8 +00bc b.hs     #0xcc5e14
00cc59ec +00c0 ldur     w0, [x4, #0xf]
00cc59f0 +00c4 add      x0, x0, x28, lsl #32
00cc59f4 +00c8 add      x16, x0, x5, lsl #2
00cc59f8 +00cc ldur     w1, [x16, #0xf]
00cc59fc +00d0 add      x1, x1, x28, lsl #32
00cc5a00 +00d4 ldur     w0, [x1, #0x27]
00cc5a04 +00d8 add      x0, x0, x28, lsl #32
00cc5a08 +00dc ldr      x16, [x27, #0x40]
00cc5a0c +00e0 cmp      w0, w16
00cc5a10 +00e4 b.ne     #0xcc5a1c
00cc5a14 +00e8 ldr      x2, [x27, #0x7ce8]
00cc5a18 +00ec bl       #0x1b5632c => (None, None)
00cc5a1c +00f0 mov      x3, x0
00cc5a20 +00f4 mov      x2, x22
00cc5a24 +00f8 mov      x1, x22
00cc5a28 +00fc stur     x3, [x29, #-8]
00cc5a2c +0100 mov      x4, #0x3c
00cc5a30 +0104 tbz      w0, #0, #0xcc5a3c
00cc5a34 +0108 ldur     x4, [x0, #-1]
00cc5a38 +010c ubfx     x4, x4, #0xc, #0x14
00cc5a3c +0110 cmp      x4, #0x740
00cc5a40 +0114 b.eq     #0xcc5a54
00cc5a44 +0118 ldr      x8, [x27, #0x7cf0]
00cc5a48 +011c add      x3, x27, #0xaa, lsl #12
00cc5a4c +0120 ldr      x3, [x3, #0x340]
00cc5a50 +0124 bl       #0x195cff0 => (None, None)
00cc5a54 +0128 ldr      x0, [x26, #0x78]
00cc5a58 +012c ldr      x0, [x0, #0x2490]
00cc5a5c +0130 ldr      x16, [x27, #0x40]
00cc5a60 +0134 cmp      w0, w16
00cc5a64 +0138 b.ne     #0xcc5a70
00cc5a68 +013c ldr      x2, [x27, #0x60]
00cc5a6c +0140 bl       #0x1b563d0 => (None, None)
00cc5a70 +0144 mov      x1, x22
00cc5a74 +0148 mov      x2, #0xc
00cc5a78 +014c stur     x0, [x29, #-0x20]
00cc5a7c +0150 bl       #0x1b58288 => (None, None)
00cc5a80 +0154 mov      x2, x0
00cc5a84 +0158 add      x16, x27, #0x60, lsl #12
00cc5a88 +015c ldr      x16, [x16, #0x6f0]
00cc5a8c +0160 stur     w16, [x2, #0xf]
00cc5a90 +0164 ldur     x0, [x29, #-8]
00cc5a94 +0168 ldur     w3, [x0, #7]
00cc5a98 +016c add      x3, x3, x28, lsl #32
00cc5a9c +0170 stur     x3, [x29, #-0x28]
00cc5aa0 +0174 ldur     x4, [x3, #0x2f]
00cc5aa4 +0178 sbfiz    x0, x4, #1, #0x1f
00cc5aa8 +017c cmp      x4, x0, asr #1
00cc5aac +0180 b.eq     #0xcc5ab8
00cc5ab0 +0184 bl       #0x1b58510 => (None, None)
00cc5ab4 +0188 stur     x4, [x0, #7]
00cc5ab8 +018c stur     w0, [x2, #0x13]
00cc5abc +0190 add      x16, x27, #0xe, lsl #12
00cc5ac0 +0194 ldr      x16, [x16, #0x9a0]
00cc5ac4 +0198 stur     w16, [x2, #0x17]
00cc5ac8 +019c ldur     x4, [x3, #0x37]
00cc5acc +01a0 sbfiz    x0, x4, #1, #0x1f
00cc5ad0 +01a4 cmp      x4, x0, asr #1
00cc5ad4 +01a8 b.eq     #0xcc5ae0
00cc5ad8 +01ac bl       #0x1b58510 => (None, None)
00cc5adc +01b0 stur     x4, [x0, #7]
00cc5ae0 +01b4 stur     w0, [x2, #0x1b]
00cc5ae4 +01b8 add      x16, x27, #0xe, lsl #12
00cc5ae8 +01bc ldr      x16, [x16, #0x9a8]
00cc5aec +01c0 stur     w16, [x2, #0x1f]
00cc5af0 +01c4 ldur     x4, [x3, #0x3f]
00cc5af4 +01c8 sbfiz    x0, x4, #1, #0x1f
00cc5af8 +01cc cmp      x4, x0, asr #1
00cc5afc +01d0 b.eq     #0xcc5b08
00cc5b00 +01d4 bl       #0x1b58510 => (None, None)
00cc5b04 +01d8 stur     x4, [x0, #7]
00cc5b08 +01dc stur     w0, [x2, #0x23]
00cc5b0c +01e0 str      x2, [x15]
00cc5b10 +01e4 bl       #0x868b8c => ('_StringBase._interpolate', 8817548)
00cc5b14 +01e8 ldur     x1, [x29, #-0x20]
00cc5b18 +01ec mov      x3, x0
00cc5b1c +01f0 add      x2, x27, #0xe, lsl #12
00cc5b20 +01f4 ldr      x2, [x2, #0x910]
00cc5b24 +01f8 ldr      x4, [x27, #0x140]
00cc5b28 +01fc bl       #0x86af0c => ('_LoggerImpl.logWithTag', 8826636)
00cc5b2c +0200 ldur     x0, [x29, #-0x28]
00cc5b30 +0204 ldur     x2, [x0, #0x2f]
00cc5b34 +0208 ldur     x1, [x29, #-0x10]
00cc5b38 +020c bl       #0x8bfb4c => ('LaptopWorkspaceGetxController.getIndexByScreenId', 9173836)
00cc5b3c +0210 mov      x2, x0
00cc5b40 +0214 ldur     x0, [x29, #-0x10]
00cc5b44 +0218 stur     x2, [x29, #-0x30]
00cc5b48 +021c ldur     w1, [x0, #0x2f]
00cc5b4c +0220 add      x1, x1, x28, lsl #32
00cc5b50 +0224 bl       #0x1772e58 => ('RxList.value', 24587864)
00cc5b54 +0228 mov      x3, x0
00cc5b58 +022c ldur     x2, [x29, #-0x30]
00cc5b5c +0230 sbfiz    x0, x2, #1, #0x1f
00cc5b60 +0234 cmp      x2, x0, asr #1
00cc5b64 +0238 b.eq     #0xcc5b70
00cc5b68 +023c bl       #0x1b58510 => (None, None)
00cc5b6c +0240 stur     x2, [x0, #7]
00cc5b70 +0244 ldur     x1, [x3, #-1]
00cc5b74 +0248 ubfx     x1, x1, #0xc, #0x14
00cc5b78 +024c stp      x0, x3, [x15]
00cc5b7c +0250 mov      x0, x1
00cc5b80 +0254 sub      x30, x0, #0xd01
00cc5b84 +0258 ldr      x30, [x21, x30, lsl #3]
00cc5b88 +025c blr      x30
00cc5b8c +0260 ldur     w1, [x0, #0x1b]
00cc5b90 +0264 add      x1, x1, x28, lsl #32
00cc5b94 +0268 ldur     x0, [x29, #-0x28]
00cc5b98 +026c ldur     x2, [x0, #0x37]
00cc5b9c +0270 ldur     x3, [x0, #0x3f]
00cc5ba0 +0274 bl       #0xcc4600 => ('CellLayoutGetxController.calculateCenterGlobalPositionByCellXY', 13387264)
00cc5ba4 +0278 mov      x2, x0
00cc5ba8 +027c ldur     x0, [x29, #-0x28]
00cc5bac +0280 stur     x2, [x29, #-8]
00cc5bb0 +0284 ldur     x3, [x0, #0x2f]
00cc5bb4 +0288 ldur     x1, [x29, #-0x10]
00cc5bb8 +028c stur     x3, [x29, #-0x38]
00cc5bbc +0290 bl       #0xb6075c => ('LaptopWorkspaceGetxController.getCurrentScreenId', 11929436)
00cc5bc0 +0294 mov      x1, x0
00cc5bc4 +0298 ldur     x0, [x29, #-0x38]
00cc5bc8 +029c cmp      x0, x1
00cc5bcc +02a0 b.eq     #0xcc5c98
00cc5bd0 +02a4 ldur     x1, [x29, #-0x10]
00cc5bd4 +02a8 ldur     x0, [x29, #-0x30]
00cc5bd8 +02ac ldr      x0, [x26, #0x78]
00cc5bdc +02b0 ldr      x0, [x0, #0x1cd0]
00cc5be0 +02b4 ldr      x16, [x27, #0x40]
00cc5be4 +02b8 cmp      w0, w16
00cc5be8 +02bc b.ne     #0xcc5bf4
00cc5bec +02c0 ldr      x2, [x27, #0x7800]
00cc5bf0 +02c4 bl       #0x1b563d0 => (None, None)
00cc5bf4 +02c8 add      x16, x27, #8, lsl #12
00cc5bf8 +02cc ldr      x16, [x16, #0xfc8]
00cc5bfc +02d0 str      x16, [x15]
00cc5c00 +02d4 ldr      x4, [x27, #0x50]
00cc5c04 +02d8 bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc5c08 +02dc mov      x1, x0
00cc5c0c +02e0 bl       #0x8a72a4 => ('AppServiceImpl.navKey', 9073316)
00cc5c10 +02e4 mov      x1, x0
00cc5c14 +02e8 bl       #0x8a7230 => ('GlobalKey._currentElement', 9073200)
00cc5c18 +02ec cmp      w0, w22
00cc5c1c +02f0 b.eq     #0xcc5e18
00cc5c20 +02f4 mov      x1, x0
00cc5c24 +02f8 bl       #0xabc8ac => ('MediaQuery.sizeOf', 11258028)
00cc5c28 +02fc ldur     d0, [x0, #7]
00cc5c2c +0300 ldur     x0, [x29, #-0x10]
00cc5c30 +0304 stur     d0, [x29, #-0x40]
00cc5c34 +0308 ldur     w1, [x0, #0x43]
00cc5c38 +030c add      x1, x1, x28, lsl #32
00cc5c3c +0310 bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
00cc5c40 +0314 sbfx     x1, x0, #1, #0x1f
00cc5c44 +0318 tbz      w0, #0, #0xcc5c4c
00cc5c48 +031c ldur     x1, [x0, #7]
00cc5c4c +0320 ldur     x0, [x29, #-0x30]
00cc5c50 +0324 sub      x2, x0, x1
00cc5c54 +0328 scvtf    d0, x2
00cc5c58 +032c ldur     d1, [x29, #-0x40]
00cc5c5c +0330 fmul     d2, d1, d0
00cc5c60 +0334 stur     d2, [x29, #-0x48]
00cc5c64 +0338 bl       #0x18b600c => (None, None)
00cc5c68 +033c ldur     d0, [x29, #-0x48]
00cc5c6c +0340 stur     d0, [x0, #7]
00cc5c70 +0344 stur     xzr, [x0, #0xf]
00cc5c74 +0348 ldur     x1, [x29, #-8]
00cc5c78 +034c mov      x2, x0
00cc5c7c +0350 bl       #0x892a78 => ('Offset.+', 8989304)
00cc5c80 +0354 ldur     x1, [x29, #-0x18]
00cc5c84 +0358 mov      x2, x0
00cc5c88 +035c stur     x0, [x29, #-0x10]
00cc5c8c +0360 bl       #0xb39c9c => ('DragObject.setDropTargetGlobalPosition', 11771036)
00cc5c90 +0364 ldur     x0, [x29, #-0x10]
00cc5c94 +0368 b        #0xcc5d54
00cc5c98 +036c ldur     x0, [x29, #-8]
00cc5c9c +0370 mov      x1, x22
00cc5ca0 +0374 mov      x2, #8
00cc5ca4 +0378 bl       #0x1b58288 => (None, None)
00cc5ca8 +037c add      x16, x27, #0x60, lsl #12
00cc5cac +0380 ldr      x16, [x16, #0x748]
00cc5cb0 +0384 stur     w16, [x0, #0xf]
00cc5cb4 +0388 ldur     x2, [x29, #-8]
00cc5cb8 +038c ldur     d0, [x2, #7]
00cc5cbc +0390 ldp      x1, x3, [x26, #0x60]
00cc5cc0 +0394 add      x1, x1, #0x10
00cc5cc4 +0398 cmp      x3, x1
00cc5cc8 +039c b.ls     #0xcc5e1c
00cc5ccc +03a0 str      x1, [x26, #0x60]
00cc5cd0 +03a4 sub      x1, x1, #0xf
00cc5cd4 +03a8 mov      x3, #0xe15c
00cc5cd8 +03ac movk     x3, #3, lsl #16
00cc5cdc +03b0 stur     x3, [x1, #-1]
00cc5ce0 +03b4 stur     d0, [x1, #7]
00cc5ce4 +03b8 stur     w1, [x0, #0x13]
00cc5ce8 +03bc add      x16, x27, #0x34, lsl #12
00cc5cec +03c0 ldr      x16, [x16, #0x8a0]
00cc5cf0 +03c4 stur     w16, [x0, #0x17]
00cc5cf4 +03c8 ldur     d0, [x2, #0xf]
00cc5cf8 +03cc ldp      x1, x3, [x26, #0x60]
00cc5cfc +03d0 add      x1, x1, #0x10
00cc5d00 +03d4 cmp      x3, x1
00cc5d04 +03d8 b.ls     #0xcc5e38
00cc5d08 +03dc str      x1, [x26, #0x60]
00cc5d0c +03e0 sub      x1, x1, #0xf
00cc5d10 +03e4 mov      x3, #0xe15c
00cc5d14 +03e8 movk     x3, #3, lsl #16
00cc5d18 +03ec stur     x3, [x1, #-1]
00cc5d1c +03f0 stur     d0, [x1, #7]
00cc5d20 +03f4 stur     w1, [x0, #0x1b]
00cc5d24 +03f8 str      x0, [x15]
00cc5d28 +03fc bl       #0x868b8c => ('_StringBase._interpolate', 8817548)
00cc5d2c +0400 ldur     x1, [x29, #-0x20]
00cc5d30 +0404 mov      x3, x0
00cc5d34 +0408 add      x2, x27, #0xe, lsl #12
00cc5d38 +040c ldr      x2, [x2, #0x910]
00cc5d3c +0410 ldr      x4, [x27, #0x140]
00cc5d40 +0414 bl       #0x86af0c => ('_LoggerImpl.logWithTag', 8826636)
00cc5d44 +0418 ldur     x1, [x29, #-0x18]
00cc5d48 +041c ldur     x2, [x29, #-8]
00cc5d4c +0420 bl       #0xb39c9c => ('DragObject.setDropTargetGlobalPosition', 11771036)
00cc5d50 +0424 ldur     x0, [x29, #-8]
00cc5d54 +0428 stur     x0, [x29, #-8]
00cc5d58 +042c mov      x1, x22
00cc5d5c +0430 mov      x2, #8
00cc5d60 +0434 bl       #0x1b58288 => (None, None)
00cc5d64 +0438 add      x16, x27, #0x60, lsl #12
00cc5d68 +043c ldr      x16, [x16, #0x748]
00cc5d6c +0440 stur     w16, [x0, #0xf]
00cc5d70 +0444 ldur     x1, [x29, #-8]
00cc5d74 +0448 ldur     d0, [x1, #7]
00cc5d78 +044c ldp      x2, x3, [x26, #0x60]
00cc5d7c +0450 add      x2, x2, #0x10
00cc5d80 +0454 cmp      x3, x2
00cc5d84 +0458 b.ls     #0xcc5e54
00cc5d88 +045c str      x2, [x26, #0x60]
00cc5d8c +0460 sub      x2, x2, #0xf
00cc5d90 +0464 mov      x3, #0xe15c
00cc5d94 +0468 movk     x3, #3, lsl #16
00cc5d98 +046c stur     x3, [x2, #-1]
00cc5d9c +0470 stur     d0, [x2, #7]
00cc5da0 +0474 stur     w2, [x0, #0x13]
00cc5da4 +0478 add      x16, x27, #0x34, lsl #12
00cc5da8 +047c ldr      x16, [x16, #0x8a0]
00cc5dac +0480 stur     w16, [x0, #0x17]
00cc5db0 +0484 ldur     d0, [x1, #0xf]
00cc5db4 +0488 ldp      x1, x2, [x26, #0x60]
00cc5db8 +048c add      x1, x1, #0x10
00cc5dbc +0490 cmp      x2, x1
00cc5dc0 +0494 b.ls     #0xcc5e70
00cc5dc4 +0498 str      x1, [x26, #0x60]
00cc5dc8 +049c sub      x1, x1, #0xf
00cc5dcc +04a0 mov      x2, #0xe15c
00cc5dd0 +04a4 movk     x2, #3, lsl #16
00cc5dd4 +04a8 stur     x2, [x1, #-1]
00cc5dd8 +04ac stur     d0, [x1, #7]
00cc5ddc +04b0 stur     w1, [x0, #0x1b]
00cc5de0 +04b4 str      x0, [x15]
00cc5de4 +04b8 bl       #0x868b8c => ('_StringBase._interpolate', 8817548)
00cc5de8 +04bc ldur     x1, [x29, #-0x20]
00cc5dec +04c0 mov      x3, x0
00cc5df0 +04c4 add      x2, x27, #0xe, lsl #12
00cc5df4 +04c8 ldr      x2, [x2, #0x910]
00cc5df8 +04cc ldr      x4, [x27, #0x140]
00cc5dfc +04d0 bl       #0x86af0c => ('_LoggerImpl.logWithTag', 8826636)
00cc5e00 +04d4 mov      x0, x22
00cc5e04 +04d8 mov      x15, x29
00cc5e08 +04dc ldp      x29, x30, [x15], #0x10
00cc5e0c +04e0 ret      
00cc5e10 +04e4 bl       #0x1b588b8 => (None, None)
00cc5e14 +04e8 bl       #0x1b588b8 => (None, None)
00cc5e18 +04ec bl       #0x1b58a18 => (None, None)
00cc5e1c +04f0 str      q0, [x15, #-0x10]!
00cc5e20 +04f4 stp      x0, x2, [x15, #-0x10]!
00cc5e24 +04f8 bl       #0x1b581e0 => (None, None)
00cc5e28 +04fc mov      x1, x0
00cc5e2c +0500 ldp      x0, x2, [x15], #0x10
00cc5e30 +0504 ldr      q0, [x15], #0x10
00cc5e34 +0508 b        #0xcc5ce0
00cc5e38 +050c str      q0, [x15, #-0x10]!
00cc5e3c +0510 stp      x0, x2, [x15, #-0x10]!
00cc5e40 +0514 bl       #0x1b581e0 => (None, None)
00cc5e44 +0518 mov      x1, x0
00cc5e48 +051c ldp      x0, x2, [x15], #0x10
00cc5e4c +0520 ldr      q0, [x15], #0x10
00cc5e50 +0524 b        #0xcc5d1c
00cc5e54 +0528 str      q0, [x15, #-0x10]!
00cc5e58 +052c stp      x0, x1, [x15, #-0x10]!
00cc5e5c +0530 bl       #0x1b581e0 => (None, None)
00cc5e60 +0534 mov      x2, x0
00cc5e64 +0538 ldp      x0, x1, [x15], #0x10
00cc5e68 +053c ldr      q0, [x15], #0x10
00cc5e6c +0540 b        #0xcc5d9c
00cc5e70 +0544 str      q0, [x15, #-0x10]!
00cc5e74 +0548 str      x0, [x15, #-8]!
00cc5e78 +054c bl       #0x1b581e0 => (None, None)
00cc5e7c +0550 mov      x1, x0
00cc5e80 +0554 ldr      x0, [x15], #8
00cc5e84 +0558 ldr      q0, [x15], #0x10
00cc5e88 +055c b        #0xcc5dd8