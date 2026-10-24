# FolderIconGetxController.getCellRect va=0x13e84c0 size=0x3a4
  013e84c0: fd 79 bf a9        stp        x29, x30, [x15, #-0x10]!  ; [x15=dsp]
  013e84c4: fd 03 0f aa        mov        x29, x15  ; x15=dsp
  013e84c8: ef c1 00 d1        sub        x15, x15, #0x30  ; x15=dsp
  013e84cc: a1 83 1f f8        stur       x1, [x29, #-8]
  013e84d0: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  013e84d4: 00 88 53 f9        ldr        x0, [x0, #0x2710]
  013e84d8: 1f 00 16 6b        cmp        w0, w22
  013e84dc: e1 01 00 54        b.ne       #0x13e8518
  013e84e0: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  013e84e4: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  013e84e8: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  013e84ec: 1f 00 10 6b        cmp        w0, w16
  013e84f0: 61 00 00 54        b.ne       #0x13e84fc
  013e84f4: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  013e84f8: b6 b7 1d 94        bl         #0x1b563d0
  013e84fc: 70 23 40 91        add        x16, x27, #8, lsl #12  ; x16=ip0, x27=pp
  013e8500: 10 e2 47 f9        ldr        x16, [x16, #0xfc0]  ; x16=ip0, [x16=ip0]
  013e8504: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  013e8508: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  013e850c: 27 dd dc 97        bl         #0xb1f9a8  -> Inst|find
  013e8510: e1 03 00 aa        mov        x1, x0
  013e8514: 02 00 00 14        b          #0x13e851c  -> FolderIconGetxController.getCellRect
  013e8518: e1 03 00 aa        mov        x1, x0
  013e851c: 57 c7 dc 97        bl         #0xb1a278  -> GridController.currentConfig
  013e8520: 00 b0 42 fc        ldur       d0, [x0, #0x2b]
  013e8524: a0 03 1f fc        stur       d0, [x29, #-0x10]
  013e8528: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  013e852c: 00 88 53 f9        ldr        x0, [x0, #0x2710]
  013e8530: 1f 00 16 6b        cmp        w0, w22
  013e8534: e1 01 00 54        b.ne       #0x13e8570
  013e8538: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  013e853c: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  013e8540: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  013e8544: 1f 00 10 6b        cmp        w0, w16
  013e8548: 61 00 00 54        b.ne       #0x13e8554
  013e854c: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  013e8550: a0 b7 1d 94        bl         #0x1b563d0
  013e8554: 70 23 40 91        add        x16, x27, #8, lsl #12  ; x16=ip0, x27=pp
  013e8558: 10 e2 47 f9        ldr        x16, [x16, #0xfc0]  ; x16=ip0, [x16=ip0]
  013e855c: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  013e8560: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  013e8564: 11 dd dc 97        bl         #0xb1f9a8  -> Inst|find
  013e8568: e1 03 00 aa        mov        x1, x0
  013e856c: 02 00 00 14        b          #0x13e8574  -> FolderIconGetxController.getCellRect
  013e8570: e1 03 00 aa        mov        x1, x0
  013e8574: a0 83 5f f8        ldur       x0, [x29, #-8]
  013e8578: 40 c7 dc 97        bl         #0xb1a278  -> GridController.currentConfig
  013e857c: 00 30 43 fc        ldur       d0, [x0, #0x33]
  013e8580: a0 83 5f f8        ldur       x0, [x29, #-8]
  013e8584: a0 83 1e fc        stur       d0, [x29, #-0x18]
  013e8588: 01 30 48 b8        ldur       w1, [x0, #0x83]
  013e858c: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  013e8590: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  013e8594: 3f 00 10 6b        cmp        w1, w16
  013e8598: 20 14 00 54        b.eq       #0x13e881c
  013e859c: 22 70 44 f8        ldur       x2, [x1, #0x47]
  013e85a0: 5f 90 01 b1        cmn        x2, #0x64
  013e85a4: 61 0c 00 54        b.ne       #0x13e8730
  013e85a8: e1 03 00 aa        mov        x1, x0
  013e85ac: c2 c2 00 91        add        x2, x22, #0x30
  013e85b0: ad 00 00 94        bl         #0x13e8864  -> FolderIconGetxController.setIsShinkTitle
  013e85b4: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  013e85b8: 00 88 53 f9        ldr        x0, [x0, #0x2710]
  013e85bc: 1f 00 16 6b        cmp        w0, w22
  013e85c0: e1 01 00 54        b.ne       #0x13e85fc
  013e85c4: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  013e85c8: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  013e85cc: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  013e85d0: 1f 00 10 6b        cmp        w0, w16
  013e85d4: 61 00 00 54        b.ne       #0x13e85e0
  013e85d8: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  013e85dc: 7d b7 1d 94        bl         #0x1b563d0
  013e85e0: 70 23 40 91        add        x16, x27, #8, lsl #12  ; x16=ip0, x27=pp
  013e85e4: 10 e2 47 f9        ldr        x16, [x16, #0xfc0]  ; x16=ip0, [x16=ip0]
  013e85e8: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  013e85ec: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  013e85f0: ee dc dc 97        bl         #0xb1f9a8  -> Inst|find
  013e85f4: e1 03 00 aa        mov        x1, x0
  013e85f8: 02 00 00 14        b          #0x13e8600  -> FolderIconGetxController.getCellRect
  013e85fc: e1 03 00 aa        mov        x1, x0
  013e8600: a0 83 5f f8        ldur       x0, [x29, #-8]
  013e8604: a0 03 5f fc        ldur       d0, [x29, #-0x10]
  013e8608: 1c c7 dc 97        bl         #0xb1a278  -> GridController.currentConfig
  013e860c: 01 b0 43 b8        ldur       w1, [x0, #0x3b]
  013e8610: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  013e8614: 20 70 40 fc        ldur       d0, [x1, #7]
  013e8618: a1 83 5f f8        ldur       x1, [x29, #-8]
  013e861c: 20 30 48 b8        ldur       w0, [x1, #0x83]
  013e8620: 00 80 1c 8b        add        x0, x0, x28, lsl #32  ; x28=heap
  013e8624: 02 70 43 f8        ldur       x2, [x0, #0x37]
  013e8628: 41 00 62 9e        scvtf      d1, x2
  013e862c: a2 03 5f fc        ldur       d2, [x29, #-0x10]
  013e8630: 23 08 62 1e        fmul       d3, d1, d2
  013e8634: 01 28 63 1e        fadd       d1, d0, d3
  013e8638: a1 03 1e fc        stur       d1, [x29, #-0x20]
  013e863c: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  013e8640: 00 88 53 f9        ldr        x0, [x0, #0x2710]
  013e8644: 1f 00 16 6b        cmp        w0, w22
  013e8648: e1 01 00 54        b.ne       #0x13e8684
  013e864c: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  013e8650: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  013e8654: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  013e8658: 1f 00 10 6b        cmp        w0, w16
  013e865c: 61 00 00 54        b.ne       #0x13e8668
  013e8660: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  013e8664: 5b b7 1d 94        bl         #0x1b563d0
  013e8668: 70 23 40 91        add        x16, x27, #8, lsl #12  ; x16=ip0, x27=pp
  013e866c: 10 e2 47 f9        ldr        x16, [x16, #0xfc0]  ; x16=ip0, [x16=ip0]
  013e8670: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  013e8674: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  013e8678: cc dc dc 97        bl         #0xb1f9a8  -> Inst|find
  013e867c: e1 03 00 aa        mov        x1, x0
  013e8680: 02 00 00 14        b          #0x13e8688  -> FolderIconGetxController.getCellRect
  013e8684: e1 03 00 aa        mov        x1, x0
  013e8688: a0 83 5f f8        ldur       x0, [x29, #-8]
  013e868c: a0 03 5e fc        ldur       d0, [x29, #-0x20]
  013e8690: a1 83 5e fc        ldur       d1, [x29, #-0x18]
  013e8694: 22 30 4f b8        ldur       w2, [x1, #0xf3]
  013e8698: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  013e869c: e1 03 02 aa        mov        x1, x2
  013e86a0: 7b a3 0f 94        bl         #0x17d148c  -> RxObjectMixin.value
  013e86a4: e1 03 00 aa        mov        x1, x0
  013e86a8: a0 83 5f f8        ldur       x0, [x29, #-8]
  013e86ac: 02 30 48 b8        ldur       w2, [x0, #0x83]
  013e86b0: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  013e86b4: 40 f0 43 f8        ldur       x0, [x2, #0x3f]
  013e86b8: 00 00 62 9e        scvtf      d0, x0
  013e86bc: a1 83 5e fc        ldur       d1, [x29, #-0x18]
  013e86c0: 02 08 61 1e        fmul       d2, d0, d1
  013e86c4: 20 70 40 fc        ldur       d0, [x1, #7]
  013e86c8: 03 28 62 1e        fadd       d3, d0, d2
  013e86cc: a0 03 5e fc        ldur       d0, [x29, #-0x20]
  013e86d0: 40 07 46 a9        ldp        x0, x1, [x26, #0x60]  ; [x26=thread]
  013e86d4: 00 40 00 91        add        x0, x0, #0x10
  013e86d8: 3f 00 00 eb        cmp        x1, x0
  013e86dc: 69 0a 00 54        b.ls       #0x13e8828
  013e86e0: 40 33 00 f9        str        x0, [x26, #0x60]  ; [x26=thread]
  013e86e4: 00 3c 00 d1        sub        x0, x0, #0xf
  013e86e8: 81 2b 9c d2        mov        x1, #0xe15c
  013e86ec: 61 00 a0 f2        movk       x1, #3, lsl #16
  013e86f0: 01 f0 1f f8        stur       x1, [x0, #-1]
  013e86f4: 00 70 00 fc        stur       d0, [x0, #7]
  013e86f8: 41 0f 46 a9        ldp        x1, x3, [x26, #0x60]  ; [x26=thread]
  013e86fc: 21 40 00 91        add        x1, x1, #0x10
  013e8700: 7f 00 01 eb        cmp        x3, x1
  013e8704: 29 0a 00 54        b.ls       #0x13e8848
  013e8708: 41 33 00 f9        str        x1, [x26, #0x60]  ; [x26=thread]
  013e870c: 21 3c 00 d1        sub        x1, x1, #0xf
  013e8710: 83 2b 9c d2        mov        x3, #0xe15c
  013e8714: 63 00 a0 f2        movk       x3, #3, lsl #16
  013e8718: 23 f0 1f f8        stur       x3, [x1, #-1]
  013e871c: 23 70 00 fc        stur       d3, [x1, #7]
  013e8720: f0 03 02 aa        mov        x16, x2  ; x16=ip0
  013e8724: e2 03 00 aa        mov        x2, x0
  013e8728: e0 03 10 aa        mov        x0, x16  ; x16=ip0
  013e872c: 11 00 00 14        b          #0x13e8770  -> FolderIconGetxController.getCellRect
  013e8730: 01 1c a0 4e        mov        v1.16b, v0.16b
  013e8734: 5f 94 01 b1        cmn        x2, #0x65
  013e8738: 61 01 00 54        b.ne       #0x13e8764
  013e873c: e1 03 00 aa        mov        x1, x0
  013e8740: c2 82 00 91        add        x2, x22, #0x20
  013e8744: 48 00 00 94        bl         #0x13e8864  -> FolderIconGetxController.setIsShinkTitle
  013e8748: a0 83 5f f8        ldur       x0, [x29, #-8]
  013e874c: 01 30 48 b8        ldur       w1, [x0, #0x83]
  013e8750: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  013e8754: f9 e8 d9 97        bl         #0xa62b38  -> WidgetPositionUtil.getCellRectInHotSeat
  013e8758: ef 03 1d aa        mov        x15, x29  ; x15=dsp
  013e875c: fd 79 c1 a8        ldp        x29, x30, [x15], #0x10  ; [x15=dsp]
  013e8760: c0 03 5f d6        ret        
  013e8764: e0 03 01 aa        mov        x0, x1
  013e8768: 62 23 40 f9        ldr        x2, [x27, #0x40]  ; [x27=pp]
  013e876c: 61 23 40 f9        ldr        x1, [x27, #0x40]  ; [x27=pp]
  013e8770: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  013e8774: 5f 00 10 6b        cmp        w2, w16
  013e8778: e0 03 00 54        b.eq       #0x13e87f4
  013e877c: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  013e8780: 3f 00 10 6b        cmp        w1, w16
  013e8784: 20 04 00 54        b.eq       #0x13e8808
  013e8788: a0 03 5f fc        ldur       d0, [x29, #-0x10]
  013e878c: 03 f0 41 f8        ldur       x3, [x0, #0x1f]
  013e8790: 62 00 62 9e        scvtf      d2, x3
  013e8794: 03 08 62 1e        fmul       d3, d0, d2
  013e8798: 03 70 42 f8        ldur       x3, [x0, #0x27]
  013e879c: 60 00 62 9e        scvtf      d0, x3
  013e87a0: 22 08 60 1e        fmul       d2, d1, d0
  013e87a4: 40 70 40 fc        ldur       d0, [x2, #7]
  013e87a8: a0 83 1d fc        stur       d0, [x29, #-0x28]
  013e87ac: 01 28 63 1e        fadd       d1, d0, d3
  013e87b0: a1 03 1e fc        stur       d1, [x29, #-0x20]
  013e87b4: 23 70 40 fc        ldur       d3, [x1, #7]
  013e87b8: a3 83 1e fc        stur       d3, [x29, #-0x18]
  013e87bc: 64 28 62 1e        fadd       d4, d3, d2
  013e87c0: a4 03 1f fc        stur       d4, [x29, #-0x10]
  013e87c4: f3 d1 15 94        bl         #0x195cf90
  013e87c8: a0 83 5d fc        ldur       d0, [x29, #-0x28]
  013e87cc: 00 70 00 fc        stur       d0, [x0, #7]
  013e87d0: a0 83 5e fc        ldur       d0, [x29, #-0x18]
  013e87d4: 00 f0 00 fc        stur       d0, [x0, #0xf]
  013e87d8: a0 03 5e fc        ldur       d0, [x29, #-0x20]
  013e87dc: 00 70 01 fc        stur       d0, [x0, #0x17]
  013e87e0: a0 03 5f fc        ldur       d0, [x29, #-0x10]
  013e87e4: 00 f0 01 fc        stur       d0, [x0, #0x1f]
  013e87e8: ef 03 1d aa        mov        x15, x29  ; x15=dsp
  013e87ec: fd 79 c1 a8        ldp        x29, x30, [x15], #0x10  ; [x15=dsp]
  013e87f0: c0 03 5f d6        ret        
  013e87f4: 70 a3 43 91        add        x16, x27, #0xe8, lsl #12  ; x16=ip0, x27=pp
  013e87f8: 10 ae 45 f9        ldr        x16, [x16, #0xb58]  ; x16=ip0, [x16=ip0]
  013e87fc: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  013e8800: 0c 6d d2 97        bl         #0x883c30  -> LateError._throwLocalNotInitialized
  013e8804: 00 00 20 d4        brk        #0
  013e8808: 70 a3 43 91        add        x16, x27, #0xe8, lsl #12  ; x16=ip0, x27=pp
  013e880c: 10 b2 45 f9        ldr        x16, [x16, #0xb60]  ; x16=ip0, [x16=ip0]
  013e8810: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  013e8814: 07 6d d2 97        bl         #0x883c30  -> LateError._throwLocalNotInitialized
  013e8818: 00 00 20 d4        brk        #0
  013e881c: 69 27 40 91        add        x9, x27, #9, lsl #12  ; x27=pp
  013e8820: 29 0d 47 f9        ldr        x9, [x9, #0xe18]
  013e8824: 33 c1 1d 94        bl         #0x1b58cf0
  013e8828: e1 0d bf ad        stp        q1, q3, [x15, #-0x20]!  ; [x15=dsp]
  013e882c: e0 0d 9f 3c        str        q0, [x15, #-0x10]!  ; [x15=dsp]
  013e8830: e2 8d 1f f8        str        x2, [x15, #-8]!  ; [x15=dsp]
  013e8834: 6b be 1d 94        bl         #0x1b581e0
  013e8838: e2 85 40 f8        ldr        x2, [x15], #8  ; [x15=dsp]
  013e883c: e0 05 c1 3c        ldr        q0, [x15], #0x10  ; [x15=dsp]
  013e8840: e1 0d c1 ac        ldp        q1, q3, [x15], #0x20  ; [x15=dsp]
  013e8844: ac ff ff 17        b          #0x13e86f4  -> FolderIconGetxController.getCellRect
  013e8848: e1 0d bf ad        stp        q1, q3, [x15, #-0x20]!  ; [x15=dsp]
  013e884c: e0 09 bf a9        stp        x0, x2, [x15, #-0x10]!  ; [x15=dsp]
  013e8850: 64 be 1d 94        bl         #0x1b581e0
  013e8854: e1 03 00 aa        mov        x1, x0
  013e8858: e0 09 c1 a8        ldp        x0, x2, [x15], #0x10  ; [x15=dsp]
  013e885c: e1 0d c1 ac        ldp        q1, q3, [x15], #0x20  ; [x15=dsp]
  013e8860: af ff ff 17        b          #0x13e871c  -> FolderIconGetxController.getCellRect
