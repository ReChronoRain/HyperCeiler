# dynamic-address-audit

These suites validate the original workspace/Dock/capsule/title/widget paths they
explicitly exercise. They do **not** prove all current OS4 launcher entries or
all newly added folder features. A runtime-derived address still needs its
consumer/control-flow/ABI/lifetime contract proved.

Additional production-binder coverage:
- `tests/os4-audit-big-folder-20261005/verify.py`: 1506 checks, dynamic strategy
  fields, full original continuation, live parent-frame ratio, settings race.
- `tests/os4-audit-folder-geometry-20261005/verify.py`: real ELF, independently
  relocated callers/callees, moved grid fields, NOP-shifted scalar windows,
  incoming branch refusal, anonymous runtime helper bodies and atomic admission.

Full audit status and unresolved areas live in `handover/OS4_CODE_AUDIT.md`.
No suite here implies that the full OS4 audit is complete.

The audit is a three-legged A/B:

| leg | script | asserts |
|---|---|---|
| static address derivation | `verify.py` | each `needle`/`xref` walk lands on a named body, and the decode of the immediate/branch it feeds is right |
| real production binders | `verify_bindings.py` | the resolver functions in the shipped `home_*.cpp` return the addresses the static walk predicts |
| host geometry | `verify_geometry.py` | the geometry entry points reject an unresolved field set instead of computing on garbage |

`verify.py` and `verify_bindings.py` are **baseline-sensitive**: run against the
pre-change sources they must fail, against the working tree they must pass.
`verify_geometry.py` is not - it is a pure gate.

## Running

```sh
python -X utf8 tests/dynamic-address-audit/verify.py         <source-root>
python -X utf8 tests/dynamic-address-audit/verify_bindings.py <source-root>
python -X utf8 tests/dynamic-address-audit/verify_geometry.py
```

`verify_geometry.py` builds its own freestanding host objects under `geometry/`
and needs the vendored zig at `runtime/zig-windows-x86_64-0.13.0/zig.exe` (see
`.gitignore` - the toolchain and its caches are not tracked, so a fresh clone has
to supply its own).

`verify.py` and `verify_bindings.py` read the offline `libapp.so` image that
`verify.py`'s harness expects under the source root; without it they report a
missing-image error rather than a false pass.

## `transaction.py`

Regenerates the change artifacts (`MODIFIED_FILE.zip`, `DIFF_FILE.patch`,
`ROLLBACK.sh`, `VERIFICATION.txt`) and then runs the full A/B in both directions -
baseline must fail, modified must pass, and an independent rollback copy must fail
again. `python transaction.py rollback <dir>` restores the baseline into a
directory that must be `rollback-copy/`; it refuses any other target so it cannot
clobber the working tree.

## `device-YYYYMMDD/`

`modules_config.db` copies pulled off a device as LSPosed-module evidence. These
are per-run forensic captures: large, binary, and worthless a week later. They are
deliberately **not** tracked - see `.gitignore`.

## `*.asm`

Disassembly listings of the launcher symbols the audit resolves, kept as the
readable record of what each address actually is. They are inputs to review, not
to the build.
