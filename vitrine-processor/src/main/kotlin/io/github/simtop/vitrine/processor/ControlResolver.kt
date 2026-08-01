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

    private const val VITRINE_RANGE = "io.github.simtop.vitrine.VitrineRange"

    /**
     * AndroidX's range annotations are read too, so a codebase that already documents its bounds
     * does not have to repeat itself. They cannot be the *only* mechanism: Lint's
     * `SupportAnnotationUsage` rejects `@FloatRange` on a `Dp` parameter ("expected float or
     * double"), so bounding a `Dp` slider through it would make the consumer's own build fail.
     */
    private val ANDROIDX_RANGE_ANNOTATIONS = setOf(
        "androidx.annotation.FloatRange",
        "androidx.annotation.IntRange",
    )

    /**
     * Resolves the control for [parameter], or null if its type is not controllable.
     *
     * `@PreviewParameter` is checked first and deliberately wins over the type-based mapping: it
     * works for *any* type, so an author who supplies one has explicitly overridden whatever
     * default the type would have got.
     */
    fun resolve(parameter: KSValueParameter): ParameterControl? {
        val control = fromPreviewParameter(parameter) ?: fromType(parameter.type.resolve())
        return applyRange(control, parameter)
    }

    /**
     * Narrows a slider's span to the parameter's `@FloatRange` / `@IntRange`, if it has one and the
     * control is rangeable.
     *
     * A range annotation on a non-slider parameter is left alone rather than reported: `@IntRange`
     * on an `Int` is ordinary documentation, and warning about it would punish good practice.
     */
    private fun applyRange(
        control: ParameterControl?,
        parameter: KSValueParameter,
    ): ParameterControl? {
        // Reach inside a nullable so `@FloatRange` works on `Float?` too.
        val target = (control as? ParameterControl.Nullable)?.inner ?: control
        if (target !is ParameterControl.Ranged) return control

        val range = declaredRange(parameter) ?: return control
        val ranged = target.withRange(range)
        return if (control is ParameterControl.Nullable) ParameterControl.Nullable(ranged) else ranged
    }

    /**
     * Reads the declared span, preferring `@VitrineRange` — it is the only one legal on every
     * slider-backed type, so an author who wrote both meant that one.
     */
    private fun declaredRange(parameter: KSValueParameter): SliderRange? {
        val byName = parameter.annotations.associateBy {
            it.annotationType.resolve().declaration.qualifiedName?.asString()
        }
        val annotation = byName[VITRINE_RANGE]
            ?: ANDROIDX_RANGE_ANNOTATIONS.firstNotNullOfOrNull { byName[it] }
            ?: return null

        fun bound(name: String): Float? =
            (annotation.arguments.find { it.name?.asString() == name }?.value as? Number)?.toFloat()

        // @FloatRange defaults its bounds to infinities, so a half-specified annotation
        // (`@FloatRange(from = 0.0)`) yields an unusable range. Fall back to the control's default
        // rather than emitting a slider that cannot be positioned.
        return SliderRange.orNull(
            min = bound("from") ?: return null,
            max = bound("to") ?: return null,
        )
    }

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
