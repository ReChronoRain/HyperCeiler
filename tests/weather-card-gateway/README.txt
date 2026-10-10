Weather card gateway component, prepared after the sealed test7 release

Production ownership in this task is limited to weather_card_arm64.S and
weather_card_gateway.h. These templates do not register a hook, install code,
verify the snapshot window, select an image, query HWS1, or verify seccomp.

Interface: Kind::Recompute / Kind::Constructor; kCodeSize=284,
kLeaseLiteralOffset=288, kTemplateSize=296. copy_template writes a caller-owned,
unpublished RW buffer with natural8-byte alignment. Assembly exports
hc_weather_card_{recompute,constructor}_template, _code_end, _lease_literal,
_end. The first symbol is also the beginning of the copyable byte range.
The two reviewed original normal epilogues are at3a8408 and3aece0.

Each code body is the reviewed v3 body with exactly two ADRP+ADD pairs replaced
by internal LDR-literal+NOP pairs. No other instruction changes. The injected
literal is a pointer, not a copied lease value. Each gateway acquire-loads the
pointed-to word twice. Raw lowbits0/1 deny; raw2/3 may write only with a sampled
unexpired absolute BOOTTIME deadline. A producer can reserve raw1 as a terminal
sentinel; an ordinary zero update must not reopen a producer terminal state.
The installer/publisher owns that stronger state machine.

Both copied RX memory and the aligned lease word need process-lifetime storage
while gateways can execute. Revoke through the producer's atomic word protocol;
never free either buffer as a revocation mechanism. The installer must flush
I-cache before publication and verify source guards, page placement and its
single-word branch plan. Copying this template grants no installation permission.

Reproduce with a new output directory from project root:
 python3 source/tests/weather-card-gateway/run.py --output /tmp/new-card-gateway-run --negative-controls
 python3 source/tests/weather-card-gateway/audit_compiled.py --run /tmp/new-card-gateway-run --output /tmp/new-card-gateway-audit
The local ARM compiler/QEMU and exact ROM ELF paths and hashes are in the report.

The fixture executes actual production templates copied into two independent
RX pages, four copies using two different injected lease addresses. Only the
first original MOV becomes B; the other three original epilogue words remain
unchanged. Both normal and forced-ENOSYS builds pass14392 assertions. Fifteen
source/header-copy negative controls fail. The complete Android shared-library
templates match the executed GCC program templates byte-for-byte, have no
dynamic relocations, no external branches, and two literal reads at offset288.
The helper header also compiles with the actual Android NDK C++20 toolchain.

The fixture descends from the frozen v3 source; ancestry.json pins those inputs.
It retains explicit leaf substitutes for original constructor/recompute helpers.
It executes the complete original constructor and selected original recompute
tails, not every real recompute dependency or the whole Dart VM. Guard-page
and register assertions are controlled tests, not phone tests. No previous
claim of11 complete guards is inherited; full source-image guards are separate.

Boundaries: no real device, framework injection, actual Android vendor thread
seccomp filter, PAC/BTI integration, asynchronous GC/safepoint execution,
production installation/rollback, or long-term stability is proven here.
A raw syscall returning ENOSYS safely falls back. A seccomp TRAP/KILL cannot
return through that fallback. Auxiliary pinned AOSP source inspection does not
prove the vendor policy or an API37 release association.

The Thread+0x48 check is a read, not an interrupt reservation. Valid original
Thread/frame/receiver are prerequisites; the low-bit/CID254 check is not an
arbitrary-pointer validator. Lease/clock reads are finite samples: preemption,
revocation after final observation and same-value ABA are not excluded. Neither
this code nor release-storing deny restores a field already consumed/cached by
Dart. Counts are assertions, not numbers of supported module settings.
