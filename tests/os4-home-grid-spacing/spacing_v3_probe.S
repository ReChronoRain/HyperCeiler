// Explicit host-to-Dart register boundary. No Dart VM/GC is emulated.
.text
.p2align 2
.global call_geometry
.type call_geometry,%function
call_geometry:
 sub sp,sp,#176
 stp x19,x20,[sp]
 stp x21,x22,[sp,#16]
 stp x23,x24,[sp,#32]
 stp x25,x26,[sp,#48]
 stp x27,x28,[sp,#64]
 stp x29,x30,[sp,#80]
 stp d8,d9,[sp,#96]
 stp d10,d11,[sp,#112]
 stp d12,d13,[sp,#128]
 stp d14,d15,[sp,#144]
 str x0,[sp,#160]
 ldr x16,[x0]
 ldp x1,x2,[x0,#8]
 ldp x15,x27,[x0,#24]
 ldp x28,x22,[x0,#40]
 ldr x26,[x0,#56]
 ldr d0,[x0,#80]
 add x9,sp,#1,lsl #12
 str x9,[x26,#0x678]
 blr x16
 ldr x9,[sp,#160]
 str x15,[x9,#64]
 str d0,[x9,#72]
 str x0,[x9,#88]
 ldp x19,x20,[sp]
 ldp x21,x22,[sp,#16]
 ldp x23,x24,[sp,#32]
 ldp x25,x26,[sp,#48]
 ldp x27,x28,[sp,#64]
 ldp x29,x30,[sp,#80]
 ldp d8,d9,[sp,#96]
 ldp d10,d11,[sp,#112]
 ldp d12,d13,[sp,#128]
 ldp d14,d15,[sp,#144]
 add sp,sp,#176
 ret
.size call_geometry,.-call_geometry
.global geometry_context
.type geometry_context,%function
geometry_context:
 ldr x0,[x26,#0x60]
 add x16,x0,#64
 str x16,[x26,#0x60]
 add x0,x0,#1
 ret
.size geometry_context,.-geometry_context
.global geometry_null
.type geometry_null,%function
geometry_null:
 mov x0,x22
 ret
.size geometry_null,.-geometry_null
.global geometry_add
.type geometry_add,%function
geometry_add:
 ldp x0,x1,[x15]
 ldur d1,[x1,#7]
 cbz x0,1f
 ldur d0,[x0,#7]
 fadd d1,d0,d1
1:
 ldr x0,[x26,#0x60]
 add x16,x0,#16
 str x16,[x26,#0x60]
 add x0,x0,#1
 stur d1,[x0,#7]
 ret
.size geometry_add,.-geometry_add
.global geometry_rx
.type geometry_rx,%function
geometry_rx:
 ldur w0,[x1,#0x13]
 ret
.size geometry_rx,.-geometry_rx
.global spacing_copy_context
.type spacing_copy_context,%function
spacing_copy_context:
 // The exact final descriptor selects a recorder, not a Dart copyWith emulator.
 add x16,x27,#0x33000
 ldr x16,[x16,#0xec0]
 cmp x4,x16
 b.ne 1f
 ldr x17,[x26,#0x80]
 ldp x9,x10,[x15]
 stp x9,x10,[x17]
 ldp x9,x10,[x15,#16]
 stp x9,x10,[x17,#16]
 ldp x9,x10,[x15,#32]
 stp x9,x10,[x17,#32]
 ldp x9,x10,[x15,#48]
 stp x9,x10,[x17,#48]
 ldr x9,[x17,#64]
 add x9,x9,#1
 str x9,[x17,#64]
1: mov x0,x1
 ret
.size spacing_copy_context,.-spacing_copy_context
.global spacing_copy_insets
.type spacing_copy_insets,%function
spacing_copy_insets:
 mov x0,x1
 ret
.size spacing_copy_insets,.-spacing_copy_insets
.global spacing_provider
.type spacing_provider,%function
spacing_provider:
 ldr x0,[x26,#0x88]
 ret
.size spacing_provider,.-spacing_provider
.section .note.GNU-stack,"",%progbits
