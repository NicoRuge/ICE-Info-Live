package com.nruge.iceinfo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nruge.iceinfo.model.Coach
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TrainHeader(status = status, reducedMotion = reducedMotion)

        NextStopCard(
            status = status,
            showRelative = showRelative,
            referenceTime = referenceTime
        )

        StopSelectionCard(
            status = status,
            weather = weather,
            onTargetStopChange = onTargetStopChange,
            showRelative = showRelative,
            referenceTime = referenceTime
        )

        SeatRow(
            coaches = coaches,
            selectedCoach = selectedCoach,
            seatNumber = seatNumber,
            onCoachChange = onCoachChange,
            onSeatChange = onSeatChange
        )

        if (coaches.isNotEmpty()) {
            WagenreihungCard(coaches = coaches, stopName = coachStopName, selectedCoach = selectedCoach)
        }

        ConnectivityRow(status = status)

        if (status.delayReason.isNotEmpty()) {
            DelayReasonCard(reason = status.delayReason)
        }
        if (isMockMode && showDemoSpeed) {
            DemoSpeedCard(demoSpeed = demoSpeed, onDemoSpeedChange = onDemoSpeedChange)
        }
        Spacer(modifier = Modifier.height(96.dp))
    }
}
