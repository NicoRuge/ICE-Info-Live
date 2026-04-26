package com.nruge.iceinfo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nruge.iceinfo.TrainRepository
import com.nruge.iceinfo.model.*
import com.nruge.iceinfo.sampleTrainStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.nruge.iceinfo.model.ConnectingTrain

class MainViewModel : ViewModel() {

    private val _trainStatus = MutableStateFlow(sampleTrainStatus.copy(isConnected = false))
    val trainStatus = _trainStatus.asStateFlow()

    private val _pois = MutableStateFlow<List<PoiItem>>(emptyList())
    val pois = _pois.asStateFlow()

    private val _isMockMode = MutableStateFlow(false)
    val isMockMode = _isMockMode.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking = _isChecking.asStateFlow()

    private val _isWIFIonICE = MutableStateFlow(false)
    val isWIFIonICE = _isWIFIonICE.asStateFlow()

    private var pollingJob: Job? = null

    private val _connections = MutableStateFlow<List<ConnectingTrain>>(emptyList())
    val connections = _connections.asStateFlow()


    init {
        startPolling()
    }

    fun setMockMode(enabled: Boolean) {
        _isMockMode.value = enabled
        if (enabled) {
            stopPolling()
            _trainStatus.value = sampleTrainStatus.copy(isConnected = true)
        } else {
            _trainStatus.value = sampleTrainStatus.copy(isConnected = false)
            startPolling()
        }
    }

    fun updateWifiStatus(isOnICE: Boolean) {
        _isWIFIonICE.value = isOnICE
    }

    fun retryConnection() {
        _isMockMode.value = false
        _isChecking.value = true
        viewModelScope.launch {
            val status = TrainRepository.fetchTrainStatus()
            _trainStatus.value = status
            _pois.value = TrainRepository.fetchPois()
            _isChecking.value = false
            if (status.isConnected) {
                startPolling()
            }
        }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                if (!_isMockMode.value) {
                    _trainStatus.value = TrainRepository.fetchTrainStatus()
                    _pois.value = TrainRepository.fetchPois()
                    _connections.value = TrainRepository.fetchConnections(
                        _trainStatus.value.nextStopEva
                    )
                }
                delay(3000)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }
}
