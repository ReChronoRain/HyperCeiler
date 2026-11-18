# Native Provider bootstrap SDK boundary

Run `python3 run.py /path/to/read-only-sdk-samples` from this directory. The thirteen
SDK filenames are listed in the runner; no vendor binary is committed.
Production headers resolve the C API's 48-byte result, all three normal return
paths and actual discriminants. An error producer reached from that interface
establishes native Status consumption, error code and the inverse three-word
move into the SDK byte-vector destructor. Callback targets follow actual ABS64
relocations, including Android APS2 data tables; private names are not signatures.
The resolver refuses incompatible widths, ownership moves and ambiguous data.
Tests run under ASan/UBSan and repeat with renamed private linkage/GNU hashes.

`android_sdk_test.cpp` separately loads the actual SDK and captures relocated
images. It creates only private AStatus objects and invokes the validated
constructor/SDK-owned destructor 4000 times with empty, UTF-8 and long messages.
This passed on the current device and changes no desktop state or settings.
It does not exercise a Provider request or prove launcher cold recovery.

The bootstrap implementation queues normal acquireUnstableContentProviderClient
requests on one event-driven worker. Configuration maintenance never waits for
that Binder call. An authenticated configuration publication releases the client;
a pending startup has a limited hold followed by 0.5/2/10/30 second backoff.
Unsupported ABI stops repeated requests. No shell command, arbitrary component
or arbitrary Binder forwarding is accepted. Actual cold recovery and retained
Context sampling remain separate device acceptance requirements.

The resolver constructor takes a reference to the SDK Runtime handle. Passing
the raw handle as though it were that reference caused a real launcher SIGBUS;
return-layout tests had not established input ABI correctness. The production
function type now requires `void* const*`, and its input load/clone propagation
is checked. The context observer also requires a matching input from an actual
stock constructor call before enabling bootstrap. The bootstrap's own direct
call cannot satisfy that caller-specific observation. This corrected path passed device acceptance on 2026-11-01: both ending
the module and ending the module plus its own Root daemon recovered the
configuration publication and native-card owner automatically, with the same
launcher PID. No manual service-start broadcast was used. A warm module that
was started in wms mode does not re-run Application.onCreate when an external
debug property changes; a successful acquire alone does not prove Root startup. Resolver and Client are kept together and closed
in Client-then-Resolver order; the opaque Client is not assumed independent.

The suite now additionally requires libhyper_os_schema_public.so. CallContract
checks the complete Provider call adapter, separately from acquire: equal-sized
48-byte results have different normal payloads. Tests move optional-string and
result fields, corrupt Bundle forwarding, and repeat with renamed private linkage.
Bundle ownership matches the actual returned Unique constructor against the
public constructor/drop allocator and size; bool packing is resolved from its
public getter. android_bundle_test.cpp passed 4000 real SDK empty-Bundle/key
lookups and destructions. This is separate from live Provider and material tests.

The native application-glass probe runs once per launcher only when root-socket
and debug.hyperceiler.home_glass_backend=app-probe are selected. It uses the
existing background worker and fixed homeglass authority. Registered :home_glass
HWUI, native input/Binder transfer, real SurfaceControl decoding, and a hidden
native child/OnCommit transaction passed device acceptance on 2026-11-01.
The Root HWUI host instead failed WMS Unknown pid/uid0. The visible WMS backend
has not been replaced; hidden transactions do not prove live background, geometry,
rotation or reveal behavior.

The extended suite also requires libhyper_os_aidl.dylib.so. The device's actual
BaseBundle producer appends mHasIntent after the length-delimited nonempty map.
BundleWireGolden.java captures that real producer; the native SDK's corresponding
AParcel_readBool consumer is required at runtime. This is not alignment padding.
android_parcel_test.cpp passed 4000 true/false UTF-8-key conversions through the
SDK and its public destructor. Native app-probe now constructs sorted typed
configuration and a real NDK Binder owner, calls the helper's actual create path,
and releases it. This passed on launcher17708, 2026-11-01, with activeHosts=0.
SurfaceControl transfer and hidden-parent submission passed separately; actual
background appearance and all visible lifecycle behavior remain unverified. Tests never marshal Binder-bearing packets.

The extended surface suite requires UI public/private, schema private, and app
public/private samples. Native ownership and borrowed conversions follow actual
SDK constructors, destructors and producer/consumer data flow. Java Parcelable
payloads retain their descriptor and are returned at their end; production
read_payload rewinds and validates the class name with AParcel_readString, without
fixed skips. A real framework Rect golden passed 4000 SDK round trips including
wrong-type refusal. Binder-bearing SurfaceControls are never marshalled.

Display geometry is a distinct boundary. Display_get_real_metrics takes both
Display and Configuration; an early independent test omitted the second input
and crashed only that test process. It was not connected to the desktop. Current
code uses context_get_configuration on the retained, validated live Context and
DisplayMetrics_get_metrics on that owned real Configuration. Both Configuration
copy/Drop and full default-display input forwarding are checked at runtime. The
independent android_display_test now tests only 4000 POD default metrics lifetimes,
not live geometry or configuration sourcing. Live Context sampling needs its own
device evidence; default configuration objects are not substitutes.


Native blur/corner bridges are traced from public SDK setters through native
Transaction borrowing and SurfaceControl producer/consumer ownership. The
current platform corner ABI has TWO float radii; scalar floating-point moves and
AAPCS64 low-64-bit v8-v15 preservation are modeled explicitly. Missing retains,
changed stack references, swapped floats and changed integer arguments refuse
binding. Private vendor symbol renaming remains part of the regression.

A native bridge is not the SDK's nullable wrapper. An independent Root-only test
with a null SurfaceControl reached HyperOS TransactionState::getLayerState and
crashed that test process. The module checks its typed native inputs before
calling; never use null/dummy pointers to probe vendor drawing setters. The
attempted Java Root fixture also could not load private SDK dependencies in its
JNI namespace; neither result is live-render acceptance. Test real, retained,
always-hidden surfaces in the actual launcher process and verify commit,
reference cleanup and launcher stability. Production WMS remains selected until
the separate visible backend has passed acceptance.

`python3 endpoint_run.py /path/to/read-only-sdk-samples` separately validates
minimal nine-image Provider/Bundle/owned-Binder composition for the extra gesture
snapshot endpoint. It deliberately omits UI, input and surface/material SDKs.
The suite checks matching Bundle destruction and NDK Binder retention, refuses
a damaged Bundle-drop tail, and repeats with private linkage renamed and GNU
hashes rebuilt. The runtime helper has a fixed authority, method and Binder key;
it does not accept caller-supplied commands or arbitrary Provider forwarding.
Actual SystemUI-to-native-Home snapshot transport is separate device acceptance.
