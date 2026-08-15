package de.grocerycompare.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.grocerycompare.app.R
import de.grocerycompare.app.data.local.OfferEntity

@Composable
fun SearchScreen(
    onVoiceSearch: () -> Unit,
    modifier: Modifier = Modifier,
    voiceQuery: String? = null,
    onVoiceQueryConsumed: () -> Unit = {},
    vm: SearchViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()

    // When speech recognition returns a query, populate the field and search.
    LaunchedEffect(voiceQuery) {
        if (!voiceQuery.isNullOrBlank()) {
            vm.onQueryChange(voiceQuery)
            vm.submit()
            onVoiceQueryConsumed()
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = ui.query,
            onValueChange = vm::onQueryChange,
            label = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                IconButton(onClick = onVoiceSearch) {
                    Icon(Icons.Filled.Mic, stringResource(R.string.voice_search))
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // autocomplete suggestions
        if (ui.suggestions.isNotEmpty()) {
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.suggestions.take(3).forEach { s ->
                    ElevatedAssistChip(
                        onClick = { vm.onQueryChange(s); vm.submit() },
                        label = { Text(s, maxLines = 1) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (ui.offline) {
            Text(stringResource(R.string.offline_notice), style = MaterialTheme.typography.bodySmall)
        }

        if (results.isEmpty() && !ui.loading) {
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.no_results))
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Spacer(Modifier.height(4.dp)) }
                items(results, key = { it.id }) { OfferCard(it) { vm.watch(it.query) } }
            }
        }

        Text(
            stringResource(R.string.disclaimer),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun OfferCard(offer: OfferEntity, onWatch: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(chainLabel(offer.chain), fontWeight = FontWeight.Bold)
                Text(offer.productName, style = MaterialTheme.typography.bodyMedium)
                offer.packageSize?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("%.2f €".format(offer.price), fontWeight = FontWeight.SemiBold)
                    if (offer.unitPrice != null && offer.unitPriceUnit != null) {
                        Text(stringResource(R.string.per_unit,
                            "%.2f €".format(offer.unitPrice), offer.unitPriceUnit))
                    }
                    offer.distanceKm?.let {
                        Text(stringResource(R.string.distance_km, it))
                    }
                }
                Text(
                    if (offer.priceType == "offer") stringResource(R.string.weekly_offer)
                    else stringResource(R.string.regular_price),
                    style = MaterialTheme.typography.labelSmall,
                )
                offer.validTo?.let {
                    Text(stringResource(R.string.valid_until, it),
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onWatch) {
                Icon(Icons.Filled.NotificationsNone, stringResource(R.string.watch_item))
            }
        }
    }
}

private fun chainLabel(chain: String): String = when (chain) {
    "aldi_sued" -> "ALDI SÜD"
    "aldi_nord" -> "ALDI Nord"
    "lidl" -> "Lidl"
    "netto" -> "Netto Marken-Discount"
    "kaufland" -> "Kaufland"
    else -> chain
}
