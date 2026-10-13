# FolderIconGetxController.calOriginPreviewIconLoc va=0xa7c234 size=0x2a4
  00a7c234: fd 79 bf a9        stp        x29, x30, [x15, #-0x10]!  ; [x15=dsp]
  00a7c238: fd 03 0f aa        mov        x29, x15  ; x15=dsp
  00a7c23c: ef e1 00 d1        sub        x15, x15, #0x38  ; x15=dsp
  00a7c240: a1 83 1f f8        stur       x1, [x29, #-8]
  00a7c244: a2 03 1f f8        stur       x2, [x29, #-0x10]
  00a7c248: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a7c24c: 00 88 53 f9        ldr        x0, [x0, #0x2710]
  00a7c250: 1f 00 16 6b        cmp        w0, w22
  00a7c254: e1 01 00 54        b.ne       #0xa7c290
  00a7c258: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a7c25c: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a7c260: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a7c264: 1f 00 10 6b        cmp        w0, w16
  00a7c268: 61 00 00 54        b.ne       #0xa7c274
  00a7c26c: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a7c270: 58 68 43 94        bl         #0x1b563d0
  00a7c274: 70 23 40 91        add        x16, x27, #8, lsl #12  ; x16=ip0, x27=pp
  00a7c278: 10 e2 47 f9        ldr        x16, [x16, #0xfc0]  ; x16=ip0, [x16=ip0]
  00a7c27c: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a7c280: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a7c284: c9 8d 02 94        bl         #0xb1f9a8  -> Inst|find
  00a7c288: e1 03 00 aa        mov        x1, x0
  00a7c28c: 02 00 00 14        b          #0xa7c294  -> FolderIconGetxController.calOriginPreviewIconLoc
  00a7c290: e1 03 00 aa        mov        x1, x0
  00a7c294: f9 77 02 94        bl         #0xb1a278  -> GridController.currentConfig
  00a7c298: 00 b0 42 fc        ldur       d0, [x0, #0x2b]
  00a7c29c: a0 83 1e fc        stur       d0, [x29, #-0x18]
  00a7c2a0: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a7c2a4: 00 88 53 f9        ldr        x0, [x0, #0x2710]
  00a7c2a8: 1f 00 16 6b        cmp        w0, w22
  00a7c2ac: e1 01 00 54        b.ne       #0xa7c2e8
  00a7c2b0: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a7c2b4: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a7c2b8: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a7c2bc: 1f 00 10 6b        cmp        w0, w16
  00a7c2c0: 61 00 00 54        b.ne       #0xa7c2cc
  00a7c2c4: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a7c2c8: 42 68 43 94        bl         #0x1b563d0
  00a7c2cc: 70 23 40 91        add        x16, x27, #8, lsl #12  ; x16=ip0, x27=pp
  00a7c2d0: 10 e2 47 f9        ldr        x16, [x16, #0xfc0]  ; x16=ip0, [x16=ip0]
  00a7c2d4: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a7c2d8: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a7c2dc: b3 8d 02 94        bl         #0xb1f9a8  -> Inst|find
  00a7c2e0: e1 03 00 aa        mov        x1, x0
  00a7c2e4: 02 00 00 14        b          #0xa7c2ec  -> FolderIconGetxController.calOriginPreviewIconLoc
  00a7c2e8: e1 03 00 aa        mov        x1, x0
  00a7c2ec: e3 77 02 94        bl         #0xb1a278  -> GridController.currentConfig
  00a7c2f0: 00 30 43 fc        ldur       d0, [x0, #0x33]
  00a7c2f4: a0 03 1e fc        stur       d0, [x29, #-0x20]
  00a7c2f8: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a7c2fc: 00 88 53 f9        ldr        x0, [x0, #0x2710]
  00a7c300: 1f 00 16 6b        cmp        w0, w22
  00a7c304: e1 01 00 54        b.ne       #0xa7c340
  00a7c308: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a7c30c: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a7c310: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a7c314: 1f 00 10 6b        cmp        w0, w16
  00a7c318: 61 00 00 54        b.ne       #0xa7c324
  00a7c31c: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a7c320: 2c 68 43 94        bl         #0x1b563d0
  00a7c324: 70 23 40 91        add        x16, x27, #8, lsl #12  ; x16=ip0, x27=pp
  00a7c328: 10 e2 47 f9        ldr        x16, [x16, #0xfc0]  ; x16=ip0, [x16=ip0]
  00a7c32c: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a7c330: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a7c334: 9d 8d 02 94        bl         #0xb1f9a8  -> Inst|find
  00a7c338: e1 03 00 aa        mov        x1, x0
  00a7c33c: 02 00 00 14        b          #0xa7c344  -> FolderIconGetxController.calOriginPreviewIconLoc
  00a7c340: e1 03 00 aa        mov        x1, x0
  00a7c344: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a7c348: a0 83 5e fc        ldur       d0, [x29, #-0x18]
  00a7c34c: cb 77 02 94        bl         #0xb1a278  -> GridController.currentConfig
  00a7c350: 01 b0 43 b8        ldur       w1, [x0, #0x3b]
  00a7c354: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a7c358: 20 70 40 fc        ldur       d0, [x1, #7]
  00a7c35c: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a7c360: 20 30 48 b8        ldur       w0, [x1, #0x83]
  00a7c364: 00 80 1c 8b        add        x0, x0, x28, lsl #32  ; x28=heap
  00a7c368: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a7c36c: 1f 00 10 6b        cmp        w0, w16
  00a7c370: e0 0a 00 54        b.eq       #0xa7c4cc
  00a7c374: 02 70 43 f8        ldur       x2, [x0, #0x37]
  00a7c378: 41 00 62 9e        scvtf      d1, x2
  00a7c37c: a2 83 5e fc        ldur       d2, [x29, #-0x18]
  00a7c380: 23 08 62 1e        fmul       d3, d1, d2
  00a7c384: 01 28 63 1e        fadd       d1, d0, d3
  00a7c388: a1 83 1d fc        stur       d1, [x29, #-0x28]
  00a7c38c: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a7c390: 00 88 53 f9        ldr        x0, [x0, #0x2710]
  00a7c394: 1f 00 16 6b        cmp        w0, w22
  00a7c398: e1 01 00 54        b.ne       #0xa7c3d4
  00a7c39c: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a7c3a0: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a7c3a4: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a7c3a8: 1f 00 10 6b        cmp        w0, w16
  00a7c3ac: 61 00 00 54        b.ne       #0xa7c3b8
  00a7c3b0: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a7c3b4: 07 68 43 94        bl         #0x1b563d0
  00a7c3b8: 70 23 40 91        add        x16, x27, #8, lsl #12  ; x16=ip0, x27=pp
  00a7c3bc: 10 e2 47 f9        ldr        x16, [x16, #0xfc0]  ; x16=ip0, [x16=ip0]
  00a7c3c0: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a7c3c4: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a7c3c8: 78 8d 02 94        bl         #0xb1f9a8  -> Inst|find
  00a7c3cc: e1 03 00 aa        mov        x1, x0
  00a7c3d0: 02 00 00 14        b          #0xa7c3d8  -> FolderIconGetxController.calOriginPreviewIconLoc
  00a7c3d4: e1 03 00 aa        mov        x1, x0
  00a7c3d8: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a7c3dc: a0 83 5e fc        ldur       d0, [x29, #-0x18]
  00a7c3e0: a1 03 5e fc        ldur       d1, [x29, #-0x20]
  00a7c3e4: 22 30 4f b8        ldur       w2, [x1, #0xf3]
  00a7c3e8: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7c3ec: e1 03 02 aa        mov        x1, x2
  00a7c3f0: 27 54 35 94        bl         #0x17d148c  -> RxObjectMixin.value
  00a7c3f4: e1 03 00 aa        mov        x1, x0
  00a7c3f8: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a7c3fc: 02 30 48 b8        ldur       w2, [x0, #0x83]
  00a7c400: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7c404: 43 f0 43 f8        ldur       x3, [x2, #0x3f]
  00a7c408: 60 00 62 9e        scvtf      d0, x3
  00a7c40c: a1 03 5e fc        ldur       d1, [x29, #-0x20]
  00a7c410: 02 08 61 1e        fmul       d2, d0, d1
  00a7c414: 20 70 40 fc        ldur       d0, [x1, #7]
  00a7c418: 01 28 62 1e        fadd       d1, d0, d2
  00a7c41c: a1 03 1e fc        stur       d1, [x29, #-0x20]
  00a7c420: 41 f0 42 f8        ldur       x1, [x2, #0x2f]
  00a7c424: a0 83 5d fc        ldur       d0, [x29, #-0x28]
  00a7c428: 64 3b 40 f9        ldr        x4, [x27, #0x70]  ; [x27=pp]
  00a7c42c: f7 f2 fe 97        bl         #0xa39008  -> WidgetPositionUtil.transformPointX
  00a7c430: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a7c434: a0 03 1d fc        stur       d0, [x29, #-0x30]
  00a7c438: 20 30 48 b8        ldur       w0, [x1, #0x83]
  00a7c43c: 00 80 1c 8b        add        x0, x0, x28, lsl #32  ; x28=heap
  00a7c440: 02 f0 41 f8        ldur       x2, [x0, #0x1f]
  00a7c444: 41 00 62 9e        scvtf      d1, x2
  00a7c448: a2 83 5e fc        ldur       d2, [x29, #-0x18]
  00a7c44c: 43 08 61 1e        fmul       d3, d2, d1
  00a7c450: a3 83 1d fc        stur       d3, [x29, #-0x28]
  00a7c454: 46 a9 f9 97        bl         #0x8e696c  -> SwipeController.isRtl
  00a7c458: 60 01 20 37        tbnz       w0, #4, #0xa7c484
  00a7c45c: a0 03 5f f8        ldur       x0, [x29, #-0x10]
  00a7c460: a0 03 5d fc        ldur       d0, [x29, #-0x30]
  00a7c464: a1 83 5d fc        ldur       d1, [x29, #-0x28]
  00a7c468: 02 28 61 1e        fadd       d2, d0, d1
  00a7c46c: 00 70 41 fc        ldur       d0, [x0, #0x17]
  00a7c470: 41 38 60 1e        fsub       d1, d2, d0
  00a7c474: 00 70 40 fc        ldur       d0, [x0, #7]
  00a7c478: 22 38 60 1e        fsub       d2, d1, d0
  00a7c47c: 41 1c a2 4e        mov        v1.16b, v2.16b
  00a7c480: 06 00 00 14        b          #0xa7c498  -> FolderIconGetxController.calOriginPreviewIconLoc
  00a7c484: a0 03 5f f8        ldur       x0, [x29, #-0x10]
  00a7c488: a0 03 5d fc        ldur       d0, [x29, #-0x30]
  00a7c48c: 01 70 41 fc        ldur       d1, [x0, #0x17]
  00a7c490: 02 28 61 1e        fadd       d2, d0, d1
  00a7c494: 41 1c a2 4e        mov        v1.16b, v2.16b
  00a7c498: a0 03 5e fc        ldur       d0, [x29, #-0x20]
  00a7c49c: a1 83 1d fc        stur       d1, [x29, #-0x28]
  00a7c4a0: 02 f0 41 fc        ldur       d2, [x0, #0x1f]
  00a7c4a4: 03 28 62 1e        fadd       d3, d0, d2
  00a7c4a8: a3 83 1e fc        stur       d3, [x29, #-0x18]
  00a7c4ac: d8 e6 38 94        bl         #0x18b600c
  00a7c4b0: a0 83 5d fc        ldur       d0, [x29, #-0x28]
  00a7c4b4: 00 70 00 fc        stur       d0, [x0, #7]
  00a7c4b8: a0 83 5e fc        ldur       d0, [x29, #-0x18]
  00a7c4bc: 00 f0 00 fc        stur       d0, [x0, #0xf]
  00a7c4c0: ef 03 1d aa        mov        x15, x29  ; x15=dsp
  00a7c4c4: fd 79 c1 a8        ldp        x29, x30, [x15], #0x10  ; [x15=dsp]
  00a7c4c8: c0 03 5f d6        ret        
  00a7c4cc: 69 27 40 91        add        x9, x27, #9, lsl #12  ; x27=pp
  00a7c4d0: 29 0d 47 f9        ldr        x9, [x9, #0xe18]
  00a7c4d4: 07 72 43 94        bl         #0x1b58cf0
