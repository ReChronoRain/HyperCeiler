/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
struct ZygiskNextAPI;
// Control worker only, after fresh authorization. No new thread or JNI.
void hyperceiler_hyos_retry_home_report(const ZygiskNextAPI *api);
