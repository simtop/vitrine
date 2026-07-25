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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.simtop.vitrine.VitrineComponent

/** Exercises the String and Boolean controls. */
@VitrineComponent(tab = "Buttons", name = "Primary button")
@Composable
fun PrimaryButton(label: String, enabled: Boolean) {
    Button(onClick = {}, enabled = enabled) { Text(label) }
}

/** Exercises the Int control and the default display name (no `name` argument). */
@VitrineComponent(tab = "Text")
@Composable
fun RepeatedLabel(text: String, times: Int) {
    Text(text.repeat(maxOf(times, 0)))
}

/** Exercises the Float control. */
@VitrineComponent(tab = "Progress", name = "Determinate progress")
@Composable
fun DeterminateProgress(progress: Float) {
    LinearProgressIndicator(progress = { progress })
}

/**
 * Exercises [VitrineComponent.demoContainer] and the unsupported-parameter path: `modifier` has no
 * control, so it is omitted from the generated call and falls back to its default.
 */
@VitrineComponent(tab = "Text", name = "Framed label", demoContainer = "FramedLabelDemo")
@Composable
fun FramedLabel(caption: String, modifier: Modifier = Modifier) {
    Text(caption, modifier = modifier)
}

@Composable
fun FramedLabelDemo(caption: String) {
    Surface {
        Box(Modifier.padding(12.dp)) { FramedLabel(caption) }
    }
}
