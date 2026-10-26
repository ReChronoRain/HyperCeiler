# OS4 runtime layout contracts

Run the shipping C++ binders and callbacks against launcher 7751 instructions:

```sh
python3 tests/runtime-layout-contracts/run.py
# Optional real Rust ELF checks:
python3 tests/runtime-layout-contracts/run.py /path/to/libapp_launcher.so
# Verify fixture provenance and production symbol-span boundaries:
python3 tests/runtime-layout-contracts/verify_fixture.py /path/to/launcher-7751.apk
```

The fixture comes from RELEASE-8.01.02.7751. APK SHA-256:
`f4fe410efcd72ae30550bf6beac9a991d06d1ce82f0d51b51f644b11658e7cc8`.
Named roots use the next executable STT_FUNC boundary from the complete mini-ELF
symbol table, including unnamed and zero-sized entries, matching production
`SymbolIndex::SpanFrom`. They do not use the often partial reported `st_size`.
For example, Workspace's reported 620 bytes excludes the anonymous child that
builds the indicator's Insets; its actual symbol span is 2152 bytes. Anonymous
callee prefixes are 28 original words for allocator/runtime-helper proofs.

Coverage includes Dock margin/folder row-spacing consumers, double-return animation
chains, animation gear branches, new-install visual methods, big-folder sizing,
grid auto-height, folder/drop geometry and original indicator construction.
Tests move frame roles consistently and execute the shipping callbacks on both
original and relocated frames. Indicator cases also move all fields, allocation
size and CID; check exact touched bytes and unchanged shared animation scalar.
Image rebasing, ambiguous consumers, broken aliases, wrong scalar sources/types,
changed arithmetic, invalid fields and branches entering replay windows are rejected.
`run.py` extracts the exact production auto-height callback and indicator binder;
their behavior is not reproduced in a separate test-only implementation.

The optional Rust test parses the real ELF/log-literal/unwind owner and mutates
the receiver's state-field operand. A new valid field is accepted; header overlap
and missing state stores are rejected. These are offline code/data-flow checks,
not device animation, GC or universal OTA acceptance tests.
