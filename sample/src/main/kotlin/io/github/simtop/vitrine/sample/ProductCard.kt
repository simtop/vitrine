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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.simtop.vitrine.VitrineComponent
import io.github.simtop.vitrine.VitrineRange

/**
 * A deliberately awkward component, to show what [VitrineComponent.demoContainer] is for.
 *
 * Three things make it a bad fit for the default treatment:
 *  1. `onAddToCart` is a lambda — Vitrine has no control for it, and firing it should visibly *do*
 *     something or the button looks broken.
 *  2. It stretches to its container, so rendered full-bleed it reads as a banner, not a card.
 *  3. Its interesting states (long title truncating, out of stock) are easy to miss unless the demo
 *     puts them in front of you.
 *
 * `ProductCardDemo` below solves all three. The annotation is on *this* function, so the generated
 * controls still come from this signature — the container only changes what gets rendered.
 */
@VitrineComponent(
    tab = "Commerce",
    name = "Product card",
    demoContainer = "ProductCardDemo",
)
@Composable
fun ProductCard(
    title: String,
    @VitrineRange(from = 0.0, to = 500.0) price: Double,
    inStock: Boolean,
    accent: Color,
    onAddToCart: () -> Unit = {},
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(accent, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            )
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("€%.2f".format(price), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (inStock) "In stock" else "Sold out",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (inStock) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                Button(
                    onClick = onAddToCart,
                    enabled = inStock,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add to cart")
                }
            }
        }
    }
}

/**
 * The custom demo view for [ProductCard].
 *
 * A demo container takes the same *controllable* parameters as the component it stands in for —
 * Vitrine passes them straight through — and is free to do anything with them. This one:
 *
 *  - supplies the `onAddToCart` lambda Vitrine cannot generate a control for, and **shows its
 *    effect**, so the button proves it works instead of looking inert;
 *  - constrains the card to a realistic width instead of letting it span the screen;
 *  - renders a **second, fixed instance** with a deliberately long title, so the truncation
 *    behaviour is always on screen rather than something you have to think to test.
 */
@Composable
fun ProductCardDemo(
    title: String,
    price: Double,
    inStock: Boolean,
    accent: Color,
) {
    var cartCount by remember { mutableIntStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.width(300.dp)) {
            ProductCard(
                title = title,
                price = price,
                inStock = inStock,
                accent = accent,
                onAddToCart = { cartCount++ },
            )
        }

        Text(
            if (cartCount == 0) "onAddToCart has not fired" else "onAddToCart fired ×$cartCount",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            "Fixed case — long title truncates:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.width(300.dp)) {
            ProductCard(
                title = "Single-origin cold brew concentrate, 1L bottle",
                price = price,
                inStock = inStock,
                accent = accent,
            )
        }
    }
}
