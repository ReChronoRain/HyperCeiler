# FolderAnimController._updateFolderIconImgViewLoc va=0xa7b5f0 size=0x4bc
  00a7b5f0: fd 79 bf a9        stp        x29, x30, [x15, #-0x10]!  ; [x15=dsp]
  00a7b5f4: fd 03 0f aa        mov        x29, x15  ; x15=dsp
  00a7b5f8: ef 41 01 d1        sub        x15, x15, #0x50  ; x15=dsp
  00a7b5fc: e0 03 01 aa        mov        x0, x1
  00a7b600: a1 83 1f f8        stur       x1, [x29, #-8]
  00a7b604: a2 03 1f f8        stur       x2, [x29, #-0x10]
  00a7b608: a3 83 1e f8        stur       x3, [x29, #-0x18]
  00a7b60c: 71 21 80 d2        mov        x17, #0x10b  ; x17=ip1
  00a7b610: 01 68 71 b8        ldr        w1, [x0, x17]
  00a7b614: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a7b618: 24 7c 41 93        sbfx       x4, x1, #1, #0x1f
  00a7b61c: 41 00 00 36        tbz        w1, #0, #0xa7b624
  00a7b620: 24 70 40 f8        ldur       x4, [x1, #7]
  00a7b624: 7f 00 04 eb        cmp        x3, x4
  00a7b628: 81 07 00 54        b.ne       #0xa7b718
  00a7b62c: e1 03 00 aa        mov        x1, x0
  00a7b630: a7 08 00 94        bl         #0xa7d8cc  -> FolderAnimController._isBigFolder
  00a7b634: c0 06 20 37        tbnz       w0, #4, #0xa7b70c
  00a7b638: a0 03 5f f8        ldur       x0, [x29, #-0x10]
  00a7b63c: 42 1c 22 6e        eor        v2.16b, v2.16b, v2.16b
  00a7b640: 00 f0 41 fc        ldur       d0, [x0, #0x1f]
  00a7b644: 01 f0 40 fc        ldur       d1, [x0, #0xf]
  00a7b648: 03 38 61 1e        fsub       d3, d0, d1
  00a7b64c: 60 20 62 1e        fcmp       d3, d2
  00a7b650: 4d 03 00 54        b.le       #0xa7b6b8
  00a7b654: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a7b658: f1 20 80 d2        mov        x17, #0x107  ; x17=ip1
  00a7b65c: 20 68 71 b8        ldr        w0, [x1, x17]
  00a7b660: 00 80 1c 8b        add        x0, x0, x28, lsl #32  ; x28=heap
  00a7b664: 00 70 40 fc        ldur       d0, [x0, #7]
  00a7b668: 01 18 63 1e        fdiv       d1, d0, d3
  00a7b66c: 40 0b 46 a9        ldp        x0, x2, [x26, #0x60]  ; [x26=thread]
  00a7b670: 00 40 00 91        add        x0, x0, #0x10
  00a7b674: 5f 00 00 eb        cmp        x2, x0
  00a7b678: 89 1e 00 54        b.ls       #0xa7ba48
  00a7b67c: 40 33 00 f9        str        x0, [x26, #0x60]  ; [x26=thread]
  00a7b680: 00 3c 00 d1        sub        x0, x0, #0xf
  00a7b684: 82 2b 9c d2        mov        x2, #0xe15c
  00a7b688: 62 00 a0 f2        movk       x2, #3, lsl #16
  00a7b68c: 02 f0 1f f8        stur       x2, [x0, #-1]
  00a7b690: 01 70 00 fc        stur       d1, [x0, #7]
  00a7b694: 20 f0 0f b8        stur       w0, [x1, #0xff]
  00a7b698: 30 f0 5f 38        ldurb      w16, [x1, #-1]
  00a7b69c: 11 f0 5f 38        ldurb      w17, [x0, #-1]
  00a7b6a0: 30 0a 50 8a        and        x16, x17, x16, lsr #2  ; x16=ip0, x17=ip1
  00a7b6a4: 1f 82 5c ea        tst        x16, x28, lsr #32  ; x16=ip0, x28=heap
  00a7b6a8: 40 00 00 54        b.eq       #0xa7b6b0
  00a7b6ac: b3 6c 43 94        bl         #0x1b56978
  00a7b6b0: 20 1c a1 4e        mov        v0.16b, v1.16b
  00a7b6b4: 14 00 00 14        b          #0xa7b704  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7b6b8: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a7b6bc: 20 f0 4e fc        ldur       d0, [x1, #0xef]
  00a7b6c0: 40 0b 46 a9        ldp        x0, x2, [x26, #0x60]  ; [x26=thread]
  00a7b6c4: 00 40 00 91        add        x0, x0, #0x10
  00a7b6c8: 5f 00 00 eb        cmp        x2, x0
  00a7b6cc: a9 1c 00 54        b.ls       #0xa7ba60
  00a7b6d0: 40 33 00 f9        str        x0, [x26, #0x60]  ; [x26=thread]
  00a7b6d4: 00 3c 00 d1        sub        x0, x0, #0xf
  00a7b6d8: 82 2b 9c d2        mov        x2, #0xe15c
  00a7b6dc: 62 00 a0 f2        movk       x2, #3, lsl #16
  00a7b6e0: 02 f0 1f f8        stur       x2, [x0, #-1]
  00a7b6e4: 00 70 00 fc        stur       d0, [x0, #7]
  00a7b6e8: 20 f0 0f b8        stur       w0, [x1, #0xff]
  00a7b6ec: 30 f0 5f 38        ldurb      w16, [x1, #-1]
  00a7b6f0: 11 f0 5f 38        ldurb      w17, [x0, #-1]
  00a7b6f4: 30 0a 50 8a        and        x16, x17, x16, lsr #2  ; x16=ip0, x17=ip1
  00a7b6f8: 1f 82 5c ea        tst        x16, x28, lsr #32  ; x16=ip0, x28=heap
  00a7b6fc: 40 00 00 54        b.eq       #0xa7b704
  00a7b700: 9e 6c 43 94        bl         #0x1b56978
  00a7b704: 01 1c a0 4e        mov        v1.16b, v0.16b
  00a7b708: 0b 00 00 14        b          #0xa7b734  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7b70c: a1 83 5f f8        ldur       x1, [x29, #-8]
  00a7b710: 42 1c 22 6e        eor        v2.16b, v2.16b, v2.16b
  00a7b714: 03 00 00 14        b          #0xa7b720  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7b718: e1 03 00 aa        mov        x1, x0
  00a7b71c: 42 1c 22 6e        eor        v2.16b, v2.16b, v2.16b
  00a7b720: 20 f0 4e fc        ldur       d0, [x1, #0xef]
  00a7b724: 21 70 4f fc        ldur       d1, [x1, #0xf7]
  00a7b728: 3f 1c a1 4e        mov        v31.16b, v1.16b
  00a7b72c: 01 1c a0 4e        mov        v1.16b, v0.16b
  00a7b730: e0 1f bf 4e        mov        v0.16b, v31.16b
  00a7b734: a0 83 5e f8        ldur       x0, [x29, #-0x18]
  00a7b738: a1 83 1b fc        stur       d1, [x29, #-0x48]
  00a7b73c: a0 03 1b fc        stur       d0, [x29, #-0x50]
  00a7b740: 22 30 4d b8        ldur       w2, [x1, #0xd3]
  00a7b744: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7b748: 43 70 40 fc        ldur       d3, [x2, #7]
  00a7b74c: 23 70 4d b8        ldur       w3, [x1, #0xd7]
  00a7b750: 63 80 1c 8b        add        x3, x3, x28, lsl #32  ; x28=heap
  00a7b754: 64 70 40 fc        ldur       d4, [x3, #7]
  00a7b758: 65 38 64 1e        fsub       d5, d3, d4
  00a7b75c: 71 22 80 d2        mov        x17, #0x113  ; x17=ip1
  00a7b760: 24 68 71 b8        ldr        w4, [x1, x17]
  00a7b764: 84 80 1c 8b        add        x4, x4, x28, lsl #32  ; x28=heap
  00a7b768: 83 70 40 fc        ldur       d3, [x4, #7]
  00a7b76c: a4 18 63 1e        fdiv       d4, d5, d3
  00a7b770: a4 03 1c fc        stur       d4, [x29, #-0x40]
  00a7b774: 45 f0 40 fc        ldur       d5, [x2, #0xf]
  00a7b778: 66 f0 40 fc        ldur       d6, [x3, #0xf]
  00a7b77c: a7 38 66 1e        fsub       d7, d5, d6
  00a7b780: e5 18 63 1e        fdiv       d5, d7, d3
  00a7b784: a5 83 1c fc        stur       d5, [x29, #-0x38]
  00a7b788: be 2e 39 94        bl         #0x18c7280
  00a7b78c: 04 04 80 d2        mov        x4, #0x20
  00a7b790: a0 03 1f f8        stur       x0, [x29, #-0x10]
  00a7b794: 4d 70 43 94        bl         #0x1b578c8
  00a7b798: e1 03 00 aa        mov        x1, x0
  00a7b79c: a0 03 5f f8        ldur       x0, [x29, #-0x10]
  00a7b7a0: 01 70 00 b8        stur       w1, [x0, #7]
  00a7b7a4: e1 03 00 aa        mov        x1, x0
  00a7b7a8: a0 ed f9 97        bl         #0x8f6e28  -> Matrix4.setIdentity
  00a7b7ac: a1 03 5f f8        ldur       x1, [x29, #-0x10]
  00a7b7b0: a0 03 5c fc        ldur       d0, [x29, #-0x40]
  00a7b7b4: a1 83 5c fc        ldur       d1, [x29, #-0x38]
  00a7b7b8: 42 1c 22 6e        eor        v2.16b, v2.16b, v2.16b
  00a7b7bc: 3f ea f9 97        bl         #0x8f60b8  -> Matrix4.translateByDouble
  00a7b7c0: a0 83 5b fc        ldur       d0, [x29, #-0x48]
  00a7b7c4: 42 03 46 a9        ldp        x2, x0, [x26, #0x60]  ; [x26=thread]
  00a7b7c8: 42 40 00 91        add        x2, x2, #0x10
  00a7b7cc: 1f 00 02 eb        cmp        x0, x2
  00a7b7d0: 49 15 00 54        b.ls       #0xa7ba78
  00a7b7d4: 42 33 00 f9        str        x2, [x26, #0x60]  ; [x26=thread]
  00a7b7d8: 42 3c 00 d1        sub        x2, x2, #0xf
  00a7b7dc: 80 2b 9c d2        mov        x0, #0xe15c
  00a7b7e0: 60 00 a0 f2        movk       x0, #3, lsl #16
  00a7b7e4: 40 f0 1f f8        stur       x0, [x2, #-1]
  00a7b7e8: 40 70 00 fc        stur       d0, [x2, #7]
  00a7b7ec: a1 03 5b fc        ldur       d1, [x29, #-0x50]
  00a7b7f0: 43 03 46 a9        ldp        x3, x0, [x26, #0x60]  ; [x26=thread]
  00a7b7f4: 63 40 00 91        add        x3, x3, #0x10
  00a7b7f8: 1f 00 03 eb        cmp        x0, x3
  00a7b7fc: 89 14 00 54        b.ls       #0xa7ba8c
  00a7b800: 43 33 00 f9        str        x3, [x26, #0x60]  ; [x26=thread]
  00a7b804: 63 3c 00 d1        sub        x3, x3, #0xf
  00a7b808: 80 2b 9c d2        mov        x0, #0xe15c
  00a7b80c: 60 00 a0 f2        movk       x0, #3, lsl #16
  00a7b810: 60 f0 1f f8        stur       x0, [x3, #-1]
  00a7b814: 61 70 00 fc        stur       d1, [x3, #7]
  00a7b818: a1 03 5f f8        ldur       x1, [x29, #-0x10]
  00a7b81c: ee ed f9 97        bl         #0x8f6fd4  -> Matrix4.scaleByDouble
  00a7b820: a3 83 5f f8        ldur       x3, [x29, #-8]
  00a7b824: 64 b0 49 b8        ldur       w4, [x3, #0x9b]
  00a7b828: 84 80 1c 8b        add        x4, x4, x28, lsl #32  ; x28=heap
  00a7b82c: a2 83 5e f8        ldur       x2, [x29, #-0x18]
  00a7b830: a4 83 1d f8        stur       x4, [x29, #-0x28]
  00a7b834: 40 78 7f 93        sbfiz      x0, x2, #1, #0x1f
  00a7b838: 5f 04 80 eb        cmp        x2, x0, asr #1
  00a7b83c: 60 00 00 54        b.eq       #0xa7b848
  00a7b840: 34 73 43 94        bl         #0x1b58510
  00a7b844: 02 70 00 f8        stur       x2, [x0, #7]
  00a7b848: e1 03 04 aa        mov        x1, x4
  00a7b84c: e2 03 00 aa        mov        x2, x0
  00a7b850: a0 03 1e f8        stur       x0, [x29, #-0x20]
  00a7b854: fe 5d 38 94        bl         #0x189304c  -> _LinkedHashMapMixin._getValueOrData
  00a7b858: a1 83 5d f8        ldur       x1, [x29, #-0x28]
  00a7b85c: 22 f0 40 b8        ldur       w2, [x1, #0xf]
  00a7b860: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7b864: 5f 00 00 6b        cmp        w2, w0
  00a7b868: 60 00 00 54        b.eq       #0xa7b874
  00a7b86c: 1f 00 16 6b        cmp        w0, w22
  00a7b870: a1 02 00 54        b.ne       #0xa7b8c4
  00a7b874: a0 03 5f f8        ldur       x0, [x29, #-0x10]
  00a7b878: 82 2e 39 94        bl         #0x18c7280
  00a7b87c: 04 04 80 d2        mov        x4, #0x20
  00a7b880: a0 03 1d f8        stur       x0, [x29, #-0x30]
  00a7b884: 11 70 43 94        bl         #0x1b578c8
  00a7b888: e1 03 00 aa        mov        x1, x0
  00a7b88c: a0 03 5d f8        ldur       x0, [x29, #-0x30]
  00a7b890: 01 70 00 b8        stur       w1, [x0, #7]
  00a7b894: e1 03 00 aa        mov        x1, x0
  00a7b898: 64 ed f9 97        bl         #0x8f6e28  -> Matrix4.setIdentity
  00a7b89c: 73 de 3a 94        bl         #0x1933268
  00a7b8a0: e1 03 00 aa        mov        x1, x0
  00a7b8a4: a0 03 5f f8        ldur       x0, [x29, #-0x10]
  00a7b8a8: 20 70 00 b8        stur       w0, [x1, #7]
  00a7b8ac: a2 03 5d f8        ldur       x2, [x29, #-0x30]
  00a7b8b0: 22 b0 00 b8        stur       w2, [x1, #0xb]
  00a7b8b4: e3 03 01 aa        mov        x3, x1
  00a7b8b8: a1 83 5d f8        ldur       x1, [x29, #-0x28]
  00a7b8bc: a2 03 5e f8        ldur       x2, [x29, #-0x20]
  00a7b8c0: c1 9a 34 94        bl         #0x17a23c4  -> _LinkedHashMapMixin.[]=
  00a7b8c4: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a7b8c8: 71 2a 80 d2        mov        x17, #0x153  ; x17=ip1
  00a7b8cc: 03 68 71 b8        ldr        w3, [x0, x17]
  00a7b8d0: 63 80 1c 8b        add        x3, x3, x28, lsl #32  ; x28=heap
  00a7b8d4: e1 03 03 aa        mov        x1, x3
  00a7b8d8: a2 03 5e f8        ldur       x2, [x29, #-0x20]
  00a7b8dc: a3 83 1d f8        stur       x3, [x29, #-0x28]
  00a7b8e0: db 5d 38 94        bl         #0x189304c  -> _LinkedHashMapMixin._getValueOrData
  00a7b8e4: e1 03 00 aa        mov        x1, x0
  00a7b8e8: a0 83 5d f8        ldur       x0, [x29, #-0x28]
  00a7b8ec: 02 f0 40 b8        ldur       w2, [x0, #0xf]
  00a7b8f0: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7b8f4: 5f 00 01 6b        cmp        w2, w1
  00a7b8f8: 61 00 00 54        b.ne       #0xa7b904
  00a7b8fc: e0 03 16 aa        mov        x0, x22
  00a7b900: 02 00 00 14        b          #0xa7b908  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7b904: e0 03 01 aa        mov        x0, x1
  00a7b908: a0 03 1d f8        stur       x0, [x29, #-0x30]
  00a7b90c: 1f 00 16 6b        cmp        w0, w22
  00a7b910: 80 05 00 54        b.eq       #0xa7b9c0
  00a7b914: a3 83 5f f8        ldur       x3, [x29, #-8]
  00a7b918: 64 f0 4a b8        ldur       w4, [x3, #0xaf]
  00a7b91c: 84 80 1c 8b        add        x4, x4, x28, lsl #32  ; x28=heap
  00a7b920: e1 03 04 aa        mov        x1, x4
  00a7b924: e2 03 00 aa        mov        x2, x0
  00a7b928: a4 83 1d f8        stur       x4, [x29, #-0x28]
  00a7b92c: c8 5d 38 94        bl         #0x189304c  -> _LinkedHashMapMixin._getValueOrData
  00a7b930: e1 03 00 aa        mov        x1, x0
  00a7b934: a0 83 5d f8        ldur       x0, [x29, #-0x28]
  00a7b938: 02 f0 40 b8        ldur       w2, [x0, #0xf]
  00a7b93c: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7b940: 5f 00 01 6b        cmp        w2, w1
  00a7b944: e0 03 00 54        b.eq       #0xa7b9c0
  00a7b948: 3f 00 16 6b        cmp        w1, w22
  00a7b94c: a0 03 00 54        b.eq       #0xa7b9c0
  00a7b950: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a7b954: 03 f0 4a b8        ldur       w3, [x0, #0xaf]
  00a7b958: 63 80 1c 8b        add        x3, x3, x28, lsl #32  ; x28=heap
  00a7b95c: e1 03 03 aa        mov        x1, x3
  00a7b960: a2 03 5d f8        ldur       x2, [x29, #-0x30]
  00a7b964: a3 83 1d f8        stur       x3, [x29, #-0x28]
  00a7b968: b9 5d 38 94        bl         #0x189304c  -> _LinkedHashMapMixin._getValueOrData
  00a7b96c: e1 03 00 aa        mov        x1, x0
  00a7b970: a0 83 5d f8        ldur       x0, [x29, #-0x28]
  00a7b974: 02 f0 40 b8        ldur       w2, [x0, #0xf]
  00a7b978: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7b97c: 5f 00 01 6b        cmp        w2, w1
  00a7b980: 61 00 00 54        b.ne       #0xa7b98c
  00a7b984: e0 03 16 aa        mov        x0, x22
  00a7b988: 02 00 00 14        b          #0xa7b990  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7b98c: e0 03 01 aa        mov        x0, x1
  00a7b990: 1f 00 16 6b        cmp        w0, w22
  00a7b994: a0 08 00 54        b.eq       #0xa7baa8
  00a7b998: 01 b0 41 b8        ldur       w1, [x0, #0x1b]
  00a7b99c: 21 80 1c 8b        add        x1, x1, x28, lsl #32  ; x28=heap
  00a7b9a0: 20 f0 5f f8        ldur       x0, [x1, #-1]
  00a7b9a4: 00 7c 4c d3        ubfx       x0, x0, #0xc, #0x14
  00a7b9a8: 1e f8 3f d1        sub        x30, x0, #0xffe
  00a7b9ac: be 7a 7e f8        ldr        x30, [x21, x30, lsl #3]
  00a7b9b0: c0 03 3f d6        blr        x30
  00a7b9b4: 00 70 40 fc        ldur       d0, [x0, #7]
  00a7b9b8: 01 1c a0 4e        mov        v1.16b, v0.16b
  00a7b9bc: 02 00 00 14        b          #0xa7b9c4  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7b9c0: 01 10 6e 1e        fmov       d1, #1.00000000
  00a7b9c4: 71 33 40 91        add        x17, x27, #0xc, lsl #12  ; x17=ip1, x27=pp
  00a7b9c8: 20 ae 46 fd        ldr        d0, [x17, #0xd58]  ; [x17=ip1]
  00a7b9cc: 20 20 60 1e        fcmp       d1, d0
  00a7b9d0: 4d 03 00 54        b.le       #0xa7ba38
  00a7b9d4: a0 83 5f f8        ldur       x0, [x29, #-8]
  00a7b9d8: 03 b0 49 b8        ldur       w3, [x0, #0x9b]
  00a7b9dc: 63 80 1c 8b        add        x3, x3, x28, lsl #32  ; x28=heap
  00a7b9e0: e1 03 03 aa        mov        x1, x3
  00a7b9e4: a2 03 5e f8        ldur       x2, [x29, #-0x20]
  00a7b9e8: a3 83 1d f8        stur       x3, [x29, #-0x28]
  00a7b9ec: 98 5d 38 94        bl         #0x189304c  -> _LinkedHashMapMixin._getValueOrData
  00a7b9f0: a1 83 5d f8        ldur       x1, [x29, #-0x28]
  00a7b9f4: 22 f0 40 b8        ldur       w2, [x1, #0xf]
  00a7b9f8: 42 80 1c 8b        add        x2, x2, x28, lsl #32  ; x28=heap
  00a7b9fc: 5f 00 00 6b        cmp        w2, w0
  00a7ba00: 61 00 00 54        b.ne       #0xa7ba0c
  00a7ba04: e1 03 16 aa        mov        x1, x22
  00a7ba08: 02 00 00 14        b          #0xa7ba10  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7ba0c: e1 03 00 aa        mov        x1, x0
  00a7ba10: 3f 00 16 6b        cmp        w1, w22
  00a7ba14: 20 01 00 54        b.eq       #0xa7ba38
  00a7ba18: a0 03 5f f8        ldur       x0, [x29, #-0x10]
  00a7ba1c: 20 70 00 b8        stur       w0, [x1, #7]
  00a7ba20: 30 f0 5f 38        ldurb      w16, [x1, #-1]
  00a7ba24: 11 f0 5f 38        ldurb      w17, [x0, #-1]
  00a7ba28: 30 0a 50 8a        and        x16, x17, x16, lsr #2  ; x16=ip0, x17=ip1
  00a7ba2c: 1f 82 5c ea        tst        x16, x28, lsr #32  ; x16=ip0, x28=heap
  00a7ba30: 40 00 00 54        b.eq       #0xa7ba38
  00a7ba34: d1 6b 43 94        bl         #0x1b56978
  00a7ba38: e0 03 16 aa        mov        x0, x22
  00a7ba3c: ef 03 1d aa        mov        x15, x29  ; x15=dsp
  00a7ba40: fd 79 c1 a8        ldp        x29, x30, [x15], #0x10  ; [x15=dsp]
  00a7ba44: c0 03 5f d6        ret        
  00a7ba48: e1 09 bf ad        stp        q1, q2, [x15, #-0x20]!  ; [x15=dsp]
  00a7ba4c: e1 8d 1f f8        str        x1, [x15, #-8]!  ; [x15=dsp]
  00a7ba50: e4 71 43 94        bl         #0x1b581e0
  00a7ba54: e1 85 40 f8        ldr        x1, [x15], #8  ; [x15=dsp]
  00a7ba58: e1 09 c1 ac        ldp        q1, q2, [x15], #0x20  ; [x15=dsp]
  00a7ba5c: 0d ff ff 17        b          #0xa7b690  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7ba60: e0 09 bf ad        stp        q0, q2, [x15, #-0x20]!  ; [x15=dsp]
  00a7ba64: e1 8d 1f f8        str        x1, [x15, #-8]!  ; [x15=dsp]
  00a7ba68: de 71 43 94        bl         #0x1b581e0
  00a7ba6c: e1 85 40 f8        ldr        x1, [x15], #8  ; [x15=dsp]
  00a7ba70: e0 09 c1 ac        ldp        q0, q2, [x15], #0x20  ; [x15=dsp]
  00a7ba74: 1c ff ff 17        b          #0xa7b6e4  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7ba78: e0 0d 9f 3c        str        q0, [x15, #-0x10]!  ; [x15=dsp]
  00a7ba7c: d9 71 43 94        bl         #0x1b581e0
  00a7ba80: e2 03 00 aa        mov        x2, x0
  00a7ba84: e0 05 c1 3c        ldr        q0, [x15], #0x10  ; [x15=dsp]
  00a7ba88: 58 ff ff 17        b          #0xa7b7e8  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7ba8c: e0 05 bf ad        stp        q0, q1, [x15, #-0x20]!  ; [x15=dsp]
  00a7ba90: e2 8d 1f f8        str        x2, [x15, #-8]!  ; [x15=dsp]
  00a7ba94: d3 71 43 94        bl         #0x1b581e0
  00a7ba98: e3 03 00 aa        mov        x3, x0
  00a7ba9c: e2 85 40 f8        ldr        x2, [x15], #8  ; [x15=dsp]
  00a7baa0: e0 05 c1 ac        ldp        q0, q1, [x15], #0x20  ; [x15=dsp]
  00a7baa4: 5c ff ff 17        b          #0xa7b814  -> FolderAnimController._updateFolderIconImgViewLoc
  00a7baa8: dc 73 43 94        bl         #0x1b58a18
