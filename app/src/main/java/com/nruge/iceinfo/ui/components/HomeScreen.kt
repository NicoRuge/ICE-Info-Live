package com.nruge.iceinfo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.model.Coach
import com.nruge.iceinfo.model.MenuCategory
import com.nruge.iceinfo.model.TrainStatus
import com.nruge.iceinfo.model.WeatherInfo
import com.nruge.iceinfo.sampleTrainStatus
import com.nruge.iceinfo.sampleWeather
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    status: TrainStatus = sampleTrainStatus,
    weather: WeatherInfo? = sampleWeather,
    isMockMode: Boolean = false,
    demoSpeed: Int = 114,
    showDemoSpeed: Boolean = true,
    reducedMotion: Boolean = false,
    coaches: List<Coach> = emptyList(),
    coachStopName: String = "",
    selectedCoach: Int? = null,
    seatNumber: String = "",
    menuCategories: List<MenuCategory> = emptyList(),
    isMenuLoading: Boolean = false,
    onLoadMenu: () -> Unit = {},
    menuFabClicks: Int = 0,
    onDemoSpeedChange: (Int) -> Unit = {},
    onTargetStopChange: (String?) -> Unit = {},
    onCoachChange: (Int?) -> Unit = {},
    onSeatChange: (String) -> Unit = {}
) {
    var showRelative by remember { mutableStateOf(false) }
    val referenceTime = if (isMockMode) LocalTime.of(8, 30) else LocalTime.now()
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            showRelative = !showRelative
        }
    }

    // Speisekarte: eingeklappt beim Start, lädt erst beim Aufklappen
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(menuExpanded, status.isConnected) {
        if (menuExpanded && (status.isConnected || isMockMode)) onLoadMenu()
    }

    val listState = rememberLazyListState()

    // Listenindex des Chip-Items in der Menü-Sektion: Anzahl aller davor
    // registrierten Items (inkl. Toggle-Karte). Beim Hinzufügen/Entfernen
    // von Home-Karten mitpflegen!
    val menuChipsIndex = 6 +
        (if (coaches.isNotEmpty()) 1 else 0) +
        (if (status.delayReason.isNotEmpty()) 1 else 0) +
        (if (isMockMode && showDemoSpeed) 1 else 0)
    val menuToggleIndex = menuChipsIndex - 1

    // Speisekarten-FAB (liegt fix über dem Pager, siehe AppNavigation):
    // jeder Klick klappt auf und scrollt sichtbar zur Sektion.
    // handledFabClicks verhindert erneutes Feuern nach Pager-Recreation.
    var handledFabClicks by rememberSaveable { mutableIntStateOf(menuFabClicks) }
    LaunchedEffect(menuFabClicks) {
        if (menuFabClicks > handledFabClicks) {
            handledFabClicks = menuFabClicks
            menuExpanded = true
            if (reducedMotion) listState.scrollToItem(menuToggleIndex)
            else listState.animateScrollToItemSlowly(menuToggleIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "train_header") {
            TrainHeader(status = status, reducedMotion = reducedMotion)
        }
        item(key = "next_stop") {
            NextStopCard(
                status = status,
                showRelative = showRelative,
                referenceTime = referenceTime
            )
        }
        item(key = "stop_selection") {
            StopSelectionCard(
                status = status,
                weather = weather,
                onTargetStopChange = onTargetStopChange,
                showRelative = showRelative,
                referenceTime = referenceTime
            )
        }
        item(key = "seat_row") {
            SeatRow(
                coaches = coaches,
                selectedCoach = selectedCoach,
                seatNumber = seatNumber,
                onCoachChange = onCoachChange,
                onSeatChange = onSeatChange
            )
        }
        if (coaches.isNotEmpty()) {
            item(key = "wagenreihung") {
                WagenreihungCard(coaches = coaches, stopName = coachStopName, selectedCoach = selectedCoach)
            }
        }
        item(key = "connectivity") {
            ConnectivityRow(status = status)
        }
        if (status.delayReason.isNotEmpty()) {
            item(key = "delay_reason") {
                DelayReasonCard(reason = status.delayReason)
            }
        }
        if (isMockMode && showDemoSpeed) {
            item(key = "demo_speed") {
                DemoSpeedCard(demoSpeed = demoSpeed, onDemoSpeedChange = onDemoSpeedChange)
            }
        }

        menuSection(
            categories = menuCategories,
            isLoading = isMenuLoading,
            expanded = menuExpanded,
            onToggleExpanded = { menuExpanded = !menuExpanded },
            listState = listState,
            chipsItemIndex = menuChipsIndex
        )
    }
}

/**
 * Scrollt sichtbar langsam zum Ziel-Item statt zu springen — der Nutzer soll
 * wahrnehmen, dass die Speisekarte Teil derselben Seite ist.
 * (`animateScrollToItem` bietet keine einstellbare Dauer.)
 */
private suspend fun LazyListState.animateScrollToItemSlowly(targetIndex: Int) {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) {
        animateScrollToItem(targetIndex)
        return
    }
    val targetItem = visible.firstOrNull { it.index == targetIndex }
    val distance = if (targetItem != null) {
        targetItem.offset.toFloat()
    } else {
        // Ziel noch nicht komponiert → Distanz über mittlere Item-Höhe schätzen
        val avgItemSize = visible.sumOf { it.size } / visible.size + info.mainAxisItemSpacing
        ((targetIndex - firstVisibleItemIndex) * avgItemSize - firstVisibleItemScrollOffset).toFloat()
    }
    animateScrollBy(distance, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    // Feinkorrektur, falls die Schätzung daneben lag
    animateScrollToItem(targetIndex)
}
