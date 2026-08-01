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
package io.github.simtop.vitrine.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.simtop.vitrine.VitrineComponent

/**
 * Every component here exists to make the processor generate a control it would not otherwise
 * generate. If one of these stops compiling, code generation regressed for that type.
 */

enum class Emphasis { LOW, MEDIUM, HIGH }

/** Enum → dropdown over the entries, initialised to the enum's own first entry. */
@VitrineComponent(tab = "Types", name = "Enum control")
@Composable
fun EmphasisLabel(emphasis: Emphasis) {
    Text("emphasis = $emphasis")
}

/** Color → swatch row; Dp → slider over 0..64. */
@VitrineComponent(tab = "Types", name = "Color and Dp controls")
@Composable
fun ColorSwatch(tint: Color, size: Dp) {
    Box(Modifier.size(size).background(tint))
}

/** Long → numeric field; Double → slider. */
@VitrineComponent(tab = "Types", name = "Long and Double controls")
@Composable
fun NumericReadout(count: Long, ratio: Double) {
    Text("count = $count, ratio = $ratio")
}

/** Nullable → a checkbox gating the inner control, with the inner value preserved when off. */
@VitrineComponent(tab = "Types", name = "Nullable control")
@Composable
fun OptionalCaption(caption: String?) {
    Text(caption ?: "(no caption)")
}

/** A nullable enum: the two composite controls compose. */
@VitrineComponent(tab = "Types", name = "Nullable enum control")
@Composable
fun OptionalEmphasis(emphasis: Emphasis?) {
    Text("emphasis = ${emphasis?.name ?: "unset"}")
}

class UserProvider : PreviewParameterProvider<User> {
    override val values = sequenceOf(User("Ada", 36), User("Grace", 45))
}

data class User(val name: String, val age: Int) {
    override fun toString() = "$name ($age)"
}

/**
 * The escape hatch: `User` has no built-in control, but a `@PreviewParameter` provider supplies
 * values, so the catalog renders a dropdown over them instead of failing.
 */
@VitrineComponent(tab = "Types", name = "PreviewParameter escape hatch")
@Composable
fun UserBadge(@PreviewParameter(UserProvider::class) user: User) {
    Text("user = $user", modifier = Modifier.padding(4.dp))
}

/** `@PreviewParameter` wins over the type mapping: this String uses the provider, not a TextField. */
@VitrineComponent(tab = "Types", name = "PreviewParameter overrides a supported type")
@Composable
fun PresetGreeting(@PreviewParameter(GreetingProvider::class) greeting: String) {
    Text(greeting)
}

class GreetingProvider : PreviewParameterProvider<String> {
    override val values = sequenceOf("Hello", "Hola", "Bonjour")
}

