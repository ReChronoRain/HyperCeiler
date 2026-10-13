# OS4 folder title alignment regression

This probe exercises the real production symbol filter, current launcher 7722 instruction windows,
TextAlign semantic roots, replay ABI, and local native callbacks.

Run from the repository root:

```powershell
python -X utf8 tests/folder-title-center-os4/verify.py .
```

The current snapshot passes 96 host checks. These are not a substitute for visual verification.
The production splice now updates the freshly built inner Container's alignment before its
first consumer. Its replay window, frame, owner local, fields and semantic roots are derived
from launcher instructions; Dart objects remain 8-byte aligned. Folder title centering is
installed and user-confirmed on launcher 7722. The padding field is discovered but is not
rewritten by this callback. The splice starts at the common text/editor join, rejects incoming
branches into its interior, and compares the selected x3 child with the fresh Container local
before writing. This covers the editor branch that previously entered the middle of the patch.
On launcher 7722, three editor entries, clear/rename/save and three folder-close cycles
passed without new crash records; the original folder name was restored exactly.
Long-title and RTL behavior still require separate device regression; do not reuse
older preference backups that reset the user's title setting.

The local handover/WORKTREE_TAKEOVER.md records the continuation details; handover is gitignored.
Device screenshots, preference backups, debugger probes, APKs, signing material, and tool binaries
remain local and are not part of this source commit.

The host probes currently require the local launcher snapshot/mini symbols documented in the
script, Python with Capstone, and the local Zig compiler under tests/dynamic-address-audit/runtime.
