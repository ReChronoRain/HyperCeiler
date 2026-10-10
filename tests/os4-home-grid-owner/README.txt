OS4 phone-grid owner candidate (offline test16 work)
=================================================

Target: popsicle / OS4.0.0.44.XPBCNXM, pinned launcher libapp.so
a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992.

This directory generates and executes an experimental owner in a separate
output directory. It does not modify the production owner, the production
generated header, the installed module, or the sealed test15 APK.

Run from source:
  python3 tests/os4-home-grid-owner/run.py --output NEW_DIRECTORY

generate_plan.py retains the hash-pinned v5 non-phone variants and replaces
phone family 0 with the three typed column producers and five variable-to-fixed
call redirections. It independently reads every original word from the ELF.
The candidate has 43 unique constant sites and 18 inline hooks. New hooks are
installed downstream first: correctWorkspace at a4ab78, secondary entry at
a49adc, then primary entry at 15228e8. The runtime still validates the backend's
16-byte overwrite contract and all guarded bytes after every installation.

This owner expects three resident assembly implementations. They are not linked
into this host fixture; fake backend functions exercise ownership and failure
paths only. The primary entry and secondary entry must perform their own caller
routing even when the shared epoch is closed. They must not be wrapped in the
old generic epoch-off -> original-fixed gateway.

The one phone configuration key is 0 (disabled) or (columns << 8) | rows. The
entry consumer must first acquire an odd hc_layout_startup_epoch_gate and then
acquire hc_home_grid_config_key once. A late nonzero key cannot authorize a
closed epoch. Nonzero dimensions are pinned at the initial snapshot. Row-only
changes are explicitly rejected because they no longer change static words.
Turning the feature off retains the structural hooks while other features may
keep the common epoch active. Re-enabling a restored first-use word group needs
a new launcher process; this test does not promise hot reconfiguration.

After any successful activation, error handling closes admission and restores
owned constant words but retains hook continuations. In particular, losing
authorization in the final post-activation check cannot safely unhook a
quantizer needed by a frame that just entered. Before any activation, verified
startup rollback may still release backend hooks. This is a lifetime rule, not
a claim that closing a gate establishes thread quiescence or rolls back cached
Dart/Rust values.

host-dev-v1 kept a surviving negative control: the selected feature-off test
observed the key only after the later publisher had zeroed it again. The test
selection was corrected to read the key immediately after request_revoke.
host-final-v1/earlier plans were also superseded after independent review
demonstrated missing guards on arithmetic callees. The final v2 plan includes
those actual full bodies, with explicit first/last-word refusal tests. Earlier
reports are retained as historical results, not selected as the final plan.

The positive suite includes the original owner fault tests, all 794 plan
variants, explicit phone admission/publication checks, invalid dimensions,
row-only late changes, retained hooks after activation, and semantic callee
guards. Negative controls must fail a test assertion; a compile error or an
unexplained process failure does not count as a detected defect.

Limits:
  - The host maps OEM code as data. This suite does not execute that ARM code.
  - Inline patching, authorization scheduling, and consumer calls are adapters.
  - ARM routers, original arithmetic, and default placement have separate
    reports with their own explicit adapters and boundaries.
  - Real LSPosed/Zygisk injection, Dart GC, UI rendering, SQLite persistence,
    asynchronous races and long-duration device stability remain NOT_TESTED.
  - Passing this owner candidate alone does not approve production PhoneGrid.
