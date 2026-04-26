package com.nruge.iceinfo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nruge.iceinfo.TrainRepository
import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.sampleTrainStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.nruge.iceinfo.model.ConnectingTrain
import com.nruge.iceinfo.util.SettingsManager
import com.nruge.iceinfo.widget.WidgetUpdater

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _trainStatus: MutableStateFlow<TrainStatus> = MutableStateFlow(sampleTrainStatus.copy(isConnected = false))
    val trainStatus: StateFlow<TrainStatus> = _trainStatus.asStateFlow()

    private val _pois: MutableStateFlow<List<PoiItem>> = MutableStateFlow<List<PoiItem>>(emptyList())
    val pois: StateFlow<List<PoiItem>> = _pois.asStateFlow()

    private val _isMockMode: MutableStateFlow<Boolean> = MutableStateFlow(SettingsManager.isMockMode(application))
    val isMockMode: StateFlow<Boolean> = _isMockMode.asStateFlow()

    private val _demoSpeed: MutableStateFlow<Int> = MutableStateFlow(SettingsManager.getDemoSpeed(application))
    val demoSpeed: StateFlow<Int> = _demoSpeed.asStateFlow()

    private val _isChecking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isWIFIonICE: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isWIFIonICE: StateFlow<Boolean> = _isWIFIonICE.asStateFlow()

    private var pollingJob: Job? = null

    private val _connections: MutableStateFlow<List<ConnectingTrain>> = MutableStateFlow<List<ConnectingTrain>>(emptyList())
    val connections: StateFlow<List<ConnectingTrain>> = _connections.asStateFlow()


    init {
        val initialTarget = SettingsManager.getTargetStopEva(application)
        if (_isMockMode.value) {
            _trainStatus.value = sampleTrainStatus.copy(
                isConnected = true,
                targetStopEva = initialTarget
            )
            updateWidget(_trainStatus.value)
        } else {
            startPolling()
        }
    }

    fun setTargetStop(eva: String?) {
        SettingsManager.setTargetStopEva(getApplication(), eva)
        _trainStatus.value = _trainStatus.value.copy(targetStopEva = eva)
        updateWidget(_trainStatus.value)
        
        // Notify the service about the target change.
        // We use startService (not startForegroundService) so it doesn't 
        // trigger a notification if the service wasn't already running.
        val intent = android.content.Intent(getApplication(), com.nruge.iceinfo.IceNotificationService::class.java).apply {
            action = com.nruge.iceinfo.IceNotificationService.ACTION_UPDATE_TARGET
            putExtra(com.nruge.iceinfo.IceNotificationService.EXTRA_TARGET_EVA, eva)
        }
        getApplication<android.app.Application>().startService(intent)
    }

    fun setMockMode(enabled: Boolean) {
        _isMockMode.value = enabled
        SettingsManager.setMockMode(getApplication(), enabled)
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
            updateWidget(status)
        } else {
            _trainStatus.value = _trainStatus.value.copy(isConnected = false, targetStopEva = currentTarget)
            startPolling()
        }
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
        _isWIFIonICE.value = isOnICE
    }

    fun retryConnection() {
        _isMockMode.value = false
        SettingsManager.setMockMode(getApplication(), false)
        _isChecking.value = true
        viewModelScope.launch {
            val status = TrainRepository.fetchTrainStatus()
            _trainStatus.value = status
            _pois.value = TrainRepository.fetchPois()
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
            while (isActive) {
                if (!_isMockMode.value) {
                    val status = TrainRepository.fetchTrainStatus()
                    val currentTarget = SettingsManager.getTargetStopEva(getApplication())
                    val updatedStatus = status.copy(targetStopEva = currentTarget)
                    _trainStatus.value = updatedStatus
                    _pois.value = TrainRepository.fetchPois()
                    _connections.value = TrainRepository.fetchConnections(
                        status.nextStopEva
                    )
                    updateWidget(updatedStatus)
                }
                delay(3000)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }
}
