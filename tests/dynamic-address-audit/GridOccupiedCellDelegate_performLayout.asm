016f702c +0000 stp      x29, x30, [x15, #-0x10]!
016f7030 +0004 mov      x29, x15
016f7034 +0008 sub      x15, x15, #0x88
016f7038 +000c stur     x1, [x29, #-8]
016f703c +0010 stur     x2, [x29, #-0x10]
016f7040 +0014 mov      x1, #2
016f7044 +0018 bl       #0x1b571f8 => (None, None)
016f7048 +001c mov      x1, x0
016f704c +0020 ldur     x0, [x29, #-8]
016f7050 +0024 stur     x1, [x29, #-0x18]
016f7054 +0028 stur     w0, [x1, #0xf]
016f7058 +002c ldur     x2, [x29, #-0x10]
016f705c +0030 stur     w2, [x1, #0x13]
016f7060 +0034 ldur     d0, [x2, #7]
016f7064 +0038 fmov     d1, #1.00000000
016f7068 +003c fcmp     d1, d0
016f706c +0040 b.gt     #0x16f707c
016f7070 +0044 ldur     d0, [x2, #0xf]
016f7074 +0048 fcmp     d1, d0
016f7078 +004c b.le     #0x16f70bc
016f707c +0050 ldr      x0, [x26, #0x78]
016f7080 +0054 ldr      x0, [x0, #0x2490]
016f7084 +0058 ldr      x16, [x27, #0x40]
016f7088 +005c cmp      w0, w16
016f708c +0060 b.ne     #0x16f7098
016f7090 +0064 ldr      x2, [x27, #0x60]
016f7094 +0068 bl       #0x1b563d0 => (None, None)
016f7098 +006c ldur     x2, [x29, #-0x18]
016f709c +0070 add      x1, x27, #0xa8, lsl #12
016f70a0 +0074 ldr      x1, [x1, #0x460]
016f70a4 +0078 stur     x0, [x29, #-0x10]
016f70a8 +007c bl       #0x1b575bc => (None, None)
016f70ac +0080 ldur     x1, [x29, #-0x10]
016f70b0 +0084 mov      x2, x0
016f70b4 +0088 ldr      x4, [x27, #0x70]
016f70b8 +008c bl       #0x8a8810 => ('_LoggerImpl.info', 9078800)
016f70bc +0090 ldur     x2, [x29, #-8]
016f70c0 +0094 ldur     w0, [x2, #0x17]
016f70c4 +0098 add      x0, x0, x28, lsl #32
016f70c8 +009c ldur     d0, [x0, #0x2b]
016f70cc +00a0 stur     d0, [x29, #-0x60]
016f70d0 +00a4 ldur     d1, [x0, #0x33]
016f70d4 +00a8 stur     d1, [x29, #-0x58]
016f70d8 +00ac ldur     w3, [x2, #0xf]
016f70dc +00b0 add      x3, x3, x28, lsl #32
016f70e0 +00b4 stur     x3, [x29, #-0x30]
016f70e4 +00b8 ldur     w1, [x3, #0xb]
016f70e8 +00bc sbfx     x4, x1, #1, #0x1f
016f70ec +00c0 stur     x4, [x29, #-0x28]
016f70f0 +00c4 ldur     w1, [x0, #0x3b]
016f70f4 +00c8 add      x1, x1, x28, lsl #32
016f70f8 +00cc ldur     d2, [x1, #7]
016f70fc +00d0 stur     d2, [x29, #-0x50]
016f7100 +00d4 ldp      x5, x0, [x26, #0x60]
016f7104 +00d8 add      x5, x5, #0x10
016f7108 +00dc cmp      x0, x5
016f710c +00e0 b.ls     #0x16f764c
016f7110 +00e4 str      x5, [x26, #0x60]
016f7114 +00e8 sub      x5, x5, #0xf
016f7118 +00ec mov      x0, #0xe15c
016f711c +00f0 movk     x0, #3, lsl #16
016f7120 +00f4 stur     x0, [x5, #-1]
016f7124 +00f8 stur     d2, [x5, #7]
016f7128 +00fc stur     x5, [x29, #-0x18]
016f712c +0100 mov      x0, #0
016f7130 +0104 ldur     w1, [x3, #0xb]
016f7134 +0108 sbfx     x6, x1, #1, #0x1f
016f7138 +010c cmp      x4, x6
016f713c +0110 b.ne     #0x16f762c
016f7140 +0114 cmp      x0, x6
016f7144 +0118 b.ge     #0x16f761c
016f7148 +011c ldur     w1, [x3, #0xf]
016f714c +0120 add      x1, x1, x28, lsl #32
016f7150 +0124 add      x16, x1, x0, lsl #2
016f7154 +0128 ldur     w6, [x16, #0xf]
016f7158 +012c add      x6, x6, x28, lsl #32
016f715c +0130 add      x7, x0, #1
016f7160 +0134 stur     x7, [x29, #-0x20]
016f7164 +0138 ldur     w8, [x6, #7]
016f7168 +013c add      x8, x8, x28, lsl #32
016f716c +0140 stur     x8, [x29, #-0x10]
016f7170 +0144 ldur     x0, [x8, #-1]
016f7174 +0148 ubfx     x0, x0, #0xc, #0x14
016f7178 +014c mov      x1, x8
016f717c +0150 sub      x30, x0, #0xe8d
016f7180 +0154 ldr      x30, [x21, x30, lsl #3]
016f7184 +0158 blr      x30
016f7188 +015c mov      x3, x0
016f718c +0160 ldur     x0, [x29, #-0x10]
016f7190 +0164 stur     x3, [x29, #-0x38]
016f7194 +0168 ldur     x1, [x0, #-1]
016f7198 +016c ubfx     x1, x1, #0xc, #0x14
016f719c +0170 cmp      x1, #0x7e7
016f71a0 +0174 b.ne     #0x16f71d8
016f71a4 +0178 mov      x1, x22
016f71a8 +017c mov      x2, #4
016f71ac +0180 bl       #0x1b58288 => (None, None)
016f71b0 +0184 ldur     x1, [x29, #-0x38]
016f71b4 +0188 stur     w1, [x0, #0xf]
016f71b8 +018c add      x16, x27, #0x19, lsl #12
016f71bc +0190 ldr      x16, [x16, #0x708]
016f71c0 +0194 stur     w16, [x0, #0x13]
016f71c4 +0198 str      x0, [x15]
016f71c8 +019c bl       #0x868b8c => ('_StringBase._interpolate', 8817548)
016f71cc +01a0 mov      x1, x0
016f71d0 +01a4 mov      x3, x1
016f71d4 +01a8 b        #0x16f71e0
016f71d8 +01ac mov      x1, x3
016f71dc +01b0 mov      x3, x1
016f71e0 +01b4 ldur     x0, [x29, #-8]
016f71e4 +01b8 stur     x3, [x29, #-0x40]
016f71e8 +01bc ldur     w4, [x0, #0xb]
016f71ec +01c0 add      x4, x4, x28, lsl #32
016f71f0 +01c4 stur     x4, [x29, #-0x38]
016f71f4 +01c8 cmp      w4, w22
016f71f8 +01cc b.eq     #0x16f7678
016f71fc +01d0 mov      x1, x4
016f7200 +01d4 mov      x2, x3
016f7204 +01d8 bl       #0x189304c => ('_LinkedHashMapMixin._getValueOrData', 25768012)
016f7208 +01dc mov      x1, x0
016f720c +01e0 ldur     x0, [x29, #-0x38]
016f7210 +01e4 ldur     w2, [x0, #0xf]
016f7214 +01e8 add      x2, x2, x28, lsl #32
016f7218 +01ec cmp      w2, w1
016f721c +01f0 b.eq     #0x16f75f8
016f7220 +01f4 cmp      w1, w22
016f7224 +01f8 b.eq     #0x16f75f8
016f7228 +01fc ldur     d0, [x29, #-0x60]
016f722c +0200 ldur     d1, [x29, #-0x58]
016f7230 +0204 ldur     d2, [x29, #-0x50]
016f7234 +0208 ldur     x0, [x29, #-0x10]
016f7238 +020c ldur     x1, [x29, #-0x18]
016f723c +0210 ldur     x2, [x0, #0x37]
016f7240 +0214 scvtf    d3, x2
016f7244 +0218 fmul     d4, d3, d0
016f7248 +021c fadd     d3, d2, d4
016f724c +0220 stur     d3, [x29, #-0x70]
016f7250 +0224 ldur     x2, [x0, #0x3f]
016f7254 +0228 scvtf    d4, x2
016f7258 +022c fmul     d5, d4, d1
016f725c +0230 stur     d5, [x29, #-0x68]
016f7260 +0234 ldr      x0, [x26, #0x78]
016f7264 +0238 ldr      x0, [x0, #0x2490]
016f7268 +023c ldr      x16, [x27, #0x40]
016f726c +0240 cmp      w0, w16
016f7270 +0244 b.ne     #0x16f727c
016f7274 +0248 ldr      x2, [x27, #0x60]
016f7278 +024c bl       #0x1b563d0 => (None, None)
016f727c +0250 mov      x1, x22
016f7280 +0254 mov      x2, #8
016f7284 +0258 stur     x0, [x29, #-0x38]
016f7288 +025c bl       #0x1b58288 => (None, None)
016f728c +0260 stur     x0, [x29, #-0x48]
016f7290 +0264 add      x16, x27, #0xa8, lsl #12
016f7294 +0268 ldr      x16, [x16, #0x468]
016f7298 +026c stur     w16, [x0, #0xf]
016f729c +0270 ldur     x1, [x29, #-0x18]
016f72a0 +0274 stur     w1, [x0, #0x13]
016f72a4 +0278 ldr      x16, [x27, #0x33f0]
016f72a8 +027c stur     w16, [x0, #0x17]
016f72ac +0280 ldr      x2, [x26, #0x78]
016f72b0 +0284 ldr      x2, [x2, #0x2710]
016f72b4 +0288 cmp      w2, w22
016f72b8 +028c b.ne     #0x16f72f4
016f72bc +0290 ldr      x0, [x26, #0x78]
016f72c0 +0294 ldr      x0, [x0, #0x1cd0]
016f72c4 +0298 ldr      x16, [x27, #0x40]
016f72c8 +029c cmp      w0, w16
016f72cc +02a0 b.ne     #0x16f72d8
016f72d0 +02a4 ldr      x2, [x27, #0x7800]
016f72d4 +02a8 bl       #0x1b563d0 => (None, None)
016f72d8 +02ac add      x16, x27, #8, lsl #12
016f72dc +02b0 ldr      x16, [x16, #0xfc0]
016f72e0 +02b4 str      x16, [x15]
016f72e4 +02b8 ldr      x4, [x27, #0x50]
016f72e8 +02bc bl       #0xb1f9a8 => ('Inst|find', 11663784)
016f72ec +02c0 mov      x1, x0
016f72f0 +02c4 b        #0x16f72f8
016f72f4 +02c8 mov      x1, x2
016f72f8 +02cc ldur     x2, [x29, #-8]
016f72fc +02d0 ldur     d0, [x29, #-0x60]
016f7300 +02d4 ldur     d1, [x29, #-0x58]
016f7304 +02d8 ldur     d2, [x29, #-0x70]
016f7308 +02dc ldur     d3, [x29, #-0x68]
016f730c +02e0 ldur     x0, [x29, #-0x10]
016f7310 +02e4 bl       #0xb1a278 => ('GridController.currentConfig', 11641464)
016f7314 +02e8 ldur     d0, [x0, #0x2b]
016f7318 +02ec ldp      x0, x1, [x26, #0x60]
016f731c +02f0 add      x0, x0, #0x10
016f7320 +02f4 cmp      x1, x0
016f7324 +02f8 b.ls     #0x16f767c
016f7328 +02fc str      x0, [x26, #0x60]
016f732c +0300 sub      x0, x0, #0xf
016f7330 +0304 mov      x1, #0xe15c
016f7334 +0308 movk     x1, #3, lsl #16
016f7338 +030c stur     x1, [x0, #-1]
016f733c +0310 stur     d0, [x0, #7]
016f7340 +0314 ldur     x1, [x29, #-0x48]
016f7344 +0318 add      x25, x1, #0x1b
016f7348 +031c str      w0, [x25]
016f734c +0320 tbz      w0, #0, #0x16f7368
016f7350 +0324 ldurb    w16, [x1, #-1]
016f7354 +0328 ldurb    w17, [x0, #-1]
016f7358 +032c and      x16, x17, x16, lsr #2
016f735c +0330 tst      x16, x28, lsr #32
016f7360 +0334 b.eq     #0x16f7368
016f7364 +0338 bl       #0x1b56534 => (None, None)
016f7368 +033c ldur     x16, [x29, #-0x48]
016f736c +0340 str      x16, [x15]
016f7370 +0344 bl       #0x868b8c => ('_StringBase._interpolate', 8817548)
016f7374 +0348 ldur     x1, [x29, #-0x38]
016f7378 +034c mov      x5, x0
016f737c +0350 ldr      x2, [x27, #0x78]
016f7380 +0354 add      x3, x27, #0x89, lsl #12
016f7384 +0358 ldr      x3, [x3, #0xdb8]
016f7388 +035c mov      x6, x22
016f738c +0360 mov      x7, x22
016f7390 +0364 bl       #0x86af9c => ('_LoggerImpl._log', 8826780)
016f7394 +0368 mov      x1, x22
016f7398 +036c mov      x2, #8
016f739c +0370 bl       #0x1b58288 => (None, None)
016f73a0 +0374 add      x16, x27, #0xa8, lsl #12
016f73a4 +0378 ldr      x16, [x16, #0x470]
016f73a8 +037c stur     w16, [x0, #0xf]
016f73ac +0380 ldur     d0, [x29, #-0x70]
016f73b0 +0384 ldp      x1, x2, [x26, #0x60]
016f73b4 +0388 add      x1, x1, #0x10
016f73b8 +038c cmp      x2, x1
016f73bc +0390 b.ls     #0x16f768c
016f73c0 +0394 str      x1, [x26, #0x60]
016f73c4 +0398 sub      x1, x1, #0xf
016f73c8 +039c mov      x2, #0xe15c
016f73cc +03a0 movk     x2, #3, lsl #16
016f73d0 +03a4 stur     x2, [x1, #-1]
016f73d4 +03a8 stur     d0, [x1, #7]
016f73d8 +03ac stur     w1, [x0, #0x13]
016f73dc +03b0 ldr      x16, [x27, #0x33f0]
016f73e0 +03b4 stur     w16, [x0, #0x17]
016f73e4 +03b8 ldur     d1, [x29, #-0x68]
016f73e8 +03bc ldp      x1, x2, [x26, #0x60]
016f73ec +03c0 add      x1, x1, #0x10
016f73f0 +03c4 cmp      x2, x1
016f73f4 +03c8 b.ls     #0x16f76a8
016f73f8 +03cc str      x1, [x26, #0x60]
016f73fc +03d0 sub      x1, x1, #0xf
016f7400 +03d4 mov      x2, #0xe15c
016f7404 +03d8 movk     x2, #3, lsl #16
016f7408 +03dc stur     x2, [x1, #-1]
016f740c +03e0 stur     d1, [x1, #7]
016f7410 +03e4 stur     w1, [x0, #0x1b]
016f7414 +03e8 str      x0, [x15]
016f7418 +03ec bl       #0x868b8c => ('_StringBase._interpolate', 8817548)
016f741c +03f0 ldur     x1, [x29, #-0x38]
016f7420 +03f4 mov      x5, x0
016f7424 +03f8 ldr      x2, [x27, #0x78]
016f7428 +03fc add      x3, x27, #0x89, lsl #12
016f742c +0400 ldr      x3, [x3, #0xdb8]
016f7430 +0404 mov      x6, x22
016f7434 +0408 mov      x7, x22
016f7438 +040c bl       #0x86af9c => ('_LoggerImpl._log', 8826780)
016f743c +0410 ldur     x0, [x29, #-0x10]
016f7440 +0414 ldur     x1, [x0, #0x1f]
016f7444 +0418 scvtf    d0, x1
016f7448 +041c ldur     d1, [x29, #-0x60]
016f744c +0420 fmul     d2, d1, d0
016f7450 +0424 stur     d2, [x29, #-0x80]
016f7454 +0428 ldur     x1, [x0, #0x27]
016f7458 +042c scvtf    d0, x1
016f745c +0430 ldur     d3, [x29, #-0x58]
016f7460 +0434 fmul     d4, d3, d0
016f7464 +0438 stur     d4, [x29, #-0x78]
016f7468 +043c bl       #0x191ccf8 => (None, None)
016f746c +0440 ldur     d0, [x29, #-0x80]
016f7470 +0444 stur     x0, [x29, #-0x38]
016f7474 +0448 stur     d0, [x0, #7]
016f7478 +044c stur     d0, [x0, #0xf]
016f747c +0450 ldur     d0, [x29, #-0x78]
016f7480 +0454 stur     d0, [x0, #0x17]
016f7484 +0458 stur     d0, [x0, #0x1f]
016f7488 +045c ldur     x3, [x29, #-8]
016f748c +0460 ldur     w4, [x3, #0xb]
016f7490 +0464 add      x4, x4, x28, lsl #32
016f7494 +0468 stur     x4, [x29, #-0x10]
016f7498 +046c cmp      w4, w22
016f749c +0470 b.eq     #0x16f76c4
016f74a0 +0474 mov      x1, x4
016f74a4 +0478 ldur     x2, [x29, #-0x40]
016f74a8 +047c bl       #0x189304c => ('_LinkedHashMapMixin._getValueOrData', 25768012)
016f74ac +0480 mov      x1, x0
016f74b0 +0484 ldur     x0, [x29, #-0x10]
016f74b4 +0488 ldur     w2, [x0, #0xf]
016f74b8 +048c add      x2, x2, x28, lsl #32
016f74bc +0490 cmp      w2, w1
016f74c0 +0494 b.ne     #0x16f74cc
016f74c4 +0498 mov      x4, x22
016f74c8 +049c b        #0x16f74d0
016f74cc +04a0 mov      x4, x1
016f74d0 +04a4 ldur     x3, [x29, #-8]
016f74d4 +04a8 ldur     d0, [x29, #-0x70]
016f74d8 +04ac ldur     d1, [x29, #-0x68]
016f74dc +04b0 stur     x4, [x29, #-0x10]
016f74e0 +04b4 cmp      w4, w22
016f74e4 +04b8 b.eq     #0x16f76c8
016f74e8 +04bc ldur     x0, [x4, #-1]
016f74ec +04c0 ubfx     x0, x0, #0xc, #0x14
016f74f0 +04c4 add      x16, x22, #0x20
016f74f4 +04c8 str      x16, [x15]
016f74f8 +04cc mov      x1, x4
016f74fc +04d0 ldur     x2, [x29, #-0x38]
016f7500 +04d4 add      x4, x27, #0x47, lsl #12
016f7504 +04d8 ldr      x4, [x4, #0x658]
016f7508 +04dc mov      x17, #0x2aa2
016f750c +04e0 movk     x17, #1, lsl #16
016f7510 +04e4 add      x30, x0, x17
016f7514 +04e8 ldr      x30, [x21, x30, lsl #3]
016f7518 +04ec blr      x30
016f751c +04f0 ldur     x1, [x29, #-0x10]
016f7520 +04f4 bl       #0x8a6bb4 => ('RenderBox.size', 9071540)
016f7524 +04f8 bl       #0x18b600c => (None, None)
016f7528 +04fc ldur     d0, [x29, #-0x70]
016f752c +0500 stur     x0, [x29, #-0x38]
016f7530 +0504 stur     d0, [x0, #7]
016f7534 +0508 ldur     d0, [x29, #-0x68]
016f7538 +050c stur     d0, [x0, #0xf]
016f753c +0510 ldur     x3, [x29, #-8]
016f7540 +0514 ldur     w4, [x3, #0xb]
016f7544 +0518 add      x4, x4, x28, lsl #32
016f7548 +051c stur     x4, [x29, #-0x10]
016f754c +0520 cmp      w4, w22
016f7550 +0524 b.eq     #0x16f76cc
016f7554 +0528 mov      x1, x4
016f7558 +052c ldur     x2, [x29, #-0x40]
016f755c +0530 bl       #0x189304c => ('_LinkedHashMapMixin._getValueOrData', 25768012)
016f7560 +0534 mov      x1, x0
016f7564 +0538 ldur     x0, [x29, #-0x10]
016f7568 +053c ldur     w2, [x0, #0xf]
016f756c +0540 add      x2, x2, x28, lsl #32
016f7570 +0544 cmp      w2, w1
016f7574 +0548 b.ne     #0x16f7580
016f7578 +054c mov      x0, x22
016f757c +0550 b        #0x16f7584
016f7580 +0554 mov      x0, x1
016f7584 +0558 cmp      w0, w22
016f7588 +055c b.eq     #0x16f76d0
016f758c +0560 ldur     w3, [x0, #7]
016f7590 +0564 add      x3, x3, x28, lsl #32
016f7594 +0568 stur     x3, [x29, #-0x10]
016f7598 +056c cmp      w3, w22
016f759c +0570 b.eq     #0x16f76d4
016f75a0 +0574 mov      x0, x3
016f75a4 +0578 mov      x2, x22
016f75a8 +057c mov      x1, x22
016f75ac +0580 ldur     x4, [x0, #-1]
016f75b0 +0584 ubfx     x4, x4, #0xc, #0x14
016f75b4 +0588 mov      x17, #0x158b
016f75b8 +058c cmp      x4, x17
016f75bc +0590 b.eq     #0x16f75d4
016f75c0 +0594 add      x8, x27, #0x77, lsl #12
016f75c4 +0598 ldr      x8, [x8, #0x240]
016f75c8 +059c add      x3, x27, #0xa8, lsl #12
016f75cc +05a0 ldr      x3, [x3, #0x478]
016f75d0 +05a4 bl       #0x1b56180 => (None, None)
016f75d4 +05a8 ldur     x0, [x29, #-0x38]
016f75d8 +05ac ldur     x1, [x29, #-0x10]
016f75dc +05b0 stur     w0, [x1, #7]
016f75e0 +05b4 ldurb    w16, [x1, #-1]
016f75e4 +05b8 ldurb    w17, [x0, #-1]
016f75e8 +05bc and      x16, x17, x16, lsr #2
016f75ec +05c0 tst      x16, x28, lsr #32
016f75f0 +05c4 b.eq     #0x16f75f8
016f75f4 +05c8 bl       #0x1b56978 => (None, None)
016f75f8 +05cc ldur     x0, [x29, #-0x20]
016f75fc +05d0 ldur     x2, [x29, #-8]
016f7600 +05d4 ldur     d0, [x29, #-0x60]
016f7604 +05d8 ldur     d1, [x29, #-0x58]
016f7608 +05dc ldur     x3, [x29, #-0x30]
016f760c +05e0 ldur     d2, [x29, #-0x50]
016f7610 +05e4 ldur     x4, [x29, #-0x28]
016f7614 +05e8 ldur     x5, [x29, #-0x18]
016f7618 +05ec b        #0x16f7130
016f761c +05f0 mov      x0, x22
016f7620 +05f4 mov      x15, x29
016f7624 +05f8 ldp      x29, x30, [x15], #0x10
016f7628 +05fc ret      
016f762c +0600 mov      x0, x3
016f7630 +0604 bl       #0x18a735c => (None, None)
016f7634 +0608 mov      x1, x0
016f7638 +060c ldur     x0, [x29, #-0x30]
016f763c +0610 stur     w0, [x1, #0xb]
016f7640 +0614 mov      x0, x1
016f7644 +0618 bl       #0x1b56510 => (None, None)
016f7648 +061c brk      #0
016f764c +0620 stp      q1, q2, [x15, #-0x20]!
016f7650 +0624 str      q0, [x15, #-0x10]!
016f7654 +0628 stp      x3, x4, [x15, #-0x10]!
016f7658 +062c str      x2, [x15, #-8]!
016f765c +0630 bl       #0x1b581e0 => (None, None)
016f7660 +0634 mov      x5, x0
016f7664 +0638 ldr      x2, [x15], #8
016f7668 +063c ldp      x3, x4, [x15], #0x10
016f766c +0640 ldr      q0, [x15], #0x10
016f7670 +0644 ldp      q1, q2, [x15], #0x20
016f7674 +0648 b        #0x16f7124
016f7678 +064c bl       #0x1b58a18 => (None, None)
016f767c +0650 str      q0, [x15, #-0x10]!
016f7680 +0654 bl       #0x1b581e0 => (None, None)
016f7684 +0658 ldr      q0, [x15], #0x10
016f7688 +065c b        #0x16f733c
016f768c +0660 str      q0, [x15, #-0x10]!
016f7690 +0664 str      x0, [x15, #-8]!
016f7694 +0668 bl       #0x1b581e0 => (None, None)
016f7698 +066c mov      x1, x0
016f769c +0670 ldr      x0, [x15], #8
016f76a0 +0674 ldr      q0, [x15], #0x10
016f76a4 +0678 b        #0x16f73d4
016f76a8 +067c stp      q0, q1, [x15, #-0x20]!
016f76ac +0680 str      x0, [x15, #-8]!
016f76b0 +0684 bl       #0x1b581e0 => (None, None)
016f76b4 +0688 mov      x1, x0
016f76b8 +068c ldr      x0, [x15], #8
016f76bc +0690 ldp      q0, q1, [x15], #0x20
016f76c0 +0694 b        #0x16f740c
016f76c4 +0698 bl       #0x1b58a18 => (None, None)
016f76c8 +069c bl       #0x1b58a64 => (None, None)
016f76cc +06a0 bl       #0x1b58a18 => (None, None)
016f76d0 +06a4 bl       #0x1b58a18 => (None, None)
016f76d4 +06a8 bl       #0x1b58a18 => (None, None)