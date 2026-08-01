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

/**
 * Everything about the generated output that a consumer can configure, resolved from KSP arguments.
 *
 * This type is deliberately free of any KSP dependency so the option contract can be unit-tested
 * without running a compilation.
 */
internal data class VitrineOptions(
    val generatedPackage: String,
    val providerClassName: String,
    val isInternal: Boolean,
) {
    companion object {
        const val ARG_PACKAGE: String = "vitrine.generatedPackage"
        const val ARG_PROVIDER_NAME: String = "vitrine.providerName"
        const val ARG_VISIBILITY: String = "vitrine.visibility"

        const val DEFAULT_PACKAGE: String = "io.github.simtop.vitrine.generated"

        /**
         * Resolves options from the raw KSP argument map.
         *
         * [moduleName] only supplies the default provider class name; an explicit
         * [ARG_PROVIDER_NAME] wins. Blank values are treated as absent so that
         * `arg("vitrine.visibility", "")` behaves like not setting it at all.
         *
         * @throws IllegalArgumentException if [ARG_VISIBILITY] is set to anything other than
         *   `public` or `internal` — a silent fallback here would produce a catalog that compiles
         *   but cannot be seen from the app module.
         */
        fun from(args: Map<String, String>, moduleName: String): VitrineOptions {
            val visibility = args.nonBlank(ARG_VISIBILITY) ?: "public"
            require(visibility == "public" || visibility == "internal") {
                "$ARG_VISIBILITY must be 'public' or 'internal', was '$visibility'"
            }
            return VitrineOptions(
                generatedPackage = args.nonBlank(ARG_PACKAGE) ?: DEFAULT_PACKAGE,
                providerClassName = args.nonBlank(ARG_PROVIDER_NAME)
                    ?: defaultProviderClassName(moduleName),
                isInternal = visibility == "internal",
            )
        }

        /**
         * Turns a module name into a valid class name — `presentation_utils` becomes
         * `PresentationUtilsVitrineProvider`.
         *
         * The module name is part of the class name because every module generates its own
         * provider and they all land in the same package by default.
         *
         * **KSP hands us the leaf module name, not the Gradle path**: `:core:designsystem` arrives
         * as `designsystem`, so it yields `DesignsystemVitrineProvider`. Two modules with the same
         * leaf name under different parents therefore generate the same class name in the same
         * package. Set [ARG_PROVIDER_NAME] (or [ARG_PACKAGE]) on at least one of them if that
         * happens; this function cannot disambiguate what it was never given.
         */
        fun defaultProviderClassName(moduleName: String): String {
            val camelCased = moduleName
                .split(':', '-', '_', '.', ' ')
                .filter { it.isNotBlank() }
                .joinToString("") { part ->
                    part.replaceFirstChar { it.uppercaseChar() }
                }
                .filter { it.isLetterOrDigit() }
            val safe = when {
                camelCased.isEmpty() -> "Module"
                camelCased.first().isDigit() -> "Module$camelCased"
                else -> camelCased
            }
            return "${safe}VitrineProvider"
        }

        private fun Map<String, String>.nonBlank(key: String): String? =
            this[key]?.takeIf { it.isNotBlank() }
    }
}
