/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.home.dock

/**
 * Exception-only replacements for Kotlin's [runCatching], shared by the dock glass classes.
 *
 * <p>Kotlin's `runCatching` catches [Throwable], so it silently converts `OutOfMemoryError`,
 * `StackOverflowError` and `ThreadDeath` into an ordinary failure result. Inside system_server
 * that hides a dying process behind a log line; these helpers catch [Exception] only, so
 * VM-fatal throwables keep their normal behaviour. Deliberate `Error` tolerance elsewhere
 * (e.g. `loadClass` returning null for an optional class missing on another ROM) is a
 * compatibility decision and must stay explicit instead of being expressed through these.
 */

/** Runs [body], passing a thrown [Exception] to [onError]; VM-fatals propagate. */
internal inline fun catchingRecoverable(body: () -> Unit, onError: (Exception) -> Unit = {}) {
    try {
        body()
    } catch (error: Exception) {
        onError(error)
    }
}

/**
 * Runs [body] and returns its result, or [fallback] when it throws an [Exception].
 *
 * <p>Drop-in replacement for `runCatching { ... }.getOrDefault(fallback)` with the same
 * Exception-only contract as [catchingRecoverable].
 */
internal inline fun <T> catchingRecoverableOr(fallback: T, body: () -> T): T =
    try {
        body()
    } catch (error: Exception) {
        fallback
    }
