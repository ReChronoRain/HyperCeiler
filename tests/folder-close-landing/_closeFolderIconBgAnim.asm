# FolderAnimController._closeFolderIconBgAnim va=0xa7db2c size=0x178
  00a7db2c: fd 79 bf a9        stp        x29, x30, [x15, #-0x10]!  ; [x15=dsp]
  00a7db30: fd 03 0f aa        mov        x29, x15  ; x15=dsp
  00a7db34: ef c1 00 d1        sub        x15, x15, #0x30  ; x15=dsp
  00a7db38: e0 03 01 aa        mov        x0, x1
  00a7db3c: a1 83 1f f8        stur       x1, [x29, #-8]
  00a7db40: 31 01 00 94        bl         #0xa7e004  -> FolderAnimController._isPocoLauncherOrThemeIcon
  00a7db44: 60 00 20 37        tbnz       w0, #4, #0xa7db50
  00a7db48: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a7db4c: 0f 00 00 14        b          #0xa7db88  -> FolderAnimController._closeFolderIconBgAnim
  00a7db50: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a7db54: 20 f0 46 b8        ldur       w0, [x1, #0x6f]
  00a7db58: 00 80 1c 8b        add        x0, x0, x28, lsl #32  ; x28=heap
  00a7db5c: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a7db60: 1f 00 10 6b        cmp        w0, w16
  00a7db64: 40 09 00 54        b.eq       #0xa7dc8c
  00a7db68: 02 30 43 b8        ldur       w2, [x0, #0x33]
  00a7db6c: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7db70: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a7db74: 5f 00 10 6b        cmp        w2, w16
  00a7db78: 00 09 00 54        b.eq       #0xa7dc98
  00a7db7c: 40 f0 40 f8        ldur       x0, [x2, #0xf]
  00a7db80: 1f cc 00 f1        cmp        x0, #0x33
  00a7db84: 81 00 00 54        b.ne       #0xa7db94
  00a7db88: e0 03 01 aa        mov        x0, x1
  00a7db8c: c2 82 00 91        add        x2, x22, #0x20
  00a7db90: 14 00 00 14        b          #0xa7dbe0  -> FolderAnimController._closeFolderIconBgAnim
  00a7db94: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a7db98: 00 6c 53 f9        ldr        x0, [x0, #0x26d8]
  00a7db9c: 1f 00 16 6b        cmp        w0, w22
  00a7dba0: 81 01 00 54        b.ne       #0xa7dbd0
  00a7dba4: 40 3f 40 f9        ldr        x0, [x26, #0x78]  ; [x26=thread]
  00a7dba8: 00 68 4e f9        ldr        x0, [x0, #0x1cd0]
  00a7dbac: 70 23 40 f9        ldr        x16, [x27, #0x40]  ; x16=ip0, [x27=pp]
  00a7dbb0: 1f 00 10 6b        cmp        w0, w16
  00a7dbb4: 61 00 00 54        b.ne       #0xa7dbc0
  00a7dbb8: 62 03 7c f9        ldr        x2, [x27, #0x7800]  ; [x27=pp]
  00a7dbbc: 05 62 43 94        bl         #0x1b563d0
  00a7dbc0: 70 07 7c f9        ldr        x16, [x27, #0x7808]  ; x16=ip0, [x27=pp]
  00a7dbc4: f0 01 00 f9        str        x16, [x15]  ; x16=ip0, [x15=dsp]
  00a7dbc8: 64 2b 40 f9        ldr        x4, [x27, #0x50]  ; [x27=pp]
  00a7dbcc: 77 87 02 94        bl         #0xb1f9a8  -> Inst|find
  00a7dbd0: 01 b0 4e b8        ldur       w1, [x0, #0xeb]
  00a7dbd4: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a7dbd8: e2 03 01 aa        mov        x2, x1
  00a7dbdc: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a7dbe0: a2 03 1f f8        stur       x2, [x29, #-0x10]
  00a7dbe4: 00 70 43 fc        ldur       d0, [x0, #0x37]
  00a7dbe8: 01 70 40 fc        ldur       d1, [x0, #7]
  00a7dbec: e1 03 16 aa        mov        x1, x22
  00a7dbf0: 58 95 f8 97        bl         #0x8a3150
  00a7dbf4: e2 03 00 aa        mov        x2, x0
  00a7dbf8: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a7dbfc: a2 83 1e f8        stur       x2, [x29, #-0x18]
  00a7dc00: 00 70 43 fc        ldur       d0, [x0, #0x37]
  00a7dc04: 01 70 40 fc        ldur       d1, [x0, #7]
  00a7dc08: e1 03 16 aa        mov        x1, x22
  00a7dc0c: 51 95 f8 97        bl         #0x8a3150
  00a7dc10: e1 03 00 aa        mov        x1, x0
  00a7dc14: a0 03 5f f8        ldur       x0, [x29, #-0x10]
  00a7dc18: a1 83 1d f8        stur       x1, [x29, #-0x28]
  00a7dc1c: 80 00 20 37        tbnz       w0, #4, #0xa7dc2c
  00a7dc20: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a7dc24: 02 f0 45 f8        ldur       x2, [x0, #0x5f]
  00a7dc28: 03 00 00 14        b          #0xa7dc34  -> FolderAnimController._closeFolderIconBgAnim
  00a7dc2c: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a7dc30: 02 00 80 d2        mov        x2, #0
  00a7dc34: 10 7d 80 d2        mov        x16, #0x3e8  ; x16=ip0
  00a7dc38: 43 7c 10 9b        mul        x3, x2, x16  ; x16=ip0
  00a7dc3c: a3 03 1e f8        stur       x3, [x29, #-0x20]
  00a7dc40: 73 aa 38 94        bl         #0x18a860c
  00a7dc44: e1 03 00 aa        mov        x1, x0
  00a7dc48: a0 03 5e f8        ldur       x0, [x29, #-0x20]
  00a7dc4c: 20 70 00 f8        stur       x0, [x1, #7]
  00a7dc50: e1 01 00 f9        str        x1, [x15]  ; [x15=dsp]
  00a7dc54: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a7dc58: a3 83 5e f8        ldur       x3, [x29, #-0x18]
  00a7dc5c: a5 83 5d f8        ldur       x5, [x29, #-0x28]
  00a7dc60: 62 f3 4e f9        ldr        x2, [x27, #0x1de0]  ; [x27=pp]
  00a7dc64: 00 10 6e 1e        fmov       d0, #1.00000000
  00a7dc68: 01 10 6e 1e        fmov       d1, #1.00000000
  00a7dc6c: 02 10 6e 1e        fmov       d2, #1.00000000
  00a7dc70: 64 ab 40 91        add        x4, x27, #0x2a, lsl #12  ; x27=pp
  00a7dc74: 84 50 47 f9        ldr        x4, [x4, #0xea0]
  00a7dc78: 0b 00 00 94        bl         #0xa7dca4  -> FolderAnimController._updateBgAnim
  00a7dc7c: e0 03 16 aa        mov        x0, x22
  00a7dc80: ef 03 1d aa        mov        x15, x29  ; x15=dsp
  00a7dc84: fd 79 c1 a8        ldp        x29, x30, [x15], #0x10  ; [x15=dsp]
  00a7dc88: c0 03 5f d6        ret        
  00a7dc8c: 69 ab 40 91        add        x9, x27, #0x2a, lsl #12  ; x27=pp
  00a7dc90: 29 25 46 f9        ldr        x9, [x9, #0xc48]
  00a7dc94: 03 6c 43 94        bl         #0x1b58ca0
  00a7dc98: 69 3f 40 91        add        x9, x27, #0xf, lsl #12  ; x27=pp
  00a7dc9c: 29 65 43 f9        ldr        x9, [x9, #0x6c8]
  00a7dca0: 00 6c 43 94        bl         #0x1b58ca0
