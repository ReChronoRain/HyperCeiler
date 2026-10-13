01927624 +0000 stp      x29, x30, [x15, #-0x10]!
01927628 +0004 mov      x29, x15
0192762c +0008 sub      x15, x15, #0xf0
01927630 +000c stur     x22, [x29, #-8]
01927634 +0010 stur     x1, [x29, #-0xa8]
01927638 +0014 stur     x2, [x29, #-0xb0]
0192763c +0018 stur     x3, [x29, #-0xb8]
01927640 +001c mov      x1, #0xb
01927644 +0020 bl       #0x1b571f8 => (None, None)
01927648 +0024 mov      x2, x0
0192764c +0028 ldur     x1, [x29, #-0xa8]
01927650 +002c stur     x2, [x29, #-0xc0]
01927654 +0030 stur     w1, [x2, #0xf]
01927658 +0034 ldur     x0, [x29, #-0xb0]
0192765c +0038 lsl      x3, x0, #1
01927660 +003c stur     w3, [x2, #0x13]
01927664 +0040 ldur     x0, [x29, #-0xb8]
01927668 +0044 stur     w0, [x2, #0x17]
0192766c +0048 ldr      x0, [x27, #0x58]
01927670 +004c bl       #0x18acb58 => (None, None)
01927674 +0050 ldr      x0, [x26, #0x78]
01927678 +0054 ldr      x0, [x0, #0x2490]
0192767c +0058 ldr      x16, [x27, #0x40]
01927680 +005c cmp      w0, w16
01927684 +0060 b.ne     #0x1927690
01927688 +0064 ldr      x2, [x27, #0x60]
0192768c +0068 bl       #0x1b563d0 => (None, None)
01927690 +006c ldur     x2, [x29, #-0xc0]
01927694 +0070 add      x1, x27, #0x27, lsl #12
01927698 +0074 ldr      x1, [x1, #0x2a8]
0192769c +0078 stur     x0, [x29, #-0xb8]
019276a0 +007c bl       #0x1b575bc => (None, None)
019276a4 +0080 ldur     x1, [x29, #-0xb8]
019276a8 +0084 mov      x2, x0
019276ac +0088 ldr      x4, [x27, #0x70]
019276b0 +008c bl       #0x89c194 => ('_LoggerImpl.d', 9027988)
019276b4 +0090 ldur     x1, [x29, #-0xa8]
019276b8 +0094 ldur     w0, [x1, #0x57]
019276bc +0098 add      x0, x0, x28, lsl #32
019276c0 +009c stur     x0, [x29, #-0xc8]
019276c4 +00a0 bl       #0xa4c210 => ('Utilities.getStatusBarHeight', 10797584)
019276c8 +00a4 ldp      x2, x0, [x26, #0x60]
019276cc +00a8 add      x2, x2, #0x10
019276d0 +00ac cmp      x0, x2
019276d4 +00b0 b.ls     #0x1929100
019276d8 +00b4 str      x2, [x26, #0x60]
019276dc +00b8 sub      x2, x2, #0xf
019276e0 +00bc mov      x0, #0xe15c
019276e4 +00c0 movk     x0, #3, lsl #16
019276e8 +00c4 stur     x0, [x2, #-1]
019276ec +00c8 stur     d0, [x2, #7]
019276f0 +00cc ldur     x1, [x29, #-0xc8]
019276f4 +00d0 bl       #0xb1956c => ('RxObjectMixin.value=', 11638124)
019276f8 +00d4 ldur     x1, [x29, #-0xa8]
019276fc +00d8 ldur     w0, [x1, #0x1f]
01927700 +00dc add      x0, x0, x28, lsl #32
01927704 +00e0 stur     x0, [x29, #-0xc8]
01927708 +00e4 ldr      x2, [x26, #0x78]
0192770c +00e8 ldr      x2, [x2, #0x26d8]
01927710 +00ec cmp      w2, w22
01927714 +00f0 b.ne     #0x1927748
01927718 +00f4 ldr      x0, [x26, #0x78]
0192771c +00f8 ldr      x0, [x0, #0x1cd0]
01927720 +00fc ldr      x16, [x27, #0x40]
01927724 +0100 cmp      w0, w16
01927728 +0104 b.ne     #0x1927734
0192772c +0108 ldr      x2, [x27, #0x7800]
01927730 +010c bl       #0x1b563d0 => (None, None)
01927734 +0110 ldr      x16, [x27, #0x7808]
01927738 +0114 str      x16, [x15]
0192773c +0118 ldr      x4, [x27, #0x50]
01927740 +011c bl       #0xb1f9a8 => ('Inst|find', 11663784)
01927744 +0120 mov      x2, x0
01927748 +0124 ldur     x1, [x29, #-0xa8]
0192774c +0128 ldur     x0, [x29, #-0xc8]
01927750 +012c ldur     w3, [x2, #0x27]
01927754 +0130 add      x3, x3, x28, lsl #32
01927758 +0134 stur     w3, [x0, #0xdf]
0192775c +0138 ldur     w0, [x1, #0x1f]
01927760 +013c add      x0, x0, x28, lsl #32
01927764 +0140 stur     x0, [x29, #-0xc8]
01927768 +0144 ldr      x2, [x26, #0x78]
0192776c +0148 ldr      x2, [x2, #0x26d8]
01927770 +014c cmp      w2, w22
01927774 +0150 b.ne     #0x19277a8
01927778 +0154 ldr      x0, [x26, #0x78]
0192777c +0158 ldr      x0, [x0, #0x1cd0]
01927780 +015c ldr      x16, [x27, #0x40]
01927784 +0160 cmp      w0, w16
01927788 +0164 b.ne     #0x1927794
0192778c +0168 ldr      x2, [x27, #0x7800]
01927790 +016c bl       #0x1b563d0 => (None, None)
01927794 +0170 ldr      x16, [x27, #0x7808]
01927798 +0174 str      x16, [x15]
0192779c +0178 ldr      x4, [x27, #0x50]
019277a0 +017c bl       #0xb1f9a8 => ('Inst|find', 11663784)
019277a4 +0180 mov      x2, x0
019277a8 +0184 ldur     x0, [x29, #-0xa8]
019277ac +0188 ldur     x1, [x29, #-0xc8]
019277b0 +018c ldur     w3, [x2, #0x2b]
019277b4 +0190 add      x3, x3, x28, lsl #32
019277b8 +0194 stur     w3, [x1, #0xe3]
019277bc +0198 ldur     w2, [x0, #0x1f]
019277c0 +019c add      x2, x2, x28, lsl #32
019277c4 +01a0 mov      x1, x0
019277c8 +01a4 stur     x2, [x29, #-0xc8]
019277cc +01a8 bl       #0xa458a0 => ('GridConfig.getScreenMarginTop', 10770592)
019277d0 +01ac ldur     x0, [x29, #-0xc8]
019277d4 +01b0 stur     d0, [x0, #0x27]
019277d8 +01b4 ldur     x0, [x29, #-0xa8]
019277dc +01b8 ldur     w1, [x0, #0x1f]
019277e0 +01bc add      x1, x1, x28, lsl #32
019277e4 +01c0 bl       #0xa4c200 => ('GridSizeCalRules.updateCalGridUsingNav', 10797568)
019277e8 +01c4 ldur     x0, [x29, #-0xa8]
019277ec +01c8 ldur     w2, [x0, #0x1f]
019277f0 +01cc add      x2, x2, x28, lsl #32
019277f4 +01d0 mov      x1, x0
019277f8 +01d4 stur     x2, [x29, #-0xc8]
019277fc +01d8 bl       #0xa4c178 => ('GridConfig.getScreenMarginBottom', 10797432)
01927800 +01dc ldur     x0, [x29, #-0xc8]
01927804 +01e0 stur     d0, [x0, #0xb7]
01927808 +01e4 ldur     x0, [x29, #-0xa8]
0192780c +01e8 ldur     w2, [x0, #0x1f]
01927810 +01ec add      x2, x2, x28, lsl #32
01927814 +01f0 stur     x2, [x29, #-0xc8]
01927818 +01f4 ldur     x1, [x2, #-1]
0192781c +01f8 ubfx     x1, x1, #0xc, #0x14
01927820 +01fc cmp      x1, #0x785
01927824 +0200 b.ne     #0x192784c
01927828 +0204 mov      x1, x2
0192782c +0208 bl       #0xa4c108 => ('GridSizeCalRules.showCapsuleIndicator', 10797320)
01927830 +020c tbnz     w0, #4, #0x192783c
01927834 +0210 fmov     d0, #30.00000000
01927838 +0214 b        #0x1927840
0192783c +0218 fmov     d0, #16.00000000
01927840 +021c ldur     x0, [x29, #-0xc8]
01927844 +0220 stur     d0, [x0, #0x5f]
01927848 +0224 b        #0x1927898
0192784c +0228 mov      x0, x2
01927850 +022c mov      x1, x0
01927854 +0230 add      x2, x27, #0x27, lsl #12
01927858 +0234 ldr      x2, [x2, #0x2b0]
0192785c +0238 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01927860 +023c ldur     x1, [x29, #-0xc8]
01927864 +0240 bl       #0x1854f0c => ('GridSizeCalRules.getStableIndicatorHeight', 25513740)
01927868 +0244 mov      x0, x22
0192786c +0248 mov      x2, x22
01927870 +024c mov      x1, x22
01927874 +0250 ldur     x4, [x0, #-1]
01927878 +0254 ubfx     x4, x4, #0xc, #0x14
0192787c +0258 cmp      x4, #0xef3
01927880 +025c b.eq     #0x1927898
01927884 +0260 add      x8, x27, #0x27, lsl #12
01927888 +0264 ldr      x8, [x8, #0x2b8]
0192788c +0268 add      x3, x27, #0x27, lsl #12
01927890 +026c ldr      x3, [x3, #0x2c0]
01927894 +0270 bl       #0x1b56180 => (None, None)
01927898 +0274 ldur     x1, [x29, #-0xa8]
0192789c +0278 ldur     x2, [x29, #-0xc0]
019278a0 +027c ldur     w0, [x1, #0x1f]
019278a4 +0280 add      x0, x0, x28, lsl #32
019278a8 +0284 stur     x0, [x29, #-0xc8]
019278ac +0288 ldr      x0, [x26, #0x78]
019278b0 +028c ldr      x0, [x0, #0x3da0]
019278b4 +0290 ldr      x16, [x27, #0x40]
019278b8 +0294 cmp      w0, w16
019278bc +0298 b.ne     #0x19278cc
019278c0 +029c add      x2, x27, #0x27, lsl #12
019278c4 +02a0 ldr      x2, [x2, #0x2d0]
019278c8 +02a4 bl       #0x1b563d0 => (None, None)
019278cc +02a8 mov      x1, x0
019278d0 +02ac bl       #0xa467c0 => ('HomeStateManager.showSearchBar', 10774464)
019278d4 +02b0 mov      x1, x0
019278d8 +02b4 ldur     x0, [x29, #-0xc8]
019278dc +02b8 stur     w1, [x0, #0xdb]
019278e0 +02bc ldur     x2, [x29, #-0xc0]
019278e4 +02c0 ldur     w0, [x2, #0x13]
019278e8 +02c4 sbfx     x3, x0, #1, #0x1f
019278ec +02c8 cmp      x3, #6
019278f0 +02cc b.gt     #0x1927a98
019278f4 +02d0 cmp      x3, #3
019278f8 +02d4 b.gt     #0x19279a8
019278fc +02d8 cmp      x3, #2
01927900 +02dc b.gt     #0x192791c
01927904 +02e0 cmp      x3, #1
01927908 +02e4 b.gt     #0x19284c8
0192790c +02e8 cmp      w0, #2
01927910 +02ec b.ne     #0x1928440
01927914 +02f0 mov      x4, x2
01927918 +02f4 b        #0x1927aac
0192791c +02f8 ldur     x0, [x29, #-0xa8]
01927920 +02fc mov      x1, x0
01927924 +0300 bl       #0x1929d00 => ('GridConfig._loadCellConfigFromController', 26385664)
01927928 +0304 mov      x1, x0
0192792c +0308 stur     x1, [x29, #-0xc8]
01927930 +030c bl       #0x18ac91c => (None, None)
01927934 +0310 ldur     x0, [x29, #-0xa8]
01927938 +0314 ldur     w2, [x0, #0x1f]
0192793c +0318 add      x2, x2, x28, lsl #32
01927940 +031c stur     x2, [x29, #-0xc8]
01927944 +0320 ldur     w1, [x0, #0x5b]
01927948 +0324 add      x1, x1, x28, lsl #32
0192794c +0328 bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
01927950 +032c ldur     x4, [x29, #-0xc0]
01927954 +0330 ldur     w1, [x4, #0x13]
01927958 +0334 ldur     x2, [x29, #-0xc8]
0192795c +0338 ldur     x3, [x2, #-1]
01927960 +033c ubfx     x3, x3, #0xc, #0x14
01927964 +0340 cmp      x3, #0x785
01927968 +0344 b.ne     #0x1927994
0192796c +0348 sbfx     x3, x0, #1, #0x1f
01927970 +034c tbz      w0, #0, #0x1927978
01927974 +0350 ldur     x3, [x0, #7]
01927978 +0354 sbfx     x0, x1, #1, #0x1f
0192797c +0358 mov      x1, x2
01927980 +035c mov      x2, x3
01927984 +0360 mov      x3, x0
01927988 +0364 mov      x5, #-1
0192798c +0368 bl       #0xa4b5d4 => ('PhoneDeviceRules._calGridSizeByVariable', 10794452)
01927990 +036c b        #0x19284c8
01927994 +0370 mov      x1, x2
01927998 +0374 add      x2, x27, #0x27, lsl #12
0192799c +0378 ldr      x2, [x2, #0x2d8]
019279a0 +037c bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
019279a4 +0380 b        #0x19284c8
019279a8 +0384 mov      x4, x2
019279ac +0388 cmp      x3, #5
019279b0 +038c b.gt     #0x1928440
019279b4 +0390 cmp      x3, #4
019279b8 +0394 b.gt     #0x1928440
019279bc +0398 ldur     w0, [x4, #0x17]
019279c0 +039c add      x0, x0, x28, lsl #32
019279c4 +03a0 stur     x0, [x29, #-0xc8]
019279c8 +03a4 mov      x1, #0x3c
019279cc +03a8 tbz      w0, #0, #0x19279d8
019279d0 +03ac ldur     x1, [x0, #-1]
019279d4 +03b0 ubfx     x1, x1, #0xc, #0x14
019279d8 +03b4 cmp      x1, #0x766
019279dc +03b8 b.ne     #0x19284c8
019279e0 +03bc ldur     x4, [x29, #-0xa8]
019279e4 +03c0 ldur     w1, [x4, #0x1f]
019279e8 +03c4 add      x1, x1, x28, lsl #32
019279ec +03c8 cmp      w0, w22
019279f0 +03cc b.eq     #0x1929114
019279f4 +03d0 ldur     x2, [x0, #7]
019279f8 +03d4 ldur     x5, [x0, #0x1f]
019279fc +03d8 ldur     x6, [x1, #-1]
01927a00 +03dc ubfx     x6, x6, #0xc, #0x14
01927a04 +03e0 cmp      x6, #0x785
01927a08 +03e4 b.ne     #0x1927a14
01927a0c +03e8 bl       #0xa4b5d4 => ('PhoneDeviceRules._calGridSizeByVariable', 10794452)
01927a10 +03ec b        #0x1927a20
01927a14 +03f0 add      x2, x27, #0x27, lsl #12
01927a18 +03f4 ldr      x2, [x2, #0x2d8]
01927a1c +03f8 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01927a20 +03fc ldur     x0, [x29, #-0xa8]
01927a24 +0400 ldur     w3, [x0, #0x1f]
01927a28 +0404 add      x3, x3, x28, lsl #32
01927a2c +0408 stur     x3, [x29, #-0xd0]
01927a30 +040c ldur     x1, [x3, #-1]
01927a34 +0410 ubfx     x1, x1, #0xc, #0x14
01927a38 +0414 cmp      x1, #0x785
01927a3c +0418 b.ne     #0x1927a4c
01927a40 +041c ldur     w1, [x3, #0x43]
01927a44 +0420 add      x1, x1, x28, lsl #32
01927a48 +0424 b        #0x1927a78
01927a4c +0428 mov      x1, x3
01927a50 +042c add      x2, x27, #0x27, lsl #12
01927a54 +0430 ldr      x2, [x2, #0x2e0]
01927a58 +0434 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01927a5c +0438 ldur     x1, [x29, #-0xd0]
01927a60 +043c add      x2, x27, #0xe, lsl #12
01927a64 +0440 ldr      x2, [x2, #0xfa0]
01927a68 +0444 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01927a6c +0448 ldur     x0, [x29, #-0xd0]
01927a70 +044c ldur     w1, [x0, #0x43]
01927a74 +0450 add      x1, x1, x28, lsl #32
01927a78 +0454 ldur     x0, [x29, #-0xc8]
01927a7c +0458 bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
01927a80 +045c sbfx     x1, x0, #1, #0x1f
01927a84 +0460 tbz      w0, #0, #0x1927a8c
01927a88 +0464 ldur     x1, [x0, #7]
01927a8c +0468 ldur     x0, [x29, #-0xc8]
01927a90 +046c stur     x1, [x0, #0xf]
01927a94 +0470 b        #0x19284c8
01927a98 +0474 mov      x4, x2
01927a9c +0478 cmp      x3, #9
01927aa0 +047c b.gt     #0x19281f0
01927aa4 +0480 cmp      x3, #8
01927aa8 +0484 b.le     #0x1928440
01927aac +0488 ldur     x1, [x29, #-0xb8]
01927ab0 +048c add      x2, x27, #0x27, lsl #12
01927ab4 +0490 ldr      x2, [x2, #0x2e8]
01927ab8 +0494 ldr      x4, [x27, #0x70]
01927abc +0498 bl       #0x89c194 => ('_LoggerImpl.d', 9027988)
01927ac0 +049c ldr      x0, [x26, #0x78]
01927ac4 +04a0 ldr      x0, [x0, #0x2700]
01927ac8 +04a4 ldr      x16, [x27, #0x40]
01927acc +04a8 cmp      w0, w16
01927ad0 +04ac b.ne     #0x1927ae0
01927ad4 +04b0 add      x2, x27, #0x10, lsl #12
01927ad8 +04b4 ldr      x2, [x2, #0x830]
01927adc +04b8 bl       #0x1b563d0 => (None, None)
01927ae0 +04bc mov      x1, x0
01927ae4 +04c0 bl       #0x1926184 => ('SystemPropertyProvider.reloadAccessibilityStatus', 26370436)
01927ae8 +04c4 mov      x1, x0
01927aec +04c8 stur     x1, [x29, #-0xc8]
01927af0 +04cc bl       #0x18ac91c => (None, None)
01927af4 +04d0 ldur     x2, [x29, #-0xc0]
01927af8 +04d4 ldur     w0, [x2, #0x13]
01927afc +04d8 cmp      w0, #0x12
01927b00 +04dc b.ne     #0x192816c
01927b04 +04e0 stur     w22, [x2, #0x1b]
01927b08 +04e4 stur     w22, [x2, #0x1f]
01927b0c +04e8 stur     w22, [x2, #0x2b]
01927b10 +04ec stur     w22, [x2, #0x2f]
01927b14 +04f0 bl       #0xa4b33c => ('getCurrentRotateScreenSize', 10793788)
01927b18 +04f4 mov      x1, x0
01927b1c +04f8 stur     x1, [x29, #-0xc8]
01927b20 +04fc bl       #0x18ac91c => (None, None)
01927b24 +0500 mov      x1, x0
01927b28 +0504 ldur     w2, [x1, #0xf]
01927b2c +0508 add      x2, x2, x28, lsl #32
01927b30 +050c mov      x0, x2
01927b34 +0510 ldur     x3, [x29, #-0xc0]
01927b38 +0514 stur     w0, [x3, #0x2b]
01927b3c +0518 tbz      w0, #0, #0x1927b58
01927b40 +051c ldurb    w16, [x3, #-1]
01927b44 +0520 ldurb    w17, [x0, #-1]
01927b48 +0524 and      x16, x17, x16, lsr #2
01927b4c +0528 tst      x16, x28, lsr #32
01927b50 +052c b.eq     #0x1927b58
01927b54 +0530 bl       #0x1b569b8 => (None, None)
01927b58 +0534 ldur     w4, [x1, #0x13]
01927b5c +0538 add      x4, x4, x28, lsl #32
01927b60 +053c mov      x0, x4
01927b64 +0540 stur     w0, [x3, #0x2f]
01927b68 +0544 tbz      w0, #0, #0x1927b84
01927b6c +0548 ldurb    w16, [x3, #-1]
01927b70 +054c ldurb    w17, [x0, #-1]
01927b74 +0550 and      x16, x17, x16, lsr #2
01927b78 +0554 tst      x16, x28, lsr #32
01927b7c +0558 b.eq     #0x1927b84
01927b80 +055c bl       #0x1b569b8 => (None, None)
01927b84 +0560 sbfx     x0, x2, #1, #0x1f
01927b88 +0564 tbz      w2, #0, #0x1927b90
01927b8c +0568 ldur     x0, [x2, #7]
01927b90 +056c stur     x0, [x29, #-0xd8]
01927b94 +0570 cmp      x0, #0
01927b98 +0574 b.le     #0x1927d84
01927b9c +0578 sbfx     x1, x4, #1, #0x1f
01927ba0 +057c tbz      w4, #0, #0x1927ba8
01927ba4 +0580 ldur     x1, [x4, #7]
01927ba8 +0584 stur     x1, [x29, #-0xb0]
01927bac +0588 cmp      x1, #0
01927bb0 +058c b.le     #0x1927d84
01927bb4 +0590 ldr      x2, [x26, #0x78]
01927bb8 +0594 ldr      x2, [x2, #0xe58]
01927bbc +0598 stur     x2, [x29, #-0xc8]
01927bc0 +059c cmp      w2, w22
01927bc4 +05a0 b.eq     #0x1929118
01927bc8 +05a4 ldr      x0, [x26, #0x78]
01927bcc +05a8 ldr      x0, [x0, #0xa08]
01927bd0 +05ac ldr      x16, [x27, #0x40]
01927bd4 +05b0 cmp      w0, w16
01927bd8 +05b4 b.ne     #0x1927be4
01927bdc +05b8 ldr      x2, [x27, #0xdc0]
01927be0 +05bc bl       #0x1b563d0 => (None, None)
01927be4 +05c0 ldur     w2, [x0, #0x17]
01927be8 +05c4 add      x2, x2, x28, lsl #32
01927bec +05c8 stur     x2, [x29, #-0xc8]
01927bf0 +05cc ldr      x1, [x27, #0x2170]
01927bf4 +05d0 bl       #0x18aa5e0 => (None, None)
01927bf8 +05d4 mov      x1, x0
01927bfc +05d8 ldur     x0, [x29, #-0xc8]
01927c00 +05dc stur     w0, [x1, #0xb]
01927c04 +05e0 bl       #0xe1d03c => ('Iterable.first', 14798908)
01927c08 +05e4 mov      x3, x0
01927c0c +05e8 stur     x3, [x29, #-0xd0]
01927c10 +05ec mov      x0, #0x3c
01927c14 +05f0 tbz      w3, #0, #0x1927c20
01927c18 +05f4 ldur     x0, [x3, #-1]
01927c1c +05f8 ubfx     x0, x0, #0xc, #0x14
01927c20 +05fc mov      x17, #0x2390
01927c24 +0600 cmp      x0, x17
01927c28 +0604 b.ne     #0x1927c3c
01927c2c +0608 ldur     w0, [x3, #0x13]
01927c30 +060c add      x0, x0, x28, lsl #32
01927c34 +0610 mov      x2, x0
01927c38 +0614 b        #0x1927cd0
01927c3c +0618 ldur     w0, [x3, #0xf]
01927c40 +061c add      x0, x0, x28, lsl #32
01927c44 +0620 ldur     w4, [x0, #0x17]
01927c48 +0624 add      x4, x4, x28, lsl #32
01927c4c +0628 stur     x4, [x29, #-0xc8]
01927c50 +062c ldur     x2, [x3, #7]
01927c54 +0630 sbfiz    x0, x2, #1, #0x1f
01927c58 +0634 cmp      x2, x0, asr #1
01927c5c +0638 b.eq     #0x1927c68
01927c60 +063c bl       #0x1b58510 => (None, None)
01927c64 +0640 stur     x2, [x0, #7]
01927c68 +0644 mov      x1, x4
01927c6c +0648 mov      x2, x0
01927c70 +064c bl       #0x189304c => ('_LinkedHashMapMixin._getValueOrData', 25768012)
01927c74 +0650 mov      x1, x0
01927c78 +0654 ldur     x0, [x29, #-0xc8]
01927c7c +0658 ldur     w2, [x0, #0xf]
01927c80 +065c add      x2, x2, x28, lsl #32
01927c84 +0660 cmp      w2, w1
01927c88 +0664 b.ne     #0x1927c90
01927c8c +0668 mov      x1, x22
01927c90 +066c cmp      w1, w22
01927c94 +0670 b.ne     #0x1927ca0
01927c98 +0674 mov      x0, x22
01927c9c +0678 b        #0x1927cb4
01927ca0 +067c ldur     x0, [x1, #-1]
01927ca4 +0680 ubfx     x0, x0, #0xc, #0x14
01927ca8 +0684 sub      x30, x0, #0xff3
01927cac +0688 ldr      x30, [x21, x30, lsl #3]
01927cb0 +068c blr      x30
01927cb4 +0690 cmp      w0, w22
01927cb8 +0694 b.ne     #0x1927ccc
01927cbc +0698 ldur     x0, [x29, #-0xd0]
01927cc0 +069c ldur     w1, [x0, #0x13]
01927cc4 +06a0 add      x1, x1, x28, lsl #32
01927cc8 +06a4 mov      x0, x1
01927ccc +06a8 mov      x2, x0
01927cd0 +06ac ldur     x3, [x29, #-0xc0]
01927cd4 +06b0 ldur     x0, [x29, #-0xd8]
01927cd8 +06b4 ldur     x1, [x29, #-0xb0]
01927cdc +06b8 ldur     d0, [x2, #0x13]
01927ce0 +06bc scvtf    d1, x0
01927ce4 +06c0 fdiv     d2, d1, d0
01927ce8 +06c4 scvtf    d1, x1
01927cec +06c8 fdiv     d3, d1, d0
01927cf0 +06cc fmin     v0.2d, v2.2d, v3.2d
01927cf4 +06d0 ldp      x0, x1, [x26, #0x60]
01927cf8 +06d4 add      x0, x0, #0x10
01927cfc +06d8 cmp      x1, x0
01927d00 +06dc b.ls     #0x192911c
01927d04 +06e0 str      x0, [x26, #0x60]
01927d08 +06e4 sub      x0, x0, #0xf
01927d0c +06e8 mov      x1, #0xe15c
01927d10 +06ec movk     x1, #3, lsl #16
01927d14 +06f0 stur     x1, [x0, #-1]
01927d18 +06f4 stur     d0, [x0, #7]
01927d1c +06f8 stur     w0, [x3, #0x1b]
01927d20 +06fc ldurb    w16, [x3, #-1]
01927d24 +0700 ldurb    w17, [x0, #-1]
01927d28 +0704 and      x16, x17, x16, lsr #2
01927d2c +0708 tst      x16, x28, lsr #32
01927d30 +070c b.eq     #0x1927d38
01927d34 +0710 bl       #0x1b569b8 => (None, None)
01927d38 +0714 fmax     v0.2d, v2.2d, v3.2d
01927d3c +0718 ldp      x0, x1, [x26, #0x60]
01927d40 +071c add      x0, x0, #0x10
01927d44 +0720 cmp      x1, x0
01927d48 +0724 b.ls     #0x192913c
01927d4c +0728 str      x0, [x26, #0x60]
01927d50 +072c sub      x0, x0, #0xf
01927d54 +0730 mov      x1, #0xe15c
01927d58 +0734 movk     x1, #3, lsl #16
01927d5c +0738 stur     x1, [x0, #-1]
01927d60 +073c stur     d0, [x0, #7]
01927d64 +0740 stur     w0, [x3, #0x1f]
01927d68 +0744 ldurb    w16, [x3, #-1]
01927d6c +0748 ldurb    w17, [x0, #-1]
01927d70 +074c and      x16, x17, x16, lsr #2
01927d74 +0750 tst      x16, x28, lsr #32
01927d78 +0754 b.eq     #0x1927d80
01927d7c +0758 bl       #0x1b569b8 => (None, None)
01927d80 +075c b        #0x1927dc8
01927d84 +0760 mov      x2, x3
01927d88 +0764 add      x1, x27, #0x27, lsl #12
01927d8c +0768 ldr      x1, [x1, #0x2f0]
01927d90 +076c bl       #0x1b575bc => (None, None)
01927d94 +0770 ldur     x1, [x29, #-0xb8]
01927d98 +0774 mov      x2, x0
01927d9c +0778 ldr      x4, [x27, #0x70]
01927da0 +077c bl       #0x8a379c => ('_LoggerImpl.w', 9058204)
01927da4 +0780 b        #0x1927dc8
01927da8 +0784 sub      x15, x29, #0xf0
01927dac +0788 stp      x1, x0, [x15]
01927db0 +078c ldur     x1, [x29, #-0xb8]
01927db4 +0790 add      x2, x27, #0x27, lsl #12
01927db8 +0794 ldr      x2, [x2, #0x2f8]
01927dbc +0798 add      x4, x27, #8, lsl #12
01927dc0 +079c ldr      x4, [x4, #0x9c0]
01927dc4 +07a0 bl       #0x8c0a3c => ('_LoggerImpl.e', 9177660)
01927dc8 +07a4 ldur     x2, [x29, #-0xc0]
01927dcc +07a8 ldur     w0, [x2, #0x1b]
01927dd0 +07ac add      x0, x0, x28, lsl #32
01927dd4 +07b0 cmp      w0, w22
01927dd8 +07b4 b.eq     #0x1927dec
01927ddc +07b8 ldur     w0, [x2, #0x1f]
01927de0 +07bc add      x0, x0, x28, lsl #32
01927de4 +07c0 cmp      w0, w22
01927de8 +07c4 b.ne     #0x1927ec0
01927dec +07c8 ldr      x0, [x26, #0x78]
01927df0 +07cc ldr      x0, [x0, #0x1cd0]
01927df4 +07d0 ldr      x16, [x27, #0x40]
01927df8 +07d4 cmp      w0, w16
01927dfc +07d8 b.ne     #0x1927e08
01927e00 +07dc ldr      x2, [x27, #0x7800]
01927e04 +07e0 bl       #0x1b563d0 => (None, None)
01927e08 +07e4 add      x16, x27, #8, lsl #12
01927e0c +07e8 ldr      x16, [x16, #0xfc8]
01927e10 +07ec str      x16, [x15]
01927e14 +07f0 ldr      x4, [x27, #0x50]
01927e18 +07f4 bl       #0xb1f9a8 => ('Inst|find', 11663784)
01927e1c +07f8 mov      x1, x0
01927e20 +07fc bl       #0xa4afac => ('AppServiceImpl.logicalSize', 10792876)
01927e24 +0800 ldur     d0, [x0, #7]
01927e28 +0804 ldur     d1, [x0, #0xf]
01927e2c +0808 fmin     v2.2d, v0.2d, v1.2d
01927e30 +080c ldp      x0, x1, [x26, #0x60]
01927e34 +0810 add      x0, x0, #0x10
01927e38 +0814 cmp      x1, x0
01927e3c +0818 b.ls     #0x1929154
01927e40 +081c str      x0, [x26, #0x60]
01927e44 +0820 sub      x0, x0, #0xf
01927e48 +0824 mov      x1, #0xe15c
01927e4c +0828 movk     x1, #3, lsl #16
01927e50 +082c stur     x1, [x0, #-1]
01927e54 +0830 stur     d2, [x0, #7]
01927e58 +0834 ldur     x2, [x29, #-0xc0]
01927e5c +0838 stur     w0, [x2, #0x1b]
01927e60 +083c ldurb    w16, [x2, #-1]
01927e64 +0840 ldurb    w17, [x0, #-1]
01927e68 +0844 and      x16, x17, x16, lsr #2
01927e6c +0848 tst      x16, x28, lsr #32
01927e70 +084c b.eq     #0x1927e78
01927e74 +0850 bl       #0x1b56998 => (None, None)
01927e78 +0854 fmax     v2.2d, v0.2d, v1.2d
01927e7c +0858 ldp      x0, x1, [x26, #0x60]
01927e80 +085c add      x0, x0, #0x10
01927e84 +0860 cmp      x1, x0
01927e88 +0864 b.ls     #0x192916c
01927e8c +0868 str      x0, [x26, #0x60]
01927e90 +086c sub      x0, x0, #0xf
01927e94 +0870 mov      x1, #0xe15c
01927e98 +0874 movk     x1, #3, lsl #16
01927e9c +0878 stur     x1, [x0, #-1]
01927ea0 +087c stur     d2, [x0, #7]
01927ea4 +0880 stur     w0, [x2, #0x1f]
01927ea8 +0884 ldurb    w16, [x2, #-1]
01927eac +0888 ldurb    w17, [x0, #-1]
01927eb0 +088c and      x16, x17, x16, lsr #2
01927eb4 +0890 tst      x16, x28, lsr #32
01927eb8 +0894 b.eq     #0x1927ec0
01927ebc +0898 bl       #0x1b56998 => (None, None)
01927ec0 +089c ldr      x0, [x26, #0x78]
01927ec4 +08a0 ldr      x0, [x0, #0x26d8]
01927ec8 +08a4 cmp      w0, w22
01927ecc +08a8 b.ne     #0x1927f04
01927ed0 +08ac ldr      x0, [x26, #0x78]
01927ed4 +08b0 ldr      x0, [x0, #0x1cd0]
01927ed8 +08b4 ldr      x16, [x27, #0x40]
01927edc +08b8 cmp      w0, w16
01927ee0 +08bc b.ne     #0x1927eec
01927ee4 +08c0 ldr      x2, [x27, #0x7800]
01927ee8 +08c4 bl       #0x1b563d0 => (None, None)
01927eec +08c8 ldr      x16, [x27, #0x7808]
01927ef0 +08cc str      x16, [x15]
01927ef4 +08d0 ldr      x4, [x27, #0x50]
01927ef8 +08d4 bl       #0xb1f9a8 => ('Inst|find', 11663784)
01927efc +08d8 mov      x1, x0
01927f00 +08dc b        #0x1927f08
01927f04 +08e0 mov      x1, x0
01927f08 +08e4 ldur     x0, [x29, #-0xc0]
01927f0c +08e8 ldur     w2, [x0, #0x1b]
01927f10 +08ec add      x2, x2, x28, lsl #32
01927f14 +08f0 bl       #0xa4af2c => ('DeviceConfig.isFoldScreenSizeTrusted', 10792748)
01927f18 +08f4 tbz      w0, #4, #0x19280dc
01927f1c +08f8 ldur     x2, [x29, #-0xc0]
01927f20 +08fc ldr      x0, [x26, #0x78]
01927f24 +0900 ldr      x0, [x0, #0x1cd0]
01927f28 +0904 ldr      x16, [x27, #0x40]
01927f2c +0908 cmp      w0, w16
01927f30 +090c b.ne     #0x1927f3c
01927f34 +0910 ldr      x2, [x27, #0x7800]
01927f38 +0914 bl       #0x1b563d0 => (None, None)
01927f3c +0918 add      x16, x27, #8, lsl #12
01927f40 +091c ldr      x16, [x16, #0xfc8]
01927f44 +0920 str      x16, [x15]
01927f48 +0924 ldr      x4, [x27, #0x50]
01927f4c +0928 bl       #0xb1f9a8 => ('Inst|find', 11663784)
01927f50 +092c mov      x1, x0
01927f54 +0930 bl       #0xa4afac => ('AppServiceImpl.logicalSize', 10792876)
01927f58 +0934 ldur     d0, [x0, #7]
01927f5c +0938 ldur     d1, [x0, #0xf]
01927f60 +093c fmin     v2.2d, v0.2d, v1.2d
01927f64 +0940 ldp      x1, x0, [x26, #0x60]
01927f68 +0944 add      x1, x1, #0x10
01927f6c +0948 cmp      x0, x1
01927f70 +094c b.ls     #0x1929184
01927f74 +0950 str      x1, [x26, #0x60]
01927f78 +0954 sub      x1, x1, #0xf
01927f7c +0958 mov      x0, #0xe15c
01927f80 +095c movk     x0, #3, lsl #16
01927f84 +0960 stur     x0, [x1, #-1]
01927f88 +0964 stur     d2, [x1, #7]
01927f8c +0968 mov      x0, x1
01927f90 +096c ldur     x2, [x29, #-0xc0]
01927f94 +0970 stur     x1, [x29, #-0xd0]
01927f98 +0974 stur     w0, [x2, #0x23]
01927f9c +0978 ldurb    w16, [x2, #-1]
01927fa0 +097c ldurb    w17, [x0, #-1]
01927fa4 +0980 and      x16, x17, x16, lsr #2
01927fa8 +0984 tst      x16, x28, lsr #32
01927fac +0988 b.eq     #0x1927fb4
01927fb0 +098c bl       #0x1b56998 => (None, None)
01927fb4 +0990 fmax     v2.2d, v0.2d, v1.2d
01927fb8 +0994 ldp      x3, x0, [x26, #0x60]
01927fbc +0998 add      x3, x3, #0x10
01927fc0 +099c cmp      x0, x3
01927fc4 +09a0 b.ls     #0x19291a0
01927fc8 +09a4 str      x3, [x26, #0x60]
01927fcc +09a8 sub      x3, x3, #0xf
01927fd0 +09ac mov      x0, #0xe15c
01927fd4 +09b0 movk     x0, #3, lsl #16
01927fd8 +09b4 stur     x0, [x3, #-1]
01927fdc +09b8 stur     d2, [x3, #7]
01927fe0 +09bc mov      x0, x3
01927fe4 +09c0 stur     x3, [x29, #-0xc8]
01927fe8 +09c4 stur     w0, [x2, #0x27]
01927fec +09c8 ldurb    w16, [x2, #-1]
01927ff0 +09cc ldurb    w17, [x0, #-1]
01927ff4 +09d0 and      x16, x17, x16, lsr #2
01927ff8 +09d4 tst      x16, x28, lsr #32
01927ffc +09d8 b.eq     #0x1928004
01928000 +09dc bl       #0x1b56998 => (None, None)
01928004 +09e0 ldr      x0, [x26, #0x78]
01928008 +09e4 ldr      x0, [x0, #0x26d8]
0192800c +09e8 cmp      w0, w22
01928010 +09ec b.ne     #0x192802c
01928014 +09f0 ldr      x16, [x27, #0x7808]
01928018 +09f4 str      x16, [x15]
0192801c +09f8 ldr      x4, [x27, #0x50]
01928020 +09fc bl       #0xb1f9a8 => ('Inst|find', 11663784)
01928024 +0a00 mov      x1, x0
01928028 +0a04 b        #0x1928030
0192802c +0a08 mov      x1, x0
01928030 +0a0c ldur     x2, [x29, #-0xd0]
01928034 +0a10 bl       #0xa4af2c => ('DeviceConfig.isFoldScreenSizeTrusted', 10792748)
01928038 +0a14 tbnz     w0, #4, #0x19280ac
0192803c +0a18 ldur     x0, [x29, #-0xc0]
01928040 +0a1c mov      x2, x0
01928044 +0a20 add      x1, x27, #0x27, lsl #12
01928048 +0a24 ldr      x1, [x1, #0x300]
0192804c +0a28 bl       #0x1b575bc => (None, None)
01928050 +0a2c ldur     x1, [x29, #-0xb8]
01928054 +0a30 mov      x2, x0
01928058 +0a34 ldr      x4, [x27, #0x70]
0192805c +0a38 bl       #0x8a379c => ('_LoggerImpl.w', 9058204)
01928060 +0a3c ldur     x0, [x29, #-0xd0]
01928064 +0a40 ldur     x3, [x29, #-0xc0]
01928068 +0a44 stur     w0, [x3, #0x1b]
0192806c +0a48 ldurb    w16, [x3, #-1]
01928070 +0a4c ldurb    w17, [x0, #-1]
01928074 +0a50 and      x16, x17, x16, lsr #2
01928078 +0a54 tst      x16, x28, lsr #32
0192807c +0a58 b.eq     #0x1928084
01928080 +0a5c bl       #0x1b569b8 => (None, None)
01928084 +0a60 ldur     x0, [x29, #-0xc8]
01928088 +0a64 stur     w0, [x3, #0x1f]
0192808c +0a68 ldurb    w16, [x3, #-1]
01928090 +0a6c ldurb    w17, [x0, #-1]
01928094 +0a70 and      x16, x17, x16, lsr #2
01928098 +0a74 tst      x16, x28, lsr #32
0192809c +0a78 b.eq     #0x19280a4
019280a0 +0a7c bl       #0x1b569b8 => (None, None)
019280a4 +0a80 add      x0, x22, #0x20
019280a8 +0a84 b        #0x19280d4
019280ac +0a88 ldur     x3, [x29, #-0xc0]
019280b0 +0a8c mov      x2, x3
019280b4 +0a90 add      x1, x27, #0x27, lsl #12
019280b8 +0a94 ldr      x1, [x1, #0x308]
019280bc +0a98 bl       #0x1b575bc => (None, None)
019280c0 +0a9c ldur     x1, [x29, #-0xb8]
019280c4 +0aa0 mov      x2, x0
019280c8 +0aa4 ldr      x4, [x27, #0x70]
019280cc +0aa8 bl       #0x8a379c => ('_LoggerImpl.w', 9058204)
019280d0 +0aac add      x0, x22, #0x30
019280d4 +0ab0 mov      x4, x0
019280d8 +0ab4 b        #0x19280e0
019280dc +0ab8 add      x4, x22, #0x20
019280e0 +0abc ldur     x0, [x29, #-0xc0]
019280e4 +0ac0 stur     x4, [x29, #-0xc8]
019280e8 +0ac4 ldur     w2, [x0, #0x1b]
019280ec +0ac8 add      x2, x2, x28, lsl #32
019280f0 +0acc ldur     w3, [x0, #0x1f]
019280f4 +0ad0 add      x3, x3, x28, lsl #32
019280f8 +0ad4 ldur     x1, [x29, #-0xa8]
019280fc +0ad8 bl       #0xa4ae94 => ('GridConfig.updateScreenSize', 10792596)
01928100 +0adc ldr      x0, [x26, #0x78]
01928104 +0ae0 ldr      x0, [x0, #0x26d8]
01928108 +0ae4 cmp      w0, w22
0192810c +0ae8 b.ne     #0x192813c
01928110 +0aec ldr      x0, [x26, #0x78]
01928114 +0af0 ldr      x0, [x0, #0x1cd0]
01928118 +0af4 ldr      x16, [x27, #0x40]
0192811c +0af8 cmp      w0, w16
01928120 +0afc b.ne     #0x192812c
01928124 +0b00 ldr      x2, [x27, #0x7800]
01928128 +0b04 bl       #0x1b563d0 => (None, None)
0192812c +0b08 ldr      x16, [x27, #0x7808]
01928130 +0b0c str      x16, [x15]
01928134 +0b10 ldr      x4, [x27, #0x50]
01928138 +0b14 bl       #0xb1f9a8 => ('Inst|find', 11663784)
0192813c +0b18 ldur     w1, [x0, #0xdb]
01928140 +0b1c add      x1, x1, x28, lsl #32
01928144 +0b20 tbnz     w1, #4, #0x192816c
01928148 +0b24 ldur     x0, [x29, #-0xc8]
0192814c +0b28 tbnz     w0, #4, #0x192816c
01928150 +0b2c ldur     x0, [x29, #-0xc0]
01928154 +0b30 ldur     w2, [x0, #0x1b]
01928158 +0b34 add      x2, x2, x28, lsl #32
0192815c +0b38 ldur     w3, [x0, #0x1f]
01928160 +0b3c add      x3, x3, x28, lsl #32
01928164 +0b40 ldur     x1, [x29, #-0xa8]
01928168 +0b44 bl       #0xa4ae74 => ('GridConfig.updateDeviceSize', 10792564)
0192816c +0b48 ldur     x0, [x29, #-0xa8]
01928170 +0b4c ldur     x2, [x29, #-0xc0]
01928174 +0b50 mov      x1, x0
01928178 +0b54 bl       #0x1929d00 => ('GridConfig._loadCellConfigFromController', 26385664)
0192817c +0b58 ldur     x0, [x29, #-0xa8]
01928180 +0b5c ldur     w2, [x0, #0x1f]
01928184 +0b60 add      x2, x2, x28, lsl #32
01928188 +0b64 stur     x2, [x29, #-0xc8]
0192818c +0b68 ldur     w1, [x0, #0x5b]
01928190 +0b6c add      x1, x1, x28, lsl #32
01928194 +0b70 bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
01928198 +0b74 ldur     x4, [x29, #-0xc0]
0192819c +0b78 ldur     w1, [x4, #0x13]
019281a0 +0b7c ldur     x2, [x29, #-0xc8]
019281a4 +0b80 ldur     x3, [x2, #-1]
019281a8 +0b84 ubfx     x3, x3, #0xc, #0x14
019281ac +0b88 cmp      x3, #0x785
019281b0 +0b8c b.ne     #0x19281dc
019281b4 +0b90 sbfx     x3, x0, #1, #0x1f
019281b8 +0b94 tbz      w0, #0, #0x19281c0
019281bc +0b98 ldur     x3, [x0, #7]
019281c0 +0b9c sbfx     x0, x1, #1, #0x1f
019281c4 +0ba0 mov      x1, x2
019281c8 +0ba4 mov      x2, x3
019281cc +0ba8 mov      x3, x0
019281d0 +0bac mov      x5, #-1
019281d4 +0bb0 bl       #0xa4b5d4 => ('PhoneDeviceRules._calGridSizeByVariable', 10794452)
019281d8 +0bb4 b        #0x19284c8
019281dc +0bb8 mov      x1, x2
019281e0 +0bbc add      x2, x27, #0x27, lsl #12
019281e4 +0bc0 ldr      x2, [x2, #0x2d8]
019281e8 +0bc4 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
019281ec +0bc8 b        #0x19284c8
019281f0 +0bcc cmp      x3, #0xb
019281f4 +0bd0 b.gt     #0x1928430
019281f8 +0bd4 cmp      x3, #0xa
019281fc +0bd8 b.gt     #0x192828c
01928200 +0bdc ldur     x0, [x29, #-0xa8]
01928204 +0be0 ldur     x1, [x29, #-0xb8]
01928208 +0be4 add      x2, x27, #0x27, lsl #12
0192820c +0be8 ldr      x2, [x2, #0x310]
01928210 +0bec ldr      x4, [x27, #0x70]
01928214 +0bf0 bl       #0x89c194 => ('_LoggerImpl.d', 9027988)
01928218 +0bf4 ldur     x0, [x29, #-0xa8]
0192821c +0bf8 ldur     w2, [x0, #0x1f]
01928220 +0bfc add      x2, x2, x28, lsl #32
01928224 +0c00 stur     x2, [x29, #-0xc8]
01928228 +0c04 ldur     w1, [x0, #0x5b]
0192822c +0c08 add      x1, x1, x28, lsl #32
01928230 +0c0c bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
01928234 +0c10 ldur     x3, [x29, #-0xc0]
01928238 +0c14 ldur     w1, [x3, #0x13]
0192823c +0c18 ldur     x2, [x29, #-0xc8]
01928240 +0c1c ldur     x3, [x2, #-1]
01928244 +0c20 ubfx     x3, x3, #0xc, #0x14
01928248 +0c24 cmp      x3, #0x785
0192824c +0c28 b.ne     #0x1928278
01928250 +0c2c sbfx     x3, x0, #1, #0x1f
01928254 +0c30 tbz      w0, #0, #0x192825c
01928258 +0c34 ldur     x3, [x0, #7]
0192825c +0c38 sbfx     x0, x1, #1, #0x1f
01928260 +0c3c mov      x1, x2
01928264 +0c40 mov      x2, x3
01928268 +0c44 mov      x3, x0
0192826c +0c48 mov      x5, #-1
01928270 +0c4c bl       #0xa4b5d4 => ('PhoneDeviceRules._calGridSizeByVariable', 10794452)
01928274 +0c50 b        #0x19284c8
01928278 +0c54 mov      x1, x2
0192827c +0c58 add      x2, x27, #0x27, lsl #12
01928280 +0c5c ldr      x2, [x2, #0x2d8]
01928284 +0c60 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928288 +0c64 b        #0x19284c8
0192828c +0c68 mov      x3, x4
01928290 +0c6c ldur     w2, [x3, #0x17]
01928294 +0c70 add      x2, x2, x28, lsl #32
01928298 +0c74 mov      x0, #0x3c
0192829c +0c78 tbz      w2, #0, #0x19282a8
019282a0 +0c7c ldur     x0, [x2, #-1]
019282a4 +0c80 ubfx     x0, x0, #0xc, #0x14
019282a8 +0c84 cmp      x0, #0x766
019282ac +0c88 b.ne     #0x19284c8
019282b0 +0c8c ldur     x4, [x29, #-0xa8]
019282b4 +0c90 cmp      w2, w22
019282b8 +0c94 b.eq     #0x19291bc
019282bc +0c98 ldur     x5, [x2, #7]
019282c0 +0c9c sbfiz    x0, x5, #1, #0x1f
019282c4 +0ca0 cmp      x5, x0, asr #1
019282c8 +0ca4 b.eq     #0x19282d4
019282cc +0ca8 bl       #0x1b58510 => (None, None)
019282d0 +0cac stur     x5, [x0, #7]
019282d4 +0cb0 stur     w0, [x3, #0x33]
019282d8 +0cb4 tbz      w0, #0, #0x19282f4
019282dc +0cb8 ldurb    w16, [x3, #-1]
019282e0 +0cbc ldurb    w17, [x0, #-1]
019282e4 +0cc0 and      x16, x17, x16, lsr #2
019282e8 +0cc4 tst      x16, x28, lsr #32
019282ec +0cc8 b.eq     #0x19282f4
019282f0 +0ccc bl       #0x1b569b8 => (None, None)
019282f4 +0cd0 ldur     x5, [x2, #0xf]
019282f8 +0cd4 sbfiz    x0, x5, #1, #0x1f
019282fc +0cd8 cmp      x5, x0, asr #1
01928300 +0cdc b.eq     #0x192830c
01928304 +0ce0 bl       #0x1b58510 => (None, None)
01928308 +0ce4 stur     x5, [x0, #7]
0192830c +0ce8 stur     w0, [x3, #0x37]
01928310 +0cec tbz      w0, #0, #0x192832c
01928314 +0cf0 ldurb    w16, [x3, #-1]
01928318 +0cf4 ldurb    w17, [x0, #-1]
0192831c +0cf8 and      x16, x17, x16, lsr #2
01928320 +0cfc tst      x16, x28, lsr #32
01928324 +0d00 b.eq     #0x192832c
01928328 +0d04 bl       #0x1b569b8 => (None, None)
0192832c +0d08 mov      x2, x3
01928330 +0d0c add      x1, x27, #0x27, lsl #12
01928334 +0d10 ldr      x1, [x1, #0x318]
01928338 +0d14 bl       #0x1b575bc => (None, None)
0192833c +0d18 ldur     x1, [x29, #-0xb8]
01928340 +0d1c mov      x2, x0
01928344 +0d20 ldr      x4, [x27, #0x70]
01928348 +0d24 bl       #0x89c194 => ('_LoggerImpl.d', 9027988)
0192834c +0d28 ldur     x0, [x29, #-0xa8]
01928350 +0d2c ldur     w1, [x0, #0x1f]
01928354 +0d30 add      x1, x1, x28, lsl #32
01928358 +0d34 ldur     x4, [x29, #-0xc0]
0192835c +0d38 ldur     w2, [x4, #0x33]
01928360 +0d3c add      x2, x2, x28, lsl #32
01928364 +0d40 ldur     w3, [x4, #0x37]
01928368 +0d44 add      x3, x3, x28, lsl #32
0192836c +0d48 ldur     w5, [x4, #0x17]
01928370 +0d4c add      x5, x5, x28, lsl #32
01928374 +0d50 cmp      w5, w22
01928378 +0d54 b.eq     #0x19291c0
0192837c +0d58 ldur     x6, [x5, #0x1f]
01928380 +0d5c ldur     x5, [x1, #-1]
01928384 +0d60 ubfx     x5, x5, #0xc, #0x14
01928388 +0d64 cmp      x5, #0x785
0192838c +0d68 b.ne     #0x19283bc
01928390 +0d6c sbfx     x5, x2, #1, #0x1f
01928394 +0d70 tbz      w2, #0, #0x192839c
01928398 +0d74 ldur     x5, [x2, #7]
0192839c +0d78 sbfx     x2, x3, #1, #0x1f
019283a0 +0d7c tbz      w3, #0, #0x19283a8
019283a4 +0d80 ldur     x2, [x3, #7]
019283a8 +0d84 mov      x3, x2
019283ac +0d88 mov      x2, x5
019283b0 +0d8c mov      x5, x6
019283b4 +0d90 bl       #0xa49c58 => ('PhoneDeviceRules._calGridSizeByFixedRows', 10787928)
019283b8 +0d94 b        #0x19283c8
019283bc +0d98 add      x2, x27, #0x27, lsl #12
019283c0 +0d9c ldr      x2, [x2, #0x2d8]
019283c4 +0da0 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
019283c8 +0da4 ldur     x0, [x29, #-0xa8]
019283cc +0da8 ldur     x3, [x29, #-0xc0]
019283d0 +0dac mov      x2, x3
019283d4 +0db0 add      x1, x27, #0x27, lsl #12
019283d8 +0db4 ldr      x1, [x1, #0x320]
019283dc +0db8 bl       #0x1b575bc => (None, None)
019283e0 +0dbc ldur     x1, [x29, #-0xb8]
019283e4 +0dc0 mov      x2, x0
019283e8 +0dc4 ldr      x4, [x27, #0x70]
019283ec +0dc8 bl       #0x89c194 => ('_LoggerImpl.d', 9027988)
019283f0 +0dcc ldur     x0, [x29, #-0xa8]
019283f4 +0dd0 ldur     w1, [x0, #0x5b]
019283f8 +0dd4 add      x1, x1, x28, lsl #32
019283fc +0dd8 ldur     x3, [x29, #-0xc0]
01928400 +0ddc ldur     w2, [x3, #0x33]
01928404 +0de0 add      x2, x2, x28, lsl #32
01928408 +0de4 bl       #0xb1956c => ('RxObjectMixin.value=', 11638124)
0192840c +0de8 ldur     x0, [x29, #-0xa8]
01928410 +0dec ldur     w1, [x0, #0x5f]
01928414 +0df0 add      x1, x1, x28, lsl #32
01928418 +0df4 ldur     x2, [x29, #-0xc0]
0192841c +0df8 ldur     w3, [x2, #0x37]
01928420 +0dfc add      x3, x3, x28, lsl #32
01928424 +0e00 mov      x2, x3
01928428 +0e04 bl       #0xb1956c => ('RxObjectMixin.value=', 11638124)
0192842c +0e08 b        #0x19284c8
01928430 +0e0c cmp      x3, #0xd
01928434 +0e10 b.lt     #0x1928440
01928438 +0e14 cmp      w0, #0x1a
0192843c +0e18 b.eq     #0x19284c8
01928440 +0e1c ldur     x0, [x29, #-0xa8]
01928444 +0e20 ldur     w2, [x0, #0x1f]
01928448 +0e24 add      x2, x2, x28, lsl #32
0192844c +0e28 stur     x2, [x29, #-0xc0]
01928450 +0e2c ldur     w1, [x0, #0x5b]
01928454 +0e30 add      x1, x1, x28, lsl #32
01928458 +0e34 bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
0192845c +0e38 mov      x2, x0
01928460 +0e3c ldur     x0, [x29, #-0xa8]
01928464 +0e40 stur     x2, [x29, #-0xc8]
01928468 +0e44 ldur     w1, [x0, #0x5f]
0192846c +0e48 add      x1, x1, x28, lsl #32
01928470 +0e4c bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
01928474 +0e50 ldur     x1, [x29, #-0xc0]
01928478 +0e54 ldur     x2, [x1, #-1]
0192847c +0e58 ubfx     x2, x2, #0xc, #0x14
01928480 +0e5c cmp      x2, #0x785
01928484 +0e60 b.ne     #0x19284bc
01928488 +0e64 ldur     x2, [x29, #-0xc8]
0192848c +0e68 sbfx     x3, x2, #1, #0x1f
01928490 +0e6c tbz      w2, #0, #0x1928498
01928494 +0e70 ldur     x3, [x2, #7]
01928498 +0e74 sbfx     x2, x0, #1, #0x1f
0192849c +0e78 tbz      w0, #0, #0x19284a4
019284a0 +0e7c ldur     x2, [x0, #7]
019284a4 +0e80 mov      x16, x2
019284a8 +0e84 mov      x2, x3
019284ac +0e88 mov      x3, x16
019284b0 +0e8c mov      x5, #-1
019284b4 +0e90 bl       #0xa49c58 => ('PhoneDeviceRules._calGridSizeByFixedRows', 10787928)
019284b8 +0e94 b        #0x19284c8
019284bc +0e98 add      x2, x27, #0x27, lsl #12
019284c0 +0e9c ldr      x2, [x2, #0x2d8]
019284c4 +0ea0 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
019284c8 +0ea4 ldur     x0, [x29, #-0xa8]
019284cc +0ea8 ldur     w2, [x0, #0x23]
019284d0 +0eac add      x2, x2, x28, lsl #32
019284d4 +0eb0 ldr      x16, [x27, #0x40]
019284d8 +0eb4 cmp      w2, w16
019284dc +0eb8 b.eq     #0x19291c4
019284e0 +0ebc stur     x2, [x29, #-0xc0]
019284e4 +0ec0 ldur     w1, [x0, #0x5b]
019284e8 +0ec4 add      x1, x1, x28, lsl #32
019284ec +0ec8 bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
019284f0 +0ecc ldur     x1, [x29, #-0xa8]
019284f4 +0ed0 stur     x0, [x29, #-0xc8]
019284f8 +0ed4 ldur     w3, [x1, #0x1f]
019284fc +0ed8 add      x3, x3, x28, lsl #32
01928500 +0edc stur     x3, [x29, #-0xd0]
01928504 +0ee0 ldur     x2, [x3, #-1]
01928508 +0ee4 ubfx     x2, x2, #0xc, #0x14
0192850c +0ee8 cmp      x2, #0x785
01928510 +0eec b.ne     #0x1928524
01928514 +0ef0 ldur     d0, [x3, #0x47]
01928518 +0ef4 ldr      x4, [x27, #0x418]
0192851c +0ef8 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928520 +0efc b        #0x1928544
01928524 +0f00 mov      x1, x3
01928528 +0f04 add      x2, x27, #0x27, lsl #12
0192852c +0f08 ldr      x2, [x2, #0x328]
01928530 +0f0c bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928534 +0f10 ldur     x0, [x29, #-0xd0]
01928538 +0f14 ldur     d0, [x0, #0x47]
0192853c +0f18 ldr      x4, [x27, #0x418]
01928540 +0f1c bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928544 +0f20 ldur     x0, [x29, #-0xa8]
01928548 +0f24 ldur     x1, [x29, #-0xc8]
0192854c +0f28 sbfx     x2, x1, #1, #0x1f
01928550 +0f2c tbz      w1, #0, #0x1928558
01928554 +0f30 ldur     x2, [x1, #7]
01928558 +0f34 ldur     x1, [x29, #-0xc0]
0192855c +0f38 bl       #0xa48708 => ('_IconConfig.updateCellWidth', 10782472)
01928560 +0f3c mov      x1, x22
01928564 +0f40 mov      x2, #8
01928568 +0f44 bl       #0x1b58288 => (None, None)
0192856c +0f48 stur     x0, [x29, #-0xc0]
01928570 +0f4c add      x16, x27, #0x27, lsl #12
01928574 +0f50 ldr      x16, [x16, #0x330]
01928578 +0f54 stur     w16, [x0, #0xf]
0192857c +0f58 ldur     x1, [x29, #-0xa8]
01928580 +0f5c ldur     w3, [x1, #0x1f]
01928584 +0f60 add      x3, x3, x28, lsl #32
01928588 +0f64 stur     x3, [x29, #-0xc8]
0192858c +0f68 ldur     x2, [x3, #-1]
01928590 +0f6c ubfx     x2, x2, #0xc, #0x14
01928594 +0f70 cmp      x2, #0x785
01928598 +0f74 b.ne     #0x19285ac
0192859c +0f78 ldur     d0, [x3, #0x47]
019285a0 +0f7c ldr      x4, [x27, #0x418]
019285a4 +0f80 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
019285a8 +0f84 b        #0x19285cc
019285ac +0f88 mov      x1, x3
019285b0 +0f8c add      x2, x27, #0x27, lsl #12
019285b4 +0f90 ldr      x2, [x2, #0x328]
019285b8 +0f94 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
019285bc +0f98 ldur     x0, [x29, #-0xc8]
019285c0 +0f9c ldur     d0, [x0, #0x47]
019285c4 +0fa0 ldr      x4, [x27, #0x418]
019285c8 +0fa4 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
019285cc +0fa8 ldur     x3, [x29, #-0xa8]
019285d0 +0fac ldur     x2, [x29, #-0xc0]
019285d4 +0fb0 ldp      x0, x1, [x26, #0x60]
019285d8 +0fb4 add      x0, x0, #0x10
019285dc +0fb8 cmp      x1, x0
019285e0 +0fbc b.ls     #0x19291d0
019285e4 +0fc0 str      x0, [x26, #0x60]
019285e8 +0fc4 sub      x0, x0, #0xf
019285ec +0fc8 mov      x1, #0xe15c
019285f0 +0fcc movk     x1, #3, lsl #16
019285f4 +0fd0 stur     x1, [x0, #-1]
019285f8 +0fd4 stur     d0, [x0, #7]
019285fc +0fd8 mov      x1, x2
01928600 +0fdc add      x25, x1, #0x13
01928604 +0fe0 str      w0, [x25]
01928608 +0fe4 tbz      w0, #0, #0x1928624
0192860c +0fe8 ldurb    w16, [x1, #-1]
01928610 +0fec ldurb    w17, [x0, #-1]
01928614 +0ff0 and      x16, x17, x16, lsr #2
01928618 +0ff4 tst      x16, x28, lsr #32
0192861c +0ff8 b.eq     #0x1928624
01928620 +0ffc bl       #0x1b56534 => (None, None)
01928624 +1000 add      x16, x27, #0x27, lsl #12
01928628 +1004 ldr      x16, [x16, #0x338]
0192862c +1008 stur     w16, [x2, #0x17]
01928630 +100c ldur     w1, [x3, #0x23]
01928634 +1010 add      x1, x1, x28, lsl #32
01928638 +1014 bl       #0xa486d4 => ('_IconConfig.getScale', 10782420)
0192863c +1018 mov      x1, x0
01928640 +101c bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
01928644 +1020 ldur     x1, [x29, #-0xc0]
01928648 +1024 add      x25, x1, #0x1b
0192864c +1028 str      w0, [x25]
01928650 +102c tbz      w0, #0, #0x192866c
01928654 +1030 ldurb    w16, [x1, #-1]
01928658 +1034 ldurb    w17, [x0, #-1]
0192865c +1038 and      x16, x17, x16, lsr #2
01928660 +103c tst      x16, x28, lsr #32
01928664 +1040 b.eq     #0x192866c
01928668 +1044 bl       #0x1b56534 => (None, None)
0192866c +1048 ldur     x16, [x29, #-0xc0]
01928670 +104c str      x16, [x15]
01928674 +1050 bl       #0x868b8c => ('_StringBase._interpolate', 8817548)
01928678 +1054 ldur     x1, [x29, #-0xb8]
0192867c +1058 mov      x3, x0
01928680 +105c add      x2, x27, #0x27, lsl #12
01928684 +1060 ldr      x2, [x2, #0x2d8]
01928688 +1064 ldr      x4, [x27, #0x140]
0192868c +1068 bl       #0x86af0c => ('_LoggerImpl.logWithTag', 8826636)
01928690 +106c ldur     x0, [x29, #-0xa8]
01928694 +1070 ldur     w2, [x0, #0x27]
01928698 +1074 add      x2, x2, x28, lsl #32
0192869c +1078 ldr      x16, [x27, #0x40]
019286a0 +107c cmp      w2, w16
019286a4 +1080 b.eq     #0x19291e8
019286a8 +1084 stur     x2, [x29, #-0xc0]
019286ac +1088 ldur     w1, [x0, #0x5b]
019286b0 +108c add      x1, x1, x28, lsl #32
019286b4 +1090 bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
019286b8 +1094 sbfx     x2, x0, #1, #0x1f
019286bc +1098 tbz      w0, #0, #0x19286c4
019286c0 +109c ldur     x2, [x0, #7]
019286c4 +10a0 ldur     x1, [x29, #-0xc0]
019286c8 +10a4 bl       #0xa48640 => ('TextSizeConfig.setCurrentColumn', 10782272)
019286cc +10a8 ldur     x1, [x29, #-0xa8]
019286d0 +10ac ldur     w0, [x1, #0x27]
019286d4 +10b0 add      x0, x0, x28, lsl #32
019286d8 +10b4 stur     x0, [x29, #-0xc0]
019286dc +10b8 ldur     w3, [x1, #0x1f]
019286e0 +10bc add      x3, x3, x28, lsl #32
019286e4 +10c0 stur     x3, [x29, #-0xc8]
019286e8 +10c4 ldur     x2, [x3, #-1]
019286ec +10c8 ubfx     x2, x2, #0xc, #0x14
019286f0 +10cc cmp      x2, #0x785
019286f4 +10d0 b.ne     #0x1928708
019286f8 +10d4 ldur     d0, [x3, #0x47]
019286fc +10d8 ldr      x4, [x27, #0x418]
01928700 +10dc bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928704 +10e0 b        #0x1928728
01928708 +10e4 mov      x1, x3
0192870c +10e8 add      x2, x27, #0x27, lsl #12
01928710 +10ec ldr      x2, [x2, #0x328]
01928714 +10f0 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928718 +10f4 ldur     x0, [x29, #-0xc8]
0192871c +10f8 ldur     d0, [x0, #0x47]
01928720 +10fc ldr      x4, [x27, #0x418]
01928724 +1100 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928728 +1104 ldur     x0, [x29, #-0xa8]
0192872c +1108 stur     d0, [x29, #-0xe0]
01928730 +110c ldur     w1, [x0, #0x23]
01928734 +1110 add      x1, x1, x28, lsl #32
01928738 +1114 bl       #0xa486d4 => ('_IconConfig.getScale', 10782420)
0192873c +1118 mov      x1, x0
01928740 +111c bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
01928744 +1120 ldur     d1, [x0, #7]
01928748 +1124 ldur     x1, [x29, #-0xc0]
0192874c +1128 ldur     d0, [x29, #-0xe0]
01928750 +112c bl       #0xa47cb4 => ('TextSizeConfig.setFittingTextSize', 10779828)
01928754 +1130 ldur     x0, [x29, #-0xa8]
01928758 +1134 ldur     w1, [x0, #0x23]
0192875c +1138 add      x1, x1, x28, lsl #32
01928760 +113c bl       #0xa47948 => ('_IconConfig.calIconPadding', 10778952)
01928764 +1140 ldur     x0, [x29, #-0xa8]
01928768 +1144 ldur     w1, [x0, #0x2b]
0192876c +1148 add      x1, x1, x28, lsl #32
01928770 +114c ldr      x16, [x27, #0x40]
01928774 +1150 cmp      w1, w16
01928778 +1154 b.eq     #0x19291f4
0192877c +1158 bl       #0xa4746c => ('_FolderConfig.updateFolderConfig', 10777708)
01928780 +115c ldur     x0, [x29, #-0xa8]
01928784 +1160 ldur     d0, [x0, #0x4f]
01928788 +1164 stur     d0, [x29, #-0xe0]
0192878c +1168 ldur     w3, [x0, #0x1f]
01928790 +116c add      x3, x3, x28, lsl #32
01928794 +1170 stur     x3, [x29, #-0xc0]
01928798 +1174 ldur     x1, [x3, #-1]
0192879c +1178 ubfx     x1, x1, #0xc, #0x14
019287a0 +117c cmp      x1, #0x785
019287a4 +1180 b.ne     #0x19287b8
019287a8 +1184 mov      x1, x3
019287ac +1188 bl       #0xa4744c => ('GridSizeCalRules.screenMarginBottom', 10777676)
019287b0 +118c mov      v1.16b, v0.16b
019287b4 +1190 b        #0x19287dc
019287b8 +1194 mov      x1, x3
019287bc +1198 add      x2, x27, #0x27, lsl #12
019287c0 +119c ldr      x2, [x2, #0x340]
019287c4 +11a0 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
019287c8 +11a4 ldur     x0, [x29, #-0xc0]
019287cc +11a8 ldur     d0, [x0, #0xb7]
019287d0 +11ac ldr      x4, [x27, #0x418]
019287d4 +11b0 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
019287d8 +11b4 mov      v1.16b, v0.16b
019287dc +11b8 ldur     x0, [x29, #-0xa8]
019287e0 +11bc ldur     d0, [x29, #-0xe0]
019287e4 +11c0 fsub     d2, d0, d1
019287e8 +11c4 stur     d2, [x0, #0x47]
019287ec +11c8 mov      x1, x22
019287f0 +11cc mov      x2, #0x44
019287f4 +11d0 bl       #0x1b58288 => (None, None)
019287f8 +11d4 stur     x0, [x29, #-0xc0]
019287fc +11d8 add      x16, x27, #0x27, lsl #12
01928800 +11dc ldr      x16, [x16, #0x348]
01928804 +11e0 stur     w16, [x0, #0xf]
01928808 +11e4 ldur     x2, [x29, #-0xa8]
0192880c +11e8 ldur     w1, [x2, #0x5b]
01928810 +11ec add      x1, x1, x28, lsl #32
01928814 +11f0 bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
01928818 +11f4 ldur     x1, [x29, #-0xc0]
0192881c +11f8 add      x25, x1, #0x13
01928820 +11fc str      w0, [x25]
01928824 +1200 tbz      w0, #0, #0x1928840
01928828 +1204 ldurb    w16, [x1, #-1]
0192882c +1208 ldurb    w17, [x0, #-1]
01928830 +120c and      x16, x17, x16, lsr #2
01928834 +1210 tst      x16, x28, lsr #32
01928838 +1214 b.eq     #0x1928840
0192883c +1218 bl       #0x1b56534 => (None, None)
01928840 +121c ldur     x0, [x29, #-0xc0]
01928844 +1220 add      x16, x27, #0x27, lsl #12
01928848 +1224 ldr      x16, [x16, #0x350]
0192884c +1228 stur     w16, [x0, #0x17]
01928850 +122c ldur     x1, [x29, #-0xa8]
01928854 +1230 bl       #0x8dd794 => ('GridConfig.countCellY', 9295764)
01928858 +1234 mov      x1, x0
0192885c +1238 bl       #0x17d148c => ('RxObjectMixin.value', 24974476)
01928860 +123c ldur     x1, [x29, #-0xc0]
01928864 +1240 add      x25, x1, #0x1b
01928868 +1244 str      w0, [x25]
0192886c +1248 tbz      w0, #0, #0x1928888
01928870 +124c ldurb    w16, [x1, #-1]
01928874 +1250 ldurb    w17, [x0, #-1]
01928878 +1254 and      x16, x17, x16, lsr #2
0192887c +1258 tst      x16, x28, lsr #32
01928880 +125c b.eq     #0x1928888
01928884 +1260 bl       #0x1b56534 => (None, None)
01928888 +1264 ldur     x1, [x29, #-0xc0]
0192888c +1268 add      x16, x27, #0x27, lsl #12
01928890 +126c ldr      x16, [x16, #0x358]
01928894 +1270 stur     w16, [x1, #0x1f]
01928898 +1274 ldur     x0, [x29, #-0xa8]
0192889c +1278 ldur     w3, [x0, #0x1f]
019288a0 +127c add      x3, x3, x28, lsl #32
019288a4 +1280 stur     x3, [x29, #-0xc8]
019288a8 +1284 ldur     x2, [x3, #-1]
019288ac +1288 ubfx     x2, x2, #0xc, #0x14
019288b0 +128c cmp      x2, #0x785
019288b4 +1290 b.ne     #0x19288c8
019288b8 +1294 ldur     d0, [x3, #0x1f]
019288bc +1298 ldr      x4, [x27, #0x418]
019288c0 +129c bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
019288c4 +12a0 b        #0x19288e8
019288c8 +12a4 mov      x1, x3
019288cc +12a8 add      x2, x27, #0x27, lsl #12
019288d0 +12ac ldr      x2, [x2, #0x360]
019288d4 +12b0 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
019288d8 +12b4 ldur     x0, [x29, #-0xc8]
019288dc +12b8 ldur     d0, [x0, #0x1f]
019288e0 +12bc ldr      x4, [x27, #0x418]
019288e4 +12c0 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
019288e8 +12c4 ldur     x3, [x29, #-0xa8]
019288ec +12c8 ldur     x2, [x29, #-0xc0]
019288f0 +12cc ldp      x0, x1, [x26, #0x60]
019288f4 +12d0 add      x0, x0, #0x10
019288f8 +12d4 cmp      x1, x0
019288fc +12d8 b.ls     #0x1929200
01928900 +12dc str      x0, [x26, #0x60]
01928904 +12e0 sub      x0, x0, #0xf
01928908 +12e4 mov      x1, #0xe15c
0192890c +12e8 movk     x1, #3, lsl #16
01928910 +12ec stur     x1, [x0, #-1]
01928914 +12f0 stur     d0, [x0, #7]
01928918 +12f4 mov      x1, x2
0192891c +12f8 add      x25, x1, #0x23
01928920 +12fc str      w0, [x25]
01928924 +1300 tbz      w0, #0, #0x1928940
01928928 +1304 ldurb    w16, [x1, #-1]
0192892c +1308 ldurb    w17, [x0, #-1]
01928930 +130c and      x16, x17, x16, lsr #2
01928934 +1310 tst      x16, x28, lsr #32
01928938 +1314 b.eq     #0x1928940
0192893c +1318 bl       #0x1b56534 => (None, None)
01928940 +131c add      x16, x27, #0x27, lsl #12
01928944 +1320 ldr      x16, [x16, #0x368]
01928948 +1324 stur     w16, [x2, #0x27]
0192894c +1328 mov      x1, x3
01928950 +132c bl       #0xa458a0 => ('GridConfig.getScreenMarginTop', 10770592)
01928954 +1330 ldp      x0, x1, [x26, #0x60]
01928958 +1334 add      x0, x0, #0x10
0192895c +1338 cmp      x1, x0
01928960 +133c b.ls     #0x1929218
01928964 +1340 str      x0, [x26, #0x60]
01928968 +1344 sub      x0, x0, #0xf
0192896c +1348 mov      x1, #0xe15c
01928970 +134c movk     x1, #3, lsl #16
01928974 +1350 stur     x1, [x0, #-1]
01928978 +1354 stur     d0, [x0, #7]
0192897c +1358 ldur     x1, [x29, #-0xc0]
01928980 +135c add      x25, x1, #0x2b
01928984 +1360 str      w0, [x25]
01928988 +1364 tbz      w0, #0, #0x19289a4
0192898c +1368 ldurb    w16, [x1, #-1]
01928990 +136c ldurb    w17, [x0, #-1]
01928994 +1370 and      x16, x17, x16, lsr #2
01928998 +1374 tst      x16, x28, lsr #32
0192899c +1378 b.eq     #0x19289a4
019289a0 +137c bl       #0x1b56534 => (None, None)
019289a4 +1380 ldur     x0, [x29, #-0xc0]
019289a8 +1384 add      x16, x27, #0x27, lsl #12
019289ac +1388 ldr      x16, [x16, #0x370]
019289b0 +138c stur     w16, [x0, #0x2f]
019289b4 +1390 ldur     x3, [x29, #-0xa8]
019289b8 +1394 ldur     w4, [x3, #0x1f]
019289bc +1398 add      x4, x4, x28, lsl #32
019289c0 +139c stur     x4, [x29, #-0xc8]
019289c4 +13a0 ldur     x1, [x4, #-1]
019289c8 +13a4 ubfx     x1, x1, #0xc, #0x14
019289cc +13a8 cmp      x1, #0x785
019289d0 +13ac b.ne     #0x19289ec
019289d4 +13b0 ldur     w1, [x4, #0x3f]
019289d8 +13b4 add      x1, x1, x28, lsl #32
019289dc +13b8 mov      x4, x3
019289e0 +13bc mov      x3, x0
019289e4 +13c0 mov      x0, x1
019289e8 +13c4 b        #0x1928a14
019289ec +13c8 mov      x1, x4
019289f0 +13cc add      x2, x27, #0x27, lsl #12
019289f4 +13d0 ldr      x2, [x2, #0x378]
019289f8 +13d4 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
019289fc +13d8 ldur     x0, [x29, #-0xc8]
01928a00 +13dc ldur     w1, [x0, #0x3f]
01928a04 +13e0 add      x1, x1, x28, lsl #32
01928a08 +13e4 mov      x0, x1
01928a0c +13e8 ldur     x4, [x29, #-0xa8]
01928a10 +13ec ldur     x3, [x29, #-0xc0]
01928a14 +13f0 mov      x1, x3
01928a18 +13f4 add      x25, x1, #0x33
01928a1c +13f8 str      w0, [x25]
01928a20 +13fc tbz      w0, #0, #0x1928a3c
01928a24 +1400 ldurb    w16, [x1, #-1]
01928a28 +1404 ldurb    w17, [x0, #-1]
01928a2c +1408 and      x16, x17, x16, lsr #2
01928a30 +140c tst      x16, x28, lsr #32
01928a34 +1410 b.eq     #0x1928a3c
01928a38 +1414 bl       #0x1b56534 => (None, None)
01928a3c +1418 add      x16, x27, #0x27, lsl #12
01928a40 +141c ldr      x16, [x16, #0x380]
01928a44 +1420 stur     w16, [x3, #0x37]
01928a48 +1424 ldur     w1, [x4, #0x1f]
01928a4c +1428 add      x1, x1, x28, lsl #32
01928a50 +142c ldur     x0, [x1, #-1]
01928a54 +1430 ubfx     x0, x0, #0xc, #0x14
01928a58 +1434 cmp      x0, #0x785
01928a5c +1438 b.ne     #0x1928a6c
01928a60 +143c mov      x0, x4
01928a64 +1440 mov      x1, x3
01928a68 +1444 b        #0x1928a80
01928a6c +1448 add      x2, x27, #0x27, lsl #12
01928a70 +144c ldr      x2, [x2, #0x388]
01928a74 +1450 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928a78 +1454 ldur     x0, [x29, #-0xa8]
01928a7c +1458 ldur     x1, [x29, #-0xc0]
01928a80 +145c stur     wzr, [x1, #0x3b]
01928a84 +1460 add      x16, x27, #0x27, lsl #12
01928a88 +1464 ldr      x16, [x16, #0x390]
01928a8c +1468 stur     w16, [x1, #0x3f]
01928a90 +146c ldur     w3, [x0, #0x1f]
01928a94 +1470 add      x3, x3, x28, lsl #32
01928a98 +1474 stur     x3, [x29, #-0xc8]
01928a9c +1478 ldur     x2, [x3, #-1]
01928aa0 +147c ubfx     x2, x2, #0xc, #0x14
01928aa4 +1480 cmp      x2, #0x785
01928aa8 +1484 b.ne     #0x1928abc
01928aac +1488 ldur     d0, [x3, #0x47]
01928ab0 +148c ldr      x4, [x27, #0x418]
01928ab4 +1490 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928ab8 +1494 b        #0x1928adc
01928abc +1498 mov      x1, x3
01928ac0 +149c add      x2, x27, #0x27, lsl #12
01928ac4 +14a0 ldr      x2, [x2, #0x328]
01928ac8 +14a4 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928acc +14a8 ldur     x0, [x29, #-0xc8]
01928ad0 +14ac ldur     d0, [x0, #0x47]
01928ad4 +14b0 ldr      x4, [x27, #0x418]
01928ad8 +14b4 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928adc +14b8 ldur     x3, [x29, #-0xa8]
01928ae0 +14bc ldur     x2, [x29, #-0xc0]
01928ae4 +14c0 ldp      x0, x1, [x26, #0x60]
01928ae8 +14c4 add      x0, x0, #0x10
01928aec +14c8 cmp      x1, x0
01928af0 +14cc b.ls     #0x1929228
01928af4 +14d0 str      x0, [x26, #0x60]
01928af8 +14d4 sub      x0, x0, #0xf
01928afc +14d8 mov      x1, #0xe15c
01928b00 +14dc movk     x1, #3, lsl #16
01928b04 +14e0 stur     x1, [x0, #-1]
01928b08 +14e4 stur     d0, [x0, #7]
01928b0c +14e8 mov      x1, x2
01928b10 +14ec add      x25, x1, #0x43
01928b14 +14f0 str      w0, [x25]
01928b18 +14f4 tbz      w0, #0, #0x1928b34
01928b1c +14f8 ldurb    w16, [x1, #-1]
01928b20 +14fc ldurb    w17, [x0, #-1]
01928b24 +1500 and      x16, x17, x16, lsr #2
01928b28 +1504 tst      x16, x28, lsr #32
01928b2c +1508 b.eq     #0x1928b34
01928b30 +150c bl       #0x1b56534 => (None, None)
01928b34 +1510 add      x16, x27, #0x27, lsl #12
01928b38 +1514 ldr      x16, [x16, #0x398]
01928b3c +1518 stur     w16, [x2, #0x47]
01928b40 +151c ldur     w0, [x3, #0x1f]
01928b44 +1520 add      x0, x0, x28, lsl #32
01928b48 +1524 stur     x0, [x29, #-0xc8]
01928b4c +1528 ldur     x1, [x0, #-1]
01928b50 +152c ubfx     x1, x1, #0xc, #0x14
01928b54 +1530 cmp      x1, #0x785
01928b58 +1534 b.ne     #0x1928b6c
01928b5c +1538 ldur     d0, [x0, #0x4f]
01928b60 +153c ldr      x4, [x27, #0x418]
01928b64 +1540 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928b68 +1544 b        #0x1928b8c
01928b6c +1548 mov      x1, x0
01928b70 +154c add      x2, x27, #0x27, lsl #12
01928b74 +1550 ldr      x2, [x2, #0x3a0]
01928b78 +1554 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928b7c +1558 ldur     x0, [x29, #-0xc8]
01928b80 +155c ldur     d0, [x0, #0x4f]
01928b84 +1560 ldr      x4, [x27, #0x418]
01928b88 +1564 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928b8c +1568 ldur     x3, [x29, #-0xa8]
01928b90 +156c ldur     x2, [x29, #-0xc0]
01928b94 +1570 ldp      x0, x1, [x26, #0x60]
01928b98 +1574 add      x0, x0, #0x10
01928b9c +1578 cmp      x1, x0
01928ba0 +157c b.ls     #0x1929240
01928ba4 +1580 str      x0, [x26, #0x60]
01928ba8 +1584 sub      x0, x0, #0xf
01928bac +1588 mov      x1, #0xe15c
01928bb0 +158c movk     x1, #3, lsl #16
01928bb4 +1590 stur     x1, [x0, #-1]
01928bb8 +1594 stur     d0, [x0, #7]
01928bbc +1598 mov      x1, x2
01928bc0 +159c add      x25, x1, #0x4b
01928bc4 +15a0 str      w0, [x25]
01928bc8 +15a4 tbz      w0, #0, #0x1928be4
01928bcc +15a8 ldurb    w16, [x1, #-1]
01928bd0 +15ac ldurb    w17, [x0, #-1]
01928bd4 +15b0 and      x16, x17, x16, lsr #2
01928bd8 +15b4 tst      x16, x28, lsr #32
01928bdc +15b8 b.eq     #0x1928be4
01928be0 +15bc bl       #0x1b56534 => (None, None)
01928be4 +15c0 add      x16, x27, #0x27, lsl #12
01928be8 +15c4 ldr      x16, [x16, #0x3a8]
01928bec +15c8 stur     w16, [x2, #0x4f]
01928bf0 +15cc mov      x1, x3
01928bf4 +15d0 bl       #0xa47154 => ('GridConfig.iconSize', 10776916)
01928bf8 +15d4 ldp      x0, x1, [x26, #0x60]
01928bfc +15d8 add      x0, x0, #0x10
01928c00 +15dc cmp      x1, x0
01928c04 +15e0 b.ls     #0x1929258
01928c08 +15e4 str      x0, [x26, #0x60]
01928c0c +15e8 sub      x0, x0, #0xf
01928c10 +15ec mov      x1, #0xe15c
01928c14 +15f0 movk     x1, #3, lsl #16
01928c18 +15f4 stur     x1, [x0, #-1]
01928c1c +15f8 stur     d0, [x0, #7]
01928c20 +15fc ldur     x1, [x29, #-0xc0]
01928c24 +1600 add      x25, x1, #0x53
01928c28 +1604 str      w0, [x25]
01928c2c +1608 tbz      w0, #0, #0x1928c48
01928c30 +160c ldurb    w16, [x1, #-1]
01928c34 +1610 ldurb    w17, [x0, #-1]
01928c38 +1614 and      x16, x17, x16, lsr #2
01928c3c +1618 tst      x16, x28, lsr #32
01928c40 +161c b.eq     #0x1928c48
01928c44 +1620 bl       #0x1b56534 => (None, None)
01928c48 +1624 ldur     x0, [x29, #-0xc0]
01928c4c +1628 add      x16, x27, #0x27, lsl #12
01928c50 +162c ldr      x16, [x16, #0x3b0]
01928c54 +1630 stur     w16, [x0, #0x57]
01928c58 +1634 ldur     x3, [x29, #-0xa8]
01928c5c +1638 ldur     w4, [x3, #0x1f]
01928c60 +163c add      x4, x4, x28, lsl #32
01928c64 +1640 stur     x4, [x29, #-0xc8]
01928c68 +1644 ldur     x1, [x4, #-1]
01928c6c +1648 ubfx     x1, x1, #0xc, #0x14
01928c70 +164c cmp      x1, #0x785
01928c74 +1650 b.ne     #0x1928c90
01928c78 +1654 ldur     w1, [x4, #0x7f]
01928c7c +1658 add      x1, x1, x28, lsl #32
01928c80 +165c mov      x4, x3
01928c84 +1660 mov      x3, x0
01928c88 +1664 mov      x0, x1
01928c8c +1668 b        #0x1928cb8
01928c90 +166c mov      x1, x4
01928c94 +1670 add      x2, x27, #0x27, lsl #12
01928c98 +1674 ldr      x2, [x2, #0x3b8]
01928c9c +1678 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928ca0 +167c ldur     x0, [x29, #-0xc8]
01928ca4 +1680 ldur     w1, [x0, #0x7f]
01928ca8 +1684 add      x1, x1, x28, lsl #32
01928cac +1688 mov      x0, x1
01928cb0 +168c ldur     x4, [x29, #-0xa8]
01928cb4 +1690 ldur     x3, [x29, #-0xc0]
01928cb8 +1694 mov      x1, x3
01928cbc +1698 add      x25, x1, #0x5b
01928cc0 +169c str      w0, [x25]
01928cc4 +16a0 tbz      w0, #0, #0x1928ce0
01928cc8 +16a4 ldurb    w16, [x1, #-1]
01928ccc +16a8 ldurb    w17, [x0, #-1]
01928cd0 +16ac and      x16, x17, x16, lsr #2
01928cd4 +16b0 tst      x16, x28, lsr #32
01928cd8 +16b4 b.eq     #0x1928ce0
01928cdc +16b8 bl       #0x1b56534 => (None, None)
01928ce0 +16bc add      x16, x27, #0x27, lsl #12
01928ce4 +16c0 ldr      x16, [x16, #0x3c0]
01928ce8 +16c4 stur     w16, [x3, #0x5f]
01928cec +16c8 ldur     w0, [x4, #0x1f]
01928cf0 +16cc add      x0, x0, x28, lsl #32
01928cf4 +16d0 stur     x0, [x29, #-0xc8]
01928cf8 +16d4 ldur     x1, [x0, #-1]
01928cfc +16d8 ubfx     x1, x1, #0xc, #0x14
01928d00 +16dc cmp      x1, #0x785
01928d04 +16e0 b.ne     #0x1928d20
01928d08 +16e4 ldur     w1, [x0, #0x83]
01928d0c +16e8 add      x1, x1, x28, lsl #32
01928d10 +16ec mov      x0, x1
01928d14 +16f0 mov      x2, x3
01928d18 +16f4 mov      x3, x4
01928d1c +16f8 b        #0x1928d48
01928d20 +16fc mov      x1, x0
01928d24 +1700 add      x2, x27, #0x27, lsl #12
01928d28 +1704 ldr      x2, [x2, #0x3c8]
01928d2c +1708 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928d30 +170c ldur     x0, [x29, #-0xc8]
01928d34 +1710 ldur     w1, [x0, #0x83]
01928d38 +1714 add      x1, x1, x28, lsl #32
01928d3c +1718 mov      x0, x1
01928d40 +171c ldur     x3, [x29, #-0xa8]
01928d44 +1720 ldur     x2, [x29, #-0xc0]
01928d48 +1724 mov      x1, x2
01928d4c +1728 add      x25, x1, #0x63
01928d50 +172c str      w0, [x25]
01928d54 +1730 tbz      w0, #0, #0x1928d70
01928d58 +1734 ldurb    w16, [x1, #-1]
01928d5c +1738 ldurb    w17, [x0, #-1]
01928d60 +173c and      x16, x17, x16, lsr #2
01928d64 +1740 tst      x16, x28, lsr #32
01928d68 +1744 b.eq     #0x1928d70
01928d6c +1748 bl       #0x1b56534 => (None, None)
01928d70 +174c add      x16, x27, #0x27, lsl #12
01928d74 +1750 ldr      x16, [x16, #0x3d0]
01928d78 +1754 stur     w16, [x2, #0x67]
01928d7c +1758 ldur     w0, [x3, #0x1f]
01928d80 +175c add      x0, x0, x28, lsl #32
01928d84 +1760 stur     x0, [x29, #-0xc8]
01928d88 +1764 ldur     x1, [x0, #-1]
01928d8c +1768 ubfx     x1, x1, #0xc, #0x14
01928d90 +176c cmp      x1, #0x785
01928d94 +1770 b.ne     #0x1928da4
01928d98 +1774 mov      x1, x0
01928d9c +1778 bl       #0xa47134 => ('GridSizeCalRules.hotseatCellHeight', 10776884)
01928da0 +177c b        #0x1928dc4
01928da4 +1780 mov      x1, x0
01928da8 +1784 add      x2, x27, #0x27, lsl #12
01928dac +1788 ldr      x2, [x2, #0x3d8]
01928db0 +178c bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928db4 +1790 ldur     x0, [x29, #-0xc8]
01928db8 +1794 ldur     d0, [x0, #0x87]
01928dbc +1798 ldr      x4, [x27, #0x418]
01928dc0 +179c bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928dc4 +17a0 ldur     x3, [x29, #-0xa8]
01928dc8 +17a4 ldur     x2, [x29, #-0xc0]
01928dcc +17a8 ldp      x0, x1, [x26, #0x60]
01928dd0 +17ac add      x0, x0, #0x10
01928dd4 +17b0 cmp      x1, x0
01928dd8 +17b4 b.ls     #0x1929268
01928ddc +17b8 str      x0, [x26, #0x60]
01928de0 +17bc sub      x0, x0, #0xf
01928de4 +17c0 mov      x1, #0xe15c
01928de8 +17c4 movk     x1, #3, lsl #16
01928dec +17c8 stur     x1, [x0, #-1]
01928df0 +17cc stur     d0, [x0, #7]
01928df4 +17d0 mov      x1, x2
01928df8 +17d4 add      x25, x1, #0x6b
01928dfc +17d8 str      w0, [x25]
01928e00 +17dc tbz      w0, #0, #0x1928e1c
01928e04 +17e0 ldurb    w16, [x1, #-1]
01928e08 +17e4 ldurb    w17, [x0, #-1]
01928e0c +17e8 and      x16, x17, x16, lsr #2
01928e10 +17ec tst      x16, x28, lsr #32
01928e14 +17f0 b.eq     #0x1928e1c
01928e18 +17f4 bl       #0x1b56534 => (None, None)
01928e1c +17f8 add      x16, x27, #0x27, lsl #12
01928e20 +17fc ldr      x16, [x16, #0x3e0]
01928e24 +1800 stur     w16, [x2, #0x6f]
01928e28 +1804 ldur     w0, [x3, #0x1f]
01928e2c +1808 add      x0, x0, x28, lsl #32
01928e30 +180c stur     x0, [x29, #-0xc8]
01928e34 +1810 ldur     x1, [x0, #-1]
01928e38 +1814 ubfx     x1, x1, #0xc, #0x14
01928e3c +1818 cmp      x1, #0x785
01928e40 +181c b.ne     #0x1928e50
01928e44 +1820 mov      x1, x0
01928e48 +1824 bl       #0xa47114 => ('GridSizeCalRules.hotseatMarginBottom', 10776852)
01928e4c +1828 b        #0x1928e70
01928e50 +182c mov      x1, x0
01928e54 +1830 add      x2, x27, #0x27, lsl #12
01928e58 +1834 ldr      x2, [x2, #0x3e8]
01928e5c +1838 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928e60 +183c ldur     x0, [x29, #-0xc8]
01928e64 +1840 ldur     d0, [x0, #0x9f]
01928e68 +1844 ldr      x4, [x27, #0x418]
01928e6c +1848 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928e70 +184c ldur     x3, [x29, #-0xa8]
01928e74 +1850 ldur     x2, [x29, #-0xc0]
01928e78 +1854 ldp      x0, x1, [x26, #0x60]
01928e7c +1858 add      x0, x0, #0x10
01928e80 +185c cmp      x1, x0
01928e84 +1860 b.ls     #0x1929280
01928e88 +1864 str      x0, [x26, #0x60]
01928e8c +1868 sub      x0, x0, #0xf
01928e90 +186c mov      x1, #0xe15c
01928e94 +1870 movk     x1, #3, lsl #16
01928e98 +1874 stur     x1, [x0, #-1]
01928e9c +1878 stur     d0, [x0, #7]
01928ea0 +187c mov      x1, x2
01928ea4 +1880 add      x25, x1, #0x73
01928ea8 +1884 str      w0, [x25]
01928eac +1888 tbz      w0, #0, #0x1928ec8
01928eb0 +188c ldurb    w16, [x1, #-1]
01928eb4 +1890 ldurb    w17, [x0, #-1]
01928eb8 +1894 and      x16, x17, x16, lsr #2
01928ebc +1898 tst      x16, x28, lsr #32
01928ec0 +189c b.eq     #0x1928ec8
01928ec4 +18a0 bl       #0x1b56534 => (None, None)
01928ec8 +18a4 add      x16, x27, #0x27, lsl #12
01928ecc +18a8 ldr      x16, [x16, #0x3f0]
01928ed0 +18ac stur     w16, [x2, #0x77]
01928ed4 +18b0 ldur     w0, [x3, #0x1f]
01928ed8 +18b4 add      x0, x0, x28, lsl #32
01928edc +18b8 stur     x0, [x29, #-0xc8]
01928ee0 +18bc ldur     x1, [x0, #-1]
01928ee4 +18c0 ubfx     x1, x1, #0xc, #0x14
01928ee8 +18c4 cmp      x1, #0x785
01928eec +18c8 b.ne     #0x1928efc
01928ef0 +18cc mov      x1, x0
01928ef4 +18d0 bl       #0xa470f4 => ('GridSizeCalRules.searchBarMarginBottom', 10776820)
01928ef8 +18d4 b        #0x1928f1c
01928efc +18d8 mov      x1, x0
01928f00 +18dc add      x2, x27, #0x27, lsl #12
01928f04 +18e0 ldr      x2, [x2, #0x3f8]
01928f08 +18e4 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
01928f0c +18e8 ldur     x0, [x29, #-0xc8]
01928f10 +18ec ldur     d0, [x0, #0xaf]
01928f14 +18f0 ldr      x4, [x27, #0x418]
01928f18 +18f4 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01928f1c +18f8 ldur     x3, [x29, #-0xa8]
01928f20 +18fc ldur     x2, [x29, #-0xc0]
01928f24 +1900 ldp      x0, x1, [x26, #0x60]
01928f28 +1904 add      x0, x0, #0x10
01928f2c +1908 cmp      x1, x0
01928f30 +190c b.ls     #0x1929298
01928f34 +1910 str      x0, [x26, #0x60]
01928f38 +1914 sub      x0, x0, #0xf
01928f3c +1918 mov      x1, #0xe15c
01928f40 +191c movk     x1, #3, lsl #16
01928f44 +1920 stur     x1, [x0, #-1]
01928f48 +1924 stur     d0, [x0, #7]
01928f4c +1928 mov      x1, x2
01928f50 +192c add      x25, x1, #0x7b
01928f54 +1930 str      w0, [x25]
01928f58 +1934 tbz      w0, #0, #0x1928f74
01928f5c +1938 ldurb    w16, [x1, #-1]
01928f60 +193c ldurb    w17, [x0, #-1]
01928f64 +1940 and      x16, x17, x16, lsr #2
01928f68 +1944 tst      x16, x28, lsr #32
01928f6c +1948 b.eq     #0x1928f74
01928f70 +194c bl       #0x1b56534 => (None, None)
01928f74 +1950 add      x16, x27, #0x27, lsl #12
01928f78 +1954 ldr      x16, [x16, #0x400]
01928f7c +1958 stur     w16, [x2, #0x7f]
01928f80 +195c ldur     d0, [x3, #0x3f]
01928f84 +1960 ldp      x0, x1, [x26, #0x60]
01928f88 +1964 add      x0, x0, #0x10
01928f8c +1968 cmp      x1, x0
01928f90 +196c b.ls     #0x19292b0
01928f94 +1970 str      x0, [x26, #0x60]
01928f98 +1974 sub      x0, x0, #0xf
01928f9c +1978 mov      x1, #0xe15c
01928fa0 +197c movk     x1, #3, lsl #16
01928fa4 +1980 stur     x1, [x0, #-1]
01928fa8 +1984 stur     d0, [x0, #7]
01928fac +1988 mov      x1, x2
01928fb0 +198c add      x25, x1, #0x83
01928fb4 +1990 str      w0, [x25]
01928fb8 +1994 tbz      w0, #0, #0x1928fd4
01928fbc +1998 ldurb    w16, [x1, #-1]
01928fc0 +199c ldurb    w17, [x0, #-1]
01928fc4 +19a0 and      x16, x17, x16, lsr #2
01928fc8 +19a4 tst      x16, x28, lsr #32
01928fcc +19a8 b.eq     #0x1928fd4
01928fd0 +19ac bl       #0x1b56534 => (None, None)
01928fd4 +19b0 add      x16, x27, #0x27, lsl #12
01928fd8 +19b4 ldr      x16, [x16, #0x408]
01928fdc +19b8 stur     w16, [x2, #0x87]
01928fe0 +19bc ldur     d0, [x3, #0x47]
01928fe4 +19c0 ldp      x0, x1, [x26, #0x60]
01928fe8 +19c4 add      x0, x0, #0x10
01928fec +19c8 cmp      x1, x0
01928ff0 +19cc b.ls     #0x19292c8
01928ff4 +19d0 str      x0, [x26, #0x60]
01928ff8 +19d4 sub      x0, x0, #0xf
01928ffc +19d8 mov      x1, #0xe15c
01929000 +19dc movk     x1, #3, lsl #16
01929004 +19e0 stur     x1, [x0, #-1]
01929008 +19e4 stur     d0, [x0, #7]
0192900c +19e8 mov      x1, x2
01929010 +19ec add      x25, x1, #0x8b
01929014 +19f0 str      w0, [x25]
01929018 +19f4 tbz      w0, #0, #0x1929034
0192901c +19f8 ldurb    w16, [x1, #-1]
01929020 +19fc ldurb    w17, [x0, #-1]
01929024 +1a00 and      x16, x17, x16, lsr #2
01929028 +1a04 tst      x16, x28, lsr #32
0192902c +1a08 b.eq     #0x1929034
01929030 +1a0c bl       #0x1b56534 => (None, None)
01929034 +1a10 add      x16, x27, #0x27, lsl #12
01929038 +1a14 ldr      x16, [x16, #0x410]
0192903c +1a18 stur     w16, [x2, #0x8f]
01929040 +1a1c ldur     w0, [x3, #0x1f]
01929044 +1a20 add      x0, x0, x28, lsl #32
01929048 +1a24 stur     x0, [x29, #-0xc8]
0192904c +1a28 ldur     x1, [x0, #-1]
01929050 +1a2c ubfx     x1, x1, #0xc, #0x14
01929054 +1a30 cmp      x1, #0x785
01929058 +1a34 b.ne     #0x192906c
0192905c +1a38 ldur     d0, [x0, #0xb7]
01929060 +1a3c ldr      x4, [x27, #0x418]
01929064 +1a40 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
01929068 +1a44 b        #0x192908c
0192906c +1a48 mov      x1, x0
01929070 +1a4c add      x2, x27, #0x27, lsl #12
01929074 +1a50 ldr      x2, [x2, #0x340]
01929078 +1a54 bl       #0x8dd7f8 => ('_UninitializedPhoneDeviceRules._logAccess', 9295864)
0192907c +1a58 ldur     x0, [x29, #-0xc8]
01929080 +1a5c ldur     d0, [x0, #0xb7]
01929084 +1a60 ldr      x4, [x27, #0x418]
01929088 +1a64 bl       #0xa44308 => ('PixelPerfect|alignPixel', 10765064)
0192908c +1a68 ldp      x0, x1, [x26, #0x60]
01929090 +1a6c add      x0, x0, #0x10
01929094 +1a70 cmp      x1, x0
01929098 +1a74 b.ls     #0x19292e0
0192909c +1a78 str      x0, [x26, #0x60]
019290a0 +1a7c sub      x0, x0, #0xf
019290a4 +1a80 mov      x1, #0xe15c
019290a8 +1a84 movk     x1, #3, lsl #16
019290ac +1a88 stur     x1, [x0, #-1]
019290b0 +1a8c stur     d0, [x0, #7]
019290b4 +1a90 ldur     x1, [x29, #-0xc0]
019290b8 +1a94 add      x25, x1, #0x93
019290bc +1a98 str      w0, [x25]
019290c0 +1a9c tbz      w0, #0, #0x19290dc
019290c4 +1aa0 ldurb    w16, [x1, #-1]
019290c8 +1aa4 ldurb    w17, [x0, #-1]
019290cc +1aa8 and      x16, x17, x16, lsr #2
019290d0 +1aac tst      x16, x28, lsr #32
019290d4 +1ab0 b.eq     #0x19290dc
019290d8 +1ab4 bl       #0x1b56534 => (None, None)
019290dc +1ab8 ldur     x16, [x29, #-0xc0]
019290e0 +1abc str      x16, [x15]
019290e4 +1ac0 bl       #0x868b8c => ('_StringBase._interpolate', 8817548)
019290e8 +1ac4 ldur     x1, [x29, #-0xb8]
019290ec +1ac8 mov      x2, x0
019290f0 +1acc ldr      x4, [x27, #0x70]
019290f4 +1ad0 bl       #0x8b3840 => ('_LoggerImpl.i', 9123904)
019290f8 +1ad4 mov      x0, x22
019290fc +1ad8 b        #0x18ac864
01929100 +1adc str      q0, [x15, #-0x10]!
01929104 +1ae0 bl       #0x1b581e0 => (None, None)
01929108 +1ae4 mov      x2, x0
0192910c +1ae8 ldr      q0, [x15], #0x10
01929110 +1aec b        #0x19276ec
01929114 +1af0 bl       #0x1b58bc8 => (None, None)
01929118 +1af4 bl       #0x1b58a18 => (None, None)
0192911c +1af8 stp      q2, q3, [x15, #-0x20]!
01929120 +1afc str      q0, [x15, #-0x10]!
01929124 +1b00 str      x3, [x15, #-8]!
01929128 +1b04 bl       #0x1b581e0 => (None, None)
0192912c +1b08 ldr      x3, [x15], #8
01929130 +1b0c ldr      q0, [x15], #0x10
01929134 +1b10 ldp      q2, q3, [x15], #0x20
01929138 +1b14 b        #0x1927d18
0192913c +1b18 str      q0, [x15, #-0x10]!
01929140 +1b1c str      x3, [x15, #-8]!
01929144 +1b20 bl       #0x1b581e0 => (None, None)
01929148 +1b24 ldr      x3, [x15], #8
0192914c +1b28 ldr      q0, [x15], #0x10
01929150 +1b2c b        #0x1927d60
01929154 +1b30 stp      q1, q2, [x15, #-0x20]!
01929158 +1b34 str      q0, [x15, #-0x10]!
0192915c +1b38 bl       #0x1b581e0 => (None, None)
01929160 +1b3c ldr      q0, [x15], #0x10
01929164 +1b40 ldp      q1, q2, [x15], #0x20
01929168 +1b44 b        #0x1927e54
0192916c +1b48 str      q2, [x15, #-0x10]!
01929170 +1b4c str      x2, [x15, #-8]!
01929174 +1b50 bl       #0x1b581e0 => (None, None)
01929178 +1b54 ldr      x2, [x15], #8
0192917c +1b58 ldr      q2, [x15], #0x10
01929180 +1b5c b        #0x1927ea0
01929184 +1b60 stp      q1, q2, [x15, #-0x20]!
01929188 +1b64 str      q0, [x15, #-0x10]!
0192918c +1b68 bl       #0x1b581e0 => (None, None)
01929190 +1b6c mov      x1, x0
01929194 +1b70 ldr      q0, [x15], #0x10
01929198 +1b74 ldp      q1, q2, [x15], #0x20
0192919c +1b78 b        #0x1927f88
019291a0 +1b7c str      q2, [x15, #-0x10]!
019291a4 +1b80 stp      x1, x2, [x15, #-0x10]!
019291a8 +1b84 bl       #0x1b581e0 => (None, None)
019291ac +1b88 mov      x3, x0
019291b0 +1b8c ldp      x1, x2, [x15], #0x10
019291b4 +1b90 ldr      q2, [x15], #0x10
019291b8 +1b94 b        #0x1927fdc
019291bc +1b98 bl       #0x1b58bc8 => (None, None)
019291c0 +1b9c bl       #0x1b58bc8 => (None, None)
019291c4 +1ba0 add      x9, x27, #0x27, lsl #12
019291c8 +1ba4 ldr      x9, [x9, #0x418]
019291cc +1ba8 bl       #0x1b58ca0 => (None, None)
019291d0 +1bac str      q0, [x15, #-0x10]!
019291d4 +1bb0 stp      x2, x3, [x15, #-0x10]!
019291d8 +1bb4 bl       #0x1b581e0 => (None, None)
019291dc +1bb8 ldp      x2, x3, [x15], #0x10
019291e0 +1bbc ldr      q0, [x15], #0x10
019291e4 +1bc0 b        #0x19285f8
019291e8 +1bc4 add      x9, x27, #0x27, lsl #12
019291ec +1bc8 ldr      x9, [x9, #0x420]
019291f0 +1bcc bl       #0x1b58ca0 => (None, None)
019291f4 +1bd0 add      x9, x27, #0x27, lsl #12
019291f8 +1bd4 ldr      x9, [x9, #0x428]
019291fc +1bd8 bl       #0x1b58ca0 => (None, None)
01929200 +1bdc str      q0, [x15, #-0x10]!
01929204 +1be0 stp      x2, x3, [x15, #-0x10]!
01929208 +1be4 bl       #0x1b581e0 => (None, None)
0192920c +1be8 ldp      x2, x3, [x15], #0x10
01929210 +1bec ldr      q0, [x15], #0x10
01929214 +1bf0 b        #0x1928914
01929218 +1bf4 str      q0, [x15, #-0x10]!
0192921c +1bf8 bl       #0x1b581e0 => (None, None)
01929220 +1bfc ldr      q0, [x15], #0x10
01929224 +1c00 b        #0x1928978
01929228 +1c04 str      q0, [x15, #-0x10]!
0192922c +1c08 stp      x2, x3, [x15, #-0x10]!
01929230 +1c0c bl       #0x1b581e0 => (None, None)
01929234 +1c10 ldp      x2, x3, [x15], #0x10
01929238 +1c14 ldr      q0, [x15], #0x10
0192923c +1c18 b        #0x1928b08
01929240 +1c1c str      q0, [x15, #-0x10]!
01929244 +1c20 stp      x2, x3, [x15, #-0x10]!
01929248 +1c24 bl       #0x1b581e0 => (None, None)
0192924c +1c28 ldp      x2, x3, [x15], #0x10
01929250 +1c2c ldr      q0, [x15], #0x10
01929254 +1c30 b        #0x1928bb8
01929258 +1c34 str      q0, [x15, #-0x10]!
0192925c +1c38 bl       #0x1b581e0 => (None, None)
01929260 +1c3c ldr      q0, [x15], #0x10
01929264 +1c40 b        #0x1928c1c
01929268 +1c44 str      q0, [x15, #-0x10]!
0192926c +1c48 stp      x2, x3, [x15, #-0x10]!
01929270 +1c4c bl       #0x1b581e0 => (None, None)
01929274 +1c50 ldp      x2, x3, [x15], #0x10
01929278 +1c54 ldr      q0, [x15], #0x10
0192927c +1c58 b        #0x1928df0
01929280 +1c5c str      q0, [x15, #-0x10]!
01929284 +1c60 stp      x2, x3, [x15, #-0x10]!
01929288 +1c64 bl       #0x1b581e0 => (None, None)
0192928c +1c68 ldp      x2, x3, [x15], #0x10
01929290 +1c6c ldr      q0, [x15], #0x10
01929294 +1c70 b        #0x1928e9c
01929298 +1c74 str      q0, [x15, #-0x10]!
0192929c +1c78 stp      x2, x3, [x15, #-0x10]!
019292a0 +1c7c bl       #0x1b581e0 => (None, None)
019292a4 +1c80 ldp      x2, x3, [x15], #0x10
019292a8 +1c84 ldr      q0, [x15], #0x10
019292ac +1c88 b        #0x1928f48
019292b0 +1c8c str      q0, [x15, #-0x10]!
019292b4 +1c90 stp      x2, x3, [x15, #-0x10]!
019292b8 +1c94 bl       #0x1b581e0 => (None, None)
019292bc +1c98 ldp      x2, x3, [x15], #0x10
019292c0 +1c9c ldr      q0, [x15], #0x10
019292c4 +1ca0 b        #0x1928fa8
019292c8 +1ca4 str      q0, [x15, #-0x10]!
019292cc +1ca8 stp      x2, x3, [x15, #-0x10]!
019292d0 +1cac bl       #0x1b581e0 => (None, None)
019292d4 +1cb0 ldp      x2, x3, [x15], #0x10
019292d8 +1cb4 ldr      q0, [x15], #0x10
019292dc +1cb8 b        #0x1929008
019292e0 +1cbc str      q0, [x15, #-0x10]!
019292e4 +1cc0 bl       #0x1b581e0 => (None, None)
019292e8 +1cc4 ldr      q0, [x15], #0x10
019292ec +1cc8 b        #0x19290b0