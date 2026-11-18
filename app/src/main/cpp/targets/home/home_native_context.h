/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
void hc_native_context_prime();
void hc_native_context_refresh();
// Borrowed only while this process retains the corresponding SDK Runtime.
// Configuration/bootstrap worker only; never a Dart/UI callback.
void* hc_native_context_get();
