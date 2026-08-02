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
package io.github.simtop.vitrine.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.simtop.vitrine.VitrineItem
import io.github.simtop.vitrine.loadVitrineItems

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CatalogApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogApp() {
    // Every module's generated provider, found through ServiceLoader. Nothing is registered by hand.
    val items = remember { loadVitrineItems() }
    var selected by remember { mutableStateOf<VitrineItem?>(null) }
    val current = selected

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.name ?: "Vitrine") },
                navigationIcon = {
                    if (current != null) {
                        IconButton(onClick = { selected = null }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        },
    ) { padding ->
        if (current == null) {
            ComponentList(items, Modifier.padding(padding)) { selected = it }
        } else {
            BackHandler { selected = null }
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            ) {
                current.content()
            }
        }
    }
}

@Composable
private fun ComponentList(
    items: List<VitrineItem>,
    modifier: Modifier = Modifier,
    onSelect: (VitrineItem) -> Unit,
) {
    // Items arrive sorted by (tab, name), so grouping preserves that order.
    val byTab = remember(items) { items.groupBy { it.tab } }

    LazyColumn(modifier.fillMaxSize()) {
        byTab.forEach { (tab, tabItems) ->
            item(key = "tab-$tab") {
                Text(
                    tab.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
                )
            }
            items(tabItems.size, key = { "$tab-${tabItems[it].name}" }) { index ->
                val item = tabItems[index]
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
