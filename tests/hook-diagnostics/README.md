# Hook diagnostics regressions

```sh
python3 tests/hook-diagnostics/run.py
```

The tests compile the shipping pure Java wire/rules/provenance code, the shipping
Java observer against small Android/framework boundaries, and the actual Native
hook bank. They verify 10,000 warm callbacks/checks, original returns/exceptions,
null/extras forwarding, pending hot reload, logger failure isolation, resolver
uniqueness and foreign-patch refusal. The actual Room migration and history trim
SQL run against SQLite with pre-existing log rows.
Private export snapshots are tested with a reconstructed consumer that retains
only a saved token, immutable report generations, missing-cache/path rejection,
and bounded file/byte retention.
Presentation checks compile the shipping native feature-name function, verify all
236 legacy slot identities against the frozen consumer registry, exercise runtime
recent settings/actions and verify specific Java labels without changing evidence
or lifecycle states. Human copy allows commas only. Both launcher entries and the
first overview card are also checked in resources.

The observer also verifies non-launcher processes retain the original callback
and emit no diagnostics. UI/package filtering is desktop-only; see
[architecture and limitations](../../docs/hook-diagnostics.md).

The overview and export share status counts. Current executed/applied hooks count
as successful, installation alone stays pending. Tests cover failure precedence,
rearm waiting for new execution, disabled hooks, duplicate events, stale processes
and unavailable process identity, while preserving original event records. The UI
and report omit the former installation/execution/final-effect prompts.
