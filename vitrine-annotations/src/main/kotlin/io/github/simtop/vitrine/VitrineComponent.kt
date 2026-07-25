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
package io.github.simtop.vitrine

/**
 * Marks a `@Composable` function for inclusion in the generated component catalog.
 *
 * For every annotated function, Vitrine generates an interactive wrapper that renders a Material 3
 * control for each supported parameter — a text field for [String], a checkbox for [Boolean], a
 * numeric field for [Int], a slider for [Float] — above a live instance of the component. Editing a
 * control recomposes the component, so the catalog is a playground rather than a static gallery.
 *
 * Parameters whose types are not in that set are omitted from both the controls and the generated
 * call, so they must have default values.
 *
 * ```kotlin
 * @VitrineComponent(tab = "Buttons", name = "Primary button")
 * @Composable
 * fun PrimaryButton(label: String, enabled: Boolean) { /* … */ }
 * ```
 *
 * @property tab Groups components in the catalog UI. Components sharing a tab appear together.
 * @property name Display name in the catalog. Defaults to the function's own name when empty.
 * @property demoContainer Name of a sibling function, in the same package, to render instead of the
 *   annotated one. Use it when a component needs surrounding scaffolding (a `Surface`, a fixed-size
 *   `Box`) to look right in isolation. The container must accept the same parameters.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class VitrineComponent(
    val tab: String,
    val name: String = "",
    val demoContainer: String = "",
)
