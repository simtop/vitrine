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
 * The control Vitrine generates for a component parameter.
 *
 * This is a sealed hierarchy rather than an enum because half the controls need an initial value
 * that is a *function of the parameter's own type* — an enum parameter starts at its own first
 * entry, a nullable one wraps another control — which a constant cannot express.
 *
 * It carries no KSP types: an enum control holds the entry *names* it was resolved from, not the
 * `KSType`. That keeps the whole model unit-testable without running a compilation, and it is why
 * [ControlResolver] (which does touch KSP) is a separate file.
 */
internal sealed interface ParameterControl {

    /** `String` — a text field. */
    data object TextField : ParameterControl

    /** `Boolean` — a checkbox. */
    data object Checkbox : ParameterControl

    /** `Int` — a numeric text field. */
    data object IntField : ParameterControl

    /** `Long` — a numeric text field. */
    data object LongField : ParameterControl

    /** `Float` — a slider over `0f..1f`. */
    data object FloatSlider : ParameterControl

    /** `Double` — a slider over `0.0..1.0`. */
    data object DoubleSlider : ParameterControl

    /** `Dp` — a slider over `0.dp..64.dp`. */
    data object DpSlider : ParameterControl

    /** `Color` — a row of clickable swatches over a fixed palette. */
    data object ColorSwatches : ParameterControl

    /**
     * An enum class — a dropdown over its entries.
     *
     * @property packageName package of the enum class.
     * @property simpleNames the enum's simple names, outermost first, so nested enums stay
     *   addressable (`Outer`, `Inner`).
     * @property entryNames the enum's entries, in declaration order. Never empty — an enum with no
     *   entries has no value to start from and is not resolvable to a control.
     */
    data class EnumDropdown(
        val packageName: String,
        val simpleNames: List<String>,
        val entryNames: List<String>,
    ) : ParameterControl {
        init {
            require(simpleNames.isNotEmpty()) { "an enum control needs at least one simple name" }
            require(entryNames.isNotEmpty()) { "an enum control needs at least one entry" }
        }
    }

    /**
     * A nullable parameter — a checkbox gating [inner]'s control.
     *
     * The generated state is the inner control's value plus a presence flag; the value passed to
     * the component is derived from both. Holding `T?` directly would mean losing the edited value
     * every time the parameter is toggled off.
     */
    data class Nullable(val inner: ParameterControl) : ParameterControl {
        init {
            require(inner !is Nullable) { "T?? is not a thing; nullable controls do not nest" }
        }
    }

    /**
     * The escape hatch for arbitrary types: a `@PreviewParameter` provider supplies the values and
     * the catalog renders a dropdown over them.
     *
     * The values cannot be read at processing time — they only exist once the provider is
     * instantiated — so the generated code constructs the provider and indexes into its sequence.
     *
     * @property packageName package of the provider class.
     * @property simpleNames the provider's simple names, outermost first.
     */
    data class PreviewParameterValues(
        val packageName: String,
        val simpleNames: List<String>,
    ) : ParameterControl {
        init {
            require(simpleNames.isNotEmpty()) { "a provider control needs at least one simple name" }
        }
    }

    companion object {
        /**
         * Returns the control for a non-null, non-enum type, by fully-qualified name — the cases
         * that depend on nothing but the name itself.
         *
         * Enums, nullables and `@PreviewParameter` need more than a name to resolve; see
         * [ControlResolver].
         */
        fun forScalar(qualifiedName: String?): ParameterControl? = when (qualifiedName) {
            "kotlin.String" -> TextField
            "kotlin.Boolean" -> Checkbox
            "kotlin.Int" -> IntField
            "kotlin.Long" -> LongField
            "kotlin.Float" -> FloatSlider
            "kotlin.Double" -> DoubleSlider
            "androidx.compose.ui.unit.Dp" -> DpSlider
            "androidx.compose.ui.graphics.Color" -> ColorSwatches
            else -> null
        }
    }
}
