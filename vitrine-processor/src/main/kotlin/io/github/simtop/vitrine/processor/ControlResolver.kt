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

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter

/**
 * Turns a component parameter into the [ParameterControl] Vitrine should generate for it.
 *
 * This is the only place that touches KSP types; [ParameterControl] itself stays a pure data model.
 */
internal object ControlResolver {

    const val PREVIEW_PARAMETER: String = "androidx.compose.ui.tooling.preview.PreviewParameter"

    /**
     * Resolves the control for [parameter], or null if its type is not controllable.
     *
     * `@PreviewParameter` is checked first and deliberately wins over the type-based mapping: it
     * works for *any* type, so an author who supplies one has explicitly overridden whatever
     * default the type would have got.
     */
    fun resolve(parameter: KSValueParameter): ParameterControl? =
        fromPreviewParameter(parameter) ?: fromType(parameter.type.resolve())

    private fun fromPreviewParameter(parameter: KSValueParameter): ParameterControl? {
        val annotation = parameter.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == PREVIEW_PARAMETER
        } ?: return null

        val provider = annotation.arguments
            .find { it.name?.asString() == "provider" }
            ?.value as? KSType
            ?: return null

        val declaration = provider.declaration as? KSClassDeclaration ?: return null
        return ParameterControl.PreviewParameterValues(
            packageName = declaration.packageName.asString(),
            simpleNames = declaration.simpleNames(),
        )
    }

    private fun fromType(type: KSType): ParameterControl? {
        if (type.isMarkedNullable) {
            // Recurse on the non-null type: T? is "a control for T, plus a way to say null".
            val inner = fromType(type.makeNotNullable()) ?: return null
            return ParameterControl.Nullable(inner)
        }

        ParameterControl.forScalar(type.declaration.qualifiedName?.asString())
            ?.let { return it }

        return asEnum(type)
    }

    private fun asEnum(type: KSType): ParameterControl? {
        val declaration = type.declaration as? KSClassDeclaration ?: return null
        if (declaration.classKind != ClassKind.ENUM_CLASS) return null

        val entries = declaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.ENUM_ENTRY }
            .map { it.simpleName.asString() }
            .toList()

        // An enum with no entries has no value to initialise the control with, so it is not
        // controllable — the caller reports it like any other unsupported type.
        if (entries.isEmpty()) return null

        return ParameterControl.EnumDropdown(
            packageName = declaration.packageName.asString(),
            simpleNames = declaration.simpleNames(),
            entryNames = entries,
        )
    }

    /** Simple names outermost first, so a nested declaration stays addressable. */
    private fun KSClassDeclaration.simpleNames(): List<String> {
        val names = ArrayDeque<String>()
        var current: KSClassDeclaration? = this
        while (current != null) {
            names.addFirst(current.simpleName.asString())
            current = current.parentDeclaration as? KSClassDeclaration
        }
        return names.toList()
    }
}
