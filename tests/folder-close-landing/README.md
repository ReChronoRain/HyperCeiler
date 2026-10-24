# folder-close-landing

Regression for the folder close animation landing in the wrong place - it visibly
jumped: the first frame used stale settings, then snapped to the real workspace.

## Root cause it pins down

`WorkspaceRenderSnapshot::same_grid` compared **GC-movable `GridInfo` pointer
identity**. A cloned or freshly laid-out `GridInfo` is a different object with
identical numbers, so the cache missed, the animation math was fed an unrendered
destination, and the result was corrected a frame later. The fix canonicalizes to
the numeric scalars (`columns` / `rows` / `raw_width` / `raw_height`).

The `regression.cpp` fixture encodes exactly that case: a cloned/moved `GridInfo`
with 4x6 and raw 92/100 whose rendered top is -30 with height 85, while the
published settings say top=80, bottom=44. Passing means the animation targets the
rendered frame; failing means it would use the published numbers.

## Running

```sh
python -X utf8 tests/folder-close-landing/run.py modified
```

`modified` is the real gate - it builds `regression.cpp` plus the four existing
`tests/home-layout-native` suites (workspace / folder endpoint / folder
rendered-frame / drop-back) and expects all of them to pass.

The other two modes are the negative control and must **fail** the regression:

```sh
python -X utf8 tests/folder-close-landing/run.py baseline   # pre-change sources
python -X utf8 tests/folder-close-landing/run.py rollback   # restored originals
```

They materialise the "before" tree under `baseline-tree/` / `rollback-tree/` and
assert `TEST_EXIT=1`. A `baseline` that passes means the fixture no longer
discriminates, and the suite has quietly stopped testing anything.

## Freestanding host build

There is no host C++ runtime here on purpose: the tests compile with
`-nostdinc++` and link `ld.lld` with `/nodefaultlib` against a synthesised import
library, so a missing header or an accidental dependency on the MSVC CRT shows up
as a build failure instead of as subtly different numbers.

- `host-shim/` - the nine headers the tests need (`cstdio`, `cstring`, `cassert`,
  `cstdint`, `cstddef`, `cmath`, `limits`, `climits`, `vector`). The `vector` is
  declaration-only on purpose: a real call site fails at link time rather than
  quietly computing wrong numbers.
- `host-link/` - `crt.c` (the `entry` / `test_fail` pair, since no CRT is linked)
  and the `.def` files the import libraries are generated from.

All of it is rebuilt from source on every run, so none of it is tracked. Setting
`HC_NDK_BIN` overrides the NDK toolchain path.

**`crt.obj` must not be replaced by a `crt.h` declaration inside the shim** - it
already defines `test_fail`, and a second definition is a duplicate symbol at link
time. `host-shim/cassert` includes `crt.h` for the declaration only.

## stdout is block-buffered

Under this freestanding CRT there is no `fflush` and no unbuffered console. A
crashing test loses its buffered output, so "no output" never means "never reached
`main`". Debug with a line-prefix bisect rather than concluding from silence.

## Not tracked

See `.gitignore`. Build output, the transaction snapshots, and the screen/log
captures are all regenerable; `baseline-tree/` and `rollback-tree/` are per-run.
