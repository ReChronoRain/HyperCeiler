# FolderIconGetxController.iconIconLoc va=0xa794bc size=0x194
  00a794bc: fd 79 bf a9        stp        x29, x30, [x15, #-0x10]!  ; [x15=dsp]
  00a794c0: fd 03 0f aa        mov        x29, x15  ; x15=dsp
  00a794c4: ef 01 01 d1        sub        x15, x15, #0x40  ; x15=dsp
  00a794c8: e0 03 01 aa        mov        x0, x1
  00a794cc: a1 83 1f f8        stur       x1, [x29, #-8]
  00a794d0: c8 f4 ff 97        bl         #0xa767f0  -> FolderIconGetxController.folderIconLoc
  00a794d4: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a794d8: a0 03 1f f8        stur       x0, [x29, #-0x10]
  00a794dc: a8 00 00 94        bl         #0xa7977c  -> FolderIconGetxController.folderIconSize
  00a794e0: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a794e4: a0 83 1e f8        stur       x0, [x29, #-0x18]
  00a794e8: 22 30 48 b8        ldur       w2, [x1, #0x83]
  00a794ec: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a794f0: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a794f4: 5f 00 10 6b        cmp        w2, w16
  00a794f8: 00 0a 00 54        b.eq       #0xa79638
  00a794fc: 43 70 44 f8        ldur       x3, [x2, #0x47]
  00a79500: 7f 94 01 b1        cmn        x3, #0x65
  00a79504: 20 01 00 54        b.eq       #0xa79528
  00a79508: 7f 98 01 b1        cmn        x3, #0x66
  00a7950c: e0 00 00 54        b.eq       #0xa79528
  00a79510: 7f 9c 01 b1        cmn        x3, #0x67
  00a79514: a0 00 00 54        b.eq       #0xa79528
  00a79518: 7f a4 01 b1        cmn        x3, #0x69
  00a7951c: 60 00 00 54        b.eq       #0xa79528
  00a79520: 7f a8 01 b1        cmn        x3, #0x6a
  00a79524: 61 00 00 54        b.ne       #0xa79530
  00a79528: 01 10 6e 1e        fmov       d1, #1.00000000
  00a7952c: 18 00 00 14        b          #0xa7958c  -> FolderIconGetxController.iconIconLoc
  00a79530: 42 3f 40 f9        ldr        x2, [x26, #0x78]  ; [x26=thread]
  00a79534: 42 0c 5a f9        ldr        x2, [x2, #0x3418]
  00a79538: 5f 00 16 6b        cmp        w2, w22
  00a7953c: e1 01 00 54        b.ne       #0xa79578
  00a79540: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a79544: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a79548: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a7954c: 1f 00 10 6b        cmp        w0, w16
  00a79550: 61 00 00 54        b.ne       #0xa7955c
  00a79554: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a79558: 9e 73 43 94        bl         #0x1b563d0
  00a7955c: 70 27 40 91        add        x16, x27, #9, lsl #12  ; x16=ip0, x27=pp
  00a79560: 10 62 43 f9        ldr        x16, [x16, #0x6c0]  ; x16=ip0, [x16=ip0]
  00a79564: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a79568: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a7956c: 0f 99 02 94        bl         #0xb1f9a8  -> Inst|find
  00a79570: e1 03 00 aa        mov        x1, x0
  00a79574: 02 00 00 14        b          #0xa7957c  -> FolderIconGetxController.iconIconLoc
  00a79578: e1 03 02 aa        mov        x1, x2
  00a7957c: 35 00 00 94        bl         #0xa79650  -> EditModeGetxController.editScale
  00a79580: 01 1c a0 4e        mov        v1.16b, v0.16b
  00a79584: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a79588: a0 83 5e f8        ldur       x0, [x29, #-0x18]
  00a7958c: a2 03 5f f8        ldur       x2, [x29, #-0x10]
  00a79590: 00 10 60 1e        fmov       d0, #2.00000000
  00a79594: a1 83 1c fc        stur       d1, [x29, #-0x38]
  00a79598: 42 70 40 fc        ldur       d2, [x2, #7]
  00a7959c: 03 70 40 fc        ldur       d3, [x0, #7]
  00a795a0: 71 33 80 d2        mov        x17, #0x19b  ; x17=ip1
  00a795a4: 23 68 71 b8        ldr        w3, [x1, x17]
  00a795a8: 63 80 1c 8b        add        x3, x3, x28, lsl #32  ; x28=heap
  00a795ac: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a795b0: 7f 00 10 6b        cmp        w3, w16
  00a795b4: 80 04 00 54        b.eq       #0xa79644
  00a795b8: 64 f0 40 fc        ldur       d4, [x3, #0xf]
  00a795bc: 85 08 61 1e        fmul       d5, d4, d1
  00a795c0: 64 38 65 1e        fsub       d4, d3, d5
  00a795c4: 83 18 60 1e        fdiv       d3, d4, d0
  00a795c8: 40 28 63 1e        fadd       d0, d2, d3
  00a795cc: a0 03 1d fc        stur       d0, [x29, #-0x30]
  00a795d0: 42 f0 40 fc        ldur       d2, [x2, #0xf]
  00a795d4: a2 83 1d fc        stur       d2, [x29, #-0x28]
  00a795d8: 03 f0 40 fc        ldur       d3, [x0, #0xf]
  00a795dc: 64 70 41 fc        ldur       d4, [x3, #0x17]
  00a795e0: 85 08 61 1e        fmul       d5, d4, d1
  00a795e4: 64 38 65 1e        fsub       d4, d3, d5
  00a795e8: a4 03 1e fc        stur       d4, [x29, #-0x20]
  00a795ec: 95 fd fe 97        bl         #0xa38c40  -> FolderIconGetxController.getTitleHeight
  00a795f0: 01 1c a0 4e        mov        v1.16b, v0.16b
  00a795f4: a0 83 5c fc        ldur       d0, [x29, #-0x38]
  00a795f8: 22 08 60 1e        fmul       d2, d1, d0
  00a795fc: a0 03 5e fc        ldur       d0, [x29, #-0x20]
  00a79600: 01 38 62 1e        fsub       d1, d0, d2
  00a79604: 00 10 6c 1e        fmov       d0, #0.50000000
  00a79608: 22 08 60 1e        fmul       d2, d1, d0
  00a7960c: a0 83 5d fc        ldur       d0, [x29, #-0x28]
  00a79610: 01 28 62 1e        fadd       d1, d0, d2
  00a79614: a1 03 1e fc        stur       d1, [x29, #-0x20]
  00a79618: 7d f2 38 94        bl         #0x18b600c
  00a7961c: a0 03 5d fc        ldur       d0, [x29, #-0x30]
  00a79620: 00 70 00 fc        stur       d0, [x0, #7]
  00a79624: a0 03 5e fc        ldur       d0, [x29, #-0x20]
  00a79628: 00 f0 00 fc        stur       d0, [x0, #0xf]
  00a7962c: ef 03 1d aa        mov        x15, x29  ; x15=dsp
  00a79630: fd 79 c1 a8        ldp        x29, x30, [x15], #0x10  ; [x15=dsp]
  00a79634: c0 03 5f d6        ret        
  00a79638: 69 27 40 91        add        x9, x27, #9, lsl #12  ; x27=pp
  00a7963c: 29 0d 47 f9        ldr        x9, [x9, #0xe18]
  00a79640: 98 7d 43 94        bl         #0x1b58ca0
  00a79644: 69 27 40 91        add        x9, x27, #9, lsl #12  ; x27=pp
  00a79648: 29 09 47 f9        ldr        x9, [x9, #0xe10]
  00a7964c: a9 7d 43 94        bl         #0x1b58cf0
