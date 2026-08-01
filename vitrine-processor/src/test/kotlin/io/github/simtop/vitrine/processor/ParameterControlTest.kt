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

class ParameterControlTest {

    @Test
    fun `every scalar type maps to its control`() {
        assertEquals(ParameterControl.TextField, ParameterControl.forScalar("kotlin.String"))
        assertEquals(ParameterControl.Checkbox, ParameterControl.forScalar("kotlin.Boolean"))
        assertEquals(ParameterControl.IntField, ParameterControl.forScalar("kotlin.Int"))
        assertEquals(ParameterControl.LongField, ParameterControl.forScalar("kotlin.Long"))
        assertEquals(ParameterControl.FloatSlider, ParameterControl.forScalar("kotlin.Float"))
        assertEquals(ParameterControl.DoubleSlider, ParameterControl.forScalar("kotlin.Double"))
        assertEquals(
            ParameterControl.DpSlider,
            ParameterControl.forScalar("androidx.compose.ui.unit.Dp"),
        )
        assertEquals(
            ParameterControl.ColorSwatches,
            ParameterControl.forScalar("androidx.compose.ui.graphics.Color"),
        )
    }

    @Test
    fun `unsupported types are not guessed at`() {
        assertNull(ParameterControl.forScalar("androidx.compose.ui.Modifier"))
        assertNull(ParameterControl.forScalar("com.example.MyType"))
        assertNull(ParameterControl.forScalar("kotlin.Short"))
        assertNull(ParameterControl.forScalar(null))
    }

    @Test
    fun `forScalar does not resolve nullability - that is the resolver's job`() {
        // forScalar sees fully-qualified names only; nullability is stripped before it is called,
        // so a name carrying a '?' is not a String and must not be treated as one.
        assertNull(ParameterControl.forScalar("kotlin.String?"))
    }

    @Test
    fun `an enum control carries its entries so the initial value can be its own first entry`() {
        val control = ParameterControl.EnumDropdown(
            packageName = "com.example",
            simpleNames = listOf("Size"),
            entryNames = listOf("SMALL", "LARGE"),
        )

        // This is the property the old enum-with-a-constant-String model could not express.
        assertEquals("SMALL", control.entryNames.first())
    }

    @Test
    fun `an enum with no entries is rejected - there is no value to start from`() {
        assertThrows(IllegalArgumentException::class.java) {
            ParameterControl.EnumDropdown("com.example", listOf("Empty"), entryNames = emptyList())
        }
    }

    @Test
    fun `a nested enum keeps every simple name so it stays addressable`() {
        val control = ParameterControl.EnumDropdown(
            packageName = "com.example",
            simpleNames = listOf("Outer", "Inner"),
            entryNames = listOf("A"),
        )

        assertEquals(listOf("Outer", "Inner"), control.simpleNames)
    }

    @Test
    fun `a nullable control wraps the control for its non-null type`() {
        val control = ParameterControl.Nullable(ParameterControl.TextField)

        assertEquals(ParameterControl.TextField, control.inner)
    }

    @Test
    fun `nullable controls do not nest`() {
        assertThrows(IllegalArgumentException::class.java) {
            ParameterControl.Nullable(ParameterControl.Nullable(ParameterControl.TextField))
        }
    }

    @Test
    fun `a provider control carries the class the generated code must instantiate`() {
        val control = ParameterControl.PreviewParameterValues(
            packageName = "com.example",
            simpleNames = listOf("NameProvider"),
        )

        assertEquals("com.example", control.packageName)
        assertEquals(listOf("NameProvider"), control.simpleNames)
    }
}
