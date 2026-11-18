/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
void hc_native_input_receive_refresh(bool requested);
bool hc_native_input_receive_due();
enum class HomeInputStart {ready, pending, unsupported};
HomeInputStart hc_native_input_receive_resident();
// SDK control worker only. Retirement succeeds only after manager, monitor,
// timer and foreign carrier owner are gone, before restoring the old backend.
bool hc_native_input_receive_active();
bool hc_native_input_receive_retire();
