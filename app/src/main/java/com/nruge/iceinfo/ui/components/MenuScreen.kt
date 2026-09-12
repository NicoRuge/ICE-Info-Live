package com.nruge.iceinfo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.MenuCategory
import kotlinx.coroutines.launch

/**
 * Speisekarten-Sektion am Ende des Home-Screens (ehemals eigener Bottom-Tab).
 *
 * Wird als Abschnitt in die LazyColumn des Home-Screens eingehängt, damit die
 * Kategorie-Überschriften echte Sticky-Header der Seiten-Scrollliste sind.
 * Eingeklappt wird nur die Toggle-Karte gerendert — das Menü lädt erst beim
 * Aufklappen (siehe HomeScreen).
 *
 * @param chipsItemIndex Index des Chip-Row-Items in der umgebenden Liste;
 *   die Kategorie-Header folgen direkt dahinter (je Kategorie: Header + Karte).
 */
fun LazyListScope.menuSection(
    categories: List<MenuCategory>,
    isLoading: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    listState: LazyListState,
    chipsItemIndex: Int
) {
    item(key = "menu_toggle") {
        MenuToggleCard(expanded = expanded, onClick = onToggleExpanded)
    }
    if (!expanded) return

    when {
        isLoading && categories.isEmpty() -> item(key = "menu_loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        categories.isEmpty() -> item(key = "menu_empty") {
            Text(
                text = stringResource(R.string.menu_no_connection),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
        else -> {
            item(key = "menu_chips") {
                MenuCategoryChips(
                    categories = categories,
                    listState = listState,
                    chipsItemIndex = chipsItemIndex
                )
            }
            categories.forEach { category ->
                val headerKey = "menu_header_${category.title}"
                stickyHeader(key = headerKey) {
                    StickyMenuHeader(listState = listState, headerKey = headerKey) {
                        MenuSectionHeader(category.title)
                    }
                }
                item(key = "menu_card_${category.title}") {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        category.items.forEachIndexed { index, menuItem ->
                            MenuItemRow(menuItem)
                            if (index < category.items.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuToggleCard(expanded: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nav_menu),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.menu_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val chevronRotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                label = "menuChevron"
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(chevronRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MenuCategoryChips(
    categories: List<MenuCategory>,
    listState: LazyListState,
    chipsItemIndex: Int
) {
    val chipRowState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Listenindex des Sticky-Headers von Kategorie i (Chips + je Kategorie Header/Karte)
    fun headerIndex(i: Int) = chipsItemIndex + 1 + 2 * i

    val activeIndex by remember(categories, chipsItemIndex) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            categories.indices.lastOrNull { headerIndex(it) <= firstVisible } ?: 0
        }
    }
    LaunchedEffect(activeIndex) {
        chipRowState.animateScrollToItem(activeIndex)
    }

    LazyRow(
        state = chipRowState,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        itemsIndexed(categories) { i, category ->
            FilterChip(
                selected = i == activeIndex,
                onClick = {
                    scope.launch { listState.animateScrollToItem(headerIndex(i)) }
                },
                label = { Text(category.title, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
private fun MenuSectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StickyMenuHeader(
    listState: LazyListState,
    headerKey: String,
    content: @Composable () -> Unit
) {
    val isStuck by remember(headerKey) {
        derivedStateOf {
            val idx = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == headerKey }?.index ?: return@derivedStateOf false
            listState.firstVisibleItemIndex > idx
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isStuck) MaterialTheme.colorScheme.surfaceContainer
                else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        content()
    }
}
