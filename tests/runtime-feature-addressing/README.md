# OS4 executable feature addressing

The scaling/title binders and workspace paths migrated here discover candidates using executable PT_LOAD
instructions only. They do not read a launcher function name, a symbol span,
section headers or `.gnu_debugdata`. `Code` inventories Dart frame entries and
actual direct-call destinations. These are candidate boundaries, not identities;
each resolver separately proves its unique producer/consumer/callee contract.
Missing or ambiguous contracts preserve original behavior and report why.

Covered production paths:

- App icon dimensions and canonical desktop/Dock width/height mixins.
- Progress icon lifecycle dimensions, independent of duplicate mixin names.
- Small/large folder background, preview dimensions and padding entry.
- Folder-close dimensions/cached centering and app-to-folder stable landing.
- Animation ratio source/API chain and animation gear multiplier/predicates.
- The common widget Observer creation path, selected by the real dispatcher entrypoint and a validated caller; transform construction is an allocation/layout witness.
- Dock title location gates/animation wrappers, sharing their actual scope
  producer and two model getters with all five hotseat container IDs.
- Drawer font/line-height inputs in the validated icon builder; both helpers
  must have instruction-proved unboxed-double returns through their call chains.

- Workspace autofit after the original cap, correlating its actual legacy caller,
  cap and handler fields/frame consumers.
- Workspace empty/occupied cell delegates and Dock horizontal layout, sharing
  count, stride and item-index fields from their actual config/reactive sources.
- Dock bottom margin: the layout's actual subtraction uses a config-derived
  scalar, its original absolute-value diamond and a linked typed screen inset.
- Folder row spacing: the actual quotient is published as the aspect of a fresh
  grid delegate after its count and two gaps; unrelated divisions do not qualify.
- Column argument and persistence: the original map/default phone handler's
  actual callee clones the same raw counts used in config bounds. Its allocator,
  field widths, fallback locals, integer write sink, two preference boundaries
  and live layout-type store form one coherent bank.
- Widget movement gates: model rejection and editing rejection share the actual
  span check, reactive value capture and original reject arm. Closure and grid
  stack slots come from their real producers. Wrong aligned frame aliases and a
  low model-flag field are covered; supported span values still run stock.
- Indicator position: another anonymous owner allocates the fresh Insets; the
  independently identified Rect deflation determines its four fields, and a
  bounded constructor receives that same rooted object. A real same-producer
  decoy lacks the forwarding relationship and must be rejected. The shipping
  mutation is executed with original and relocated field/class/frame layouts.
- Indicator dot/edit policy: the owning anonymous closure's branches and decoded
  empty-widget pool load are linked to a proven canonical-Boolean edit query.
- Indicator slide policy: the actual show/animate raw-state field, typed idle
  producer and refresh return PC agree; a same-shaped show method is a decoy.
- Drag landing in global coordinates: its completed Offset must traverse both
  original projective transforms. The same local-centre arithmetic in another
  function is deliberately rejected; dimensions alone cannot identify this owner.

The default widget path bypasses `_TransformedWidgetContainer.build`. That
method is an allocation/layout witness, not a sufficient rendering target.
The common dispatcher is identified by its two matching three-way render
switches, shared captures and actual Miui/Maml producers. The image's own
AllocateClosure instructions supply the closure entrypoint field and the
context capture layout. Semantically identical Observer callers cannot be
identified by shape alone: their validated final-publication/epilogue banks
run stock unless the freshly returned closure's actual entrypoint AND caller
PC match the dispatcher contract. All candidate frame/child operands must agree;
all prepared banks must be ready before enabling the policy.

The hook runs AFTER the original allocator returned, covering either fast or
slow allocation returns. No VM allocator code, slow-allocation call or GC return
PC is patched or relocated. Only the proven widget path completes its original
Observer store/epilogue and allocates a centered wrapper in the bounded nursery
fast path. Unrelated closures and unknown callers preserve the original parent.
The wrapper cannot call Dart or trigger GC. Native callbacks retain numeric
contracts only; immutable routing snapshots survive concurrent replacement.

`widget_render_run.py` checks pinned 7751 and 8583 graphs, full executable-only
repackaging, independent relocation (including allocator tail branches), missing
and ambiguous graphs, and the shipping Observer completion/allocation with both
0/8-mod-16 nursery tops, invalid alignment, insufficient space and neutral scale.
The ARM64 script executes original Observer publication/epilogue, fast/slow
allocator-return register states, relocated fields, disabled/unrelated/unknown
callers and register/flag preservation. It does not emulate an actual VM GC.

The whole inventory is built once on the existing worker under the setup lease,
never inside the loader callback. Only immutable numeric contracts are retained;
code buffers are freed. An unsupported inventory is negatively cached until an
image-generation reset, preventing repeated full-image scans.

```sh
python3 tests/runtime-feature-addressing/run.py
python3 tests/runtime-feature-addressing/folder_run.py
python3 tests/runtime-feature-addressing/grid_run.py
python3 tests/runtime-feature-addressing/scalar_run.py
python3 tests/runtime-feature-addressing/columns_run.py
python3 tests/runtime-feature-addressing/indicator_run.py
python3 tests/runtime-feature-addressing/widget_move_run.py
python3 tests/runtime-feature-addressing/widget_render_run.py
# Optional pinned full 7751 image (not distributed in this repository):
python3 tests/runtime-feature-addressing/run.py --image /path/to/libapp.so
python3 tests/runtime-feature-addressing/folder_run.py --image /path/to/libapp.so
python3 tests/runtime-feature-addressing/grid_run.py --image /path/to/libapp.so
python3 tests/runtime-feature-addressing/scalar_run.py --image /path/to/libapp.so
python3 tests/runtime-feature-addressing/columns_run.py --image /path/to/libapp.so
python3 tests/runtime-feature-addressing/indicator_run.py --image /path/to/libapp.so
python3 tests/runtime-feature-addressing/widget_move_run.py --image /path/to/libapp.so
python3 tests/runtime-feature-addressing/verify_fixture.py /path/to/libapp.so
python3 tests/independent-object-scale/run.py
python3 tests/independent-object-scale/verify_arm.py /path/to/android-ndk
python3 tests/runtime-feature-addressing/verify_columns_arm.py /path/to/android-ndk
python3 tests/runtime-feature-addressing/verify_indicator_arm.py /path/to/android-ndk
```

Full-image tests repackage only executable words, removing ALL original section,
symbol, string and debug data. They also rebase text independently of file
offsets. Sparse fixtures independently move owners/callees and decoded fields,
duplicate candidates, remove links and corrupt control flow. The original
words come from launcher RELEASE-8.01.02.7751; the fixture contains its SHA256.
`verify_fixture.py` uses symbols solely as an independent test oracle.

This is not a repository-wide no-symbol claim. Remaining name-dependent desktop
paths are listed in [AUDIT.md](AUDIT.md). Actual widget appearance, inverse hit
testing, custom model transforms, phone/tablet variants and animations still
need validation on the newly built APK; offline instruction tests do not prove
device rendering or universal OTA compatibility.
