package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.R
import com.nruge.iceinfo.model.MenuCategory
import com.nruge.iceinfo.model.MenuItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    categories: List<MenuCategory>,
    isLoading: Boolean,
    trainConnected: Boolean,
    onLoad: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { onLoad() }
    LaunchedEffect(trainConnected) { if (trainConnected) onLoad() }

    PullToRefreshBox(
        isRefreshing = isLoading && categories.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            when {
                isLoading && categories.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                categories.isEmpty() -> MenuEmptyState()
                else -> MenuContent(categories = categories)
            }
        }
    }
}

@Composable
private fun MenuContent(
    categories: List<MenuCategory>
) {
    val categoryNames = remember(categories) { categories.map { it.title } }

    val categoryFirstIndex = remember(categories) {
        var idx = 0
        categories.associate { cat ->
            val start = idx
            idx += 2
            cat.title to start
        }
    }

    val listState = rememberLazyListState()
    val chipRowState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val activeCategory by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            categoryNames.lastOrNull { name -> (categoryFirstIndex[name] ?: 0) <= firstVisible }
                ?: categoryNames.firstOrNull() ?: ""
        }
    }

    LaunchedEffect(activeCategory) {
        val index = categoryNames.indexOf(activeCategory)
        if (index >= 0) chipRowState.animateScrollToItem(index)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = stringResource(R.string.nav_menu),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.menu_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(
            state = chipRowState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            items(categoryNames) { name ->
                FilterChip(
                    selected = name == activeCategory,
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(categoryFirstIndex[name] ?: 0)
                        }
                    },
                    label = { Text(name, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
        val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
        if (isScrolled) HorizontalDivider()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categories.forEach { category ->
                val headerKey = "header_${category.title}"
                stickyHeader(key = headerKey) {
                    StickyMenuHeader(listState = listState, headerKey = headerKey) {
                        MenuSectionHeader(category.title)
                    }
                }
                item(key = "card_${category.title}") {
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
private fun MenuEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.nav_menu),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.menu_no_connection),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
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
