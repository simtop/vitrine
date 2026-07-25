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

import androidx.compose.runtime.Composable
import java.util.ServiceLoader

/** One entry in the catalog: a named, tab-grouped, renderable component demo. */
public data class VitrineItem(
    val tab: String,
    val name: String,
    val content: @Composable () -> Unit,
)

/**
 * The generated per-module catalog index.
 *
 * Vitrine generates one implementation per module that contains [VitrineComponent]-annotated
 * functions, and registers it as a `java.util.ServiceLoader` service. Call [loadVitrineItems] to
 * collect every module's items at runtime — that is what makes the catalog work across a
 * multi-module project without any central registration.
 */
public interface VitrineProvider {
    public val items: List<VitrineItem>
}

/**
 * Loads every [VitrineProvider] on the classpath and returns their items, sorted by tab then name
 * so the catalog's ordering does not depend on classpath order.
 */
public fun loadVitrineItems(
    // Nullable because Class.getClassLoader() returns null for bootstrap-loaded classes, and
    // ServiceLoader treats null as "use the system class loader".
    classLoader: ClassLoader? = VitrineProvider::class.java.classLoader,
): List<VitrineItem> =
    ServiceLoader.load(VitrineProvider::class.java, classLoader)
        .flatMap { it.items }
        .sortedWith(compareBy({ it.tab }, { it.name }))
