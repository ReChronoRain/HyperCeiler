# FolderIconGetxController.getCenterGlobalPosition va=0xa381a8 size=0x288
  00a381a8: fd 79 bf a9        stp        x29, x30, [x15, #-0x10]!  ; [x15=dsp]
  00a381ac: fd 03 0f aa        mov        x29, x15  ; x15=dsp
  00a381b0: ef e1 00 d1        sub        x15, x15, #0x38  ; x15=dsp
  00a381b4: a1 83 1f f8        stur       x1, [x29, #-8]
  00a381b8: 41 00 80 d2        mov        x1, #2
  00a381bc: 0f 7c 44 94        bl         #0x1b571f8
  00a381c0: e4 03 00 aa        mov        x4, x0
  00a381c4: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a381c8: a4 03 1f f8        stur       x4, [x29, #-0x10]
  00a381cc: 80 f0 00 b8        stur       w0, [x4, #0xf]
  00a381d0: 01 30 48 b8        ldur       w1, [x0, #0x83]
  00a381d4: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a381d8: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a381dc: 3f 00 10 6b        cmp        w1, w16
  00a381e0: 20 12 00 54        b.eq       #0xa38424
  00a381e4: 22 70 43 f8        ldur       x2, [x1, #0x37]
  00a381e8: 23 f0 43 f8        ldur       x3, [x1, #0x3f]
  00a381ec: 25 f0 42 f8        ldur       x5, [x1, #0x2f]
  00a381f0: e1 03 02 aa        mov        x1, x2
  00a381f4: e2 03 03 aa        mov        x2, x3
  00a381f8: e3 03 05 aa        mov        x3, x5
  00a381fc: 02 03 00 94        bl         #0xa38e04  -> WidgetPositionUtil.getCellPosition
  00a38200: a0 83 1e f8        stur       x0, [x29, #-0x18]
  00a38204: 00 70 40 fc        ldur       d0, [x0, #7]
  00a38208: a0 03 1e fc        stur       d0, [x29, #-0x20]
  00a3820c: 41 3f 40 f9        ldr        x1, [x26, #0x78]  ; [x26=thread]
  00a38210: 21 88 53 f9        ldr        x1, [x1, #0x2710]
  00a38214: 3f 00 16 6b        cmp        w1, w22
  00a38218: c1 01 00 54        b.ne       #0xa38250
  00a3821c: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a38220: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a38224: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a38228: 1f 00 10 6b        cmp        w0, w16
  00a3822c: 61 00 00 54        b.ne       #0xa38238
  00a38230: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a38234: 67 78 44 94        bl         #0x1b563d0
  00a38238: 70 23 40 91        add        x16, x27, #8, lsl #12  ; x16=ip0, x27=pp
  00a3823c: 10 e2 47 f9        ldr        x16, [x16, #0xfc0]  ; x16=ip0, [x16=ip0]
  00a38240: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a38244: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a38248: d8 9d 03 94        bl         #0xb1f9a8  -> Inst|find
  00a3824c: e1 03 00 aa        mov        x1, x0
  00a38250: a0 83 5e f8        ldur       x0, [x29, #-0x18]
  00a38254: a0 03 5e fc        ldur       d0, [x29, #-0x20]
  00a38258: 08 88 03 94        bl         #0xb1a278  -> GridController.currentConfig
  00a3825c: 00 b0 42 fc        ldur       d0, [x0, #0x2b]
  00a38260: 01 10 6c 1e        fmov       d1, #0.50000000
  00a38264: 02 08 61 1e        fmul       d2, d0, d1
  00a38268: a0 03 5e fc        ldur       d0, [x29, #-0x20]
  00a3826c: 03 28 62 1e        fadd       d3, d0, d2
  00a38270: a0 83 5e f8        ldur       x0, [x29, #-0x18]
  00a38274: a3 83 1d fc        stur       d3, [x29, #-0x28]
  00a38278: 00 f0 40 fc        ldur       d0, [x0, #0xf]
  00a3827c: a0 03 1e fc        stur       d0, [x29, #-0x20]
  00a38280: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a38284: 00 88 53 f9        ldr        x0, [x0, #0x2710]
  00a38288: 1f 00 16 6b        cmp        w0, w22
  00a3828c: e1 01 00 54        b.ne       #0xa382c8
  00a38290: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a38294: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a38298: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a3829c: 1f 00 10 6b        cmp        w0, w16
  00a382a0: 61 00 00 54        b.ne       #0xa382ac
  00a382a4: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a382a8: 4a 78 44 94        bl         #0x1b563d0
  00a382ac: 70 23 40 91        add        x16, x27, #8, lsl #12  ; x16=ip0, x27=pp
  00a382b0: 10 e2 47 f9        ldr        x16, [x16, #0xfc0]  ; x16=ip0, [x16=ip0]
  00a382b4: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a382b8: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a382bc: bb 9d 03 94        bl         #0xb1f9a8  -> Inst|find
  00a382c0: e1 03 00 aa        mov        x1, x0
  00a382c4: 02 00 00 14        b          #0xa382cc  -> FolderIconGetxController.getCenterGlobalPosition
  00a382c8: e1 03 00 aa        mov        x1, x0
  00a382cc: a2 03 5f f8        ldur       x2, [x29, #-0x10]
  00a382d0: a0 83 5d fc        ldur       d0, [x29, #-0x28]
  00a382d4: a1 03 5e fc        ldur       d1, [x29, #-0x20]
  00a382d8: e8 87 03 94        bl         #0xb1a278  -> GridController.currentConfig
  00a382dc: 00 30 43 fc        ldur       d0, [x0, #0x33]
  00a382e0: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a382e4: a0 03 1d fc        stur       d0, [x29, #-0x30]
  00a382e8: 56 02 00 94        bl         #0xa38c40  -> FolderIconGetxController.getTitleHeight
  00a382ec: 01 1c a0 4e        mov        v1.16b, v0.16b
  00a382f0: a0 03 5d fc        ldur       d0, [x29, #-0x30]
  00a382f4: 02 38 61 1e        fsub       d2, d0, d1
  00a382f8: 00 10 6c 1e        fmov       d0, #0.50000000
  00a382fc: 41 08 60 1e        fmul       d1, d2, d0
  00a38300: a0 03 5e fc        ldur       d0, [x29, #-0x20]
  00a38304: 02 28 61 1e        fadd       d2, d0, d1
  00a38308: a2 03 1d fc        stur       d2, [x29, #-0x30]
  00a3830c: 40 f7 39 94        bl         #0x18b600c
  00a38310: a0 83 5d fc        ldur       d0, [x29, #-0x28]
  00a38314: 00 70 00 fc        stur       d0, [x0, #7]
  00a38318: a0 03 5d fc        ldur       d0, [x29, #-0x30]
  00a3831c: 00 f0 00 fc        stur       d0, [x0, #0xf]
  00a38320: a2 03 5f f8        ldur       x2, [x29, #-0x10]
  00a38324: 40 30 01 b8        stur       w0, [x2, #0x13]
  00a38328: 50 f0 5f 38        ldurb      w16, [x2, #-1]
  00a3832c: 11 f0 5f 38        ldurb      w17, [x0, #-1]
  00a38330: 30 0a 50 8a        and        x16, x17, x16, lsr #2  ; x16=ip0, x17=ip1
  00a38334: 1f 82 5c ea        tst        x16, x28, lsr #32  ; x16=ip0, x28=heap
  00a38338: 40 00 00 54        b.eq       #0xa38340
  00a3833c: 97 79 44 94        bl         #0x1b56998
  00a38340: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a38344: 00 48 52 f9        ldr        x0, [x0, #0x2490]
  00a38348: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a3834c: 1f 00 10 6b        cmp        w0, w16
  00a38350: 61 00 00 54        b.ne       #0xa3835c
  00a38354: 62 33 40 f9        ldr        x2, [x27, #0x60]  ; [x27=pp]
  00a38358: 1e 78 44 94        bl         #0x1b563d0
  00a3835c: a2 03 5f f8        ldur       x2, [x29, #-0x10]
  00a38360: 61 ab 40 91        add        x1, x27, #0x2a, lsl #12  ; x27=pp
  00a38364: 21 2c 41 f9        ldr        x1, [x1, #0x258]
  00a38368: a0 83 1e f8        stur       x0, [x29, #-0x18]
  00a3836c: 94 7c 44 94        bl         #0x1b575bc
  00a38370: a1 83 5e f8        ldur       x1, [x29, #-0x18]
  00a38374: e2 03 00 aa        mov        x2, x0
  00a38378: 64 3b 40 f9        ldr        x4, [x27, #0x70]  ; [x27=pp]
  00a3837c: 25 c1 f9 97        bl         #0x8a8810  -> _LoggerImpl.info
  00a38380: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a38384: 00 0c 5a f9        ldr        x0, [x0, #0x3418]
  00a38388: 1f 00 16 6b        cmp        w0, w22
  00a3838c: a1 01 00 54        b.ne       #0xa383c0
  00a38390: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a38394: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a38398: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a3839c: 1f 00 10 6b        cmp        w0, w16
  00a383a0: 61 00 00 54        b.ne       #0xa383ac
  00a383a4: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a383a8: 0a 78 44 94        bl         #0x1b563d0
  00a383ac: 70 27 40 91        add        x16, x27, #9, lsl #12  ; x16=ip0, x27=pp
  00a383b0: 10 62 43 f9        ldr        x16, [x16, #0x6c0]  ; x16=ip0, [x16=ip0]
  00a383b4: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a383b8: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a383bc: 7b 9d 03 94        bl         #0xb1f9a8  -> Inst|find
  00a383c0: 01 70 43 b8        ldur       w1, [x0, #0x37]
  00a383c4: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a383c8: 31 64 36 94        bl         #0x17d148c  -> RxObjectMixin.value
  00a383cc: e0 01 20 37        tbnz       w0, #4, #0xa38408
  00a383d0: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a383d4: a0 03 5f f8        ldur       x0, [x29, #-0x10]
  00a383d8: 02 30 41 b8        ldur       w2, [x0, #0x13]
  00a383dc: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a383e0: 20 30 48 b8        ldur       w0, [x1, #0x83]
  00a383e4: 00 80 1c 8b        add        x0, x0, x28, lsl #32  ; x28=heap
  00a383e8: 01 f0 42 f8        ldur       x1, [x0, #0x2f]
  00a383ec: f0 03 01 aa        mov        x16, x1  ; x16=ip0
  00a383f0: e1 03 02 aa        mov        x1, x2
  00a383f4: e2 03 10 aa        mov        x2, x16  ; x16=ip0
  00a383f8: 0e 00 00 94        bl         #0xa38430  -> WidgetPositionUtil.getLocInWorkSpaceWhenEdit
  00a383fc: ef 03 1d aa        mov        x15, x29  ; x15=dsp
  00a38400: fd 79 c1 a8        ldp        x29, x30, [x15], #0x10  ; [x15=dsp]
  00a38404: c0 03 5f d6        ret        
  00a38408: a0 03 5f f8        ldur       x0, [x29, #-0x10]
  00a3840c: 01 30 41 b8        ldur       w1, [x0, #0x13]
  00a38410: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a38414: e0 03 01 aa        mov        x0, x1
  00a38418: ef 03 1d aa        mov        x15, x29  ; x15=dsp
  00a3841c: fd 79 c1 a8        ldp        x29, x30, [x15], #0x10  ; [x15=dsp]
  00a38420: c0 03 5f d6        ret        
  00a38424: 69 27 40 91        add        x9, x27, #9, lsl #12  ; x27=pp
  00a38428: 29 0d 47 f9        ldr        x9, [x9, #0xe18]
  00a3842c: 1d 82 44 94        bl         #0x1b58ca0
