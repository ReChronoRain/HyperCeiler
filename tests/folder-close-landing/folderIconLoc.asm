# FolderIconGetxController.folderIconLoc va=0xa767f0 size=0x228
  00a767f0: fd 79 bf a9        stp        x29, x30, [x15, #-0x10]!  ; [x15=dsp]
  00a767f4: fd 03 0f aa        mov        x29, x15  ; x15=dsp
  00a767f8: ef 81 00 d1        sub        x15, x15, #0x20  ; x15=dsp
  00a767fc: a1 83 1f f8        stur       x1, [x29, #-8]
  00a76800: 20 30 48 b8        ldur       w0, [x1, #0x83]
  00a76804: 00 80 1c 8b        add        x0, x0, x28, lsl #32  ; x28=heap
  00a76808: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a7680c: 1f 00 10 6b        cmp        w0, w16
  00a76810: e0 0f 00 54        b.eq       #0xa76a0c
  00a76814: 02 70 44 f8        ldur       x2, [x0, #0x47]
  00a76818: 5f 94 01 b1        cmn        x2, #0x65
  00a7681c: 20 01 00 54        b.eq       #0xa76840
  00a76820: 5f 98 01 b1        cmn        x2, #0x66
  00a76824: e0 00 00 54        b.eq       #0xa76840
  00a76828: 5f 9c 01 b1        cmn        x2, #0x67
  00a7682c: a0 00 00 54        b.eq       #0xa76840
  00a76830: 5f a4 01 b1        cmn        x2, #0x69
  00a76834: 60 00 00 54        b.eq       #0xa76840
  00a76838: 5f a8 01 b1        cmn        x2, #0x6a
  00a7683c: c1 02 00 54        b.ne       #0xa76894
  00a76840: 71 29 80 d2        mov        x17, #0x14b  ; x17=ip1
  00a76844: 22 68 71 b8        ldr        w2, [x1, x17]
  00a76848: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7684c: 5f 00 16 6b        cmp        w2, w22
  00a76850: 81 00 00 54        b.ne       #0xa76860
  00a76854: e1 03 00 aa        mov        x1, x0
  00a76858: b8 b0 ff 97        bl         #0xa62b38  -> WidgetPositionUtil.getCellRectInHotSeat
  00a7685c: 02 00 00 14        b          #0xa76864  -> FolderIconGetxController.folderIconLoc
  00a76860: e0 03 02 aa        mov        x0, x2
  00a76864: 00 70 40 fc        ldur       d0, [x0, #7]
  00a76868: a0 83 1e fc        stur       d0, [x29, #-0x18]
  00a7686c: 01 f0 40 fc        ldur       d1, [x0, #0xf]
  00a76870: a1 03 1f fc        stur       d1, [x29, #-0x10]
  00a76874: e6 fd 38 94        bl         #0x18b600c
  00a76878: a0 83 5e fc        ldur       d0, [x29, #-0x18]
  00a7687c: 00 70 00 fc        stur       d0, [x0, #7]
  00a76880: a0 03 5f fc        ldur       d0, [x29, #-0x10]
  00a76884: 00 f0 00 fc        stur       d0, [x0, #0xf]
  00a76888: e1 03 00 aa        mov        x1, x0
  00a7688c: b8 01 00 94        bl         #0xa76f6c  -> WidgetPositionUtil.physicalToFolderClingLocal
  00a76890: 5c 00 00 14        b          #0xa76a00  -> FolderIconGetxController.folderIconLoc
  00a76894: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a76898: 00 0c 5a f9        ldr        x0, [x0, #0x3418]
  00a7689c: 1f 00 16 6b        cmp        w0, w22
  00a768a0: a1 01 00 54        b.ne       #0xa768d4
  00a768a4: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a768a8: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a768ac: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a768b0: 1f 00 10 6b        cmp        w0, w16
  00a768b4: 61 00 00 54        b.ne       #0xa768c0
  00a768b8: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a768bc: c5 7e 43 94        bl         #0x1b563d0
  00a768c0: 70 27 40 91        add        x16, x27, #9, lsl #12  ; x16=ip0, x27=pp
  00a768c4: 10 62 43 f9        ldr        x16, [x16, #0x6c0]  ; x16=ip0, [x16=ip0]
  00a768c8: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a768cc: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a768d0: 36 a4 02 94        bl         #0xb1f9a8  -> Inst|find
  00a768d4: 01 70 48 b8        ldur       w1, [x0, #0x87]
  00a768d8: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a768dc: ec 6a 35 94        bl         #0x17d148c  -> RxObjectMixin.value
  00a768e0: 80 02 20 37        tbnz       w0, #4, #0xa76930
  00a768e4: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a768e8: 01 30 48 b8        ldur       w1, [x0, #0x83]
  00a768ec: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a768f0: 22 70 43 f8        ldur       x2, [x1, #0x37]
  00a768f4: 23 f0 43 f8        ldur       x3, [x1, #0x3f]
  00a768f8: 24 f0 42 f8        ldur       x4, [x1, #0x2f]
  00a768fc: e1 03 02 aa        mov        x1, x2
  00a76900: e2 03 03 aa        mov        x2, x3
  00a76904: e3 03 04 aa        mov        x3, x4
  00a76908: 3f 09 ff 97        bl         #0xa38e04  -> WidgetPositionUtil.getCellPosition
  00a7690c: e1 03 00 aa        mov        x1, x0
  00a76910: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a76914: 02 30 48 b8        ldur       w2, [x0, #0x83]
  00a76918: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7691c: 40 f0 42 f8        ldur       x0, [x2, #0x2f]
  00a76920: e2 03 00 aa        mov        x2, x0
  00a76924: c3 06 ff 97        bl         #0xa38430  -> WidgetPositionUtil.getLocInWorkSpaceWhenEdit
  00a76928: e1 03 00 aa        mov        x1, x0
  00a7692c: 34 00 00 14        b          #0xa769fc  -> FolderIconGetxController.folderIconLoc
  00a76930: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a76934: 41 3f 40 f9        ldr        x1, [x26, #0x78]  ; [x26=thread]
  00a76938: 21 0c 5a f9        ldr        x1, [x1, #0x3418]
  00a7693c: 3f 00 16 6b        cmp        w1, w22
  00a76940: c1 01 00 54        b.ne       #0xa76978
  00a76944: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a76948: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a7694c: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a76950: 1f 00 10 6b        cmp        w0, w16
  00a76954: 61 00 00 54        b.ne       #0xa76960
  00a76958: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a7695c: 9d 7e 43 94        bl         #0x1b563d0
  00a76960: 70 27 40 91        add        x16, x27, #9, lsl #12  ; x16=ip0, x27=pp
  00a76964: 10 62 43 f9        ldr        x16, [x16, #0x6c0]  ; x16=ip0, [x16=ip0]
  00a76968: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a7696c: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a76970: 0e a4 02 94        bl         #0xb1f9a8  -> Inst|find
  00a76974: 02 00 00 14        b          #0xa7697c  -> FolderIconGetxController.folderIconLoc
  00a76978: e0 03 01 aa        mov        x0, x1
  00a7697c: 01 f0 44 b8        ldur       w1, [x0, #0x4f]
  00a76980: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a76984: c2 6a 35 94        bl         #0x17d148c  -> RxObjectMixin.value
  00a76988: 80 02 20 37        tbnz       w0, #4, #0xa769d8
  00a7698c: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a76990: 01 30 48 b8        ldur       w1, [x0, #0x83]
  00a76994: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a76998: 22 70 43 f8        ldur       x2, [x1, #0x37]
  00a7699c: 23 f0 43 f8        ldur       x3, [x1, #0x3f]
  00a769a0: 24 f0 42 f8        ldur       x4, [x1, #0x2f]
  00a769a4: e1 03 02 aa        mov        x1, x2
  00a769a8: e2 03 03 aa        mov        x2, x3
  00a769ac: e3 03 04 aa        mov        x3, x4
  00a769b0: 15 09 ff 97        bl         #0xa38e04  -> WidgetPositionUtil.getCellPosition
  00a769b4: e1 03 00 aa        mov        x1, x0
  00a769b8: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a769bc: 02 30 48 b8        ldur       w2, [x0, #0x83]
  00a769c0: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a769c4: 40 f0 42 f8        ldur       x0, [x2, #0x2f]
  00a769c8: e2 03 00 aa        mov        x2, x0
  00a769cc: 13 00 00 94        bl         #0xa76a18  -> WidgetPositionUtil.getLocInWorkSpaceWhenQuickEdit
  00a769d0: e1 03 00 aa        mov        x1, x0
  00a769d4: 0a 00 00 14        b          #0xa769fc  -> FolderIconGetxController.folderIconLoc
  00a769d8: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a769dc: 01 30 48 b8        ldur       w1, [x0, #0x83]
  00a769e0: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a769e4: 20 70 43 f8        ldur       x0, [x1, #0x37]
  00a769e8: 22 f0 43 f8        ldur       x2, [x1, #0x3f]
  00a769ec: 23 f0 42 f8        ldur       x3, [x1, #0x2f]
  00a769f0: e1 03 00 aa        mov        x1, x0
  00a769f4: 04 09 ff 97        bl         #0xa38e04  -> WidgetPositionUtil.getCellPosition
  00a769f8: e1 03 00 aa        mov        x1, x0
  00a769fc: e0 03 01 aa        mov        x0, x1
  00a76a00: ef 03 1d aa        mov        x15, x29  ; x15=dsp
  00a76a04: fd 79 c1 a8        ldp        x29, x30, [x15], #0x10  ; [x15=dsp]
  00a76a08: c0 03 5f d6        ret        
  00a76a0c: 69 27 40 91        add        x9, x27, #9, lsl #12  ; x27=pp
  00a76a10: 29 0d 47 f9        ldr        x9, [x9, #0xe18]
  00a76a14: a3 88 43 94        bl         #0x1b58ca0
