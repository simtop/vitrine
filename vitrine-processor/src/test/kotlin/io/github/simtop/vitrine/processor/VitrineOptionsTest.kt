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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VitrineOptionsTest {

    @Test
    fun `defaults apply when no arguments are set`() {
        val options = VitrineOptions.from(emptyMap(), moduleName = ":feature:beerslist")

        assertEquals(VitrineOptions.DEFAULT_PACKAGE, options.generatedPackage)
        assertEquals("FeatureBeerslistVitrineProvider", options.providerClassName)
        assertFalse(options.isInternal)
    }

    @Test
    fun `explicit arguments win over defaults`() {
        val options = VitrineOptions.from(
            args = mapOf(
                VitrineOptions.ARG_PACKAGE to "com.example.generated",
                VitrineOptions.ARG_PROVIDER_NAME to "MyProvider",
                VitrineOptions.ARG_VISIBILITY to "internal",
            ),
            moduleName = ":feature:beerslist",
        )

        assertEquals("com.example.generated", options.generatedPackage)
        assertEquals("MyProvider", options.providerClassName)
        assertTrue(options.isInternal)
    }

    @Test
    fun `blank arguments are treated as absent`() {
        val options = VitrineOptions.from(
            args = mapOf(
                VitrineOptions.ARG_PACKAGE to "   ",
                VitrineOptions.ARG_PROVIDER_NAME to "",
                VitrineOptions.ARG_VISIBILITY to "",
            ),
            moduleName = "app",
        )

        assertEquals(VitrineOptions.DEFAULT_PACKAGE, options.generatedPackage)
        assertEquals("AppVitrineProvider", options.providerClassName)
        assertFalse(options.isInternal)
    }

    @Test
    fun `an unrecognised visibility fails loudly instead of silently falling back`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            VitrineOptions.from(
                args = mapOf(VitrineOptions.ARG_VISIBILITY to "private"),
                moduleName = "app",
            )
        }

        assertTrue(failure.message!!.contains("'private'"), failure.message)
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        ":feature:beerslist, FeatureBeerslistVitrineProvider",
        "app-dev-beerslist,  AppDevBeerslistVitrineProvider",
        "core_common,        CoreCommonVitrineProvider",
        "com.example.ui,     ComExampleUiVitrineProvider",
        "app,                AppVitrineProvider",
    )
    fun `module names become valid class names`(moduleName: String, expected: String) {
        assertEquals(expected, VitrineOptions.defaultProviderClassName(moduleName.trim()))
    }

    @Test
    fun `a module name starting with a digit is prefixed so the class name stays valid`() {
        assertEquals("Module2048GameVitrineProvider", VitrineOptions.defaultProviderClassName("2048-game"))
    }

    @Test
    fun `a module name with no usable characters still yields a valid class name`() {
        assertEquals("ModuleVitrineProvider", VitrineOptions.defaultProviderClassName("::"))
    }
}
