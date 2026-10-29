# OS4 custom application titles

Run the shipping resolver and callbacks with sanitizers:

```sh
python3 tests/home-custom-title-os4/run.py
python3 tests/home-custom-title-os4/run.py --image /path/to/current/libapp.so
```

The checked-in instruction fixture comes from launcher 8.01.02.8584. It has no
symbol table. The optional image check first discovers the existing icon and
title graphs, then uses those graphs as inputs to custom title discovery.
Addresses in the fixture are test coordinates, not production lookup keys.

Coverage includes independent function relocation, missing graph links, moved
stack slots, controller model fields, string class IDs and nursery fields. The
test extracts the actual shipping C callbacks and checks desktop and drawer
root replay, the original allocator continuation, unrelated registers, Unicode,
512 UTF-16 units, pinned models, disabled and unmatched titles, insufficient
space, misalignment and both valid nursery alignments, zero and eight modulo
sixteen. The desktop case deliberately uses a controller distinct from its
model, so passing the controller to model dispatch cannot silently regress.

Discovery verifies the original producer and consumer chain before installing
either site. Replacement strings use the decoded nursery fast allocator layout
and become roots in the original Dart stack before resuming the original Dart
allocation call. No Dart slow allocation is called from a native C frame.

Hook installation, callback execution, replacement allocation and visible title
are separate evidence. Host tests cannot establish the final visible effect on
a device, and the fixture is not a cross-version compatibility guarantee.

Device verification on 2026-10-27 used Xiaomi 15, OS4.0.0.22 and launcher
8.01.02.8584. With the user's existing Notes rule, disabling restored 笔记 and
enabling restored 自定义标题 after launcher restart. Tapping the renamed icon
opened Notes, returning home preserved the title and launcher PID. Native
diagnostics independently recorded EXECUTED and INTERNAL_APPLIED. Preferences
were identical to the saved pre-test snapshot and the screen timeout was
restored. Drawer rendering was covered by host callbacks but was not exercised
on this device, whose launcher remained in its original desktop mode.
