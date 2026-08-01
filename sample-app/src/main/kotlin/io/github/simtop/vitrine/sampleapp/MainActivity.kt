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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@Composable
private fun CatalogApp() {
    // Every module's generated provider, found through ServiceLoader. Nothing is registered by hand.
    val items = remember { loadVitrineItems() }
    var selected by remember { mutableStateOf<VitrineItem?>(null) }

    Scaffold { padding ->
        val current = selected
        if (current == null) {
            ComponentList(items, Modifier.padding(padding)) { selected = it }
        } else {
            BackHandler { selected = null }
            ComponentDetail(current, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ComponentList(
    items: List<VitrineItem>,
    modifier: Modifier = Modifier,
    onSelect: (VitrineItem) -> Unit,
) {
    LazyColumn(modifier.fillMaxSize()) {
        items(items) { item ->
            Column(
                Modifier.fillMaxWidth().clickable { onSelect(item) }.padding(16.dp)
            ) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.tab, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ComponentDetail(item: VitrineItem, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            item.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )
        HorizontalDivider()
        item.content()
    }
}
