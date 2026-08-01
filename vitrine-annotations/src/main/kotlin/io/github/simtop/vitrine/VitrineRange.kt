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
 * Sets the span of the slider Vitrine generates for a parameter.
 *
 * ```kotlin
 * @VitrineComponent(tab = "Layout")
 * @Composable
 * fun Spacer(@VitrineRange(0.0, 320.0) width: Dp) { /* … */ }
 * ```
 *
 * Applies to the slider-backed types — `Float`, `Double` and `Dp`. On any other parameter it is
 * ignored rather than reported, so it is harmless on a type whose control has no span.
 *
 * **Prefer AndroidX's `@FloatRange` where it is legal.** Vitrine reads that too, and on a `Float` or
 * `Double` parameter it documents the same constraint to every other tool as well. It exists here
 * because AndroidX's annotation is *rejected by Lint* on a `Dp` parameter — `@FloatRange` is
 * specified for `float`/`double` only — so there was no legal way to bound a `Dp` slider without
 * this. When both are present, `@VitrineRange` wins.
 *
 * A range whose bounds are not finite and ascending is ignored, and the control keeps its default
 * span; a slider over an infinite span cannot be positioned.
 *
 * @property from the low end of the slider.
 * @property to the high end of the slider.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
public annotation class VitrineRange(
    val from: Double,
    val to: Double,
)
