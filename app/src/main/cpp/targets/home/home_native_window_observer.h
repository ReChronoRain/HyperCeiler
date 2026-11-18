/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
// Root-only passive probe of the launcher's own standard IWindow callback
// factory. Does not decode/consume events or change the renderer backend.
void hc_native_window_observer_prime();
void hc_native_window_observer_refresh();
