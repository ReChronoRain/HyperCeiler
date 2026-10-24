00cc4800 +0000 stp      x29, x30, [x15, #-0x10]!
00cc4804 +0004 mov      x29, x15
00cc4808 +0008 sub      x15, x15, #0x50
00cc480c +000c stur     x1, [x29, #-8]
00cc4810 +0010 stur     x2, [x29, #-0x10]
00cc4814 +0014 stur     x3, [x29, #-0x18]
00cc4818 +0018 ldr      x0, [x26, #0x78]
00cc481c +001c ldr      x0, [x0, #0x2710]
00cc4820 +0020 cmp      w0, w22
00cc4824 +0024 b.ne     #0xcc4860
00cc4828 +0028 ldr      x0, [x26, #0x78]
00cc482c +002c ldr      x0, [x0, #0x1cd0]
00cc4830 +0030 ldr      x16, [x27, #0x40]
00cc4834 +0034 cmp      w0, w16
00cc4838 +0038 b.ne     #0xcc4844
00cc483c +003c ldr      x2, [x27, #0x7800]
00cc4840 +0040 bl       #0x1b563d0 => (None, None)
00cc4844 +0044 add      x16, x27, #8, lsl #12
00cc4848 +0048 ldr      x16, [x16, #0xfc0]
00cc484c +004c str      x16, [x15]
00cc4850 +0050 ldr      x4, [x27, #0x50]
00cc4854 +0054 bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc4858 +0058 mov      x1, x0
00cc485c +005c b        #0xcc4864
00cc4860 +0060 mov      x1, x0
00cc4864 +0064 bl       #0xb1a278 => ('GridController.currentConfig', 11641464)
00cc4868 +0068 ldur     d0, [x0, #0x2b]
00cc486c +006c stur     d0, [x29, #-0x38]
00cc4870 +0070 ldr      x0, [x26, #0x78]
00cc4874 +0074 ldr      x0, [x0, #0x2710]
00cc4878 +0078 cmp      w0, w22
00cc487c +007c b.ne     #0xcc48b8
00cc4880 +0080 ldr      x0, [x26, #0x78]
00cc4884 +0084 ldr      x0, [x0, #0x1cd0]
00cc4888 +0088 ldr      x16, [x27, #0x40]
00cc488c +008c cmp      w0, w16
00cc4890 +0090 b.ne     #0xcc489c
00cc4894 +0094 ldr      x2, [x27, #0x7800]
00cc4898 +0098 bl       #0x1b563d0 => (None, None)
00cc489c +009c add      x16, x27, #8, lsl #12
00cc48a0 +00a0 ldr      x16, [x16, #0xfc0]
00cc48a4 +00a4 str      x16, [x15]
00cc48a8 +00a8 ldr      x4, [x27, #0x50]
00cc48ac +00ac bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc48b0 +00b0 mov      x1, x0
00cc48b4 +00b4 b        #0xcc48bc
00cc48b8 +00b8 mov      x1, x0
00cc48bc +00bc bl       #0xb1a278 => ('GridController.currentConfig', 11641464)
00cc48c0 +00c0 ldur     d0, [x0, #0x33]
00cc48c4 +00c4 stur     d0, [x29, #-0x40]
00cc48c8 +00c8 ldr      x0, [x26, #0x78]
00cc48cc +00cc ldr      x0, [x0, #0x2710]
00cc48d0 +00d0 cmp      w0, w22
00cc48d4 +00d4 b.ne     #0xcc4910
00cc48d8 +00d8 ldr      x0, [x26, #0x78]
00cc48dc +00dc ldr      x0, [x0, #0x1cd0]
00cc48e0 +00e0 ldr      x16, [x27, #0x40]
00cc48e4 +00e4 cmp      w0, w16
00cc48e8 +00e8 b.ne     #0xcc48f4
00cc48ec +00ec ldr      x2, [x27, #0x7800]
00cc48f0 +00f0 bl       #0x1b563d0 => (None, None)
00cc48f4 +00f4 add      x16, x27, #8, lsl #12
00cc48f8 +00f8 ldr      x16, [x16, #0xfc0]
00cc48fc +00fc str      x16, [x15]
00cc4900 +0100 ldr      x4, [x27, #0x50]
00cc4904 +0104 bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc4908 +0108 mov      x1, x0
00cc490c +010c b        #0xcc4914
00cc4910 +0110 mov      x1, x0
00cc4914 +0114 ldur     x0, [x29, #-0x10]
00cc4918 +0118 ldur     d0, [x29, #-0x38]
00cc491c +011c bl       #0xb1a278 => ('GridController.currentConfig', 11641464)
00cc4920 +0120 ldur     w1, [x0, #0x3b]
00cc4924 +0124 add      x1, x1, x28, lsl #32
00cc4928 +0128 ldur     d0, [x1, #7]
00cc492c +012c ldur     x0, [x29, #-0x10]
00cc4930 +0130 scvtf    d1, x0
00cc4934 +0134 ldur     d2, [x29, #-0x38]
00cc4938 +0138 fmul     d3, d1, d2
00cc493c +013c fadd     d1, d0, d3
00cc4940 +0140 stur     d1, [x29, #-0x38]
00cc4944 +0144 ldr      x0, [x26, #0x78]
00cc4948 +0148 ldr      x0, [x0, #0x2710]
00cc494c +014c cmp      w0, w22
00cc4950 +0150 b.ne     #0xcc498c
00cc4954 +0154 ldr      x0, [x26, #0x78]
00cc4958 +0158 ldr      x0, [x0, #0x1cd0]
00cc495c +015c ldr      x16, [x27, #0x40]
00cc4960 +0160 cmp      w0, w16
00cc4964 +0164 b.ne     #0xcc4970
00cc4968 +0168 ldr      x2, [x27, #0x7800]
00cc496c +016c bl       #0x1b563d0 => (None, None)
00cc4970 +0170 add      x16, x27, #8, lsl #12
00cc4974 +0174 ldr      x16, [x16, #0xfc0]
00cc4978 +0178 str      x16, [x15]
00cc497c +017c ldr      x4, [x27, #0x50]
00cc4980 +0180 bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc4984 +0184 mov      x1, x0
00cc4988 +0188 b        #0xcc4990
00cc498c +018c mov      x1, x0
00cc4990 +0190 ldur     x0, [x29, #-0x18]
00cc4994 +0194 ldur     d0, [x29, #-0x40]
00cc4998 +0198 ldur     w2, [x1, #0xf3]
00cc499c +019c add      x2, x2, x28, lsl #32
00cc49a0 +01a0 mov      x1, x2
00cc49a4 +01a4 bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
00cc49a8 +01a8 mov      x1, x0
00cc49ac +01ac ldur     x0, [x29, #-0x18]
00cc49b0 +01b0 scvtf    d0, x0
00cc49b4 +01b4 ldur     d1, [x29, #-0x40]
00cc49b8 +01b8 fmul     d2, d0, d1
00cc49bc +01bc ldur     d0, [x1, #7]
00cc49c0 +01c0 fadd     d1, d0, d2
00cc49c4 +01c4 ldur     x1, [x29, #-8]
00cc49c8 +01c8 stur     d1, [x29, #-0x40]
00cc49cc +01cc bl       #0x8a5214 => ('CellLayoutGetxController._shouldApplyDualPageOffset', 9064980)
00cc49d0 +01d0 tbnz     w0, #4, #0xcc4a2c
00cc49d4 +01d4 ldr      x0, [x26, #0x78]
00cc49d8 +01d8 ldr      x0, [x0, #0x2710]
00cc49dc +01dc cmp      w0, w22
00cc49e0 +01e0 b.ne     #0xcc4a14
00cc49e4 +01e4 ldr      x0, [x26, #0x78]
00cc49e8 +01e8 ldr      x0, [x0, #0x1cd0]
00cc49ec +01ec ldr      x16, [x27, #0x40]
00cc49f0 +01f0 cmp      w0, w16
00cc49f4 +01f4 b.ne     #0xcc4a00
00cc49f8 +01f8 ldr      x2, [x27, #0x7800]
00cc49fc +01fc bl       #0x1b563d0 => (None, None)
00cc4a00 +0200 add      x16, x27, #8, lsl #12
00cc4a04 +0204 ldr      x16, [x16, #0xfc0]
00cc4a08 +0208 str      x16, [x15]
00cc4a0c +020c ldr      x4, [x27, #0x50]
00cc4a10 +0210 bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc4a14 +0214 ldur     d0, [x29, #-0x38]
00cc4a18 +0218 fmov     d1, #2.00000000
00cc4a1c +021c ldur     d2, [x0, #0xaf]
00cc4a20 +0220 fdiv     d3, d2, d1
00cc4a24 +0224 fadd     d1, d0, d3
00cc4a28 +0228 b        #0xcc4a34
00cc4a2c +022c ldur     d0, [x29, #-0x38]
00cc4a30 +0230 mov      v1.16b, v0.16b
00cc4a34 +0234 ldur     d0, [x29, #-0x40]
00cc4a38 +0238 stur     d1, [x29, #-0x38]
00cc4a3c +023c bl       #0x18b600c => (None, None)
00cc4a40 +0240 ldur     d0, [x29, #-0x38]
00cc4a44 +0244 stur     x0, [x29, #-0x20]
00cc4a48 +0248 stur     d0, [x0, #7]
00cc4a4c +024c ldur     d0, [x29, #-0x40]
00cc4a50 +0250 stur     d0, [x0, #0xf]
00cc4a54 +0254 ldr      x1, [x26, #0x78]
00cc4a58 +0258 ldr      x1, [x1, #0x2710]
00cc4a5c +025c cmp      w1, w22
00cc4a60 +0260 b.ne     #0xcc4a98
00cc4a64 +0264 ldr      x0, [x26, #0x78]
00cc4a68 +0268 ldr      x0, [x0, #0x1cd0]
00cc4a6c +026c ldr      x16, [x27, #0x40]
00cc4a70 +0270 cmp      w0, w16
00cc4a74 +0274 b.ne     #0xcc4a80
00cc4a78 +0278 ldr      x2, [x27, #0x7800]
00cc4a7c +027c bl       #0x1b563d0 => (None, None)
00cc4a80 +0280 add      x16, x27, #8, lsl #12
00cc4a84 +0284 ldr      x16, [x16, #0xfc0]
00cc4a88 +0288 str      x16, [x15]
00cc4a8c +028c ldr      x4, [x27, #0x50]
00cc4a90 +0290 bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc4a94 +0294 mov      x1, x0
00cc4a98 +0298 bl       #0x962d54 => ('GridController.gridHeight', 9842004)
00cc4a9c +029c stur     d0, [x29, #-0x38]
00cc4aa0 +02a0 ldr      x0, [x26, #0x78]
00cc4aa4 +02a4 ldr      x0, [x0, #0x45e8]
00cc4aa8 +02a8 cmp      w0, w22
00cc4aac +02ac b.ne     #0xcc4ae8
00cc4ab0 +02b0 ldr      x0, [x26, #0x78]
00cc4ab4 +02b4 ldr      x0, [x0, #0x1cd0]
00cc4ab8 +02b8 ldr      x16, [x27, #0x40]
00cc4abc +02bc cmp      w0, w16
00cc4ac0 +02c0 b.ne     #0xcc4acc
00cc4ac4 +02c4 ldr      x2, [x27, #0x7800]
00cc4ac8 +02c8 bl       #0x1b563d0 => (None, None)
00cc4acc +02cc add      x16, x27, #0x13, lsl #12
00cc4ad0 +02d0 ldr      x16, [x16, #0xb28]
00cc4ad4 +02d4 str      x16, [x15]
00cc4ad8 +02d8 ldr      x4, [x27, #0x50]
00cc4adc +02dc bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc4ae0 +02e0 mov      x1, x0
00cc4ae4 +02e4 b        #0xcc4aec
00cc4ae8 +02e8 mov      x1, x0
00cc4aec +02ec bl       #0x962e2c => ('CellScaleGetxController.getEditModePivotY', 9842220)
00cc4af0 +02f0 stur     d0, [x29, #-0x40]
00cc4af4 +02f4 ldr      x0, [x26, #0x78]
00cc4af8 +02f8 ldr      x0, [x0, #0x2710]
00cc4afc +02fc cmp      w0, w22
00cc4b00 +0300 b.ne     #0xcc4b34
00cc4b04 +0304 ldr      x0, [x26, #0x78]
00cc4b08 +0308 ldr      x0, [x0, #0x1cd0]
00cc4b0c +030c ldr      x16, [x27, #0x40]
00cc4b10 +0310 cmp      w0, w16
00cc4b14 +0314 b.ne     #0xcc4b20
00cc4b18 +0318 ldr      x2, [x27, #0x7800]
00cc4b1c +031c bl       #0x1b563d0 => (None, None)
00cc4b20 +0320 add      x16, x27, #8, lsl #12
00cc4b24 +0324 ldr      x16, [x16, #0xfc0]
00cc4b28 +0328 str      x16, [x15]
00cc4b2c +032c ldr      x4, [x27, #0x50]
00cc4b30 +0330 bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc4b34 +0334 ldur     w1, [x0, #0xf3]
00cc4b38 +0338 add      x1, x1, x28, lsl #32
00cc4b3c +033c bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
00cc4b40 +0340 stur     x0, [x29, #-0x28]
00cc4b44 +0344 ldr      x0, [x26, #0x78]
00cc4b48 +0348 ldr      x0, [x0, #0x26f0]
00cc4b4c +034c ldr      x16, [x27, #0x40]
00cc4b50 +0350 cmp      w0, w16
00cc4b54 +0354 b.ne     #0xcc4b60
00cc4b58 +0358 ldr      x2, [x27, #0x7818]
00cc4b5c +035c bl       #0x1b563d0 => (None, None)
00cc4b60 +0360 mov      x1, x0
00cc4b64 +0364 bl       #0x8a5438 => ('FoldDeviceManager.isFoldDualPageMode', 9065528)
00cc4b68 +0368 tbnz     w0, #4, #0xcc4b84
00cc4b6c +036c ldur     d0, [x29, #-0x40]
00cc4b70 +0370 ldur     x0, [x29, #-0x28]
00cc4b74 +0374 ldur     d1, [x0, #7]
00cc4b78 +0378 fadd     d2, d0, d1
00cc4b7c +037c mov      v1.16b, v2.16b
00cc4b80 +0380 b        #0xcc4be0
00cc4b84 +0384 ldur     d1, [x29, #-0x38]
00cc4b88 +0388 ldur     d0, [x29, #-0x40]
00cc4b8c +038c ldur     x0, [x29, #-0x28]
00cc4b90 +0390 ldr      x0, [x26, #0x78]
00cc4b94 +0394 ldr      x0, [x0, #0x2708]
00cc4b98 +0398 ldr      x16, [x27, #0x40]
00cc4b9c +039c cmp      w0, w16
00cc4ba0 +03a0 b.ne     #0xcc4bb0
00cc4ba4 +03a4 add      x2, x27, #0x14, lsl #12
00cc4ba8 +03a8 ldr      x2, [x2, #0x58]
00cc4bac +03ac bl       #0x1b563d0 => (None, None)
00cc4bb0 +03b0 mov      x1, x0
00cc4bb4 +03b4 bl       #0x95ec2c => ('LauncherLayoutTypeProvider.getScreenMarginBottom', 9825324)
00cc4bb8 +03b8 mov      v1.16b, v0.16b
00cc4bbc +03bc ldur     d0, [x29, #-0x38]
00cc4bc0 +03c0 fsub     d2, d0, d1
00cc4bc4 +03c4 ldur     d1, [x29, #-0x40]
00cc4bc8 +03c8 fmul     d3, d2, d1
00cc4bcc +03cc fdiv     d1, d3, d0
00cc4bd0 +03d0 ldur     x0, [x29, #-0x28]
00cc4bd4 +03d4 ldur     d0, [x0, #7]
00cc4bd8 +03d8 fadd     d2, d1, d0
00cc4bdc +03dc mov      v1.16b, v2.16b
00cc4be0 +03e0 stur     d1, [x29, #-0x38]
00cc4be4 +03e4 ldr      x0, [x26, #0x78]
00cc4be8 +03e8 ldr      x0, [x0, #0x45e8]
00cc4bec +03ec cmp      w0, w22
00cc4bf0 +03f0 b.ne     #0xcc4c24
00cc4bf4 +03f4 ldr      x0, [x26, #0x78]
00cc4bf8 +03f8 ldr      x0, [x0, #0x1cd0]
00cc4bfc +03fc ldr      x16, [x27, #0x40]
00cc4c00 +0400 cmp      w0, w16
00cc4c04 +0404 b.ne     #0xcc4c10
00cc4c08 +0408 ldr      x2, [x27, #0x7800]
00cc4c0c +040c bl       #0x1b563d0 => (None, None)
00cc4c10 +0410 add      x16, x27, #0x13, lsl #12
00cc4c14 +0414 ldr      x16, [x16, #0xb28]
00cc4c18 +0418 str      x16, [x15]
00cc4c1c +041c ldr      x4, [x27, #0x50]
00cc4c20 +0420 bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc4c24 +0424 ldur     w1, [x0, #0x2b]
00cc4c28 +0428 add      x1, x1, x28, lsl #32
00cc4c2c +042c bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
00cc4c30 +0430 stur     x0, [x29, #-0x28]
00cc4c34 +0434 ldr      x1, [x26, #0x78]
00cc4c38 +0438 ldr      x1, [x1, #0x45e8]
00cc4c3c +043c cmp      w1, w22
00cc4c40 +0440 b.ne     #0xcc4c78
00cc4c44 +0444 ldr      x0, [x26, #0x78]
00cc4c48 +0448 ldr      x0, [x0, #0x1cd0]
00cc4c4c +044c ldr      x16, [x27, #0x40]
00cc4c50 +0450 cmp      w0, w16
00cc4c54 +0454 b.ne     #0xcc4c60
00cc4c58 +0458 ldr      x2, [x27, #0x7800]
00cc4c5c +045c bl       #0x1b563d0 => (None, None)
00cc4c60 +0460 add      x16, x27, #0x13, lsl #12
00cc4c64 +0464 ldr      x16, [x16, #0xb28]
00cc4c68 +0468 str      x16, [x15]
00cc4c6c +046c ldr      x4, [x27, #0x50]
00cc4c70 +0470 bl       #0xb1f9a8 => ('Inst|find', 11663784)
00cc4c74 +0474 mov      x1, x0
00cc4c78 +0478 ldur     x0, [x29, #-8]
00cc4c7c +047c ldur     d1, [x29, #-0x38]
00cc4c80 +0480 ldur     x3, [x29, #-0x28]
00cc4c84 +0484 bl       #0xa38a54 => ('CellScaleGetxController.getPivotXLoc', 10717780)
00cc4c88 +0488 stur     d0, [x29, #-0x40]
00cc4c8c +048c bl       #0x18c7280 => (None, None)
00cc4c90 +0490 mov      x4, #0x20
00cc4c94 +0494 stur     x0, [x29, #-0x30]
00cc4c98 +0498 bl       #0x1b578c8 => (None, None)
00cc4c9c +049c mov      x1, x0
00cc4ca0 +04a0 ldur     x0, [x29, #-0x30]
00cc4ca4 +04a4 stur     w1, [x0, #7]
00cc4ca8 +04a8 mov      x1, x0
00cc4cac +04ac bl       #0x8f6e28 => ('Matrix4.setIdentity', 9399848)
00cc4cb0 +04b0 ldur     x1, [x29, #-0x30]
00cc4cb4 +04b4 ldur     d0, [x29, #-0x40]
00cc4cb8 +04b8 ldur     d1, [x29, #-0x38]
00cc4cbc +04bc eor      v2.16b, v2.16b, v2.16b
00cc4cc0 +04c0 bl       #0x8f60b8 => ('Matrix4.translateByDouble', 9396408)
00cc4cc4 +04c4 ldur     x3, [x29, #-0x28]
00cc4cc8 +04c8 ldur     d1, [x3, #7]
00cc4ccc +04cc ldur     x1, [x29, #-0x30]
00cc4cd0 +04d0 mov      x2, x3
00cc4cd4 +04d4 mov      v0.16b, v1.16b
00cc4cd8 +04d8 stur     d1, [x29, #-0x48]
00cc4cdc +04dc bl       #0x8f6fd4 => ('Matrix4.scaleByDouble', 9400276)
00cc4ce0 +04e0 ldur     d0, [x29, #-0x40]
00cc4ce4 +04e4 fneg     d1, d0
00cc4ce8 +04e8 ldur     d0, [x29, #-0x38]
00cc4cec +04ec fneg     d2, d0
00cc4cf0 +04f0 ldur     x1, [x29, #-0x30]
00cc4cf4 +04f4 mov      v0.16b, v1.16b
00cc4cf8 +04f8 mov      v1.16b, v2.16b
00cc4cfc +04fc eor      v2.16b, v2.16b, v2.16b
00cc4d00 +0500 bl       #0x8f60b8 => ('Matrix4.translateByDouble', 9396408)
00cc4d04 +0504 bl       #0x18c7280 => (None, None)
00cc4d08 +0508 mov      x4, #0x20
00cc4d0c +050c stur     x0, [x29, #-0x28]
00cc4d10 +0510 bl       #0x1b578c8 => (None, None)
00cc4d14 +0514 mov      x1, x0
00cc4d18 +0518 ldur     x0, [x29, #-0x28]
00cc4d1c +051c stur     w1, [x0, #7]
00cc4d20 +0520 mov      x1, x0
00cc4d24 +0524 bl       #0x8f6e28 => ('Matrix4.setIdentity', 9399848)
00cc4d28 +0528 ldur     x1, [x29, #-0x28]
00cc4d2c +052c eor      v0.16b, v0.16b, v0.16b
00cc4d30 +0530 eor      v1.16b, v1.16b, v1.16b
00cc4d34 +0534 eor      v2.16b, v2.16b, v2.16b
00cc4d38 +0538 bl       #0x8f60b8 => ('Matrix4.translateByDouble', 9396408)
00cc4d3c +053c ldur     x1, [x29, #-0x30]
00cc4d40 +0540 ldur     x2, [x29, #-0x20]
00cc4d44 +0544 bl       #0x9c5258 => ('MatrixUtils.transformPoint', 10244696)
00cc4d48 +0548 ldur     x1, [x29, #-0x28]
00cc4d4c +054c mov      x2, x0
00cc4d50 +0550 bl       #0x9c5258 => ('MatrixUtils.transformPoint', 10244696)
00cc4d54 +0554 mov      x2, x0
00cc4d58 +0558 ldur     x0, [x29, #-8]
00cc4d5c +055c stur     x2, [x29, #-0x20]
00cc4d60 +0560 ldur     x1, [x0, #0x1f]
00cc4d64 +0564 bl       #0xa3879c => ('ScreenPageView.getQ18EditTranslateX', 10717084)
00cc4d68 +0568 mov      v1.16b, v0.16b
00cc4d6c +056c eor      v0.16b, v0.16b, v0.16b
00cc4d70 +0570 fcmp     d1, d0
00cc4d74 +0574 b.eq     #0xcc4db0
00cc4d78 +0578 ldur     x0, [x29, #-0x20]
00cc4d7c +057c ldur     d0, [x29, #-0x48]
00cc4d80 +0580 ldur     d2, [x0, #7]
00cc4d84 +0584 fmul     d3, d1, d0
00cc4d88 +0588 fadd     d0, d2, d3
00cc4d8c +058c stur     d0, [x29, #-0x40]
00cc4d90 +0590 ldur     d1, [x0, #0xf]
00cc4d94 +0594 stur     d1, [x29, #-0x38]
00cc4d98 +0598 bl       #0x18b600c => (None, None)
00cc4d9c +059c ldur     d0, [x29, #-0x40]
00cc4da0 +05a0 stur     d0, [x0, #7]
00cc4da4 +05a4 ldur     d0, [x29, #-0x38]
00cc4da8 +05a8 stur     d0, [x0, #0xf]
00cc4dac +05ac b        #0xcc4db4
00cc4db0 +05b0 ldur     x0, [x29, #-0x20]
00cc4db4 +05b4 mov      x15, x29
00cc4db8 +05b8 ldp      x29, x30, [x15], #0x10
00cc4dbc +05bc ret      