/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * The geometry knobs this module exposes for the launcher layout.
 *
 * Each knob names the Dart accessor that produces the value *at layout time*, and the address is
 * found at run time from the launcher image's own symbol table (`.gnu_debugdata`). That is the
 * whole trick: the name is part of the launcher's build, so it survives an OTA, while the address
 * is read from the image that is actually loaded. Nothing here carries an offset, a signature or a
 * pool slot. The 2026-09-16 launcher OTA (8.01.02.6180 -> 8.01.02.6305) moved every address in the
 * image and left every name intact, which is exactly what this indirection is for.
 *
 * Why accessors and not the values the launcher computes from:
 *  - the geometry the desktop lays out with is read through `GridController`/`HotSeatsConstants2`
 *    accessors on every layout, so a hook installed after startup still takes effect on the next
 *    frame;
 *  - the values the launcher *computes* the config from (`GridSizeCalRules.stable*` and friends) are
 *    read once during start-up, before libapp.so is mapped and therefore before any hook can exist,
 *    so hooking those would never be seen. They remain useful only as a cross-check.
 *
 * The trampoline adds the user's delta to whatever the accessor returned, so an untouched slider is
 * exactly the launcher's own value to the last bit.
 *
 * `default_dp` is the settings page's own default, which is what makes a delta neutral when the
 * user has not moved the slider; `min_dp`/`max_dp` are the page's own bounds and are enforced again
 * here so a hostile or stale snapshot cannot push the launcher somewhere absurd.
 */
#pragma once

/*
 * X(name, Column, Symbol, DefaultDp, MinDp, MaxDp)
 *
 * `Column` doubles as the trampoline/global suffix: hc_layout_dart_<Column>_entry,
 * hc_layout_dart_<Column>_original, hc_layout_dart_<Column>_delta,
 * hc_layout_dart_<Column>_enabled, hc_layout_dart_<Column>_hits.
 */
#define HC_LAYOUT_KNOBS(X)                                                                       \
    X(HotseatMargin, HotseatMargin, "GridController.hotSeatsMarginBottom", 70, 0, 150)           \
    X(HotseatHeight, HotseatHeight, "HotSeatsConstants2.hotSeatsHeight", 80, 60, 150)            \
    X(WorkspaceTop, WorkspaceTop, "GridSizeCalRules.stableWorkspaceCellPaddingTop", 30, 0, 150)   \
    X(WorkspaceBottom, WorkspaceBottom, "GridController.workspaceCellPaddingBottom", 120, 0, 240) \
    X(WorkspaceSide, WorkspaceSide, "GridController.workspaceCellPaddingSide", 20, 0, 100)        \
    X(IndicatorMargin, IndicatorMargin, "GridController.workspaceIndicatorMarginBottom", 70, 0, 150) \
    X(SearchBarMargin, SearchBarMargin, "GridController.searchBarMarginBottom", 30, 0, 150)       \
    X(SearchBarWidth, SearchBarWidth, "GridController.searchBarWidthPx", 30, 0, 400)

#define HC_LAYOUT_KNOB_COUNT 8
