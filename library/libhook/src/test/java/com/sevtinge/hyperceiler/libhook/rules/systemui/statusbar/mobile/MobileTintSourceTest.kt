/* SPDX-License-Identifier: AGPL-3.0-only */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile

import org.junit.Assert.*
import org.junit.Test

class MobileTintSourceTest {
    class StateFlowImpl(private var current: Any) {
        fun getValue(): Any = current
        fun setValue(value: Any) { current = value }
    }

    private class Binding(value: Any) {
        val visibility = StateFlowImpl(0)
        val unrelated = StateFlowImpl(Triple("label", false, 1))
        val tint = StateFlowImpl(value)
    }

    @Test fun backgroundChangeIsReadWithoutThemeOrSignalCallback() {
        val binding = Binding(Triple(false, true, -1))
        val source = requireNotNull(MobileTintSource.find(binding))
        assertEquals(DarkInfo.fromTintLightColor(false, true, -1), source.read())
        binding.tint.setValue(Triple(false, false, 0xff000000.toInt()))
        assertEquals(DarkInfo.fromTintLightColor(false, false, 0xff000000.toInt()), source.read())
        binding.tint.setValue(Triple(false, true, -1))
        assertEquals(DarkInfo.fromTintLightColor(false, true, -1), source.read())
    }

    @Test fun tintOnlyUpdateKeepsArgbIncludingAlpha() {
        val binding = Binding(Triple(true, false, 0xff000000.toInt()))
        val source = requireNotNull(MobileTintSource.find(binding))
        binding.tint.setValue(Triple(true, false, 0x99336699.toInt()))
        assertEquals(DarkInfo.fromTintLightColor(true, false, 0x99336699.toInt()), source.read())
    }

    @Test fun unchangedStateCanBeDeduplicatedButModeChangesAreDistinct() {
        val binding = Binding(Triple(true, true, -1))
        val source = requireNotNull(MobileTintSource.find(binding))
        val previous = source.read()
        assertEquals(previous, source.read())
        binding.tint.setValue(Triple(false, true, -1))
        assertNotEquals(previous, source.read())
    }

    @Test fun unrelatedFlowsAndTriplesAreIgnored() {
        assertNull(MobileTintSource.find(Binding(true)))
        val binding = Binding(Triple(true, false, 123))
        assertEquals(123, requireNotNull(MobileTintSource.find(binding)).read().color)
    }
}
