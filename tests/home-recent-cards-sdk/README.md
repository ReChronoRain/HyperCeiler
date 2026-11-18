# Native SDK TaskInfo contract for recent-card filtering

Run from this directory:

```sh
python3 run.py /path/to/read-only-sdk-samples
```

The directory must contain `ui-public.so`, `aidl.so`, `ui-private.so`,
`schema-public.so`, `binder-rs.so` and `std.so`, from one current system/launcher generation.
No vendor binary is checked in. The runner records each input SHA256 and compiles
production headers under ASan/UBSan. It also renames private Rust imports and
exports in temporary copies, rebuilds a valid GNU hash table and repeats the tests.

The entry anchors are public platform C ABI functions. Private function names
are never feature signatures: the resolver follows actual direct calls and
validated AArch64 PLT relocations. Program headers and unwind information establish
code bounds; ELF section metadata is unnecessary. Each Graph owns its snapshots
and memoizes linkage on one setup worker. Do not share the parser with callbacks.

The current supported ABI is little-endian AArch64, its standard AAPCS64 register
roles and the validated PLT family. This is not a claim that every OS4 SDK or future
compiler is compatible. Missing, changed or ambiguous contracts decline.

The resolver verifies:

- Three nullable Activity consumers and four consecutive ComponentName writes in
  TaskInfo parcel order, with the fourth identified as realActivity
- Corresponding deserializer calls, error checks, old-value cleanup and every byte
  needed by the package-name consumer, including integer/vector temporary copies
- Direct or inlined optional ComponentName decoding against the public member
  consumers, the actual String producer family and all forward control-flow paths
- RunningTaskInfo's inline TaskInfo size against two actual reader copy lengths
- Typed Intent serializer/deserializer linkage, nullable component/package access,
  pure borrowed string getters and complete required ranges within TaskInfo

`visit_task` synchronously consumes only borrowed SDK data through a whole-read-or-
failure transport. It retains no task/string address after its visitor returns.
It preserves realActivity, Intent component, Intent package priority, including
present empty or oversized names, and propagates memory failures for whole-reply
retention. Host memory fixtures provide only this boundary transport.

Tests cover a consistently relocated fourth Activity field (including cleanup and
payload), writer-only and header-only moves, altered pointer widths, null branch,
missing and duplicate linkage, size mismatch, owning snapshot lifetime, absent
sections, renamed scratch registers, 5000 random decoder inputs and invalid CFGs.
Access tests run with both baseline and relocated contracts, exercising every read
failure, null/empty/oversized names, precedence and visitor refusal.

The suite also resolves GroupedTaskInfo producer/consumer/drop contracts,
nullable one-level mixed children, and all four returned RVec word roles.
`group_test.cpp` exercises the real production accessor and reply compaction,
including allocator-family linkage, pointer/length/capacity/table preservation,
owned root-box disposal, read failures, rejected commits and changed snapshots.
The fixed stack plan does not allocate per query. Private names are rewritten in
all six SDK inputs before a second complete run.

`android_sdk_test.cpp` is a separate Android executable. It captures actual
relocated SDK images, calls their real group allocator/getter/destructor, and
checks Binder weak-listener lifetime and standard one-way notification protocol.
It does not modify the desktop. Its device result is recorded separately from
host fixtures and live launcher testing.

The launcher integration is experimental behind `root-socket`, with versioned
Root acknowledgement and exclusive native/SystemUI ownership. Ordinary card
hide/restore was verified on device. Mixed/split UI and every launcher version
still require separate validation; parser acceptance or installation alone is
not evidence of displayed results. No SDK binary is committed.
