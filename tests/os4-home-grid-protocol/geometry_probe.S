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
 blr x16
 ldr x9,[sp,#160]
 str x15,[x9,#64]
 str d0,[x9,#72]
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
.section .note.GNU-stack,"",%progbits
