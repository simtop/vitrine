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
import org.junit.jupiter.api.Test

class ParameterControlTest {

    @Test
    fun `supported primitives map to their controls`() {
        assertEquals(ParameterControl.TEXT_FIELD, ParameterControl.forType("kotlin.String"))
        assertEquals(ParameterControl.CHECKBOX, ParameterControl.forType("kotlin.Boolean"))
        assertEquals(ParameterControl.NUMBER_FIELD, ParameterControl.forType("kotlin.Int"))
        assertEquals(ParameterControl.SLIDER, ParameterControl.forType("kotlin.Float"))
    }

    @Test
    fun `unsupported types are skipped rather than guessed at`() {
        assertNull(ParameterControl.forType("kotlin.Long"))
        assertNull(ParameterControl.forType("androidx.compose.ui.Modifier"))
        assertNull(ParameterControl.forType("com.example.MyType"))
        assertNull(ParameterControl.forType(null))
    }

    @Test
    fun `a nullable String is not treated as a String`() {
        // The processor resolves declarations, so nullability never reaches forType as part of the
        // name — but if that ever changes, silently mapping it would generate a non-null control
        // for a nullable parameter. Pin the current contract.
        assertNull(ParameterControl.forType("kotlin.String?"))
    }

    @Test
    fun `initial values are valid Kotlin literals for their control type`() {
        assertEquals("\"Default Value\"", ParameterControl.TEXT_FIELD.initialValue)
        assertEquals("false", ParameterControl.CHECKBOX.initialValue)
        assertEquals("0", ParameterControl.NUMBER_FIELD.initialValue)
        assertEquals("0f", ParameterControl.SLIDER.initialValue)
    }
}
