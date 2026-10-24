016f63f0 +0000 stp      x29, x30, [x15, #-0x10]!
016f63f4 +0004 mov      x29, x15
016f63f8 +0008 sub      x15, x15, #0xa0
016f63fc +000c stur     x1, [x29, #-8]
016f6400 +0010 stur     x2, [x29, #-0x10]
016f6404 +0014 mov      x1, #6
016f6408 +0018 bl       #0x1b571f8 => (None, None)
016f640c +001c mov      x1, x0
016f6410 +0020 ldur     x0, [x29, #-8]
016f6414 +0024 stur     x1, [x29, #-0x18]
016f6418 +0028 stur     w0, [x1, #0xf]
016f641c +002c ldur     x2, [x29, #-0x10]
016f6420 +0030 stur     w2, [x1, #0x13]
016f6424 +0034 ldr      x2, [x26, #0x78]
016f6428 +0038 ldr      x2, [x2, #0x2710]
016f642c +003c cmp      w2, w22
016f6430 +0040 b.ne     #0x16f646c
016f6434 +0044 ldr      x0, [x26, #0x78]
016f6438 +0048 ldr      x0, [x0, #0x1cd0]
016f643c +004c ldr      x16, [x27, #0x40]
016f6440 +0050 cmp      w0, w16
016f6444 +0054 b.ne     #0x16f6450
016f6448 +0058 ldr      x2, [x27, #0x7800]
016f644c +005c bl       #0x1b563d0 => (None, None)
016f6450 +0060 add      x16, x27, #8, lsl #12
016f6454 +0064 ldr      x16, [x16, #0xfc0]
016f6458 +0068 str      x16, [x15]
016f645c +006c ldr      x4, [x27, #0x50]
016f6460 +0070 bl       #0xb1f9a8 => ('Inst|find', 11663784)
016f6464 +0074 mov      x3, x0
016f6468 +0078 b        #0x16f6470
016f646c +007c mov      x3, x2
016f6470 +0080 ldur     x0, [x29, #-8]
016f6474 +0084 ldur     x2, [x29, #-0x18]
016f6478 +0088 mov      x1, x3
016f647c +008c stur     x3, [x29, #-0x10]
016f6480 +0090 bl       #0xa89abc => ('GridController.dockCellWidth', 11049660)
016f6484 +0094 ldp      x0, x1, [x26, #0x60]
016f6488 +0098 add      x0, x0, #0x10
016f648c +009c cmp      x1, x0
016f6490 +00a0 b.ls     #0x16f6aa4
016f6494 +00a4 str      x0, [x26, #0x60]
016f6498 +00a8 sub      x0, x0, #0xf
016f649c +00ac mov      x1, #0xe15c
016f64a0 +00b0 movk     x1, #3, lsl #16
016f64a4 +00b4 stur     x1, [x0, #-1]
016f64a8 +00b8 stur     d0, [x0, #7]
016f64ac +00bc ldur     x2, [x29, #-0x18]
016f64b0 +00c0 stur     w0, [x2, #0x17]
016f64b4 +00c4 ldurb    w16, [x2, #-1]
016f64b8 +00c8 ldurb    w17, [x0, #-1]
016f64bc +00cc and      x16, x17, x16, lsr #2
016f64c0 +00d0 tst      x16, x28, lsr #32
016f64c4 +00d4 b.eq     #0x16f64cc
016f64c8 +00d8 bl       #0x1b56998 => (None, None)
016f64cc +00dc ldur     x1, [x29, #-0x10]
016f64d0 +00e0 bl       #0x8e2c48 => ('GridController.dockCellHeight', 9317448)
016f64d4 +00e4 ldur     x1, [x29, #-0x10]
016f64d8 +00e8 stur     d0, [x29, #-0x70]
016f64dc +00ec bl       #0xb1a278 => ('GridController.currentConfig', 11641464)
016f64e0 +00f0 ldur     w1, [x0, #0x3b]
016f64e4 +00f4 add      x1, x1, x28, lsl #32
016f64e8 +00f8 ldur     d0, [x1, #7]
016f64ec +00fc ldp      x0, x1, [x26, #0x60]
016f64f0 +0100 add      x0, x0, #0x10
016f64f4 +0104 cmp      x1, x0
016f64f8 +0108 b.ls     #0x16f6ab4
016f64fc +010c str      x0, [x26, #0x60]
016f6500 +0110 sub      x0, x0, #0xf
016f6504 +0114 mov      x1, #0xe15c
016f6508 +0118 movk     x1, #3, lsl #16
016f650c +011c stur     x1, [x0, #-1]
016f6510 +0120 stur     d0, [x0, #7]
016f6514 +0124 ldur     x2, [x29, #-0x18]
016f6518 +0128 stur     w0, [x2, #0x1b]
016f651c +012c ldurb    w16, [x2, #-1]
016f6520 +0130 ldurb    w17, [x0, #-1]
016f6524 +0134 and      x16, x17, x16, lsr #2
016f6528 +0138 tst      x16, x28, lsr #32
016f652c +013c b.eq     #0x16f6534
016f6530 +0140 bl       #0x1b56998 => (None, None)
016f6534 +0144 ldur     x1, [x29, #-8]
016f6538 +0148 ldur     w0, [x1, #0x1b]
016f653c +014c add      x0, x0, x28, lsl #32
016f6540 +0150 ldur     d0, [x0, #0xf]
016f6544 +0154 ldp      x0, x3, [x26, #0x60]
016f6548 +0158 add      x0, x0, #0x10
016f654c +015c cmp      x3, x0
016f6550 +0160 b.ls     #0x16f6ac4
016f6554 +0164 str      x0, [x26, #0x60]
016f6558 +0168 sub      x0, x0, #0xf
016f655c +016c mov      x3, #0xe15c
016f6560 +0170 movk     x3, #3, lsl #16
016f6564 +0174 stur     x3, [x0, #-1]
016f6568 +0178 stur     d0, [x0, #7]
016f656c +017c stur     w0, [x2, #0x1f]
016f6570 +0180 ldurb    w16, [x2, #-1]
016f6574 +0184 ldurb    w17, [x0, #-1]
016f6578 +0188 and      x16, x17, x16, lsr #2
016f657c +018c tst      x16, x28, lsr #32
016f6580 +0190 b.eq     #0x16f6588
016f6584 +0194 bl       #0x1b56998 => (None, None)
016f6588 +0198 bl       #0x8b0eb4 => ('HotseatLayerGetxController.to', 9113268)
016f658c +019c ldur     x2, [x29, #-0x18]
016f6590 +01a0 stur     w0, [x2, #0x23]
016f6594 +01a4 tbz      w0, #0, #0x16f65b0
016f6598 +01a8 ldurb    w16, [x2, #-1]
016f659c +01ac ldurb    w17, [x0, #-1]
016f65a0 +01b0 and      x16, x17, x16, lsr #2
016f65a4 +01b4 tst      x16, x28, lsr #32
016f65a8 +01b8 b.eq     #0x16f65b0
016f65ac +01bc bl       #0x1b56998 => (None, None)
016f65b0 +01c0 ldr      x0, [x26, #0x78]
016f65b4 +01c4 ldr      x0, [x0, #0x2490]
016f65b8 +01c8 ldr      x16, [x27, #0x40]
016f65bc +01cc cmp      w0, w16
016f65c0 +01d0 b.ne     #0x16f65cc
016f65c4 +01d4 ldr      x2, [x27, #0x60]
016f65c8 +01d8 bl       #0x1b563d0 => (None, None)
016f65cc +01dc ldur     x2, [x29, #-0x18]
016f65d0 +01e0 add      x1, x27, #0xed, lsl #12
016f65d4 +01e4 ldr      x1, [x1, #0x9b0]
016f65d8 +01e8 stur     x0, [x29, #-0x20]
016f65dc +01ec bl       #0x1b575bc => (None, None)
016f65e0 +01f0 ldur     x1, [x29, #-0x20]
016f65e4 +01f4 mov      x2, x0
016f65e8 +01f8 ldr      x4, [x27, #0x70]
016f65ec +01fc bl       #0x8a8810 => ('_LoggerImpl.info', 9078800)
016f65f0 +0200 ldur     x2, [x29, #-0x18]
016f65f4 +0204 ldur     w3, [x2, #0x13]
016f65f8 +0208 add      x3, x3, x28, lsl #32
016f65fc +020c stur     x3, [x29, #-0x48]
016f6600 +0210 ldur     w0, [x3, #0xb]
016f6604 +0214 sbfx     x4, x0, #1, #0x1f
016f6608 +0218 ldur     x5, [x29, #-8]
016f660c +021c stur     x4, [x29, #-0x40]
016f6610 +0220 ldur     x6, [x5, #0x13]
016f6614 +0224 sbfiz    x0, x6, #1, #0x1f
016f6618 +0228 cmp      x6, x0, asr #1
016f661c +022c b.eq     #0x16f6628
016f6620 +0230 bl       #0x1b58510 => (None, None)
016f6624 +0234 stur     x6, [x0, #7]
016f6628 +0238 stur     x0, [x29, #-0x38]
016f662c +023c ldur     d0, [x5, #0x2b]
016f6630 +0240 stur     d0, [x29, #-0x78]
016f6634 +0244 mov      x1, #0
016f6638 +0248 ldur     d1, [x29, #-0x70]
016f663c +024c stur     x2, [x29, #-0x30]
016f6640 +0250 ldur     w6, [x3, #0xb]
016f6644 +0254 sbfx     x7, x6, #1, #0x1f
016f6648 +0258 cmp      x4, x7
016f664c +025c b.ne     #0x16f6a84
016f6650 +0260 cmp      x1, x7
016f6654 +0264 b.ge     #0x16f6a74
016f6658 +0268 ldur     w6, [x3, #0xf]
016f665c +026c add      x6, x6, x28, lsl #32
016f6660 +0270 add      x16, x6, x1, lsl #2
016f6664 +0274 ldur     w7, [x16, #0xf]
016f6668 +0278 add      x7, x7, x28, lsl #32
016f666c +027c stur     x7, [x29, #-0x18]
016f6670 +0280 add      x6, x1, #1
016f6674 +0284 stur     x6, [x29, #-0x28]
016f6678 +0288 mov      x1, #3
016f667c +028c bl       #0x1b571f8 => (None, None)
016f6680 +0290 mov      x3, x0
016f6684 +0294 ldur     x2, [x29, #-0x30]
016f6688 +0298 stur     x3, [x29, #-0x50]
016f668c +029c stur     w2, [x3, #0xb]
016f6690 +02a0 ldur     x0, [x29, #-0x18]
016f6694 +02a4 stur     w0, [x3, #0xf]
016f6698 +02a8 ldur     w1, [x0, #7]
016f669c +02ac add      x1, x1, x28, lsl #32
016f66a0 +02b0 ldur     x0, [x1, #-1]
016f66a4 +02b4 ubfx     x0, x0, #0xc, #0x14
016f66a8 +02b8 sub      x30, x0, #0xe8d
016f66ac +02bc ldr      x30, [x21, x30, lsl #3]
016f66b0 +02c0 blr      x30
016f66b4 +02c4 mov      x3, x0
016f66b8 +02c8 ldur     x0, [x29, #-8]
016f66bc +02cc stur     x3, [x29, #-0x58]
016f66c0 +02d0 ldur     w4, [x0, #0xb]
016f66c4 +02d4 add      x4, x4, x28, lsl #32
016f66c8 +02d8 stur     x4, [x29, #-0x18]
016f66cc +02dc cmp      w4, w22
016f66d0 +02e0 b.eq     #0x16f6adc
016f66d4 +02e4 mov      x1, x4
016f66d8 +02e8 mov      x2, x3
016f66dc +02ec bl       #0x189304c => ('_LinkedHashMapMixin._getValueOrData', 25768012)
016f66e0 +02f0 mov      x1, x0
016f66e4 +02f4 ldur     x0, [x29, #-0x18]
016f66e8 +02f8 ldur     w2, [x0, #0xf]
016f66ec +02fc add      x2, x2, x28, lsl #32
016f66f0 +0300 cmp      w2, w1
016f66f4 +0304 b.eq     #0x16f6a54
016f66f8 +0308 cmp      w1, w22
016f66fc +030c b.eq     #0x16f6a54
016f6700 +0310 ldur     x2, [x29, #-0x50]
016f6704 +0314 bl       #0x8b0eb4 => ('HotseatLayerGetxController.to', 9113268)
016f6708 +0318 mov      x3, x0
016f670c +031c ldur     x2, [x29, #-0x50]
016f6710 +0320 ldur     w0, [x2, #0xf]
016f6714 +0324 add      x0, x0, x28, lsl #32
016f6718 +0328 ldur     w1, [x0, #7]
016f671c +032c add      x1, x1, x28, lsl #32
016f6720 +0330 ldur     x4, [x1, #0x37]
016f6724 +0334 sbfiz    x0, x4, #1, #0x1f
016f6728 +0338 cmp      x4, x0, asr #1
016f672c +033c b.eq     #0x16f6738
016f6730 +0340 bl       #0x1b58510 => (None, None)
016f6734 +0344 stur     x4, [x0, #7]
016f6738 +0348 ldur     x16, [x29, #-0x38]
016f673c +034c stp      x16, x3, [x15, #8]
016f6740 +0350 str      x0, [x15]
016f6744 +0354 mov      x4, #0
016f6748 +0358 ldr      x0, [x15, #0x10]
016f674c +035c add      x16, x27, #0xed, lsl #12
016f6750 +0360 add      x16, x16, #0x9b8
016f6754 +0364 ldp      x30, x5, [x16]
016f6758 +0368 blr      x30
016f675c +036c ldur     x2, [x29, #-0x50]
016f6760 +0370 stur     w0, [x2, #0x13]
016f6764 +0374 ldurb    w16, [x2, #-1]
016f6768 +0378 ldurb    w17, [x0, #-1]
016f676c +037c and      x16, x17, x16, lsr #2
016f6770 +0380 tst      x16, x28, lsr #32
016f6774 +0384 b.eq     #0x16f677c
016f6778 +0388 bl       #0x1b56998 => (None, None)
016f677c +038c ldur     w0, [x2, #0xf]
016f6780 +0390 add      x0, x0, x28, lsl #32
016f6784 +0394 ldur     w1, [x0, #7]
016f6788 +0398 add      x1, x1, x28, lsl #32
016f678c +039c ldur     x0, [x1, #0x3f]
016f6790 +03a0 stur     x0, [x29, #-0x60]
016f6794 +03a4 ldr      x3, [x26, #0x78]
016f6798 +03a8 ldr      x3, [x3, #0x26d8]
016f679c +03ac cmp      w3, w22
016f67a0 +03b0 b.ne     #0x16f67d4
016f67a4 +03b4 ldr      x0, [x26, #0x78]
016f67a8 +03b8 ldr      x0, [x0, #0x1cd0]
016f67ac +03bc ldr      x16, [x27, #0x40]
016f67b0 +03c0 cmp      w0, w16
016f67b4 +03c4 b.ne     #0x16f67c0
016f67b8 +03c8 ldr      x2, [x27, #0x7800]
016f67bc +03cc bl       #0x1b563d0 => (None, None)
016f67c0 +03d0 ldr      x16, [x27, #0x7808]
016f67c4 +03d4 str      x16, [x15]
016f67c8 +03d8 ldr      x4, [x27, #0x50]
016f67cc +03dc bl       #0xb1f9a8 => ('Inst|find', 11663784)
016f67d0 +03e0 b        #0x16f67d8
016f67d4 +03e4 mov      x0, x3
016f67d8 +03e8 ldur     w1, [x0, #0xeb]
016f67dc +03ec add      x1, x1, x28, lsl #32
016f67e0 +03f0 tbnz     w1, #4, #0x16f67f8
016f67e4 +03f4 ldur     x1, [x29, #-0x10]
016f67e8 +03f8 bl       #0xb1a278 => ('GridController.currentConfig', 11641464)
016f67ec +03fc ldur     d0, [x0, #0x77]
016f67f0 +0400 mov      v2.16b, v0.16b
016f67f4 +0404 b        #0x16f6808
016f67f8 +0408 ldur     x1, [x29, #-0x10]
016f67fc +040c bl       #0xb1a278 => ('GridController.currentConfig', 11641464)
016f6800 +0410 ldur     d0, [x0, #0x33]
016f6804 +0414 mov      v2.16b, v0.16b
016f6808 +0418 ldur     x1, [x29, #-8]
016f680c +041c ldur     d1, [x29, #-0x70]
016f6810 +0420 ldur     x3, [x29, #-0x30]
016f6814 +0424 ldur     x2, [x29, #-0x50]
016f6818 +0428 ldur     x0, [x29, #-0x60]
016f681c +042c ldur     d0, [x29, #-0x78]
016f6820 +0430 scvtf    d3, x0
016f6824 +0434 fmul     d4, d3, d2
016f6828 +0438 fadd     d2, d4, d0
016f682c +043c stur     d2, [x29, #-0x88]
016f6830 +0440 ldp      x0, x4, [x26, #0x60]
016f6834 +0444 add      x0, x0, #0x10
016f6838 +0448 cmp      x4, x0
016f683c +044c b.ls     #0x16f6ae0
016f6840 +0450 str      x0, [x26, #0x60]
016f6844 +0454 sub      x0, x0, #0xf
016f6848 +0458 mov      x4, #0xe15c
016f684c +045c movk     x4, #3, lsl #16
016f6850 +0460 stur     x4, [x0, #-1]
016f6854 +0464 stur     d2, [x0, #7]
016f6858 +0468 stur     w0, [x2, #0x17]
016f685c +046c ldurb    w16, [x2, #-1]
016f6860 +0470 ldurb    w17, [x0, #-1]
016f6864 +0474 and      x16, x17, x16, lsr #2
016f6868 +0478 tst      x16, x28, lsr #32
016f686c +047c b.eq     #0x16f6874
016f6870 +0480 bl       #0x1b56998 => (None, None)
016f6874 +0484 ldur     w0, [x3, #0x17]
016f6878 +0488 add      x0, x0, x28, lsl #32
016f687c +048c ldur     d3, [x0, #7]
016f6880 +0490 stur     d3, [x29, #-0x80]
016f6884 +0494 bl       #0x191ccf8 => (None, None)
016f6888 +0498 ldur     d0, [x29, #-0x80]
016f688c +049c stur     x0, [x29, #-0x68]
016f6890 +04a0 stur     d0, [x0, #7]
016f6894 +04a4 stur     d0, [x0, #0xf]
016f6898 +04a8 ldur     d0, [x29, #-0x70]
016f689c +04ac stur     d0, [x0, #0x17]
016f68a0 +04b0 stur     d0, [x0, #0x1f]
016f68a4 +04b4 ldur     x3, [x29, #-8]
016f68a8 +04b8 ldur     w4, [x3, #0xb]
016f68ac +04bc add      x4, x4, x28, lsl #32
016f68b0 +04c0 stur     x4, [x29, #-0x18]
016f68b4 +04c4 cmp      w4, w22
016f68b8 +04c8 b.eq     #0x16f6b08
016f68bc +04cc mov      x1, x4
016f68c0 +04d0 ldur     x2, [x29, #-0x58]
016f68c4 +04d4 bl       #0x189304c => ('_LinkedHashMapMixin._getValueOrData', 25768012)
016f68c8 +04d8 mov      x3, x0
016f68cc +04dc ldur     x0, [x29, #-0x18]
016f68d0 +04e0 ldur     w1, [x0, #0xf]
016f68d4 +04e4 add      x1, x1, x28, lsl #32
016f68d8 +04e8 cmp      w1, w3
016f68dc +04ec b.ne     #0x16f68e8
016f68e0 +04f0 mov      x5, x22
016f68e4 +04f4 b        #0x16f68ec
016f68e8 +04f8 mov      x5, x3
016f68ec +04fc ldur     x3, [x29, #-8]
016f68f0 +0500 ldur     x4, [x29, #-0x50]
016f68f4 +0504 ldur     d0, [x29, #-0x88]
016f68f8 +0508 stur     x5, [x29, #-0x18]
016f68fc +050c cmp      w5, w22
016f6900 +0510 b.eq     #0x16f6b0c
016f6904 +0514 ldur     x0, [x5, #-1]
016f6908 +0518 ubfx     x0, x0, #0xc, #0x14
016f690c +051c add      x16, x22, #0x20
016f6910 +0520 str      x16, [x15]
016f6914 +0524 mov      x1, x5
016f6918 +0528 ldur     x2, [x29, #-0x68]
016f691c +052c add      x4, x27, #0x47, lsl #12
016f6920 +0530 ldr      x4, [x4, #0x658]
016f6924 +0534 mov      x17, #0x2aa2
016f6928 +0538 movk     x17, #1, lsl #16
016f692c +053c add      x30, x0, x17
016f6930 +0540 ldr      x30, [x21, x30, lsl #3]
016f6934 +0544 blr      x30
016f6938 +0548 ldur     x1, [x29, #-0x18]
016f693c +054c bl       #0x8a6bb4 => ('RenderBox.size', 9071540)
016f6940 +0550 ldur     x2, [x29, #-0x50]
016f6944 +0554 ldur     w0, [x2, #0x13]
016f6948 +0558 add      x0, x0, x28, lsl #32
016f694c +055c ldur     d0, [x0, #7]
016f6950 +0560 stur     d0, [x29, #-0x80]
016f6954 +0564 bl       #0x18b600c => (None, None)
016f6958 +0568 ldur     d0, [x29, #-0x80]
016f695c +056c stur     x0, [x29, #-0x68]
016f6960 +0570 stur     d0, [x0, #7]
016f6964 +0574 ldur     d0, [x29, #-0x88]
016f6968 +0578 stur     d0, [x0, #0xf]
016f696c +057c ldur     x3, [x29, #-8]
016f6970 +0580 ldur     w4, [x3, #0xb]
016f6974 +0584 add      x4, x4, x28, lsl #32
016f6978 +0588 stur     x4, [x29, #-0x18]
016f697c +058c cmp      w4, w22
016f6980 +0590 b.eq     #0x16f6b10
016f6984 +0594 mov      x1, x4
016f6988 +0598 ldur     x2, [x29, #-0x58]
016f698c +059c bl       #0x189304c => ('_LinkedHashMapMixin._getValueOrData', 25768012)
016f6990 +05a0 mov      x3, x0
016f6994 +05a4 ldur     x0, [x29, #-0x18]
016f6998 +05a8 ldur     w1, [x0, #0xf]
016f699c +05ac add      x1, x1, x28, lsl #32
016f69a0 +05b0 cmp      w1, w3
016f69a4 +05b4 b.ne     #0x16f69b0
016f69a8 +05b8 mov      x0, x22
016f69ac +05bc b        #0x16f69b4
016f69b0 +05c0 mov      x0, x3
016f69b4 +05c4 cmp      w0, w22
016f69b8 +05c8 b.eq     #0x16f6b14
016f69bc +05cc ldur     w3, [x0, #7]
016f69c0 +05d0 add      x3, x3, x28, lsl #32
016f69c4 +05d4 stur     x3, [x29, #-0x18]
016f69c8 +05d8 cmp      w3, w22
016f69cc +05dc b.eq     #0x16f6b18
016f69d0 +05e0 mov      x0, x3
016f69d4 +05e4 mov      x2, x22
016f69d8 +05e8 mov      x1, x22
016f69dc +05ec ldur     x4, [x0, #-1]
016f69e0 +05f0 ubfx     x4, x4, #0xc, #0x14
016f69e4 +05f4 mov      x17, #0x158b
016f69e8 +05f8 cmp      x4, x17
016f69ec +05fc b.eq     #0x16f6a04
016f69f0 +0600 add      x8, x27, #0x77, lsl #12
016f69f4 +0604 ldr      x8, [x8, #0x240]
016f69f8 +0608 add      x3, x27, #0xed, lsl #12
016f69fc +060c ldr      x3, [x3, #0x9c8]
016f6a00 +0610 bl       #0x1b56180 => (None, None)
016f6a04 +0614 ldur     x0, [x29, #-0x68]
016f6a08 +0618 ldur     x1, [x29, #-0x18]
016f6a0c +061c stur     w0, [x1, #7]
016f6a10 +0620 ldurb    w16, [x1, #-1]
016f6a14 +0624 ldurb    w17, [x0, #-1]
016f6a18 +0628 and      x16, x17, x16, lsr #2
016f6a1c +062c tst      x16, x28, lsr #32
016f6a20 +0630 b.eq     #0x16f6a28
016f6a24 +0634 bl       #0x1b56978 => (None, None)
016f6a28 +0638 ldur     x2, [x29, #-0x50]
016f6a2c +063c add      x1, x27, #0xed, lsl #12
016f6a30 +0640 ldr      x1, [x1, #0x9d8]
016f6a34 +0644 bl       #0x1b575bc => (None, None)
016f6a38 +0648 ldur     x1, [x29, #-0x20]
016f6a3c +064c mov      x5, x0
016f6a40 +0650 ldr      x2, [x27, #0xd58]
016f6a44 +0654 mov      x3, x22
016f6a48 +0658 mov      x6, x22
016f6a4c +065c mov      x7, x22
016f6a50 +0660 bl       #0x86af9c => ('_LoggerImpl._log', 8826780)
016f6a54 +0664 ldur     x2, [x29, #-0x30]
016f6a58 +0668 ldur     x1, [x29, #-0x28]
016f6a5c +066c ldur     x5, [x29, #-8]
016f6a60 +0670 ldur     x3, [x29, #-0x48]
016f6a64 +0674 ldur     d0, [x29, #-0x78]
016f6a68 +0678 ldur     x4, [x29, #-0x40]
016f6a6c +067c ldur     x0, [x29, #-0x38]
016f6a70 +0680 b        #0x16f6638
016f6a74 +0684 mov      x0, x22
016f6a78 +0688 mov      x15, x29
016f6a7c +068c ldp      x29, x30, [x15], #0x10
016f6a80 +0690 ret      
016f6a84 +0694 mov      x0, x3
016f6a88 +0698 bl       #0x18a735c => (None, None)
016f6a8c +069c mov      x1, x0
016f6a90 +06a0 ldur     x0, [x29, #-0x48]
016f6a94 +06a4 stur     w0, [x1, #0xb]
016f6a98 +06a8 mov      x0, x1
016f6a9c +06ac bl       #0x1b56510 => (None, None)
016f6aa0 +06b0 brk      #0
016f6aa4 +06b4 str      q0, [x15, #-0x10]!
016f6aa8 +06b8 bl       #0x1b581e0 => (None, None)
016f6aac +06bc ldr      q0, [x15], #0x10
016f6ab0 +06c0 b        #0x16f64a8
016f6ab4 +06c4 str      q0, [x15, #-0x10]!
016f6ab8 +06c8 bl       #0x1b581e0 => (None, None)
016f6abc +06cc ldr      q0, [x15], #0x10
016f6ac0 +06d0 b        #0x16f6510
016f6ac4 +06d4 str      q0, [x15, #-0x10]!
016f6ac8 +06d8 stp      x1, x2, [x15, #-0x10]!
016f6acc +06dc bl       #0x1b581e0 => (None, None)
016f6ad0 +06e0 ldp      x1, x2, [x15], #0x10
016f6ad4 +06e4 ldr      q0, [x15], #0x10
016f6ad8 +06e8 b        #0x16f6568
016f6adc +06ec bl       #0x1b58a18 => (None, None)
016f6ae0 +06f0 stp      q1, q2, [x15, #-0x20]!
016f6ae4 +06f4 str      q0, [x15, #-0x10]!
016f6ae8 +06f8 stp      x2, x3, [x15, #-0x10]!
016f6aec +06fc str      x1, [x15, #-8]!
016f6af0 +0700 bl       #0x1b581e0 => (None, None)
016f6af4 +0704 ldr      x1, [x15], #8
016f6af8 +0708 ldp      x2, x3, [x15], #0x10
016f6afc +070c ldr      q0, [x15], #0x10
016f6b00 +0710 ldp      q1, q2, [x15], #0x20
016f6b04 +0714 b        #0x16f6854
016f6b08 +0718 bl       #0x1b58a64 => (None, None)
016f6b0c +071c bl       #0x1b58a64 => (None, None)
016f6b10 +0720 bl       #0x1b58a18 => (None, None)
016f6b14 +0724 bl       #0x1b58a18 => (None, None)
016f6b18 +0728 bl       #0x1b58a18 => (None, None)