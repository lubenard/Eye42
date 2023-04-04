package com.lubenard.eye42.pages.search

import androidx.lifecycle.ViewModel
import com.lubenard.eye42.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow

class SearchViewModel : ViewModel() {
    val networkStatus = MutableStateFlow("Not connected")

    var connectionStatus: ConnectionStatus = ConnectionStatus.NOT_CONNECTED
    private val connectionStatusMap = mapOf(
        ConnectionStatus.NOT_CONNECTED to "Not connected",
        ConnectionStatus.CONNECTING to "Trying to connect...",
        ConnectionStatus.CONNECTED to "Connected to Api !",
        ConnectionStatus.ERROR_CONNECTING to "Error while connecting to api",
    )

    fun getNetworkStatus(connectionStatus: ConnectionStatus) {
        this.connectionStatus = connectionStatus
        networkStatus.value = connectionStatusMap[connectionStatus].toString()
    }


}