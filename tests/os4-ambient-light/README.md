# OS4 AmbientLight lifecycle regression

`run.py --output <new-directory> --negative-controls` compiles the whole production
`AmbientLightOS4.kt` and the whole `AmbientLight.kt` route. It executes the actual
EzHook 1.3.0 / libxposed API102 before/after chain. The registration backend and
Android views/Looper, shader, color resolver, ROM original methods, and executor
scheduling are explicit host fixtures. The executor intentionally permits stale
and reentrant deliveries so the production generation checks must reject them.
This suite does not execute Xiaomi SystemUI, the GPU, or LSPosed on a phone.

The route is compiled separately against the SDK37 and frozen test12 peer jars
whose paths/hashes are recorded in the SDK report; the host runtime supplies
minimal version/preference adapters. The OS3 test proves branch selection with
all legacy options disabled and no OS4 classes. Byte identity of the retained
legacy body is checked separately; this is not a full OS3 integration test.

`compile_android.py --output <new-directory>` compiles the route, adapter, current
resolver, native color wrapper and actual shared Drawable together against real SDK37/API102 and
frozen test12 peer jars. It uses no Android, resolver, or shader stand-ins.
The report records all five source hashes before/after. It is a compilation
check, independent from the host state-machine suite and the Android shader
pixel/state probe run by the root task.

Relevant scenarios include independent NC/DI selection; DI modes 0/1/2 and
unknown imports; only selected dependency resolution; all-hook activation and
rollback; separate NC/DI controllers and both island holders; original skip and
exceptions; attach/detach nesting; pairing mismatch cleanup; pre-window color
preparation; original bounds and outline ownership; current full-AOD/dark mode;
playing/drag state and dummy offset; failure restoring OEM backgrounds; stale
and reentrant result rejection; listener/removal failures attempting all owned
cleanup; successful null DI binds restoring OEM while NC null binds remain no-ops; and hot reload revocation. Mutations modify temporary copies only.

OEM background/parent ownership changes are respected. A failed restore is
reported rather than called a successful cleanup. Full-AOD static state is
re-read at every supported event/commit; the five NC entry points do not prove
zero transitional frames immediately after every external static flag write.
Preference changes use the existing SystemUI restart boundary. Device status
remains NOT_TESTED. This component suite does not approve the independent
native-backed color algorithm or the final APK.
