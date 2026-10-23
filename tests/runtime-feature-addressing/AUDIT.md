# Remaining desktop address discovery

Audit date: 2026-10-23. Scope: native OS4 desktop.

The scaling, related-animation, Dock-title and drawer-font binders, workspace
autofit/drag field/drop binders, the Dock bottom/folder row scalar getters and
three default workspace/Dock geometry roles, the column argument/persistence
graph, all three default indicator paths and the two widget-move gates now use
`ensure_runtime_features` and pure executable feature resolvers. The production
integration regression rejects symbol lookup or symbol-bounded body reads in
those binders. Addresses, fields, class tags and callees come from the
current image. ABI opcode/register contracts remain explicit and conservative.

Other production paths in `home_layout_hooks.cpp` still use named roots and
`dartscan::function_span` (which uses `HomeTweaksSymbolSpan`). They are not counted
as completed merely because they validate instructions after the name lookup:

| Remaining family | Entry points to migrate |
|---|---|
| Folder content/preview/title centering and sizing | `bind_folder_preview`, folder layout/geometry and big-folder binders |
| Title color/custom text/hiding and new-install appearance | `bind_title_color`, `bind_title_custom`, `bind_desktop_title`, title-hide/new-install binders |
| Dock count/edit state | `sync_hotseat_capacity`, `bind_dock_edit` |
| Widget transport bridge, folder auto-close | `bind_gadget_bridge`, `sync_folder_auto_close` |
| Workspace tap/status/swipe policies | `bind_workspace_double`, `bind_workspace_status`, `bind_workspace_scope` |

`tweaks/ht_plan.cpp::LocateFunction` also remains symbol-first. Its signature
fallback retains several business immediates and cannot be described as a
fully semantic resolver. Its folder-column initialization route has an
additional direct `SymbolIndex::Find` gate. Existing tests that force
`SetForceSymbolOnlyForTest(true)` do not validate symbol independence.

Completed geometry discovery also validates the same actual current-config to
reactive-value call chain, all live frame slots and branch-free replay windows.
The optional Dock delegate is disabled on ambiguity without disabling proven
workspace geometry. Column discovery additionally links the actual cloned config counts to the
shared integer persistence sink and live layout-type store. Indicator policy
now owns its anonymous closure directly, and the idle return PC is derived
from the actual typed refresh/show/animate chain. Folder content-width/title
layout remains separate migration work. Indicator position additionally proves
the anonymous owner's fresh Insets, Rect-deflation fields, bounded constructor
and rooted argument forwarding; the source scalar remains unchanged.

The now-unused `capsule_wrapper_layout_compatible`, `indicator_policy_compatible`
and `indicator_slide_compatible`
predicates remain only for historical host-fixture extraction. Default runtime
binders do not invoke them; integration tests assert this explicitly.

Widget-move admission now derives the actual reactive/span callees and the
closure/grid frame producers. Its two previously literal frame slots have been
removed; whole-frame relocation and wrong-but-aligned alias rejection are tested.
The independent Gadget transport serializer/clone bridge is still name-dependent
and is not counted as migrated by changing its shared movement gate.

Pure calibration probes behind explicit debug properties can retain named
diagnostics. Platform imports such as `AServiceManager_getService`/`madvise` and
our own assembly globals are ABI linkage, not private launcher hook discovery;
they are not candidates for replacing with guessed machine-code searches.

Rust back regions, windows, gestures and native grid columns already use
image/log-literal/unwind/instruction relationships. Their runtime semantics are
separate from this Dart migration. Subsequent device verification and the
common-widget rendering correction are recorded in
`../independent-object-scale/README.md`. No remote Git state was changed.
