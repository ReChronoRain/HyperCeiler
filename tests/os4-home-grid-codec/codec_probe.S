// Host ABI to the ROM's Dart register ABI; no target algorithm is translated.
.text
.align 2
.global call_dart
.type call_dart,%function
call_dart:
 sub sp,sp,#176
 stp x19,x20,[sp,#0]
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
 ldr x16,[x0,#0]
 ldp x1,x2,[x0,#8]
 ldp x3,x4,[x0,#24]
 ldr x15,[x0,#40]
 ldr x27,[x0,#48]
 ldr x28,[x0,#56]
 ldr x22,[x0,#64]
 blr x16
 ldr x9,[sp,#160]
 stp x0,x15,[x9,#72]
 ldp x19,x20,[sp,#0]
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
.size call_dart,.-call_dart

.global dart_factory_adapter
.type dart_factory_adapter,%function
dart_factory_adapter:
 adrp x0,adapter_writer
 ldr x0,[x0,:lo12:adapter_writer]
 ret
.size dart_factory_adapter,.-dart_factory_adapter
.global dart_allocator_adapter
.type dart_allocator_adapter,%function
dart_allocator_adapter:
 adrp x0,adapter_serializer
 ldr x0,[x0,:lo12:adapter_serializer]
 ret
.size dart_allocator_adapter,.-dart_allocator_adapter
.global dart_send_adapter
.type dart_send_adapter,%function
dart_send_adapter:
 adrp x16,sent_registers
 add x16,x16,:lo12:sent_registers
 stp x1,x2,[x16]
 stp x3,x4,[x16,#16]
 ldr x17,[x15]
 str x17,[x16,#32]
 ldr x17,[x16,#40]
 add x17,x17,#1
 str x17,[x16,#40]
 mov x0,x22
 ret
.size dart_send_adapter,.-dart_send_adapter
.section .note.GNU-stack,"",%progbits
