/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
// Existing configuration maintenance worker only. Queues normal platform IPC
// on an event-driven worker; never waits for Provider startup on this caller.
void hc_native_provider_refresh();
// Binder death callback: only coalesces one event, never calls the SDK.
void hc_native_provider_notify();
