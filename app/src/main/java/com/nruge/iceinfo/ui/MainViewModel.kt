package com.nruge.iceinfo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nruge.iceinfo.DepartureBoardRepository
import com.nruge.iceinfo.JourneyRepository
import com.nruge.iceinfo.MenuRepository
import com.nruge.iceinfo.OsmRepository
import com.nruge.iceinfo.StationFacilitiesRepository
import com.nruge.iceinfo.TrainRepository
import com.nruge.iceinfo.WeatherRepository
import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.sampleConnections
import com.nruge.iceinfo.sampleDepartures
import com.nruge.iceinfo.sampleJourneys
import com.nruge.iceinfo.sampleCoaches
import com.nruge.iceinfo.sampleMenuCategories
import com.nruge.iceinfo.sampleOsmTrackData
import com.nruge.iceinfo.samplePois
import com.nruge.iceinfo.sampleTrainStatus
import com.nruge.iceinfo.sampleWeather
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nruge.iceinfo.model.ConnectingTrain
import com.nruge.iceinfo.model.LiveRecordingState
import com.nruge.iceinfo.model.SavedJourney
import com.nruge.iceinfo.model.TrackPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import com.nruge.iceinfo.util.SettingsManager
import com.nruge.iceinfo.widget.WidgetUpdater
import com.nruge.iceinfo.WagenreihungRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _trainStatus: MutableStateFlow<TrainStatus> = MutableStateFlow(sampleTrainStatus.copy(isConnected = false))
    val trainStatus: StateFlow<TrainStatus> = _trainStatus.asStateFlow()

    private val _pois: MutableStateFlow<List<PoiItem>> = MutableStateFlow<List<PoiItem>>(emptyList())
    val pois: StateFlow<List<PoiItem>> = _pois.asStateFlow()

    private val _isMockMode: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isMockMode: StateFlow<Boolean> = _isMockMode.asStateFlow()

    private val _demoSpeed: MutableStateFlow<Int> = MutableStateFlow(SettingsManager.getDemoSpeed(application))
    val demoSpeed: StateFlow<Int> = _demoSpeed.asStateFlow()

    private val _reducedMotion: MutableStateFlow<Boolean> = MutableStateFlow(SettingsManager.isReducedMotion(application))
    val reducedMotion: StateFlow<Boolean> = _reducedMotion.asStateFlow()

    private val _isChecking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isWIFIonICE: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isWIFIonICE: StateFlow<Boolean> = _isWIFIonICE.asStateFlow()

    // Debug: Zug-WLAN-Erkennung erzwingen (nur für die laufende Session, nicht persistiert)
    private var lastRealWifiOnIce = false
    private val _simulateWifiOnIce = MutableStateFlow(false)
    val simulateWifiOnIce: StateFlow<Boolean> = _simulateWifiOnIce.asStateFlow()

    private var pollingJob: Job? = null
    private val appInForeground = MutableStateFlow(true)

    private val _connections: MutableStateFlow<List<ConnectingTrain>> = MutableStateFlow<List<ConnectingTrain>>(emptyList())
    val connections: StateFlow<List<ConnectingTrain>> = _connections.asStateFlow()

    private val _departures: MutableStateFlow<List<Departure>> = MutableStateFlow<List<Departure>>(emptyList())
    val departures: StateFlow<List<Departure>> = _departures.asStateFlow()

    private val _serviceStation = MutableStateFlow<StationInfo?>(null)
    val serviceStation: StateFlow<StationInfo?> = _serviceStation.asStateFlow()

    private val _stationSearchResults = MutableStateFlow<List<StationSearchResult>>(emptyList())
    val stationSearchResults: StateFlow<List<StationSearchResult>> = _stationSearchResults.asStateFlow()

    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    private val _osmData = MutableStateFlow(OsmTrackData(isLoading = false))
    val osmData: StateFlow<OsmTrackData> = _osmData.asStateFlow()

    private var lastWeatherEva = ""
    private var lastWeatherFetchMs = 0L
    private var lastOsmLat = 0.0
    private var lastOsmLon = 0.0
    private var lastConnectionsFetchMs = 0L

    private val _journeys = MutableStateFlow<List<SavedJourney>>(emptyList())
    val journeys: StateFlow<List<SavedJourney>> = _journeys.asStateFlow()

    private val _showRecordingConsent = MutableStateFlow(false)
    val showRecordingConsent: StateFlow<Boolean> = _showRecordingConsent.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()
    private var lastConnectedMs = 0L
    private val RECONNECTING_WINDOW_MS = 30_000L

    private val _liveRecording = MutableStateFlow<LiveRecordingState?>(null)
    val liveRecording: StateFlow<LiveRecordingState?> = _liveRecording.asStateFlow()

    private val _menuCategories = MutableStateFlow<List<com.nruge.iceinfo.model.MenuCategory>>(emptyList())
    val menuCategories: StateFlow<List<com.nruge.iceinfo.model.MenuCategory>> = _menuCategories.asStateFlow()

    private val _isMenuLoading = MutableStateFlow(false)
    val isMenuLoading: StateFlow<Boolean> = _isMenuLoading.asStateFlow()

    private val _activeOrder = MutableStateFlow<com.nruge.iceinfo.model.ActiveOrder?>(null)
    val activeOrder: StateFlow<com.nruge.iceinfo.model.ActiveOrder?> = _activeOrder.asStateFlow()

    private val _orderError = MutableStateFlow<String?>(null)
    val orderError: StateFlow<String?> = _orderError.asStateFlow()

    private var orderPollingJob: Job? = null

    private var menuFetchedForTrain: String? = null
    private var wagenreihungFetchedForTrain: String? = null
    private var directionChangesFetchedForTrain: String? = null
    private var directionChanges: Map<String, Boolean> = emptyMap()

    private val _coaches = MutableStateFlow<List<com.nruge.iceinfo.model.Coach>>(emptyList())
    val coaches: StateFlow<List<com.nruge.iceinfo.model.Coach>> = _coaches.asStateFlow()

    // Bahnhof, für den die aktuell angezeigten Sektoren gelten (nächster Halt bzw. Ausstieg)
    private val _coachStopName = MutableStateFlow("")
    val coachStopName: StateFlow<String> = _coachStopName.asStateFlow()

    private val _selectedCoach = MutableStateFlow<Int?>(SettingsManager.getCoachNumber(application))
    val selectedCoach: StateFlow<Int?> = _selectedCoach.asStateFlow()

    private val _seatNumber = MutableStateFlow(SettingsManager.getSeatNumber(application))
    val seatNumber: StateFlow<String> = _seatNumber.asStateFlow()

    // Interner Aufzeichnungszustand
    private inner class ActiveRecording(
        val id: String = UUID.randomUUID().toString(),
        val trainType: String,
        val trainNumber: String,
        val originStation: String,
        val destinationEvaNr: String,
        val destinationStation: String,
        val date: String,
        val departureTime: String,
        val originDistanceFromStart: Int,
        val destinationDistanceFromStart: Int,
        val stopsCount: Int,
        val recordGps: Boolean = false,
        val startMs: Long = System.currentTimeMillis(),
        val speedSamples: MutableList<Int> = mutableListOf(),
        val trackPoints: MutableList<TrackPoint> = mutableListOf(),
        var topSpeedKmh: Int = 0,
        // Letzter bekannter Stand für Offline-Abschluss ohne Zug-WLAN
        val destinationScheduledArrivalMs: Long = 0L,
        var lastDelayMinutes: Int = 0,
        var lastPassedCount: Int = 0,
        var lastDistanceFromStart: Int = 0,
        // Kompletter Fahrtverlauf, bei jedem Poll aktualisiert (für Offline-Abschluss)
        var lastStops: List<com.nruge.iceinfo.model.JourneyStop> = emptyList(),
        // Automatisch bei Aufzeichnungsstart erfasst
        val tzn: String = "",
        val series: String = "",
        val seat: String = ""
    ) {
        fun toPending() = PendingRecording(
            id = id,
            trainType = trainType,
            trainNumber = trainNumber,
            originStation = originStation,
            destinationEvaNr = destinationEvaNr,
            destinationStation = destinationStation,
            date = date,
            departureTime = departureTime,
            originDistanceFromStart = originDistanceFromStart,
            destinationDistanceFromStart = destinationDistanceFromStart,
            stopsCount = stopsCount,
            recordGps = recordGps,
            startMs = startMs,
            speedSamples = speedSamples.toList(),
            trackPoints = trackPoints.toList(),
            topSpeedKmh = topSpeedKmh,
            destinationScheduledArrivalMs = destinationScheduledArrivalMs,
            lastDelayMinutes = lastDelayMinutes,
            lastPassedCount = lastPassedCount,
            lastDistanceFromStart = lastDistanceFromStart,
            stops = lastStops,
            tzn = tzn,
            series = series,
            seat = seat
        )
    }

    private var activeRecording: ActiveRecording? = null
    private var wasConnected = false
    private var lastCheckpointMs = 0L
    // Merkt sich, ob die laufende Aufzeichnung den Foreground-Service selbst gestartet
    // hat. Nur dann wird er beim Beenden wieder gestoppt — eine vom Nutzer manuell
    // aktivierte Live-Notification bleibt unberührt.
    private var recordingStartedService = false

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = JourneyRepository.loadJourneys(getApplication())
            _journeys.value = stored.ifEmpty { sampleJourneys }
            // Unterbrochene Aufzeichnung wiederherstellen (App wurde beendet o.ä.)
            JourneyRepository.loadPendingRecording(getApplication())?.let {
                restorePendingRecording(it)
            }
        }
        val initialTarget = SettingsManager.getTargetStopEva(application)
        if (_isMockMode.value) {
            _trainStatus.value = sampleTrainStatus.copy(
                isConnected = true,
                targetStopEva = initialTarget
            )
            _connections.value = sampleConnections
            _departures.value = sampleDepartures
            _pois.value = samplePois
            _weather.value = sampleWeather
            _osmData.value = sampleOsmTrackData
            _coaches.value = sampleCoaches
            _coachStopName.value = relevantBoardStop(_trainStatus.value)?.name.orEmpty()
            updateWidget(_trainStatus.value)
        } else {
            startPolling()
        }
    }

    fun setCoach(coach: Int?) {
        _selectedCoach.value = coach
        SettingsManager.setCoachNumber(getApplication(), coach)
    }

    fun setSeat(seat: String) {
        _seatNumber.value = seat
        SettingsManager.setSeatNumber(getApplication(), seat)
    }

    fun setTargetStop(eva: String?) {
        SettingsManager.setTargetStopEva(getApplication(), eva)
        _trainStatus.value = _trainStatus.value.copy(targetStopEva = eva)
        updateWidget(_trainStatus.value)

        lastConnectionsFetchMs = 0L
        viewModelScope.launch {
            val status = _trainStatus.value
            val updatedStatus = status.copy(targetStopEva = eva)

            // Verbindungen + Wetter für neuen Zielbahnhof
            val boardStop = relevantBoardStop(updatedStatus)
            val connections = TrainRepository.fetchConnections(
                boardStop?.evaNr ?: status.nextStopEva,
                boardStop?.effectiveArrivalMs ?: 0L
            )
            val departures = boardStop?.let { fetchDeparturesForStop(it) } ?: emptyList()
            _departures.value = departures
            _connections.value = enrichConnectionDestinations(connections, departures)
            refreshWeatherIfNeeded(updatedStatus)

            // Wagenreihung neu abfragen: Sektoren gelten für Ausstieg bzw. nächsten Halt
            val queryStop = relevantBoardStop(updatedStatus)
            val trainKey = "${status.trainType}${status.trainNumber}_${queryStop?.evaNr.orEmpty()}"
            if (wagenreihungFetchedForTrain != trainKey) {
                wagenreihungFetchedForTrain = trainKey
                val wagenreihung = WagenreihungRepository.fetch(status, queryStop)
                if (wagenreihung.isNotEmpty()) {
                    _coaches.value = wagenreihung
                    _coachStopName.value = queryStop?.name.orEmpty()
                }
            }
        }
        
        if (com.nruge.iceinfo.IceNotificationService.isRunning.value) {
            val intent = android.content.Intent(getApplication(), com.nruge.iceinfo.IceNotificationService::class.java).apply {
                action = com.nruge.iceinfo.IceNotificationService.ACTION_UPDATE_TARGET
                putExtra(com.nruge.iceinfo.IceNotificationService.EXTRA_TARGET_EVA, eva)
            }
            getApplication<android.app.Application>().startService(intent)
        }
    }

    fun setMockMode(enabled: Boolean) {
        _isMockMode.value = enabled
        val currentTarget = SettingsManager.getTargetStopEva(getApplication())
        if (enabled) {
            stopPolling()
            val status = sampleTrainStatus.copy(
                isConnected = true,
                speed = _demoSpeed.value,
                targetStopEva = currentTarget,
                nextConnectivity = sampleTrainStatus.nextConnectivity,
                connectivityRemainingSeconds = sampleTrainStatus.connectivityRemainingSeconds
            )
            _trainStatus.value = status
            _connections.value = sampleConnections
            _departures.value = sampleDepartures
            _pois.value = samplePois
            _weather.value = sampleWeather
            _osmData.value = sampleOsmTrackData
            _menuCategories.value = sampleMenuCategories
            _coaches.value = sampleCoaches
            _coachStopName.value = relevantBoardStop(status)?.name.orEmpty()
            menuFetchedForTrain = "${sampleTrainStatus.trainType}${sampleTrainStatus.trainNumber}"
            updateWidget(status)
        } else {
            _trainStatus.value = _trainStatus.value.copy(isConnected = false, targetStopEva = currentTarget)
            _connections.value = emptyList()
            _departures.value = emptyList()
            _pois.value = emptyList()
            _weather.value = null
            _osmData.value = OsmTrackData()
            _menuCategories.value = emptyList()
            menuFetchedForTrain = null
            lastWeatherEva = ""
            lastOsmLat = 0.0
            lastOsmLon = 0.0
            startPolling()
        }
    }

    fun setReducedMotion(enabled: Boolean) {
        _reducedMotion.value = enabled
        SettingsManager.setReducedMotion(getApplication(), enabled)
    }

    fun setDemoSpeed(speed: Int) {
        _demoSpeed.value = speed
        SettingsManager.setDemoSpeed(getApplication(), speed)
        if (_isMockMode.value) {
            val status = _trainStatus.value.copy(speed = speed)
            _trainStatus.value = status
            updateWidget(status)
        }
    }

    /**
     * API-Ergebnisse der Richtungswechsel-Erkennung über die Heuristik-Flags legen:
     * Wo die Wagenreihungs-API eine Aussage hat, gewinnt sie (true wie false);
     * für Halte ohne Eintrag bleibt das Heuristik-Flag aus dem Repository stehen.
     */
    private fun applyDirectionChanges(status: TrainStatus): TrainStatus {
        if (directionChanges.isEmpty()) return status
        return status.copy(stops = status.stops.map { stop ->
            val api = directionChanges[stop.evaNr] ?: return@map stop
            if (stop.isCancelled) stop else stop.copy(directionChange = api)
        })
    }

    private fun updateWidget(status: TrainStatus) {
        val targetEva = SettingsManager.getTargetStopEva(getApplication())
        val targetStop = status.stops.find { it.evaNr == targetEva }
        WidgetUpdater.update(
            getApplication(),
            status,
            _isMockMode.value,
            targetStop?.name
        )
    }

    fun updateWifiStatus(isOnICE: Boolean) {
        lastRealWifiOnIce = isOnICE
        _isWIFIonICE.value = isOnICE || _simulateWifiOnIce.value
    }

    fun setSimulateWifiOnIce(enabled: Boolean) {
        _simulateWifiOnIce.value = enabled
        _isWIFIonICE.value = lastRealWifiOnIce || enabled
    }

    fun retryConnection() {
        _isMockMode.value = false
        _isChecking.value = true
        viewModelScope.launch {
            val status = applyDirectionChanges(TrainRepository.fetchTrainStatus())
            _trainStatus.value = status
            _pois.value = TrainRepository.fetchPois(status.latitude, status.longitude)
            _isChecking.value = false
            if (status.isConnected) {
                startPolling()
            }
            updateWidget(status)
        }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            var consecutiveFailures = 0
            while (isActive) {
                // Im Hintergrund nicht pollen — außer wenn eine Aufzeichnung läuft
                if (!appInForeground.value && activeRecording == null) {
                    appInForeground.first { it }
                }

                if (!_isMockMode.value) {
                    val status = TrainRepository.fetchTrainStatus()
                    val currentTarget = SettingsManager.getTargetStopEva(getApplication())
                    val updatedStatus = applyDirectionChanges(status.copy(targetStopEva = currentTarget))

                    // Nur nach 2 aufeinanderfolgenden Fehlern auf "getrennt" wechseln –
                    // verhindert kurzen NoWifi-Flash nach einzelnem Hintergrund-Timeout.
                    if (status.isConnected) {
                        consecutiveFailures = 0
                        _trainStatus.value = updatedStatus
                    } else {
                        consecutiveFailures++
                        if (consecutiveFailures >= 2 || !wasConnected) {
                            _trainStatus.value = updatedStatus
                        }
                    }

                    if (status.isConnected) {
                        _pois.value = TrainRepository.fetchPois(status.latitude, status.longitude)
                        refreshOsmDataIfNeeded(status.latitude, status.longitude)
                        refreshWeatherIfNeeded(updatedStatus)
                    }
                    // Fahrtrichtungswechsel: Wagen-Orientierungen einmalig pro Fahrt
                    // für alle Halte abfragen (Repository cached, kein API-Spam)
                    val journeyKey = "${status.trainType}${status.trainNumber}_${status.stops.firstOrNull()?.evaNr.orEmpty()}"
                    if (status.isConnected && status.stops.isNotEmpty() &&
                        directionChangesFetchedForTrain != journeyKey
                    ) {
                        directionChangesFetchedForTrain = journeyKey
                        directionChanges = emptyMap() // Ergebnisse der vorigen Fahrt nicht weiterverwenden
                        viewModelScope.launch {
                            directionChanges = WagenreihungRepository.fetchDirectionChanges(updatedStatus)
                            if (directionChanges.isNotEmpty()) {
                                _trainStatus.value = applyDirectionChanges(_trainStatus.value)
                            }
                        }
                    }

                    // Ohne gewählten Ausstieg: nächster Halt → Sektoren aktualisieren
                    // sich nach jedem Stopp automatisch für den kommenden Bahnhof.
                    val queryStop = relevantBoardStop(updatedStatus)
                    // Key enthält Abfrage-EVA → neu abfragen wenn Ziel ODER nächster Halt wechselt
                    val trainKey = "${status.trainType}${status.trainNumber}_${queryStop?.evaNr.orEmpty()}"
                    if (status.isConnected && wagenreihungFetchedForTrain != trainKey) {
                        wagenreihungFetchedForTrain = trainKey
                        val wagenreihung = WagenreihungRepository.fetch(status, queryStop)
                        _coaches.value = wagenreihung.ifEmpty {
                            TrainRepository.fetchCoaches()
                        }
                        _coachStopName.value = if (wagenreihung.isNotEmpty()) queryStop?.name.orEmpty() else ""
                    }

                    // Verbindungsstatus tracken
                    if (status.isConnected) {
                        lastConnectedMs = System.currentTimeMillis()
                        _isReconnecting.value = false
                    } else if (_isWIFIonICE.value && lastConnectedMs > 0) {
                        val elapsed = System.currentTimeMillis() - lastConnectedMs
                        _isReconnecting.value = elapsed < RECONNECTING_WINDOW_MS
                    } else {
                        _isReconnecting.value = false
                    }

                    // Laufende Aufzeichnung bei WLAN-Verlust absichern
                    if (activeRecording != null && !status.isConnected) {
                        if (wasConnected) {
                            // Verbindung gerade verloren → Teilstrecke sofort sichern
                            maybeCheckpoint(force = true)
                        }
                        val rec = activeRecording
                        if (rec != null && journeyLikelyEnded(rec.destinationScheduledArrivalMs, rec.lastDelayMinutes)) {
                            // WLAN verlassen + Ziel lt. Reiseplan erreicht → Fahrt automatisch speichern
                            finishRecordingOffline()
                        }
                    }

                    // Neue Fahrt erkennen
                    if (!wasConnected && status.isConnected) {
                        checkForNewJourney(status)
                    }
                    wasConnected = status.isConnected

                    // Aufzeichnung aktualisieren
                    if (status.isConnected) {
                        updateRecording(status)
                    }

                    val now = System.currentTimeMillis()
                    if (status.isConnected && now - lastConnectionsFetchMs > 30_000L) {
                        lastConnectionsFetchMs = now
                        val boardStop = relevantBoardStop(updatedStatus)
                        val connections = TrainRepository.fetchConnections(
                            boardStop?.evaNr ?: status.nextStopEva,
                            boardStop?.effectiveArrivalMs ?: 0L
                        )
                        val departures = boardStop?.let { fetchDeparturesForStop(it) } ?: emptyList()
                        _departures.value = departures
                        _connections.value = enrichConnectionDestinations(connections, departures)
                    }

                    updateWidget(updatedStatus)
                }
                delay(5000)
            }
        }
    }

    private fun checkForNewJourney(status: TrainStatus) {
        if (status.trainNumber.isBlank()) return
        val date = LocalDate.now().format(dateFormatter)
        val journeyKey = "${status.trainType}${status.trainNumber}_$date"
        val lastKey = SettingsManager.getLastJourneyKey(getApplication())
        if (journeyKey != lastKey) {
            SettingsManager.setLastJourneyKey(getApplication(), journeyKey)
            _showRecordingConsent.value = true
        }
    }

    fun requestRecording() {
        if (!_trainStatus.value.isConnected) return
        _showRecordingConsent.value = true
    }

    fun startRecording(recordGps: Boolean = false) {
        // Dialog bewusst NICHT schließen — er bleibt offen, damit der Nutzer im selben
        // Dialog noch über einen Träwelling-Check-in entscheiden kann. Das Schließen
        // übernimmt declineRecording() (Schließen-Button / Scrim).
        val status = _trainStatus.value
        if (!status.isConnected) return
        if (_isRecording.value) return
        val targetEva = status.targetStopEva
        val destinationStop = targetEva?.let { eva -> status.stops.find { it.evaNr == eva && !it.passed } }
            ?: status.stops.lastOrNull()
        val originStop = status.stops.lastOrNull { it.passed }
            ?: status.stops.firstOrNull()
        _isRecording.value = true
        ensureServiceForRecording()
        val rec = ActiveRecording(
            trainType = status.trainType,
            trainNumber = status.trainNumber,
            originStation = originStop?.name ?: "Unbekannt",
            destinationEvaNr = destinationStop?.evaNr ?: "",
            destinationStation = destinationStop?.name ?: status.destination,
            date = LocalDate.now().format(dateFormatter),
            departureTime = originStop?.actualDeparture?.ifEmpty { originStop.scheduledDeparture } ?: "",
            originDistanceFromStart = originStop?.distanceFromStart ?: 0,
            destinationDistanceFromStart = destinationStop?.distanceFromStart ?: 0,
            stopsCount = status.stops.count { !it.passed && !it.isCancelled },
            recordGps = recordGps,
            destinationScheduledArrivalMs = destinationStop?.scheduledArrivalMs ?: 0L,
            lastDelayMinutes = destinationStop?.delayMinutes ?: 0,
            lastDistanceFromStart = originStop?.distanceFromStart ?: 0,
            tzn = status.tzn,
            series = status.series,
            // Sitzplatz aus der Wagen-/Platzwahl übernehmen, falls gesetzt
            seat = listOfNotNull(
                _selectedCoach.value?.let { "Wg. $it" },
                _seatNumber.value.takeIf { it.isNotBlank() }?.let { "Pl. $it" }
            ).joinToString(", ")
        )
        activeRecording = rec
        maybeCheckpoint(force = true)
        _liveRecording.value = LiveRecordingState(
            trainType = rec.trainType,
            trainNumber = rec.trainNumber,
            originStation = rec.originStation,
            destinationStation = rec.destinationStation,
            date = rec.date,
            departureTime = rec.departureTime,
            startMs = rec.startMs,
            currentSpeedKmh = status.speed,
            topSpeedKmh = 0,
            sampleCount = 0,
            trackPointCount = 0,
            recordGps = rec.recordGps
        )
    }

    fun declineRecording() {
        _showRecordingConsent.value = false
    }

    private fun updateRecording(status: TrainStatus) {
        val rec = activeRecording ?: return
        // Speed tracken
        if (status.speed > rec.topSpeedKmh) rec.topSpeedKmh = status.speed
        rec.speedSamples.add(status.speed)
        // GPS-Spur aufzeichnen. Im Stand (0 km/h) nur den ersten Punkt speichern,
        // sonst bläht jeder Halt die Spur mit identischen Punkten auf.
        val lastPoint = rec.trackPoints.lastOrNull()
        val standingStill = status.speed == 0 && lastPoint != null && lastPoint.speedKmh == 0
        if (rec.recordGps && status.latitude != 0.0 && status.longitude != 0.0 && !standingStill) {
            val secondsFromStart = ((System.currentTimeMillis() - rec.startMs) / 1000L).toInt()
            rec.trackPoints.add(
                TrackPoint(
                    lat = status.latitude,
                    lon = status.longitude,
                    speedKmh = status.speed,
                    secondsFromStart = secondsFromStart
                ).rounded()
            )
        }
        // Live-State aktualisieren
        _liveRecording.value = _liveRecording.value?.copy(
            currentSpeedKmh = status.speed,
            topSpeedKmh = rec.topSpeedKmh,
            sampleCount = rec.speedSamples.size,
            trackPointCount = rec.trackPoints.size
        )
        // Letzten bekannten Stand für einen möglichen Offline-Abschluss mitführen
        val destinationStop = status.stops.find { it.evaNr == rec.destinationEvaNr }
        rec.lastDelayMinutes = destinationStop?.delayMinutes ?: status.delayMinutes
        rec.lastPassedCount = status.stops.count { it.passed }
        rec.lastDistanceFromStart = status.actualPosition
        // Kompletten Fahrtverlauf mitführen (Zusatz-/Ausfallhalte, aktuelle Verspätungen)
        if (status.stops.isNotEmpty()) rec.lastStops = captureStops(status)
        maybeCheckpoint()
        // Prüfen ob Ziel-Halt erreicht
        if (destinationStop?.passed == true) {
            finishRecording(status, destinationStop)
        }
    }

    private data class FinalDelayInfo(
        val station: String,
        val delayMinutes: Int,
        val isPrognosis: Boolean
    )

    /**
     * Ermittelt die Verspätung am Endbahnhof des Zuges relativ zum Ausstiegshalt
     * des Nutzers ([exitEva]/[exitDelay]). Ist der Ausstieg zugleich der Endbahnhof,
     * ist der Wert die tatsächliche Ankunftsverspätung; sonst die aktuelle Prognose.
     */
    /**
     * Kompletter Fahrtverlauf des Zuges als Halteliste, Stand des aktuellen Status.
     * Wird während der Fahrt bei jedem Poll neu erfasst (Zusatz-/Ausfall-Halte,
     * aktuelle Verspätungen) und im Pending-Snapshot mitgeführt. Nach dem Ausstieg
     * friert der zuletzt erfasste Stand ein.
     */
    private fun captureStops(status: TrainStatus, exitEva: String = ""): List<com.nruge.iceinfo.model.JourneyStop> {
        val exitIdx = if (exitEva.isBlank()) -1 else status.stops.indexOfFirst { it.evaNr == exitEva }
        return status.stops.mapIndexed { i, s ->
            com.nruge.iceinfo.model.JourneyStop(
                name = s.name,
                time = s.scheduledArrival.ifEmpty { s.scheduledDeparture },
                delayMinutes = s.delayMinutes,
                cancelled = s.isCancelled,
                additional = s.isAdditional,
                prognosis = exitIdx >= 0 && i > exitIdx
            )
        }
    }

    /** Markiert Halte hinter dem Ausstieg (per Name) als Prognose — für den Offline-Abschluss. */
    private fun markPrognosisAfter(
        stops: List<com.nruge.iceinfo.model.JourneyStop>,
        exitName: String
    ): List<com.nruge.iceinfo.model.JourneyStop> {
        val idx = stops.indexOfLast { it.name == exitName }
        return if (idx < 0) stops
        else stops.mapIndexed { i, s -> if (i > idx) s.copy(prognosis = true) else s }
    }

    private fun computeFinalDelay(status: TrainStatus, exitEva: String, exitDelay: Int): FinalDelayInfo {
        val finalStop = status.stops.lastOrNull { !it.isCancelled }
        val isFinal = finalStop != null && finalStop.evaNr.isNotBlank() && finalStop.evaNr == exitEva
        return FinalDelayInfo(
            station = finalStop?.name ?: status.destination,
            delayMinutes = if (isFinal) exitDelay else (finalStop?.delayMinutes ?: exitDelay),
            isPrognosis = !isFinal
        )
    }

    private fun finishRecording(status: TrainStatus, destinationStop: com.nruge.iceinfo.model.TrainStop) {
        val rec = activeRecording ?: return
        stopRecordingState()
        val durationMinutes = ((System.currentTimeMillis() - rec.startMs) / 60_000L).toInt()
        val avgSpeed = if (rec.speedSamples.isNotEmpty()) rec.speedSamples.average().toInt() else 0
        val distanceKm = (destinationStop.distanceFromStart - rec.originDistanceFromStart) / 1000
        val arrivalTime = destinationStop.actualArrival.ifEmpty { destinationStop.scheduledArrival }
        val finalDelay = computeFinalDelay(status, destinationStop.evaNr, destinationStop.delayMinutes)
        val journey = SavedJourney(
            id = rec.id,
            trainType = rec.trainType,
            trainNumber = rec.trainNumber,
            originStation = rec.originStation,
            destinationStation = rec.destinationStation,
            date = rec.date,
            departureTime = rec.departureTime,
            arrivalTime = arrivalTime,
            delayMinutes = destinationStop.delayMinutes,
            distanceKm = distanceKm,
            topSpeedKmh = rec.topSpeedKmh,
            avgSpeedKmh = avgSpeed,
            durationMinutes = durationMinutes,
            stopsCount = rec.stopsCount,
            recordedGps = rec.recordGps,
            trackPoints = rec.trackPoints.toList(),
            tzn = rec.tzn,
            series = rec.series,
            seat = rec.seat,
            finalStation = finalDelay.station,
            finalDelayMinutes = finalDelay.delayMinutes,
            finalDelayIsPrognosis = finalDelay.isPrognosis,
            stops = captureStops(status, destinationStop.evaNr)
        )
        persistFinishedJourney(journey)
    }

    fun cancelRecording() {
        stopRecordingState()
        viewModelScope.launch { JourneyRepository.clearPendingRecording(getApplication()) }
    }

    fun saveRecordingNow() {
        val rec = activeRecording ?: return
        val cachedStatus = _trainStatus.value
        stopRecordingState()

        // Ohne Zug-WLAN keine verlässlichen Live-Daten → letzten bekannten Stand nutzen
        if (!cachedStatus.isConnected) {
            persistFinishedJourney(buildOfflineJourney(rec.toPending()))
            return
        }

        viewModelScope.launch {
            // _trainStatus.value kann bis zu einem Poll-Zyklus veraltet sein: Der
            // Poll-Loop übernimmt einen einzelnen fehlgeschlagenen Fetch NICHT sofort
            // als "getrennt" (Flackerschutz), sondern hält den letzten erfolgreichen
            // Snapshot. Genau beim Einfahren in den Bahnhof (Türen, WLAN-Handover)
            // fällt so oft ein Poll aus — der Cache zeigt dann noch den Zustand kurz
            // VOR Erreichen des Halts (actualPosition/actualArrival noch nicht
            // gesetzt), wodurch reachedNext unten fälschlich false wird und der
            // vorherige statt des aktuellen Bahnhofs als Ausstieg gewertet wird.
            // Deshalb hier einen frischen Fetch statt des Caches verwenden.
            val fresh = TrainRepository.fetchTrainStatus()
            val status = if (fresh.isConnected)
                applyDirectionChanges(fresh.copy(targetStopEva = cachedStatus.targetStopEva))
            else cachedStatus

            val now = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val lastPassedStop = status.stops.lastOrNull { it.passed }
            val nextStop = status.stops.firstOrNull { it.isNext }
            // Beim manuellen Speichern steht der Zug in aller Regel im Ausstiegsbahnhof.
            // Die ICE-API markiert einen Halt aber erst als "passed", wenn der Zug dort
            // wieder abfährt (an Endbahnhöfen nie) — dann liefert lastPassed noch die
            // Station davor. Hat der Zug den nächsten Halt bereits erreicht (Position dort
            // erreicht ODER Zug hält mit gesetzter Ist-Ankunft), gilt DIESER als Ausstieg.
            val reachedNext = nextStop != null && (
                nextStop.distanceFromStart in 1..status.actualPosition ||
                (status.speed == 0 && nextStop.actualArrival.isNotEmpty())
            )
            val exitStop = if (reachedNext) nextStop else lastPassedStop
            val currentDistanceFromStart = exitStop?.distanceFromStart
                ?: status.stops.firstOrNull()?.distanceFromStart ?: 0
            val distanceKm = (currentDistanceFromStart - rec.originDistanceFromStart) / 1000
            val durationMinutes = ((System.currentTimeMillis() - rec.startMs) / 60_000L).toInt()
            val avgSpeed = if (rec.speedSamples.isNotEmpty()) rec.speedSamples.average().toInt() else 0
            val finalDelay = computeFinalDelay(
                status,
                exitStop?.evaNr ?: "",
                exitStop?.delayMinutes ?: 0
            )
            val arrivalTime = if (reachedNext && exitStop != null)
                exitStop.actualArrival.ifEmpty { exitStop.scheduledArrival }.ifEmpty { now }
            else now
            // Erreichter Ausstiegshalt zählt als absolvierter Halt mit.
            val passedCount = status.stops.count { it.passed }
            val journey = SavedJourney(
                id = rec.id,
                trainType = rec.trainType,
                trainNumber = rec.trainNumber,
                originStation = rec.originStation,
                destinationStation = exitStop?.name ?: rec.destinationStation,
                date = rec.date,
                departureTime = rec.departureTime,
                arrivalTime = arrivalTime,
                delayMinutes = exitStop?.delayMinutes ?: 0,
                distanceKm = distanceKm,
                topSpeedKmh = rec.topSpeedKmh,
                avgSpeedKmh = avgSpeed,
                durationMinutes = durationMinutes,
                stopsCount = if (reachedNext) passedCount + 1 else passedCount,
                recordedGps = rec.recordGps,
                trackPoints = rec.trackPoints.toList(),
                tzn = rec.tzn,
                series = rec.series,
                seat = rec.seat,
                finalStation = finalDelay.station,
                finalDelayMinutes = finalDelay.delayMinutes,
                finalDelayIsPrognosis = finalDelay.isPrognosis,
                stops = captureStops(status, exitStop?.evaNr ?: "")
            )
            persistFinishedJourney(journey)
        }
    }

    /**
     * Startet den Foreground-Service für die Dauer der Aufzeichnung. Er hält den
     * Prozess auf Vordergrund-Priorität, damit ihn das System im Standby/Doze nicht
     * einfriert und die Poll-Schleife (und damit die GPS-Aufzeichnung) weiterläuft.
     * Läuft der Service bereits (vom Nutzer aktivierte Live-Notification), bleibt er
     * unverändert und wird beim Beenden der Aufzeichnung nicht gestoppt.
     */
    private fun ensureServiceForRecording() {
        if (com.nruge.iceinfo.IceNotificationService.isRunning.value) return
        val app = getApplication<android.app.Application>()
        val intent = android.content.Intent(app, com.nruge.iceinfo.IceNotificationService::class.java)
        runCatching { app.startForegroundService(intent) }
            .onSuccess { recordingStartedService = true }
            .onFailure { android.util.Log.e("MainViewModel", "startForegroundService failed: ${it.message}") }
    }

    /** Stoppt den Service wieder — aber nur, wenn ihn die Aufzeichnung selbst gestartet hat. */
    private fun releaseServiceForRecording() {
        if (!recordingStartedService) return
        recordingStartedService = false
        val app = getApplication<android.app.Application>()
        val intent = android.content.Intent(app, com.nruge.iceinfo.IceNotificationService::class.java).apply {
            action = com.nruge.iceinfo.IceNotificationService.ACTION_STOP
        }
        runCatching { app.startService(intent) }
    }

    /** Beendet den Live-Aufzeichnungszustand, ohne zu speichern. */
    private fun stopRecordingState() {
        activeRecording = null
        _isRecording.value = false
        _liveRecording.value = null
        releaseServiceForRecording()
    }

    /** Fahrt speichern und den persistierten Zwischenstand entfernen. */
    private fun persistFinishedJourney(journey: SavedJourney) {
        viewModelScope.launch {
            JourneyRepository.saveJourney(getApplication(), journey)
            JourneyRepository.clearPendingRecording(getApplication())
            _journeys.value = listOf(journey) + _journeys.value
        }
    }

    /** Speichert den Aufzeichnungs-Zwischenstand, gedrosselt auf alle 30 s (force überspringt). */
    private fun maybeCheckpoint(force: Boolean = false) {
        val rec = activeRecording ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastCheckpointMs < 30_000L) return
        lastCheckpointMs = now
        val pending = rec.toPending()
        viewModelScope.launch {
            JourneyRepository.savePendingRecording(getApplication(), pending)
        }
    }

    /** Reise gilt als beendet, wenn die effektive Ankunftszeit am Ziel erreicht ist. */
    private fun journeyLikelyEnded(destinationScheduledArrivalMs: Long, lastDelayMinutes: Int): Boolean {
        if (destinationScheduledArrivalMs <= 0L) return false
        return System.currentTimeMillis() >= destinationScheduledArrivalMs + lastDelayMinutes * 60_000L
    }

    /** Aufzeichnung ohne Zug-WLAN abschließen — mit dem letzten bekannten Stand. */
    private fun finishRecordingOffline() {
        val rec = activeRecording ?: return
        val pending = rec.toPending()
        stopRecordingState()
        persistFinishedJourney(buildOfflineJourney(pending))
    }

    private fun buildOfflineJourney(p: PendingRecording): SavedJourney {
        val nowMs = System.currentTimeMillis()
        val plannedArrivalMs = if (p.destinationScheduledArrivalMs > 0L)
            p.destinationScheduledArrivalMs + p.lastDelayMinutes * 60_000L else 0L
        // Ziel gilt als erreicht, wenn die effektive Ankunftszeit bereits vorbei ist;
        // sonst ist es eine manuell gespeicherte Teilstrecke.
        val reachedDestination = plannedArrivalMs in 1..nowMs
        val endMs = if (reachedDestination) plannedArrivalMs else nowMs
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val arrivalTime = java.time.Instant.ofEpochMilli(endMs)
            .atZone(java.time.ZoneId.systemDefault())
            .format(timeFormatter)
        val distanceMeters = if (reachedDestination)
            p.destinationDistanceFromStart - p.originDistanceFromStart
        else
            p.lastDistanceFromStart - p.originDistanceFromStart
        return SavedJourney(
            id = p.id,
            trainType = p.trainType,
            trainNumber = p.trainNumber,
            originStation = p.originStation,
            destinationStation = p.destinationStation,
            date = p.date,
            departureTime = p.departureTime,
            arrivalTime = arrivalTime,
            delayMinutes = p.lastDelayMinutes,
            distanceKm = (distanceMeters / 1000).coerceAtLeast(0),
            topSpeedKmh = p.topSpeedKmh,
            avgSpeedKmh = if (p.speedSamples.isNotEmpty()) p.speedSamples.average().toInt() else 0,
            durationMinutes = ((endMs - p.startMs) / 60_000L).toInt().coerceAtLeast(0),
            stopsCount = if (reachedDestination) p.stopsCount else p.lastPassedCount,
            recordedGps = p.recordGps,
            trackPoints = p.trackPoints,
            tzn = p.tzn,
            series = p.series,
            seat = p.seat,
            stops = markPrognosisAfter(p.stops, p.destinationStation)
        )
    }

    /**
     * Beim App-Start gefundenen Zwischenstand verwerten: Ist die Reise laut Plan
     * vorbei, wird sie als Fahrt gespeichert; sonst läuft die Aufzeichnung weiter
     * und wird bei erneuter Zug-WLAN-Verbindung nahtlos fortgesetzt.
     */
    private fun restorePendingRecording(p: PendingRecording) {
        if (activeRecording != null) return
        if (journeyLikelyEnded(p.destinationScheduledArrivalMs, p.lastDelayMinutes)) {
            persistFinishedJourney(buildOfflineJourney(p))
            return
        }
        activeRecording = ActiveRecording(
            id = p.id,
            trainType = p.trainType,
            trainNumber = p.trainNumber,
            originStation = p.originStation,
            destinationEvaNr = p.destinationEvaNr,
            destinationStation = p.destinationStation,
            date = p.date,
            departureTime = p.departureTime,
            originDistanceFromStart = p.originDistanceFromStart,
            destinationDistanceFromStart = p.destinationDistanceFromStart,
            stopsCount = p.stopsCount,
            recordGps = p.recordGps,
            startMs = p.startMs,
            speedSamples = p.speedSamples.toMutableList(),
            trackPoints = p.trackPoints.toMutableList(),
            topSpeedKmh = p.topSpeedKmh,
            destinationScheduledArrivalMs = p.destinationScheduledArrivalMs,
            lastDelayMinutes = p.lastDelayMinutes,
            lastPassedCount = p.lastPassedCount,
            lastDistanceFromStart = p.lastDistanceFromStart,
            lastStops = p.stops,
            tzn = p.tzn,
            series = p.series,
            seat = p.seat
        )
        _isRecording.value = true
        ensureServiceForRecording()
        _liveRecording.value = LiveRecordingState(
            trainType = p.trainType,
            trainNumber = p.trainNumber,
            originStation = p.originStation,
            destinationStation = p.destinationStation,
            date = p.date,
            departureTime = p.departureTime,
            startMs = p.startMs,
            currentSpeedKmh = 0,
            topSpeedKmh = p.topSpeedKmh,
            sampleCount = p.speedSamples.size,
            trackPointCount = p.trackPoints.size,
            recordGps = p.recordGps
        )
    }

    fun deleteJourney(id: String) {
        viewModelScope.launch {
            JourneyRepository.deleteJourney(getApplication(), id)
            _journeys.value = _journeys.value.filter { it.id != id }
        }
    }

    fun updateJourney(journey: SavedJourney) {
        viewModelScope.launch {
            JourneyRepository.updateJourney(getApplication(), journey)
            _journeys.value = _journeys.value.map {
                if (it.id == journey.id) journey else it
            }
        }
    }

    /**
     * Schreibt alle gespeicherten Fahrten als JSON in die gewählte Datei.
     * Exportiert bewusst nur den persistierten Bestand — keine Demo-Beispieldaten.
     */
    fun exportJourneys(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                val journeys = JourneyRepository.loadJourneys(getApplication())
                val content = JourneyRepository.encodeJourneys(journeys)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(content.toByteArray())
                    } ?: error("Stream konnte nicht geöffnet werden")
                }
            }.isSuccess
            onResult(ok)
        }
    }

    /**
     * Liest eine Export-Datei und führt die Fahrten mit den vorhandenen zusammen.
     * onResult: Anzahl neu importierter Fahrten, -1 bei ungültiger Datei.
     */
    fun importJourneys(uri: Uri, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val raw = runCatching {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                }
            }.getOrNull()
            val imported = raw?.let { JourneyRepository.decodeJourneys(it) }
            if (imported == null) {
                onResult(-1)
                return@launch
            }
            val (added, merged) = JourneyRepository.importJourneys(getApplication(), imported)
            _journeys.value = merged
            onResult(added)
        }
    }

    fun searchStations(query: String) {
        searchJob?.cancel()
        if (query.length < 4) {
            _stationSearchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _stationSearchResults.value = StationFacilitiesRepository.searchStations(query)
        }
    }

    fun selectServiceStation(result: StationSearchResult) {
        _stationSearchResults.value = emptyList()
        _serviceStation.value = StationInfo(evaNr = result.evaNr, name = result.name, isLoading = true)
        viewModelScope.launch {
            _serviceStation.value = StationFacilitiesRepository.fetchFacilities(result.evaNr, result.name)
        }
    }

    fun loadServiceStationFromTrain(evaNr: String, name: String) {
        _serviceStation.value = StationInfo(evaNr = evaNr, name = name, isLoading = true)
        viewModelScope.launch {
            _serviceStation.value = StationFacilitiesRepository.fetchFacilities(evaNr, name)
        }
    }

    fun fetchMenuIfNeeded() {
        val trainKey = _trainStatus.value.let { "${it.trainType}${it.trainNumber}" }
            .takeIf { it.isNotBlank() } ?: return
        if (menuFetchedForTrain == trainKey && _menuCategories.value.isNotEmpty()) return
        viewModelScope.launch {
            _isMenuLoading.value = true
            val result = MenuRepository.fetchMenu()
            val availabilities = MenuRepository.fetchAvailabilities()
            val categories = applyAvailabilities(result.categories, availabilities)
            _menuCategories.value = categories
            if (categories.isNotEmpty()) menuFetchedForTrain = trainKey
            _isMenuLoading.value = false
        }
    }

    fun refreshMenu() {
        if (_isMockMode.value) return
        viewModelScope.launch {
            _isMenuLoading.value = true
            val result = MenuRepository.fetchMenu()
            val availabilities = MenuRepository.fetchAvailabilities()
            _menuCategories.value = applyAvailabilities(result.categories, availabilities)
            menuFetchedForTrain = _trainStatus.value
                .let { "${it.trainType}${it.trainNumber}" }.takeIf { it.isNotBlank() }
            _isMenuLoading.value = false
        }
    }

    private fun applyAvailabilities(
        categories: List<com.nruge.iceinfo.model.MenuCategory>,
        availabilities: Map<Int, Boolean>
    ): List<com.nruge.iceinfo.model.MenuCategory> {
        if (availabilities.isEmpty()) return categories
        return categories.map { cat ->
            cat.copy(items = cat.items.map { item ->
                availabilities[item.id]?.let { visible -> item.copy(visible = visible) } ?: item
            })
        }
    }

    fun onForeground() {
        appInForeground.value = true
        // Sofortiger Fetch: laufenden Delay unterbrechen und neu starten
        pollingJob?.cancel()
        startPolling()
    }

    fun onBackground() {
        appInForeground.value = false
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }

    private fun refreshOsmDataIfNeeded(lat: Double, lon: Double) {
        if (lat == 0.0 && lon == 0.0) return
        val dLat = kotlin.math.abs(lat - lastOsmLat)
        val dLon = kotlin.math.abs(lon - lastOsmLon)
        // ~3 km threshold before re-querying
        if (lastOsmLat != 0.0 && dLat < 0.027 && dLon < 0.035) return
        lastOsmLat = lat
        lastOsmLon = lon
        viewModelScope.launch {
            _osmData.value = OsmTrackData(isLoading = true)
            _osmData.value = OsmRepository.fetchTrackData(lat, lon)
        }
    }

    private fun relevantBoardStop(status: TrainStatus): TrainStop? {
        val targetEva = status.targetStopEva
        val target = targetEva?.let { eva -> status.stops.find { it.evaNr == eva && !it.passed } }
        return target ?: status.stops.firstOrNull { !it.passed }
    }

    private suspend fun fetchDeparturesForStop(stop: TrainStop): List<Departure> {
        if (stop.evaNr.isBlank() || stop.scheduledArrivalMs <= 0L) return emptyList()
        val arrivalMs = stop.scheduledArrivalMs + stop.delayMinutes * 60_000L
        // Fenster beginnt 5 min vor der Ankunft, damit knapp verpasste Züge
        // auf der Tafel bleiben (Sektion „Verpasst" statt kommentarlos weg).
        return DepartureBoardRepository.fetchDepartures(stop.evaNr, arrivalMs - 5 * 60_000L)
    }

    private fun enrichConnectionDestinations(
        connections: List<ConnectingTrain>,
        departures: List<Departure>
    ): List<ConnectingTrain> {
        if (departures.isEmpty()) return connections
        val destinationByLine = departures.associate { it.line.trim() to it.destination }
        return connections.map { conn ->
            if (conn.destination.isNotBlank()) conn
            else conn.copy(destination = destinationByLine["${conn.trainType} ${conn.trainNumber}"].orEmpty())
        }
    }

    private fun weatherStop(status: TrainStatus): TrainStop? {
        val targetEva = status.targetStopEva
        return if (targetEva != null) {
            status.stops.find { it.evaNr == targetEva && !it.passed }
        } else {
            status.stops.lastOrNull()
        }
    }

    private suspend fun refreshWeatherIfNeeded(status: TrainStatus) {
        val stop = weatherStop(status) ?: return
        val now = System.currentTimeMillis()
        if (stop.evaNr == lastWeatherEva && now - lastWeatherFetchMs < 120_000L) return
        lastWeatherEva = stop.evaNr
        lastWeatherFetchMs = now
        _weather.value = WeatherRepository.fetchWeatherForStation(stop.name)
    }

    // ── Bestellungen ──────────────────────────────────────────────────────────

    fun placeOrder(
        item: com.nruge.iceinfo.model.MenuItem,
        option: com.nruge.iceinfo.model.MenuItemOption? = null
    ) {
        val app = getApplication<Application>()
        val seat   = SettingsManager.getSeatNumber(app).toIntOrNull()
        val coach  = SettingsManager.getCoachNumber(app)
        val exit   = _trainStatus.value.destination

        if (seat == null || coach == null || exit.isBlank()) {
            _orderError.value = "Bitte zuerst Wagen und Platz in den Einstellungen eintragen."
            return
        }

        viewModelScope.launch {
            _orderError.value = null
            try {
                val response = com.nruge.iceinfo.OrderRepository.placeOrder(seat, coach, exit, item, option)
                _activeOrder.value = com.nruge.iceinfo.model.ActiveOrder(response.id, response.status)
                startOrderPolling(response.id)
            } catch (e: Exception) {
                _orderError.value = "Bestellung fehlgeschlagen: ${e.message}"
            }
        }
    }

    private fun startOrderPolling(orderId: String) {
        orderPollingJob?.cancel()
        orderPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                try {
                    val status = com.nruge.iceinfo.OrderRepository.getOrderStatus(orderId)
                    _activeOrder.value = _activeOrder.value?.copy(status = status)
                    if (status.isFinal) break
                } catch (_: Exception) {}
            }
        }
    }

    fun dismissOrder() {
        orderPollingJob?.cancel()
        _activeOrder.value = null
        _orderError.value = null
    }
}
