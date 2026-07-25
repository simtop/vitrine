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
 * The Material 3 control Vitrine generates for a component parameter, chosen by the parameter's
 * type.
 *
 * A parameter whose type maps to `null` here is left out of the generated controls *and* the
 * generated call, so it falls back to its default value. That is why an unsupported parameter
 * without a default is a compile error in the generated code rather than a processor error — the
 * error points at the component, which is where the fix belongs.
 *
 * Kept free of KSP types so the type-to-control contract is unit-testable.
 */
public enum class ParameterControl(
    /** The literal used to seed the remembered state for this control. */
    public val initialValue: String,
) {
    TEXT_FIELD("\"Default Value\""),
    CHECKBOX("false"),
    NUMBER_FIELD("0"),
    SLIDER("0f"),
    ;

    public companion object {
        /** Returns the control for a fully-qualified Kotlin type name, or null if unsupported. */
        public fun forType(qualifiedName: String?): ParameterControl? = when (qualifiedName) {
            "kotlin.String" -> TEXT_FIELD
            "kotlin.Boolean" -> CHECKBOX
            "kotlin.Int" -> NUMBER_FIELD
            "kotlin.Float" -> SLIDER
            else -> null
        }
    }
}
