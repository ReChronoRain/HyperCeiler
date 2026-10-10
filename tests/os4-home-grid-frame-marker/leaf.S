// Experimental immutable entry router. No native/Dart call or extra frame.
// x16/x17 scratch contract is explicit; all parameter/vector/flags/LR intact
// on passthrough. Exact actual caller literals and original target addresses.
.text
.balign 8
.global marked_router_begin,marked_router_end,marked_router_gate,marked_router_targets,marked_router_callers,marked_router_captured,marked_router_payload_store
.type marked_router_begin,%function
marked_router_begin:
 mov x6,#0
 ldr x17,marked_router_callers
 eor x17,x30,x17
 cbz x17,1f
 ldr x17,marked_router_callers+8
 eor x17,x30,x17
 cbz x17,1f
 ldr x17,marked_router_callers+16
 eor x17,x30,x17
 cbz x17,1f
 ldr x17,marked_router_callers+24
 eor x17,x30,x17
 cbz x17,1f
 ldr x17,marked_router_callers+32
 eor x17,x30,x17
 cbz x17,2f
 ldr x17,marked_router_callers+40
 eor x17,x30,x17
 cbz x17,2f
 b 5f
// Preserve caller NZCV while validating the one shared feature key.
.macro ADMIT closed, cut
 add x17,x5,#1
 lsr x17,x17,#2
 cbnz x17,\closed
 adr x17,marked_router_gate
 ldar x16,[x17]
\cut:
 nop // injected cut may close shared epoch after the one admission read
 tbz x16,#0,\closed
 adr x17,marked_router_key
 ldar x6,[x17]
 mrs x16,nzcv
 lsr x17,x6,#16
 cbnz x17,8f
 ubfx x17,x6,#8,#8
 cmp x17,#3
 b.lo 8f
 cmp x17,#9
 b.hi 8f
 and x17,x6,#0xff
 cmp x17,#4
 b.lo 8f
 cmp x17,#13
 b.hi 8f
 msr nzcv,x16
 b 3f
8:
 msr nzcv,x16
 mov x6,#0
 b \closed
.endm
1:
 ADMIT 4f,marked_router_captured
2:
 ADMIT 5f,marked_router_captured_fixed
3:
 ubfx x2,x6,#8,#8
 and x3,x6,#0xff
 ldr x17,marked_router_callers+8
 eor x17,x30,x17
 cbnz x17,5f
marked_router_payload_store:
 stur x2,[x0,#7]
5:
 ldr x16,marked_router_targets
 br x16
4:
 ldr x16,marked_router_targets+8
 br x16
.balign 8
marked_router_gate: .quad 0
.global marked_router_key,marked_router_captured_fixed
marked_router_key: .quad 0
marked_router_targets: .quad 0,0
marked_router_callers: .quad 0,0,0,0,0,0
marked_router_end:
.size marked_router_begin,.-marked_router_begin

// Fixed local frame marker: raw int64 remains int64; x5/x2 stay original.
.balign 8
.global marked_store_begin,marked_store_end,marked_store_branch,marked_store_write
marked_store_begin:
 stur x3,[x29,#-0x18]
 stur x5,[x29,#-0x20]
 cbz x6,1f
 add x16,x5,#1
 eor x16,x16,#0x4000000000000000
marked_store_write:
 stur x16,[x29,#-0x20]
1:
 ldur d0,[x4,#7]
 mov x1,x4
marked_store_branch:
 b .
marked_store_end:
// Exact original correctWorkspace caller plus exact fixed parent caller.
// For admitted frames restore raw layout BEFORE any math/tail use.
.balign 8
.global marked_math_begin,marked_math_end,marked_math_expected,marked_math_callers,marked_math_branch,marked_math_restore,marked_math_quantize
marked_math_begin:
 ldur x16,[x29,#8]
 ldr x17,marked_math_expected
 eor x16,x16,x17
 cbnz x16,9f
 ldr x16,[x29]
 ldr x16,[x16,#8]
 ldr x17,marked_math_callers
 eor x17,x16,x17
 cbz x17,1f
 ldr x17,marked_math_callers+8
 eor x17,x16,x17
 cbz x17,1f
 ldr x17,marked_math_callers+16
 eor x17,x16,x17
 cbz x17,1f
 ldr x17,marked_math_callers+24
 eor x17,x16,x17
 cbz x17,1f
 ldr x17,marked_math_callers+32
 eor x17,x16,x17
 cbz x17,1f
 ldr x17,marked_math_callers+40
 eor x17,x16,x17
 cbnz x17,9f
1:
 ldr x16,[x29]
 ldur x17,[x16,#-0x20]
 eor x17,x17,#0x4000000000000000
 lsr x16,x17,#2
 cbnz x16,9f
 sub x17,x17,#1
 ldr x16,[x29]
marked_math_restore:
 stur x17,[x16,#-0x20]
marked_math_quantize:
 ldur x1,[x29,#-8]
 ldur d3,[x1,#0x47]
 fmul d3,d3,d1
 frintz d3,d3
 fdiv d3,d3,d1
 stur d3,[x1,#0x47]
 stur d3,[x1,#0x4f]
 fmov d0,d3
9:
 ldur x0,[x29,#-0x10]
 scvtf d1,x0
 fmul d2,d0,d1
 stur d2,[x29,#-0x18]
marked_math_branch:
 b .
.balign 8
marked_math_expected: .quad 0
marked_math_callers: .quad 0,0,0,0,0,0
marked_math_end:
// Test-only outer-frame adapter: original natural caller suffix 1927f10.
.global marked_dispatch
marked_dispatch:
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 sub x15,x15,#0xf0
 ldr x6,[x26,#0x128]
 mov x17,#0xa0000000
 msr nzcv,x17
 ldr x17,[x26,#0xe0]
 br x17
// Test-only observers. Replayed original heads and branch targets follow.
.balign 8
.global observer_getter,observer_getter_end,observer_getter_branch
observer_getter:
 str x2,[x26,#0x130]
 str x5,[x26,#0x138]
 ldur x16,[x29,#-0x20]
 str x16,[x26,#0x140]
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 ldur d0,[x1,#0x17]
 ldr x4,[x27,#0x418]
observer_getter_branch: b .
observer_getter_end:
.balign 8
.global observer_tail,observer_tail_end,observer_tail_branch
observer_tail:
 str x2,[x26,#0x148]
 stp x29,x30,[x15,#-16]!
 mov x29,x15
 sub x15,x15,#0x20
 mov v2.16b,v0.16b
observer_tail_branch: b .
observer_tail_end:
.balign 8
.global observer_math,observer_math_end,observer_math_branch
observer_math:
 ldr x16,[x26,#0x150]
 cbz x16,2f
 ldr x17,[x26,#0x158]
 str xzr,[x17]
1:
 ldr x16,[x26,#0x160]
 cbz x16,1f
 ldr x17,[x26,#0x158]
 ldr x17,[x17]
 cbnz x17,1f
 // Mutant models invalid late-global-gate completion: erase admission marker.
 ldr x16,[x29]
 ldur x17,[x16,#-0x20]
 eor x17,x17,#0x4000000000000000
 sub x17,x17,#1
 stur x17,[x16,#-0x20]
1:
2:
observer_math_branch: b .
observer_math_end:
.section .note.GNU-stack,"",%progbits
