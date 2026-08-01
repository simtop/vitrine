/*
 * Copyright 2026 simtop
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.simtop.vitrine.processor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SliderRangeTest {

    @Test
    fun `a half-specified FloatRange is not a usable span`() {
        // @FloatRange(from = 0.0) leaves `to` at +infinity. Emitting a slider over 0..infinity
        // would produce a control that cannot be positioned, so the control keeps its default.
        assertNull(SliderRange.orNull(0f, Float.POSITIVE_INFINITY))
        assertNull(SliderRange.orNull(Float.NEGATIVE_INFINITY, 1f))
        assertNull(SliderRange.orNull(Float.NaN, 1f))
    }

    @Test
    fun `an inverted or empty span is rejected`() {
        assertNull(SliderRange.orNull(10f, 1f))
        assertNull(SliderRange.orNull(5f, 5f))
    }

    @Test
    fun `a finite ascending span is accepted, including negatives`() {
        assertEquals(SliderRange(-50f, 50f), SliderRange.orNull(-50f, 50f))
        assertEquals(SliderRange(0f, 320f), SliderRange.orNull(0f, 320f))
    }

    @Test
    fun `constructing an invalid range directly fails loudly`() {
        assertThrows(IllegalArgumentException::class.java) { SliderRange(1f, 0f) }
        assertThrows(IllegalArgumentException::class.java) {
            SliderRange(0f, Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `the starting value is pulled inside a range that excludes it`() {
        // Dp's conventional 8 is outside 100..200, so the slider must not start there — the state
        // would be passed to the component while sitting outside its own slider's bounds.
        assertEquals(100f, SliderRange(100f, 200f).start(8f))
        assertEquals(24f, SliderRange(4f, 24f).start(50f))
    }

    @Test
    fun `the starting value is kept when the range already contains it`() {
        assertEquals(8f, SliderRange(4f, 24f).start(8f))
        assertEquals(0f, SliderRange.UNIT.start(0f))
    }

    @Test
    fun `defaults match the conventions the types are documented with`() {
        assertEquals(SliderRange(0f, 1f), SliderRange.UNIT)
        assertEquals(SliderRange(0f, 64f), SliderRange.DP_DEFAULT)
    }

    @Test
    fun `ranged controls carry their default until one is declared`() {
        assertEquals(SliderRange.UNIT, ParameterControl.FloatSlider().range)
        assertEquals(SliderRange.UNIT, ParameterControl.DoubleSlider().range)
        assertEquals(SliderRange.DP_DEFAULT, ParameterControl.DpSlider().range)
    }

    @Test
    fun `withRange replaces the span without changing the control type`() {
        val narrowed = ParameterControl.DpSlider().withRange(SliderRange(4f, 24f))

        assertEquals(ParameterControl.DpSlider(SliderRange(4f, 24f)), narrowed)
    }
}
